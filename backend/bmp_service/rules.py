"""Very small in-memory clinical rules set for MVP demo.
This will be replaced by a richer dataset later.
"""

from __future__ import annotations

from typing import Dict, List, Set, Tuple

from .interactions_ref import get_interactions

# Build interaction dict with sorted keys to match detection logic
_INTERACTIONS_RAW: list[tuple[list[str], str]] = [
    (
        ["LISINOPRIL", "IBUPROFEN"],
        "Association d’un IEC et d’un AINS : risque d’insuffisance rénale aiguë.",
    ),
    (
        ["ACETYLSALICYLIC_ACID", "APIXABAN"],
        "Double traitement anti-thrombotique, risque hémorragique accru.",
    ),
    (
        ["ATORVASTATIN", "CLARITHROMYCIN"],
        "Statine + macrolide : risque augmenté de rhabdomyolyse.",
    ),
    (
        ["SIMVASTATIN", "CLARITHROMYCIN"],
        "Statine + macrolide : risque augmenté de rhabdomyolyse.",
    ),
    (
        ["WARFARIN", "TRIMETHOPRIM"],
        "Warfarine + triméthoprime : risque hémorragique accru.",
    ),
    (["LITHIUM", "IBUPROFEN"], "Lithium + AINS : risque de toxicité du lithium."),
    (
        ["MORPHINE", "DIAZEPAM"],
        "Opioïde + benzodiazépine : risque de dépression respiratoire.",
    ),
    (["WARFARIN", "IBUPROFEN"], "Warfarine + AINS : risque hémorragique."),
    (["SIMVASTATIN", "ITRACONAZOLE"], "Statine + azolé : myopathie/rhabdomyolyse."),
    (["DIGOXIN", "VERAPAMIL"], "Digoxine + vérapamil : bradycardie/surdosage."),
    (
        ["SILDENAFIL", "NITROGLYCERIN"],
        "Sildénafil + dérivé nitré : hypotension sévère.",
    ),
    (
        ["CARBAMAZEPINE", "ERYTHROMYCIN"],
        "Carbamazépine + érythromycine : hausse taux carbamazépine.",
    ),
]

# Merge with large ANSM thesaurus (if available)
_INTERACTIONS: Dict[Tuple[str, str], str] = {
    tuple(sorted(pair)): desc for pair, desc in _INTERACTIONS_RAW
}
_INTERACTIONS.update(get_interactions())


def _key(a: str, b: str) -> Tuple[str, str]:
    return tuple(sorted((a.upper(), b.upper())))  # type: ignore[return-value]


def detect_interactions(medications: List[str]) -> List[str]:
    """Return list of interaction descriptions found in the medication list."""
    meds = [m.upper() for m in medications]
    seen: Set[Tuple[str, str]] = set()
    problems: List[str] = []

    for i, a in enumerate(meds):
        for b in meds[i + 1 :]:
            key = _key(a, b)
            if key in seen:
                continue
            seen.add(key)
            if key in _INTERACTIONS:
                problems.append(_INTERACTIONS[key])
    return problems
