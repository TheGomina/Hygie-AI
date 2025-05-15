"""Minimal BDPM parser to map CIP → DCI.

For the thesis demo we ship a tiny in-memory subset (paracetamol etc.).
In production, load full BDPM CSVs.
"""

from __future__ import annotations

import csv
import sqlite3
from functools import lru_cache
from pathlib import Path
from typing import Dict

HERE = Path(__file__).parent

# Fallback minimal mapping when CSV not present.
_FALLBACK = {
    "3400932716455": "PARACETAMOL",  # Doliprane 1g cp
    "PARACETAMOL": "PARACETAMOL",
}

# First preference : SQLite built via scripts/build_bdpm_db.py
DB_PATH = HERE / "data" / "bdpm.sqlite"

# Fallback CSV subset (lightweight extract for unit tests)
CSV_PATH = HERE / "data" / "bdpm_substances.csv"  # optional


def _parse_csv(path: Path) -> Dict[str, str]:
    mapping: Dict[str, str] = {}
    with path.open(newline="", encoding="utf-8") as fh:
        reader = csv.DictReader(fh, delimiter=";")
        for row in reader:
            cip = row.get("CIP7") or row.get("CIP13") or ""
            dci = row.get("DCI") or ""
            if cip and dci:
                mapping[cip] = dci.upper()
    return mapping


def _parse_sqlite(db: Path) -> Dict[str, str]:  # ≤20 lignes (rule 4)
    """Return CIP/DCI mapping from bdpm.sqlite."""
    assert db.exists(), "SQLite BDPM introuvable"
    conn = sqlite3.connect(str(db))
    cur = conn.cursor()
    mapping: Dict[str, str] = {}
    # CIS -> substance
    for cis, substance in cur.execute("SELECT cis, substance FROM compositions"):
        mapping[cis] = substance
        mapping[substance] = substance  # alias directe

    # CIP -> substance via join cips
    cur.execute(
        "SELECT c.cip, comp.substance FROM cips c JOIN compositions comp USING(cis)"
    )
    for cip, substance in cur.fetchall():
        mapping[cip] = substance
    conn.close()
    assert mapping, "BDPM DB vide?"
    return mapping


@lru_cache(maxsize=1)
def get_mapping() -> Dict[str, str]:
    """Load CIP→DCI mapping with preference: SQLite > CSV > fallback."""
    if DB_PATH.exists():
        return _parse_sqlite(DB_PATH)
    if CSV_PATH.exists():
        return _parse_csv(CSV_PATH)
    return _FALLBACK


def normalize_drug(token: str) -> str:
    """Return DCI in uppercase from CIP or already DCI."""
    m = get_mapping()
    return m.get(token.upper(), token.upper())
