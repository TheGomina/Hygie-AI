"""Load large ANSM interactions thesaurus into an in-memory mapping.

Parsing strategy (fast heuristics):
1. Look for a CSV in resources named *interactions*; if present, load it.
2. Else try to extract text from the PDF and parse lines containing “/”.
   We keep only substance–substance pairs (uppercase, alphabetical order).

The function is cached; on failure, returns empty dict to avoid crashing.
"""

from __future__ import annotations

import csv as _csv
import re
from functools import lru_cache
from pathlib import Path
from typing import Dict, Tuple, Iterable

from pdfminer.high_level import extract_text  # type: ignore
from prometheus_client import Histogram

_RES = Path(__file__).resolve().parents[2] / "resources"

_PAIR_RE = re.compile(
    r"^(?P<a>[A-ZÀÂÉÈÊÎÔÙÛÇ\- ]+)\s*/\s*(?P<b>[A-ZÀÂÉÈÊÎÔÙÛÇ\- ]+)", re.I
)

CSV_LOAD_TIME = Histogram(
    "bmp_csv_load_seconds", "Time spent loading CSV interactions"
)
PDF_LOAD_TIME = Histogram(
    "bmp_pdf_load_seconds", "Time spent loading PDF interactions"
)


def _order(a: str, b: str) -> Tuple[str, str]:  # noqa: D401
    return tuple(sorted((a.upper(), b.upper())))  # type: ignore[return-value]


@CSV_LOAD_TIME.time()
def _load_from_csv(fp: Path) -> Dict[Tuple[str, str], str]:
    """Load ANSM interactions from CSV, filtering gravities C/D (≈clinically
    significatives).  The parser adapts to the file size to minimise latency.
    """

    def _iter_rows(reader: Iterable[list[str]]) -> Dict[Tuple[str, str], str]:
        mapping: Dict[Tuple[str, str], str] = {}
        for row in reader:
            if len(row) < 3:
                continue
            a, b, grav = row[0].strip(), row[1].strip(), row[2].strip().upper()
            if not a or not b or grav not in {"C", "D"}:
                continue
            mapping[_order(a, b)] = f"Interaction ANSM ({grav})"
        return mapping

    try:
        size = fp.stat().st_size
    except Exception:
        size = 0

    # Fast path: small/medium file → pure Python csv (avoid heavy pandas import)
    if size < 200_000:
        try:
            with fp.open("r", encoding="utf-8", newline="") as fh:
                reader = _csv.reader(fh, delimiter=";")
                return _iter_rows(reader)
        except Exception:
            return {}

    # Fallback to pandas for larger files where vectorised parsing is faster
    try:
        import pandas as pd  # type: ignore
    except ModuleNotFoundError:  # pragma: no cover – pandas may be optional
        pd = None  # type: ignore[assignment]
    if pd is None:
        return {}

    try:
        df = pd.read_csv(
            fp,
            sep=";",
            header=None,
            names=["a", "b", "gravite"],
            usecols=[0, 1, 2],
            dtype=str,
            encoding="utf-8",
        )
    except Exception:
        return {}

    df = df.dropna(subset=["a", "b", "gravite"])
    df["gravite"] = df["gravite"].astype(str).str.strip().str.upper()
    df = df[df["gravite"].isin(["C", "D"])]
    mapping: Dict[Tuple[str, str], str] = {}
    for a, b, grav in zip(df["a"], df["b"], df["gravite"], strict=False):
        if not a or not b:
            continue
        mapping[_order(str(a).strip(), str(b).strip())] = f"Interaction ANSM ({grav})"
    return mapping


@PDF_LOAD_TIME.time()
def _load_from_pdf(fp: Path) -> Dict[Tuple[str, str], str]:  # noqa: D401
    mapping: Dict[Tuple[str, str], str] = {}
    try:
        text = extract_text(str(fp))
    except Exception:  # pragma: no cover – avoid hard crash if pdfminer fails
        return {}

    # Pre-bind locals for speed in tight loop
    pair_re = _PAIR_RE
    order = _order
    for line in text.splitlines():
        m = pair_re.match(line)
        if not m:
            continue
        # Strip whitespace in group values before ordering
        a = m.group("a").strip()
        b = m.group("b").strip()
        mapping[order(a, b)] = "Interaction ANSM (gravité non classée)"
    return mapping


@lru_cache(maxsize=1)
def get_interactions() -> Dict[Tuple[str, str], str]:  # noqa: D401
    """Return dict{(A,B): description}. Empty on failure."""
    # Priority: CSV → PDF
    for fp in _RES.glob("*.csv"):
        data = _load_from_csv(fp)
        if data:
            return data
    for fp in _RES.glob("*.pdf"):
        data = _load_from_pdf(fp)
        if data:
            return data
    return {}
