from __future__ import annotations

import time
import uuid
from datetime import datetime, timezone
from typing import Any
from uuid import UUID

import httpx
import numpy as np
import pytest
from fastapi.testclient import TestClient

from app.callback import CallbackClient, build_scoring_completed_event
from app.config import Settings
from app.decision import calculate_confidence, decide
from app.exceptions import FeatureExtractionException, ModelNotLoadedException
from app.feature_extractor import FeatureExtractor, FeatureSet
from app.main import create_app
from app.model.model_loader import ModelLoader
from app.model.preprocessing import DEFAULT_FEATURES, PreprocessingPipeline
from app.schemas import ScoreResponse


class FakeModelLoader:
    def __init__(self, risk_raw: float = 0.82, default_probability: float | None = 0.18) -> None:
        self.risk_raw = risk_raw
        self.default_probability = default_probability
        self._ready = False
        self.load_calls = 0
        self.warmup_samples = 0
        self.last_vector: np.ndarray | None = None
        self._metadata = {
            "model_version": "v1.0.0",
            "trained_at": "2026-10-01T00:00:00Z",
            "metrics": {"auc_roc": 0.83, "gini": 0.66},
        }

    @property
    def model_loaded(self) -> bool:
        return self._ready

    @property
    def model_version(self) -> str:
        return "v1.0.0"

    @property
    def scaler_artifact(self) -> dict[str, Any]:
        return {}

    @property
    def encoder_artifact(self) -> dict[str, Any]:
        return {}

    @property
    def metadata(self) -> dict[str, Any]:
        return self._metadata

    def load(self) -> None:
        self.load_calls += 1
        self._ready = True

    def warm_up(self, dummy_batch: np.ndarray) -> None:
        self.warmup_samples = dummy_batch.shape[0]
        self._ready = True

    def predict(self, feature_vector: np.ndarray) -> tuple[float, float | None]:
        self.last_vector = feature_vector
        return self.risk_raw, self.default_probability


class BrokenModelLoader(FakeModelLoader):
    def load(self) -> None:
        raise ModelNotLoadedException("Model tidak tersedia untuk test")


class FakeFeatureExtractor:
    def __init__(self) -> None:
        self.calls: list[tuple[UUID, UUID]] = []

    async def extract(self, loan_application_id: UUID, borrower_id: UUID) -> FeatureSet:
        self.calls.append((loan_application_id, borrower_id))
        return FeatureSet(values=dict(DEFAULT_FEATURES), business_type="KULINER", loan_purpose="modal kerja")


class FailingFeatureExtractor:
    async def extract(self, loan_application_id: UUID, borrower_id: UUID) -> FeatureSet:
        raise FeatureExtractionException("Data profil tidak lengkap", ["profil tidak ditemukan"])


def make_settings(**overrides: Any) -> Settings:
    values = {
        "kafka_enabled": False,
        "callback_enabled": False,
        "db_connect_on_startup": False,
        "model_version": "v1.0.0",
        "log_level": "WARNING",
    }
    values.update(overrides)
    return Settings(_env_file=None, **values)


def make_client(
    loader: FakeModelLoader | None = None,
    extractor: FakeFeatureExtractor | FailingFeatureExtractor | None = None,
) -> tuple[TestClient, FakeModelLoader, FakeFeatureExtractor | FailingFeatureExtractor]:
    loader = loader or FakeModelLoader()
    extractor = extractor or FakeFeatureExtractor()
    app = create_app(make_settings(), model_loader=loader, feature_extractor=extractor)
    return TestClient(app), loader, extractor


@pytest.fixture
def client() -> TestClient:
    test_client, _, _ = make_client()
    with test_client as active:
        yield active


@pytest.fixture
def loader_client() -> tuple[TestClient, FakeModelLoader, FakeFeatureExtractor]:
    test_client, loader, extractor = make_client()
    with test_client as active:
        yield active, loader, extractor


def test_health_reports_model_loaded_after_warmup() -> None:
    test_client, loader, _ = make_client()
    with test_client as client:
        response = client.get("/health")
        assert response.status_code == 200
        body = response.json()
        assert body["status"] == "UP"
        assert body["model_loaded"] is True
        assert body["model_version"] == "v1.0.0"
        assert body["uptime_seconds"] >= 0
        assert loader.warmup_samples == 5


