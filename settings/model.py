from __future__ import annotations
from dataclasses import dataclass

@dataclass(frozen=True)
class AcousticSensor:
    """
    Acoustic Sensor is a underwater acoustic sensor, like a buoy that has a hydrophone
    """
    position_x: float
    position_y: float
    depth: float