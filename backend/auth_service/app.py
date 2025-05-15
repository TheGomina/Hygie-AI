from bson import ObjectId
from fastapi import Depends, FastAPI, HTTPException, Response, status
from fastapi.security import OAuth2PasswordBearer

from .database import users_col
from .models import Token, UserCreate, UserDB
from .security import create_access_token, hash_password, verify_password

# create app
app = FastAPI(title="Hygie Auth Service", version="0.1.0")

# metrics
from prometheus_client import CONTENT_TYPE_LATEST, Counter, Summary, generate_latest

# Prometheus metrics
REQUEST_COUNT = Counter(
    "auth_requests_total",
    "Total HTTP requests",
    ["method", "endpoint", "http_status"],
)
REQUEST_TIME = Summary(
    "auth_request_processing_seconds", "Time spent processing request"
)


# Middleware to record metrics
@app.middleware("http")
@REQUEST_TIME.time()
async def metrics_middleware(request, call_next):  # type: ignore[type-arg]
    response = await call_next(request)
    REQUEST_COUNT.labels(request.method, request.url.path, response.status_code).inc()
    return response


# Expose /metrics for Prometheus
@app.get("/metrics")
def metrics() -> Response:
    return Response(generate_latest(), media_type=CONTENT_TYPE_LATEST)


oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/auth/login")


def _user_to_db(u: UserCreate) -> UserDB:
    return UserDB(**u.dict(), hashed_password=hash_password(u.password))


def _user_from_mongo(doc) -> UserDB | None:
    if not doc:
        return None
    return UserDB(
        id=str(doc["_id"]),
        email=doc["email"],
        full_name=doc.get("full_name"),
        hashed_password=doc["hashed_password"],
        role=doc.get("role", "pharmacist"),
    )


@app.post("/auth/register", response_model=Token, status_code=status.HTTP_201_CREATED)
def register(payload: UserCreate):
    if users_col().find_one({"email": payload.email}):
        raise HTTPException(status_code=400, detail="Email already registered")
    user_db = _user_to_db(payload)
    res = users_col().insert_one(user_db.dict(exclude={"id"}))
    token = create_access_token(str(res.inserted_id))
    return Token(access_token=token)


@app.post("/auth/login", response_model=Token)
def login(payload: UserCreate):
    doc = users_col().find_one({"email": payload.email})
    user = _user_from_mongo(doc)
    if not user or not verify_password(payload.password, user.hashed_password):
        raise HTTPException(status_code=401, detail="Invalid credentials")
    return Token(access_token=create_access_token(user.id))
