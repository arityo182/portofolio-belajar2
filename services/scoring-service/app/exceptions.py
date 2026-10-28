from __future__ import annotations

from typing import Any


class ScoringException(Exception):
    def __init__(
        self,
        code: str,
        message: str,
        status_code: int = 500,
        details: list[str] | None = None,
    ) -> None:
        super().__init__(message)
        self.code = code
        self.message = message
        self.status_code = status_code
        self.details = details or []


class ModelNotLoadedException(ScoringException):
    def __init__(self, message: str = "Model scoring belum siap") -> None:
        super().__init__("MODEL_NOT_LOADED", message, status_code=503)


class FeatureExtractionException(ScoringException):
    def __init__(self, message: str, details: list[str] | None = None) -> None:
        super().__init__("FEATURE_EXTRACTION_FAILED", message, status_code=422, details=details)


class ScoringValidationException(ScoringException):
    def __init__(self, message: str, details: list[str] | None = None) -> None:
        super().__init__("VALIDATION_ERROR", message, status_code=400, details=details)


class ExternalServiceException(ScoringException):
    def __init__(self, message: str, details: list[str] | None = None) -> None:
        super().__init__("EXTERNAL_SERVICE_ERROR", message, status_code=503, details=details)


def error_payload(code: str, message: str, details: list[str] | None = None) -> dict[str, Any]:
    from datetime import datetime, timezone

    return {
        "success": False,
        "code": code,
        "message": message,
        "details": details or [],
        "timestamp": datetime.now(timezone.utc).isoformat(),
    }
