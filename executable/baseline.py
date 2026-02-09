from __future__ import annotations

import logging
from pathlib import Path

import numpy as np

from results.result_models import GeneticAlgorithmResult, TopologyResult
from geometry.grid_geometry import GridGeometry
from settings.environment_settings import EnvironmentSettings
from settings.simulation_settings import SimulationSettings
from acoustic.sound_speed_profile import SoundSpeedProfile

from evaluation.cost_function import evaluate_chromosome_with_report
from evaluation.chromosome_decoder import chromosome_converter
from topologies.regular_polygon import create_regular_polygon_chromosome


LOGGER_NAME = "underwater_sensor_ga.baseline"


def evaluate_topology(
    *,
    chromosome: np.ndarray,
    number_of_sensors: int,
    grid_geometry: GridGeometry,
    environment_settings: EnvironmentSettings,
    simulation_settings: SimulationSettings,
    sound_speed_profile: SoundSpeedProfile,
    seed: int,
):
    rng = __import__("random").Random(seed)

    report = evaluate_chromosome_with_report(
        chromosome=chromosome,
        number_of_sensors=number_of_sensors,
        grid_geometry=grid_geometry,
        environment_settings=environment_settings,
        simulation_settings=simulation_settings,
        sound_speed_profile=sound_speed_profile,
        random_generator=rng,
    )

    sensors = chromosome_converter(chromosome, number_of_sensors, grid_geometry, environment_settings)
    return sensors, report


def save_baseline_comparison(
    *,
    output_dir: Path,
    number_of_sensors: int,
    result: GeneticAlgorithmResult,
    grid_geometry: GridGeometry,
    environment_settings: EnvironmentSettings,
    simulation_settings: SimulationSettings,
    sound_speed_profile: SoundSpeedProfile,
) -> None:
    logger = logging.getLogger(LOGGER_NAME)

    from visualization.comparison_plotter import save_topology_comparison_figures

    polygon_chromosome = create_regular_polygon_chromosome(
        number_of_sensors=number_of_sensors,
        environment_settings=environment_settings,
        grid_geometry=grid_geometry,
        polygon_radius_meters=environment_settings.target_region_radius,
        depth_meters=(environment_settings.minimum_depth_in_meters + environment_settings.maximum_depth_in_meters) / 2.0,
        angle_offset_degrees=0.0,
    )

    eval_seed = 999  # fixed seed for fair comparison

    ga_sensors, ga_report = evaluate_topology(
        chromosome=result.best_chromosome,
        number_of_sensors=number_of_sensors,
        grid_geometry=grid_geometry,
        environment_settings=environment_settings,
        simulation_settings=simulation_settings,
        sound_speed_profile=sound_speed_profile,
        seed=eval_seed,
    )

    poly_sensors, poly_report = evaluate_topology(
        chromosome=polygon_chromosome,
        number_of_sensors=number_of_sensors,
        grid_geometry=grid_geometry,
        environment_settings=environment_settings,
        simulation_settings=simulation_settings,
        sound_speed_profile=sound_speed_profile,
        seed=eval_seed,
    )

    topology_results = [
        TopologyResult(
            label="GA Best",
            number_of_sensors=number_of_sensors,
            sensors=ga_sensors,
            mean_error_meters=ga_report.mean_localization_error_meters,
            no_coverage_rate=ga_report.number_of_impacts_without_coverage / max(1, ga_report.number_of_impacts),
            total_cost=ga_report.total_cost,
            notes="Optimized by Genetic Algorithm",
        ),
        TopologyResult(
            label="Regular Polygon",
            number_of_sensors=number_of_sensors,
            sensors=poly_sensors,
            mean_error_meters=poly_report.mean_localization_error_meters,
            no_coverage_rate=poly_report.number_of_impacts_without_coverage / max(1, poly_report.number_of_impacts),
            total_cost=poly_report.total_cost,
            notes="Regular polygon baseline",
        ),
    ]

    logger.info(
        "Baseline comparison (N=%d): GA cost=%.3f | Polygon cost=%.3f",
        number_of_sensors,
        ga_report.total_cost,
        poly_report.total_cost,
    )

    save_topology_comparison_figures(
        results=topology_results,
        title_prefix=f"Topology Comparison (N={number_of_sensors})",
        output_png_path_prefix=str(output_dir / f"topology_comparison_N{number_of_sensors}.png"),
    )
