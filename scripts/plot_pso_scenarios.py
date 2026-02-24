from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Dict, List, Optional, Sequence

import numpy as np

from settings.environment_settings import EnvironmentSettings
from geometry.grid_geometry import GridGeometry

# Reuse your exact plot_scene helpers:
# - plot_scene
# - load_impacts
from scripts.plot_ga_scenarios import plot_scene, load_impacts


# =========================
# Small adapter: vector -> "sensor-like" objects
# =========================

@dataclass(frozen=True)
class _SimpleSensor:
    position_x: float
    position_y: float
    position_z: float


def _infer_dim_per_sensor(vec: np.ndarray, n: int) -> int:
    dim = int(len(vec) / n)
    if dim * n != len(vec):
        raise ValueError(f"len(vec)={len(vec)} not divisible by n={n}")
    return dim


def _vec_to_sensors(
    vec: np.ndarray,
    n_sensors: int,
    env: EnvironmentSettings,
) -> List[_SimpleSensor]:
    vec = np.asarray(vec, dtype=float).reshape(-1)
    dim = _infer_dim_per_sensor(vec, n_sensors)
    arr = vec.reshape(n_sensors, dim)

    zmin = float(env.minimum_depth_in_meters)
    zmax = float(env.maximum_depth_in_meters)

    sensors: List[_SimpleSensor] = []
    for i in range(n_sensors):
        x = float(arr[i, 0])
        y = float(arr[i, 1])

        if dim >= 3:
            z = float(arr[i, 2])
        else:
            z = (zmin + zmax) / 2.0

        # clamp (safety)
        if z < zmin:
            z = zmin
        if z > zmax:
            z = zmax

        sensors.append(_SimpleSensor(position_x=x, position_y=y, position_z=z))

    return sensors


def _fmt_metrics_from_row(row: Optional[Dict[str, Any]]) -> str:
    if not row:
        return "metrics=n/a"
    cost = float(row.get("total_cost", "nan"))
    mean_err = float(row.get("mean_error_m", "nan"))
    no_cov_rate = float(row.get("no_coverage_rate", "nan"))
    return f"cost={cost:.3f} | mean_err={mean_err:.2f} m | no_cov={100*no_cov_rate:.1f}%"


