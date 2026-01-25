from __future__ import annotations
from typing import List, Tuple
import numpy as np

from settings.model import AcousticSensor
from settings.simulation_settings import SimulationSettings
from geometry.grid_geometry import GridGeometry
from acoustic.acoustic_model_baseline import calculate_arrival_time
from acoustic.sound_speed_profile import SoundSpeedProfile

def estimate_impact_position(sensors: List[AcousticSensor], observed_times: np.ndarray,
                             grid_geometry: GridGeometry, sound_speed_profile: SoundSpeedProfile,
                             simulation_settings: SimulationSettings) -> Tuple[float, float]:

    def calculate_cost_for_position(impact_position_x: float, impact_position_y: float) -> float:
        theoretical_times = np.array([
            calculate_arrival_time(sensor, impact_position_x, impact_position_y, sound_speed_profile) for sensor in sensors
        ], dtype=float)

        estimated_emission_time = float(np.mean(observed_times - theoretical_times))
        residue = observed_times - (estimated_emission_time + theoretical_times)
        return float(np.sum(residue * residue))


    best_position_x = grid_geometry.environment_settings.x_center_in_meters
    best_position_y = grid_geometry.environment_settings.y_center_in_meters
    best_cost = float("inf")

    target_radius = grid_geometry.environment_settings.target_region_radius
    coarse_step = simulation_settings.coarse_search_step_in_meters

    limit = target_radius
    values = np.arange(-limit, limit + 1e-9, coarse_step)

    center_x = grid_geometry.environment_settings.x_center_in_meters
    center_y = grid_geometry.environment_settings.y_center_in_meters

    for dx in values:
        for dy in values:
            if dx * dx + dy * dy > target_radius * target_radius:
                continue

            impact_x = center_x + float(dx)
            impact_y = center_y + float(dy)
            cost = calculate_cost_for_position(impact_x, impact_y)
            if cost < best_cost:
                best_cost = cost
                best_position_x, best_position_y = impact_x, impact_y

    fine_step = simulation_settings.fine_search_step_in_meters
    refinement_radius = simulation_settings.refinement_radius_in_meters

    refined_values_x = np.arange(best_position_x - refinement_radius, best_position_x + refinement_radius + 1e-9, fine_step)
    refined_values_y = np.arange(best_position_y - refinement_radius, best_position_y + refinement_radius + 1e-9, fine_step)

    refined_best_cost = best_cost

    best_refined_position_x, best_refined_position_y = best_position_x, best_position_y

    for refined_impact_position_x in refined_values_x:
        for refined_impact_position_y in refined_values_y:
            dx = refined_impact_position_x - center_x
            dy = refined_impact_position_y - center_y
            if dx * dx + dy * dy > target_radius * target_radius:
                continue

            cost = calculate_cost_for_position(float(refined_impact_position_x), float(refined_impact_position_y))
            if cost < refined_best_cost:
                refined_best_cost = cost
                best_refined_position_x, best_refined_position_y = float(refined_impact_position_x), float(refined_impact_position_y)

    quantized_x_position, quantized_y_position = grid_geometry.quantize_for_grid_point(best_refined_position_x, best_refined_position_y)
    return quantized_x_position, quantized_y_position