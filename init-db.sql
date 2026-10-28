-- ============================================================================
-- Koperasi Digital — Database Bootstrap
-- Creates one schema and one application role per bounded context.
-- Executed automatically by the PostgreSQL container on first initialization.
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ----------------------------------------------------------------------------
-- Schemas
-- ----------------------------------------------------------------------------
CREATE SCHEMA IF NOT EXISTS auth_schema;
CREATE SCHEMA IF NOT EXISTS loan_schema;
CREATE SCHEMA IF NOT EXISTS scoring_schema;
CREATE SCHEMA IF NOT EXISTS payment_schema;
CREATE SCHEMA IF NOT EXISTS notification_schema;
CREATE SCHEMA IF NOT EXISTS reporting_schema;
CREATE SCHEMA IF NOT EXISTS investor_schema;

-- ----------------------------------------------------------------------------
-- Application roles (local development defaults; rotate in production)
-- ----------------------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'auth_user') THEN
        CREATE ROLE auth_user LOGIN PASSWORD 'authP@ss';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'loan_user') THEN
        CREATE ROLE loan_user LOGIN PASSWORD 'loanP@ss';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'scoring_user') THEN
        CREATE ROLE scoring_user LOGIN PASSWORD 'scoreP@ss';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'payment_user') THEN
        CREATE ROLE payment_user LOGIN PASSWORD 'payP@ss';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'notif_user') THEN
        CREATE ROLE notif_user LOGIN PASSWORD 'notifP@ss';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'report_user') THEN
        CREATE ROLE report_user LOGIN PASSWORD 'reportP@ss';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'investor_user') THEN
        CREATE ROLE investor_user LOGIN PASSWORD 'investorP@ss';
    END IF;
END
$$;

-- ----------------------------------------------------------------------------
-- Ownership & privileges
-- ----------------------------------------------------------------------------
ALTER SCHEMA auth_schema OWNER TO auth_user;
ALTER SCHEMA loan_schema OWNER TO loan_user;
ALTER SCHEMA scoring_schema OWNER TO scoring_user;
ALTER SCHEMA payment_schema OWNER TO payment_user;
ALTER SCHEMA notification_schema OWNER TO notif_user;

-- Core service owns the notification module now, so loan_user needs access to the
-- notification schema. reporting stays read-only via report_user.
GRANT USAGE, CREATE ON SCHEMA notification_schema TO loan_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA notification_schema TO loan_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA notification_schema TO loan_user;
ALTER DEFAULT PRIVILEGES FOR ROLE notif_user IN SCHEMA notification_schema
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO loan_user;
ALTER DEFAULT PRIVILEGES FOR ROLE notif_user IN SCHEMA notification_schema
    GRANT USAGE, SELECT ON SEQUENCES TO loan_user;
ALTER SCHEMA reporting_schema OWNER TO report_user;
ALTER SCHEMA investor_schema OWNER TO investor_user;

DO $$
DECLARE
    db_name TEXT := current_database();
    role_name TEXT;
BEGIN
    FOREACH role_name IN ARRAY ARRAY[
        'auth_user', 'loan_user', 'scoring_user', 'payment_user',
        'notif_user', 'report_user', 'investor_user'
    ] LOOP
        EXECUTE format('GRANT CONNECT, TEMPORARY, CREATE ON DATABASE %I TO %I', db_name, role_name);
    END LOOP;
END
$$;

ALTER ROLE auth_user SET search_path TO auth_schema, public;
ALTER ROLE loan_user SET search_path TO loan_schema, public;
ALTER ROLE scoring_user SET search_path TO scoring_schema, public;
ALTER ROLE payment_user SET search_path TO payment_schema, public;
ALTER ROLE notif_user SET search_path TO notification_schema, public;
ALTER ROLE report_user SET search_path TO reporting_schema, public;
ALTER ROLE investor_user SET search_path TO investor_schema, public;

