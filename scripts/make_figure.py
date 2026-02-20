from __future__ import annotations

from pathlib import Path
from typing import Optional

import numpy as np
import matplotlib.pyplot as plt

from geometry.grid_geometry import GridGeometry
from evaluation.chromosome_decoder import chromosome_converter
from topologies.regular_polygon import create_regular_polygon_chromosome


def _load_npy(path: Path, expected_ndim: Optional[int] = None) -> np.ndarray:
    if not path.exists():
        raise FileNotFoundError(f"Missing file: {path}")
    arr = np.load(path)
    if expected_ndim is not None and arr.ndim != expected_ndim:
        raise ValueError(f"Invalid array ndim for {path}: got {arr.ndim}, expected {expected_ndim}")
    return np.asarray(arr, dtype=float)


def load_impacts(impacts_dir: Path, gen: int) -> np.ndarray:
    p = impacts_dir / f"impact_points_gen_{gen:04d}.npy"
    arr = _load_npy(p, expected_ndim=2)
    if arr.shape[1] != 2:
        raise ValueError(f"Invalid impacts shape in {p}: {arr.shape} (expected (M,2))")
    return arr


def load_best_chromosomes(outdir: Path) -> np.ndarray:
    # (G, 3*N)
    p = outdir / "best_chromosomes_per_generation.npy"
    arr = _load_npy(p, expected_ndim=2)
    return arr


def load_best_global(outdir: Path) -> np.ndarray:
    # (3*N,)
    p = outdir / "best_global_chromosome.npy"
    arr = _load_npy(p, expected_ndim=1)
    return arr


def draw_target_circle(ax, cx: float, cy: float, R: float) -> None:
    t = np.linspace(0.0, 2.0 * np.pi, 500)
    ax.plot(cx + R * np.cos(t), cy + R * np.sin(t), linewidth=1.8, label="Target region")


def _set_fixed_axes(ax, cx: float, cy: float, R: float, margin: float = 250.0) -> None:
    lim = R + margin
    ax.set_xlim(cx - lim, cx + lim)
    ax.set_ylim(cy - lim, cy + lim)


def plot_scene(
    *,
    out_png: Path,
    title: str,
    impacts_xy: np.ndarray,  # (M,2)
    sensors,                # list[AcousticSensor]
    env,
    show_detection: bool = True,
    show_depth_color: bool = True,
) -> None:
    cx = float(env.x_center_in_meters)
    cy = float(env.y_center_in_meters)
    R = float(env.target_region_radius)
    max_det = float(env.maximum_detection_distance)

    fig = plt.figure()
    ax = plt.gca()

    # área alvo
    draw_target_circle(ax, cx, cy, R)

    # impactos
    ax.scatter(impacts_xy[:, 0], impacts_xy[:, 1], s=14, label="Impact points")

    # boias
    sx = np.array([s.position_x for s in sensors], dtype=float)
    sy = np.array([s.position_y for s in sensors], dtype=float)
    sz = np.array([float(getattr(s, "depth", 0.0)) for s in sensors], dtype=float)

    if show_depth_color:
        # cor = profundidade (m)
        sc = ax.scatter(sx, sy, s=70, marker="^", c=sz, label="Sensors")
        cbar = plt.colorbar(sc, ax=ax)
        cbar.set_label("Depth (m)")
    else:
        ax.scatter(sx, sy, s=70, marker="^", label="Sensors")

    # índice do sensor
    for i, (x, y) in enumerate(zip(sx, sy)):
        ax.text(x, y, f"{i}", fontsize=8, ha="center", va="center")

    # círculos de detecção
    if show_detection:
        t = np.linspace(0.0, 2.0 * np.pi, 250)
        for x, y in zip(sx, sy):
            ax.plot(x + max_det * np.cos(t), y + max_det * np.sin(t), linewidth=0.8, alpha=0.25)

    _set_fixed_axes(ax, cx, cy, R)

    ax.set_aspect("equal", adjustable="box")
    ax.grid(True, alpha=0.3)
    ax.set_xlabel("x (m)")
    ax.set_ylabel("y (m)")
    ax.set_title(title)
    ax.legend(loc="best")

    out_png.parent.mkdir(parents=True, exist_ok=True)
    plt.savefig(out_png, dpi=300, bbox_inches="tight")
    plt.close(fig)


def main(
    *,
    number_of_sensors: int,
    generation_to_plot: int,
    output_root: str,
    environment_settings,
) -> None:
    outdir = Path(output_root) / f"sensors_{number_of_sensors}"
    impacts_dir = outdir / "impacts"
    figs_dir = outdir / "figures"

    grid = GridGeometry(environment_settings)

    impacts = load_impacts(impacts_dir, generation_to_plot)

    best_global_chr = load_best_global(outdir)
    best_chrs = load_best_chromosomes(outdir)

    if generation_to_plot < 0 or generation_to_plot >= best_chrs.shape[0]:
        raise ValueError(f"generation_to_plot={generation_to_plot} out of range [0, {best_chrs.shape[0]-1}]")

    best_gen_chr = best_chrs[generation_to_plot]

    # decode sensores (GA)
    ga_sensors_global = chromosome_converter(best_global_chr, number_of_sensors, grid, environment_settings)
    ga_sensors_gen = chromosome_converter(best_gen_chr, number_of_sensors, grid, environment_settings)

    # baseline polygon (mesma profundidade média)
    polygon_chr = create_regular_polygon_chromosome(
        number_of_sensors=number_of_sensors,
        environment_settings=environment_settings,
        grid_geometry=grid,
        polygon_radius_meters=environment_settings.target_region_radius,
        depth_meters=(environment_settings.minimum_depth_in_meters + environment_settings.maximum_depth_in_meters) / 2.0,
        angle_offset_degrees=0.0,
    )
    poly_sensors = chromosome_converter(polygon_chr, number_of_sensors, grid, environment_settings)

    plot_scene(
        out_png=figs_dir / f"scene_ga_best_global_gen_{generation_to_plot:04d}.png",
        title=f"GA Best (Global) — Gen {generation_to_plot}",
        impacts_xy=impacts,
        sensors=ga_sensors_global,
        env=environment_settings,
        show_detection=True,
        show_depth_color=True,
    )

    plot_scene(
        out_png=figs_dir / f"scene_ga_best_gen_{generation_to_plot:04d}.png",
        title=f"GA Best (Generation) — Gen {generation_to_plot}",
        impacts_xy=impacts,
        sensors=ga_sensors_gen,
        env=environment_settings,
        show_detection=True,
        show_depth_color=True,
    )

    plot_scene(
        out_png=figs_dir / f"scene_polygon_gen_{generation_to_plot:04d}.png",
        title=f"Regular Polygon — Gen {generation_to_plot}",
        impacts_xy=impacts,
        sensors=poly_sensors,
        env=environment_settings,
        show_detection=True,
        show_depth_color=True,
    )

    print(f"[OK] Saved figures to: {figs_dir}")


if __name__ == "__main__":
    # Exemplo: substitua pelo seu EnvironmentSettings real
    from settings.environment_settings import EnvironmentSettings

    env = EnvironmentSettings()
    main(
        number_of_sensors=8,
        generation_to_plot=199,
        output_root="outputs",
        environment_settings=env,
    )
