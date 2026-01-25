from __future__ import annotations

from typing import Dict

import matplotlib.pyplot as plt


def save_topology_comparison_bar_chart(
    title: str,
    metrics_by_label: Dict[str, Dict[str, float]],
    output_png_path: str,
) -> None:
    """Save a simple bar chart comparing topologies.

    metrics_by_label = {
        "GA_best": {"total_cost": ..., "mean_error_meters": ..., "no_coverage_rate": ...},
        "Regular_polygon": {...}
    }
    """

    labels = list(metrics_by_label.keys())
    total_costs = [metrics_by_label[l]["total_cost"] for l in labels]
    mean_errors = [metrics_by_label[l]["mean_error_meters"] for l in labels]
    no_cov = [metrics_by_label[l]["no_coverage_rate"] for l in labels]

    x = range(len(labels))

    fig, ax = plt.subplots(figsize=(10, 5))
    ax.bar([i - 0.25 for i in x], total_costs, width=0.25, label="total_cost")
    ax.bar([i for i in x], mean_errors, width=0.25, label="mean_error_meters")
    ax.bar([i + 0.25 for i in x], no_cov, width=0.25, label="no_coverage_rate")

    ax.set_xticks(list(x))
    ax.set_xticklabels(labels, rotation=15)
    ax.set_title(title)
    ax.grid(True, alpha=0.3)
    ax.legend()

    fig.tight_layout()
    fig.savefig(output_png_path, dpi=200)
    plt.close(fig)
