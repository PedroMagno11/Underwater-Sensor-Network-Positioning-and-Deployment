from __future__ import annotations

from pathlib import Path
from typing import List, Tuple, Optional
import random
import csv

import numpy as np
import matplotlib.pyplot as plt

from executable.runner import build_sound_speed_profile
from geometry.grid_geometry import GridGeometry
from evaluation.chromosome_decoder import chromosome_converter

from evaluation.cost_function import evaluate_chromosome_with_report
from acoustic.sound_speed_profile import SoundSpeedProfile
from settings.simulation_settings import SimulationSettings


# =========================
#  Plot helpers
# =========================

def draw_target_circle(ax, cx, cy, R):
    t = np.linspace(0, 2 * np.pi, 400)
    ax.plot(cx + R * np.cos(t), cy + R * np.sin(t), linewidth=1.5)


def _get_sensor_depth_m(sensor) -> Optional[float]:
    for attr in ("position_z", "position_z_m", "depth_meters", "depth", "z"):
        if hasattr(sensor, attr):
            try:
                return float(getattr(sensor, attr))
            except Exception:
                pass
    return None


def _classify_impacts_by_coverage_2d(
    impacts_xy: np.ndarray,
    sensors,
    max_det: float,
    min_sensors_for_coverage: int = 3,
) -> np.ndarray:
    if impacts_xy.size == 0:
        return np.array([], dtype=bool)

    if len(sensors) == 0:
        return np.zeros((impacts_xy.shape[0],), dtype=bool)

    sx = np.array([float(s.position_x) for s in sensors], dtype=float)
    sy = np.array([float(s.position_y) for s in sensors], dtype=float)

    dx = impacts_xy[:, 0:1] - sx.reshape(1, -1)
    dy = impacts_xy[:, 1:2] - sy.reshape(1, -1)
    d = np.sqrt(dx * dx + dy * dy)

    within = d <= float(max_det)
    count = within.sum(axis=1)
    return count >= int(min_sensors_for_coverage)


def plot_scene(
    *,
    out_png: Path,
    title: str,
    impacts_xy: np.ndarray,
    sensors,
    env,
    show_detection: bool = True,

    # cobertura
    highlight_out_of_coverage: bool = True,
    out_of_coverage_mask: Optional[np.ndarray] = None,
    min_sensors_for_coverage: int = 3,

    # ⚠️ se você não quer NENHUMA linha de polígono, deixe False
    show_sensor_polygon_links: bool = False,

    # profundidade
    show_depth_labels: bool = True,

    # cores
    covered_color: str = "green",
    uncovered_color: str = "tab:red",
):
    cx = float(env.x_center_in_meters)
    cy = float(env.y_center_in_meters)
    R  = float(env.target_region_radius)
    max_det = float(env.maximum_detection_distance)

    fig, ax = plt.subplots()

    # região alvo
    draw_target_circle(ax, cx, cy, R)

    # sensores
    sx = np.array([float(s.position_x) for s in sensors], dtype=float)
    sy = np.array([float(s.position_y) for s in sensors], dtype=float)
    ax.scatter(sx, sy, s=60, marker="^", label="Sensors")

    # círculos de detecção
    if show_detection:
        t = np.linspace(0, 2 * np.pi, 200)
        for x, y in zip(sx, sy):
            ax.plot(
                x + max_det * np.cos(t),
                y + max_det * np.sin(t),
                linewidth=0.8,
                alpha=0.25,
            )

    # impactos (coberto vs fora)
    if highlight_out_of_coverage:
        if out_of_coverage_mask is None:
            covered_mask = _classify_impacts_by_coverage_2d(
                impacts_xy=impacts_xy,
                sensors=sensors,
                max_det=max_det,
                min_sensors_for_coverage=min_sensors_for_coverage,
            )
            out_of_coverage_mask = ~covered_mask
        else:
            out_of_coverage_mask = np.asarray(out_of_coverage_mask, dtype=bool)

        if out_of_coverage_mask.shape[0] != impacts_xy.shape[0]:
            raise ValueError("out_of_coverage_mask deve ter shape (M,) com M=len(impacts_xy).")

        covered_mask = ~out_of_coverage_mask

        if covered_mask.any():
            ax.scatter(
                impacts_xy[covered_mask, 0],
                impacts_xy[covered_mask, 1],
                s=14,
                c=covered_color,
                label="Impact points covered",
                alpha=0.9,
                linewidths=0,
            )
        if out_of_coverage_mask.any():
            ax.scatter(
                impacts_xy[out_of_coverage_mask, 0],
                impacts_xy[out_of_coverage_mask, 1],
                s=22,
                c=uncovered_color,
                marker="x",
                label="Impact points not covered",
                alpha=0.95,
                linewidths=1.4,
            )
    else:
        ax.scatter(impacts_xy[:, 0], impacts_xy[:, 1], s=14, label="Impact points")

    # (removido) ligações tipo polígono — mantemos o parâmetro mas por padrão fica False

    # profundidade dos sensores
    if show_depth_labels:
        for i, s in enumerate(sensors):
            z = _get_sensor_depth_m(s)
            if z is None:
                continue
            ax.text(
                sx[i], sy[i],
                f" z={z:.1f}m",
                fontsize=8,
                ha="left",
                va="bottom",
                alpha=0.9,
            )

    ax.set_aspect("equal", adjustable="box")
    ax.grid(True, alpha=0.3)
    ax.set_xlabel("x (m)")
    ax.set_ylabel("y (m)")
    ax.set_title(title)
    ax.legend(loc="best")

    out_png.parent.mkdir(parents=True, exist_ok=True)
    plt.savefig(out_png, dpi=300, bbox_inches="tight")
    plt.close(fig)


