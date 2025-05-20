"""Module d'ajustement posologique selon fonction rénale."""
from __future__ import annotations

from typing import Literal

# Hygie rule 5: assertions pour préconditions

def calculate_clcr(creatinine: float, age: int, weight: float, sex: str) -> float:
    """Calcul Cockcroft-Gault pour clairance de la créatinine en mL/min."""
    assert creatinine > 0, "créatininémie doit être positive"
    assert weight > 0, "poids doit être positif"
    assert age > 0, "âge doit être positif"
    factor = 0.85 if sex.strip().upper() in {"F", "FEMME"} else 1.0
    clcr = ((140 - age) * weight) / (72 * creatinine) * factor
    assert clcr >= 0, "ClCr négative"
    return clcr


def adjust_for_renal(creatinine: float, age: int, weight: float, sex: str) -> str:
    """Retourne recommandation posologique selon clairance rénale."""
    clcr = calculate_clcr(creatinine, age, weight, sex)
    # Hygie rule 5: assertion postcondition
    assert clcr >= 0, "ClCr doit être non-négative"
    if clcr < 30:
        return "Contre-indication rénale"
    if clcr < 60:
        return "Réduction 50% posologique"
    return "Posologie normale"
