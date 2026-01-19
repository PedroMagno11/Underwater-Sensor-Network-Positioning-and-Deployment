from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class EvaluationReport:
    """Structured diagnostics returned by the cost function.

    This enables detailed logging per generation without flooding the output.
    """

    total_cost: float
    mean_localization_error_meters: float
    number_of_impacts: int
    number_of_localizable_impacts: int
    number_of_impacts_without_coverage: int
    coverage_penalty: float
    separation_penalty: float
