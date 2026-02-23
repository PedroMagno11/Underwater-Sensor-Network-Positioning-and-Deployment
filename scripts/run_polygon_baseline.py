from __future__ import annotations

import json
import csv
import random
from pathlib import Path
from typing import Optional, Dict, Any, List

import numpy as np

from executable.runner import build_sound_speed_profile, load_all_settings
from geometry.grid_geometry import GridGeometry
from evaluation.cost_function import evaluate_chromosome_with_report
from evaluation.chromosome_decoder import chromosome_converter
from topologies.regular_polygon import create_regular_polygon_chromosome

from settings.environment_settings import EnvironmentSettings
from settings.simulation_settings import SimulationSettings


# =========================
# HARD-CODED SETTINGS (edit here)
# =========================

EXPERIMENT_CONFIG_PATH = "experiment_config.json"

# If you want the baseline to be saved under the same root as GA outputs,
# set this to True. It will use visualization_settings.output_directory.
USE_VIS_OUTPUT_DIR_AS_ROOT = True

# Otherwise, use this path
OUTPUT_ROOT = Path("outputs_polygon_baseline")

# Ns to generate baseline for
SENSOR_COUNTS = [3, 4, 5]

# Offsets in degrees to test (rotate the polygon). We will pick best per N.
ANGLE_OFFSETS_DEG = {
    3: [0.0, 10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 80.0, 100.0],   # within 0..120
    4: [0.0, 7.5, 15.0, 22.5, 30.0, 37.5, 45.0, 60.0, 75.0],     # within 0..90
    5: [0.0, 6.0, 12.0, 18.0, 24.0, 30.0, 36.0, 42.0, 60.0],     # within 0..72
}

# Baseline radius: None => env.target_region_radius
POLYGON_RADIUS_METERS: Optional[float] = None

# Baseline depth strategy: "mid" | "min" | "max" | "fixed"
DEPTH_STRATEGY = "mid"
FIXED_DEPTH_M: Optional[float] = None

# Use same impact points as GA (recommended)
REUSE_GA_IMPACTS = True
GA_OUTPUT_ROOT = Path("outputs")              # where your GA outputs live
IMPACTS_SOURCE_GENERATION = 0                 # which gen impacts to reuse

# If not reusing GA impacts, generate deterministic impacts (disk uniform)
GENERATE_IMPACTS_IF_MISSING = False
NUM_IMPACTS = 50
IMPACTS_SEED = 2026

# Evaluation noise seed (keep fixed for reproducibility)
EVAL_NOISE_SEED = 999

# Minimum spacing constraint for baseline validation
MIN_INTER_SENSOR_DISTANCE_M = 100.0

# =========================


def _pairwise_min_distance_xy(chromosome: np.ndarray, n: int) -> float:
    pts = chromosome.reshape(n, 3)[:, :2]
    min_d = float("inf")
    for i in range(n):
        for j in range(i + 1, n):
            d = float(np.hypot(pts[i, 0] - pts[j, 0], pts[i, 1] - pts[j, 1]))
            min_d = min(min_d, d)
    return min_d if min_d != float("inf") else 0.0


def _has_duplicate_xy(chromosome: np.ndarray, n: int) -> bool:
    pts = chromosome.reshape(n, 3)[:, :2]
    seen = set()
    for i in range(n):
        key = (float(pts[i, 0]), float(pts[i, 1]))
        if key in seen:
            return True
        seen.add(key)
    return False


def pick_depth(env: EnvironmentSettings, strategy: str, fixed: Optional[float]) -> float:
    zmin = float(env.minimum_depth_in_meters)
    zmax = float(env.maximum_depth_in_meters)

    if strategy == "mid":
        return (zmin + zmax) / 2.0
    if strategy == "min":
        return zmin
    if strategy == "max":
        return zmax
    if strategy == "fixed":
        if fixed is None:
            raise ValueError("FIXED_DEPTH_M must be set when DEPTH_STRATEGY='fixed'.")
        if not (zmin <= fixed <= zmax):
            raise ValueError(f"FIXED_DEPTH_M={fixed} out of range [{zmin},{zmax}].")
        return float(fixed)
    raise ValueError("DEPTH_STRATEGY must be one of: mid|min|max|fixed")


def load_ga_impacts(outputs_root: Path, n_sensors: int, gen: int) -> np.ndarray:
    path = outputs_root / f"sensors_{n_sensors}" / "impacts" / f"impact_points_gen_{gen:04d}.npy"
    if not path.exists():
        raise FileNotFoundError(f"GA impacts not found: {path}")
    return np.load(path)


