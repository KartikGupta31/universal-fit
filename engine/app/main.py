
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
    description="Recommendation engine for Universal Fit.",
)


def require_api_key(
    x_api_key: Optional[str] = Header(default=None, alias="X-API-Key"),
) -> None:
    expected_key = os.getenv("ENGINE_API_KEY")

    if not expected_key:
        return

    if not x_api_key or not hmac.compare_digest(x_api_key, expected_key):
        raise HTTPException(
            status_code=401,
            detail="Invalid or missing API key.",
        )


def _safe_validation_errors(exc: RequestValidationError) -> list[dict]:
    safe_errors = []

    for error in exc.errors():
        location = error.get("loc", ())
        error_type = error.get("type", "validation_error")

        safe_errors.append(
            {
                "loc": [str(part) for part in location],
                "msg": "Invalid request data",
                "type": str(error_type),
            }
        )

    return safe_errors


@app.exception_handler(RequestValidationError)
async def validation_error_handler(
    request: Request,
    exc: RequestValidationError,
) -> JSONResponse:
    errors = _safe_validation_errors(exc)

    # Inspect only metadata. Never log request values or measurements.
    body = exc.body

    if isinstance(body, (bytes, bytearray)):
        body_type = "bytes"
        body_size = len(body)
        body_keys = []
    elif isinstance(body, dict):
        body_type = "object"
        body_size = None
        body_keys = sorted(str(key) for key in body.keys())
    elif body is None:
        body_type = "none"
        body_size = None
        body_keys = []
    else:
        body_type = type(body).__name__
        body_size = None
        body_keys = []

    logger.warning(
        "Validation body metadata: path=%s body_type=%s "
        "body_size=%s top_level_keys=%s",
        request.url.path,
        body_type,
        body_size,
        body_keys,
    )

    for error in errors:
        logger.warning(
            "Request validation failed: path=%s loc=%s type=%s",
            request.url.path,
            ".".join(error["loc"]),
            error["type"],
        )

    return JSONResponse(
        status_code=422,
        content={"detail": errors},
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
