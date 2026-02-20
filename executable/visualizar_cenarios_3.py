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
from topologies.regular_polygon import create_regular_polygon_chromosome

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
    """
    Tenta inferir profundidade/z do seu AcousticSensor.
    Ajuste esta lista se no seu projeto o atributo tiver outro nome.
    """
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
    """
    Retorna mask boolean (M,) => True se coberto, False se fora de cobertura,
    com critério 2D:
        coberto se (n_sensores com dist <= max_det) >= min_sensors_for_coverage
    """
    if impacts_xy.size == 0:
        return np.array([], dtype=bool)

    if len(sensors) == 0:
        return np.zeros((impacts_xy.shape[0],), dtype=bool)

    sx = np.array([float(s.position_x) for s in sensors], dtype=float)
    sy = np.array([float(s.position_y) for s in sensors], dtype=float)

    # Distâncias de cada impacto para cada sensor: (M,N)
    dx = impacts_xy[:, 0:1] - sx.reshape(1, -1)
    dy = impacts_xy[:, 1:2] - sy.reshape(1, -1)
    d = np.sqrt(dx * dx + dy * dy)

    # Conta quantos sensores estão dentro do raio
    within = d <= float(max_det)  # (M,N) bool
    count = within.sum(axis=1)    # (M,) int
    return count >= int(min_sensors_for_coverage)


def _pairwise_indices(n: int):
    for i in range(n):
        for j in range(i + 1, n):
            yield i, j


def _compute_all_sensor_pairs_with_distances(sx: np.ndarray, sy: np.ndarray) -> List[Tuple[float, int, int]]:
    pairs: List[Tuple[float, int, int]] = []
    n = int(len(sx))
    for i, j in _pairwise_indices(n):
        dist = float(np.hypot(sx[j] - sx[i], sy[j] - sy[i]))
        pairs.append((dist, i, j))
    pairs.sort(key=lambda t: t[0])
    return pairs


def _mst_edges_kruskal(n: int, edges_sorted: List[Tuple[float, int, int]]) -> List[Tuple[float, int, int]]:
    """
    Kruskal para MST em grafo completo com pesos = distâncias.
    Retorna lista de arestas (dist, i, j) da MST.
    """
    parent = list(range(n))
    rank = [0] * n

    def find(a: int) -> int:
        while parent[a] != a:
            parent[a] = parent[parent[a]]
            a = parent[a]
        return a

    def union(a: int, b: int) -> bool:
        ra, rb = find(a), find(b)
        if ra == rb:
            return False
        if rank[ra] < rank[rb]:
            parent[ra] = rb
        elif rank[ra] > rank[rb]:
            parent[rb] = ra
        else:
            parent[rb] = ra
            rank[ra] += 1
        return True

    mst = []
    for dist, i, j in edges_sorted:
        if union(i, j):
            mst.append((dist, i, j))
            if len(mst) == n - 1:
                break
    return mst


