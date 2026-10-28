from __future__ import annotations

import math
from typing import Any, Mapping

import numpy as np

BUSINESS_TYPE_CATEGORIES: tuple[str, ...] = ("KULINER", "RETAIL", "JASA", "PERTANIAN", "LAINNYA")
LOAN_PURPOSE_CATEGORIES: tuple[str, ...] = ("MODAL_KERJA", "INVENTARIS", "PERALATAN", "RENOVASI", "LAINNYA")

BASE_FEATURE_ORDER: tuple[str, ...] = (
    "payment_consistency_score",
    "avg_monthly_transaction_amount",
    "income_stability_index",
    "debt_to_income_ratio",
    "loan_to_income_ratio",
    "business_age_months",
    "business_type_code",
    "has_npwp",
    "has_formal_address",
    "monthly_revenue_to_income_ratio",
    "requested_amount_normalized",
    "tenor_normalized",
    "loan_purpose_code",
    "amount_to_annual_income_ratio",
    "previous_loan_count",
    "previous_default_count",
    "previous_ontime_payment_rate",
    "verification_completeness_score",
    "profile_completeness_score",
    "is_new_borrower",
)

CATEGORICAL_FEATURES: tuple[str, ...] = ("business_type_code", "loan_purpose_code")

DEFAULT_FEATURES: dict[str, float] = {
    "payment_consistency_score": 50.0,
    "avg_monthly_transaction_amount": 0.0,
    "income_stability_index": 0.5,
    "debt_to_income_ratio": 0.0,
    "loan_to_income_ratio": 0.0,
    "business_age_months": 0.0,
    "business_type_code": float(len(BUSINESS_TYPE_CATEGORIES) - 1),
    "has_npwp": 0.0,
    "has_formal_address": 0.0,
    "monthly_revenue_to_income_ratio": 0.0,
    "requested_amount_normalized": 0.5,
    "tenor_normalized": 0.5,
    "loan_purpose_code": float(len(LOAN_PURPOSE_CATEGORIES) - 1),
    "amount_to_annual_income_ratio": 0.0,
    "previous_loan_count": 0.0,
    "previous_default_count": 0.0,
    "previous_ontime_payment_rate": 0.0,
    "verification_completeness_score": 0.0,
    "profile_completeness_score": 0.0,
    "is_new_borrower": 1.0,
}

STANDARD_FEATURES: tuple[str, ...] = (
    "payment_consistency_score",
    "avg_monthly_transaction_amount",
    "income_stability_index",
    "debt_to_income_ratio",
    "loan_to_income_ratio",
)

DEFAULT_STANDARD_PARAMS: dict[str, dict[str, float]] = {
    "payment_consistency_score": {"mean": 60.0, "scale": 25.0},
    "avg_monthly_transaction_amount": {"mean": 5_000_000.0, "scale": 5_000_000.0},
    "income_stability_index": {"mean": 0.5, "scale": 0.3},
    "debt_to_income_ratio": {"mean": 0.3, "scale": 0.3},
    "loan_to_income_ratio": {"mean": 0.5, "scale": 0.5},
}

DEFAULT_MINMAX_PARAMS: dict[str, dict[str, float]] = {
    "business_age_months": {"min": 0.0, "max": 240.0},
    "monthly_revenue_to_income_ratio": {"min": 0.0, "max": 5.0},
    "requested_amount_normalized": {"min": 0.0, "max": 1.0},
    "tenor_normalized": {"min": 0.0, "max": 1.0},
    "amount_to_annual_income_ratio": {"min": 0.0, "max": 5.0},
    "previous_loan_count": {"min": 0.0, "max": 10.0},
    "previous_default_count": {"min": 0.0, "max": 5.0},
    "previous_ontime_payment_rate": {"min": 0.0, "max": 100.0},
    "verification_completeness_score": {"min": 0.0, "max": 100.0},
    "profile_completeness_score": {"min": 0.0, "max": 100.0},
}