def test_prefixed_health_returns_200() -> None:
    test_client, _, _ = make_client()
    with test_client as client:
        response = client.get("/api/v1/scoring/health")
        assert response.status_code == 200
        assert response.json()["model_loaded"] is True


def test_model_info_contains_version_metrics_and_features() -> None:
    test_client, _, _ = make_client()
    with test_client as client:
        response = client.get("/api/v1/scoring/model-info")
        assert response.status_code == 200
        body = response.json()
        assert body["model_version"] == "v1.0.0"
        assert body["metrics"]["auc_roc"] == 0.83
        assert body["n_features"] == 28
        assert len(body["feature_order"]) == 28


def test_score_with_provided_features_returns_complete_response_quickly() -> None:
    test_client, loader, _ = make_client()
    loan_id = uuid.uuid4()
    borrower_id = uuid.uuid4()
    payload = {
        "loan_application_id": str(loan_id),
        "borrower_id": str(borrower_id),
        "features": {
            "payment_consistency_score": 85.0,
            "debt_to_income_ratio": 0.35,
            "requested_amount_normalized": 0.4,
            "tenor_normalized": 0.5,
            "business_type_code": 0,
            "loan_purpose_code": 0,
        },
    }
    with test_client as client:
        started = time.perf_counter()
        response = client.post("/api/v1/scoring/score", json=payload)
        elapsed = time.perf_counter() - started

    assert response.status_code == 200
    body = response.json()
    assert body["loan_application_id"] == str(loan_id)
    assert body["risk_score"] == 82.0
    assert body["risk_category"] == "LOW"
    assert body["approval_recommendation"] == "APPROVE"
    assert body["confidence"] == 0.82
    assert body["model_version"] == "v1.0.0"
    assert body["processing_time_ms"] < 2000
    assert len(body["features_used"]) == 20
    assert body["features_used"]["payment_consistency_score"] == 85.0
    assert body["scored_at"]
    assert elapsed < 2.0
    assert loader.last_vector is not None
    assert loader.last_vector.shape == (28,)


def test_root_score_alias_works() -> None:
    test_client, _, _ = make_client()
    payload = {
        "loan_application_id": str(uuid.uuid4()),
        "borrower_id": str(uuid.uuid4()),
        "features": dict(DEFAULT_FEATURES),
    }
    with test_client as client:
        response = client.post("/score", json=payload)
    assert response.status_code == 200
    assert response.json()["risk_category"] == "LOW"


def test_score_without_features_uses_feature_extractor() -> None:
    test_client, _, extractor = make_client()
    loan_id = uuid.uuid4()
    borrower_id = uuid.uuid4()
    with test_client as client:
        response = client.post(
            "/api/v1/scoring/score",
            json={"loan_application_id": str(loan_id), "borrower_id": str(borrower_id)},
        )
    assert response.status_code == 200
    assert extractor.calls == [(loan_id, borrower_id)]


def test_score_with_validation_error_returns_structured_error() -> None:
    test_client, _, _ = make_client()
    with test_client as client:
        response = client.post("/api/v1/scoring/score", json={"borrower_id": str(uuid.uuid4())})
    assert response.status_code == 422
    body = response.json()
    assert body["success"] is False
    assert body["code"] == "VALIDATION_ERROR"
    assert body["details"]


def test_score_when_feature_extraction_fails_returns_422() -> None:
    test_client, _, _ = make_client(extractor=FailingFeatureExtractor())
    with test_client as client:
        response = client.post(
            "/api/v1/scoring/score",
            json={"loan_application_id": str(uuid.uuid4()), "borrower_id": str(uuid.uuid4())},
        )
    assert response.status_code == 422
    assert response.json()["code"] == "FEATURE_EXTRACTION_FAILED"