def plot_scene(
    *,
    out_png: Path,
    title: str,
    impacts_xy: np.ndarray,     # (M,2)
    sensors,                    # list[AcousticSensor]
    env,
    show_detection: bool = True,

    # cobertura
    highlight_out_of_coverage: bool = True,
    out_of_coverage_mask: Optional[np.ndarray] = None,  # (M,) True => fora
    min_sensors_for_coverage: int = 3,

    # linhas
    show_inter_sensor_lines: bool = True,
    inter_sensor_lines_mode: str = "mst",  # "all" | "limit" | "mst"
    max_lines: int = 8,

    # profundidade
    show_depth_labels: bool = True,

    # texto de dist
    distance_decimals: int = 1,
    distance_unit: str = "m",
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

        # Cores FORÇADAS (evita “mesma coloração”)
        if covered_mask.any():
            ax.scatter(
                impacts_xy[covered_mask, 0],
                impacts_xy[covered_mask, 1],
                s=14,
                c="tab:blue",
                label=f"Impact points (covered ≥{min_sensors_for_coverage})",
                alpha=0.9,
                linewidths=0,
            )
        if out_of_coverage_mask.any():
            ax.scatter(
                impacts_xy[out_of_coverage_mask, 0],
                impacts_xy[out_of_coverage_mask, 1],
                s=22,
                c="tab:red",
                marker="x",
                label=f"Impact points (out <{min_sensors_for_coverage})",
                alpha=0.95,
                linewidths=1.4,
            )
    else:
        ax.scatter(impacts_xy[:, 0], impacts_xy[:, 1], s=14, label="Impact points")

    # linhas entre boias
    if show_inter_sensor_lines and len(sensors) >= 2:
        pairs_sorted = _compute_all_sensor_pairs_with_distances(sx, sy)
        n = len(sensors)

        if inter_sensor_lines_mode == "all":
            chosen = pairs_sorted
        elif inter_sensor_lines_mode == "limit":
            chosen = pairs_sorted[: max(0, int(max_lines))]
        elif inter_sensor_lines_mode == "mst":
            chosen = _mst_edges_kruskal(n, pairs_sorted)
        else:
            raise ValueError(f"inter_sensor_lines_mode desconhecido: {inter_sensor_lines_mode}")

        fmt = f"{{:.{int(distance_decimals)}f}} {distance_unit}"

        for dist, i, j in chosen:
            x1, y1 = sx[i], sy[i]
            x2, y2 = sx[j], sy[j]
            ax.plot([x1, x2], [y1, y2], linewidth=0.8, alpha=0.35)

            mx, my = (x1 + x2) / 2.0, (y1 + y2) / 2.0
            ax.text(
                mx, my,
                fmt.format(dist),
                fontsize=7,
                alpha=0.85,
                ha="center",
                va="center",
                bbox=dict(boxstyle="round,pad=0.15", alpha=0.15),
            )

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


# =========================
#  Main generation
# =========================

def generate_all_figures(
    *,
    number_of_sensors: int,
    output_root: str,
    environment_settings,
    simulation_settings: SimulationSettings,
    sound_speed_profile: SoundSpeedProfile,
    noise_seed_for_comparison: int = 999,
    write_csv: bool = True,

    # novos knobs do plot
    min_sensors_for_coverage: int = 3,
    highlight_out_of_coverage: bool = True,
    inter_sensor_lines_mode: str = "mst",   # "mst" recomendado para paper
    max_lines: int = 8,                     # usado se mode="limit"
    show_depth_labels: bool = True,
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

        # --- avaliar custo com MESMOS impacts e MESMO noise_seed ---
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
            highlight_out_of_coverage=highlight_out_of_coverage,
            out_of_coverage_mask=None,  # auto: >=3 círculos
            min_sensors_for_coverage=min_sensors_for_coverage,
            show_inter_sensor_lines=True,
            inter_sensor_lines_mode=inter_sensor_lines_mode,
            max_lines=max_lines,
            show_depth_labels=show_depth_labels,
        )

        # 2) GA best global no cenário da geração
        plot_scene(
            out_png=figs_dir / f"scene_ga_best_global_on_gen_{gen:04d}.png",
            title=f"GA Best Global (on Gen {gen}) — {fmt(rep_ga_global)}",
            impacts_xy=impacts,
            sensors=ga_sensors_global,
            env=environment_settings,
            show_detection=True,
            highlight_out_of_coverage=highlight_out_of_coverage,
            out_of_coverage_mask=None,
            min_sensors_for_coverage=min_sensors_for_coverage,
            show_inter_sensor_lines=True,
            inter_sensor_lines_mode=inter_sensor_lines_mode,
            max_lines=max_lines,
            show_depth_labels=show_depth_labels,
        )

        # 3) polígono no cenário da geração
        plot_scene(
            out_png=figs_dir / f"scene_polygon_gen_{gen:04d}.png",
            title=f"Regular Polygon (Gen {gen}) — {fmt(rep_poly)}",
            impacts_xy=impacts,
            sensors=poly_sensors,
            env=environment_settings,
            show_detection=True,
            highlight_out_of_coverage=highlight_out_of_coverage,
            out_of_coverage_mask=None,
            min_sensors_for_coverage=min_sensors_for_coverage,
            show_inter_sensor_lines=True,
            inter_sensor_lines_mode=inter_sensor_lines_mode,
            max_lines=max_lines,
            show_depth_labels=show_depth_labels,
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

    env = EnvironmentSettings()
    sim = SimulationSettings()
    ssp = build_sound_speed_profile()

    generate_all_figures(
        number_of_sensors=3,
        output_root="outputs",
        environment_settings=env,
        simulation_settings=sim,
        sound_speed_profile=ssp,
        noise_seed_for_comparison=999,
        write_csv=True,

        # regra correta:
        min_sensors_for_coverage=3,

        # visuais (paper-friendly):
        highlight_out_of_coverage=True,
        inter_sensor_lines_mode="mst",
        max_lines=8,
        show_depth_labels=True,
    )