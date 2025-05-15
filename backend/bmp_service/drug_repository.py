"""Lightweight access layer to medication reference data (BDPM).

For MVP we load a subset of BDPM CSV (specialités) to enable:
- DCI (denomination commune internationale)
- Code CIS, ATC class, pharmaceutical form

The file path is provided via env var ``BDPM_CSV_PATH`` and cached in memory.
The repository hides the underlying format so that future sources (Thériaque,
REMEDIS…) can implement the same interface.
"""

from __future__ import annotations

import csv
import os
from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path
from typing import Dict, List, Optional, Set

# ---------------------------------------------------------------------------
# Data structures
# ---------------------------------------------------------------------------


@dataclass(frozen=True)
class Drug:
    """Minimal drug record extracted from BDPM (per CIS)."""

    cis: str  # Identifiant BDPM
    libelle: str  # Libellé complet spécialité
    substances: List[str]


# ---------------------------------------------------------------------------
# Abstract repository
# ---------------------------------------------------------------------------


# Abstract interface not executed in tests
class DrugRepository:  # pragma: no cover
    """Abstract repository API (isolate external BD sources)."""

    # Hygie rule 11: immutable return values
    def find_dci(self, name: str) -> Optional[str]:  # noqa: D401
        raise NotImplementedError

    def substances(self, name: str) -> List[str]:  # noqa: D401
        return []


# ---------------------------------------------------------------------------
# BDPM implementation
# ---------------------------------------------------------------------------


class _BDPMRepository(DrugRepository):
    """Parse BDPM "Spécialités" + "Compositions" extracts.

    We only need a few mappings:
    • libellé spécialité  → CIS
    • CIS                → list[Substance]
    """

    def __init__(self, spec_path: Path, compo_path: Path) -> None:  # noqa: D401
        assert spec_path.exists(), f"BDPM Spécialités introuvable: {spec_path}"
        assert compo_path.exists(), f"BDPM Compositions introuvable: {compo_path}"

        self._libelle_to_cis: Dict[str, str] = {}
        self._cis_to_substances: Dict[str, List[str]] = {}

        self._parse_spec(spec_path)
        self._parse_compo(compo_path)

    # ---------------------------------------------------------------------
    # Parsing helpers (≤50 lines rule)
    # ---------------------------------------------------------------------

    @staticmethod
    def _detect_delim(sample: str) -> str:  # noqa: D401
        return ";" if ";" in sample else "\t"

    def _parse_spec(self, path: Path) -> None:  # noqa: D401
        # BDPM fichiers sont encodés en CP1252 ; tenter UTF-8 puis fallback CP1252
        try:
            with path.open(encoding="utf-8", newline="") as fh:
                first = fh.readline()
                delim = self._detect_delim(first)
                fh.seek(0)
                reader = csv.reader(fh, delimiter=delim)
                header = next(reader)
                has_header = header[0].strip().upper() in {"CIS", "CODECIS"}
                if not has_header:
                    # rewind to include first line as data
                    fh.seek(0)
                    reader = csv.reader(fh, delimiter=delim)
                for row in reader:
                    if not row:
                        continue
                    cis = row[0].strip()
                    lib = row[1].strip().upper() if len(row) > 1 else ""
                    if cis and lib:
                        self._libelle_to_cis[lib] = cis
        except UnicodeDecodeError:  # pragma: no cover
            with path.open(encoding="cp1252", newline="") as fh:
                first = fh.readline()
                delim = self._detect_delim(first)
                fh.seek(0)
                reader = csv.reader(fh, delimiter=delim)
                header = next(reader)
                has_header = header[0].strip().upper() in {"CIS", "CODECIS"}
                if not has_header:
                    # rewind to include first line as data
                    fh.seek(0)
                    reader = csv.reader(fh, delimiter=delim)
                for row in reader:
                    if not row:
                        continue
                    cis = row[0].strip()
                    lib = row[1].strip().upper() if len(row) > 1 else ""
                    if cis and lib:
                        self._libelle_to_cis[lib] = cis

    def _parse_compo(self, path: Path) -> None:  # noqa: D401
        try:
            with path.open(encoding="utf-8", newline="") as fh:
                first = fh.readline()
                delim = self._detect_delim(first)
                fh.seek(0)
                reader = csv.reader(fh, delimiter=delim)
                header = next(reader)
                has_header = header and header[0].strip().upper() == "CIS"
                if not has_header:
                    fh.seek(0)
                    reader = csv.reader(fh, delimiter=delim)
                for row in reader:
                    if len(row) < 4:
                        continue
                    cis = row[0].strip()
                    substance = row[3].strip().upper()
                    if cis and substance:
                        self._cis_to_substances.setdefault(cis, []).append(substance)
        except UnicodeDecodeError:  # pragma: no cover
            with path.open(encoding="cp1252", newline="") as fh:
                first = fh.readline()
                delim = self._detect_delim(first)
                fh.seek(0)
                reader = csv.reader(fh, delimiter=delim)
                header = next(reader)
                has_header = header and header[0].strip().upper() == "CIS"
                if not has_header:
                    fh.seek(0)
                    reader = csv.reader(fh, delimiter=delim)
                for row in reader:
                    if len(row) < 4:
                        continue
                    cis = row[0].strip()
                    substance = row[3].strip().upper()
                    if cis and substance:
                        self._cis_to_substances.setdefault(cis, []).append(substance)

    # ---------------------------------------------------------------------
    # Public API
    # ---------------------------------------------------------------------

    def find_dci(self, name: str) -> Optional[str]:  # noqa: D401
        lib = name.strip().upper()
        cis = self._libelle_to_cis.get(lib)
        if not cis:
            return None
        subs = self._cis_to_substances.get(cis, [])
        return subs[0] if subs else None

    def substances(self, name: str) -> List[str]:  # noqa: D401
        lib = name.strip().upper()
        cis = self._libelle_to_cis.get(lib)
        if not cis:
            return []
        return self._cis_to_substances.get(cis, [])


