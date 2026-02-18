from __future__ import annotations

from dataclasses import dataclass
from typing import List, Optional, Tuple, Callable

import numpy as np

from settings.model import AcousticSensor


@dataclass(frozen=True)
class GenerationMetrics:
    generation_index: int
    cost_min: float
    cost_avg: float
    cost_median: float
    cost_p90: float
    avg_no_coverage_rate: float
    avg_error_meters: float
    best_global_cost: float


@dataclass(frozen=True)
class TopologyResult:
    label: str
    number_of_sensors: int
    sensors: List[AcousticSensor]
    mean_error_meters: float
    no_coverage_rate: float
    total_cost: float
    notes: Optional[str] = None


@dataclass(frozen=True)
class GeneticAlgorithmResult:
    number_of_sensors: int
    best_chromosome: np.ndarray
    best_cost: float
    best_sensors: List[AcousticSensor]
    generation_metrics: List[GenerationMetrics]
    best_chromosomes_per_generation: List[np.ndarray]
    global_seed: int


ImpactCallback = Callable[[int, int, List[Tuple[float, float]]], None]
