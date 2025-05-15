"""Subset of STOPP/START criteria (v3) for MVP.

Only a limited selection of high-yield rules are implemented to showcase the
framework. Each rule must comply with Hygie-AI dev rules: ≤50 lines, ≥2
assertions, minimal recursion, etc.
"""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Callable, List, Literal

import yaml  # type: ignore

from .drug_repository import get_repository, substances_from_atc

Predicate = Callable[[dict, list[str]], bool]


@dataclass(frozen=True)
class Rule:
    id: str
    description: str
    predicate: Predicate
    phase: Literal["STOPP", "START"] = "STOPP"

    def applies(self, demo: dict, meds: list[str]) -> bool:  # noqa: D401
        assert "age" in demo, "Missing age in demographics"
        assert isinstance(meds, list), "meds must be list"
        return self.predicate(demo, meds)


_repo = get_repository()


# ----- Utility helpers -------------------------------------------------------


def has_med(meds: list[str], target: set[str]) -> bool:  # noqa: D401
    """Return True if any med in *meds* matches *target* (case-insensitive)."""
    assert isinstance(target, set)  # rule 5
    meds_up = set(m.upper() for m in meds)
    return bool(meds_up & target)


# ----- Rules -----------------------------------------------------------------


def _rule_A1(demo: dict, meds: list[str]) -> bool:  # noqa: D401
    """STOPP A1 – Anticholinergics if age ≥ 65 y."""
    age = demo.get("age", 0)
    if age < 65:
        return False
    anticholinergics = {
        "DIPHENHYDRAMINE",
        "DEXCHLORPHENIRAMINE",
        "OXYBUTYNIN",
    }
    return has_med(meds, anticholinergics)


def _rule_B2(demo: dict, meds: list[str]) -> bool:  # noqa: D401
    """STOPP B2 – ACE-I with K+ supplement causing hyperkalaemia risk."""
    icao = {"LISINOPRIL", "RAMIPRIL", "PERINDOPRIL"}
    potassium = {"POTASSIUM", "KCL"}
    return has_med(meds, icao) and has_med(meds, potassium)


_RULES: list[Rule] = [
    Rule("A1", "Anticholinergic chez >65 ans", _rule_A1),
    Rule("B2", "IEC + supplément de potassium", _rule_B2),
]


# ----- YAML loader (dynamic criteria) ---------------------------------------


def _load_yaml_rules() -> List[Rule]:  # noqa: D401
    """Load additional STOPP/START rules from YAML file.

    Schema per entry ::
        id: str
        description: str
        substances: list[str]
        age_min: int (optional)
        age_max: int (optional)
        enabled: bool (default True)
        phase: Literal["STOPP", "START"] (default "STOPP")
    """
    path = Path(__file__).resolve().parent / "data" / "stopp_start_v3.yaml"
    if not path.exists():
        return []

    with path.open(encoding="utf-8") as fh:
        raw = yaml.safe_load(fh) or []

    rules: List[Rule] = []

    for item in raw:
        if not item.get("enabled", True):
            continue

        # allow substances list or ATC code
        if "atc" in item:
            subs = substances_from_atc(item["atc"])  # type: ignore[arg-type]
        else:
            subs = {s.upper() for s in item.get("substances", [])}
        age_min = int(item.get("age_min", 0))
        age_max = int(item.get("age_max", 200))
        phase = item.get("phase", "STOPP").upper()

        def _make_pred(substances=subs, amin=age_min, amax=age_max):  # noqa: D401
            def _pred(demo: dict, meds: list[str]) -> bool:  # noqa: D401
                age = demo.get("age", 0)
                if age < amin or age > amax:
                    return False
                return has_med(meds, substances)

            return _pred

        rules.append(
            Rule(str(item["id"]), str(item["description"]), _make_pred(), phase)
        )

    return rules


# Extend static list with dynamic ones
_RULES.extend(_load_yaml_rules())


def evaluate_rules(
    demo: dict, meds: list[str]
) -> tuple[List[str], List[str]]:  # noqa: D401
    """Return tuple of (STOPP descriptions, START descriptions)."""
    stops: List[str] = []
    starts: List[str] = []
    for rule in _RULES:
        if rule.applies(demo, meds):
            if rule.phase == "START":
                starts.append(rule.description)
            else:
                stops.append(rule.description)
    return stops, starts
