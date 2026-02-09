from __future__ import annotations

from typing import List, Tuple
import logging
import random

import numpy as np

from geometry.impact_position import ImpactPosition
from settings.environment_settings import EnvironmentSettings
from settings.simulation_settings import SimulationSettings
from geometry.grid_geometry import GridGeometry
from geometry.distance_functions import calculate_distance_2d
from acoustic.sound_speed_profile import SoundSpeedProfile
from localization.mle_estimator import estimate_impact_position
from acoustic.acoustic_model_baseline import calculate_arrival_time, calculate_arrival_time_straight_line
from evaluation.chromosome_decoder import chromosome_converter
from evaluation.penalties import calculate_penalty_for_separation_between_sensors
from evaluation.evaluation_report import EvaluationReport

logger = logging.getLogger("underwater_sensor_ga.evaluation")


def evaluate_chromosome_with_report(
    chromosome: np.ndarray,
    number_of_sensors: int,
    grid_geometry: GridGeometry,
    environment_settings: EnvironmentSettings,
    simulation_settings: SimulationSettings,
    sound_speed_profile: SoundSpeedProfile,
    random_generator: random.Random,
) -> EvaluationReport:
    sensors = chromosome_converter(chromosome, number_of_sensors, grid_geometry, environment_settings)

    separation_penalty = calculate_penalty_for_separation_between_sensors(
        sensors, simulation_settings.minimum_distance_between_buoys_in_meters
    )

    number_of_impacts = simulation_settings.number_of_impact_points_per_evaluation
    coverage_penalty = 0.0
    location_errors: List[float] = []
    impacts_without_coverage = 0
    impact_points: List[Tuple[float, float]] = []
    for i, _ in enumerate(range(number_of_impacts)):
        impact_position_x, impact_position_y = ImpactPosition.generate_impact_position(grid_geometry=grid_geometry, global_seed=number_of_impacts, target_index=i)
        impact_points.append((impact_position_x, impact_position_y))
        sensor_detection_indices: List[int] = []
        for index, sensor in enumerate(sensors):
            distance_2d = calculate_distance_2d(
                sensor.position_x,
                sensor.position_y,
                impact_position_x,
                impact_position_y,
            )

            if distance_2d <= environment_settings.maximum_detection_distance:
                sensor_detection_indices.append(index)

        if len(sensor_detection_indices) < 3:
            impacts_without_coverage += 1
            coverage_penalty += simulation_settings.invalid_coverage_penalty
            continue

        sensors_that_detected = [sensors[i] for i in sensor_detection_indices]

        theoretical_times = np.array(
            [
                calculate_arrival_time_straight_line(sensor, impact_position_x, impact_position_y, sound_speed_profile)
                for sensor in sensors_that_detected
            ],
            dtype=float,
        )

        actual_emission_time = 0.0
        noise = np.array(
            [
                random_generator.gauss(0.0, simulation_settings.time_noise_standard_deviation)
                for _ in range(len(sensors_that_detected))
            ],
            dtype=float,
        )
        # print(f'TEMPO EMISSAO: {actual_emission_time} - TEMPO NOISE: {noise} - TEMPO TEORICO: {theoretical_times}')
        observed_times = actual_emission_time + theoretical_times + noise
        # print(f"TEMPO OBSERVADO: {observed_times}")
        estimated_impact_position_x, estimated_impact_position_y = estimate_impact_position(
            sensors_that_detected,
            observed_times,
            grid_geometry,
            sound_speed_profile,
            simulation_settings,
        )
        # print(f'Impacto Real {i} - Posicao: {impact_position_x}, {impact_position_y}\nImpacto Calculado: {estimated_impact_position_x}, {estimated_impact_position_y}')

        # tolerance_m = simulation_settings.localization_tolerance_meters

        error = calculate_distance_2d(
            estimated_impact_position_x,
            estimated_impact_position_y,
            impact_position_x,
            impact_position_y,
        )

        # effective_error = max(0.0, float(error) - tolerance_m)
        location_errors.append(error)
        
    if len(location_errors) == 0:
        total_cost = float(
            number_of_impacts * simulation_settings.invalid_coverage_penalty
            + simulation_settings.penalty_for_buoys_too_close * separation_penalty
        )
        return EvaluationReport(
            total_cost=total_cost,
            mean_localization_error_meters=float("inf"),
            number_of_impacts=number_of_impacts,
            number_of_localizable_impacts=0,
            number_of_impacts_without_coverage=number_of_impacts,
            coverage_penalty=float(number_of_impacts) * simulation_settings.invalid_coverage_penalty,
            separation_penalty=float(separation_penalty),
            impact_points=impact_points
        )

    average_error = float(np.mean(np.array(location_errors, dtype=float)))

    cost = (
        average_error
        + simulation_settings.penalty_for_buoys_too_close * separation_penalty
        + coverage_penalty
    )

    return EvaluationReport(
        total_cost=float(cost),
        mean_localization_error_meters=average_error,
        number_of_impacts=number_of_impacts,
        number_of_localizable_impacts=len(location_errors),
        number_of_impacts_without_coverage=impacts_without_coverage,
        coverage_penalty=float(coverage_penalty),
        separation_penalty=float(separation_penalty),
        impact_points=impact_points
    )


# def evaluate_chromosome_cost(
#     chromosome: np.ndarray,
#     number_of_sensors: int,
#     grid_geometry: GridGeometry,
#     environment_settings: EnvironmentSettings,
#     simulation_settings: SimulationSettings,
#     sound_speed_profile: SoundSpeedProfile,
#     random_generator: random.Random,
# ) -> float:
#     """Backward compatible wrapper (returns only the scalar cost)."""
#     report = evaluate_chromosome_with_report(
#         chromosome=chromosome,
#         number_of_sensors=number_of_sensors,
#         grid_geometry=grid_geometry,
#         environment_settings=environment_settings,
#         simulation_settings=simulation_settings,
#         sound_speed_profile=sound_speed_profile,
#         random_generator=random_generator,
#     )
#     return report.total_cost
