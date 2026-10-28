from __future__ import annotations

import json
import logging
from pathlib import Path
from typing import Any

import numpy as np

from app.config import Settings
from app.exceptions import ModelNotLoadedException

logger = logging.getLogger(__name__)


class ModelLoader:
    """Loads the Keras model and preprocessing artifacts, then warms the graph up."""

    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self._model: Any = None
        self._ready = False
        self._scaler_artifact: dict[str, Any] = {}
        self._encoder_artifact: dict[str, Any] = {}
        self._metadata: dict[str, Any] = {}

    @property
    def model_loaded(self) -> bool:
        return self._ready

    @property
    def model_version(self) -> str:
        return str(self._metadata.get("model_version", self.settings.model_version))

    @property
    def scaler_artifact(self) -> dict[str, Any]:
        return self._scaler_artifact

    @property
    def encoder_artifact(self) -> dict[str, Any]:
        return self._encoder_artifact

    @property
    def metadata(self) -> dict[str, Any]:
        return self._metadata

    def load(self) -> None:
        try:
            import tensorflow as tf  # noqa: PLC0415 - optional heavy dependency
        except ImportError as exc:  # pragma: no cover - exercised only without TensorFlow
            raise ModelNotLoadedException("TensorFlow tidak tersedia pada runtime") from exc

        model_path = Path(self.settings.ml_model_path)
        if not model_path.is_file():
            raise ModelNotLoadedException(f"File model tidak ditemukan: {model_path}")

        try:
            self._model = tf.keras.models.load_model(str(model_path), compile=False)
        except Exception as exc:  # pragma: no cover - depends on model artifact
            logger.exception("Gagal memuat model scoring dari %s", model_path)
            raise ModelNotLoadedException("Model scoring gagal dimuat") from exc

        self._scaler_artifact = self._load_pickle_artifact(self.settings.ml_scaler_path)
        self._encoder_artifact = self._load_pickle_artifact(self.settings.ml_encoder_path)
        self._metadata = self._load_metadata(self.settings.ml_metadata_path)
        self._ready = False
        logger.info("Model scoring dimuat dari %s", model_path)

    def warm_up(self, dummy_batch: np.ndarray) -> None:
        if self._model is None:
            raise ModelNotLoadedException("Model scoring belum dimuat")
        sample = np.asarray(dummy_batch, dtype=np.float32)
        if sample.ndim != 2 or sample.shape[0] < 1:
            raise ModelNotLoadedException("Batch warm-up model tidak valid")
        try:
            self._model.predict(sample, verbose=0)
        except Exception as exc:  # pragma: no cover - depends on runtime model
            logger.exception("Warm-up model scoring gagal")
            raise ModelNotLoadedException("Warm-up model scoring gagal") from exc
        self._ready = True
        logger.info("Model scoring warmed up dengan %s dummy prediction", sample.shape[0])

    def predict(self, feature_vector: np.ndarray) -> tuple[float, float | None]:
        if not self._ready or self._model is None:
            raise ModelNotLoadedException()
        sample = np.asarray(feature_vector, dtype=np.float32).reshape(1, -1)
        prediction = self._model.predict(sample, verbose=0)
        return self._extract_outputs(prediction)

    def _extract_outputs(self, prediction: Any) -> tuple[float, float | None]:
        risk_raw: Any
        default_raw: Any | None = None

        if isinstance(prediction, dict):
            risk_raw = prediction.get("risk_score")
            default_raw = prediction.get("default_prob")
        elif isinstance(prediction, (list, tuple)) and len(prediction) >= 2:
            risk_raw, default_raw = prediction[0], prediction[1]
        else:
            risk_raw = prediction

        risk = self._first_float(risk_raw)
        default_probability = self._first_float(default_raw) if default_raw is not None else None
        if risk is None:
            raise ModelNotLoadedException("Output model tidak valid")
        return risk, default_probability

    @staticmethod
    def _first_float(value: Any) -> float | None:
        if value is None:
            return None
        array = np.asarray(value, dtype=np.float32).reshape(-1)
        if array.size == 0:
            return None
        return float(array[0])

    @staticmethod
    def _load_pickle_artifact(path: str) -> dict[str, Any]:
        artifact_path = Path(path)
        if not artifact_path.is_file():
            logger.warning("Artifact preprocessing tidak ditemukan: %s; memakai default", artifact_path)
            return {}
        try:
            import joblib  # noqa: PLC0415

            value = joblib.load(artifact_path)
            return value if isinstance(value, dict) else {}
        except Exception:  # pragma: no cover - corrupt artifact
            logger.exception("Gagal memuat artifact %s; memakai default", artifact_path)
            return {}

    @staticmethod
    def _load_metadata(path: str) -> dict[str, Any]:
        metadata_path = Path(path)
        if not metadata_path.is_file():
            return {}
        try:
            with metadata_path.open(encoding="utf-8") as file:
                value = json.load(file)
            return value if isinstance(value, dict) else {}
        except (OSError, json.JSONDecodeError):  # pragma: no cover
            logger.exception("Gagal memuat metadata model %s", metadata_path)
            return {}
