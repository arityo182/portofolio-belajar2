ALTER TABLE loan_schema.loan_applications
    ADD COLUMN IF NOT EXISTS review_notes TEXT;

CREATE INDEX IF NOT EXISTS idx_loan_applications_reviewer_id
    ON loan_schema.loan_applications (reviewer_id);
CREATE INDEX IF NOT EXISTS idx_loan_applications_reviewed_at
    ON loan_schema.loan_applications (reviewed_at);
