"""Load large ANSM interactions thesaurus into an in-memory mapping.

Parsing strategy (fast heuristics):
1. Look for a CSV in resources named *interactions*; if present, load it.
2. Else try to extract text from the PDF and parse lines containing “/”.
   We keep only substance–substance pairs (uppercase, alphabetical order).

The function is cached; on failure, returns empty dict to avoid crashing.
"""

from __future__ import annotations

import csv
import re
from functools import lru_cache
from pathlib import Path
from typing import Dict, Tuple

from pdfminer.high_level import extract_text  # type: ignore

_RES = Path(__file__).resolve().parents[2] / "resources"

_PAIR_RE = re.compile(
    r"^(?P<a>[A-ZÀÂÉÈÊÎÔÙÛÇ\- ]{3,})\s*/\s*(?P<b>[A-ZÀÂÉÈÊÎÔÙÛÇ\- ]{3,})", re.I
)


def _order(a: str, b: str) -> Tuple[str, str]:  # noqa: D401
    return tuple(sorted((a.upper(), b.upper())))  # type: ignore[return-value]


def _load_from_csv(fp: Path) -> Dict[Tuple[str, str], str]:  # noqa: D401
    mapping: Dict[Tuple[str, str], str] = {}
    with fp.open(encoding="utf-8", newline="") as fh:
        reader = csv.reader(fh, delimiter=";")
        for row in reader:
            if len(row) < 3:
                continue
            a, b, gravite = row[0].strip(), row[1].strip(), row[2].strip()
            if not a or not b:
                continue
            mapping[_order(a, b)] = f"Interaction ANSM ({gravite})"
    return mapping


def _load_from_pdf(fp: Path) -> Dict[Tuple[str, str], str]:  # noqa: D401
    mapping: Dict[Tuple[str, str], str] = {}
    try:
        text = extract_text(str(fp))
    except Exception:  # pragma: no cover – avoid hard crash if pdfminer fails
        return {}

    for line in text.splitlines():
        m = _PAIR_RE.match(line.strip())
        if not m:
            continue
        a, b = m.group("a"), m.group("b")
        mapping[_order(a, b)] = "Interaction ANSM (gravité non classée)"
    return mapping


@lru_cache(maxsize=1)
def get_interactions() -> Dict[Tuple[str, str], str]:  # noqa: D401
    """Return dict{(A,B): description}. Empty on failure."""
    # Priority: CSV → PDF
    for fp in _RES.glob("*interaction*.csv"):
        data = _load_from_csv(fp)
        if data:
            return data
    for fp in _RES.glob("*interaction*.pdf"):
        data = _load_from_pdf(fp)
        if data:
            return data
    return {}
