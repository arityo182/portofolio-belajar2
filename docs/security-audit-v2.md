# Audit Keamanan v2 — Arsitektur 4 Service

**Tanggal:** 2026-10-27
**Lingkup:** edge Nginx, `auth-service`, `loan-service` (core), `payment-service`, `scoring-service`, datasource reporting, Kafka, Docker Compose, K8s/Terraform.
**Metode:** review perubahan arsitektur, uji langsung endpoint, uji izin database, pemeriksaan header, dan verifikasi dependensi.
**Arsitektur sebelumnya:** [`security-audit.md`](security-audit.md) (arsip).

---

## 1. Ringkasan

| Tingkat | Ditemukan | Diperbaiki | Terbuka |
|---|---:|---:|---:|
| High | 1 | 1 | 0 |
| Medium | 3 | 2 | 1 (by design) |
| Low | 2 | 1 | 1 |

## 2. Perubahan Permukaan Serangan

| Perubahan | Dampak |
|---|---|
| `api-gateway` dihapus | validasi JWT edge & rate limit per-user hilang; tiap service tetap memvalidasi JWT sendiri |
| Notifikasi & reporting masuk core | blast radius lebih besar; boundary dijaga per modul + datasource |
| RabbitMQ dihapus | satu protokol & kredensial berkurang |
| Endpoint internal kini melewati Nginx | wajib diblokir eksplisit di edge |
| `loan_user` mendapat akses `notification_schema` | least privilege melemah |

## 3. Temuan

| # | Temuan | Tingkat | Status | Bukti / Perbaikan |
|---|---|---|---|---|
| S2-1 | Endpoint internal (`/api/v1/loans/internal/**`) dapat dijangkau publik melalui edge | **High** | ✅ FIXED | Nginx `return 404`; Ingress `fixed-response 404`; diverifikasi `404`. Token internal tetap fail-closed bila kosong. |
| S2-2 | Rate limit per-user hilang bersama gateway | Medium | ✅ MITIGATED | Nginx `limit_req`: login 5/m, register 3/m, API 100/m per IP. Per-user limit tidak lagi tersedia (tercatat). |
| S2-3 | `loan_user` memiliki INSERT/UPDATE/DELETE pada `notification_schema` | Medium | ⚠️ ACCEPTED | Hanya tabel `notifications`; `report_user` tetap read-only. Opsi: datasource notifikasi terpisah. |
| S2-4 | Retry payment memakai jadwal in-memory | Medium | ⚠️ ACCEPTED | Tidak ada eksekusi kode dari input; hanya kehilangan retry saat restart. |
| S2-5 | Poison message Kafka dapat menyebabkan loop konsumsi | Low | ✅ FIXED | `spring.json.value.default.type` + DLT; diverifikasi `payment.retry.DLT`. |
| S2-6 | Token disimpan di storage yang dapat dibaca JavaScript | Low* | ⚠️ OPEN | Temuan lama (security audit terpisah). Migrasi HttpOnly/BFF belum dilakukan. |

\* Dampak tinggi bila XSS terjadi; mitigasi CSP + sanitasi sudah ada, tetapi bukan pengganti HttpOnly.

## 4. Kontrol yang Diverifikasi

| Kontrol | Hasil |
|---|---|
| `report_user` read-only | `SELECT` lintas schema `200`; `INSERT` ke `notifications` **ditolak** |
| Header keamanan edge | HSTS, X-Frame-Options DENY, nosniff, Referrer-Policy, Permissions-Policy terpasang |
| Endpoint internal tanpa token | `404` |
| WebSocket | hanya header `Authorization`; token query tidak diterima |
| SSE | memerlukan JWT header (`401` tanpa token) |
| Webhook | verifikasi signature Midtrans/Xendit tetap aktif |
| Secrets | `JWT_SECRET`, `PII_ENCRYPTION_KEY`, `INTERNAL_CALLBACK_TOKEN` wajib diisi (fail-fast di Compose) |
| Dependensi frontend | `npm audit` 0 vulnerabilities (Zod 4.6.7) |

## 5. Catatan K8s / Terraform

| Item | Status |
|---|---|
| Blokir `/api/v1/loans/internal/` di Ingress | ✅ `server-snippet` + fixed-response |
| Routing ALB per service | ✅ auth/core/payment/scoring + `/ws` ke core |
| Target group gateway/notification/reporting | ✅ dihapus |
| Secrets via Secrets Manager | tetap (tidak berubah) |
| Rate limit Ingress | `limit-rps: 100` (per IP) |

## 6. Rekomendasi Lanjutan

1. Migrasikan token ke HttpOnly cookie/BFF (temuan lama, masih terbuka).
2. Pisahkan datasource notifikasi agar `loan_user` tidak punya hak tulis lintas schema.
3. Tambahkan rate limit per-user di core (Bucket4j) bila diperlukan.
4. Ganti scheduler retry in-memory dengan tabel retry persisten.
5. Aktifkan image scanning + dependency check di CI.

## 7. Kesimpulan

Konsolidasi tidak menambah temuan High yang terbuka. Satu risiko High baru (endpoint internal terekspos) sudah ditutup dan diverifikasi. Sisa temuan bersifat trade-off arsitektur yang terdokumentasi atau pekerjaan keamanan lanjutan yang sudah tercatat.