-- ----------------------------------------------------------------------------
-- Default privileges so future Flyway-created objects are usable by the owner
-- and readable by the reporting role where explicitly intended.
-- ----------------------------------------------------------------------------
-- Scoring service needs read-only access to the bounded contexts used for feature extraction.
GRANT USAGE ON SCHEMA loan_schema, auth_schema, payment_schema TO scoring_user;
GRANT SELECT ON ALL TABLES IN SCHEMA loan_schema TO scoring_user;
GRANT SELECT ON ALL TABLES IN SCHEMA auth_schema TO scoring_user;
GRANT SELECT ON ALL TABLES IN SCHEMA payment_schema TO scoring_user;
ALTER DEFAULT PRIVILEGES FOR ROLE loan_user IN SCHEMA loan_schema GRANT SELECT ON TABLES TO scoring_user;
ALTER DEFAULT PRIVILEGES FOR ROLE auth_user IN SCHEMA auth_schema GRANT SELECT ON TABLES TO scoring_user;
ALTER DEFAULT PRIVILEGES FOR ROLE payment_user IN SCHEMA payment_schema GRANT SELECT ON TABLES TO scoring_user;

-- Reporting service is read-only across the operational bounded contexts.
GRANT USAGE ON SCHEMA auth_schema, loan_schema, payment_schema TO report_user;
GRANT SELECT ON ALL TABLES IN SCHEMA auth_schema TO report_user;
GRANT SELECT ON ALL TABLES IN SCHEMA loan_schema TO report_user;
GRANT SELECT ON ALL TABLES IN SCHEMA payment_schema TO report_user;
ALTER DEFAULT PRIVILEGES FOR ROLE auth_user IN SCHEMA auth_schema GRANT SELECT ON TABLES TO report_user;
ALTER DEFAULT PRIVILEGES FOR ROLE loan_user IN SCHEMA loan_schema GRANT SELECT ON TABLES TO report_user;
ALTER DEFAULT PRIVILEGES FOR ROLE payment_user IN SCHEMA payment_schema GRANT SELECT ON TABLES TO report_user;

ALTER DEFAULT PRIVILEGES FOR ROLE auth_user IN SCHEMA auth_schema GRANT ALL ON TABLES TO auth_user;
ALTER DEFAULT PRIVILEGES FOR ROLE auth_user IN SCHEMA auth_schema GRANT ALL ON SEQUENCES TO auth_user;
ALTER DEFAULT PRIVILEGES FOR ROLE loan_user IN SCHEMA loan_schema GRANT ALL ON TABLES TO loan_user;
ALTER DEFAULT PRIVILEGES FOR ROLE loan_user IN SCHEMA loan_schema GRANT ALL ON SEQUENCES TO loan_user;
ALTER DEFAULT PRIVILEGES FOR ROLE payment_user IN SCHEMA payment_schema GRANT ALL ON TABLES TO payment_user;
ALTER DEFAULT PRIVILEGES FOR ROLE payment_user IN SCHEMA payment_schema GRANT ALL ON SEQUENCES TO payment_user;
ALTER DEFAULT PRIVILEGES FOR ROLE notif_user IN SCHEMA notification_schema GRANT ALL ON TABLES TO notif_user;
ALTER DEFAULT PRIVILEGES FOR ROLE notif_user IN SCHEMA notification_schema GRANT ALL ON SEQUENCES TO notif_user;
ALTER DEFAULT PRIVILEGES FOR ROLE report_user IN SCHEMA reporting_schema GRANT ALL ON TABLES TO report_user;
ALTER DEFAULT PRIVILEGES FOR ROLE report_user IN SCHEMA reporting_schema GRANT ALL ON SEQUENCES TO report_user;
ALTER DEFAULT PRIVILEGES FOR ROLE scoring_user IN SCHEMA scoring_schema GRANT ALL ON TABLES TO scoring_user;
ALTER DEFAULT PRIVILEGES FOR ROLE scoring_user IN SCHEMA scoring_schema GRANT ALL ON SEQUENCES TO scoring_user;
ALTER DEFAULT PRIVILEGES FOR ROLE investor_user IN SCHEMA investor_schema GRANT ALL ON TABLES TO investor_user;
ALTER DEFAULT PRIVILEGES FOR ROLE investor_user IN SCHEMA investor_schema GRANT ALL ON SEQUENCES TO investor_user;
