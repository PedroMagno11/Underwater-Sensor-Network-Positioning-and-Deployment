from __future__ import annotations
import matplotlib
matplotlib.use("Agg", force=True)
import matplotlib.pyplot as plt
from typing import List, Optional
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
    fig, ax = plt.subplots(figsize=(9, 9))

    center_x = environment_settings.x_center_in_meters
    center_y = environment_settings.y_center_in_meters
    target_radius = environment_settings.target_region_radius

    if show_target_region:
        target_circle = Circle(
            (center_x, center_y),
            target_radius,
            fill=False,
            linewidth=2,
            edgecolor="black",
            zorder=1
        )
        ax.add_patch(target_circle)

    # Detection range
    if show_detection_range:
        for sensor in sensors:
            detection_circle = Circle(
                (sensor.position_x, sensor.position_y),
                environment_settings.maximum_detection_distance,
                fill=False,
                linewidth=0.8,
                linestyle="--",
                alpha=0.5,
                edgecolor="gray",
                zorder=0
            )
            ax.add_patch(detection_circle)

    for idx, sensor in enumerate(sensors, start=1):
        ax.scatter(
            sensor.position_x,
            sensor.position_y,
            s=120,
            marker="o",
            zorder=5
        )
        ax.annotate(
            f"Sensor {idx}\nz={sensor.depth:.1f} meters",
            xy=(sensor.position_x, sensor.position_y),
            textcoords="offset points",
            xytext=(8,8),
            fontsize=10,
            zorder=6,
            bbox=dict(boxstyle="round", fc="white", alpha=0.8),
        )

    # Target center
    ax.scatter(
        [center_x],
        [center_y],
        marker="x",
        s=120,
        color="black",
        zorder=6
    )

    # Smart Zoom: include all sensors + target region
    xs = [sensor.position_x for sensor in sensors] + [center_x]
    ys = [sensor.position_y for sensor in sensors] + [center_y]

    min_x, max_x = min(xs), max(xs)
    min_y, max_y = min(ys), max(ys)

    margin = 0.15 * max(target_radius, max(max_x - min_x, max_y - min_y))
    ax.set_xlim(min_x - margin, max_x + margin)
    ax.set_ylim(min_y - margin, max_y + margin)

    ax.set_title(title)
    ax.set_xlabel("x (meters)")
    ax.set_ylabel("y (meters)")
    ax.set_aspect("equal", adjustable="box")
    ax.grid(True, alpha=0.3)

    # Annotation box
    if annotation_text:
        ax.text(
            0.02,
            0.02,
            annotation_text,
            transform=ax.transAxes,
            fontsize=10,
            verticalalignment="bottom",
            bbox=dict(boxstyle="round", fc="white", alpha=0.85),
            zorder=10,
        )

    fig.tight_layout()
    fig.savefig(output_png_path, dpi=200)
    plt.close(fig)
