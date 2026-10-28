from __future__ import annotations

from datetime import datetime
from typing import Literal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field

RiskCategory = Literal["LOW", "MEDIUM", "HIGH"]
ApprovalRecommendation = Literal["APPROVE", "REVIEW", "REJECT"]


class ApiModel(BaseModel):
    model_config = ConfigDict(protected_namespaces=())


class ScoreRequest(ApiModel):
    model_config = ConfigDict(extra="forbid", protected_namespaces=())

    loan_application_id: UUID
    borrower_id: UUID
    features: dict[str, float] | None = Field(
        default=None,
        description="Opsional. Bila diisi, feature extractor DB dilewati.",
    )


class ScoreResponse(ApiModel):
    loan_application_id: UUID
    borrower_id: UUID
    risk_score: float = Field(ge=0.0, le=100.0)
    risk_category: RiskCategory
    approval_recommendation: ApprovalRecommendation
    confidence: float = Field(ge=0.0, le=1.0)
    features_used: dict[str, float]
    model_version: str
    processing_time_ms: int = Field(ge=0)
    scored_at: datetime


class HealthResponse(ApiModel):
    status: str
    service: str
    version: str
    model_loaded: bool
    model_version: str
    uptime_seconds: float
    timestamp: datetime


class ModelInfoResponse(ApiModel):
    model_version: str
    trained_at: str
    metrics: dict[str, float]
    n_features: int
    feature_order: list[str]
    canary_traffic_pct: float


class ServiceInfoResponse(ApiModel):
    service: str
    version: str
    environment: str
    model_version: str
    status: str


class ErrorResponse(ApiModel):
    success: bool = False
    code: str
    message: str
    details: list[str] = Field(default_factory=list)
    timestamp: datetime
