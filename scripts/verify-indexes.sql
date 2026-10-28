-- Verifikasi index penting. Jalankan dengan:
--   psql "$DATABASE_URL" -f scripts/verify-indexes.sql
-- Setiap query harus mengembalikan minimal satu baris.
SELECT schemaname, tablename, indexname
FROM pg_indexes
WHERE schemaname IN ('auth_schema','loan_schema','payment_schema','notification_schema','reporting_schema','scoring_schema')
  AND (
    indexname LIKE '%borrower%'
    OR indexname LIKE '%status%'
    OR indexname LIKE '%created_at%'
    OR indexname LIKE '%due_date%'
    OR indexname LIKE '%source_event%'
    OR indexname LIKE '%idempotency%'
    OR indexname LIKE '%user_id%'
  )
ORDER BY schemaname, tablename, indexname;

SELECT count(*) AS foreign_keys
FROM information_schema.table_constraints
WHERE constraint_type = 'FOREIGN KEY'
  AND table_schema IN ('auth_schema','loan_schema','payment_schema','notification_schema','reporting_schema','scoring_schema');
