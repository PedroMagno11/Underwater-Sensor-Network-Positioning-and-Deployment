from __future__ import annotations

import csv
import json
import math
from pathlib import Path
from typing import Any, Dict, List, Tuple, Optional

import numpy as np
import matplotlib.pyplot as plt


# =========================
# HARD-CODED SETTINGS (edit here)
# =========================

GA_OUTPUT_ROOT = Path("outputs")
POLY_OUTPUT_ROOT = Path("outputs_polygon_baseline")
OUT_ROOT = Path("outputs_comparison")

SENSOR_COUNTS = [3, 4, 5]

# GA file produced by your pipeline (write_best_reports_jsonl)
GA_REPORTS_JSONL_REL = Path("best_reports.jsonl")

# Polygon baseline file produced by run_polygon_baseline.py
POLY_METRICS_REL = Path("polygon_baseline_metrics.csv")

# GA labels (internal names used in compare)
GA_LABEL_BEST_GEN = "GA_best_gen"
GA_LABEL_BEST_GLOBAL = "GA_best_global"
POLY_LABEL = "polygon"  # informational only

# Plots
SAVE_PLOTS = True
DPI = 300

# Behavior if files are missing
SKIP_MISSING_FILES = False

# =========================


# ---------- IO helpers ----------

def read_csv_dicts(path: Path) -> List[Dict[str, Any]]:
    if not path.exists():
        raise FileNotFoundError(f"CSV not found: {path}")
    with path.open("r", encoding="utf-8", newline="") as f:
        r = csv.DictReader(f)
        if r.fieldnames is None:
            raise RuntimeError(f"CSV has no header: {path}")
        return [dict(row) for row in r]


