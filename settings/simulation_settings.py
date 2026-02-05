from __future__ import annotations
from dataclasses import dataclass

@dataclass(frozen=True)
class SimulationSettings:
    number_of_impact_points_per_evaluation: int = 80
    time_noise_standard_deviation: float = 0.0005  # 0.5 ms

    coarse_search_step_in_meters: float = 45.0  # 5 * 9m
    fine_search_step_in_meters: float = 9.0
    refinement_radius_in_meters: float = 180.0  # 20 * 9m

    invalid_coverage_penalty: float = 1e6
    penalty_for_buoys_too_close: float = 1.0
    minimum_distance_between_buoys_in_meters: float = 100.0

    localization_tolerance_meters = 15.0