"""Build a lightweight SQLite database from BDPM CSV extracts.

Usage:
    python -m bmp_service.scripts.build_bdpm_db --csv-dir path/to/extract

This script expects at least two files inside --csv-dir:
    CIS_bdpm.txt           (Spécialités)
    CIS_COMPO_bdpm.txt     (Compositions)

They are provided by the official ANSM extract (`base-2025-03-01.zip`, etc.).
The produced `bdpm.sqlite` contains two tables and indices for fast lookup.
"""

from __future__ import annotations

import argparse
import csv
import sqlite3
from pathlib import Path
from typing import Iterator, List

_DB_NAME = "bdpm.sqlite"


def _detect_delim(sample: str) -> str:  # noqa: D401
    """Return CSV delimiter for a sample line (\t or ;)"""
    return ";" if ";" in sample else "\t"


def _iter_rows(path: Path, expected_cols: int) -> Iterator[List[str]]:  # noqa: D401
    """Yield lists of strings for each non-empty CSV row."""
    for enc in ("utf-8", "cp1252"):
        try:
            with path.open(encoding=enc, newline="") as fh:
                first = fh.readline()
                delim = _detect_delim(first)
                fh.seek(0)
                reader = csv.reader(fh, delimiter=delim)
                header = next(reader)
                # Detect header presence (starts with CIS or CodeCIS)
                has_header = header and header[0].strip().upper() in {"CIS", "CODECIS"}
                if not has_header:
                    # Rewind to include first line as data
                    fh.seek(0)
                    reader = csv.reader(fh, delimiter=delim)
                for row in reader:
                    if not row:
                        continue
                    # Pad to expected length
                    if len(row) < expected_cols:
                        row.extend([""] * (expected_cols - len(row)))
                    yield row[:expected_cols]
            break  # success, stop trying encodings
        except UnicodeDecodeError:
            continue
    else:  # pragma: no cover
        raise UnicodeDecodeError("Failed to decode CSV with UTF-8/CP1252")


def build_db(csv_dir: Path) -> Path:  # noqa: D401
    def _locate(name_variants):
        for variant in name_variants:
            p = csv_dir / variant
            if p.exists():
                return p
        raise FileNotFoundError(
            f"Aucun des fichiers {name_variants} trouvé dans {csv_dir}. "
            "Vérifiez l'extraction BDPM."
        )

    spec_path = _locate(
        ["CIS_bdpm.txt", "CIS_bdpm.csv", "cis_bdpm.txt", "CIS_BDPM.txt"]
    )
    compo_path = _locate(
        [
            "CIS_COMPO_bdpm.txt",
            "CIS_COMPO_bdpm.csv",
            "cis_compo_bdpm.txt",
            "CIS_COMPO_BDPM.txt",
        ]
    )

    # Optional CIP→CIS mapping (Présentations) – fichier "CIS_CIP_bdpm.*"
    cip_path = None
    for variant in [
        "CIS_CIP_bdpm.csv",
        "CIS_CIP_bdpm.txt",
        "cis_cip_bdpm.csv",
        "cis_cip_bdpm.txt",
    ]:
        p = csv_dir / variant
        if p.exists():
            cip_path = p
            break

    db_path = csv_dir / _DB_NAME
    if db_path.exists():
        db_path.unlink()

    conn = sqlite3.connect(db_path)
    cur = conn.cursor()
    cur.executescript(
        """
        CREATE TABLE specialites (
            cis TEXT PRIMARY KEY,
            libelle TEXT NOT NULL
        );
        CREATE TABLE compositions (
            cis TEXT NOT NULL,
            substance TEXT NOT NULL,
            dosage TEXT,
            PRIMARY KEY (cis, substance)
        );
        CREATE INDEX idx_spec_lib ON specialites(libelle);
        CREATE INDEX idx_comp_sub ON compositions(substance);
        CREATE TABLE IF NOT EXISTS cips (
            cip TEXT PRIMARY KEY,
            cis TEXT NOT NULL
        );
        CREATE INDEX idx_cip_cis ON cips(cis);
        """
    )

    # Insert Spécialités
    for cis, lib in _iter_rows(spec_path, 2):
        cur.execute("INSERT INTO specialites VALUES (?, ?)", (cis, lib.upper()))

    # Insert Compositions (col 1 CIS, col 4 substance, col 5 dosage)
    for row in _iter_rows(compo_path, 5):
        cis, substance, dosage = row[0], row[3].upper(), row[4]
        cur.execute(
            "INSERT OR IGNORE INTO compositions VALUES (?, ?, ?)",
            (cis, substance, dosage),
        )

    # Insert CIP→CIS (col 2 CIP13, col 1 CIS) depending on file layout
    if cip_path:
        for row in _iter_rows(cip_path, 8):
            # Layout: CodeCIS;CodeCIP7;LibelléPresentation;...;CIP13 (col 7 index 6)
            cis = row[0]
            cip7 = row[1]
            cip13 = row[6] if len(row) > 6 else ""
            # insert CIP7
            if cip7:
                cur.execute("INSERT OR IGNORE INTO cips VALUES (?, ?)", (cip7, cis))
            # insert CIP13
            if cip13:
                cur.execute("INSERT OR IGNORE INTO cips VALUES (?, ?)", (cip13, cis))
    else:
        print(
            "[BDPM] Avertissement : fichier CIS_CIP_bdpm.* introuvable, table CIP vide."
        )

    conn.commit()
    conn.close()
    return db_path


def main() -> None:  # noqa: D401
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--csv-dir",
        type=Path,
        required=True,
        help="Directory containing BDPM CSV extracts",
    )
    args = parser.parse_args()

    db_path = build_db(args.csv_dir)
    print(f"[BDPM] SQLite construit → {db_path}")


if __name__ == "__main__":
    main()
