"""Generate the development-only Keras model and preprocessing artifacts.

This script is intentionally deterministic (fixed seed) and produces a tiny
multi-output model with the same shape as the production scoring model:
    inputs:  (batch, 28)
    outputs: risk_score (sigmoid), default_prob (sigmoid)

Usage:
    python scripts/generate_model_artifacts.py
"""

from __future__ import annotations

import json
import sys
from datetime import datetime, timezone
from pathlib import Path

import joblib
import numpy as np
import tensorflow as tf
from tensorflow.keras import Input, Model, layers

SERVICE_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SERVICE_ROOT))

from app.config import Settings  # noqa: E402
from app.model.preprocessing import (  # noqa: E402
    DEFAULT_MINMAX_PARAMS,
    DEFAULT_STANDARD_PARAMS,
    BUSINESS_TYPE_CATEGORIES,
    LOAN_PURPOSE_CATEGORIES,
    PreprocessingPipeline,
)

MODELS_DIR = SERVICE_ROOT / "models"
MODEL_FILE = MODELS_DIR / "credit_model_v1.0.0.h5"
SCALER_FILE = MODELS_DIR / "scaler.pkl"
ENCODER_FILE = MODELS_DIR / "encoder.pkl"
METADATA_FILE = MODELS_DIR / "model_metadata.json"


def build_model(n_features: int) -> Model:
    tf.keras.utils.set_random_seed(42)
    inputs = Input(shape=(n_features,), name="features")
    x = layers.Dense(128, activation="relu", name="dense_1")(inputs)
    x = layers.BatchNormalization(name="batch_norm_1")(x)
    x = layers.Dropout(0.3, name="dropout_1")(x)
    x = layers.Dense(64, activation="relu", name="dense_2")(x)
    x = layers.BatchNormalization(name="batch_norm_2")(x)
    x = layers.Dropout(0.2, name="dropout_2")(x)
    x = layers.Dense(32, activation="relu", name="dense_3")(x)
    risk_score = layers.Dense(1, activation="sigmoid", name="risk_score")(x)
    default_prob = layers.Dense(1, activation="sigmoid", name="default_prob")(x)

    model = Model(inputs=inputs, outputs=[risk_score, default_prob], name="credit_risk_model")
    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=0.001),
        loss={"risk_score": "mse", "default_prob": "binary_crossentropy"},
        loss_weights={"risk_score": 0.4, "default_prob": 0.6},
    )
    return model


def main() -> None:
    MODELS_DIR.mkdir(parents=True, exist_ok=True)
    pipeline = PreprocessingPipeline()
    model = build_model(pipeline.n_features)

    warmup = pipeline.dummy_batch(5)
    predictions = model.predict(warmup, verbose=0)
    risk_preview = float(np.asarray(predictions[0]).reshape(-1)[0]) * 100.0
    default_preview = float(np.asarray(predictions[1]).reshape(-1)[0])

    model.save(MODEL_FILE)

    joblib.dump(
        {
            "standard": DEFAULT_STANDARD_PARAMS,
            "minmax": DEFAULT_MINMAX_PARAMS,
        },
        SCALER_FILE,
    )
    joblib.dump(
        {
            "business_type_categories": list(BUSINESS_TYPE_CATEGORIES),
            "loan_purpose_categories": list(LOAN_PURPOSE_CATEGORIES),
        },
        ENCODER_FILE,
    )

    settings = Settings(_env_file=None)
    metadata = {
        "model_version": settings.model_version,
        "trained_at": datetime.now(timezone.utc).isoformat(),
        "metrics": settings.model_metrics,
        "n_features": pipeline.n_features,
        "feature_order": pipeline.feature_order,
        "dummy": True,
        "warmup_sample_size": 5,
        "warmup_risk_score_preview": round(risk_preview, 2),
        "warmup_default_probability_preview": round(default_preview, 4),
    }
    METADATA_FILE.write_text(json.dumps(metadata, indent=2), encoding="utf-8")

    print(f"Model saved: {MODEL_FILE}")
    print(f"Scaler saved: {SCALER_FILE}")
    print(f"Encoder saved: {ENCODER_FILE}")
    print(f"Metadata saved: {METADATA_FILE}")
    print(f"Features: {pipeline.n_features}")
    print(f"Warm-up risk score preview: {risk_preview:.2f}")


if __name__ == "__main__":
    main()
