from __future__ import annotations

import json
from dataclasses import asdict
from pathlib import Path
from typing import Any, Dict, Tuple

from settings.environment_settings import EnvironmentSettings
from settings.genetic_algorithm_settings import GeneticAlgorithmSettings
from settings.simulation_settings import SimulationSettings
from settings.performance_settings import PerformanceSettings
from settings.visualization_settings import VisualizationSettings

# NEW
from settings.particle_swarm_settings import ParticleSwarmSettings


def load_json_config(path: str) -> Dict[str, Any]:
    p = Path(path)
    with p.open("r", encoding="utf-8") as f:
        return json.load(f)


def _dataclass_from_dict(cls, data: Dict[str, Any]):
    return cls(**data)


def load_settings_from_config(config: Dict[str, Any]) -> Tuple[
    EnvironmentSettings,
    GeneticAlgorithmSettings,
    SimulationSettings,
    PerformanceSettings,
    VisualizationSettings,
    ParticleSwarmSettings,   # NEW
]:
    env = _dataclass_from_dict(EnvironmentSettings, config.get("environment", {}))
    ga = _dataclass_from_dict(GeneticAlgorithmSettings, config.get("genetic_algorithm", {}))
    sim = _dataclass_from_dict(SimulationSettings, config.get("simulation", {}))
    perf = _dataclass_from_dict(PerformanceSettings, config.get("performance", {}))
    viz = _dataclass_from_dict(VisualizationSettings, config.get("visualization", {}))

    # NEW
    pso = _dataclass_from_dict(ParticleSwarmSettings, config.get("particle_swarm", {}))

    return env, ga, sim, perf, viz, pso


def write_default_config(path: str) -> None:
    default = {
        "environment": asdict(EnvironmentSettings()),
        "genetic_algorithm": asdict(GeneticAlgorithmSettings()),
        "simulation": asdict(SimulationSettings()),
        "performance": asdict(PerformanceSettings()),
        "visualization": asdict(VisualizationSettings()),
        # NEW
        "particle_swarm": asdict(ParticleSwarmSettings()),
    }
    Path(path).write_text(json.dumps(default, indent=2), encoding="utf-8")