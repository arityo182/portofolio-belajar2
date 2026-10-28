CREATE SCHEMA IF NOT EXISTS loan_schema;

CREATE TABLE loan_schema.loan_products (
    id                    SERIAL        NOT NULL,
    name                  VARCHAR(100)  NOT NULL,
    min_amount            NUMERIC(15, 2) NOT NULL,
    max_amount            NUMERIC(15, 2) NOT NULL,
    interest_rate_annual  NUMERIC(5, 2)  NOT NULL,
    min_tenor_months      INTEGER        NOT NULL,
    max_tenor_months      INTEGER        NOT NULL,
    is_active             BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_loan_products PRIMARY KEY (id),
    CONSTRAINT uq_loan_products_name UNIQUE (name),
    CONSTRAINT chk_loan_products_amount CHECK (min_amount > 0 AND max_amount >= min_amount),
    CONSTRAINT chk_loan_products_tenor CHECK (min_tenor_months > 0 AND max_tenor_months >= min_tenor_months)
);

CREATE INDEX idx_loan_products_is_active ON loan_schema.loan_products (is_active);

CREATE TABLE loan_schema.loan_applications (
    id                UUID           NOT NULL,
    borrower_id       UUID           NOT NULL,
    loan_product_id   INTEGER        NOT NULL,
    amount            NUMERIC(15, 2) NOT NULL,
    tenor_months      INTEGER        NOT NULL,
    purpose           TEXT           NOT NULL,
    status            VARCHAR(30)    NOT NULL DEFAULT 'PENDING',
    reviewer_id       UUID,
    rejection_reason  TEXT,
    submitted_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    reviewed_at       TIMESTAMPTZ,
    disbursed_at      TIMESTAMPTZ,
    completed_at      TIMESTAMPTZ,
    idempotency_key   VARCHAR(100)   NOT NULL,
    version           BIGINT         NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_loan_applications PRIMARY KEY (id),
    CONSTRAINT fk_loan_applications_product FOREIGN KEY (loan_product_id)
        REFERENCES loan_schema.loan_products (id),
    CONSTRAINT chk_loan_applications_status CHECK (status IN (
        'PENDING', 'SCORING', 'UNDER_REVIEW', 'APPROVED', 'REJECTED',
        'DISBURSED', 'ACTIVE', 'COMPLETED', 'DEFAULT'
    )),
    CONSTRAINT chk_loan_applications_amount CHECK (amount > 0),
    CONSTRAINT chk_loan_applications_tenor CHECK (tenor_months > 0)
);

CREATE UNIQUE INDEX uq_loan_applications_idempotency_key
    ON loan_schema.loan_applications (idempotency_key);
CREATE INDEX idx_loan_applications_borrower_id ON loan_schema.loan_applications (borrower_id);
CREATE INDEX idx_loan_applications_loan_product_id ON loan_schema.loan_applications (loan_product_id);
CREATE INDEX idx_loan_applications_reviewer_id ON loan_schema.loan_applications (reviewer_id);
CREATE INDEX idx_loan_applications_status ON loan_schema.loan_applications (status);
CREATE INDEX idx_loan_applications_created_at ON loan_schema.loan_applications (created_at);

