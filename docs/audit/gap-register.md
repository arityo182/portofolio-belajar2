> **Arsip:** dokumen audit ini disusun untuk arsitektur 7-service. Lihat
> [functional-audit-v2.md](../functional-audit-v2.md) dan
> [security-audit-v2.md](../security-audit-v2.md) untuk arsitektur terkini.

# Gap Register

| ID | Severity | Gap | Status | Rekomendasi |
|---|---|---|---|---|
| G-01 | High | Gateway JWT edge validation belum | Open | Global filter + test |
| G-02 | High | Borrower profile & upload KTP/selfie belum | Open | Service + storage + API |
| G-03 | High | Reporting service masih scaffold | Open | Implement REST + consumer |
| G-04 | High | Forgot/reset password backend belum | Open | Endpoint + email token |
| G-05 | High | `loan.installment.overdue` producer belum | Open | Scheduler + event |
| G-06 | High | OWASP scan belum dijalankan | Open | CI `dependencyCheckAnalyze` |
| G-07 | High | Compose full smoke belum | Open | `docker compose up` + verify-health |
| G-08 | High | Terraform plan/apply belum | Open | Sandbox plan |
| G-09 | High | Enkripsi data sensitif di DB belum | Open | AES-GCM + KMS |
| G-10 | High | DSR/retensi UU PDP belum otomatis | Open | Workflow + audit |
| G-11 | Medium | Admin endpoints/notes belum di API contract | Open | Update docs |
| G-12 | Medium | Rate limit gateway in-memory | Open | Redis backend |
| G-13 | Medium | Dashboard admin data sample/client-side | Open | Reporting aggregate |
| G-14 | Medium | N+1 dashboard | Open | Endpoint agregat |
| G-15 | Medium | CI belum dieksekusi | Open | Push repo |
| G-16 | Low | Chart bundle besar | Open | Dynamic import |
| G-17 | Low | `promtool`/Alertmanager belum | Open | Tambah tooling |
