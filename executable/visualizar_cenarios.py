from __future__ import annotations

from pathlib import Path
from typing import List, Tuple
import random
import csv

import numpy as np
import matplotlib.pyplot as plt

from executable.runner import build_sound_speed_profile
from geometry.grid_geometry import GridGeometry
from evaluation.chromosome_decoder import chromosome_converter
from topologies.regular_polygon import create_regular_polygon_chromosome

from evaluation.cost_function import evaluate_chromosome_with_report
from acoustic.sound_speed_profile import SoundSpeedProfile
from settings.simulation_settings import SimulationSettings


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

    draw_target_circle(ax, cx, cy, R)

    ax.scatter(impacts_xy[:, 0], impacts_xy[:, 1], s=14, label="Impact points")

    sx = np.array([s.position_x for s in sensors], dtype=float)
    sy = np.array([s.position_y for s in sensors], dtype=float)
    ax.scatter(sx, sy, s=60, marker="^", label="Sensors")

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


def _to_impact_list(impacts_xy: np.ndarray) -> List[Tuple[float, float]]:
    return [(float(x), float(y)) for x, y in impacts_xy]


def evaluate_chromosome_on_impacts(
    *,
    chromosome: np.ndarray,
    number_of_sensors: int,
    env,
    sim: SimulationSettings,
    ssp: SoundSpeedProfile,
    grid: GridGeometry,
    impacts_xy: np.ndarray,
    noise_seed: int,
):
    rng = random.Random(int(noise_seed))
    impact_points = _to_impact_list(impacts_xy)

    report = evaluate_chromosome_with_report(
        chromosome=chromosome,
        number_of_sensors=number_of_sensors,
        grid_geometry=grid,
        environment_settings=env,
        simulation_settings=sim,
        sound_speed_profile=ssp,
        random_generator=rng,
        impact_points=impact_points,
    )
    return report


def generate_all_figures(
    *,
    number_of_sensors: int,
    output_root: str,
    environment_settings,
    simulation_settings: SimulationSettings,
    sound_speed_profile: SoundSpeedProfile,
    noise_seed_for_comparison: int = 999,
    write_csv: bool = True,
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

    # CSV comparativo (opcional)
    rows = []
    csv_path = figs_dir / "comparison_metrics.csv"

    for gen in range(num_generations):
        impacts = load_impacts(impacts_dir, gen)

        best_gen_chr = best_chrs[gen]

        # --- avaliar fitness/custo com MESMOS impacts e MESMO noise_seed ---
        rep_ga_gen = evaluate_chromosome_on_impacts(
            chromosome=best_gen_chr,
            number_of_sensors=number_of_sensors,
            env=environment_settings,
            sim=simulation_settings,
            ssp=sound_speed_profile,
            grid=grid,
            impacts_xy=impacts,
            noise_seed=noise_seed_for_comparison,
        )

        rep_ga_global = evaluate_chromosome_on_impacts(
            chromosome=best_global_chr,
            number_of_sensors=number_of_sensors,
            env=environment_settings,
            sim=simulation_settings,
            ssp=sound_speed_profile,
            grid=grid,
            impacts_xy=impacts,
            noise_seed=noise_seed_for_comparison,
        )

        rep_poly = evaluate_chromosome_on_impacts(
            chromosome=polygon_chr,
            number_of_sensors=number_of_sensors,
            env=environment_settings,
            sim=simulation_settings,
            ssp=sound_speed_profile,
            grid=grid,
            impacts_xy=impacts,
            noise_seed=noise_seed_for_comparison,
        )

        # --- decode sensores pra plot ---
        ga_sensors_gen = chromosome_converter(best_gen_chr, number_of_sensors, grid, environment_settings)
        ga_sensors_global = chromosome_converter(best_global_chr, number_of_sensors, grid, environment_settings)
        poly_sensors = chromosome_converter(polygon_chr, number_of_sensors, grid, environment_settings)

        # helper de título (paper-friendly)
        def fmt(rep):
            no_cov = rep.number_of_impacts_without_coverage / max(1, rep.number_of_impacts)
            return f"cost={rep.total_cost:.3f} | mean_err={rep.mean_localization_error_meters:.2f} m | no_cov={100*no_cov:.1f}%"

        # 1) GA best da geração
        plot_scene(
            out_png=figs_dir / f"scene_ga_best_gen_{gen:04d}.png",
            title=f"GA Best (Gen {gen}) — {fmt(rep_ga_gen)}",
            impacts_xy=impacts,
            sensors=ga_sensors_gen,
            env=environment_settings,
            show_detection=True,
        )

        # 2) GA best global no cenário da geração
        plot_scene(
            out_png=figs_dir / f"scene_ga_best_global_on_gen_{gen:04d}.png",
            title=f"GA Best Global (on Gen {gen}) — {fmt(rep_ga_global)}",
            impacts_xy=impacts,
            sensors=ga_sensors_global,
            env=environment_settings,
            show_detection=True,
        )

        # 3) polígono no cenário da geração
        plot_scene(
            out_png=figs_dir / f"scene_polygon_gen_{gen:04d}.png",
            title=f"Regular Polygon (Gen {gen}) — {fmt(rep_poly)}",
            impacts_xy=impacts,
            sensors=poly_sensors,
            env=environment_settings,
            show_detection=True,
        )

        if write_csv:
            def add_row(label, rep):
                no_cov = rep.number_of_impacts_without_coverage / max(1, rep.number_of_impacts)
                rows.append({
                    "generation": gen,
                    "label": label,
                    "total_cost": float(rep.total_cost),
                    "mean_error_m": float(rep.mean_localization_error_meters),
                    "no_coverage_rate": float(no_cov),
                    "localizable_impacts": int(rep.number_of_localizable_impacts),
                    "num_impacts": int(rep.number_of_impacts),
                    "noise_seed": int(noise_seed_for_comparison),
                })

            add_row("GA_best_gen", rep_ga_gen)
            add_row("GA_best_global", rep_ga_global)
            add_row("polygon", rep_poly)

    if write_csv:
        csv_path.parent.mkdir(parents=True, exist_ok=True)
        with csv_path.open("w", newline="", encoding="utf-8") as f:
            w = csv.DictWriter(f, fieldnames=list(rows[0].keys()) if rows else [])
            w.writeheader()
            w.writerows(rows)

    print(f"Saved {num_generations * 3} figures to: {figs_dir}")
    if write_csv:
        print(f"Saved comparison CSV to: {csv_path}")


if __name__ == "__main__":
    from settings.environment_settings import EnvironmentSettings
    from settings.simulation_settings import SimulationSettings
    from acoustic.sound_speed_profile import SoundSpeedProfile

    env = EnvironmentSettings()
    sim = SimulationSettings()
    ssp = build_sound_speed_profile()

    generate_all_figures(
        number_of_sensors=5,
        output_root="outputs",
        environment_settings=env,
        simulation_settings=sim,
        sound_speed_profile=ssp,
        noise_seed_for_comparison=999,
        write_csv=True,
    )
