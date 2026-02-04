from __future__ import annotations

from dataclasses import dataclass
from typing import List, Tuple


@dataclass(frozen=True)
class EvaluationReport:
    total_cost: float
    mean_localization_error_meters: float
    number_of_impacts: int
    number_of_localizable_impacts: int
    number_of_impacts_without_coverage: int
    coverage_penalty: float
    separation_penalty: float
    impact_points: List[Tuple[float, float]]
