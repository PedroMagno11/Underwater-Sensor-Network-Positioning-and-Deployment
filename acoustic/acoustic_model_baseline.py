from __future__ import annotations
from settings.model import AcousticSensor
from geometry.distance_functions import calculate_distance_3d
from acoustic.sound_speed_profile import SoundSpeedProfile

def calculate_arrival_time(
        sensor: AcousticSensor,
        impact_position_x: float,
        impact_position_y: float,
        sound_speed_profile: SoundSpeedProfile,
)-> float:
    distance = calculate_distance_3d(sensor.position_x, sensor.position_y, sensor.depth, impact_position_x, impact_position_y, 0.0)

    average_depth = 0.5 * sensor.depth
    approximate_speed = sound_speed_profile.sound_speed(average_depth)

    return distance / approximate_speed