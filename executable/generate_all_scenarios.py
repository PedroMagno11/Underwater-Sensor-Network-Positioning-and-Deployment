from __future__ import annotations

from pathlib import Path
import numpy as np
import matplotlib.pyplot as plt

from geometry.grid_geometry import GridGeometry
from evaluation.chromosome_decoder import chromosome_converter
from topologies.regular_polygon import create_regular_polygon_chromosome


def draw_target_circle(ax, cx, cy, R):
    t = np.linspace(0, 2*np.pi, 400)
    ax.plot(cx + R*np.cos(t), cy + R*np.sin(t), linewidth=1.5)


def plot_scene(
    *,
    out_png: Path,
    title: str,
    impacts_xy: np.ndarray,     # (M,2)
    sensors,                   # list[AcousticSensor]
    env,
    show_detection: bool = True,
):
    cx = float(env.x_center_in_meters)
    cy = float(env.y_center_in_meters)
    R  = float(env.target_region_radius)
    max_det = float(env.maximum_detection_distance)

    fig, ax = plt.subplots()

    # círculo da área alvo
    draw_target_circle(ax, cx, cy, R)

    # impactos
    ax.scatter(impacts_xy[:, 0], impacts_xy[:, 1], s=14, label="Impact points")

    # sensores
    sx = np.array([s.position_x for s in sensors], dtype=float)
    sy = np.array([s.position_y for s in sensors], dtype=float)
    ax.scatter(sx, sy, s=60, marker="^", label="Sensors")

    # círculos de detecção
    if show_detection:
        t = np.linspace(0, 2*np.pi, 200)
        for x, y in zip(sx, sy):
            ax.plot(x + max_det*np.cos(t), y + max_det*np.sin(t), linewidth=0.8, alpha=0.25)

    ax.set_aspect("equal", adjustable="box")
    ax.grid(True, alpha=0.3)
    ax.set_xlabel("x (m)")
    ax.set_ylabel("y (m)")
    ax.set_title(title)
    ax.legend(loc="best")

    out_png.parent.mkdir(parents=True, exist_ok=True)
    plt.savefig(out_png, dpi=300, bbox_inches="tight")
    plt.close(fig)


def load_impacts(impacts_dir: Path, gen: int) -> np.ndarray:
    return np.load(impacts_dir / f"impact_points_gen_{gen:04d}.npy")


def generate_all_figures(
    *,
    number_of_sensors: int,
    output_root: str,
    environment_settings,
):
    outdir = Path(output_root) / f"sensors_{number_of_sensors}"
    impacts_dir = outdir / "impacts"
    figs_dir = outdir / "figures"

    grid = GridGeometry(environment_settings)

    best_chrs = np.load(outdir / "best_chromosomes_per_generation.npy")  # (G, L)
    best_global_chr = np.load(outdir / "best_global_chromosome.npy")     # (L,)

    num_generations = int(best_chrs.shape[0])

    # baseline polygon (fixo)
    polygon_chr = create_regular_polygon_chromosome(
        number_of_sensors=number_of_sensors,
        environment_settings=environment_settings,
        grid_geometry=grid,
        polygon_radius_meters=environment_settings.target_region_radius,
        depth_meters=(environment_settings.minimum_depth_in_meters + environment_settings.maximum_depth_in_meters) / 2.0,
        angle_offset_degrees=0.0,
    )
    poly_sensors = chromosome_converter(polygon_chr, number_of_sensors, grid, environment_settings)

    # decode sensores do best global (fixo)
    ga_sensors_global = chromosome_converter(best_global_chr, number_of_sensors, grid, environment_settings)

    # (opcional) salva UMA figura fixa do best global sem depender de geração
    # mas como impactos mudam por geração, normalmente você quer desenhar no cenário de cada geração.
    for gen in range(num_generations):
        impacts = load_impacts(impacts_dir, gen)

        # best da geração
        best_gen_chr = best_chrs[gen]
        ga_sensors_gen = chromosome_converter(best_gen_chr, number_of_sensors, grid, environment_settings)

        # 1) cenário: GA best da geração
        plot_scene(
            out_png=figs_dir / f"scene_ga_best_gen_{gen:04d}.png",
            title=f"GA Best (Generation) - Gen {gen}",
            impacts_xy=impacts,
            sensors=ga_sensors_gen,
            env=environment_settings,
            show_detection=True,
        )

        # 2) cenário: GA best global desenhado no cenário da geração gen
        plot_scene(
            out_png=figs_dir / f"scene_ga_best_global_on_gen_{gen:04d}.png",
            title=f"GA Best (Global) on Gen {gen}",
            impacts_xy=impacts,
            sensors=ga_sensors_global,
            env=environment_settings,
            show_detection=True,
        )

        # 3) cenário: polígono no cenário da geração gen
        plot_scene(
            out_png=figs_dir / f"scene_polygon_gen_{gen:04d}.png",
            title=f"Regular Polygon - Gen {gen}",
            impacts_xy=impacts,
            sensors=poly_sensors,
            env=environment_settings,
            show_detection=True,
        )

    print(f"Saved {num_generations * 3} figures to: {figs_dir}")


if __name__ == "__main__":
    from settings.environment_settings import EnvironmentSettings

    env = EnvironmentSettings()

    generate_all_figures(
        number_of_sensors=4,
        output_root="outputs",
        environment_settings=env,
    )
