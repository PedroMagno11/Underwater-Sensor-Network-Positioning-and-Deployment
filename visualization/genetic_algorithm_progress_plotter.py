from __future__ import annotations

from typing import List
import matplotlib.pyplot as plt

from results.result_models import GenerationMetrics


def save_ga_progress_figure(
    metrics: List[GenerationMetrics],
    title: str,
    output_png_path: str
) -> None:
    generations = [m.generation_index for m in metrics]
    cost_min = [m.cost_min for m in metrics]
    cost_avg = [m.cost_avg for m in metrics]
    cost_median = [m.cost_median for m in metrics]
    cost_p90 = [m.cost_p90 for m in metrics]
    best_global = [m.best_global_cost for m in metrics]

    fig, ax = plt.subplots(figsize=(10, 5))
    ax.plot(generations, cost_min, label="min")
    ax.plot(generations, cost_avg, label="avg")
    ax.plot(generations, cost_median, label="median")
    ax.plot(generations, cost_p90, label="p90")
    ax.plot(generations, best_global, label="best_global", linewidth=2)

    ax.set_title(title)
    ax.set_xlabel("generation")
    ax.set_ylabel("cost")
    ax.grid(True, alpha=0.3)
    ax.legend()

    fig.tight_layout()
    fig.savefig(output_png_path, dpi=200)
    plt.close(fig)


def save_ga_coverage_figure(
    metrics: List[GenerationMetrics],
    title: str,
    output_png_path: str
) -> None:
    generations = [m.generation_index for m in metrics]
    no_coverage_rate = [m.avg_no_coverage_rate for m in metrics]
    avg_error = [m.avg_error_meters for m in metrics]

    fig, ax = plt.subplots(figsize=(10, 5))
    ax.plot(generations, no_coverage_rate, label="avg_no_coverage_rate")
    ax.plot(generations, avg_error, label="avg_error_meters")

    ax.set_title(title)
    ax.set_xlabel("generation")
    ax.set_ylabel("value")
    ax.grid(True, alpha=0.3)
    ax.legend()

    fig.tight_layout()
    fig.savefig(output_png_path, dpi=200)
    plt.close(fig)