def generate_random_impacts(env: EnvironmentSettings, out_path: Path) -> np.ndarray:
    rng = random.Random(IMPACTS_SEED)
    cx = float(env.x_center_in_meters)
    cy = float(env.y_center_in_meters)
    R = float(env.target_region_radius)

    pts = []
    for _ in range(NUM_IMPACTS):
        a = rng.random() * 2.0 * np.pi
        r = (rng.random() ** 0.5) * R  # uniform in disk
        x = cx + r * float(np.cos(a))
        y = cy + r * float(np.sin(a))
        pts.append((x, y))

    impacts = np.array(pts, dtype=float)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    np.save(out_path, impacts)
    return impacts


def to_impact_list(impacts_xy: np.ndarray):
    return [(float(x), float(y)) for x, y in impacts_xy]


def build_polygon_chromosome_validated(
    *,
    n_sensors: int,
    env: EnvironmentSettings,
    grid: GridGeometry,
    radius_m: Optional[float],
    angle_offset_deg: float,
    depth_m: float,
    min_sep_m: float,
) -> np.ndarray:
    if radius_m is None:
        radius_m = float(env.target_region_radius)

    chr_poly = create_regular_polygon_chromosome(
        number_of_sensors=n_sensors,
        environment_settings=env,
        grid_geometry=grid,
        polygon_radius_meters=float(radius_m),
        depth_meters=float(depth_m),
        angle_offset_degrees=float(angle_offset_deg),
    )

    # validate after grid quantization
    if _has_duplicate_xy(chr_poly, n_sensors):
        raise ValueError("Duplicate sensor positions after grid quantization.")
    dmin = _pairwise_min_distance_xy(chr_poly, n_sensors)
    if dmin < float(min_sep_m) - 1e-9:
        raise ValueError(f"Min spacing violated: dmin={dmin:.3f} < {min_sep_m:.3f}")

    return chr_poly


def evaluate_polygon(
    *,
    chromosome: np.ndarray,
    n_sensors: int,
    env: EnvironmentSettings,
    sim: SimulationSettings,
    grid: GridGeometry,
    ssp,
    impacts_xy: np.ndarray,
    noise_seed: int,
):
    rng = random.Random(int(noise_seed))
    rep = evaluate_chromosome_with_report(
        chromosome=chromosome,
        number_of_sensors=n_sensors,
        grid_geometry=grid,
        environment_settings=env,
        simulation_settings=sim,
        sound_speed_profile=ssp,
        random_generator=rng,
        impact_points=to_impact_list(impacts_xy),
    )
    return rep


def report_to_row(n: int, angle: float, depth_m: float, rep) -> Dict[str, Any]:
    no_cov = float(rep.number_of_impacts_without_coverage) / max(1, int(rep.number_of_impacts))
    localizable = getattr(rep, "number_of_localizable_impacts", None)
    return {
        "n_sensors": int(n),
        "angle_offset_deg": float(angle),
        "depth_m": float(depth_m),
        "total_cost": float(rep.total_cost),
        "mean_error_m": float(rep.mean_localization_error_meters),
        "no_coverage_rate": float(no_cov),
        "localizable_impacts": int(localizable) if localizable is not None else -1,
        "num_impacts": int(rep.number_of_impacts),
        "eval_noise_seed": int(EVAL_NOISE_SEED),
        "min_inter_sensor_distance_m": float(MIN_INTER_SENSOR_DISTANCE_M),
    }