# =========================
#  Data + evaluation helpers
# =========================

def load_impacts(impacts_dir: Path, gen: int) -> np.ndarray:
    return np.load(impacts_dir / f"impact_points_gen_{gen:04d}.npy")


def _to_impact_list(impacts_xy: np.ndarray) -> List[Tuple[float, float]]:
    return [(float(x), float(y)) for x, y in impacts_xy]


def evaluate_chromosome_on_impacts(
    *,
    chromosome: np.ndarray,
    number_of_sensors: int,
    env,
    sim,
    ssp,
    grid,
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


# =========================
#  Main generation (GA only)
# =========================

def generate_ga_figures_only(
    *,
    number_of_sensors: int,
    output_root: str,
    environment_settings,
    simulation_settings: SimulationSettings,
    sound_speed_profile: SoundSpeedProfile,
    noise_seed_for_comparison: int = 999,
    write_csv: bool = True,

    min_sensors_for_coverage: int = 3,
    covered_color: str = "gold",
    uncovered_color: str = "tab:red",

    # se quiser linhas ligando sensores no GA, coloque True
    show_sensor_polygon_links: bool = False,
):
    outdir = Path(output_root) / f"sensors_{number_of_sensors}"
    impacts_dir = outdir / "impacts"
    figs_dir = outdir / "figures_ga_only"

    grid = GridGeometry(environment_settings)

    best_chrs = np.load(outdir / "best_chromosomes_per_generation.npy")  # (G, L)
    best_global_chr = np.load(outdir / "best_global_chromosome.npy")     # (L,)
    num_generations = int(best_chrs.shape[0])

    rows = []
    csv_path = figs_dir / "ga_only_comparison_metrics.csv"

    for gen in range(num_generations):
        impacts = load_impacts(impacts_dir, gen)
        best_gen_chr = best_chrs[gen]

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

        ga_sensors_gen = chromosome_converter(best_gen_chr, number_of_sensors, grid, environment_settings)
        ga_sensors_global = chromosome_converter(best_global_chr, number_of_sensors, grid, environment_settings)

        def fmt(rep):
            no_cov = rep.number_of_impacts_without_coverage / max(1, rep.number_of_impacts)
            return f"cost={rep.total_cost:.3f} | mean_err={rep.mean_localization_error_meters:.2f} m | no_cov={100*no_cov:.1f}%"

        plot_scene(
            out_png=figs_dir / f"scene_ga_best_gen_{gen:04d}.png",
            title=f"GA Best (Gen {gen}) — {fmt(rep_ga_gen)}",
            impacts_xy=impacts,
            sensors=ga_sensors_gen,
            env=environment_settings,
            show_detection=True,
            highlight_out_of_coverage=True,
            out_of_coverage_mask=None,
            min_sensors_for_coverage=min_sensors_for_coverage,
            show_sensor_polygon_links=show_sensor_polygon_links,
            show_depth_labels=True,
            covered_color=covered_color,
            uncovered_color=uncovered_color,
        )

        plot_scene(
            out_png=figs_dir / f"scene_ga_best_global_on_gen_{gen:04d}.png",
            title=f"GA Best Global (on Gen {gen}) — {fmt(rep_ga_global)}",
            impacts_xy=impacts,
            sensors=ga_sensors_global,
            env=environment_settings,
            show_detection=True,
            highlight_out_of_coverage=True,
            out_of_coverage_mask=None,
            min_sensors_for_coverage=min_sensors_for_coverage,
            show_sensor_polygon_links=show_sensor_polygon_links,
            show_depth_labels=True,
            covered_color=covered_color,
            uncovered_color=uncovered_color,
        )

        if write_csv:
            def add_row(label, rep):
                no_cov = rep.number_of_impacts_without_coverage / max(1, rep.number_of_impacts)
                print(f'TOTAL COST: {rep.total_cost}')
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

    if write_csv and rows:
        csv_path.parent.mkdir(parents=True, exist_ok=True)
        with csv_path.open("w", newline="", encoding="utf-8") as f:
            w = csv.DictWriter(f, fieldnames=list(rows[0].keys()))
            w.writeheader()
            w.writerows(rows)

    print(f"Saved {num_generations * 2} GA figures to: {figs_dir}")
    if write_csv and rows:
        print(f"Saved GA comparison CSV to: {csv_path}")


if __name__ == "__main__":
    from settings.environment_settings import EnvironmentSettings

    env = EnvironmentSettings()
    sim = SimulationSettings()
    ssp = build_sound_speed_profile()

    generate_ga_figures_only(
        number_of_sensors=5,
        output_root="outputs",
        environment_settings=env,
        simulation_settings=sim,
        sound_speed_profile=ssp,
        noise_seed_for_comparison=999,
        write_csv=True,
        min_sensors_for_coverage=3,
        covered_color="gold",
        uncovered_color="tab:red",
        show_sensor_polygon_links=False,  # <-- sem “polígono” desenhado
    )