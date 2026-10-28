from __future__ import annotations

import asyncio
import logging
import math

import asyncpg
from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Any, Mapping
from uuid import UUID

from app.database import Database
from app.exceptions import FeatureExtractionException
from app.model.preprocessing import BASE_FEATURE_ORDER, DEFAULT_FEATURES

logger = logging.getLogger(__name__)

BUSINESS_TYPE_CODES: dict[str, int] = {
    "KULINER": 0,
    "RETAIL": 1,
    "JASA": 2,
    "PERTANIAN": 3,
}

PROFILE_REQUIRED_FIELDS = (
    "full_name",
    "date_of_birth",
    "gender",
    "address",
    "city",
    "province",
    "bank_name",
    "bank_account_encrypted",
    "income_monthly",
    "business_type",
    "business_name",
    "npwp_encrypted",
)


@dataclass
class FeatureSet:
    values: dict[str, float]
    business_type: str = "UNKNOWN"
    loan_purpose: str = "LAINNYA"

    def merged(self) -> dict[str, float]:
        return {key: float(self.values.get(key, DEFAULT_FEATURES[key])) for key in BASE_FEATURE_ORDER}


@dataclass
class _OptionalSignals:
    total_payments_6m: float = 0.0
    monthly_totals: list[float] = field(default_factory=list)
    due_installments: int = 0
    ontime_installments: int = 0
    previous_loans: int = 0
    previous_defaults: int = 0
    active_debt: float = 0.0