def write_csv(path: Path, rows: List[Dict[str, Any]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    if not rows:
        path.write_text("", encoding="utf-8")
        return
    fields = list(rows[0].keys())
    with path.open("w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=fields)
        w.writeheader()
        w.writerows(rows)


def read_jsonl(path: Path) -> List[Dict[str, Any]]:
    if not path.exists():
        raise FileNotFoundError(f"JSONL not found: {path}")
    rows: List[Dict[str, Any]] = []
    with path.open("r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            rows.append(json.loads(line))
    return rows


# ---------- parsing helpers ----------

def try_float(x: Any, default: float = float("nan")) -> float:
    try:
        if x is None:
            return default
        s = str(x).strip()
        if s == "":
            return default
        return float(s)
    except Exception:
        return default


def try_int(x: Any, default: int = 0) -> int:
    try:
        if x is None:
            return default
        s = str(x).strip()
        if s == "":
            return default
        return int(float(s))  # handles "12.0"
    except Exception:
        return default


def finite(arr: np.ndarray) -> np.ndarray:
    arr = np.asarray(arr, dtype=float)
    return arr[np.isfinite(arr)]


def fmt(x: float, nd: int = 3) -> str:
    if x is None:
        return "NaN"
    try:
        xf = float(x)
    except Exception:
        return "NaN"
    return f"{xf:.{nd}f}" if math.isfinite(xf) else "NaN"


def require_columns(rows: List[Dict[str, Any]], required: List[str], where: str) -> None:
    if not rows:
        raise RuntimeError(f"{where}: file has 0 rows.")
    cols = set(rows[0].keys())
    missing = [c for c in required if c not in cols]
    if missing:
        raise RuntimeError(f"{where}: missing columns={missing}. Found={sorted(cols)}")


# ---------- Polygon baseline ----------

def pick_best_polygon_row(poly_rows: List[Dict[str, Any]]) -> Dict[str, Any]:
    """
    Choose polygon row with minimum finite total_cost.
    """
    require_columns(poly_rows, ["total_cost"], where="Polygon CSV")

    best_row: Optional[Dict[str, Any]] = None
    best_cost = float("inf")

    for row in poly_rows:
        cost = try_float(row.get("total_cost"), float("inf"))
        if not math.isfinite(cost):
            continue
        if cost < best_cost:
            best_cost = cost
            best_row = row

    if best_row is None:
        raise RuntimeError("No valid polygon baseline row found (all total_cost invalid).")
    return best_row


# ---------- GA: best_reports.jsonl -> compare rows ----------

def _extract_report_fields(report_obj: Dict[str, Any]) -> Dict[str, float]:
    """
    Extracts the metrics we need from a serialized EvaluationReport (dict).
    Your code uses EvaluationReport fields like:
      - total_cost
      - mean_localization_error_meters
      - number_of_impacts_without_coverage
      - number_of_impacts

    This tries a few alternative key names to be robust.
    """
    total_cost = try_float(report_obj.get("total_cost", report_obj.get("cost")))
    mean_err = try_float(
        report_obj.get(
            "mean_localization_error_meters",
            report_obj.get("mean_error_m", report_obj.get("avg_error_meters")),
        )
    )

    n_impacts = try_int(report_obj.get("number_of_impacts", report_obj.get("num_impacts", 0)), 0)
    n_no_cov = try_int(
        report_obj.get("number_of_impacts_without_coverage", report_obj.get("impacts_without_coverage", 0)),
        0,
    )

    no_cov_rate = (float(n_no_cov) / float(n_impacts)) if n_impacts > 0 else float("nan")

    return {
        "total_cost": float(total_cost),
        "mean_error_m": float(mean_err),
        "no_coverage_rate": float(no_cov_rate),
    }


def ga_rows_from_best_reports_jsonl(best_reports_jsonl: Path) -> List[Dict[str, Any]]:
    """
    Converts GA best_reports.jsonl into rows compatible with compare.
    Produces two labels:
      - GA_best_gen: best_of_generation per generation
      - GA_best_global: best_global per generation
    """
    items = read_jsonl(best_reports_jsonl)
    out: List[Dict[str, Any]] = []

    for it in items:
        gen = try_int(it.get("generation_index", it.get("generation", 0)), 0)

        bog = it.get("best_of_generation")
        bg = it.get("best_global")

        if isinstance(bog, dict):
            f = _extract_report_fields(bog)
            out.append({"label": GA_LABEL_BEST_GEN, "generation": gen, **f})

        if isinstance(bg, dict):
            f = _extract_report_fields(bg)
            out.append({"label": GA_LABEL_BEST_GLOBAL, "generation": gen, **f})

    if not out:
        # Help debugging: show top-level keys from first item if available
        if items:
            keys = sorted(items[0].keys())
            raise RuntimeError(
                f"Parsed {best_reports_jsonl} but produced 0 rows. "
                f"First item keys: {keys}"
            )
        raise RuntimeError(f"Parsed {best_reports_jsonl} but it has 0 items.")

    return out


# ---------- Aggregation ----------

def aggregate_ga(ga_rows: List[Dict[str, Any]], label: str) -> Dict[str, float]:
    """
    Aggregate over generations for a given GA label.
    Expects: label, generation, mean_error_m, total_cost, no_coverage_rate
    """
    require_columns(
        ga_rows,
        ["label", "generation", "mean_error_m", "total_cost", "no_coverage_rate"],
        where="GA rows (from JSONL)",
    )

    sub = [r for r in ga_rows if r.get("label") == label]
    if not sub:
        labels_found = sorted({r.get("label", "") for r in ga_rows})
        raise RuntimeError(f"No GA rows for label='{label}'. Labels found: {labels_found}")

    gens = np.array([try_int(r.get("generation"), 0) for r in sub], dtype=int)
    order = np.argsort(gens)
    sub_sorted = [sub[i] for i in order]
    last = sub_sorted[-1]

    mean_errors = finite(np.array([try_float(r.get("mean_error_m")) for r in sub_sorted], dtype=float))
    total_costs = finite(np.array([try_float(r.get("total_cost")) for r in sub_sorted], dtype=float))
    no_cov = finite(np.array([try_float(r.get("no_coverage_rate")) for r in sub_sorted], dtype=float))

    def safe_mean(a: np.ndarray) -> float:
        return float(np.mean(a)) if a.size else float("nan")

    def safe_min(a: np.ndarray) -> float:
        return float(np.min(a)) if a.size else float("nan")

    def safe_max(a: np.ndarray) -> float:
        return float(np.max(a)) if a.size else float("nan")

    return {
        "final_mean_error_m": try_float(last.get("mean_error_m")),
        "final_total_cost": try_float(last.get("total_cost")),
        "final_no_coverage_rate": try_float(last.get("no_coverage_rate")),
        "avg_mean_error_m": safe_mean(mean_errors),
        "avg_total_cost": safe_mean(total_costs),
        "avg_no_coverage_rate": safe_mean(no_cov),
        "best_total_cost": safe_min(total_costs),
        "best_mean_error_m": safe_min(mean_errors),
        "worst_mean_error_m": safe_max(mean_errors),
        "num_generations": float(len(sub_sorted)),
        "first_generation": float(int(gens[order][0])) if len(gens) else float("nan"),
        "last_generation": float(int(gens[order][-1])) if len(gens) else float("nan"),
    }


# ---------- LaTeX ----------

def write_latex_table(path: Path, caption: str, label: str, rows: List[Dict[str, str]]) -> None:
    lines: List[str] = []
    lines.append(r"\begin{table}[t]")
    lines.append(r"\centering")
    lines.append(rf"\caption{{{caption}}}")
    lines.append(rf"\label{{{label}}}")
    lines.append(r"\begin{tabular}{lcc}")
    lines.append(r"\hline")
    lines.append(r"Metric & GA & Regular Polygon \\")
    lines.append(r"\hline")
    for row in rows:
        lines.append(f"{row['metric']} & {row['ga']} & {row['poly']} \\\\")
    lines.append(r"\hline")
    lines.append(r"\end{tabular}")
    lines.append(r"\end{table}")

    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines), encoding="utf-8")


# ---------- Plots ----------

def plot_convergence(
    *,
    out_png: Path,
    ga_rows: List[Dict[str, Any]],
    poly_best_cost: float,
    title: str,
) -> None:
    def series(label: str) -> Tuple[np.ndarray, np.ndarray]:
        sub = [r for r in ga_rows if r.get("label") == label]
        if not sub:
            return np.array([], dtype=int), np.array([], dtype=float)

        gens = np.array([try_int(r.get("generation", 0), 0) for r in sub], dtype=int)
        costs = np.array([try_float(r.get("total_cost")) for r in sub], dtype=float)

        order = np.argsort(gens)
        gens = gens[order]
        costs = costs[order]

        mask = np.isfinite(costs)
        return gens[mask], costs[mask]

    g_gen, c_gen = series(GA_LABEL_BEST_GEN)
    g_glob, c_glob = series(GA_LABEL_BEST_GLOBAL)

    # Ensure global curve is best-so-far (monotonic, converging)
    c_best_so_far = np.minimum.accumulate(c_glob) if c_glob.size else c_glob

    plt.figure()

    # GA best per generation (line, lighter)
    if c_gen.size:
        plt.plot(
            g_gen,
            c_gen,
            linewidth=1.2,
            alpha=0.55,
            label="GA best (per generation)",
        )

    # GA global best-so-far (orange, emphasized)
    if c_best_so_far.size:
        plt.plot(
            g_glob,
            c_best_so_far,
            color="tab:orange",
            linewidth=0.6,
            label="GA best-so-far (global)",
        )

    # Polygon reference
    if math.isfinite(poly_best_cost):
        plt.axhline(
            poly_best_cost,
            linestyle="--",
            linewidth=1.6,
            label="Regular polygon (best offset)",
        )

    plt.grid(True, alpha=0.3)
    plt.xlabel("Generation")
    plt.ylabel("Total cost")
    plt.title(title)
    plt.legend(loc="best")

    out_png.parent.mkdir(parents=True, exist_ok=True)
    plt.savefig(out_png, dpi=DPI, bbox_inches="tight")
    plt.close()

def plot_boxplot_mean_error(*, out_png: Path, ga_rows: List[Dict[str, Any]], poly_best_mean_error: float, title: str) -> None:
    def values(label: str) -> np.ndarray:
        sub = [r for r in ga_rows if r.get("label") == label]
        return finite(np.array([try_float(r.get("mean_error_m")) for r in sub], dtype=float))

    v_best_gen = values(GA_LABEL_BEST_GEN)
    v_best_global = values(GA_LABEL_BEST_GLOBAL)

    if v_best_gen.size == 0 and v_best_global.size == 0:
        return

    data = []
    labels = []
    if v_best_gen.size:
        data.append(v_best_gen)
        labels.append("GA best/gen")
    if v_best_global.size:
        data.append(v_best_global)
        labels.append("GA best/global")

    plt.figure()
    plt.boxplot(data, tick_labels=labels, showmeans=True)

    if math.isfinite(poly_best_mean_error):
        plt.axhline(poly_best_mean_error, linestyle="--", label="Regular polygon (best offset)")

    plt.grid(True, alpha=0.3)
    plt.ylabel("Mean localization error (m)")
    plt.title(title)
    plt.legend(loc="best")

    out_png.parent.mkdir(parents=True, exist_ok=True)
    plt.savefig(out_png, dpi=DPI, bbox_inches="tight")
    plt.close()


# ---------- Main ----------

def main() -> None:
    OUT_ROOT.mkdir(parents=True, exist_ok=True)

    all_summary: Dict[str, Any] = {"by_n": {}}
    all_rows_csv: List[Dict[str, Any]] = []

    for n in SENSOR_COUNTS:
        ga_jsonl = GA_OUTPUT_ROOT / f"sensors_{n}" / GA_REPORTS_JSONL_REL
        poly_csv = POLY_OUTPUT_ROOT / f"sensors_{n}" / POLY_METRICS_REL

        try:
            ga_rows = ga_rows_from_best_reports_jsonl(ga_jsonl)
            poly_rows = read_csv_dicts(poly_csv)
        except FileNotFoundError as e:
            if SKIP_MISSING_FILES:
                print(f"[N={n}] SKIP missing file: {e}")
                continue
            raise

        poly_best = pick_best_polygon_row(poly_rows)

        poly_best_cost = try_float(poly_best.get("total_cost"))
        poly_best_mean_error = try_float(poly_best.get("mean_error_m"))
        poly_best_no_cov = try_float(poly_best.get("no_coverage_rate"))

        ga_best_gen = aggregate_ga(ga_rows, GA_LABEL_BEST_GEN)
        ga_best_global = aggregate_ga(ga_rows, GA_LABEL_BEST_GLOBAL)

        # Report as final: GA best global (most defensible for "final solution")
        ga_report = ga_best_global

        out_dir = OUT_ROOT / f"sensors_{n}"
        out_dir.mkdir(parents=True, exist_ok=True)

        # LaTeX (per N)
        latex_rows = [
            {"metric": "Final total cost", "ga": fmt(ga_report["final_total_cost"], 3), "poly": fmt(poly_best_cost, 3)},
            {"metric": "Final mean error (m)", "ga": fmt(ga_report["final_mean_error_m"], 2), "poly": fmt(poly_best_mean_error, 2)},
            {"metric": r"Final no-coverage rate (\%)", "ga": fmt(100.0 * ga_report["final_no_coverage_rate"], 1), "poly": fmt(100.0 * poly_best_no_cov, 1)},
            {"metric": "Best mean error (m)", "ga": fmt(ga_report["best_mean_error_m"], 2), "poly": fmt(poly_best_mean_error, 2)},
            {"metric": "Avg. mean error (m)", "ga": fmt(ga_report["avg_mean_error_m"], 2), "poly": fmt(poly_best_mean_error, 2)},
        ]

        latex_path = out_dir / "table_ga_vs_polygon.tex"
        write_latex_table(
            latex_path,
            caption=f"GA vs. Regular Polygon Baseline (Deterministic Evaluation), $N={n}$.",
            label=f"tab:ga_vs_polygon_n{n}",
            rows=latex_rows,
        )

        # Per-N CSV summary
        per_n_csv = out_dir / "summary.csv"
        per_n_rows = [{
            "N": n,
            "ga_jsonl": str(ga_jsonl.as_posix()),
            "poly_csv": str(poly_csv.as_posix()),
            "ga_final_total_cost": ga_report["final_total_cost"],
            "ga_final_mean_error_m": ga_report["final_mean_error_m"],
            "ga_final_no_coverage_rate": ga_report["final_no_coverage_rate"],
            "ga_best_mean_error_m": ga_report["best_mean_error_m"],
            "ga_avg_mean_error_m": ga_report["avg_mean_error_m"],
            "poly_best_total_cost": poly_best_cost,
            "poly_best_mean_error_m": poly_best_mean_error,
            "poly_best_no_coverage_rate": poly_best_no_cov,
            "ga_generations": ga_report["num_generations"],
            "ga_first_generation": ga_report["first_generation"],
            "ga_last_generation": ga_report["last_generation"],
        }]
        write_csv(per_n_csv, per_n_rows)

        all_rows_csv.extend(per_n_rows)

        # Plots
        if SAVE_PLOTS:
            plot_convergence(
                out_png=out_dir / "convergence_cost.png",
                ga_rows=ga_rows,
                poly_best_cost=poly_best_cost,
                title=f"Convergence (N={n} sensors): GA vs Regular Polygon",
            )
            plot_boxplot_mean_error(
                out_png=out_dir / "boxplot_mean_error.png",
                ga_rows=ga_rows,
                poly_best_mean_error=poly_best_mean_error,
                title=f"Mean error distribution over generations (N={n})",
            )

        # JSON summary (debug-friendly)
        all_summary["by_n"][str(n)] = {
            "ga_jsonl": str(ga_jsonl.as_posix()),
            "polygon_csv": str(poly_csv.as_posix()),
            "polygon_best": poly_best,
            "ga_best_gen_agg": ga_best_gen,
            "ga_best_global_agg": ga_best_global,
            "latex_table": str(latex_path.as_posix()),
            "per_n_summary_csv": str(per_n_csv.as_posix()),
            "plots": {
                "convergence_cost": str((out_dir / "convergence_cost.png").as_posix()),
                "boxplot_mean_error": str((out_dir / "boxplot_mean_error.png").as_posix()),
            },
        }

        print(f"[N={n}] wrote: {latex_path} | {per_n_csv}")

    # Global outputs
    summary_json = OUT_ROOT / "comparison_summary.json"
    summary_json.write_text(json.dumps(all_summary, indent=2), encoding="utf-8")
    print(f"Saved JSON summary at: {summary_json}")

    summary_all_csv = OUT_ROOT / "summary_all.csv"
    write_csv(summary_all_csv, all_rows_csv)
    print(f"Saved global CSV at: {summary_all_csv}")

    # Global LaTeX table (one row per N)
    if all_rows_csv:
        lines = []
        lines.append(r"\begin{table}[t]")
        lines.append(r"\centering")
        lines.append(r"\caption{GA vs. Regular Polygon Baseline across sensor counts (Deterministic Evaluation).}")
        lines.append(r"\label{tab:ga_vs_polygon_all}")
        lines.append(r"\begin{tabular}{ccccccc}")
        lines.append(r"\hline")
        lines.append(r"$N$ & GA cost & Poly cost & GA err (m) & Poly err (m) & GA no-cov (\%) & Poly no-cov (\%) \\")
        lines.append(r"\hline")

        for r in all_rows_csv:
            n = int(r["N"])
            ga_cost = fmt(float(r["ga_final_total_cost"]), 3)
            po_cost = fmt(float(r["poly_best_total_cost"]), 3)
            ga_err = fmt(float(r["ga_final_mean_error_m"]), 2)
            po_err = fmt(float(r["poly_best_mean_error_m"]), 2)
            ga_nc = fmt(100.0 * float(r["ga_final_no_coverage_rate"]), 1)
            po_nc = fmt(100.0 * float(r["poly_best_no_coverage_rate"]), 1)
            lines.append(f"{n} & {ga_cost} & {po_cost} & {ga_err} & {po_err} & {ga_nc} & {po_nc} \\\\")

        lines.append(r"\hline")
        lines.append(r"\end{tabular}")
        lines.append(r"\end{table}")

        latex_all_path = OUT_ROOT / "table_ga_vs_polygon_all.tex"
        latex_all_path.write_text("\n".join(lines), encoding="utf-8")
        print(f"Saved global LaTeX table at: {latex_all_path}")


if __name__ == "__main__":
    main()