def _read_best_reports_jsonl(jsonl_path: Path) -> List[Dict[str, Any]]:
    if not jsonl_path.exists():
        raise FileNotFoundError(f"Missing PSO best_reports jsonl: {jsonl_path}")
    rows: List[Dict[str, Any]] = []
    with jsonl_path.open("r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            rows.append(json.loads(line))
    return rows


def _index_reports_by_iter(items: List[Dict[str, Any]]) -> Dict[int, Dict[str, Any]]:
    out: Dict[int, Dict[str, Any]] = {}
    for it in items:
        k = it.get("generation_index", it.get("generation", None))
        if k is None:
            continue
        try:
            idx = int(float(k))
        except Exception:
            continue
        out[idx] = it
    return out


# =========================
# Main (PSO figures)
# =========================

def generate_pso_scenario_figures(
    *,
    number_of_sensors: int,

    # roots
    pso_output_root: str = "outputs",

    # output
    figures_subdir: str = "figures_pso",

    # PSO files
    pso_best_positions_per_iter_rel: str = "pso_best_positions_per_iteration.npy",
    pso_best_global_rel: str = "pso_best_global_position.npy",
    pso_reports_jsonl_rel: str = "best_reports_pso.jsonl",

    # impacts dir
    impacts_subdir: str = "impacts_pso",         # if your PSO saves impacts here
    fallback_impacts_subdir: str = "impacts",    # if you want to reuse GA impacts

    # modes
    plot_best_global_on_all_iterations: bool = True,
    plot_snapshots_on_single_iteration_impacts: bool = True,
    snapshots_iteration_index: int = 0,
    snapshot_every: int = 50,
    max_snapshots: int = 14,

    # optional: compare GA vs PSO on same impacts (nice sanity view)
    plot_compare_ga_vs_pso_on_iteration: bool = False,
    ga_best_global_chromosome_rel: str = "best_global_chromosome.npy",

    # coverage/detection visualization (same knobs)
    min_sensors_for_coverage: int = 3,
    show_detection: bool = True,
    show_depth_labels: bool = True,
    covered_color: str = "gold",
    uncovered_color: str = "tab:red",

    # polygon edges + distances (reused by plot_scene)
    show_sensor_polygon_links: bool = True,
    polygon_line_color: str = "black",
    polygon_line_style: str = "--",
    polygon_line_width: float = 1.2,
    polygon_line_alpha: float = 0.65,
    distance_decimals: int = 1,
    distance_unit: str = "m",

    # titles
    title_prefix: str = "PSO",
    include_metrics_in_title: bool = True,
) -> None:
    outdir = Path(pso_output_root) / f"sensors_{number_of_sensors}"
    figs_dir = outdir / figures_subdir
    figs_dir.mkdir(parents=True, exist_ok=True)

    # settings (region + depth clamp)
    env = EnvironmentSettings()
    _ = GridGeometry(env)  # not strictly needed, but keeps parity w/ other scripts

    # resolve impacts directory
    impacts_dir = outdir / impacts_subdir
    if not impacts_dir.exists():
        # optionally reuse GA impacts
        impacts_dir = outdir / fallback_impacts_subdir
    if not impacts_dir.exists():
        raise FileNotFoundError(f"Missing impacts dir (tried PSO and GA): {outdir / impacts_subdir} | {outdir / fallback_impacts_subdir}")

    # load PSO arrays
    pso_hist_path = outdir / pso_best_positions_per_iter_rel
    pso_best_path = outdir / pso_best_global_rel
    if not pso_best_path.exists():
        raise FileNotFoundError(f"Missing PSO best global: {pso_best_path}")
    if not pso_hist_path.exists():
        raise FileNotFoundError(f"Missing PSO history: {pso_hist_path}")

    pso_best_global_vec = np.load(pso_best_path).astype(float)
    pso_hist = np.load(pso_hist_path).astype(float)   # shape [iters, dim]
    n_iters = int(pso_hist.shape[0])

    # reports (optional, for metrics in title)
    reports_path = outdir / pso_reports_jsonl_rel
    reports_by_iter: Dict[int, Dict[str, Any]] = {}
    if reports_path.exists():
        items = _read_best_reports_jsonl(reports_path)
        reports_by_iter = _index_reports_by_iter(items)

    def metrics_for_iter(it: int) -> Optional[Dict[str, Any]]:
        obj = reports_by_iter.get(int(it))
        if not obj:
            return None
        # prefer best_global for stability
        bg = obj.get("best_global")
        if isinstance(bg, dict):
            # expected keys: total_cost, mean_localization_error_meters, no_cov...
            return {
                "total_cost": bg.get("total_cost"),
                "mean_error_m": bg.get("mean_localization_error_meters", bg.get("mean_error_m")),
                "no_coverage_rate": (
                    (float(bg.get("number_of_impacts_without_coverage", 0)) / float(bg.get("number_of_impacts", 1)))
                    if float(bg.get("number_of_impacts", 0) or 0) > 0 else None
                ),
            }
        return None

    # -------------------------
    # MODE A: best global layout on all iterations impacts
    # -------------------------
    if plot_best_global_on_all_iterations:
        impact_files = sorted(impacts_dir.glob("impact_points_gen_*.npy"))
        if not impact_files:
            raise FileNotFoundError(f"No impact_points_gen_*.npy found in {impacts_dir}")

        sensors_best_global = _vec_to_sensors(pso_best_global_vec, number_of_sensors, env)

        for p in impact_files:
            it = int(p.stem.split("_")[-1])
            impacts = np.load(p)

            title = f"{title_prefix} (Best Global) on Iter {it}"
            if include_metrics_in_title:
                m = metrics_for_iter(it)
                title += f" — {_fmt_metrics_from_row(m)}"

            plot_scene(
                out_png=figs_dir / f"scene_pso_best_global_on_iter_{it:04d}.png",
                title=title,
                impacts_xy=impacts,
                sensors=sensors_best_global,
                env=env,
                show_detection=show_detection,
                highlight_out_of_coverage=True,
                out_of_coverage_mask=None,
                min_sensors_for_coverage=min_sensors_for_coverage,
                show_depth_labels=show_depth_labels,
                covered_color=covered_color,
                uncovered_color=uncovered_color,

                show_sensor_polygon_links=show_sensor_polygon_links,
                polygon_line_color=polygon_line_color,
                polygon_line_style=polygon_line_style,
                polygon_line_width=polygon_line_width,
                polygon_line_alpha=polygon_line_alpha,
                distance_decimals=distance_decimals,
                distance_unit=distance_unit,
            )

        print(f"Saved PSO BEST-GLOBAL figures to: {figs_dir}")

    # -------------------------
    # MODE B: snapshots (best-so-far vectors) on a single impacts iteration
    # -------------------------
    if plot_snapshots_on_single_iteration_impacts:
        impacts = load_impacts(impacts_dir, snapshots_iteration_index)

        idxs = list(range(0, n_iters, max(1, int(snapshot_every))))
        if (n_iters - 1) not in idxs:
            idxs.append(n_iters - 1)

        # cap snapshots (keep first + last + sampled middle)
        if len(idxs) > max_snapshots:
            keep = [idxs[0]]
            mid = idxs[1:-1]
            if mid:
                step = max(1, len(mid) // max(1, (max_snapshots - 2)))
                keep.extend(mid[::step])
            keep.append(idxs[-1])
            idxs = keep[:max_snapshots]

        for it in idxs:
            vec = pso_hist[int(it)]
            sensors = _vec_to_sensors(vec, number_of_sensors, env)

            title = f"{title_prefix} (Best-so-far) snapshot at iter {it} — impacts@{snapshots_iteration_index}"
            if include_metrics_in_title:
                m = metrics_for_iter(it)
                title += f" — {_fmt_metrics_from_row(m)}"

            plot_scene(
                out_png=figs_dir / f"scene_pso_snapshot_iter_{it:04d}_on_impacts_{snapshots_iteration_index:04d}.png",
                title=title,
                impacts_xy=impacts,
                sensors=sensors,
                env=env,
                show_detection=show_detection,
                highlight_out_of_coverage=True,
                out_of_coverage_mask=None,
                min_sensors_for_coverage=min_sensors_for_coverage,
                show_depth_labels=show_depth_labels,
                covered_color=covered_color,
                uncovered_color=uncovered_color,

                show_sensor_polygon_links=show_sensor_polygon_links,
                polygon_line_color=polygon_line_color,
                polygon_line_style=polygon_line_style,
                polygon_line_width=polygon_line_width,
                polygon_line_alpha=polygon_line_alpha,
                distance_decimals=distance_decimals,
                distance_unit=distance_unit,
            )

        print(f"Saved PSO SNAPSHOT figures to: {figs_dir}")

    # -------------------------
    # MODE C: GA best_global vs PSO best_global on same impacts (optional)
    # -------------------------
    if plot_compare_ga_vs_pso_on_iteration:
        # load GA best chromosome and rely on plot_ga_scenarios' converter style?
        # We avoid importing chromosome_converter to keep same style as requested;
        # here we plot GA best_global positions from saved numpy if you have it.
        ga_best_path = outdir / ga_best_global_chromosome_rel
        if not ga_best_path.exists():
            raise FileNotFoundError(f"Missing GA best_global_chromosome.npy at: {ga_best_path}")

        # GA best chromosome is in "chromosome format" => but its vector is typically 3*n (x,y,z)
        ga_best_vec = np.load(ga_best_path).astype(float).reshape(-1)
        ga_sensors = _vec_to_sensors(ga_best_vec, number_of_sensors, env)

        pso_sensors = _vec_to_sensors(pso_best_global_vec, number_of_sensors, env)
        impacts = load_impacts(impacts_dir, snapshots_iteration_index)

        plot_scene(
            out_png=figs_dir / f"scene_compare_ga_vs_pso_on_impacts_{snapshots_iteration_index:04d}.png",
            title=f"GA best_global vs PSO best_global — impacts@{snapshots_iteration_index}",
            impacts_xy=impacts,
            sensors=ga_sensors,
            env=env,
            show_detection=show_detection,
            highlight_out_of_coverage=True,
            out_of_coverage_mask=None,
            min_sensors_for_coverage=min_sensors_for_coverage,
            show_depth_labels=show_depth_labels,
            covered_color=covered_color,
            uncovered_color=uncovered_color,

            show_sensor_polygon_links=show_sensor_polygon_links,
            polygon_line_color=polygon_line_color,
            polygon_line_style=polygon_line_style,
            polygon_line_width=polygon_line_width,
            polygon_line_alpha=polygon_line_alpha,
            distance_decimals=distance_decimals,
            distance_unit=distance_unit,
        )

        # second figure for PSO
        plot_scene(
            out_png=figs_dir / f"scene_compare_pso_on_impacts_{snapshots_iteration_index:04d}.png",
            title=f"PSO best_global — impacts@{snapshots_iteration_index}",
            impacts_xy=impacts,
            sensors=pso_sensors,
            env=env,
            show_detection=show_detection,
            highlight_out_of_coverage=True,
            out_of_coverage_mask=None,
            min_sensors_for_coverage=min_sensors_for_coverage,
            show_depth_labels=show_depth_labels,
            covered_color=covered_color,
            uncovered_color=uncovered_color,

            show_sensor_polygon_links=show_sensor_polygon_links,
            polygon_line_color=polygon_line_color,
            polygon_line_style=polygon_line_style,
            polygon_line_width=polygon_line_width,
            polygon_line_alpha=polygon_line_alpha,
            distance_decimals=distance_decimals,
            distance_unit=distance_unit,
        )

        print(f"Saved GA vs PSO comparison figures to: {figs_dir}")


if __name__ == "__main__":
    generate_pso_scenario_figures(
        number_of_sensors=5,
        pso_output_root="outputs",
        figures_subdir="figures_pso",

        # choose which impacts folder you want to use
        impacts_subdir="impacts_pso",
        fallback_impacts_subdir="impacts",

        plot_best_global_on_all_iterations=True,

        plot_snapshots_on_single_iteration_impacts=True,
        snapshots_iteration_index=0,
        snapshot_every=50,
        max_snapshots=14,

        plot_compare_ga_vs_pso_on_iteration=False,

        min_sensors_for_coverage=3,
        show_detection=True,
        show_depth_labels=True,
        covered_color="gold",
        uncovered_color="tab:red",

        show_sensor_polygon_links=False,  # same as your polygon example
        polygon_line_color="black",
        polygon_line_style="--",
        polygon_line_width=1.2,
        polygon_line_alpha=0.65,
        distance_decimals=1,
        distance_unit="m",

        title_prefix="PSO",
        include_metrics_in_title=True,
    )