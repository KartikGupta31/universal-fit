
import hmac
import logging
import os
from typing import Optional

from fastapi import Depends, FastAPI, Header, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from .models import RecommendRequest, Recommendation
from .scoring import recommend

logger = logging.getLogger("universalfit.engine")

app = FastAPI(
    title="Universal Fit Recommendation Engine",
    version="0.1.0",
    description="Deterministic, explainable size scoring. Called by the Spring Boot backend.",
)


def _safe_validation_errors(exc: RequestValidationError) -> list:
    """Return validation locations and types without exposing submitted values."""
    return [
        {
            "loc": [str(part) for part in err.get("loc", ())],
            "msg": "Invalid request data",
            "type": str(err.get("type", "")),
        }
        for err in exc.errors()
    ]


@app.exception_handler(RequestValidationError)
async def validation_error_handler(
    request: Request,
    exc: RequestValidationError,
) -> JSONResponse:
    errors = _safe_validation_errors(exc)

    for err in errors:
        logger.warning(
            "Request validation failed: path=%s loc=%s type=%s",
            request.url.path,
            ".".join(err["loc"]),
            err["type"],
        )

    return JSONResponse(
        status_code=422,
        content={"detail": errors},
    )


def require_api_key(x_api_key: Optional[str] = Header(default=None)) -> None:
    """If ENGINE_API_KEY is set, callers must send it. Unset means local development."""
    expected = os.getenv("ENGINE_API_KEY")

    if not expected:
        return

    if not x_api_key or not hmac.compare_digest(x_api_key, expected):
        raise HTTPException(
            status_code=401,
            detail="Invalid or missing API key",
        )


@app.get("/health")
def health() -> dict:
    return {"status": "ok"}


@app.post(
    "/v1/recommend",
    response_model=Recommendation,
    response_model_by_alias=True,
    dependencies=[Depends(require_api_key)],
)
def recommend_size(request: RecommendRequest) -> Recommendation:
    return recommend(request)
