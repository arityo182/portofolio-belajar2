from __future__ import annotations

import logging
import time
from datetime import datetime, timezone

from app.config import Settings
from app.decision import calculate_confidence, decide
from app.feature_extractor import FeatureExtractor
from app.model.model_loader import ModelLoader
from app.model.preprocessing import PreprocessingPipeline
from app.schemas import ScoreRequest, ScoreResponse

logger = logging.getLogger(__name__)


class ScoringPipeline:
    def __init__(
        self,
        feature_extractor: FeatureExtractor,
        model_loader: ModelLoader,
        preprocessing: PreprocessingPipeline,
        settings: Settings,
    ) -> None:
        self.feature_extractor = feature_extractor
        self.model_loader = model_loader
        self.preprocessing = preprocessing
        self.settings = settings

    async def score(self, request: ScoreRequest) -> ScoreResponse:
        started = time.perf_counter()
        if request.features:
            feature_set = FeatureExtractor.from_overrides(request.features)
        else:
            feature_set = await self.feature_extractor.extract(
                request.loan_application_id, request.borrower_id
            )

        raw_features = feature_set.merged()
        vector = self.preprocessing.transform(raw_features)
        risk_raw, default_probability = self.model_loader.predict(vector)
        risk_score = self._risk_score(raw_features, risk_raw, default_probability)
        risk_category, recommendation = decide(risk_score)
        confidence = calculate_confidence(default_probability)
        processing_time_ms = max(0, int((time.perf_counter() - started) * 1000))

        return ScoreResponse(
            loan_application_id=request.loan_application_id,
            borrower_id=request.borrower_id,
            risk_score=round(risk_score, 2),
            risk_category=risk_category,
            approval_recommendation=recommendation,
            confidence=confidence,
            features_used=self.preprocessing.features_used(raw_features),
            model_version=self.model_loader.model_version,
            processing_time_ms=processing_time_ms,
            scored_at=datetime.now(timezone.utc),
        )

    @staticmethod
    def _risk_score(features: dict[str, float], risk_raw: float, default_probability: float | None) -> float:
        if risk_raw is None:
            if default_probability is None:
                raise ValueError("Model tidak menghasilkan skor risiko")
            risk_raw = 1.0 - default_probability
        score = float(risk_raw)
        if 0.0 <= score <= 1.0:
            score *= 100.0
        return max(0.0, min(100.0, score))
