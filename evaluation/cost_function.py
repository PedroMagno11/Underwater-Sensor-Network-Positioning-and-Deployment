from __future__ import annotations
from typing import List
import random
import numpy as np

from settings.environment_settings import EnvironmentSettings
from settings.simulation_settings import SimulationSettings
from geometry.grid_geometry import GridGeometry
from geometry.distance_functions import calculate_distance_2d
from acoustic.sound_speed_profile import SoundSpeedProfile
from localization.mle_estimator import estimate_impact_position
from acoustic.acoustic_model_baseline import calculate_arrival_time
from evaluation.chromosome_decoder import chromosome_converter
from evaluation.penalties import calculate_penalty_for_separation_between_sensors

def evaluate_chromosome_cost(
        chromosome: np.ndarray,
        number_of_sensors: int,
        grid_geometry: GridGeometry,
        environment_settings: EnvironmentSettings,
        simulation_settings: SimulationSettings,
        sound_speed_profile: SoundSpeedProfile,
        random_generator: random.Random
) -> float:
    sensors = chromosome_converter(chromosome, number_of_sensors, grid_geometry, environment_settings)

    separation_penalty = calculate_penalty_for_separation_between_sensors(
        sensors, simulation_settings.minimum_distance_between_buoys_in_meters
    )

    number_of_impacts = simulation_settings.number_of_impact_points_per_evaluation
    coverage_penalty = 0.0
    location_errors: List[float] = []

    for _ in range(number_of_impacts):
        impact_position_x, impact_position_y = grid_geometry.generate_random_point_in_target_region(random_generator)

        sensor_detection_indices: List[int] = []
        for index, sensor in enumerate(sensors):
            distance_2d = calculate_distance_2d(
                sensor.position_x, sensor.position_y,
                impact_position_x, impact_position_y,
            )

            if distance_2d <= environment_settings.maximum_detection_distance:
                sensor_detection_indices.append(index)

        if len(sensor_detection_indices) < 3:
            coverage_penalty += simulation_settings.invalid_coverage_penalty
            continue

        sensors_that_detected = [sensors[i] for i in sensor_detection_indices]

        theoretical_times = np.array([
            calculate_arrival_time(sensor, impact_position_x, impact_position_y, sound_speed_profile)
            for sensor in sensors_that_detected
        ], dtype=float)

        actual_emission_time = 0.0
        noise = np.array([
            random_generator.gauss(0.0, simulation_settings. time_noise_standard_deviation)
            for _ in range(len(sensors_that_detected))
        ], dtype=float)

        observed_times = actual_emission_time + theoretical_times + noise

        estimated_impact_position_x, estimated_impact_position_y = estimate_impact_position(
            sensors_that_detected,
            observed_times,
            grid_geometry,
            sound_speed_profile,
            simulation_settings
        )

        error = calculate_distance_2d(estimated_impact_position_x, estimated_impact_position_y, impact_position_x, impact_position_y)
        location_errors.append(error)

    if len(location_errors) == 0:
        return float(number_of_impacts * simulation_settings.invalid_coverage_penalty
                     + simulation_settings.penalty_for_buoys_too_close * separation_penalty
        )


    average_error = float(np.mean(location_errors, dtype=float))

    cost = (
        average_error + simulation_settings.penalty_for_buoys_too_close * separation_penalty
        + coverage_penalty
    )
    print("Cost per sensor: ", cost)
    return float(cost)