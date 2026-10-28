from __future__ import annotations

from functools import lru_cache
from urllib.parse import quote_plus

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
        case_sensitive=False,
        protected_namespaces=(),
    )

    service_name: str = "scoring-service"
    service_version: str = "1.0.0"
    app_env: str = "local"
    log_level: str = "INFO"
    server_port: int = 8083
    cors_allowed_origins: str = "http://localhost:3000"

    ml_model_path: str = "models/credit_model_v1.0.0.h5"
    ml_scaler_path: str = "models/scaler.pkl"
    ml_encoder_path: str = "models/encoder.pkl"
    ml_metadata_path: str = "models/model_metadata.json"
    model_version: str = "v1.0.0"
    model_trained_at: str = "2026-10-01T00:00:00Z"
    model_metrics: dict[str, float] = Field(
        default_factory=lambda: {
            "auc_roc": 0.83,
            "precision_approve": 0.87,
            "recall_reject": 0.78,
            "gini": 0.66,
            "ks": 0.44,
        }
    )
    canary_traffic_pct: float = 0.0

    db_dsn: str | None = None
    db_host: str = "localhost"
    db_port: int = 5432
    db_name: str = "koperasi"
    db_user: str = "scoring_user"
    db_password: str = "scoreP@ss"
    db_schema: str = "scoring_schema"
    db_connect_on_startup: bool = True
    db_connect_retries: int = 10
    db_connect_retry_delay_seconds: float = 2.0
    db_pool_min_size: int = 1
    db_pool_max_size: int = 10
    db_command_timeout_seconds: float = 5.0

    kafka_enabled: bool = False
    kafka_bootstrap_servers: str = "localhost:9092"
    kafka_group_id: str = "scoring-service-group"
    kafka_topic_scoring_requested: str = "loan.scoring.requested"
    kafka_topic_scoring_completed: str = "loan.scoring.completed"
    kafka_auto_offset_reset: str = "earliest"

    loan_service_url: str = "http://localhost:8082"
    scoring_callback_path: str = "/api/v1/loans/internal/scoring/completed"
    callback_enabled: bool = True
    callback_timeout_seconds: float = 5.0
    callback_max_retries: int = 2
    internal_callback_token: str = ""

    @property
    def database_url(self) -> str:
        if self.db_dsn:
            return self.db_dsn
        return (
            f"postgresql://{quote_plus(self.db_user)}:{quote_plus(self.db_password)}"
            f"@{self.db_host}:{self.db_port}/{self.db_name}"
        )

    @property
    def cors_origins(self) -> list[str]:
        return [origin.strip() for origin in self.cors_allowed_origins.split(",") if origin.strip()]

    @property
    def callback_url(self) -> str:
        return f"{self.loan_service_url.rstrip('/')}/{self.scoring_callback_path.lstrip('/')}"


@lru_cache
def get_settings() -> Settings:
    return Settings()
