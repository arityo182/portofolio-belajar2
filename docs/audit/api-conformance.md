> **Arsip:** dokumen audit ini disusun untuk arsitektur 7-service. Lihat
> [functional-audit-v2.md](../functional-audit-v2.md) dan
> [security-audit-v2.md](../security-audit-v2.md) untuk arsitektur terkini.

# API Conformance

## Sesuai
- Auth: register, login, refresh, logout, me.
- Loan: products, applications, detail, installments, score (borrower own/reviewer/admin), status update.
- Payment: pay installment, status, webhooks, disbursement.
- Notification: list, read, read-all, SSE.
- Admin users dan admin loan products (tambahan Fase 8).
- Response envelope sukses/error konsisten.

## Deviasi / Tambahan
| Temuan | Severity | Rekomendasi |
|---|---|---|
| `PATCH /loans/applications/{id}/status` menerima `notes` tambahan; belum tercantum di kontrak | Medium | Perbarui `api-contract.md` |
| `/api/v1/admin/users` dan `/api/v1/loans/admin/products` belum ada di kontrak | Medium | Dokumentasikan |
| Endpoint internal `X-Internal-Token` tidak publik | Low | Dokumentasikan di dokumen arsitektur |
| Borrower score sebelumnya reviewer-only | Resolved | Kontrak diperbarui |
| Forgot/reset password belum punya endpoint backend | High | Implementasi endpoint + test |
| Dokumen KTP/selfie belum punya API upload | High | Bangun borrower-profile service |
| Reporting REST `/api/v1/reports/*` belum diimplementasikan | High | Implementasi reporting-service |
| Gateway tidak memvalidasi JWT di edge | High | Tambah filter JWT/gateway introspection |
