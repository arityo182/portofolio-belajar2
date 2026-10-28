CREATE TABLE auth_schema.borrower_profiles (
    id                       UUID           NOT NULL,
    user_id                  UUID           NOT NULL,
    nik_encrypted            TEXT,
    npwp_encrypted           TEXT,
    full_name                VARCHAR(255),
    date_of_birth            DATE,
    gender                   VARCHAR(1),
    address                  VARCHAR(500),
    city                     VARCHAR(100),
    province                 VARCHAR(100),
    bank_name                VARCHAR(100),
    bank_account_encrypted   TEXT,
    income_monthly           NUMERIC(15, 2),
    business_type            VARCHAR(100),
    business_name            VARCHAR(255),
    is_verified              BOOLEAN        NOT NULL DEFAULT FALSE,
    ktp_photo_url            TEXT,
    selfie_photo_url         TEXT,
    ktp_photo_data           BYTEA,
    selfie_photo_data        BYTEA,
    ktp_photo_content_type   VARCHAR(50),
    selfie_photo_content_type VARCHAR(50),
    created_at               TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_borrower_profiles PRIMARY KEY (id),
    CONSTRAINT uq_borrower_profiles_user_id UNIQUE (user_id),
    CONSTRAINT fk_borrower_profiles_user FOREIGN KEY (user_id)
        REFERENCES auth_schema.users (id) ON DELETE CASCADE,
    CONSTRAINT chk_borrower_profiles_gender CHECK (gender IS NULL OR gender IN ('M', 'F')),
    CONSTRAINT chk_borrower_profiles_income CHECK (income_monthly IS NULL OR income_monthly >= 0)
);

CREATE INDEX idx_borrower_profiles_created_at ON auth_schema.borrower_profiles (created_at);
CREATE INDEX idx_borrower_profiles_is_verified ON auth_schema.borrower_profiles (is_verified);