class FeatureExtractor:
    """Computes the 20 raw scoring features from PostgreSQL."""

    APPLICATION_QUERY = """
        SELECT la.id,
               la.borrower_id,
               la.amount,
               la.tenor_months,
               la.purpose,
               la.status,
               lp.max_amount,
               lp.max_tenor_months
        FROM loan_schema.loan_applications la
        JOIN loan_schema.loan_products lp ON lp.id = la.loan_product_id
        WHERE la.id = $1 AND la.borrower_id = $2
    """

    PROFILE_QUERY = """
        SELECT full_name,
               date_of_birth,
               gender,
               address,
               city,
               province,
               bank_name,
               bank_account_encrypted,
               income_monthly,
               business_type,
               business_name,
               npwp_encrypted,
               is_verified,
               ktp_photo_url,
               selfie_photo_url,
               created_at AS profile_created_at
        FROM auth_schema.borrower_profiles
        WHERE user_id = $1
    """

    PAYMENT_TOTALS_QUERY = """
        SELECT COALESCE(SUM(amount), 0) AS total,
               COUNT(*) AS payment_count
        FROM payment_schema.payments
        WHERE borrower_id = $1
          AND status = 'SUCCESS'
          AND created_at >= NOW() - INTERVAL '6 months'
    """

    MONTHLY_PAYMENTS_QUERY = """
        SELECT date_trunc('month', created_at) AS month,
               COALESCE(SUM(amount), 0) AS amount
        FROM payment_schema.payments
        WHERE borrower_id = $1
          AND status = 'SUCCESS'
          AND created_at >= NOW() - INTERVAL '6 months'
        GROUP BY date_trunc('month', created_at)
        ORDER BY month
    """

    INSTALLMENTS_QUERY = """
        SELECT COUNT(*) FILTER (WHERE li.due_date <= CURRENT_DATE) AS due_count,
               COUNT(*) FILTER (
                   WHERE li.status = 'PAID'
                     AND li.paid_at IS NOT NULL
                     AND li.paid_at::date <= li.due_date
               ) AS ontime_count
        FROM loan_schema.loan_installments li
        JOIN loan_schema.loan_applications la ON la.id = li.loan_application_id
        WHERE la.borrower_id = $1
    """

    PREVIOUS_LOANS_QUERY = """
        SELECT COUNT(*) FILTER (WHERE status IN ('COMPLETED', 'DEFAULT')) AS previous_loans,
               COUNT(*) FILTER (WHERE status = 'DEFAULT') AS previous_defaults
        FROM loan_schema.loan_applications
        WHERE borrower_id = $1 AND id <> $2
    """

    ACTIVE_DEBT_QUERY = """
        SELECT COALESCE(SUM(li.amount), 0) AS active_debt
        FROM loan_schema.loan_installments li
        JOIN loan_schema.loan_applications la ON la.id = li.loan_application_id
        WHERE la.borrower_id = $1
          AND la.id <> $2
          AND li.status IN ('PENDING', 'OVERDUE')
    """

    def __init__(self, database: Database) -> None:
        self.database = database

    @classmethod
    def from_overrides(cls, features: Mapping[str, float]) -> FeatureSet:
        values = dict(DEFAULT_FEATURES)
        for key in BASE_FEATURE_ORDER:
            if key in features:
                value = float(features[key])
                values[key] = value if math.isfinite(value) else DEFAULT_FEATURES[key]
        return FeatureSet(values=values, business_type="UNKNOWN", loan_purpose="LAINNYA")

    async def extract(self, loan_application_id: UUID, borrower_id: UUID) -> FeatureSet:
        try:
            application = await self.database.fetchrow(
                self.APPLICATION_QUERY, loan_application_id, borrower_id
            )
            if application is None:
                raise FeatureExtractionException(
                    "Pengajuan pinjaman tidak ditemukan untuk borrower tersebut",
                    [f"loanApplicationId={loan_application_id}"],
                )

            profile, payment_totals, monthly_payments, installments, previous, active_debt = await asyncio.gather(
                self._optional_fetchrow(self.PROFILE_QUERY, borrower_id),
                self._optional_fetchrow(self.PAYMENT_TOTALS_QUERY, borrower_id),
                self._optional_fetch(self.MONTHLY_PAYMENTS_QUERY, borrower_id),
                self._optional_fetchrow(self.INSTALLMENTS_QUERY, borrower_id),
                self._optional_fetchrow(self.PREVIOUS_LOANS_QUERY, borrower_id, loan_application_id),
                self._optional_fetchrow(self.ACTIVE_DEBT_QUERY, borrower_id, loan_application_id),
            )
        except FeatureExtractionException:
            raise
        except Exception as exc:
            logger.exception("Gagal mengekstrak fitur scoring loanApplicationId=%s", loan_application_id)
            raise FeatureExtractionException(
                "Gagal mengambil data fitur dari database",
            ) from exc

        merged = dict(application)
        for field_name in PROFILE_REQUIRED_FIELDS + (
            "address",
            "city",
            "province",
            "is_verified",
            "ktp_photo_url",
            "selfie_photo_url",
            "created_at",
            "profile_created_at",
        ):
            merged.setdefault(field_name, None)
        if profile is not None:
            merged.update(dict(profile))

        signals = _OptionalSignals(
            total_payments_6m=_as_float(payment_totals["total"] if payment_totals else 0),
            monthly_totals=[_as_float(row["amount"]) for row in (monthly_payments or [])],
            due_installments=int(installments["due_count"] or 0) if installments else 0,
            ontime_installments=int(installments["ontime_count"] or 0) if installments else 0,
            previous_loans=int(previous["previous_loans"] or 0) if previous else 0,
            previous_defaults=int(previous["previous_defaults"] or 0) if previous else 0,
            active_debt=_as_float(active_debt["active_debt"] if active_debt else 0),
        )
        return self._build_features(merged, signals)

    async def _optional_fetchrow(self, query: str, *args: Any) -> Any:
        try:
            return await self.database.fetchrow(query, *args)
        except asyncpg.PostgresError as exc:
            if _is_missing_relation(exc):
                logger.warning("Tabel opsional belum tersedia: %s", exc)
                return None
            raise

    async def _optional_fetch(self, query: str, *args: Any) -> list[Any]:
        try:
            return await self.database.fetch(query, *args)
        except asyncpg.PostgresError as exc:
            if _is_missing_relation(exc):
                logger.warning("Tabel opsional belum tersedia: %s", exc)
                return []
            raise

    def _build_features(self, application: Any, signals: _OptionalSignals) -> FeatureSet:
        income = _as_float(application["income_monthly"])
        amount = _as_float(application["amount"])
        tenor_months = max(1, int(application["tenor_months"] or 1))
        max_amount = max(1.0, _as_float(application["max_amount"]))
        max_tenor = max(1, int(application["max_tenor_months"] or 1))
        purpose = str(application["purpose"] or "").lower()
        business_type = str(application["business_type"] or "").upper()
        business_type_code = BUSINESS_TYPE_CODES.get(business_type, len(BUSINESS_TYPE_CODES))
        loan_purpose_code = _purpose_code(purpose)

        average_monthly_payment = signals.total_payments_6m / 6.0
        income_stability = _income_stability_index(signals.monthly_totals)
        loan_to_income = amount / (income * 12.0) if income > 0 else 0.0
        debt_to_income = signals.active_debt / income if income > 0 else 0.0
        monthly_revenue_to_income = average_monthly_payment / income if income > 0 else 0.0
        ontime_rate = (
            signals.ontime_installments / signals.due_installments * 100.0
            if signals.due_installments > 0
            else 0.0
        )
        profile_completeness = _profile_completeness(application)
        verification_completeness = _verification_completeness(application)
        business_age = _business_age_months(application["profile_created_at"])

        values = {
            "payment_consistency_score": _clamp(ontime_rate, 0.0, 100.0),
            "avg_monthly_transaction_amount": max(0.0, average_monthly_payment),
            "income_stability_index": income_stability,
            "debt_to_income_ratio": max(0.0, debt_to_income),
            "loan_to_income_ratio": max(0.0, loan_to_income),
            "business_age_months": business_age,
            "business_type_code": float(business_type_code),
            "has_npwp": 1.0 if application["npwp_encrypted"] else 0.0,
            "has_formal_address": (
                1.0
                if application["address"] and application["city"] and application["province"]
                else 0.0
            ),
            "monthly_revenue_to_income_ratio": max(0.0, monthly_revenue_to_income),
            "requested_amount_normalized": _clamp(amount / max_amount, 0.0, 1.0),
            "tenor_normalized": _clamp(tenor_months / max_tenor, 0.0, 1.0),
            "loan_purpose_code": float(loan_purpose_code),
            "amount_to_annual_income_ratio": max(0.0, loan_to_income),
            "previous_loan_count": float(signals.previous_loans),
            "previous_default_count": float(signals.previous_defaults),
            "previous_ontime_payment_rate": _clamp(ontime_rate, 0.0, 100.0),
            "verification_completeness_score": verification_completeness,
            "profile_completeness_score": profile_completeness,
            "is_new_borrower": 1.0 if signals.previous_loans == 0 else 0.0,
        }
        return FeatureSet(values=values, business_type=business_type or "UNKNOWN", loan_purpose=purpose or "LAINNYA")


