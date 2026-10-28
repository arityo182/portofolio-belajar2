from __future__ import annotations

import asyncio
import logging
import uuid
from datetime import datetime, timezone
from typing import Any

import httpx

from app.config import Settings
from app.schemas import ScoreResponse

logger = logging.getLogger(__name__)


def build_scoring_completed_event(
    score: ScoreResponse,
    trace_id: str | None = None,
) -> dict[str, Any]:
    return {
        "eventId": str(uuid.uuid4()),
        "eventType": "loan.scoring.completed",
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "traceId": trace_id or str(uuid.uuid4()),
        "version": "1.0",
        "payload": {
            "loanApplicationId": str(score.loan_application_id),
            "borrowerId": str(score.borrower_id),
            "riskScore": score.risk_score,
            "riskCategory": score.risk_category,
            "approvalRecommendation": score.approval_recommendation,
            "confidence": score.confidence,
            "features": score.features_used,
            "modelVersion": score.model_version,
            "processingTimeMs": score.processing_time_ms,
            "scoringStatus": "SUCCESS",
            "scoredAt": score.scored_at.isoformat(),
        },
    }


class CallbackClient:
    def __init__(self, settings: Settings, transport: httpx.AsyncBaseTransport | None = None) -> None:
        self.settings = settings
        self.transport = transport

    async def send(self, score: ScoreResponse, trace_id: str | None = None) -> bool:
        if not self.settings.callback_enabled:
            return False
        event = build_scoring_completed_event(score, trace_id)
        headers = {"Content-Type": "application/json"}
        if trace_id:
            headers["X-Correlation-Id"] = trace_id
        if self.settings.internal_callback_token:
            headers["X-Internal-Token"] = self.settings.internal_callback_token

        attempts = max(1, self.settings.callback_max_retries + 1)
        for attempt in range(1, attempts + 1):
            try:
                async with httpx.AsyncClient(
                    timeout=self.settings.callback_timeout_seconds,
                    transport=self.transport,
                ) as client:
                    response = await client.post(
                        self.settings.callback_url,
                        json=event,
                        headers=headers,
                    )
                if 200 <= response.status_code < 300:
                    logger.info(
                        "Callback scoring terkirim loanApplicationId=%s",
                        score.loan_application_id,
                    )
                    return True
                logger.warning(
                    "Callback scoring ditolak status=%s attempt=%s",
                    response.status_code,
                    attempt,
                )
            except httpx.HTTPError as exc:
                logger.warning("Callback scoring gagal attempt=%s: %s", attempt, exc)
            if attempt < attempts:
                await asyncio.sleep(0.25 * attempt)
        return False
