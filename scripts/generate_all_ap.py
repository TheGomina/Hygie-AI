#!/usr/bin/env python3
"""Script pour générer tous les algorithmes pharmaceutiques par classes ATC."""
import yaml
from pathlib import Path
import sys
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from backend.bmp_service.drug_repository import supported_atc_codes
from backend.bmp_service.algorithm_repository import generate_algorithms_for_atc


def main():
    atc_codes = supported_atc_codes()
    algos = generate_algorithms_for_atc(atc_codes)
    output = []
    for algo in algos:
        output.append({
            "name": algo.name,
            "steps": [
                {"id": s.id, "title": s.title, "content": s.content}
                for s in algo.steps
            ],
        })
    dest = Path(__file__).resolve().parent.parent / "backend" / "resources" / "algorithms_all.yaml"
    dest.parent.mkdir(parents=True, exist_ok=True)
    with open(dest, 'w', encoding='utf-8') as f:
        yaml.dump(output, f, allow_unicode=True)
    print(f"Generated {len(algos)} algorithms in {dest}")


if __name__ == "__main__":
    main()
