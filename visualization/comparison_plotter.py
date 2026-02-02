from __future__ import annotations
import matplotlib
matplotlib.use("Agg", force=True)
import matplotlib.pyplot as plt
from typing import Dict, List
from results.result_models import TopologyResult

def save_topology_comparison_figures(
        results: List[TopologyResult],
        title_prefix: str,
        output_png_path_prefix: str
) -> None:
    """
    Creates paper-ready comparison figures:
    1) mean localization error (m)
    2) no coverage rate
    3) total cost (optional but useful)
    """

    labels = [r.label for r in results]
    mean_errors = [r.mean_error_meters for r in results]
    no_coverage_rate = [r.no_coverage_rate for r in results]
    total_costs = [r.total_cost for r in results]

    # --- Figure 1: Mean localization error (m) ---
    fig1, ax1 = plt.subplots(figsize=(10, 5))
    ax1.bar(labels, mean_errors)
    ax1.set_title(f"{title_prefix} - Mean Localization Error")
    ax1.set_xlabel("Topology")
    ax1.set_ylabel("Mean error (meters)")
    ax1.grid(True, axis="y", alpha=0.3)
    fig1.tight_layout()
    fig1.savefig(f"{output_png_path_prefix}_mean_error.png", dpi=300)
    plt.close(fig1)

    # --- Figure 2: No coverage rate
    fig2, ax2 = plt.subplots(figsize=(10, 5))
    ax2.bar(labels, no_coverage_rate)
    ax2.set_title(f"{title_prefix} — No Coverage Rate")
    ax2.set_xlabel("Topology")
    ax2.set_ylabel("No coverage rate (fraction)")
    ax2.set_ylim(0.0, 1.0)
    ax2.grid(True, axis="y", alpha=0.3)
    fig2.tight_layout()
    fig2.savefig(f"{output_png_path_prefix}_no_coverage.png", dpi=300)
    plt.close(fig2)

    # --- Figure 3: Total cost
    fig3, ax3 = plt.subplots(figsize=(10, 5))
    ax3.bar(labels, total_costs)
    ax3.set_title(f"{title_prefix} — Total Cost")
    ax3.set_xlabel("Topology")
    ax3.set_ylabel("Total cost (lower is better)")
    ax3.grid(True, axis="y", alpha=0.3)
    fig3.tight_layout()
    fig3.savefig(f"{output_png_path_prefix}_total_cost.png", dpi=300)
    plt.close(fig3)

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

def save_topology_comparison_figures(
    results: List[TopologyResult],
    title_prefix: str,
    output_png_path_prefix: str
) -> None:
    """
    Creates paper-ready comparison figures:
      1) mean localization error (m)
      2) no coverage rate
      3) total cost (optional but useful)
    """

    labels = [r.label for r in results]
    mean_errors = [r.mean_error_meters for r in results]
    no_coverage_rates = [r.no_coverage_rate for r in results]
    total_costs = [r.total_cost for r in results]

    # --- Figure 1: Mean localization error (m)
    fig1, ax1 = plt.subplots(figsize=(10, 5))
    ax1.bar(labels, mean_errors)
    ax1.set_title(f"{title_prefix} — Mean Localization Error")
    ax1.set_xlabel("Topology")
    ax1.set_ylabel("Mean error (meters)")
    ax1.grid(True, axis="y", alpha=0.3)
    fig1.tight_layout()
    fig1.savefig(f"{output_png_path_prefix}_mean_error.png", dpi=200)
    plt.close(fig1)

    # --- Figure 2: No coverage rate
    fig2, ax2 = plt.subplots(figsize=(10, 5))
    ax2.bar(labels, no_coverage_rates)
    ax2.set_title(f"{title_prefix} — No Coverage Rate")
    ax2.set_xlabel("Topology")
    ax2.set_ylabel("No coverage rate (fraction)")
    ax2.set_ylim(0.0, 1.0)
    ax2.grid(True, axis="y", alpha=0.3)
    fig2.tight_layout()
    fig2.savefig(f"{output_png_path_prefix}_no_coverage.png", dpi=200)
    plt.close(fig2)

    # --- Figure 3: Total cost
    fig3, ax3 = plt.subplots(figsize=(10, 5))
    ax3.bar(labels, total_costs)
    ax3.set_title(f"{title_prefix} — Total Cost")
    ax3.set_xlabel("Topology")
    ax3.set_ylabel("Total cost (lower is better)")
    ax3.grid(True, axis="y", alpha=0.3)
    fig3.tight_layout()
    fig3.savefig(f"{output_png_path_prefix}_total_cost.png", dpi=200)
    plt.close(fig3)