def test_health_and_score_when_model_fails_to_load() -> None:
    test_client, _, _ = make_client(loader=BrokenModelLoader())
    with test_client as client:
        health = client.get("/health")
        score = client.post(
            "/score",
            json={
                "loan_application_id": str(uuid.uuid4()),
                "borrower_id": str(uuid.uuid4()),
                "features": dict(DEFAULT_FEATURES),
            },
        )
    assert health.status_code == 503
    assert health.json()["model_loaded"] is False
    assert score.status_code == 503
    assert score.json()["code"] == "MODEL_NOT_LOADED"


@pytest.mark.parametrize(
    ("risk_score", "expected_category", "expected_recommendation"),
    [
        (70.0, "LOW", "APPROVE"),
        (100.0, "LOW", "APPROVE"),
        (69.99, "MEDIUM", "REVIEW"),
        (40.0, "MEDIUM", "REVIEW"),
        (39.99, "HIGH", "REJECT"),
        (0.0, "HIGH", "REJECT"),
    ],
)
def test_decision_matrix_boundaries(
    risk_score: float, expected_category: str, expected_recommendation: str
) -> None:
    category, recommendation = decide(risk_score)
    assert category == expected_category
    assert recommendation == expected_recommendation


def test_confidence_is_distance_from_half() -> None:
    assert calculate_confidence(0.9) == 0.9
    assert calculate_confidence(0.1) == 0.9
    assert calculate_confidence(0.5) == 0.5
    assert calculate_confidence(None) == 0.5


def test_preprocessing_outputs_28_features_with_one_hot_encoding() -> None:
    pipeline = PreprocessingPipeline()
    features = dict(DEFAULT_FEATURES)
    features["business_type_code"] = 0.0
    features["loan_purpose_code"] = 1.0
    vector = pipeline.transform(features)

    assert pipeline.n_features == 28
    assert vector.shape == (28,)
    order = pipeline.feature_order
    assert vector[order.index("business_type_KULINER")] == 1.0
    assert vector[order.index("loan_purpose_INVENTARIS")] == 1.0
    assert sum(vector[order.index(f"business_type_{name}")] for name in ("KULINER", "RETAIL", "JASA", "PERTANIAN", "LAINNYA")) == 1.0


def test_preprocessing_defaults_and_features_used() -> None:
    pipeline = PreprocessingPipeline()
    vector = pipeline.transform({})
    used = pipeline.features_used({})

    assert vector.shape == (28,)
    assert len(used) == 20
    assert used["payment_consistency_score"] == DEFAULT_FEATURES["payment_consistency_score"]


def test_feature_extractor_from_overrides_merges_defaults() -> None:
    feature_set = FeatureExtractor.from_overrides({"payment_consistency_score": 90.0, "unknown": 1.0})
    assert feature_set.values["payment_consistency_score"] == 90.0
    assert feature_set.values["has_npwp"] == 0.0
    assert feature_set.business_type == "UNKNOWN"


@pytest.mark.asyncio
async def test_feature_extractor_calculates_features_from_database_rows() -> None:
    loan_id = uuid.uuid4()
    borrower_id = uuid.uuid4()
    application = {
        "id": loan_id,
        "borrower_id": borrower_id,
        "amount": 10_000_000,
        "tenor_months": 12,
        "purpose": "Pembelian peralatan dapur",
        "max_amount": 50_000_000,
        "max_tenor_months": 24,
        "full_name": "Budi",
        "date_of_birth": "1990-01-01",
        "gender": "M",
        "address": "Jl. Merdeka",
        "city": "Bandung",
        "province": "Jawa Barat",
        "bank_name": "BCA",
        "bank_account_encrypted": "encrypted",
        "income_monthly": 12_000_000,
        "business_type": "KULINER",
        "business_name": "Warung Budi",
        "npwp_encrypted": "encrypted",
        "is_verified": True,
        "ktp_photo_url": "s3://ktp",
        "selfie_photo_url": "s3://selfie",
        "profile_created_at": datetime(2024, 1, 1, tzinfo=timezone.utc),
    }

    class FakeDatabase:
        async def fetchrow(self, query: str, *args: Any) -> dict[str, Any] | None:
            if "auth_schema.borrower_profiles" in query:
                return application
            if "FROM loan_schema.loan_applications la" in query:
                return application
            if "due_count" in query:
                return {"due_count": 4, "ontime_count": 3}
            if "previous_loans" in query:
                return {"previous_loans": 2, "previous_defaults": 1}
            if "active_debt" in query:
                return {"active_debt": 1_200_000}
            if "payment_count" in query:
                return {"total": 6_000_000, "payment_count": 6}
            return None

        async def fetch(self, query: str, *args: Any) -> list[dict[str, Any]]:
            return [{"month": "2026-01-01", "amount": 1_000_000}, {"month": "2026-02-01", "amount": 1_000_000}]

    extractor = FeatureExtractor(FakeDatabase())
    feature_set = await extractor.extract(loan_id, borrower_id)

    assert feature_set.values["payment_consistency_score"] == 75.0
    assert feature_set.values["requested_amount_normalized"] == 0.2
    assert feature_set.values["tenor_normalized"] == 0.5
    assert feature_set.values["has_npwp"] == 1.0
    assert feature_set.values["has_formal_address"] == 1.0
    assert feature_set.values["previous_loan_count"] == 2.0
    assert feature_set.values["previous_default_count"] == 1.0
    assert feature_set.values["is_new_borrower"] == 0.0
    assert feature_set.values["verification_completeness_score"] == 100.0
    assert feature_set.values["profile_completeness_score"] == 100.0
    assert feature_set.values["debt_to_income_ratio"] == pytest.approx(0.1)