class PreprocessingPipeline:
    """Train/serve-consistent preprocessing for the scoring model.

    The artifacts are intentionally plain dictionaries so the serving image does not
    need to deserialize arbitrary pickle data from an untrusted source.
    """

    def __init__(
        self,
        scaler_artifact: Mapping[str, Any] | None = None,
        encoder_artifact: Mapping[str, Any] | None = None,
    ) -> None:
        scaler_artifact = scaler_artifact or {}
        encoder_artifact = encoder_artifact or {}

        self.business_categories = tuple(
            encoder_artifact.get("business_type_categories") or BUSINESS_TYPE_CATEGORIES
        )
        self.loan_purpose_categories = tuple(
            encoder_artifact.get("loan_purpose_categories") or LOAN_PURPOSE_CATEGORIES
        )
        self.standard_params = {
            **DEFAULT_STANDARD_PARAMS,
            **dict(scaler_artifact.get("standard") or {}),
        }
        self.minmax_params = {
            **DEFAULT_MINMAX_PARAMS,
            **dict(scaler_artifact.get("minmax") or {}),
        }
        self._scalar_features = tuple(
            feature for feature in BASE_FEATURE_ORDER if feature not in CATEGORICAL_FEATURES
        )
        self._feature_order = (
            list(self._scalar_features)
            + [f"business_type_{category}" for category in self.business_categories]
            + [f"loan_purpose_{category}" for category in self.loan_purpose_categories]
        )

    @property
    def feature_order(self) -> list[str]:
        return list(self._feature_order)

    @property
    def n_features(self) -> int:
        return len(self._feature_order)

    def default_features(self) -> dict[str, float]:
        return dict(DEFAULT_FEATURES)

    def normalise_features(self, features: Mapping[str, float] | None) -> dict[str, float]:
        normalised = dict(DEFAULT_FEATURES)
        if features:
            for key in BASE_FEATURE_ORDER:
                if key in features:
                    value = float(features[key])
                    normalised[key] = value if math.isfinite(value) else DEFAULT_FEATURES[key]
        return normalised

    def transform(self, features: Mapping[str, float] | None) -> np.ndarray:
        values = self.normalise_features(features)
        vector: list[float] = []

        for feature in self._scalar_features:
            value = values[feature]
            if feature in self.standard_params:
                params = self.standard_params[feature]
                scale = params.get("scale") or 1.0
                scaled = (value - float(params.get("mean", 0.0))) / float(scale)
                vector.append(max(-5.0, min(5.0, scaled)))
            elif feature in self.minmax_params:
                params = self.minmax_params[feature]
                minimum = float(params.get("min", 0.0))
                maximum = float(params.get("max", 1.0))
                span = maximum - minimum
                scaled = 0.0 if span == 0 else (value - minimum) / span
                vector.append(max(0.0, min(1.0, scaled)))
            else:
                vector.append(value)

        business_code = self._category_code(values["business_type_code"], len(self.business_categories))
        purpose_code = self._category_code(values["loan_purpose_code"], len(self.loan_purpose_categories))
        vector.extend(1.0 if index == business_code else 0.0 for index in range(len(self.business_categories)))
        vector.extend(1.0 if index == purpose_code else 0.0 for index in range(len(self.loan_purpose_categories)))

        return np.asarray(vector, dtype=np.float32)

    def transform_batch(self, rows: list[Mapping[str, float]]) -> np.ndarray:
        if not rows:
            return np.zeros((0, self.n_features), dtype=np.float32)
        return np.vstack([self.transform(row) for row in rows])

    def dummy_batch(self, size: int = 5) -> np.ndarray:
        return np.tile(self.transform(DEFAULT_FEATURES), (size, 1)).astype(np.float32)

    def features_used(self, features: Mapping[str, float] | None) -> dict[str, float]:
        return {key: round(value, 6) for key, value in self.normalise_features(features).items()}

    @staticmethod
    def _category_code(value: float, categories: int) -> int:
        try:
            code = int(round(float(value)))
        except (TypeError, ValueError):
            code = categories - 1
        return max(0, min(categories - 1, code))
