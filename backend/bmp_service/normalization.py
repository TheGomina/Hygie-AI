"""Medication name normalization utilities for MVP.

Maps common brand names or misspellings to their canonical Active
Pharmaceutical Ingredient (API) name (uppercase English).
This is *minimal* and demonstrative. In production, use a full
terminology service (e.g. RxNorm, ATC, Thériaque).
"""

from functools import lru_cache

_NORMALIZATION_MAP: dict[str, str] = {
    # Ibuprofen variants
    "IBUPROFENE": "IBUPROFEN",
    "ADVIL": "IBUPROFEN",
    "NUROFEN": "IBUPROFEN",
    # Aspirin / DCI
    "ASPIRINE": "ACETYLSALICYLIC_ACID",
    "ASPIRIN": "ACETYLSALICYLIC_ACID",
    "KARDEGIC": "ACETYLSALICYLIC_ACID",
    # Lisinopril
    "ZESTRIL": "LISINOPRIL",
    # Apixaban brand
    "ELIQUIS": "APIXABAN",
    # IEC plural spelling
    "IEC": "IEC",  # placeholder
}

# Prefer repository lookup when available
from .drug_repository import get_repository


@lru_cache(maxsize=1024)
def normalize(name: str) -> str:  # pragma: no cover
    """Return the canonical uppercase API name for *name*.

    If *name* is not in the map, returns the uppercased input.
    """
    key = name.strip().upper()
    repo = get_repository()
    dci = repo.find_dci(key)
    if dci:
        return dci
    return _NORMALIZATION_MAP.get(key, key)
