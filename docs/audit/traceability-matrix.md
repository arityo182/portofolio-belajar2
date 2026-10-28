> **Arsip:** dokumen audit ini disusun untuk arsitektur 7-service. Lihat
> [functional-audit-v2.md](../functional-audit-v2.md) dan
> [security-audit-v2.md](../security-audit-v2.md) untuk arsitektur terkini.

# Traceability Matrix

Status: **Sesuai / Menyimpang / Belum / Tidak berlaku**.

| Requirement | Dokumen | Implementasi | Bukti | Status |
|---|---|---|---|---|
| Registrasi, login, refresh rotation, logout | SRS, api-contract 1.1–1.5 | auth-service | AuthControllerIT, Redis blacklist | Sesuai |
| RBAC BORROWER/INVESTOR/REVIEWER/ADMIN | SRS, security-architecture | JWT claims + `@PreAuthorize` | Controller test/IT | Sesuai |
| Pengajuan pinjaman + state machine | SRS, loan-lifecycle | loan-service | LoanStateMachineTest, LoanControllerIT | Sesuai |
| Idempotency & distributed lock | SRS, execution phase 2 | Redis lock + idempotency key | LoanServiceTest, PaymentServiceTest | Sesuai |
| AI credit scoring 20 fitur + threshold | ml-model | scoring-service FastAPI | pytest, model artifacts | Sesuai |
| Disbursement & cicilan Midtrans/Xendit | SRS, api-contract 4 | payment-service | PaymentControllerIT, Webhook tests | Sesuai (sandbox) |
| Notifikasi Kafka + WebSocket/SSE + email | events, api-contract 5 | notification-service | NotificationServiceTest, WS manual | Sesuai |
| Frontend auth + base layout | execution phase 6 | Next.js App Router | lint/build, browser test | Sesuai |
| Borrower flow | execution phase 7 | frontend + loan/payment API | mock browser acceptance | Sesuai |
| Reviewer/admin flow | execution phase 8 | frontend + admin APIs | mock browser acceptance | Sesuai |
| Dashboard/reporting | SRS, api-contract 6 | reporting-service masih scaffold; admin dashboard client-side | Admin UI test | Menyimpang |
| Dokumen KTP/selfie | api-contract 2.3 | Belum ada borrower-profile service | UI placeholder | Belum |
| Forgot/reset password | execution phase 6 | UI success state; backend endpoint belum ada | Login/forgot UI | Menyimpang |
| Audit trail | security-architecture | audit.event.created dari auth | Auth event publisher | Sebagian |
| UU PDP | compliance checklist | Kontrol dasar ada; DSR/retensi belum otomatis | security-audit | Sebagian |
| DevOps & deployment | deployment docs | Compose/K8s/Nginx/CI/Terraform | devops-audit | Sebagian |
