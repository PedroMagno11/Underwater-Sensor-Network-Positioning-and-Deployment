from __future__ import annotations

import os
import logging
from pathlib import Path
from typing import Tuple

os.environ["MPLBACKEND"] = "Agg"

from logging_utils.logging_configuration import setup_logging
from settings.configuration_loader import load_json_config, load_settings_from_config
from settings.environment_settings import EnvironmentSettings
from settings.simulation_settings import SimulationSettings
from settings.performance_settings import PerformanceSettings
from settings.visualization_settings import VisualizationSettings

from acoustic.sound_speed_profile import SoundSpeedProfile

from particle_swarm.particle_swarm import run_particle_swarm
from settings.particle_swarm_settings import ParticleSwarmSettings


LOGGER_NAME = "underwater_sensor_ga.pso_runner"


def load_all_settings(config_path: str) -> Tuple[
    EnvironmentSettings,
    SimulationSettings,
    PerformanceSettings,
    VisualizationSettings,
]:
    config_file = Path(config_path)
    if not config_file.exists():
        raise FileNotFoundError(f"Config file not found: {config_path}")

    config = load_json_config(config_path)
    (
        environment_settings,
        _ga_settings,  # ignorado no PSO runner
        simulation_settings,
        performance_settings,
        visualization_settings,
    ) = load_settings_from_config(config)
    return environment_settings, simulation_settings, performance_settings, visualization_settings


def build_sound_speed_profile() -> SoundSpeedProfile:
    # mesma do seu runner
    return SoundSpeedProfile.from_temperature_salinity_profiles(
        depths_in_meters=[0.5, 2.0, 5.0, 8.0],
        temperatures_celsius=[26.5, 26.0, 25.2, 24.8],
        salinity_psu=[35.0, 35.1, 35.2, 35.2],
    )


def main() -> None:
    setup_logging(log_level=logging.INFO, log_file_path="execution_pso.log", log_to_console=True)
    logger = logging.getLogger(LOGGER_NAME)

    env, sim, perf, vis = load_all_settings("experiment_config.json")
    ssp = build_sound_speed_profile()

    # ====== PSO settings ======
    # Rápido (sanity check):
    pso_settings = ParticleSwarmSettings(
        random_seed=321,
        swarm_size=60,
        number_of_iterations=300,
        inertia_w=0.72,
        cognitive_c1=1.49,
        social_c2=1.49,
        vmax_fraction_of_range=0.05,  # ~150m/iter em XY (alinhado à sua mutação 150m)
    )

    # Se quiser "equivalente ao GA" (125k avaliações):
    # pso_settings = ParticleSwarmSettings(
    #     random_seed=321,
    #     swarm_size=100,
    #     number_of_iterations=1250,
    #     inertia_w=0.72,
    #     cognitive_c1=1.49,
    #     social_c2=1.49,
    #     vmax_fraction_of_range=0.05,
    # )

    for n in [3, 4, 5]:
        logger.info("=" * 80)
        logger.info("Running PSO for number_of_sensors=%d", n)

        result = run_particle_swarm(
            number_of_sensors=n,
            environment_settings=env,
            simulation_settings=sim,
            sound_speed_profile=ssp,
            performance_settings=perf,
            pso_settings=pso_settings,
        )

        logger.info("Final PSO best cost (N=%d): %.6f", n, result.best_cost)

    logger.info("All PSO experiments finished. Outputs saved under '%s'", vis.output_directory)


if __name__ == "__main__":
    main()