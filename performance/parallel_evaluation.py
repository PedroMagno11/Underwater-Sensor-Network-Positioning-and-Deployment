from __future__ import annotations

import os
import random
from dataclasses import dataclass
from typing import List, Tuple

import numpy as np
from concurrent.futures import ProcessPoolExecutor, as_completed

from settings.environment_settings import EnvironmentSettings
from settings.simulation_settings import SimulationSettings
from settings.performance_settings import PerformanceSettings
from geometry.grid_geometry import GridGeometry
from acoustic.sound_speed_profile import SoundSpeedProfile
from evaluation.cost_function import evaluate_chromosome_with_report
from evaluation.evaluation_report import EvaluationReport


@dataclass(frozen=True)
class EvaluationJob:
    chromosome: np.ndarray
    chromosome_index: int
    generation_index: int
    global_seed: int


def _compute_job_seed(global_seed: int, generation_index: int, chromosome_index: int) -> int:
    # A simple hash-like mix. Deterministic and stable.
    return (global_seed * 1_000_003) ^ (generation_index * 100_003) ^ (chromosome_index * 10_003)

# Avalia um cromossomo de cada vez
def _evaluate_job(
    job: EvaluationJob,
    number_of_sensors: int,
    environment_settings: EnvironmentSettings,
    simulation_settings: SimulationSettings,
    sound_speed_profile: SoundSpeedProfile,
) -> Tuple[int, EvaluationReport]:
    seed = _compute_job_seed(job.global_seed, job.generation_index, job.chromosome_index)
    random_generator = random.Random(seed)

    grid_geometry = GridGeometry(environment_settings)

    report = evaluate_chromosome_with_report(
        chromosome=job.chromosome,
        number_of_sensors=number_of_sensors,
        grid_geometry=grid_geometry,
        environment_settings=environment_settings,
        simulation_settings=simulation_settings,
        sound_speed_profile=sound_speed_profile,
        random_generator=random_generator,
    )
    return job.chromosome_index, report


def _resolve_number_of_workers(performance_settings: PerformanceSettings) -> int:
    if not performance_settings.enable_parallel_evaluation:
        return 1

    mode = (performance_settings.parallel_evaluation_mode or "auto").lower().strip()
    if mode == "off":
        return 1

    cpu_count = os.cpu_count() or 2
    if mode == "auto":
        return max(1, cpu_count - 1)

    if mode == "fixed":
        return max(1, int(performance_settings.number_of_workers))

    # Fallback
    return max(1, cpu_count - 1)


def evaluate_population(
    population: List[np.ndarray],
    number_of_sensors: int,
    environment_settings: EnvironmentSettings,
    simulation_settings: SimulationSettings,
    sound_speed_profile: SoundSpeedProfile,
    generation_index: int,
    global_seed: int,
    performance_settings: PerformanceSettings,
) -> List[EvaluationReport]:
    """Evaluate a population, potentially in parallel.

    Returns a list of reports ordered by population index.
    """

    number_of_workers = _resolve_number_of_workers(performance_settings)

    jobs = [
        EvaluationJob(
            chromosome=np.array(chromosome, dtype=float),
            chromosome_index=i,
            generation_index=generation_index,
            global_seed=global_seed,
        )
        for i, chromosome in enumerate(population)
    ]

    reports: List[EvaluationReport] = [None] * len(population)  # type: ignore

    if number_of_workers == 1:
        for job in jobs:
            idx, rep = _evaluate_job(
                job,
                number_of_sensors,
                environment_settings,
                simulation_settings,
                sound_speed_profile,
            )
            reports[idx] = rep
        return reports

    with ProcessPoolExecutor(max_workers=number_of_workers) as executor:
        futures = [
            executor.submit(
                _evaluate_job,
                job,
                number_of_sensors,
                environment_settings,
                simulation_settings,
                sound_speed_profile,
            )
            for job in jobs
        ]

        for f in as_completed(futures):
            idx, rep = f.result()
            reports[idx] = rep

    return reports
