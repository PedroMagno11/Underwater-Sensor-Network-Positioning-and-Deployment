from __future__ import annotations

import logging
from pathlib import Path

from settings.environment_settings import EnvironmentSettings
from settings.simulation_settings import SimulationSettings
from settings.visualization_settings import VisualizationSettings
from acoustic.sound_speed_profile import SoundSpeedProfile
from geometry.grid_geometry import GridGeometry
from results.result_models import GeneticAlgorithmResult

from results.csv_exporter import export_generation_metrics_to_csv
from evaluation.chromosome_decoder import chromosome_converter

from executable.baseline import save_baseline_comparison  # local module


LOGGER_NAME = "underwater_sensor_ga.postprocess"


def ensure_output_dir(output_root: str, number_of_sensors: int) -> Path:
    output_dir = Path(output_root) / f"sensors_{number_of_sensors}"
    output_dir.mkdir(parents=True, exist_ok=True)
    return output_dir


def save_best_per_generation_figures(
    *,
    output_dir: Path,
    number_of_sensors: int,
    result: GeneticAlgorithmResult,
    grid_geometry: GridGeometry,
    environment_settings: EnvironmentSettings,
    visualization_settings: VisualizationSettings,
) -> None:
    if not visualization_settings.save_best_per_generation_figures:
        return

    from visualization.scenario_plotter import save_scenario_figure

    best_dir = output_dir / "best_per_generation"
    best_dir.mkdir(parents=True, exist_ok=True)

    max_figs = min(visualization_settings.max_generation_figures, len(result.best_chromosomes_per_generation))
    for gen_index in range(max_figs):
        chromosome = result.best_chromosomes_per_generation[gen_index]
        sensors = chromosome_converter(chromosome, number_of_sensors, grid_geometry, environment_settings)

        metrics = result.generation_metrics[gen_index]
        annotation = f"gen={gen_index} | best_global_cost={metrics.best_global_cost:.3f}"

        save_scenario_figure(
            sensors=sensors,
            environment_settings=environment_settings,
            title=f"Best topology - generation {gen_index}",
            output_png_path=str(best_dir / f"gen_{gen_index:04d}.png"),
            annotation_text=annotation,
        )


def save_final_best_figure(
    *,
    output_dir: Path,
    number_of_sensors: int,
    result: GeneticAlgorithmResult,
    environment_settings: EnvironmentSettings,
    visualization_settings: VisualizationSettings,
) -> None:
    if not visualization_settings.save_final_best_figure:
        return

    from visualization.scenario_plotter import save_scenario_figure

    save_scenario_figure(
        sensors=result.best_sensors,
        environment_settings=environment_settings,
        title=f"Final best topology (N={number_of_sensors}) | cost={result.best_cost:.3f}",
        output_png_path=str(output_dir / "best_topology.png"),
        annotation_text=f"best_cost={result.best_cost:.3f}",
    )


def save_progress_figures(
    *,
    output_dir: Path,
    number_of_sensors: int,
    result: GeneticAlgorithmResult,
    visualization_settings: VisualizationSettings,
) -> None:
    if not visualization_settings.save_progress_figures:
        return

    from visualization.ga_progress_plotter import (
        save_ga_cost_progress_figure,
        save_ga_coverage_and_error_figure,
    )

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


def save_error_heatmap(
    *,
    output_dir: Path,
    number_of_sensors: int,
    result: GeneticAlgorithmResult,
    environment_settings: EnvironmentSettings,
    simulation_settings: SimulationSettings,
    sound_speed_profile: SoundSpeedProfile,
    visualization_settings: VisualizationSettings,
) -> None:
    if not visualization_settings.save_error_heatmap:
        return

    from visualization.heatmap_plotter import save_error_heatmap_figure

    save_error_heatmap_figure(
        sensors=result.best_sensors,
        environment_settings=environment_settings,
        simulation_settings=simulation_settings,
        sound_speed_profile=sound_speed_profile,
        step_in_meters=visualization_settings.heatmap_step_in_meters,
        title=f"Error heatmap (N={number_of_sensors})",
        output_png_path=str(output_dir / "error_heatmap.png"),
    )


def export_metrics_csv(
    *,
    output_dir: Path,
    result: GeneticAlgorithmResult,
    visualization_settings: VisualizationSettings,
) -> None:
    if not visualization_settings.export_metrics_csv:
        return

    export_generation_metrics_to_csv(
        generation_metrics=result.generation_metrics,
        output_csv_path=str(output_dir / "generation_metrics.csv"),
    )


def postprocess_results(
    *,
    number_of_sensors: int,
    result: GeneticAlgorithmResult,
    environment_settings: EnvironmentSettings,
    simulation_settings: SimulationSettings,
    sound_speed_profile: SoundSpeedProfile,
    visualization_settings: VisualizationSettings,
) -> None:
    logger = logging.getLogger(LOGGER_NAME)

    output_dir = ensure_output_dir(visualization_settings.output_directory, number_of_sensors)
    grid_geometry = GridGeometry(environment_settings)

    save_best_per_generation_figures(
        output_dir=output_dir,
        number_of_sensors=number_of_sensors,
        result=result,
        grid_geometry=grid_geometry,
        environment_settings=environment_settings,
        visualization_settings=visualization_settings,
    )

    save_final_best_figure(
        output_dir=output_dir,
        number_of_sensors=number_of_sensors,
        result=result,
        environment_settings=environment_settings,
        visualization_settings=visualization_settings,
    )

    save_progress_figures(
        output_dir=output_dir,
        number_of_sensors=number_of_sensors,
        result=result,
        visualization_settings=visualization_settings,
    )

    save_error_heatmap(
        output_dir=output_dir,
        number_of_sensors=number_of_sensors,
        result=result,
        environment_settings=environment_settings,
        simulation_settings=simulation_settings,
        sound_speed_profile=sound_speed_profile,
        visualization_settings=visualization_settings,
    )

    export_metrics_csv(
        output_dir=output_dir,
        result=result,
        visualization_settings=visualization_settings,
    )

    # Baseline comparison is nice-to-have; do not break the pipeline.
    try:
        save_baseline_comparison(
            output_dir=output_dir,
            number_of_sensors=number_of_sensors,
            result=result,
            grid_geometry=grid_geometry,
            environment_settings=environment_settings,
            simulation_settings=simulation_settings,
            sound_speed_profile=sound_speed_profile,
        )
    except Exception:
        logger.exception("Baseline comparison failed for N=%d", number_of_sensors)
