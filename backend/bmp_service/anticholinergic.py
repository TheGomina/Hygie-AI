"""Compute anticholinergic burden (CIA/ACB scores) from OMÉDIT CSVs.

We read all CSV files whose name contains "CIA-ACB" in *backend/resources* and
build a mapping DCI -> max(score_CIA, score_ACB). The compute function is
cached so the overhead happens only at first use.
"""

from __future__ import annotations

import csv
from functools import lru_cache
from pathlib import Path
from typing import Dict, List


def _resources_dir() -> Path:  # noqa: D401
    # backend/bmp_service/anticholinergic.py -> ../../resources
    return Path(__file__).resolve().parents[2] / "resources"


@lru_cache(maxsize=1)
def _load_scores() -> Dict[str, int]:  # noqa: D401
    mapping: Dict[str, int] = {}
    for fp in _resources_dir().glob("*CIA-ACB*.csv"):
        try:
            with fp.open(encoding="utf-8", newline="") as fh:
                reader = csv.reader(fh, delimiter=";")
                for row in reader:
                    if not row or row[0].startswith(";"):
                        continue  # skip comments/headers
                    name = row[0].strip().upper()
                    try:
                        score_cia = int(row[1]) if row[1] else 0
                    except ValueError:
                        score_cia = 0
                    try:
                        score_acb = int(row[3]) if row[3] else 0
                    except ValueError:
                        score_acb = 0
                    score = max(score_cia, score_acb)
                    if score:
                        mapping[name] = max(mapping.get(name, 0), score)
        except Exception:
            continue  # ignore malformed files
    return mapping


def compute_burden_score(meds: List[str]) -> int:  # noqa: D401
    """Return cumulative anticholinergic burden for *meds* list (uppercased)."""
    scores = _load_scores()
    total = 0
    for m in meds:
        total += scores.get(m.upper(), 0)
    return total
