from __future__ import annotations

from typing import List, Optional

import matplotlib.pyplot as plt
from matplotlib.patches import Circle

from settings.environment_settings import EnvironmentSettings
from settings.model import AcousticSensor


def save_scenario_figure(
    sensors: List[AcousticSensor],
    environment_settings: EnvironmentSettings,
    title: str,
    output_png_path: str,
    show_detection_range: bool = True,
    show_target_region: bool = True,
    annotation_text: Optional[str] = None,
) -> None:
    fig, ax = plt.subplots(figsize=(8, 8))

    center_x = environment_settings.x_center_in_meters
    center_y = environment_settings.y_center_in_meters

    if show_target_region:
        target_circle = Circle(
            (center_x, center_y),
            environment_settings.target_region_radius,
            fill=False,
            linewidth=2,
        )
        ax.add_patch(target_circle)

    if show_detection_range:
        for sensor in sensors:
            detection_circle = Circle(
                (sensor.position_x, sensor.position_y),
                environment_settings.maximum_detection_distance,
                fill=False,
                linewidth=1,
                alpha=0.3,
            )
            ax.add_patch(detection_circle)

    xs = [s.position_x for s in sensors]
    ys = [s.position_y for s in sensors]
    ax.scatter(xs, ys, s=80)

    for i, s in enumerate(sensors, start=1):
        ax.annotate(
            f"S{i}\nz={s.depth:.1f}m",
            (s.position_x, s.position_y),
            textcoords="offset points",
            xytext=(6, 6),
            fontsize=9,
        )

    ax.scatter([center_x], [center_y], marker="x", s=80)
    ax.set_title(title)
    ax.set_xlabel("x (meters)")
    ax.set_ylabel("y (meters)")
    ax.set_aspect("equal", adjustable="box")
    ax.grid(True, alpha=0.3)

    r = environment_settings.target_region_radius
    margin = 0.2 * r
    ax.set_xlim(center_x - r - margin, center_x + r + margin)
    ax.set_ylim(center_y - r - margin, center_y + r + margin)

    if annotation_text:
        ax.text(
            0.02,
            0.02,
            annotation_text,
            transform=ax.transAxes,
            fontsize=10,
            verticalalignment="bottom",
            bbox=dict(boxstyle="round", alpha=0.15),
        )

    fig.tight_layout()
    fig.savefig(output_png_path, dpi=200)
    plt.close(fig)