def sample_score() -> ScoreResponse:
    return ScoreResponse(
        loan_application_id=uuid.uuid4(),
        borrower_id=uuid.uuid4(),
        risk_score=82.0,
        risk_category="LOW",
        approval_recommendation="APPROVE",
        confidence=0.82,
        features_used={"payment_consistency_score": 85.0},
        model_version="v1.0.0",
        processing_time_ms=12,
        scored_at=datetime.now(timezone.utc),
    )


@pytest.mark.asyncio
async def test_callback_disabled_returns_false() -> None:
    client = CallbackClient(make_settings(callback_enabled=False))
    assert await client.send(sample_score(), "trace") is False


@pytest.mark.asyncio
async def test_callback_posts_completed_event_to_loan_service() -> None:
    captured: dict[str, Any] = {}

    def handler(request: httpx.Request) -> httpx.Response:
        captured["url"] = str(request.url)
        captured["token"] = request.headers.get("X-Internal-Token")
        captured["body"] = __import__("json").loads(request.content)
        return httpx.Response(200, json={"success": True})

    settings = make_settings(
        callback_enabled=True,
        loan_service_url="http://loan.test",
        internal_callback_token="token-1",
    )
    client = CallbackClient(settings, transport=httpx.MockTransport(handler))
    score = sample_score()

    result = await client.send(score, "trace-1")

    assert result is True
    assert captured["url"] == "http://loan.test/api/v1/loans/internal/scoring/completed"
    assert captured["token"] == "token-1"
    assert captured["body"]["eventType"] == "loan.scoring.completed"
    assert captured["body"]["payload"]["loanApplicationId"] == str(score.loan_application_id)


def test_scoring_completed_event_contains_loan_service_contract() -> None:
    score = sample_score()
    event = build_scoring_completed_event(score, "trace-1")
    assert event["eventType"] == "loan.scoring.completed"
    assert event["traceId"] == "trace-1"
    assert event["version"] == "1.0"
    assert event["payload"]["loanApplicationId"] == str(score.loan_application_id)
    assert event["payload"]["riskCategory"] == "LOW"
    assert event["payload"]["approvalRecommendation"] == "APPROVE"


def test_model_loader_extracts_outputs_without_tensorflow() -> None:
    loader = ModelLoader(make_settings())
    risk, default_probability = loader._extract_outputs(
        [np.asarray([[0.82]], dtype=np.float32), np.asarray([[0.18]], dtype=np.float32)]
    )
    assert risk == pytest.approx(0.82)
    assert default_probability == pytest.approx(0.18)

    risk_from_single, default_from_single = loader._extract_outputs(np.asarray([[0.6]], dtype=np.float32))
    assert risk_from_single == pytest.approx(0.6)
    assert default_from_single is None
