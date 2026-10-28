> **Arsip:** dokumen audit ini disusun untuk arsitektur 7-service. Lihat
> [functional-audit-v2.md](../functional-audit-v2.md) dan
> [security-audit-v2.md](../security-audit-v2.md) untuk arsitektur terkini.

# Database Audit

- 7 schema: auth, loan, payment, notification, reporting, scoring, investor.
- Role per schema + scoring read-only grants ada di `init-db.sql`.
- Flyway V1/V2 loan; V1 auth/payment/notification; entity `ddl-auto=validate`.
- Index utama: borrower, status, created_at, due_date, source_event_id, idempotency.
- FK loan installments → loan applications; constraint status/amount.
- Skrip verifikasi: `scripts/verify-indexes.sql`.

## Gap
| Gap | Severity | Rekomendasi |
|---|---|---|
| reporting/investor schema belum punya tabel nyata | Medium | Implementasi saat reporting/investor service |
| Tidak ada enkripsi kolom sensitif (NIK/NPWP/rekening) di kode saat ini | High | Terapkan AES-256-GCM + key management |
| `loan_installment.overdue` belum diupdate scheduler | High | Scheduler + test |
| N+1 dashboard dihitung client-side | Medium | Endpoint agregat/reporting |
| Retensi/partisi audit event belum ada | Medium | Kebijakan retensi + partisi |
