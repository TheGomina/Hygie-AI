"""Utility to build a minimal FHIR Bundle for a BMP result.

author: Hygie-AI
Only the elements required for thesis demo are included.
"""

from __future__ import annotations

import uuid
from datetime import datetime, timezone
from typing import Dict, List

FHIR_VERSION = "5.0.0"  # R5


def _id() -> str:
    return str(uuid.uuid4())


def build_bundle(
    patient: Dict[str, str | int],
    medications: List[str],
    detected_issues: List[str],
    recommendations: List[str],
) -> Dict:
    """Return a minimal FHIR Bundle (JSON-ready dict)."""

    now = datetime.now(timezone.utc).isoformat()

    bundle = {
        "resourceType": "Bundle",
        "type": "collection",
        "timestamp": now,
        "id": _id(),
        "entry": [],
    }

    # Patient (very simplified)
    patient_res = {
        "resourceType": "Patient",
        "id": _id(),
        "gender": "female" if patient.get("sex") == "F" else "male",
        "birthDate": str(2025 - int(patient.get("age", 40))),  # rough
    }
    bundle["entry"].append({"resource": patient_res})

    # MedicationStatements
    for med in medications:
        ms = {
            "resourceType": "MedicationStatement",
            "id": _id(),
            "status": "active",
            "medicationCodeableConcept": {
                "text": med,
            },
            "subject": {"reference": f"Patient/{patient_res['id']}"},
        }
        bundle["entry"].append({"resource": ms})

    # Detected Issues
    for desc in detected_issues:
        di = {
            "resourceType": "DetectedIssue",
            "id": _id(),
            "status": "final",
            "severity": "high",
            "code": {"text": desc},
        }
        bundle["entry"].append({"resource": di})

    # GuidanceResponse (recommendations aggregate)
    if recommendations:
        gr = {
            "resourceType": "GuidanceResponse",
            "id": _id(),
            "status": "success",
            "subject": {"reference": f"Patient/{patient_res['id']}"},
            "outputParameters": {
                "text": "\n".join(recommendations),
            },
        }
        bundle["entry"].append({"resource": gr})

    return bundle
