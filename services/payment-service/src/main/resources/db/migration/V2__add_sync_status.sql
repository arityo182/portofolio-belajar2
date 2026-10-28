ALTER TABLE payment_schema.payments
    ADD COLUMN sync_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN sync_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN last_sync_at TIMESTAMPTZ,
    ADD COLUMN sync_error TEXT;

ALTER TABLE payment_schema.payments
    ADD CONSTRAINT chk_payments_sync_status
        CHECK (sync_status IN ('PENDING', 'SYNCED', 'FAILED'));

CREATE INDEX idx_payments_sync_status_created_at
    ON payment_schema.payments (sync_status, created_at);

ALTER TABLE payment_schema.loan_disbursements
    ADD COLUMN sync_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN sync_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN last_sync_at TIMESTAMPTZ,
    ADD COLUMN sync_error TEXT;

ALTER TABLE payment_schema.loan_disbursements
    ADD CONSTRAINT chk_payment_disbursement_sync_status
        CHECK (sync_status IN ('PENDING', 'SYNCED', 'FAILED'));

CREATE INDEX idx_payment_disbursements_sync_status_disbursed_at
    ON payment_schema.loan_disbursements (sync_status, disbursed_at);
