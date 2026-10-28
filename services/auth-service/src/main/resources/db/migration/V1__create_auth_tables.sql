CREATE SCHEMA IF NOT EXISTS auth_schema;

CREATE TABLE auth_schema.users (
    id            UUID         NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone         VARCHAR(20),
    status        VARCHAR(30)  NOT NULL DEFAULT 'PENDING_VERIFICATION',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'PENDING_VERIFICATION'))
);

CREATE UNIQUE INDEX uq_users_email ON auth_schema.users (email);
CREATE UNIQUE INDEX uq_users_email_lower ON auth_schema.users (LOWER(email));
CREATE INDEX idx_users_status ON auth_schema.users (status);
CREATE INDEX idx_users_created_at ON auth_schema.users (created_at);

CREATE TABLE auth_schema.roles (
    id          SERIAL      NOT NULL,
    name        VARCHAR(50) NOT NULL,
    description TEXT,
    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uq_roles_name UNIQUE (name)
);

CREATE TABLE auth_schema.user_roles (
    user_id     UUID        NOT NULL,
    role_id     INTEGER     NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES auth_schema.users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES auth_schema.roles (id) ON DELETE CASCADE
);

CREATE INDEX idx_user_roles_role_id ON auth_schema.user_roles (role_id);

CREATE TABLE auth_schema.refresh_tokens (
    id          UUID         NOT NULL,
    user_id     UUID         NOT NULL,
    token_hash  VARCHAR(512) NOT NULL,
    device_info TEXT,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES auth_schema.users (id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_user_id ON auth_schema.refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON auth_schema.refresh_tokens (token_hash);
CREATE INDEX idx_refresh_tokens_expires_at ON auth_schema.refresh_tokens (expires_at);
CREATE INDEX idx_refresh_tokens_active ON auth_schema.refresh_tokens (user_id, expires_at) WHERE revoked_at IS NULL;

INSERT INTO auth_schema.roles (name, description) VALUES
    ('BORROWER', 'Peminjam yang mengajukan pinjaman'),
    ('INVESTOR', 'Pemberi dana yang menginvestasikan modal'),
    ('REVIEWER', 'Peninjau pengajuan pinjaman dan skor kredit'),
    ('ADMIN', 'Administrator platform dengan akses penuh')
ON CONFLICT (name) DO NOTHING;
