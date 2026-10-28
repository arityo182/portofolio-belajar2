from __future__ import annotations

import logging
import time
from datetime import datetime, timezone
from typing import Any

from fastapi import APIRouter, BackgroundTasks, Request, Response, status

from app.exceptions import ModelNotLoadedException
from app.schemas import (
    HealthResponse,
    ModelInfoResponse,
    ScoreRequest,
    ScoreResponse,
)
from app.scoring_pipeline import ScoringPipeline

logger = logging.getLogger(__name__)

router = APIRouter(tags=["scoring"])


def get_pipeline(request: Request) -> ScoringPipeline:
    pipeline = getattr(request.app.state, "pipeline", None)
    model_loader = getattr(request.app.state, "model_loader", None)
    if pipeline is None or model_loader is None or not model_loader.model_loaded:
        raise ModelNotLoadedException()
    return pipeline


def build_health(app: Any) -> tuple[HealthResponse, bool]:
    settings = app.state.settings
    loader = getattr(app.state, "model_loader", None)
    started_at = getattr(app.state, "started_at", time.monotonic())
    loaded = bool(loader and loader.model_loaded)
    model_version = loader.model_version if loader else settings.model_version
    return (
        HealthResponse(
            status="UP" if loaded else "DOWN",
            service=settings.service_name,
            version=settings.service_version,
            model_loaded=loaded,
            model_version=model_version,
            uptime_seconds=round(time.monotonic() - started_at, 3),
            timestamp=datetime.now(timezone.utc),
        ),
        loaded,
    )


def build_model_info(app: Any) -> ModelInfoResponse:
    settings = app.state.settings
    loader = getattr(app.state, "model_loader", None)
    preprocessing = getattr(app.state, "preprocessing", None)
    if loader is None or not loader.model_loaded or preprocessing is None:
        raise ModelNotLoadedException()
    metadata = loader.metadata or {}
    metrics = metadata.get("metrics") or settings.model_metrics
    return ModelInfoResponse(
        model_version=loader.model_version,
        trained_at=str(metadata.get("trained_at", settings.model_trained_at)),
        metrics={key: float(value) for key, value in metrics.items()},
        n_features=preprocessing.n_features,
        feature_order=preprocessing.feature_order,
        canary_traffic_pct=float(settings.canary_traffic_pct),
    )


async def score_application(
    payload: ScoreRequest,
    background_tasks: BackgroundTasks,
    request: Request,
) -> ScoreResponse:
    pipeline = get_pipeline(request)
    response = await pipeline.score(payload)
    callback = getattr(request.app.state, "callback_client", None)
    if callback is not None and request.app.state.settings.callback_enabled:
        trace_id = getattr(request.state, "trace_id", None) or request.headers.get("X-Correlation-Id")
        background_tasks.add_task(callback.send, response, trace_id)
    return response


@router.post("/score", response_model=ScoreResponse)
async def score(
    payload: ScoreRequest,
    background_tasks: BackgroundTasks,
    request: Request,
) -> ScoreResponse:
    return await score_application(payload, background_tasks, request)


@router.get("/health", response_model=HealthResponse)
async def health(request: Request, response: Response) -> HealthResponse:
    payload, ready = build_health(request.app)
    if not ready:
        response.status_code = status.HTTP_503_SERVICE_UNAVAILABLE
    return payload


@router.get("/model-info", response_model=ModelInfoResponse)
async def model_info(request: Request) -> ModelInfoResponse:
    return build_model_info(request.app)
