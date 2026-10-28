CREATE SCHEMA IF NOT EXISTS payment_schema;

CREATE TABLE payment_schema.payments (
    id                       UUID           NOT NULL,
    loan_installment_id      UUID           NOT NULL,
    borrower_id              UUID           NOT NULL,
    amount                   NUMERIC(15, 2) NOT NULL,
    payment_gateway          VARCHAR(20)    NOT NULL,
    external_transaction_id  VARCHAR(255),
    payment_url              TEXT,
    webhook_payload          JSONB,
    status                   VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    idempotency_key          VARCHAR(100)   NOT NULL,
    paid_at                  TIMESTAMPTZ,
    created_at               TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT chk_payments_gateway CHECK (payment_gateway IN ('MIDTRANS', 'XENDIT')),
    CONSTRAINT chk_payments_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED')),
    CONSTRAINT chk_payments_amount CHECK (amount > 0)
);

CREATE UNIQUE INDEX uq_payments_idempotency_key ON payment_schema.payments (idempotency_key);
CREATE INDEX idx_payments_loan_installment_id ON payment_schema.payments (loan_installment_id);
CREATE INDEX idx_payments_borrower_id ON payment_schema.payments (borrower_id);
CREATE INDEX idx_payments_status ON payment_schema.payments (status);
CREATE INDEX idx_payments_external_transaction_id ON payment_schema.payments (external_transaction_id);
CREATE INDEX idx_payments_created_at ON payment_schema.payments (created_at);

CREATE TABLE payment_schema.loan_disbursements (
    id                       UUID           NOT NULL,
    loan_application_id      UUID           NOT NULL,
    amount                   NUMERIC(15, 2) NOT NULL,
    payment_gateway          VARCHAR(20)    NOT NULL,
    external_transaction_id  VARCHAR(255),
    status                   VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    idempotency_key          VARCHAR(100)   NOT NULL,
    disbursed_at             TIMESTAMPTZ,
    created_at               TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_payment_loan_disbursements PRIMARY KEY (id),
    CONSTRAINT uq_payment_disbursement_application UNIQUE (loan_application_id),
    CONSTRAINT chk_payment_disbursement_gateway CHECK (payment_gateway IN ('MIDTRANS', 'XENDIT')),
    CONSTRAINT chk_payment_disbursement_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    CONSTRAINT chk_payment_disbursement_amount CHECK (amount > 0)
);

CREATE UNIQUE INDEX uq_payment_disbursements_idempotency_key
    ON payment_schema.loan_disbursements (idempotency_key);
CREATE INDEX idx_payment_disbursements_status ON payment_schema.loan_disbursements (status);
CREATE INDEX idx_payment_disbursements_external_transaction_id
    ON payment_schema.loan_disbursements (external_transaction_id);

CREATE TABLE payment_schema.webhook_events (
    id                UUID         NOT NULL,
    event_key         VARCHAR(255) NOT NULL,
    gateway           VARCHAR(20)  NOT NULL,
    external_id       VARCHAR(255),
    event_type        VARCHAR(100),
    payload           JSONB        NOT NULL,
    signature_valid   BOOLEAN      NOT NULL DEFAULT FALSE,
    processed         BOOLEAN      NOT NULL DEFAULT FALSE,
    processing_error  TEXT,
    received_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    processed_at      TIMESTAMPTZ,
    CONSTRAINT pk_webhook_events PRIMARY KEY (id),
    CONSTRAINT uq_webhook_events_event_key UNIQUE (event_key),
    CONSTRAINT chk_webhook_events_gateway CHECK (gateway IN ('MIDTRANS', 'XENDIT'))
);

CREATE INDEX idx_webhook_events_gateway ON payment_schema.webhook_events (gateway);
CREATE INDEX idx_webhook_events_external_id ON payment_schema.webhook_events (external_id);
CREATE INDEX idx_webhook_events_received_at ON payment_schema.webhook_events (received_at);

CREATE TABLE payment_schema.idempotency_keys (
    id              UUID         NOT NULL,
    key             VARCHAR(200) NOT NULL,
    service         VARCHAR(50)  NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PROCESSING',
    response_json   JSONB,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_idempotency_keys PRIMARY KEY (id),
    CONSTRAINT uq_idempotency_keys_key UNIQUE (key),
    CONSTRAINT chk_idempotency_keys_status CHECK (status IN ('PROCESSING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_idempotency_keys_status ON payment_schema.idempotency_keys (status);
CREATE INDEX idx_idempotency_keys_expires_at ON payment_schema.idempotency_keys (expires_at);
