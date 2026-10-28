from __future__ import annotations

import contextlib
import logging
import time
import uuid
from contextlib import asynccontextmanager
from typing import Any, AsyncIterator

from fastapi import BackgroundTasks, FastAPI, Request, Response, status
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from prometheus_fastapi_instrumentator import Instrumentator

from app.callback import CallbackClient
from app.config import Settings, get_settings
from app.database import Database
from app.exceptions import ModelNotLoadedException, ScoringException, error_payload
from app.feature_extractor import FeatureExtractor
from app.kafka_worker import KafkaEventPublisher, KafkaScoringWorker
from app.model.model_loader import ModelLoader
from app.model.preprocessing import PreprocessingPipeline
from app.routers.scoring import build_health, build_model_info, router, score_application
from app.schemas import HealthResponse, ModelInfoResponse, ScoreRequest, ScoreResponse, ServiceInfoResponse
from app.scoring_pipeline import ScoringPipeline

logger = logging.getLogger(__name__)


def configure_logging(settings: Settings) -> None:
    logging.basicConfig(
        level=getattr(logging, settings.log_level.upper(), logging.INFO),
        format="%(asctime)s %(levelname)s [%(name)s] %(message)s",
    )


def create_app(
    settings: Settings | None = None,
    model_loader: ModelLoader | None = None,
    feature_extractor: FeatureExtractor | None = None,
    database: Database | None = None,
) -> FastAPI:
    settings = settings or get_settings()
    configure_logging(settings)

    @asynccontextmanager
    async def lifespan(app: FastAPI) -> AsyncIterator[None]:
        app.state.started_at = time.monotonic()
        app.state.settings = settings

        db = database or Database(settings)
        app.state.database = db
        if database is None and settings.db_connect_on_startup:
            try:
                await db.connect()
            except Exception as exc:
                logger.error("Database scoring tidak tersedia saat startup: %s", exc)

        loader = model_loader or ModelLoader(settings)
        app.state.model_loader = loader
        worker: KafkaScoringWorker | None = None
        publisher: KafkaEventPublisher | None = None

        try:
            if not loader.model_loaded:
                loader.load()
                preprocessing = PreprocessingPipeline(loader.scaler_artifact, loader.encoder_artifact)
                loader.warm_up(preprocessing.dummy_batch(5))
            else:
                preprocessing = PreprocessingPipeline(loader.scaler_artifact, loader.encoder_artifact)
            app.state.preprocessing = preprocessing

            extractor = feature_extractor or FeatureExtractor(db)
            app.state.feature_extractor = extractor
            app.state.pipeline = ScoringPipeline(extractor, loader, preprocessing, settings)

            callback = CallbackClient(settings)
            app.state.callback_client = callback
            publisher = KafkaEventPublisher(settings)
            await publisher.start()
            app.state.kafka_publisher = publisher
            worker = KafkaScoringWorker(settings, app.state.pipeline, publisher, callback)
            await worker.start()
            app.state.kafka_worker = worker
            logger.info("Scoring service siap model_version=%s", loader.model_version)
        except ModelNotLoadedException as exc:
            logger.error("Model scoring tidak siap: %s", exc)
            app.state.model_error = str(exc)
            app.state.pipeline = None

        try:
            yield
        finally:
            if worker is not None:
                await worker.stop()
            if publisher is not None:
                await publisher.stop()
            with contextlib.suppress(Exception):
                await db.close()

    app = FastAPI(
        title="Koperasi Digital — Scoring Service",
        description="Layanan inferensi AI credit scoring untuk UMKM.",
        version=settings.service_version,
        lifespan=lifespan,
    )
    app.state.settings = settings
    app.state.injected_model_loader = model_loader
    app.state.injected_feature_extractor = feature_extractor

    app.add_middleware(
        CORSMiddleware,
        allow_origins=settings.cors_origins,
        allow_credentials=True,
        allow_methods=["GET", "POST", "OPTIONS"],
        allow_headers=["Authorization", "Content-Type", "X-Correlation-Id", "X-Internal-Token"],
        expose_headers=["X-Correlation-Id"],
    )

    @app.middleware("http")
    async def correlation_id_middleware(request: Request, call_next: Any) -> Response:
        trace_id = request.headers.get("X-Correlation-Id") or str(uuid.uuid4())
        request.state.trace_id = trace_id
        response: Response = await call_next(request)
        response.headers["X-Correlation-Id"] = trace_id
        return response

    @app.exception_handler(ScoringException)
    async def scoring_exception_handler(request: Request, exc: ScoringException) -> JSONResponse:
        return JSONResponse(
            status_code=exc.status_code,
            content=error_payload(exc.code, exc.message, exc.details),
        )

    @app.exception_handler(RequestValidationError)
    async def validation_exception_handler(request: Request, exc: RequestValidationError) -> JSONResponse:
        details = [
            f"{'.'.join(str(part) for part in error.get('loc', []))}: {error.get('msg', 'invalid')}"
            for error in exc.errors()
        ]
        return JSONResponse(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            content=error_payload("VALIDATION_ERROR", "Payload request tidak valid", details),
        )

    @app.exception_handler(Exception)
    async def unexpected_exception_handler(request: Request, exc: Exception) -> JSONResponse:
        logger.exception("Kesalahan tidak terduga pada scoring service")
        return JSONResponse(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            content=error_payload("INTERNAL_ERROR", "Terjadi kesalahan pada server"),
        )

    app.include_router(router, prefix="/api/v1/scoring")

    @app.get("/", response_model=ServiceInfoResponse)
    async def root() -> ServiceInfoResponse:
        loader = getattr(app.state, "model_loader", None)
        return ServiceInfoResponse(
            service=settings.service_name,
            version=settings.service_version,
            environment=settings.app_env,
            model_version=loader.model_version if loader else settings.model_version,
            status="ready" if loader and loader.model_loaded else "degraded",
        )

    # Root aliases are kept for the platform health check and acceptance contract.
    @app.post("/score", response_model=ScoreResponse, include_in_schema=False)
    async def score_root(
        payload: ScoreRequest,
        background_tasks: BackgroundTasks,
        request: Request,
    ) -> ScoreResponse:
        return await score_application(payload, background_tasks, request)

    @app.get("/health", response_model=HealthResponse, include_in_schema=False)
    async def health_root(request: Request, response: Response) -> HealthResponse:
        payload, ready = build_health(request.app)
        if not ready:
            response.status_code = status.HTTP_503_SERVICE_UNAVAILABLE
        return payload

    @app.get("/model-info", response_model=ModelInfoResponse, include_in_schema=False)
    async def model_info_root(request: Request) -> ModelInfoResponse:
        return build_model_info(request.app)

    Instrumentator().instrument(app).expose(app, endpoint="/metrics", include_in_schema=False)
    return app


app = create_app()
