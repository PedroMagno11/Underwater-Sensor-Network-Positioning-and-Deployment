from __future__ import annotations

import json
from dataclasses import asdict, is_dataclass
from pathlib import Path
from typing import Any, Dict, Optional

from evaluation.evaluation_report import EvaluationReport


def _to_jsonable(obj: Any) -> Any:
    if is_dataclass(obj):
        return asdict(obj)
    if isinstance(obj, (list, tuple)):
        return [_to_jsonable(x) for x in obj]
    if isinstance(obj, dict):
        return {str(k): _to_jsonable(v) for k, v in obj.items()}
    return obj


def write_best_reports_jsonl(
    *,
    output_path: str,
    generation_index: int,
    number_of_sensors: int,
    best_of_generation: EvaluationReport,
    best_global: EvaluationReport,
    include_impact_points: bool = False,
) -> None:
    """
    Appends one JSON line per generation with:
      - best report of the generation
      - best global report so far
    """
    path = Path(output_path)
    path.parent.mkdir(parents=True, exist_ok=True)

    def rep_to_dict(rep: EvaluationReport) -> Dict[str, Any]:
        d = _to_jsonable(rep)
        if not include_impact_points:
            d.pop("impact_points", None)
        return d

    row = {
        "generation_index": generation_index,
        "number_of_sensors": number_of_sensors,
        "best_of_generation": rep_to_dict(best_of_generation),
        "best_global": rep_to_dict(best_global),
    }

    with path.open("a", encoding="utf-8") as f:
        f.write(json.dumps(row, ensure_ascii=False) + "\n")