CREATE TABLE loan_schema.credit_scores (
    id                       UUID           NOT NULL,
    loan_application_id      UUID           NOT NULL,
    borrower_id              UUID           NOT NULL,
    score                    NUMERIC(5, 2)  NOT NULL,
    risk_category            VARCHAR(20)    NOT NULL,
    approval_recommendation  VARCHAR(20)    NOT NULL,
    features_json            JSONB          NOT NULL DEFAULT '{}'::jsonb,
    model_version            VARCHAR(20)    NOT NULL,
    processing_time_ms       INTEGER,
    scored_at                TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_credit_scores PRIMARY KEY (id),
    CONSTRAINT fk_credit_scores_application FOREIGN KEY (loan_application_id)
        REFERENCES loan_schema.loan_applications (id) ON DELETE CASCADE,
    CONSTRAINT chk_credit_scores_risk CHECK (risk_category IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT chk_credit_scores_recommendation CHECK (approval_recommendation IN ('APPROVE', 'REVIEW', 'REJECT')),
    CONSTRAINT chk_credit_scores_score CHECK (score >= 0 AND score <= 100)
);

CREATE UNIQUE INDEX uq_credit_scores_loan_application_id
    ON loan_schema.credit_scores (loan_application_id);
CREATE INDEX idx_credit_scores_borrower_id ON loan_schema.credit_scores (borrower_id);
CREATE INDEX idx_credit_scores_risk_category ON loan_schema.credit_scores (risk_category);
CREATE INDEX idx_credit_scores_scored_at ON loan_schema.credit_scores (scored_at);

CREATE TABLE loan_schema.loan_disbursements (
    id                       UUID           NOT NULL,
    loan_application_id      UUID           NOT NULL,
    amount                   NUMERIC(15, 2) NOT NULL,
    payment_gateway          VARCHAR(20)    NOT NULL,
    external_transaction_id  VARCHAR(255),
    status                   VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    idempotency_key          VARCHAR(100)   NOT NULL,
    disbursed_at             TIMESTAMPTZ,
    created_at               TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_loan_disbursements PRIMARY KEY (id),
    CONSTRAINT fk_loan_disbursements_application FOREIGN KEY (loan_application_id)
        REFERENCES loan_schema.loan_applications (id) ON DELETE CASCADE,
    CONSTRAINT chk_loan_disbursements_gateway CHECK (payment_gateway IN ('MIDTRANS', 'XENDIT')),
    CONSTRAINT chk_loan_disbursements_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'))
);

CREATE UNIQUE INDEX uq_loan_disbursements_loan_application_id
    ON loan_schema.loan_disbursements (loan_application_id);
CREATE UNIQUE INDEX uq_loan_disbursements_idempotency_key
    ON loan_schema.loan_disbursements (idempotency_key);
CREATE INDEX idx_loan_disbursements_status ON loan_schema.loan_disbursements (status);
CREATE INDEX idx_loan_disbursements_external_transaction_id
    ON loan_schema.loan_disbursements (external_transaction_id);

CREATE TABLE loan_schema.loan_installments (
    id                  UUID           NOT NULL,
    loan_application_id UUID           NOT NULL,
    installment_number  INTEGER        NOT NULL,
    due_date            DATE           NOT NULL,
    amount              NUMERIC(15, 2) NOT NULL,
    principal           NUMERIC(15, 2) NOT NULL,
    interest            NUMERIC(15, 2) NOT NULL,
    status              VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    paid_at             TIMESTAMPTZ,
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_loan_installments PRIMARY KEY (id),
    CONSTRAINT fk_loan_installments_application FOREIGN KEY (loan_application_id)
        REFERENCES loan_schema.loan_applications (id) ON DELETE CASCADE,
    CONSTRAINT uq_loan_installments_app_number UNIQUE (loan_application_id, installment_number),
    CONSTRAINT chk_loan_installments_status CHECK (status IN ('PENDING', 'PAID', 'OVERDUE')),
    CONSTRAINT chk_loan_installments_amount CHECK (amount >= 0 AND principal >= 0 AND interest >= 0)
);

CREATE INDEX idx_loan_installments_loan_application_id
    ON loan_schema.loan_installments (loan_application_id);
CREATE INDEX idx_loan_installments_status ON loan_schema.loan_installments (status);
CREATE INDEX idx_loan_installments_due_date ON loan_schema.loan_installments (due_date);

INSERT INTO loan_schema.loan_products
    (name, min_amount, max_amount, interest_rate_annual, min_tenor_months, max_tenor_months, is_active)
VALUES
    ('Modal Usaha Mikro', 1000000.00, 50000000.00, 18.00, 3, 24, TRUE),
    ('Modal Usaha Kecil', 5000000.00, 150000000.00, 15.00, 6, 36, TRUE),
    ('Pembiayaan Inventaris', 2000000.00, 75000000.00, 16.50, 3, 18, TRUE);
