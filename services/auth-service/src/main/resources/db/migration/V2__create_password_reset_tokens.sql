CREATE TABLE auth_schema.password_reset_tokens (
    id           UUID         NOT NULL,
    user_id      UUID         NOT NULL,
    token_hash   VARCHAR(128) NOT NULL,
    expires_at   TIMESTAMPTZ  NOT NULL,
    used_at      TIMESTAMPTZ,
    requested_ip VARCHAR(64),
    user_agent   TEXT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id)
        REFERENCES auth_schema.users (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX uq_password_reset_tokens_hash
    ON auth_schema.password_reset_tokens (token_hash);
CREATE INDEX idx_password_reset_tokens_user_id
    ON auth_schema.password_reset_tokens (user_id);
CREATE INDEX idx_password_reset_tokens_expires_at
    ON auth_schema.password_reset_tokens (expires_at);
