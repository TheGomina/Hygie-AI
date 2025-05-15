"""Quick parser for ANSM interactions thesaurus PDF → CSV.

Generates `resources/interactions_ansm.csv` (A;B;gravite) which is consumed by
`interactions_ref.get_interactions()` for fast loading.
"""

from __future__ import annotations

import argparse
import csv
import re
import sys
from pathlib import Path
from typing import List, Tuple

from pdfminer.high_level import extract_text  # type: ignore

_PAIR_RE = re.compile(
    r"^(?P<a>[A-ZÀÂÉÈÊÎÔÙÛÇ\- ]{3,})\s*/\s*(?P<b>[A-ZÀÂÉÈÊÎÔÙÛÇ\- ]{3,})", re.I
)
_GRAV_RE = re.compile(r"\b(Niveau|Level)\s+([ABCD])", re.I)

# ---------------------------------------------------------------------------


def _extract_pairs(text: str) -> List[Tuple[str, str, str]]:  # noqa: D401
    lines = [ln.strip() for ln in text.splitlines() if ln.strip()]
    out: List[Tuple[str, str, str]] = []
    grav = "?"  # default
    for ln in lines:
        g = _GRAV_RE.search(ln)
        if g:
            grav = g.group(2)
        m = _PAIR_RE.match(ln)
        if m:
            a, b = m.group("a").upper(), m.group("b").upper()
            out.append((a, b, grav))
        if len(out) > 30000:  # safety limit
            break
    return out


def _parse_pdf(pdf: Path) -> List[Tuple[str, str, str]]:  # noqa: D401
    assert pdf.exists(), pdf
    return _extract_pairs(extract_text(str(pdf)))


def _cli(argv: List[str]) -> None:  # noqa: D401
    ap = argparse.ArgumentParser()
    ap.add_argument("pdf", type=Path)
    ap.add_argument(
        "-o",
        "--output",
        type=Path,
        default=Path(__file__).resolve().parents[2]
        / "resources"
        / "interactions_ansm.csv",
    )
    args = ap.parse_args(argv)

    rows = _parse_pdf(args.pdf)
    assert rows, "No interactions found"

    with args.output.open("w", encoding="utf-8", newline="") as fh:
        w = csv.writer(fh, delimiter=";")
        w.writerows(rows)
    print(f"✔️  {len(rows)} interactions → {args.output}")


if __name__ == "__main__":  # pragma: no cover
    _cli(sys.argv[1:])
