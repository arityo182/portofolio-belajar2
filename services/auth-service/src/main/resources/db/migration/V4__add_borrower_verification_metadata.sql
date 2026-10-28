ALTER TABLE auth_schema.borrower_profiles
    ADD COLUMN verified_at TIMESTAMPTZ,
    ADD COLUMN verified_by UUID;

ALTER TABLE auth_schema.borrower_profiles
    ADD CONSTRAINT fk_borrower_profiles_verified_by
        FOREIGN KEY (verified_by) REFERENCES auth_schema.users (id) ON DELETE SET NULL;

CREATE INDEX idx_borrower_profiles_verified_at
    ON auth_schema.borrower_profiles (verified_at);