def _is_missing_relation(exc: asyncpg.PostgresError) -> bool:
    return getattr(exc, "sqlstate", None) in {"42P01", "3F000"}


def _as_float(value: Any) -> float:
    if value is None:
        return 0.0
    try:
        result = float(value)
    except (TypeError, ValueError):
        return 0.0
    return result if math.isfinite(result) else 0.0


def _clamp(value: float, minimum: float, maximum: float) -> float:
    return max(minimum, min(maximum, value))


def _income_stability_index(monthly_totals: list[float]) -> float:
    if len(monthly_totals) < 2:
        return 0.5
    values = [max(0.0, value) for value in monthly_totals]
    mean = sum(values) / len(values)
    if mean <= 0:
        return 0.5
    variance = sum((value - mean) ** 2 for value in values) / len(values)
    coefficient_of_variation = math.sqrt(variance) / mean
    return _clamp(coefficient_of_variation, 0.0, 1.0)


def _purpose_code(purpose: str) -> int:
    if "inventaris" in purpose:
        return 1
    if any(keyword in purpose for keyword in ("peralatan", "mesin", "alat")):
        return 2
    if any(keyword in purpose for keyword in ("renovasi", "bangun", "gedung")):
        return 3
    if any(keyword in purpose for keyword in ("modal", "usaha", "kerja")):
        return 0
    return 4


def _profile_completeness(application: Any) -> float:
    filled = sum(1 for field_name in PROFILE_REQUIRED_FIELDS if application[field_name])
    return round(filled / len(PROFILE_REQUIRED_FIELDS) * 100.0, 2)


def _verification_completeness(application: Any) -> float:
    components = (
        bool(application["is_verified"]),
        bool(application["ktp_photo_url"]),
        bool(application["selfie_photo_url"]),
    )
    return round(sum(1 for component in components if component) / len(components) * 100.0, 2)


def _business_age_months(created_at: datetime | None) -> float:
    if created_at is None:
        return 0.0
    reference = created_at if created_at.tzinfo else created_at.replace(tzinfo=timezone.utc)
    delta = datetime.now(timezone.utc) - reference
    return max(0.0, round(delta.days / 30.4375, 2))
