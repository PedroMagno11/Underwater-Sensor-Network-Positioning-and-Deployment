from __future__ import annotations

import os
import logging
from pathlib import Path
from typing import Tuple

os.environ["MPLBACKEND"] = "Agg"

from logging_utils.logging_configuration import setup_logging
from settings.configuration_loader import load_json_config, load_settings_from_config
from settings.environment_settings import EnvironmentSettings
from settings.genetic_algorithm_settings import GeneticAlgorithmSettings
from settings.simulation_settings import SimulationSettings
from settings.performance_settings import PerformanceSettings
from settings.visualization_settings import VisualizationSettings

from acoustic.sound_speed_profile import SoundSpeedProfile
from genetic_algorithm.genetic_algorithm import run_genetic_algorithm
from results.result_models import GeneticAlgorithmResult



LOGGER_NAME = "underwater_sensor_ga.runner"


def load_all_settings(config_path: str) -> Tuple[
    EnvironmentSettings,
    GeneticAlgorithmSettings,
    SimulationSettings,
    PerformanceSettings,
    VisualizationSettings,
]:
    config_file = Path(config_path)
    if not config_file.exists():
        raise FileNotFoundError(
            f"Config file not found: {config_path}. "
            f"Create it or copy the provided 'experiment_config.json'."
        )

    config = load_json_config(config_path)
    return load_settings_from_config(config)


def build_sound_speed_profile() -> SoundSpeedProfile:
    # Baseline SSP from T/S profiles (you can swap to CSV loader later)
    return SoundSpeedProfile.from_temperature_salinity_profiles(
        depths_in_meters=[0.5, 2.0, 5.0, 8.0],
        temperatures_celsius=[26.5, 26.0, 25.2, 24.8],
        salinity_psu=[35.0, 35.1, 35.2, 35.2],
    )

def run_experiment_for_n(
    *,
    number_of_sensors: int,
    environment_settings: EnvironmentSettings,
    genetic_algorithm_settings: GeneticAlgorithmSettings,
    simulation_settings: SimulationSettings,
    performance_settings: PerformanceSettings,
    visualization_settings: VisualizationSettings,
    sound_speed_profile: SoundSpeedProfile,
) -> None:
    logger = logging.getLogger(LOGGER_NAME)

    logger.info("=" * 80)
    logger.info("Running Genetic Algorithm for number_of_sensors=%d", number_of_sensors)

    result: GeneticAlgorithmResult = run_genetic_algorithm(
        number_of_sensors=number_of_sensors,
        environment_settings=environment_settings,
        genetic_algorithm_settings=genetic_algorithm_settings,
        simulation_settings=simulation_settings,
        sound_speed_profile=sound_speed_profile,
        performance_settings=performance_settings,
    )

    logger.info("Final best cost (N=%d): %.3f", number_of_sensors, result.best_cost)

def main() -> None:
    setup_logging(log_level=logging.INFO, log_file_path="execution.log", log_to_console=True)
    logger = logging.getLogger(LOGGER_NAME)

    (
        environment_settings,
        genetic_algorithm_settings,
        simulation_settings,
        performance_settings,
        visualization_settings,
    ) = load_all_settings("experiment_config.json")

    sound_speed_profile = build_sound_speed_profile()

    for number_of_sensors in [3, 4, 5]:
        run_experiment_for_n(
            number_of_sensors=number_of_sensors,
            environment_settings=environment_settings,
            genetic_algorithm_settings=genetic_algorithm_settings,
            simulation_settings=simulation_settings,
            performance_settings=performance_settings,
            visualization_settings=visualization_settings,
            sound_speed_profile=sound_speed_profile,
        )

    logger.info("All experiments finished. Outputs saved under '%s'", visualization_settings.output_directory)


if __name__ == "__main__":
    main()
