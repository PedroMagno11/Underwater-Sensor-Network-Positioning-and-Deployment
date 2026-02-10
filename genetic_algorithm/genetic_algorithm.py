from __future__ import annotations

from typing import List, Optional
import logging
import random
from pathlib import Path

import numpy as np
from concurrent.futures import ProcessPoolExecutor

from settings.environment_settings import EnvironmentSettings
from settings.genetic_algorithm_settings import GeneticAlgorithmSettings
from settings.simulation_settings import SimulationSettings
from settings.performance_settings import PerformanceSettings
from geometry.grid_geometry import GridGeometry
from acoustic.sound_speed_profile import SoundSpeedProfile

from performance.parallel_evaluation import evaluate_population
from evaluation.evaluation_report import EvaluationReport
from evaluation.chromosome_decoder import chromosome_converter
from genetic_algorithm.selection import select_index_for_tournament
from genetic_algorithm.crossover import perform_crossover_per_sensor
from genetic_algorithm.mutation import apply_mutation
from genetic_algorithm.population import create_random_chromosome
from results.result_models import GeneticAlgorithmResult, GenerationMetrics

# NEW
from results.report_exporter import write_best_reports_jsonl

logger = logging.getLogger("underwater_sensor_ga.ga")


def _extract_costs(reports: List[EvaluationReport]) -> np.ndarray:
    return np.array([r.total_cost for r in reports], dtype=float)


def _compute_no_coverage_rates(reports: List[EvaluationReport]) -> np.ndarray:
    return np.array(
        [
            (r.number_of_impacts_without_coverage / r.number_of_impacts) if r.number_of_impacts > 0 else 1.0
            for r in reports
        ],
        dtype=float,
    )


def _computed_average_localization_error(reports: List[EvaluationReport]) -> float:
    finite_errors = [
        r.mean_localization_error_meters
        for r in reports
        if np.isfinite(r.mean_localization_error_meters)
    ]
    return float(np.mean(finite_errors)) if finite_errors else float("inf")


def _compute_generation_metrics(
    generation_index: int,
    reports: List[EvaluationReport],
    best_global_cost: float,
) -> GenerationMetrics:
    costs = _extract_costs(reports)
    no_coverage_rates = _compute_no_coverage_rates(reports)
    avg_error = _computed_average_localization_error(reports)

    return GenerationMetrics(
        generation_index=generation_index,
        cost_min=float(np.min(costs)),
        cost_avg=float(np.mean(costs)),
        cost_median=float(np.median(costs)),
        cost_p90=float(np.percentile(costs, 90)),
        avg_no_coverage_rate=float(np.mean(no_coverage_rates)),
        avg_error_meters=avg_error,
        best_global_cost=best_global_cost,
    )


def _create_executor_if_needed(performance_settings: PerformanceSettings) -> Optional[ProcessPoolExecutor]:
    if not performance_settings.enable_parallel_evaluation:
        return None

    mode = (performance_settings.parallel_evaluation_mode or "auto").lower().strip()
    if mode == "off":
        return None

    if mode == "fixed":
        workers = max(1, int(performance_settings.number_of_workers))
    else:
        import os
        workers = max(1, (os.cpu_count() or 2) - 1)

    return ProcessPoolExecutor(max_workers=workers) if workers > 1 else None


