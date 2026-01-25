from __future__ import annotations

import csv
from typing import List

from results.result_models import GenerationMetrics


def export_generation_metrics_to_csv(
    generation_metrics: List[GenerationMetrics],
    output_csv_path: str,
) -> None:
    fieldnames = [
        "generation_index",
        "cost_min",
        "cost_avg",
        "cost_median",
        "cost_p90",
        "avg_no_coverage_rate",
        "avg_error_meters",
        "best_global_cost",
    ]

    with open(output_csv_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        for m in generation_metrics:
            writer.writerow(
                {
                    "generation_index": m.generation_index,
                    "cost_min": m.cost_min,
                    "cost_avg": m.cost_avg,
                    "cost_median": m.cost_median,
                    "cost_p90": m.cost_p90,
                    "avg_no_coverage_rate": m.avg_no_coverage_rate,
                    "avg_error_meters": m.avg_error_meters,
                    "best_global_cost": m.best_global_cost,
                }
            )
