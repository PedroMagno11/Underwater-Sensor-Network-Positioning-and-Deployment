from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import List, Tuple, Optional

import numpy as np
import matplotlib.pyplot as plt

from geometry.grid_geometry import GridGeometry
from evaluation.chromosome_decoder import chromosome_converter
from topologies.regular_polygon import create_regular_polygon_chromosome
from settings.environment_settings import EnvironmentSettings
from settings.simulation_settings import SimulationSettings


def _draw_target_circle(ax, cx: float, cy: float, R: float) -> None:
    t = np.linspace(0.0, 2.0 * np.pi, 500)
    ax.plot(cx + R * np.cos(t), cy + R * np.sin(t), linewidth=1.8, label="Target region")


def _draw_detection_circles(ax, sx: np.ndarray, sy: np.ndarray, r: float) -> None:
    t = np.linspace(0.0, 2.0 * np.pi, 250)
    for x, y in zip(sx, sy):
        ax.plot(x + r * np.cos(t), y + r * np.sin(t), linewidth=0.8, alpha=0.25)


def _load_impacts(impacts_dir: Path, gen: int) -> np.ndarray:
    p = impacts_dir / f"impact_points_gen_{gen:04d}.npy"
    if not p.exists():
        raise FileNotFoundError(f"Impact file not found: {p}")
    arr = np.load(p)
    if arr.ndim != 2 or arr.shape[1] != 2:
        raise ValueError(f"Invalid impacts array shape: {arr.shape} (expected (M,2))")
    return arr.astype(float)


def _load_best_chromosomes_per_generation(outdir: Path) -> np.ndarray:
    p = outdir / "best_chromosomes_per_generation.npy"
    if not p.exists():
        raise FileNotFoundError(
            f"Missing {p}. Save it at the end of the GA run."
        )
    arr = np.load(p)
    return np.asarray(arr, dtype=float)


def _load_best_global(outdir: Path) -> np.ndarray:
    p = outdir / "best_global_chromosome.npy"
    if not p.exists():
        raise FileNotFoundError(
            f"Missing {p}. Save it at the end of the GA run."
        )
    return np.asarray(np.load(p), dtype=float)


def plot_scene(
    *,
    out_png: Path,
    title: str,
    impacts_xy: np.ndarray,  # (M,2)
    sensors,                # List[AcousticSensor]
    env: EnvironmentSettings,
    show_detection: bool = True,
) -> None:
    cx = float(env.x_center_in_meters)
    cy = float(env.y_center_in_meters)
    R = float(env.target_region_radius)
    max_det = float(env.maximum_detection_distance)

    sx = np.array([s.position_x for s in sensors], dtype=float)
    sy = np.array([s.position_y for s in sensors], dtype=float)
    sz = np.array([s.depth for s in sensors], dtype=float)

    fig = plt.figure()
    ax = plt.gca()

    # área alvo
    _draw_target_circle(ax, cx, cy, R)

    # impactos
    ax.scatter(impacts_xy[:, 0], impacts_xy[:, 1], s=14, label="Impact points")

    # sensores (marker ^) e profundidade como texto/legenda simples
    ax.scatter(sx, sy, s=70, marker="^", label="Sensors")
    for i, (x, y, z) in enumerate(zip(sx, sy, sz)):
        ax.text(x, y, f"{i}", fontsize=8, ha="center", va="center")

    if show_detection:
        _draw_detection_circles(ax, sx, sy, max_det)

    ax.set_aspect("equal", adjustable="box")
    ax.grid(True, alpha=0.3)
    ax.set_xlabel("x (m)")
    ax.set_ylabel("y (m)")
    ax.set_title(title)
    ax.legend(loc="best")

    out_png.parent.mkdir(parents=True, exist_ok=True)
    plt.savefig(out_png, dpi=300, bbox_inches="tight")
    plt.close(fig)


def run(
    *,
    output_root: str,
    number_of_sensors: int,
    generation_to_plot: int,
    environment_settings: EnvironmentSettings,
    simulation_settings: SimulationSettings,
    show_detection: bool = True,
) -> None:
    outdir = Path(output_root) / f"sensors_{number_of_sensors}"
    impacts_dir = outdir / "impacts"
    figs_dir = outdir / "figures"

    grid = GridGeometry(environment_settings)

    impacts = _load_impacts(impacts_dir, generation_to_plot)

    best_chrs = _load_best_chromosomes_per_generation(outdir)  # (G, 3N)
    if generation_to_plot < 0 or generation_to_plot >= best_chrs.shape[0]:
        raise ValueError(f"generation_to_plot={generation_to_plot} out of range [0, {best_chrs.shape[0]-1}]")

    ga_best_gen_chr = best_chrs[generation_to_plot]
    ga_best_global_chr = _load_best_global(outdir)

    # decode sensores
    ga_sensors_gen = chromosome_converter(ga_best_gen_chr, number_of_sensors, grid, environment_settings)
    ga_sensors_global = chromosome_converter(ga_best_global_chr, number_of_sensors, grid, environment_settings)

    # baseline polígono (mesma profundidade média)
    polygon_chr = create_regular_polygon_chromosome(
        number_of_sensors=number_of_sensors,
        environment_settings=environment_settings,
        grid_geometry=grid,
        polygon_radius_meters=environment_settings.target_region_radius,
        depth_meters=(environment_settings.minimum_depth_in_meters + environment_settings.maximum_depth_in_meters) / 2.0,
        angle_offset_degrees=0.0,
    )
    poly_sensors = chromosome_converter(polygon_chr, number_of_sensors, grid, environment_settings)

    # salvar figuras
    plot_scene(
        out_png=figs_dir / f"scene_ga_best_global_gen_{generation_to_plot:04d}.png",
        title=f"GA Best (Global) — Gen {generation_to_plot}",
        impacts_xy=impacts,
        sensors=ga_sensors_global,
        env=environment_settings,
        show_detection=show_detection,
    )

    plot_scene(
        out_png=figs_dir / f"scene_ga_best_gen_{generation_to_plot:04d}.png",
        title=f"GA Best (Generation) — Gen {generation_to_plot}",
        impacts_xy=impacts,
        sensors=ga_sensors_gen,
        env=environment_settings,
        show_detection=show_detection,
    )

    plot_scene(
        out_png=figs_dir / f"scene_polygon_gen_{generation_to_plot:04d}.png",
        title=f"Regular Polygon — Gen {generation_to_plot}",
        impacts_xy=impacts,
        sensors=poly_sensors,
        env=environment_settings,
        show_detection=show_detection,
    )

    print(f"[OK] Saved figures to: {figs_dir}")


if __name__ == "__main__":
    # Exemplo de chamada manual (substitua pelos seus settings reais)
    env = EnvironmentSettings()
    sim = SimulationSettings()
    run(
        output_root="outputs",
        number_of_sensors=3,
        generation_to_plot=199,
        environment_settings=env,
        simulation_settings=sim,
        show_detection=True,
    )
