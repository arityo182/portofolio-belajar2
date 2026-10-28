CREATE SCHEMA IF NOT EXISTS notification_schema;

-- Notification module now lives inside the core service. The statement is idempotent so
-- it is safe on databases where notification-service already created these objects.
CREATE TABLE IF NOT EXISTS notification_schema.notifications (
    id               UUID         NOT NULL,
    user_id          UUID         NOT NULL,
    title            VARCHAR(200) NOT NULL,
    body             TEXT         NOT NULL,
    type             VARCHAR(20)  NOT NULL DEFAULT 'SYSTEM',
    reference_id     VARCHAR(100),
    is_read          BOOLEAN      NOT NULL DEFAULT FALSE,
    source_event_id  VARCHAR(100),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    read_at          TIMESTAMPTZ,
    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT chk_notifications_type CHECK (type IN ('LOAN_STATUS', 'PAYMENT', 'SYSTEM'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_notifications_source_event_id
    ON notification_schema.notifications (source_event_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user_id
    ON notification_schema.notifications (user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_is_read
    ON notification_schema.notifications (is_read);
CREATE INDEX IF NOT EXISTS idx_notifications_type
    ON notification_schema.notifications (type);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at
    ON notification_schema.notifications (created_at);
CREATE INDEX IF NOT EXISTS idx_notifications_user_unread
    ON notification_schema.notifications (user_id, is_read);
