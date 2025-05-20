"""Entry point for Hygie-AI Bilan Médication Partagé (BMP) service.
Stub version – will be expanded with clinical rules & LLM orchestration.
"""

from datetime import datetime, timezone

from fastapi import Depends, FastAPI, HTTPException, Response, Query
import inspect
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
from .llm_orchestrator import generate_summary, list_local_models, _resolve_name, _cached_local_generate, ensemble_generate
from .rules import detect_interactions
from .security import get_current_user, require_role, CurrentUser
from .anticholinergic import compute_burden_score
from .stopp_start import evaluate_rules

# Optional OpenTelemetry instrumentation
try:
    from opentelemetry import trace
    from opentelemetry.sdk.resources import SERVICE_NAME, Resource
    from opentelemetry.sdk.trace import TracerProvider
    from opentelemetry.sdk.trace.export import BatchSpanProcessor
    from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter
    from opentelemetry.instrumentation.fastapi import FastAPIInstrumentor
except ImportError:
    FastAPIInstrumentor = None

app = FastAPI(
    title="Hygie-AI BMP API",
    version=settings.VERSION if hasattr(settings, "VERSION") else "0.1.0",
    description="API for running shared medication review analyses.",
)

# Apply instrumentation if available
if FastAPIInstrumentor:
    resource = Resource.create({SERVICE_NAME: "hygie-bmp-service"})
    provider = TracerProvider(resource=resource)
    provider.add_span_processor(BatchSpanProcessor(OTLPSpanExporter()))
    trace.set_tracer_provider(provider)
    FastAPIInstrumentor.instrument_app(app)

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


class ChatRequest(BaseModel):
    prompt: str
    model: str | None = None


class ChatResponse(BaseModel):
    response: str


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
    strategy: str = Query("hybrid", description="LLM orchestration strategy", regex="^(hybrid|ensemble|cascade)$"),
    current_user: CurrentUser = Depends(get_current_user),
) -> BMPResponse:
    """Run the BMP pipeline – MVP v0.2.

    Currently performs simple interaction detection using in-memory rules.
    LLM orchestration and advanced algorithms will be added next.
    """
    meds_raw = [m.name for m in request.medications]
    meds_norm = [normalize(n) for n in meds_raw]

    # Extract demographics via Pydantic v2
    demo = request.demographics.model_dump()

    # Detect interactions and STOPP/START rules
    try:
        interactions = detect_interactions(meds_norm)
        stops, starts = evaluate_rules(demo, meds_norm)
        problems = interactions + stops
    except HTTPException:
        raise
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc))

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

    # Call LLM to generate high-level summary with chosen strategy
    # Determine how to call generate_summary based on signature (to support stubs)
    sig = inspect.signature(generate_summary)
    if len(sig.parameters) >= 4:
        summary = generate_summary(demo, meds_norm, problems, strategy)
    else:
        summary = generate_summary(demo, meds_norm, problems)
    recommendations.insert(0, summary)

    return BMPResponse(
        status="ok",
        problems=problems,
        recommendations=recommendations,
    )


# -------------------------- FHIR Bundle endpoint ---------------------------


@app.post("/bmp/fhir", tags=["bmp"])
async def run_bmp_fhir(
    request: BMPRequest,
    strategy: str = Query("hybrid", description="LLM orchestration strategy", regex="^(hybrid|ensemble|cascade)$"),
    current_user: CurrentUser = Depends(get_current_user),
) -> dict:
    """Same analysis but returns a FHIR Bundle (JSON)."""

    # Execute BMP pipeline with chosen strategy
    bmp_sig2 = inspect.signature(run_bmp)
    if len(bmp_sig2.parameters) >= 3:
        result = await run_bmp(request, strategy, current_user)
    elif len(bmp_sig2.parameters) == 2:
        result = await run_bmp(request, current_user)
    else:
        result = await run_bmp(request)

    bundle = build_bundle(
        patient=request.demographics.model_dump(),
        medications=[m.name for m in request.medications],
        detected_issues=result.problems,
        recommendations=result.recommendations,
    )
    return bundle


# ----------------------------- LLM utils -----------------------------------


@app.get("/models", summary="Liste des modèles LLM locaux disponibles")
async def get_models(current_user: CurrentUser = Depends(get_current_user)):  # noqa: D401
    return {"models": list_local_models()}


# Chat endpoint
@app.post("/chat", response_model=ChatResponse, tags=["chat"])
async def chat_endpoint(
    req: ChatRequest,
    current_user: CurrentUser = Depends(require_role("pro")),
) -> ChatResponse:
    """Chat endpoint (stub for now)."""
    # Stubbed until multi-LLM chat is implemented
    return ChatResponse(response="LLM stub")


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
    bundle: dict,
    strategy: str = Query("hybrid", description="LLM orchestration strategy", regex="^(hybrid|ensemble|cascade)$"),
    current_user: CurrentUser = Depends(get_current_user),
) -> dict:  # noqa: D401
    demo, meds = _parse_input_bundle(bundle)

    # Reuse existing pipeline with chosen strategy
    bmp_req = BMPRequest(
        patient_id="anonymous",
        demographics=Demographics(**demo) if demo else Demographics(age=0, sex="U"),
        medications=[Medication(name=m) for m in meds],
    )
    bmp_sig = inspect.signature(run_bmp)
    if len(bmp_sig.parameters) >= 3:
        result = await run_bmp(bmp_req, strategy, current_user)
    elif len(bmp_sig.parameters) == 2:
        result = await run_bmp(bmp_req, current_user)
    else:
        result = await run_bmp(bmp_req)

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
