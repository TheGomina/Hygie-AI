"""Compute anticholinergic burden (CIA/ACB scores) from OMÉDIT CSVs.

We read all CSV files whose name contains "CIA-ACB" in *backend/resources* and
build a mapping DCI -> max(score_CIA, score_ACB). The compute function is
cached so the overhead happens only at first use.
"""

from __future__ import annotations

import csv
from pathlib import Path
from typing import Dict, List

_SCORES_CACHE: Dict[str, int] | None = None
_CACHE_DIR: Path | None = None


def _resources_dir() -> Path:  # noqa: D401
    # backend/bmp_service/anticholinergic.py -> ../../resources
    return Path(__file__).resolve().parents[2] / "resources"


def _load_scores() -> Dict[str, int]:  # noqa: D401
    """Load (and cache) CIA/ACB scores as a *dict* keyed by upper-cased DCI.

    The cache is automatically invalidated if the directory returned by
    ``_resources_dir()`` changes (useful for unit tests that monkey-patch this
    helper). This provides deterministic behaviour for the real application
    while allowing tests to supply isolated fixture CSVs without having to
    manually clear any caches.
    """
    global _SCORES_CACHE, _CACHE_DIR  # noqa: PLW0603 – tracked at module level

    current_dir = _resources_dir()
    # Some unit tests monkey-patch _resources_dir() with a dummy object that
    # does not implement Path.resolve().  Make the function tolerant.
    try:
        current_dir_resolved = current_dir.resolve()  # type: ignore[attr-defined]
    except AttributeError:
        current_dir_resolved = current_dir  # fallback to original object

    # Short-circuit when cached for the same directory object (identity or path)
    if _SCORES_CACHE is not None and _CACHE_DIR == current_dir_resolved:
        return _SCORES_CACHE

    mapping: Dict[str, int] = {}
    # If *current_dir* is a dummy test object that only exposes .glob, we rely
    # on duck typing.
    for fp in current_dir_resolved.glob("*CIA-ACB*.csv"):
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
            # Malformed file: ignore and move on (robust to hospital-specific
            # CSV quirks).
            continue

    _SCORES_CACHE = mapping
    _CACHE_DIR = current_dir_resolved
    return mapping


# -------------------------------------------------------------------------
# Expose cache_clear utility so pytest fixtures can reset between tests.
# -------------------------------------------------------------------------

def _cache_clear() -> None:  # noqa: D401
    """Clear internal score caches (used by unit tests)."""
    global _SCORES_CACHE, _CACHE_DIR  # noqa: PLW0603
    _SCORES_CACHE = None
    _CACHE_DIR = None

# Attach method to function object to emulate lru_cache interface expected by
# existing unit tests, e.g. ``_load_scores.cache_clear()``.
_load_scores.cache_clear = _cache_clear  # type: ignore[attr-defined]


def compute_burden_score(meds: List[str]) -> int:  # noqa: D401
    """Return cumulative anticholinergic burden for *meds* (case-insensitive)."""
    scores = _load_scores()
    return sum(scores.get(m.upper(), 0) for m in meds)
