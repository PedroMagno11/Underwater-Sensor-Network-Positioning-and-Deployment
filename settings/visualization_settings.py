from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class VisualizationSettings:
    """Configuration for image/CSV outputs."""

    output_directory: str = "outputs"

    # Save a scenario figure for the best chromosome of each generation
    save_best_per_generation_figures: bool = True

    # Maximum number of best-per-generation figures to save.
    # If you run thousands of generations you may want to cap it.
    max_generation_figures: int = 10_000

    # Save final best topology figure
    save_final_best_figure: bool = True

    # Save GA progress figures (cost curves, coverage/error curves)
    save_progress_figures: bool = True

    # Export metrics to CSV
    export_metrics_csv: bool = True

    # Heatmap options
    save_error_heatmap: bool = True
    heatmap_step_in_meters: float = 90.0  # sampling step inside the target region

