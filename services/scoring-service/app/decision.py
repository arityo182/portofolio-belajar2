from __future__ import annotations

from app.schemas import ApprovalRecommendation, RiskCategory


def decide(risk_score: float) -> tuple[RiskCategory, ApprovalRecommendation]:
    """Map a 0-100 risk score to the business decision matrix."""
    score = max(0.0, min(100.0, float(risk_score)))
    if score >= 70.0:
        return "LOW", "APPROVE"
    if score >= 40.0:
        return "MEDIUM", "REVIEW"
    return "HIGH", "REJECT"


def calculate_confidence(default_probability: float | None) -> float:
    if default_probability is None:
        return 0.5
    probability = max(0.0, min(1.0, float(default_probability)))
    return round(max(probability, 1.0 - probability), 4)
