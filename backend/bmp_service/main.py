"""Entry point for Hygie-AI Bilan Médication Partagé (BMP) service.
Stub version – will be expanded with clinical rules & LLM orchestration.
"""

from datetime import datetime, timezone

from fastapi import Depends, FastAPI, HTTPException, Response
from fastapi.middleware.cors import CORSMiddleware

# fhir.resources for parsing input Bundle
from fhir.resources.bundle import Bundle as FHBundle  # type: ignore
from fhir.resources.medication import Medication as FHMedication  # type: ignore
from fhir.resources.medicationstatement import MedicationStatement  # type: ignore
from fhir.resources.patient import Patient  # type: ignore
from pydantic import BaseModel, Field

# Prometheus metrics
from prometheus_client import CONTENT_TYPE_LATEST, Counter, Summary, generate_latest

# Use BDPM mapping for canonical DCI
from .bdpm_loader import normalize_drug as normalize
from .config import settings
from .fhir_bundle import FHIR_VERSION, build_bundle
from .llm_orchestrator import generate_summary, list_local_models
from .rules import detect_interactions
from .security import get_current_user_id
from .stopp_start import evaluate_rules

app = FastAPI(
    title="Hygie-AI BMP API",
    version=settings.VERSION if hasattr(settings, "VERSION") else "0.1.0",
    description="API for running shared medication review analyses.",
)
app.openapi_tags = []

# Prometheus metrics definitions
REQUEST_COUNT = Counter(
    "bmp_requests_total",
    "Total HTTP requests (BMP)",
    ["method", "endpoint", "http_status"],
)
REQUEST_TIME = Summary(
    "bmp_request_processing_seconds", "Time spent processing BMP request"
)

# ---------------------------------------------------------------------------
# FHIR helpers
# ---------------------------------------------------------------------------


def _now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


def _capability_statement() -> dict:  # minimal, R5
    return {
        "resourceType": "CapabilityStatement",
        "status": "active",
        "date": _now_iso(),
        "kind": "instance",
        "fhirVersion": FHIR_VERSION,
        "format": ["json"],
        "rest": [
            {
                "mode": "server",
                "resource": [
                    {
                        "type": "Bundle",
                        "operation": [
                            {
                                "name": "$bmp",
                                "definition": "Bundle/$bmp",
                                "documentation": "Analyse BMP et renvoie Bundle enrichi.",
                            }
                        ],
                    }
                ],
            }
        ],
    }


def _parse_input_bundle(data: dict) -> tuple[dict, list[str]]:
    """Extract demographics + medication names (DCI) from input Bundle."""
    try:
        bundle = FHBundle.parse_obj(data)
    except Exception as exc:
        raise HTTPException(400, f"Invalid FHIR Bundle: {exc}")

    demo: dict = {}
    meds: list[str] = []

    for entry in bundle.entry or []:  # type: ignore[attr-defined]
        res = entry.resource
        if isinstance(res, Patient):
            demo = {
                "age": (
                    res.extension[0].valueUnsignedInt if res.extension else 0
                ),  # pragma: no cover
                "sex": res.gender or "U",
            }
        elif isinstance(res, MedicationStatement):
            # try get contained Medication
            if res.medication and res.medication.medicationCodeableConcept:
                meds.append(res.medication.medicationCodeableConcept.text.upper())
        elif isinstance(res, FHMedication):
            meds.append(res.code.text.upper() if res.code else "")

    if not meds:
        raise HTTPException(400, "Bundle must contain at least one Medication resource")
    return demo, meds


class Medication(BaseModel):
    name: str = Field(..., examples=["PARACETAMOL"])
    posology: str | None = Field(None, examples=["1 g x3/j"])


class Demographics(BaseModel):
    age: int = Field(..., ge=0, le=120)
    sex: str = Field(..., examples=["M", "F"])


class BMPRequest(BaseModel):
    patient_id: str
    demographics: Demographics
    medications: list[Medication]


class BMPResponse(BaseModel):
    status: str
    problems: list[str] = []
    recommendations: list[str] = []