def main() -> None:
    # === Load same settings as runner ===
    (
        environment_settings,
        genetic_algorithm_settings,   # unused here, but loaded to keep parity
        simulation_settings,
        performance_settings,         # unused here
        visualization_settings,
    ) = load_all_settings(EXPERIMENT_CONFIG_PATH)

    env = environment_settings
    sim = simulation_settings
    ssp = build_sound_speed_profile()
    grid = GridGeometry(env)

    # Output root: either under vis.output_directory or fixed path
    root = Path(visualization_settings.output_directory) / "polygon_baseline" if USE_VIS_OUTPUT_DIR_AS_ROOT else OUTPUT_ROOT
    root.mkdir(parents=True, exist_ok=True)

    summary: Dict[str, Any] = {"by_n": {}, "experiment_config": EXPERIMENT_CONFIG_PATH}

    for n in SENSOR_COUNTS:
        out_dir = root / f"sensors_{n}"
        out_dir.mkdir(parents=True, exist_ok=True)

        # impacts
        if REUSE_GA_IMPACTS:
            impacts = load_ga_impacts(GA_OUTPUT_ROOT, n, IMPACTS_SOURCE_GENERATION)
        else:
            impacts_path = out_dir / "impacts.npy"
            if impacts_path.exists():
                impacts = np.load(impacts_path)
            else:
                if not GENERATE_IMPACTS_IF_MISSING:
                    raise FileNotFoundError(
                        f"Missing impacts at {impacts_path}. Set REUSE_GA_IMPACTS=True or GENERATE_IMPACTS_IF_MISSING=True."
                    )
                impacts = generate_random_impacts(env, impacts_path)

        depth_m = pick_depth(env, DEPTH_STRATEGY, FIXED_DEPTH_M)

        offsets = ANGLE_OFFSETS_DEG.get(n)
        if not offsets:
            # fallback: cover symmetry sector 0..360/N
            sector = 360.0 / float(n)
            offsets = [float(x) for x in np.linspace(0.0, sector, 9)]

        rows: List[Dict[str, Any]] = []
        best_row: Optional[Dict[str, Any]] = None
        best_chr: Optional[np.ndarray] = None

        for angle in offsets:
            try:
                chr_poly = build_polygon_chromosome_validated(
                    n_sensors=n,
                    env=env,
                    grid=grid,
                    radius_m=POLYGON_RADIUS_METERS,
                    angle_offset_deg=float(angle),
                    depth_m=float(depth_m),
                    min_sep_m=float(MIN_INTER_SENSOR_DISTANCE_M),
                )
            except Exception as e:
                rows.append({
                    "n_sensors": int(n),
                    "angle_offset_deg": float(angle),
                    "depth_m": float(depth_m),
                    "total_cost": float("inf"),
                    "mean_error_m": float("inf"),
                    "no_coverage_rate": 1.0,
                    "localizable_impacts": -1,
                    "num_impacts": int(impacts.shape[0]),
                    "eval_noise_seed": int(EVAL_NOISE_SEED),
                    "min_inter_sensor_distance_m": float(MIN_INTER_SENSOR_DISTANCE_M),
                    "error": str(e),
                })
                continue

            rep = evaluate_polygon(
                chromosome=chr_poly,
                n_sensors=n,
                env=env,
                sim=sim,
                grid=grid,
                ssp=ssp,
                impacts_xy=impacts,
                noise_seed=EVAL_NOISE_SEED,
            )

            row = report_to_row(n, float(angle), float(depth_m), rep)
            rows.append(row)

            if best_row is None or row["total_cost"] < best_row["total_cost"]:
                best_row = row
                best_chr = chr_poly

        # Save all offsets CSV
        csv_path = out_dir / "polygon_baseline_metrics.csv"
        fieldnames = sorted({k for r in rows for k in r.keys()})
        with csv_path.open("w", newline="", encoding="utf-8") as f:
            w = csv.DictWriter(f, fieldnames=fieldnames)
            w.writeheader()
            w.writerows(rows)

        if best_row is None or best_chr is None:
            raise RuntimeError(f"No valid polygon baseline found for N={n}. Try adjusting offsets or radius.")

        # Save best chromosome
        best_chr_path = out_dir / "best_polygon_chromosome.npy"
        np.save(best_chr_path, best_chr)

        # Save best sensors
        sensors = chromosome_converter(best_chr, n, grid, env)
        sensors_json = [
            {"x": float(s.position_x), "y": float(s.position_y), "z": float(getattr(s, "position_z", depth_m))}
            for s in sensors
        ]
        with (out_dir / "best_polygon_sensors.json").open("w", encoding="utf-8") as f:
            json.dump(sensors_json, f, indent=2)

        summary["by_n"][str(n)] = {
            "best_angle_offset_deg": best_row["angle_offset_deg"],
            "best_total_cost": best_row["total_cost"],
            "best_mean_error_m": best_row["mean_error_m"],
            "best_no_coverage_rate": best_row["no_coverage_rate"],
            "depth_m": depth_m,
            "num_impacts": best_row["num_impacts"],
            "csv": str(csv_path.as_posix()),
            "best_chr": str(best_chr_path.as_posix()),
        }

        print(
            f"[Polygon baseline N={n}] best offset={best_row['angle_offset_deg']:.2f} deg | "
            f"cost={best_row['total_cost']:.3f} | mean_err={best_row['mean_error_m']:.2f} m"
        )

    with (root / "polygon_baseline_summary.json").open("w", encoding="utf-8") as f:
        json.dump(summary, f, indent=2)

    print(f"Saved polygon baseline outputs at: {root.resolve()}")


if __name__ == "__main__":
    main()