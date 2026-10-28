-- Demo accounts for local development only.
-- Run after auth-service has completed Flyway migrations:
--   docker compose exec -T postgres psql -U koperasi -d koperasi < scripts/seed-demo-accounts.sql

BEGIN;

INSERT INTO auth_schema.users (id, email, password_hash, phone, status, created_at, updated_at)
VALUES
  (gen_random_uuid(), 'admin@koperasi.id',
   public.crypt('Koperasi#2026Aman', public.gen_salt('bf', 12)),
   '+6281234567804', 'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), 'rina.reviewer@example.id',
   public.crypt('Koperasi#2026Aman', public.gen_salt('bf', 12)),
   '+6281234567803', 'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), 'budi.umkm@example.id',
   public.crypt('Koperasi#2026Aman', public.gen_salt('bf', 12)),
   '+6281234567801', 'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), 'sari.investor@example.id',
   public.crypt('Koperasi#2026Aman', public.gen_salt('bf', 12)),
   '+6281234567802', 'ACTIVE', NOW(), NOW())
ON CONFLICT (email) DO UPDATE
SET password_hash = EXCLUDED.password_hash,
    status = 'ACTIVE',
    updated_at = NOW();

-- Reset assigned roles so repeated runs remain deterministic.
DELETE FROM auth_schema.user_roles ur
USING auth_schema.users u
WHERE ur.user_id = u.id
  AND u.email IN (
    'admin@koperasi.id',
    'rina.reviewer@example.id',
    'budi.umkm@example.id',
    'sari.investor@example.id'
  );

INSERT INTO auth_schema.user_roles (user_id, role_id, assigned_at)
SELECT u.id, r.id, NOW()
FROM auth_schema.users u
JOIN auth_schema.roles r ON r.name = 'ADMIN'
WHERE u.email = 'admin@koperasi.id'
ON CONFLICT DO NOTHING;

INSERT INTO auth_schema.user_roles (user_id, role_id, assigned_at)
SELECT u.id, r.id, NOW()
FROM auth_schema.users u
JOIN auth_schema.roles r ON r.name = 'REVIEWER'
WHERE u.email = 'rina.reviewer@example.id'
ON CONFLICT DO NOTHING;

INSERT INTO auth_schema.user_roles (user_id, role_id, assigned_at)
SELECT u.id, r.id, NOW()
FROM auth_schema.users u
JOIN auth_schema.roles r ON r.name = 'BORROWER'
WHERE u.email = 'budi.umkm@example.id'
ON CONFLICT DO NOTHING;

INSERT INTO auth_schema.user_roles (user_id, role_id, assigned_at)
SELECT u.id, r.id, NOW()
FROM auth_schema.users u
JOIN auth_schema.roles r ON r.name = 'INVESTOR'
WHERE u.email = 'sari.investor@example.id'
ON CONFLICT DO NOTHING;

COMMIT;

SELECT u.email, u.status, string_agg(r.name, ', ' ORDER BY r.name) AS roles
FROM auth_schema.users u
JOIN auth_schema.user_roles ur ON ur.user_id = u.id
JOIN auth_schema.roles r ON r.id = ur.role_id
WHERE u.email IN (
  'admin@koperasi.id',
  'rina.reviewer@example.id',
  'budi.umkm@example.id',
  'sari.investor@example.id'
)
GROUP BY u.email, u.status
ORDER BY u.email;
