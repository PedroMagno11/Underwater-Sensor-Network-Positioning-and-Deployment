from __future__ import annotations

import json
from pathlib import Path
import numpy as np
import matplotlib.pyplot as plt


def load_jsonl(path: Path) -> list[dict]:
    rows = []
    with path.open("r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            rows.append(json.loads(line))
    return rows


def plot_curve(x: np.ndarray, y: np.ndarray, out_png: Path, title: str, ylabel: str) -> None:
    plt.figure()
    plt.plot(x, y)
    plt.grid(True, alpha=0.3)
    plt.xlabel("Generation")
    plt.ylabel(ylabel)
    plt.title(title)
    out_png.parent.mkdir(parents=True, exist_ok=True)
    plt.savefig(out_png, dpi=300, bbox_inches="tight")
    plt.close()


def run(output_root: str, number_of_sensors: int) -> None:
    outdir = Path(output_root) / f"sensors_{number_of_sensors}"
    figs_dir = outdir / "figures"
    jsonl_path = outdir / "best_reports.jsonl"

    if not jsonl_path.exists():
        raise FileNotFoundError(f"Missing {jsonl_path}")

    rows = load_jsonl(jsonl_path)

    gen = np.array([r["generation_index"] for r in rows], dtype=int)

    best_global_cost = np.array([r["best_global"]["total_cost"] for r in rows], dtype=float)
    best_gen_cost = np.array([r["best_of_generation"]["total_cost"] for r in rows], dtype=float)

    best_global_err = np.array([r["best_global"]["mean_localization_error_meters"] for r in rows], dtype=float)
    best_gen_err = np.array([r["best_of_generation"]["mean_localization_error_meters"] for r in rows], dtype=float)

    best_global_nocov = np.array(
        [r["best_global"]["number_of_impacts_without_coverage"] / max(1, r["best_global"]["number_of_impacts"]) for r in rows],
        dtype=float,
    )

    plot_curve(
        gen, best_global_cost,
        figs_dir / "curve_best_global_cost.png",
        "GA Convergence — Best Global Cost",
        "Best global cost",
    )

    plot_curve(
        gen, best_gen_cost,
        figs_dir / "curve_best_generation_cost.png",
        "GA — Best Cost per Generation",
        "Best-of-generation cost",
    )

    plot_curve(
        gen, best_global_err,
        figs_dir / "curve_best_global_mean_error.png",
        "GA — Best Global Mean Localization Error",
        "Mean error (m)",
    )

    plot_curve(
        gen, best_global_nocov,
        figs_dir / "curve_best_global_no_coverage_rate.png",
        "GA — Best Global No-Coverage Rate",
        "No-coverage rate",
    )

    print(f"[OK] Saved curves to: {figs_dir}")


if __name__ == "__main__":
    run(output_root="outputs", number_of_sensors=3)