def run_genetic_algorithm(
    number_of_sensors: int,
    environment_settings: EnvironmentSettings,
    genetic_algorithm_settings: GeneticAlgorithmSettings,
    simulation_settings: SimulationSettings,
    sound_speed_profile: SoundSpeedProfile,
    performance_settings: Optional[PerformanceSettings] = None,
    # NEW: where to save the jsonl log
    reports_output_path: Optional[str] = None,
) -> GeneticAlgorithmResult:
    if performance_settings is None:
        performance_settings = PerformanceSettings()

    rng = random.Random(genetic_algorithm_settings.random_seed)
    grid_geometry = GridGeometry(environment_settings)

    population: List[np.ndarray] = [
        create_random_chromosome(number_of_sensors, grid_geometry, environment_settings, rng)
        for _ in range(genetic_algorithm_settings.population_size)
    ]

    best_chromosome: Optional[np.ndarray] = None
    best_cost: float = float("inf")

    # NEW: track the best global report itself
    best_global_report: Optional[EvaluationReport] = None

    generation_metrics: List[GenerationMetrics] = []
    best_chromosomes_per_generation: List[np.ndarray] = []

    # NEW: default output location (per N sensors)
    if reports_output_path is None:
        reports_output_path = str(
            Path("outputs") / f"sensors_{number_of_sensors}" / "best_reports.jsonl"
        )

    executor = _create_executor_if_needed(performance_settings)

    try:
        for gen_idx in range(genetic_algorithm_settings.number_of_generations):
            reports = evaluate_population(
                population=population,
                number_of_sensors=number_of_sensors,
                environment_settings=environment_settings,
                simulation_settings=simulation_settings,
                sound_speed_profile=sound_speed_profile,
                generation_index=gen_idx,
                global_seed=genetic_algorithm_settings.random_seed,
                performance_settings=performance_settings,
                executor=executor,  # IMPORTANT reuse pool
            )

            costs = [r.total_cost for r in reports]
            idx_best = int(np.argmin(np.array(costs, dtype=float)))

            gen_best_cost = float(costs[idx_best])
            gen_best_report = reports[idx_best]
            gen_best_chromosome = np.array(population[idx_best], dtype=float, copy=True)

            best_chromosomes_per_generation.append(gen_best_chromosome)

            # Update global best using generation best
            if gen_best_cost < best_cost:
                best_cost = gen_best_cost
                best_chromosome = np.array(gen_best_chromosome, dtype=float, copy=True)
                best_global_report = gen_best_report  # NEW

            # If global report wasn't set yet (first generation)
            if best_global_report is None:
                best_global_report = gen_best_report

            # NEW: Persist best reports for auditing
            write_best_reports_jsonl(
                output_path=reports_output_path,
                generation_index=gen_idx,
                number_of_sensors=number_of_sensors,
                best_of_generation=gen_best_report,
                best_global=best_global_report,
                include_impact_points=False,  # set True if you want to store points (bigger file)
            )

            generation_metrics.append(
                _compute_generation_metrics(
                    generation_index=gen_idx,
                    reports=reports,
                    best_global_cost=best_cost,
                )
            )

            # elitism
            sorted_idx = list(np.argsort(np.array(costs, dtype=float)))
            elites = [
                np.array(population[i], dtype=float, copy=True)
                for i in sorted_idx[: genetic_algorithm_settings.elitism]
            ]

            new_population: List[np.ndarray] = []
            new_population.extend(elites)

            while len(new_population) < genetic_algorithm_settings.population_size:
                father_idx = select_index_for_tournament(costs, genetic_algorithm_settings.tournament_size, rng)
                mother_idx = select_index_for_tournament(costs, genetic_algorithm_settings.tournament_size, rng)

                father = population[father_idx]
                mother = population[mother_idx]

                if rng.random() < genetic_algorithm_settings.crossover_probability:
                    child = perform_crossover_per_sensor(
                        father_chromosome=father,
                        mother_chromosome=mother,
                        number_of_sensors=number_of_sensors,
                        random_generator=rng,
                    )
                else:
                    child = np.array(father, dtype=float, copy=True)

                apply_mutation(
                    chromosome=child,
                    number_of_sensors=number_of_sensors,
                    environment_settings=environment_settings,
                    grid_geometry=grid_geometry,
                    genetic_algorithm_settings=genetic_algorithm_settings,
                    random_generator=rng,
                )

                new_population.append(child)

            population = new_population

            logger.info(
                "Generation %d/%d | best_global_cost=%.3f | generation_best=%.3f",
                gen_idx + 1,
                genetic_algorithm_settings.number_of_generations,
                best_cost,
                gen_best_cost,
            )

    finally:
        if executor is not None:
            executor.shutdown(wait=True)

    if best_chromosome is None:
        raise RuntimeError("Unexpected error: best chromosome not found")

    best_sensors = chromosome_converter(best_chromosome, number_of_sensors, grid_geometry, environment_settings)

    return GeneticAlgorithmResult(
        number_of_sensors=number_of_sensors,
        best_chromosome=best_chromosome,
        best_cost=best_cost,
        best_sensors=best_sensors,
        generation_metrics=generation_metrics,
        best_chromosomes_per_generation=best_chromosomes_per_generation,
    )