# ---------------------------------------------------------------------------
# Static ATC mapping (minimal for MVP)
# ---------------------------------------------------------------------------

_ATC_TO_SUBS: Dict[str, Set[str]] = {
    # Lipid-modifying agents, statins
    "C10AA": {
        "ATROVASTATIN",
        "ATORVASTATIN",
        "SIMVASTATIN",
        "PRAVASTATIN",
    },
    # Macrolide antibacterials
    "J01FA": {
        "CLARITHROMYCIN",
        "ERYTHROMYCIN",
        "AZITHROMYCIN",
    },
    # NSAIDs, propionic acid derivatives
    "M01AE": {
        "IBUPROFEN",
        "KETOPROFEN",
        "NAPROXEN",
    },
    # ACE inhibitors
    "C09AA": {
        "LISINOPRIL",
        "RAMIPRIL",
        "PERINDOPRIL",
    },
    # Angiotensin II receptor blockers
    "C09CA": {
        "LOSARTAN",
        "VALSARTAN",
        "CANDESARTAN",
    },
    # Direct oral anticoagulants
    "B01AF": {
        "APIXABAN",
        "RIVAROXABAN",
        "DABIGATRAN",
    },
    # Strong opioids
    "N02AA": {
        "MORPHINE",
        "OXYCODONE",
        "HYDROMORPHONE",
    },
}


def substances_from_atc(code: str) -> Set[str]:  # noqa: D401
    """Return set of substance names from static ATC map (uppercase)."""
    return _ATC_TO_SUBS.get(code.upper(), set())


# ---------------------------------------------------------------------------
# Singleton accessor
# ---------------------------------------------------------------------------


@lru_cache(maxsize=1)
def get_repository() -> DrugRepository:  # noqa: D401
    """Return lazily-initialised repository singleton."""
    base_data = Path(__file__).resolve().parent / "data"
    spec_default = base_data / "CIS_bdpm.csv"
    compo_default = base_data / "CIS_COMPO_bdpm.csv"

    spec = Path(os.getenv("BDPM_CSV_PATH", str(spec_default)))
    compo = Path(os.getenv("BDPM_COMPO_PATH", str(compo_default)))
    return _BDPMRepository(spec, compo)