# Allow Next.js dev server during development
origins = [
    "http://localhost:3000",
]

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# Middleware to record metrics
@app.middleware("http")
@REQUEST_TIME.time()
async def metrics_middleware(request, call_next):  # type: ignore[type-arg]
    response = await call_next(request)
    REQUEST_COUNT.labels(request.method, request.url.path, response.status_code).inc()
    return response


@app.post("/bmp/run", response_model=BMPResponse, tags=["bmp"])
async def run_bmp(
    request: BMPRequest,
    user_id: str = Depends(get_current_user_id),
) -> BMPResponse:
    """Run the BMP pipeline – MVP v0.2.

    Currently performs simple interaction detection using in-memory rules.
    LLM orchestration and advanced algorithms will be added next.
    """
    meds_raw = [m.name for m in request.medications]
    meds_norm = [normalize(n) for n in meds_raw]

    # Pydantic v2: dict() est déprécié → model_dump(); compat v1 conservée
    demo = (
        request.demographics.model_dump()  # type: ignore[attr-defined]
        if hasattr(request.demographics, "model_dump")
        else request.demographics.dict()
    )

    # Detect interactions and STOPP/START rules
    interactions = detect_interactions(meds_norm)
    stops, starts = evaluate_rules(demo, meds_norm)
    problems = interactions + stops

    # Anticholinergic burden (CIA / ACB)
    burden = compute_burden_score(meds_norm)
    if burden >= 3:
        problems.append(
            f"Charge anticholinergique élevée : score {burden} (CIA/ACB ≥3)"
        )

    # Initialize recommendations with START rules
    recommendations: list[str] = starts.copy()
    # Add interaction recommendation if any
    if interactions:
        recommendations.append(
            "Consulter les recommandations cliniques pour les interactions identifiées."
        )

    # Call LLM to generate high-level summary
    summary = generate_summary(demo, meds_norm, problems)

    return BMPResponse(
        status="ok",
        problems=problems,
        recommendations=[summary, *recommendations],
    )


# -------------------------- FHIR Bundle endpoint ---------------------------


@app.post("/bmp/fhir", tags=["bmp"])
async def run_bmp_fhir(
    request: BMPRequest, user_id: str = Depends(get_current_user_id)
):  # noqa: D401
    """Same analysis but returns a FHIR Bundle (JSON)."""

    result = await run_bmp(request, user_id)  # reuse logic
    bundle = build_bundle(
        patient=(
            request.demographics.model_dump()  # type: ignore[attr-defined]
            if hasattr(request.demographics, "model_dump")
            else request.demographics.dict()
        ),
        medications=[m.name for m in request.medications],
        detected_issues=result.problems,
        recommendations=result.recommendations,
    )
    return bundle


# ----------------------------- LLM utils -----------------------------------


@app.get("/models", summary="Liste des modèles LLM locaux disponibles")
async def get_models():  # noqa: D401
    return {"models": list_local_models()}


# Expose Prometheus metrics endpoint
@app.get("/metrics")
def metrics() -> Response:  # noqa: D401
    return Response(generate_latest(), media_type=CONTENT_TYPE_LATEST)


# ---------------------------- New FHIR endpoints ---------------------------


@app.get("/fhir/metadata", summary="FHIR CapabilityStatement")
def fhir_metadata():  # noqa: D401
    return _capability_statement()


@app.post("/fhir/Bundle/$bmp", summary="Analyse BMP à partir d'un Bundle FHIR")
async def fhir_bmp_bundle(
    bundle: dict, user_id: str = Depends(get_current_user_id)
):  # noqa: D401
    demo, meds = _parse_input_bundle(bundle)

    # Reuse existing pipeline
    bmp_req = BMPRequest(
        patient_id="anonymous",
        demographics=Demographics(**demo) if demo else Demographics(age=0, sex="U"),
        medications=[Medication(name=m) for m in meds],
    )

    result = await run_bmp(bmp_req, user_id)

    out_bundle = build_bundle(
        patient=bmp_req.demographics.model_dump(),
        medications=meds,
        detected_issues=result.problems,
        recommendations=result.recommendations,
    )
    return out_bundle


# Local dev convenience
if __name__ == "__main__":
    import uvicorn

    uvicorn.run("bmp_service.main:app", host="0.0.0.0", port=8000, reload=True)
