from __future__ import annotations
import os

from results.result_models import TopologyResult

os.environ["MPLBACKEND"] = "Agg"
import logging
from pathlib import Path
from logging_utils.logging_configuration import setup_logging
from settings.configuration_loader import load_json_config, load_settings_from_config
from settings.environment_settings import EnvironmentSettings
from settings.genetic_algorithm_settings import GeneticAlgorithmSettings
from settings.simulation_settings import SimulationSettings
from settings.performance_settings import PerformanceSettings
from settings.visualization_settings import VisualizationSettings
from geometry.grid_geometry import GridGeometry
from evaluation.chromosome_decoder import chromosome_converter
from acoustic.sound_speed_profile import SoundSpeedProfile
from genetic_algorithm.genetic_algorithm import run_genetic_algorithm
from results.csv_exporter import export_generation_metrics_to_csv
from evaluation.cost_function import evaluate_chromosome_with_report
from topologies.regular_polygon import create_regular_polygon_chromosome



def _load_all_settings(config_path: str) -> tuple[
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


def _postprocess_and_save_outputs(
    number_of_sensors: int,
    environment_settings: EnvironmentSettings,
    simulation_settings: SimulationSettings,
    sound_speed_profile: SoundSpeedProfile,
    visualization_settings: VisualizationSettings,
    result,
) -> None:

    from visualization.scenario_plotter import save_scenario_figure
    from visualization.ga_progress_plotter import (
        save_ga_cost_progress_figure,
        save_ga_coverage_and_error_figure,
    )
    from visualization.heatmap_plotter import save_error_heatmap_figure
    from visualization.comparison_plotter import save_topology_comparison_bar_chart, save_topology_comparison_figures

    output_root = Path(visualization_settings.output_directory)
    output_dir = output_root / f"sensors_{number_of_sensors}"
    output_dir.mkdir(parents=True, exist_ok=True)

    grid_geometry = GridGeometry(environment_settings)

    # 1) Save best-per-generation topology figures
    if visualization_settings.save_best_per_generation_figures:
        best_dir = output_dir / "best_per_generation"
        best_dir.mkdir(parents=True, exist_ok=True)

        max_figs = min(
            visualization_settings.max_generation_figures,
            len(result.best_chromosomes_per_generation),
        )

        for gen_index in range(max_figs):
            chromosome = result.best_chromosomes_per_generation[gen_index]
            sensors = chromosome_converter(chromosome, number_of_sensors, grid_geometry, environment_settings)

            annotation = (
                f"gen={gen_index} | best_global_cost={result.generation_metrics[gen_index].best_global_cost:.3f}"
            )

            save_scenario_figure(
                sensors=sensors,
                environment_settings=environment_settings,
                title=f"Best topology - generation {gen_index}",
                output_png_path=str(best_dir / f"gen_{gen_index:04d}.png"),
                annotation_text=annotation,
            )

    # 2) Save final best topology
    if visualization_settings.save_final_best_figure:
        save_scenario_figure(
            sensors=result.best_sensors,
            environment_settings=environment_settings,
            title=f"Final best topology (N={number_of_sensors}) | cost={result.best_cost:.3f}",
            output_png_path=str(output_dir / "best_topology.png"),
            annotation_text=f"best_cost={result.best_cost:.3f}",
        )

    # 3) Progress figures
    if visualization_settings.save_progress_figures:
        save_ga_cost_progress_figure(
            metrics=result.generation_metrics,
            title=f"GA cost progress (N={number_of_sensors})",
            output_png_path=str(output_dir / "ga_cost_progress.png"),
        )
        save_ga_coverage_and_error_figure(
            metrics=result.generation_metrics,
            title=f"GA coverage & error progress (N={number_of_sensors})",
            output_png_path=str(output_dir / "ga_coverage_error_progress.png"),
        )

    # 4) Heatmap
    if visualization_settings.save_error_heatmap:
        save_error_heatmap_figure(
            sensors=result.best_sensors,
            environment_settings=environment_settings,
            simulation_settings=simulation_settings,
            sound_speed_profile=sound_speed_profile,
            step_in_meters=visualization_settings.heatmap_step_in_meters,
            title=f"Error heatmap (N={number_of_sensors})",
            output_png_path=str(output_dir / "error_heatmap.png"),
        )

    # 5) Export CSV
    if visualization_settings.export_metrics_csv:
        export_generation_metrics_to_csv(
            generation_metrics=result.generation_metrics,
            output_csv_path=str(output_dir / "generation_metrics.csv"),
        )

    # 6) Baseline comparison (regular polygon)
    try:
        polygon_chromosome = create_regular_polygon_chromosome(
            number_of_sensors=number_of_sensors,
            environment_settings=environment_settings,
            grid_geometry=grid_geometry,
            polygon_radius_meters=environment_settings.target_region_radius,
            depth_meters=(environment_settings.minimum_depth_in_meters + environment_settings.maximum_depth_in_meters) / 2.0,
            angle_offset_degrees=0.0,
        )

        polygon_report = evaluate_chromosome_with_report(
            chromosome=polygon_chromosome,
            number_of_sensors=number_of_sensors,
            grid_geometry=grid_geometry,
            environment_settings=environment_settings,
            simulation_settings=simulation_settings,
            sound_speed_profile=sound_speed_profile,
            random_generator=__import__("random").Random(999),
        )

        ga_best_report = evaluate_chromosome_with_report(
            chromosome=result.best_chromosome,
            number_of_sensors=number_of_sensors,
            grid_geometry=grid_geometry,
            environment_settings=environment_settings,
            simulation_settings=simulation_settings,
            sound_speed_profile=sound_speed_profile,
            random_generator=__import__("random").Random(999),
        )

        polygon_sensors = chromosome_converter(
            polygon_chromosome, number_of_sensors, grid_geometry, environment_settings
        )

        topology_results = [
            TopologyResult(
                label="GA Best",
                number_of_sensors=number_of_sensors,
                sensors=result.best_sensors,
                mean_error_meters=ga_best_report.mean_localization_error_meters,
                no_coverage_rate=ga_best_report.number_of_impacts_without_coverage/max(1, ga_best_report.number_of_impacts),
                total_cost=ga_best_report.total_cost,
                notes="Optimized by Genetic Algorithm"
            ),

            TopologyResult(
                label="Regular Polygon",
                number_of_sensors=number_of_sensors,
                sensors=polygon_sensors,
                mean_error_meters=polygon_report.mean_localization_error_meters,
                no_coverage_rate=polygon_report.number_of_impacts_without_coverage / max(1, polygon_report.number_of_impacts),
                total_cost=polygon_report.total_cost,
                notes="Regular polygon baseline",
            )
        ]

        # metrics_by_label = {
        #     "GA_best": {
        #         "total_cost": ga_best_report.total_cost,
        #         "mean_error_meters": ga_best_report.mean_localization_error_meters,
        #         "no_coverage_rate": ga_best_report.number_of_impacts_without_coverage / ga_best_report.number_of_impacts,
        #     },
        #     "Regular_polygon": {
        #         "total_cost": polygon_report.total_cost,
        #         "mean_error_meters": polygon_report.mean_localization_error_meters,
        #         "no_coverage_rate": polygon_report.number_of_impacts_without_coverage / polygon_report.number_of_impacts,
        #     },
        # }

        save_topology_comparison_figures(
            results = topology_results,
            title_prefix=f"Topology Comparison (N={number_of_sensors})",
            output_png_path_prefix=str(output_dir/ f"topology_comparison_N{number_of_sensors}.png"),
        )

        # save_topology_comparison_bar_chart(
        #     title=f"Topology comparison (N={number_of_sensors})",
        #     metrics_by_label=metrics_by_label,
        #     output_png_path=str(output_dir / "topology_comparison.png"),
        # )
    except Exception:
        # Baseline comparison is nice-to-have; do not break the pipeline if anything goes wrong.
        logger = logging.getLogger("underwater_sensor_ga.runner")
        logger.exception("Topology baseline comparison failed for N=%d", number_of_sensors)

if __name__ == "__main__":
    setup_logging(log_level=logging.INFO, log_file_path="execution.log", log_to_console=True)
    logger = logging.getLogger("underwater_sensor_ga.runner")

    # Load settings from JSON (recommended)
    (
        environment_settings,
        genetic_algorithm_settings,
        simulation_settings,
        performance_settings,
        visualization_settings,
    ) = _load_all_settings("experiment_config.json")

    # Baseline (constant SSP) - switch to CSV loader whenever you want
    sound_speed_profile = SoundSpeedProfile.create_constant_profile(1500.0)

    # Example: load SSP from CSV
    # sound_speed_profile = SoundSpeedProfile.csv_loader(
    #     "sound_speed_profile.csv",
    #     column_name_depth="depth",
    #     column_name_speed="speed",
    # )

    for number_of_sensors in [3, 4, 5]:
        logger.info("=" * 80)
        logger.info("Running Genetic Algorithm for number_of_sensors=%d", number_of_sensors)

        result = run_genetic_algorithm(
            number_of_sensors=number_of_sensors,
            environment_settings=environment_settings,
            genetic_algorithm_settings=genetic_algorithm_settings,
            simulation_settings=simulation_settings,
            sound_speed_profile=sound_speed_profile,
            performance_settings=performance_settings,
        )

        logger.info("Final best cost (N=%d): %.3f", number_of_sensors, result.best_cost)

        _postprocess_and_save_outputs(
            number_of_sensors=number_of_sensors,
            environment_settings=environment_settings,
            simulation_settings=simulation_settings,
            sound_speed_profile=sound_speed_profile,
            visualization_settings=visualization_settings,
            result=result,
        )

    logger.info("All experiments finished. Outputs saved under '%s'", visualization_settings.output_directory)
