from __future__ import annotations

from typing import List, Optional, Tuple
import logging
import random

import numpy as np

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

logger = logging.getLogger("underwater_sensor_ga.ga")

def _extract_costs(reports: List[EvaluationReport]) -> np.ndarray:
    return np.array([r.total_cost for r in reports], dtype=float)

def _compute_no_coverage_rates(reports: List[EvaluationReport]) -> np.ndarray:
    """
    Fraction of impacts without coverage.
    If a report has zero impacts, it is treated as full no-coverage (rate = 1.0).
    """

    return np.array(
        [
            (r.number_of_impacts_without_coverage / r.number_of_impacts)
            if r.number_of_impacts > 0
            else 1.0
            for r in reports
        ],
        dtype=float,
    )

def _computed_average_localization_error(reports: List[EvaluationReport]) -> float:
    """
    Computes the average localization error across reports,
    ignoring reports with undefined (infinite) error.
    """

    finite_errors = [
        r.mean_localization_error_meters for r in reports
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


def run_genetic_algorithm(
    number_of_sensors: int,
    environment_settings: EnvironmentSettings,
    genetic_algorithm_settings: GeneticAlgorithmSettings,
    simulation_settings: SimulationSettings,
    sound_speed_profile: SoundSpeedProfile,
    performance_settings: Optional[PerformanceSettings] = None,
) -> GeneticAlgorithmResult:
    """Run the GA and return a full result object.

    This function is deterministic given:
    - genetic_algorithm_settings.random_seed
    - fixed environment/simulation settings

    Evaluation parallelism is optional and controlled via PerformanceSettings.
    """

    if performance_settings is None:
        performance_settings = PerformanceSettings()

    random_generator = random.Random(genetic_algorithm_settings.random_seed)
    grid_geometry = GridGeometry(environment_settings)

    population: List[np.ndarray] = [
        create_random_chromosome(number_of_sensors, grid_geometry, environment_settings, random_generator)
        for _ in range(genetic_algorithm_settings.population_size)
    ]

    best_chromosome: Optional[np.ndarray] = None
    best_cost: float = float("inf")

    generation_metrics: List[GenerationMetrics] = []
    best_chromosomes_per_generation: List[np.ndarray] = []

    for index_of_generation in range(genetic_algorithm_settings.number_of_generations):
        reports = evaluate_population(
            population=population,
            number_of_sensors=number_of_sensors,
            environment_settings=environment_settings,
            simulation_settings=simulation_settings,
            sound_speed_profile=sound_speed_profile,
            generation_index=index_of_generation,
            global_seed=genetic_algorithm_settings.random_seed,
            performance_settings=performance_settings,
        )

        # Aqui já vem as informações obtidas após a avaliação de todos os cromossomos da população
        costs = [r.total_cost for r in reports]

        # Melhor indivíduo é o que tem o menor custo
        index_of_the_best = int(np.argmin(np.array(costs, dtype=float)))
        generation_best_cost = float(costs[index_of_the_best])
        generation_best_chromosome = np.array(population[index_of_the_best], dtype=float)

        # print(f'CUSTOS: {costs}\nINDICE DO MELHOR: {index_of_the_best}\nMELHOR CUSTO DA GERAÇÃO: {generation_best_cost}\nMelhor chromosome da geração: {generation_best_chromosome}')

        # Adiciona a lista de melhores cromossomos por geração o melhor cromossomo da geração
        best_chromosomes_per_generation.append(generation_best_chromosome)

        if generation_best_cost < best_cost:
            best_cost = generation_best_cost
            best_chromosome = np.array(generation_best_chromosome, dtype=float)

        generation_metrics.append(
            _compute_generation_metrics(
                generation_index=index_of_generation,
                reports=reports,
                best_global_cost=best_cost,
            )
        )

        sorted_indexes = list(np.argsort(np.array(costs, dtype=float)))
        elites = [np.array(population[i], dtype=float) for i in sorted_indexes[: genetic_algorithm_settings.elitism]]

        new_population: List[np.ndarray] = []
        new_population.extend(elites)

        while len(new_population) < genetic_algorithm_settings.population_size:
            father_index = select_index_for_tournament(costs, genetic_algorithm_settings.tournament_size, random_generator)
            mother_index = select_index_for_tournament(costs, genetic_algorithm_settings.tournament_size, random_generator)

            father_chromosome = population[father_index]
            mother_chromosome = population[mother_index]

            if random_generator.random() < genetic_algorithm_settings.crossover_probability:
                child = perform_crossover_per_sensor(
                    father_chromosome=father_chromosome,
                    mother_chromosome=mother_chromosome,
                    number_of_sensors=number_of_sensors,
                    random_generator=random_generator,
                )
            else:
                child = np.array(father_chromosome, dtype=float)

            apply_mutation(
                chromosome=child,
                number_of_sensors=number_of_sensors,
                environment_settings=environment_settings,
                grid_geometry=grid_geometry,
                genetic_algorithm_settings=genetic_algorithm_settings,
                random_generator=random_generator,
            )

            new_population.append(child)

        population = new_population

        # if (index_of_generation + 1) % 10 == 0 or index_of_generation == 0:
        logger.info(
            "Generation %d/%d | best_global_cost=%.3f | generation_best=%.3f",
            index_of_generation + 1,
            genetic_algorithm_settings.number_of_generations,
            best_cost,
            generation_best_cost,
        )

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
