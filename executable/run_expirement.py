from __future__ import annotations
import numpy as np

from settings.environment_settings import EnvironmentSettings
from settings.genetic_algorithm_settings import GeneticAlgorithmSettings
from settings.simulation_settings import SimulationSettings

from geometry.grid_geometry import GridGeometry
from evaluation.chromosome_decoder import chromosome_converter
from acoustic.sound_speed_profile import SoundSpeedProfile
from genetic_algorithm.genetic_algorithm import run_genetic_algorithm

def print_sensors_of_the_best_chromosome(
        best_chromosome: np.ndarray,
        number_of_sensors: int,
        environment_settings: EnvironmentSettings
) -> None:

    grid_geometry = GridGeometry(environment_settings)
    sensors = chromosome_converter(best_chromosome, number_of_sensors, grid_geometry, environment_settings)

    for index, sensor in enumerate(sensors, start=1):
        print(
            f"Sensor {index}: x{sensor.position_x:.1f}m |" 
            f"y={sensor.position_y:.1f}m |" 
            f"depth={sensor.depth:.1f}m |"
        )


if __name__ == "__main__":
    environment_settings = EnvironmentSettings(
        grid_size_in_points=1016,
        grid_spacing=9.0,
        target_region_radius=1500.0,
        maximum_detection_distance=2500.0,
        minimum_depth_in_meters=0.5,
        maximum_depth_in_meters=8.0,
    )

    genetic_algorithm_settings = GeneticAlgorithmSettings(
        number_of_generations=20,
        population_size=30,
        random_seed=123
    )

    simulation_settings = SimulationSettings(
        number_of_impact_points_per_evaluation=60
    )

    # Baseline (SSP constante)
    sound_speed_profile = SoundSpeedProfile.create_constant_profile(1500.0)

    """
    When loading sound speed profile data from a CSV file
    """
    # sound_speed_profile = SoundSpeedProfile.csv_loader(
    #     "sound_speed_profile.csv",
    #     column_name_depth="depth",
    #     column_name_speed="speed"
    # )

    for number_of_sensors in [3, 4, 5]:
        print("\n" + " = " * 70)
        print(f"Running Genetic Algorithm for number os sensors = {number_of_sensors}")

        best_chromosome, best_cost = run_genetic_algorithm(
            number_of_sensors=number_of_sensors,
            environment_settings=environment_settings,
            genetic_algorithm_settings=genetic_algorithm_settings,
            simulation_settings=simulation_settings,
            sound_speed_profile=sound_speed_profile
        )

        print(f"Best final cost: {best_cost:.3f}")
        print_sensors_of_the_best_chromosome(best_chromosome, number_of_sensors, environment_settings)

