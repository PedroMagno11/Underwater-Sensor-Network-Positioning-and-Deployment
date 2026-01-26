from __future__ import annotations
import matplotlib
matplotlib.use("Agg", force=True)
import matplotlib.pyplot as plt
from typing import List, Tuple
import random
import numpy as np
from settings.environment_settings import EnvironmentSettings
from settings.simulation_settings import SimulationSettings
from settings.model import AcousticSensor
from geometry.distance_functions import calculate_distance_2d
from geometry.grid_geometry import GridGeometry
from acoustic.sound_speed_profile import SoundSpeedProfile
from acoustic.acoustic_model_baseline import calculate_arrival_time
from localization.mle_estimator import estimate_impact_position


def generate_error_heatmap_samples(
    sensors: List[AcousticSensor],
    environment_settings: EnvironmentSettings,
    simulation_settings: SimulationSettings,
    sound_speed_profile: SoundSpeedProfile,
    step_in_meters: float,
    random_seed: int = 0,
) -> Tuple[np.ndarray, np.ndarray, np.ndarray, np.ndarray]:
    """Generate (x, y, error, coverage) samples inside the target region."""

    grid_geometry = GridGeometry(environment_settings)

    center_x = environment_settings.x_center_in_meters
    center_y = environment_settings.y_center_in_meters
    radius = environment_settings.target_region_radius

    x_min = center_x - radius
    x_max = center_x + radius
    y_min = center_y - radius
    y_max = center_y + radius

    xs: List[float] = []
    ys: List[float] = []
    errors: List[float] = []
    coverages: List[float] = []

    rng = random.Random(random_seed)

    x = x_min
    while x <= x_max:
        y = y_min
        while y <= y_max:
            if calculate_distance_2d(x, y, center_x, center_y) <= radius:
                sensor_detection_indices: List[int] = []
                for idx, sensor in enumerate(sensors):
                    d2d = calculate_distance_2d(sensor.position_x, sensor.position_y, x, y)
                    if d2d <= environment_settings.maximum_detection_distance:
                        sensor_detection_indices.append(idx)

                if len(sensor_detection_indices) < 3:
                    xs.append(x)
                    ys.append(y)
                    errors.append(float("nan"))
                    coverages.append(0.0)
                else:
                    sensors_that_detected = [sensors[i] for i in sensor_detection_indices]

                    theoretical_times = np.array(
                        [
                            calculate_arrival_time(s, x, y, sound_speed_profile)
                            for s in sensors_that_detected
                        ],
                        dtype=float,
                    )

                    # one noise sample per location by default
                    noise = np.array(
                        [
                            rng.gauss(0.0, simulation_settings.time_noise_standard_deviation)
                            for _ in range(len(sensors_that_detected))
                        ],
                        dtype=float,
                    )

                    observed_times = theoretical_times + noise

                    est_x, est_y = estimate_impact_position(
                        sensors_that_detected,
                        observed_times,
                        grid_geometry,
                        sound_speed_profile,
                        simulation_settings,
                    )

                    err = calculate_distance_2d(est_x, est_y, x, y)

                    xs.append(x)
                    ys.append(y)
                    errors.append(float(err))
                    coverages.append(1.0)
            y += step_in_meters
        x += step_in_meters

    return (
        np.array(xs, dtype=float),
        np.array(ys, dtype=float),
        np.array(errors, dtype=float),
        np.array(coverages, dtype=float),
    )


def save_error_heatmap_figure(
    sensors: List[AcousticSensor],
    environment_settings: EnvironmentSettings,
    simulation_settings: SimulationSettings,
    sound_speed_profile: SoundSpeedProfile,
    step_in_meters: float,
    title: str,
    output_png_path: str,
) -> None:
    x, y, errors, coverages = generate_error_heatmap_samples(
        sensors=sensors,
        environment_settings=environment_settings,
        simulation_settings=simulation_settings,
        sound_speed_profile=sound_speed_profile,
        step_in_meters=step_in_meters,
    )

    fig, ax = plt.subplots(figsize=(8, 8))

    valid = np.isfinite(errors)
    if np.any(valid):
        sc = ax.scatter(x[valid], y[valid], c=errors[valid], s=35)
        fig.colorbar(sc, ax=ax, label="localization error (m)")

    invalid = ~valid
    if np.any(invalid):
        ax.scatter(x[invalid], y[invalid], marker="x", s=18, alpha=0.4)

    # Overlay sensor positions
    ax.scatter([s.position_x for s in sensors], [s.position_y for s in sensors], s=80)

    ax.set_title(title)
    ax.set_xlabel("x (meters)")
    ax.set_ylabel("y (meters)")
    ax.set_aspect("equal", adjustable="box")
    ax.grid(True, alpha=0.3)

    fig.tight_layout()
    fig.savefig(output_png_path, dpi=200)
    plt.close(fig)
