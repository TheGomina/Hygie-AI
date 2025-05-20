from __future__ import annotations

import yaml
from dataclasses import dataclass
from pathlib import Path
from typing import List, Dict
from .drug_repository import substances_from_atc


@dataclass(frozen=True)
class AlgorithmStep:
    """Étape générique d’un algorithme pharmaceutique."""
    id: str
    title: str
    content: str = ""


@dataclass(frozen=True)
class PharmaAlgorithm:
    """Représente un algorithme pharmaceutique complet."""
    name: str
    steps: List[AlgorithmStep]


def load_algorithm_template() -> List[AlgorithmStep]:  # noqa: D401
    """Charger le template YAML des étapes d’un AP."""
    tpl = Path(__file__).resolve().parent.parent / "resources" / "algorithm_template.yaml"
    raw = yaml.safe_load(tpl.read_text(encoding="utf-8")) or {}
    steps = []
    for s in raw.get("steps", []):
        steps.append(AlgorithmStep(id=s.get("id", ""), title=s.get("title", "")))
    return steps


def create_algorithm(name: str) -> PharmaAlgorithm:
    """Créer un algorithme pharmaceutique vide selon le template."""
    steps = load_algorithm_template()
    return PharmaAlgorithm(name=name, steps=steps)


def load_classification_mapping() -> Dict[str, Dict[str, Dict[str, str]]]:
    """Charger les correspondances de classification (gravité, PLP, IP) depuis YAML."""
    mapping_file = Path(__file__).resolve().parent.parent / "resources" / "classification_mapping.yaml"
    assert mapping_file.exists(), f"Fichier mapping introuvable: {mapping_file}"
    raw = yaml.safe_load(mapping_file.read_text(encoding="utf-8")) or {}
    assert isinstance(raw, dict), "Mapping YAML invalide, attendu un dict"
    return raw


def load_example_algorithms() -> List[PharmaAlgorithm]:
    """Charger les exemples concrets d’AP depuis YAML."""
    examples_file = Path(__file__).resolve().parent.parent / "resources" / "algorithms_examples.yaml"
    assert examples_file.exists(), "Fichier d'exemples d'AP introuvable"
    data = yaml.safe_load(examples_file.read_text(encoding="utf-8")) or []
    assert isinstance(data, list), "Exemples d'AP invalide, attendu une liste"
    algos: List[PharmaAlgorithm] = []
    template_titles = {s.id: s.title for s in load_algorithm_template()}
    for entry in data:
        name = entry.get("name")
        assert name, "Chaque exemple doit avoir un 'name'"
        steps_data = entry.get("steps", [])
        assert isinstance(steps_data, list), "'steps' doit être une liste"
        steps: List[AlgorithmStep] = []
        for s in steps_data:
            assert isinstance(s, dict) and "id" in s and "content" in s, "Step doit avoir 'id' et 'content'"
            title = template_titles.get(s["id"], "")
            steps.append(AlgorithmStep(id=s["id"], title=title, content=s["content"]))
        algos.append(PharmaAlgorithm(name=name, steps=steps))
    return algos


def extract_elements_appreciation(text: str) -> Dict[str, str]:
    """Extraire les éléments d’appréciation d’un texte."""
    assert isinstance(text, str), "Le texte doit être une chaîne"
    parts = [p.strip() for p in text.split(";") if p.strip()]
    assert parts, "Aucun élément d'appréciation trouvé"
    elements: Dict[str, str] = {}
    for p in parts:
        if ":" in p:
            key, val = p.split(":", 1)
            elements[key.strip()] = val.strip()
    return elements


def elements_to_logic(elements: Dict[str, str]) -> str:
    """Convertir éléments en règle logique."""
    assert isinstance(elements, dict), "Elements doit être un dict"
    terms = [f"{k} -> {v}" for k, v in elements.items()]
    logic = " AND ".join(terms)
    assert logic, "Règle logique vide"
    return logic


def logic_to_code(logic: str) -> str:
    """Transformer règle logique en code Python."""
    assert isinstance(logic, str), "Logic doit être une chaîne"
    code = "lambda patient: " + logic.replace(" AND ", " and ")
    return code


# Génération automatique d’AP par classes ATC
def generate_algorithms_for_atc(atc_list: List[str]) -> List[PharmaAlgorithm]:
    """Générer des AP pour chaque classe ATC donnée."""
    assert isinstance(atc_list, list), "atc_list doit être une liste"
    assert all(isinstance(c, str) for c in atc_list), "Chaque ATC doit être une chaîne"
    algos: List[PharmaAlgorithm] = []
    template = load_algorithm_template()
    for code in atc_list:
        subs = substances_from_atc(code)
        if not subs:
            continue
        name = f"AP_{code}"
        steps: List[AlgorithmStep] = []
        for s in template:
            content = ""
            if s.id == "situation":
                content = f"Patient sous classe ATC {code}"
            elif s.id == "logical_rule":
                conditions = [f"'{sub.lower()}' in patient['medications']" for sub in subs]
                content = " OR ".join(conditions)
            elif s.id == "code_rule":
                conds = " or ".join([f"m.lower() == '{sub.lower()}'" for sub in subs])
                content = f"lambda patient, meds: any({conds} for m in meds)"
            elif s.id == "intervention":
                content = f"Vérifier l'utilisation appropriée des médicaments de la classe {code}"
            elif s.id == "references":
                content = "ANAP"
            elif s.id == "bdc_reference":
                content = code
            steps.append(AlgorithmStep(id=s.id, title=s.title, content=content))
        algos.append(PharmaAlgorithm(name=name, steps=steps))
    return algos
