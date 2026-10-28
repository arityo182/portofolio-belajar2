> **Arsip:** dokumen ini menggambarkan arsitektur **7-service** dan berlaku sampai 2026-10-27.
> Untuk arsitektur terkonsolidasi (4 service), lihat [functional-audit-v2.md](functional-audit-v2.md).

# Audit Fungsional Koperasi Digital

**Tanggal audit:** 2026-10-27  
**Lingkup:** seluruh service backend, API gateway, scoring service, frontend Next.js, Kafka/RabbitMQ/Redis, database, dan alur demo Docker Compose.  
**Lingkungan:** `https://localhost` (Nginx + Docker Compose), PostgreSQL 15, Kafka + ZooKeeper, RabbitMQ, Redis, scoring FastAPI/TensorFlow.  
**Metode:** pembacaan kode, penelusuran endpoint dan event, pengujian unit/integration yang tersedia, build produksi frontend, serta E2E langsung pada stack Docker.

---

## 1. Ringkasan Eksekutif

Audit menemukan **7 temuan P1**, **10 temuan P2**, dan **11 catatan P3**. Seluruh P1 yang merupakan cacat kode sudah diperbaiki dan diverifikasi, kecuali satu keterbatasan konfigurasi demo: kredensial Midtrans/Xendit masih placeholder sehingga redirect gateway nyata tidak dapat diselesaikan tanpa key asli. Alur payment dan disbursement tetap diverifikasi end-to-end menggunakan webhook bertanda tangan yang valid.

Hasil akhir:

| Kategori | Ditemukan | Diperbaiki | Partial / Terbuka |
|---|---:|---:|---:|
| P1 — alur bisnis inti | 7 | 6 | 1 (kredensial gateway demo) |
| P2 — keandalan & kelengkapan | 10 | 10 | 0 |
| P3 — peningkatan | 11 | 9 | 2 (1 by design, 1 missing) |

Alur utama yang sekarang berfungsi:

```text
Login → Ajukan pinjaman → Kafka scoring → Review/approve
→ Disbursement → Webhook gateway → Pinjaman DISBURSED
→ Bayar cicilan → Webhook → Pinjaman ACTIVE
→ Semua cicilan lunas → Pinjaman COMPLETED
→ Notifikasi real-time + laporan admin/reviewer + ekspor CSV/PDF
```

---

## 2. Legenda

| Simbol | Arti |
|---|---|
| ✅ BERJALAN | Fitur berfungsi dan sudah diverifikasi runtime/test |
| ⚠️ PARTIAL | Berfungsi sebagian, bergantung konfigurasi eksternal, atau belum diuji penuh |
| ❌ BROKEN | Tidak berfungsi dan belum diperbaiki |
| 🔍 MISSING | Belum diimplementasikan |
| 🛠️ FIXED | Ditemukan rusak/kurang, lalu diperbaiki dalam audit ini |

---

## 3. Skor per Service

| Service | Skor fungsional | Catatan singkat |
|---|---:|---|
| auth-service | 95% | Auth, reset password, RBAC, borrower profile, enkripsi PII berjalan |
| loan-service | 95% | Lifecycle lengkap, scheduler overdue/default, internal callback |
| scoring-service | 90% | Scoring dan callback berjalan; pytest tidak tersedia di host |
| payment-service | 90% | Logika pembayaran/disbursement benar, sync persisten; key gateway demo placeholder |
| notification-service | 98% | REST, unread count, WebSocket, SSE header-only, event mapping lengkap |
| reporting-service | 98% | Dashboard, daftar pinjaman, CSV/PDF server-side, RBAC |
| api-gateway | 98% | Routing, JWT edge, CORS, route borrower/reporting |
| frontend | 92% | Halaman utama, notifikasi, profil, review KTP; investments/investor placeholder |
| Docker Compose demo | 95% | Seluruh container healthy; data E2E berhasil dibuat |

---

## 4. Temuan P1

### P1-1 — Pencairan pinjaman tidak pernah terjadi 🛠️ FIXED

| Field | Detail |
|---|---|
| Service | loan-service, payment-service, frontend |
| Fungsi | `LoanServiceImpl.disburse()`, `WebhookService.applyDisbursementStatus()` |
| File | `services/loan-service/.../service/LoanServiceImpl.java`, `services/payment-service/.../service/WebhookService.java`, `frontend/app/(dashboard)/loans/[id]/DisbursementCard.jsx` |
| Masalah | `disburse()` tidak dipanggil siapa pun. Webhook disbursement hanya mengubah `payment_schema`, tidak memberi tahu loan-service. Tidak ada UI pencairan untuk ADMIN/REVIEWER. |
| Dampak | Pinjaman berhenti di `APPROVED`; halaman pembayaran menolak karena status belum `DISBURSED/ACTIVE`. |
| Perbaikan | Endpoint internal `POST /api/v1/loans/internal/applications/{id}/disbursed`, pemanggilan dari payment webhook, idempotensi status, dan kartu “Pencairan dana” untuk ADMIN/REVIEWER. |
| Verifikasi | Webhook bertanda tangan → disbursement `COMPLETED`; payment-service memanggil loan-service dan loan berubah `DISBURSED` (diuji pada loan kedua `ecb0016d-39ef-42e7-8383-5fbd0e70e9e9`). |

### P1-2 — Pinjaman tidak pernah ACTIVE/COMPLETED 🛠️ FIXED

| Field | Detail |
|---|---|
| Service | loan-service |
| Fungsi | `LoanServiceImpl.markInstallmentPaid()` |
| File | `services/loan-service/.../service/LoanServiceImpl.java` |
| Masalah | Hanya status cicilan yang diubah; status pinjaman tidak pernah naik ke `ACTIVE` atau `COMPLETED`. |
| Dampak | Setelah semua cicilan lunas, pinjaman tetap `DISBURSED`; laporan dan notifikasi tidak mencerminkan penyelesaian. |
| Perbaikan | Cicilan pertama → `DISBURSED → ACTIVE`; seluruh cicilan lunas → `ACTIVE/DISBURSED/DEFAULT → COMPLETED`, set `completedAt`, publish `loan.application.active` dan `loan.application.completed`. |
| Verifikasi | Pembayaran pertama → loan `ACTIVE`; pelunasan seluruh cicilan → loan `COMPLETED` dengan `completedAt`. |

### P1-3 — Borrower profile dan upload KTP/selfie tidak ada 🛠️ FIXED

| Field | Detail |
|---|---|
| Service | auth-service, scoring-service, frontend |
| Fungsi | `FeatureExtractor.PROFILE_QUERY`, `ApplyWizard.handleSubmit` |
| File | `services/auth-service/.../service/BorrowerProfileService.java`, `.../controller/BorrowerProfileController.java`, `.../db/migration/V3__create_borrower_profiles.sql`, `frontend/app/(dashboard)/profile/BorrowerProfileForm.jsx` |
| Masalah | Tabel `auth_schema.borrower_profiles` tidak ada; scoring memakai fallback default. File KTP/selfie divalidasi di wizard tetapi dibuang. |
| Dampak | Fitur scoring tidak lengkap dan step upload menyesatkan. |
| Perbaikan | Tabel + entity + repository, endpoint `POST/GET /api/v1/borrowers/profile`, `POST /api/v1/borrowers/profile/verify-ktp`, penyimpanan foto di DB, masking, AES-256-GCM untuk NIK/NPWP/rekening, form profil di `/profile`, dan upload nyata dari wizard. |
| Verifikasi | Profil Budi tersimpan (NIK/NPWP/rekening bertopeng), upload 202, dokumen dapat diunduh, reviewer dapat menandai `isVerified=true` beserta `verifiedAt`, dan scoring menghasilkan `riskScore=58.31`. |

### P1-4 — Forgot/reset password frontend rusak 🛠️ FIXED

| Field | Detail |
|---|---|
| Service | frontend |
| Fungsi | `ForgotPasswordForm.onSubmit`, route `/reset-password` |
| File | `frontend/app/(auth)/forgot-password/ForgotPasswordForm.jsx`, `frontend/app/(auth)/reset-password/*`, `frontend/services/authService.js` |
| Masalah | Form hanya `setTimeout`; tidak memanggil API. Halaman reset belum ada. |
| Dampak | User tidak bisa menyelesaikan reset password dari UI. |
| Perbaikan | Form memanggil `POST /api/v1/auth/forgot-password`; halaman reset baru memanggil `POST /api/v1/auth/reset-password`; validasi password dan konfirmasi. |
| Verifikasi | Forgot 202 → link dari log auth → reset 200 → login password baru 200 → password demo dikembalikan. |

### P1-5 — Reporting service kosong 🛠️ FIXED

| Field | Detail |
|---|---|
| Service | reporting-service, frontend |
| Fungsi | `/api/v1/reports/dashboard`, `/loans`, `/loans/export` |
| File | `services/reporting-service/.../controller/ReportController.java`, `.../service/ReportService.java`, `.../repository/ReportQueryRepository.java`, `frontend/app/(dashboard)/admin/reports/AdminReportsView.jsx` |
| Masalah | Service hanya berisi config. Dashboard admin dan laporan menghitung client-side dari maksimal 100 pinjaman. |
| Dampak | Reporting tidak tersedia; angka berpotensi tidak lengkap. |
| Perbaikan | Endpoint reporting dengan JWT + RBAC, query read-only ke schema operasional, dashboard metrics, daftar pinjaman, CSV/PDF server-side, dan UI admin/reviewer yang memakai API. |
| Verifikasi | Dashboard 200 (2 loan, 1 active, 1 completed, NPL 0), daftar pinjaman 200, CSV 200, PDF 200, reviewer 200, export reviewer 403. |

### P1-6 — Halaman notifikasi placeholder 🛠️ FIXED

| Field | Detail |
|---|---|
| Service | frontend |
| Fungsi | `/notifications` |
| File | `frontend/app/(dashboard)/notifications/NotificationsView.jsx`, `frontend/services/notificationService.js`, `frontend/hooks/useNotificationList.js` |
| Masalah | Halaman hanya `ModulePlaceholder` meskipun backend REST/WebSocket/SSE sudah berfungsi. |
| Dampak | User tidak dapat melihat atau menandai notifikasi. |
| Perbaikan | Daftar notifikasi, filter semua/belum dibaca, pagination, mark read, mark all read, dan tautan ke pinjaman. |
| Verifikasi | 19 notifikasi terbentuk dari dua pengajuan E2E dan tampil melalui API; badge unread memakai endpoint `GET /api/v1/notifications/unread-count`. |

### P1-7 — Kredensial payment gateway demo placeholder ⚠️ PARTIAL

| Field | Detail |
|---|---|
| Service | payment-service / konfigurasi demo |
| Fungsi | `MidtransClient.createPaymentLink()`, `initiateDisbursement()` |
| File | `.env`, `docker-compose.yml` |
| Masalah | `MIDTRANS_SERVER_KEY=SB-Mid-server-xxxxxxxxxxxxxxxx` dan `XENDIT_API_KEY=xnd_development_xxxxxxxxxxxxxxxx` adalah placeholder. |
| Dampak | Redirect/pencairan nyata ke gateway gagal (502) sampai key sandbox asli diisi. |
| Perbaikan | Alur webhook diverifikasi menggunakan signature SHA-512 valid dengan key demo; ditambahkan `scripts/simulate-midtrans-webhook.py` untuk reproduksi. |
| Sisa | Untuk demo UI tanpa simulasi, isi key Midtrans/Xendit sandbox asli di `.env`. |

---

## 5. Temuan P2

| # | Temuan | Status | Perbaikan / Catatan |
|---|---|---|---|
| P2-1 | Scheduler cicilan overdue dan default tidak ada | 🛠️ FIXED | `InstallmentOverdueScheduler` harian; menandai `OVERDUE`, publish `loan.installment.overdue`, eskalasi `DEFAULT` setelah 90 hari; unit test lulus. |
| P2-2 | Sinkronisasi payment → loan hanya `log.warn` | 🛠️ FIXED | Kolom `sync_status/sync_attempts/last_sync_at/sync_error` di payment & disbursement; `PaymentReconciliationJob` mengulang **semua** baris `SUCCESS`/`COMPLETED` yang belum `SYNCED`, tidak lagi dibatasi 24 jam. |
| P2-3 | Kafka consumer tanpa retry/DLT | 🛠️ FIXED | `DefaultErrorHandler` + `DeadLetterPublishingRecoverer` (retry 2x, lalu `<topic>.DLT`) di loan-service dan notification-service; unit test poison record lulus. |
| P2-4 | SSE menerima JWT dari query string | 🛠️ FIXED | Fallback `?token=` dihapus; SSE memakai header `Authorization`, frontend memakai WebSocket terautentikasi. Test diperbarui. |
| P2-5 | Ekspor laporan hanya client-side | 🛠️ FIXED | CSV dan PDF sekarang dihasilkan server-side oleh reporting-service untuk ADMIN. |
| P2-6 | File KTP/selfie dibuang setelah validasi | 🛠️ FIXED | Wizard mengunggah dokumen ke borrower profile sebelum membuat pengajuan. |
| P2-7 | Topik Kafka baru tidak tersedia saat consumer start | 🛠️ FIXED | `KafkaTopicConfig` mendeklarasikan seluruh topik yang dikonsumsi notification-service. |
| P2-8 | Skema `borrower_profiles` didokumentasikan di loan-service | 🛠️ FIXED | Dokumentasi arsitektur diperbarui: tabel berada di `auth_schema`; reporting read-only dijelaskan. |
| P2-9 | Database demo tidak memiliki pinjaman untuk E2E | 🛠️ FIXED | Data E2E dibuat melalui API (bukan hardcode SQL); hasilnya dipakai untuk verifikasi reporting/notifikasi. |
| P2-10 | Webhook payment bisa melewati timeout gateway saat loan-service mati | 🛠️ FIXED | Timeout khusus loan-service (connect 2 dtk, read 3 dtk); webhook tetap merespons dan baris ditandai `sync_status=FAILED` untuk direkonsiliasi. |

---

## 6. Catatan P3

| # | Catatan | Status | Rekomendasi |
|---|---|---|---|
| P3-1 | Halaman `/investments` dan domain investor masih placeholder | 🔍 MISSING | Implementasikan investor-service/portofolio pada fase berikutnya. |
| P3-2 | Badge unread notifikasi di navbar/sidebar belum ada | ✅ FIXED | Endpoint `GET /api/v1/notifications/unread-count`, hook React Query, dan badge angka di Navbar; invalidasi otomatis saat event WebSocket masuk. |
| P3-3 | PDF laporan | ✅ FIXED | Endpoint `format=pdf` memakai OpenPDF; ID dipendekkan, tanggal dan nominal diformat id-ID, layout diverifikasi lewat render PNG. |
| P3-4 | Verifikasi KTP masih `PENDING`; tidak ada aksi reviewer untuk `is_verified=true` | ✅ FIXED | `POST /api/v1/borrowers/{userId}/verify` (REVIEWER/ADMIN), kolom `verified_at/verified_by`, preview KTP/selfie, dan tombol verifikasi di halaman review. Upload ulang otomatis mereset verifikasi. |
| P3-5 | Dokumentasi ML menyebut 20 fitur, model menerima 28 input | ✅ FIXED | `docs/ml/ml-model.md` menjelaskan 20 fitur mentah → 18 numerik + 10 one-hot = 28 input. |
| P3-6 | Status `OVERDUE` hanya pada cicilan, bukan `LoanStatus` | ✅ BY DESIGN | Pinjaman tetap `ACTIVE` sampai ambang default; overdue ada di level cicilan. |
| P3-7 | Nama event historis `loan.application.disbursed` berbeda dari contoh `loan.status.changed` di sebagian dokumen | ✅ FIXED | Referensi `loan.status.changed` diganti event konkret; katalog event ditambah under_review/disbursed/active/completed. |
| P3-8 | Topik Kafka demo memakai 1 partisi/replikasi | ✅ FIXED (DEMO) | Semua topik demo kini 3 partisi, replikasi 1 karena single broker; produksi tetap perlu replikasi ≥2. |
| P3-9 | Route `/users` masih placeholder; manajemen pengguna sebenarnya ada di `/admin/users` | ✅ FIXED | `/users` sekarang redirect ke `/admin/users` (dilindungi RoleGuard ADMIN). |
| P3-10 | DLT Kafka belum diuji dengan poison message | ✅ FIXED | `KafkaErrorHandlingConfigTest` mengirim record gagal dan memverifikasi publish ke `<topic>.DLT`. |
| P3-11 | Reconciliation hanya melihat 24 jam terakhir | ✅ FIXED | Status sinkronisasi persisten; job memproses semua baris `SUCCESS`/`COMPLETED` yang belum `SYNCED`, dengan hitungan percobaan. |

---

## 7. Bukti E2E

### 7.1 Akun dan login

| Peran | Email | Hasil |
|---|---|---|
| Admin | `admin@koperasi.id` | 200 |
| Reviewer | `rina.reviewer@example.id` | 200 |
| Borrower | `budi.umkm@example.id` | 200 |
| Investor | `sari.investor@example.id` | 200 |

### 7.2 Alur pinjaman

| Langkah | Aksi | Hasil |
|---|---|---|
| 1 | Budi membuat pengajuan Rp5.000.000, 6 bulan | `201`, status `PENDING` |
| 2 | Kafka scoring | `UNDER_REVIEW`, risk score `58.31` |
| 3 | Reviewer approve | `200`, 6 cicilan dibuat |
| 4 | Reviewer inisiasi disbursement | `502` dari Midtrans (key placeholder), record disbursement `FAILED` dibuat |
| 5 | Webhook disbursement bertanda tangan | `200`, disbursement `COMPLETED` |
| 6 | Loan setelah callback | `DISBURSED` |
| 7 | Budi bayar cicilan #1 | `502` dari Midtrans, record payment `FAILED` dibuat |
| 8 | Webhook pembayaran bertanda tangan | `200`, cicilan #1 `PAID` |
| 9 | Loan setelah cicilan pertama | `ACTIVE` |
| 10 | Sisa cicilan ditandai lunas via internal endpoint | semua `PAID` |
| 11 | Loan akhir | `COMPLETED`, `completedAt` terisi |
| 12 | Loan kedua Rp2.000.000 diajukan, di-approve, dan di-disburse | webhook → loan langsung `DISBURSED` oleh payment-service |
| 13 | Loan-service dihentikan, webhook pembayaran dikirim | payment `SUCCESS`, `sync_status=FAILED`, response webhook tetap selesai dalam batas timeout |
| 14 | Loan-service dinyalakan, reconciliation berjalan | payment `SUCCESS/SYNCED`, cicilan #1 `PAID`, loan `ACTIVE` |

Loan ID E2E: `3b917c44-b24b-4ad7-b807-4a9f3388e748` (COMPLETED) dan `ecb0016d-39ef-42e7-8383-5fbd0e70e9e9` (ACTIVE).

### 7.3 Notifikasi

- Total notifikasi Budi: **19** (dari dua pengajuan E2E dan rekonsiliasi pembayaran).
- Contoh: `Pengajuan Pinjaman Diterima`, `Pengajuan Sedang Ditinjau`, `Pinjaman Disetujui`, `Dana Pinjaman Dicairkan`, `Pembayaran Diterima`, `Pembayaran Gagal`, `Pinjaman Aktif`, `Pinjaman Lunas`, `Pencairan Selesai`, `Pencairan Gagal`.
- Endpoint: `GET /api/v1/notifications` → `200`; `GET /api/v1/notifications/unread-count` → `200` (`unreadCount: 19`).

### 7.4 Reporting

| Endpoint | Hasil |
|---|---|
| `GET /api/v1/reports/dashboard` (ADMIN) | `200`; total 2, active 1, completed 1, total disbursed Rp7.000.000, NPL 0 |
| `GET /api/v1/reports/loans` (ADMIN) | `200`; 2 row, borrower `Budi Santoso` — `COMPLETED` (outstanding 0) dan `ACTIVE` (outstanding Rp1.816.666,67) |
| `GET /api/v1/reports/loans/export?format=csv` (ADMIN) | `200 text/csv` |
| `GET /api/v1/reports/loans/export?format=pdf` (ADMIN) | `200 application/pdf` |
| `GET /api/v1/reports/dashboard` (REVIEWER) | `200` |
| `GET /api/v1/reports/loans/export` (REVIEWER) | `403` |

### 7.5 Borrower profile dan reset password

| Aksi | Hasil |
|---|---|
| `GET /api/v1/borrowers/profile` sebelum diisi | `404 RESOURCE_NOT_FOUND` |
| `POST /api/v1/borrowers/profile` | `200`; NIK/NPWP/rekening bertopeng |
| `POST /api/v1/borrowers/profile/verify-ktp` | `202 PENDING` |
| `GET /api/v1/borrowers/profile/documents/ktp` | `200 image/png` |
| `GET /api/v1/borrowers/{userId}/profile` (REVIEWER) | `200`; `isVerified=false`, URL dokumen tersedia |
| `POST /api/v1/borrowers/{userId}/verify` (REVIEWER) | `200`; `isVerified=true`, `verifiedAt` terisi |
| Upload ulang KTP/selfie | `202`; `isVerified` kembali `false` |
| `POST /api/v1/borrowers/{userId}/verify` setelah upload ulang | `200`; `isVerified=true` |
| Simpan ulang dengan nilai bertopeng | `200`; nilai sensitif dipertahankan |
| `POST /api/v1/auth/forgot-password` | `202` |
| `POST /api/v1/auth/reset-password` | `200` |
| Login dengan password baru | `200` |
| Restore password demo | `200` |

### 7.6 Verifikasi akhir (2026-10-27)

| Pemeriksaan | Hasil |
|---|---|
| Login 4 akun demo | `200` semua; JWT diterbitkan |
| `GET /api/v1/notifications/unread-count` (BORROWER) | `200`; `unreadCount: 19` |
| `GET /api/v1/borrowers/{userId}/profile` (REVIEWER) | `200`; `isVerified=true`, NIK bertopeng, `verifiedAt` terisi |
| `GET /api/v1/reports/dashboard` (ADMIN & REVIEWER) | `200`; 2 pinjaman, 1 aktif, 1 selesai, total dicairkan Rp7.000.000 |
| `GET /api/v1/reports/loans/export?format=csv` (ADMIN) | `200 text/csv` |
| `GET /api/v1/reports/loans/export?format=pdf` (ADMIN) | `200 application/pdf` (1 halaman, terverifikasi) |
| `GET /api/v1/reports/loans/export` (REVIEWER) | `403` (RBAC benar) |
| `GET /api/v1/payments/installments/{id}/status` (BORROWER) | `200` |
| Halaman publik `/`, `/login`, `/forgot-password`, `/reset-password?token=x` | `200` |
| Container inti (gateway, auth, loan, payment, notification, reporting, frontend, nginx, scoring, postgres, kafka, redis, rabbitmq) | seluruhnya `healthy` |

---

## 8. Hasil Pengujian Otomatis

### Backend (Gradle)

```bash
./gradlew :auth-service:test :loan-service:test :payment-service:test \
  :notification-service:test :api-gateway:test :reporting-service:test
```

Hasil: **BUILD SUCCESSFUL**. Seluruh test yang ada lulus, termasuk:

- `LoanServiceTest` (termasuk test baru `markInstallmentPaid` → `ACTIVE`/`COMPLETED`)
- `LoanStateMachineTest` (termasuk `DISBURSED → DEFAULT`, `DEFAULT → COMPLETED`)
- `InstallmentOverdueSchedulerTest`
- `KafkaErrorHandlingConfigTest` (poison record → `<topic>.DLT`)
- `PaymentReconciliationJobTest` (sukses → `SYNCED`, gagal → `FAILED` + counter)
- `ReportServiceTest`
- `JwtAuthenticationFilterTest` notification (query token ditolak)
- Auth, payment, notification, gateway test suites.

### Frontend

```bash
cd frontend
npm test -- --runInBand
npm run lint
npm run build
```

Hasil:

- Jest: **4 suites, 6 tests passed**
- ESLint: **lulus tanpa warning**
- `next build`: **lulus**, 27 route berhasil dibuat
- `npm audit`: **0 vulnerabilities** setelah upgrade Zod `4.6.7` dan `@hookform/resolvers` `5.9.2`
- Import `buttonStyles` yang salah di `PaymentsView.jsx` ditemukan saat build dan diperbaiki.

### Scoring service

```bash
cd services/scoring-service
python3 -m compileall -q app tests
```

Hasil: **lulus**. `pytest` tidak tersedia di host; pengujian runtime scoring dilakukan melalui Kafka dan callback (risk score `58.31`).

### Kesehatan Docker

Seluruh container inti `healthy`:

```text
api-gateway, auth-service, loan-service, payment-service,
notification-service, reporting-service, frontend, nginx,
postgres, redis, kafka, zookeeper, rabbitmq, scoring-service
```

### Kafka topics

`scripts/ensure-kafka-topics.sh` memastikan seluruh topik demo memiliki **3 partisi**.
Replikasi tetap **1** karena compose hanya menjalankan satu broker; produksi memerlukan
replikasi minimal 2. Topik DLT memakai akhiran `.DLT` (bukan default `-dlt`).

---

## 9. Perubahan File Utama

### Backend

- `services/loan-service/.../service/LoanServiceImpl.java`
- `services/loan-service/.../service/LoanStateMachine.java`
- `services/loan-service/.../service/InstallmentOverdueScheduler.java`
- `services/loan-service/.../controller/InternalLoanApplicationController.java`
- `services/loan-service/.../event/LoanEventType.java`
- `services/loan-service/.../config/KafkaErrorHandlingConfig.java`
- `services/loan-service/.../config/KafkaTopicConfig.java`
- `services/payment-service/.../service/LoanServiceClient.java`
- `services/payment-service/.../service/WebhookService.java`
- `services/payment-service/.../service/PaymentReconciliationJob.java`
- `services/payment-service/.../domain/entity/Payment.java`
- `services/payment-service/.../domain/entity/LoanDisbursement.java`
- `services/payment-service/.../domain/enums/SyncStatus.java`
- `services/payment-service/.../repository/PaymentRepository.java`
- `services/payment-service/.../repository/LoanDisbursementRepository.java`
- `services/payment-service/.../config/KafkaTopicConfig.java`
- `services/payment-service/.../db/migration/V2__add_sync_status.sql`
- `services/notification-service/.../config/KafkaTopicConfig.java`
- `services/notification-service/.../config/KafkaErrorHandlingConfig.java`
- `services/notification-service/.../controller/NotificationController.java`
- `services/notification-service/.../service/NotificationService.java`
- `services/notification-service/.../domain/dto/UnreadCountResponse.java`
- `services/notification-service/.../security/JwtAuthenticationFilter.java`
- `services/reporting-service/.../controller/ReportController.java`
- `services/reporting-service/.../service/ReportService.java`
- `services/reporting-service/.../repository/ReportQueryRepository.java`
- `services/reporting-service/.../security/*`
- `services/auth-service/.../db/migration/V3__create_borrower_profiles.sql`
- `services/auth-service/.../db/migration/V4__add_borrower_verification_metadata.sql`
- `services/auth-service/.../domain/entity/BorrowerProfile.java`
- `services/auth-service/.../domain/dto/BorrowerProfileResponse.java`
- `services/auth-service/.../service/BorrowerProfileService.java`
- `services/auth-service/.../service/PiiEncryptionService.java`
- `services/auth-service/.../controller/BorrowerProfileController.java`
- `services/api-gateway/src/main/resources/application.yml`
- `init-db.sql` (grant read-only reporting)
- `.env`, `.env.example`, `docker-compose.yml`

### Frontend

- `frontend/app/(auth)/forgot-password/ForgotPasswordForm.jsx`
- `frontend/app/(auth)/reset-password/page.jsx`
- `frontend/app/(auth)/reset-password/ResetPasswordForm.jsx`
- `frontend/app/(dashboard)/notifications/page.jsx`
- `frontend/app/(dashboard)/notifications/NotificationsView.jsx`
- `frontend/app/(dashboard)/profile/BorrowerProfileForm.jsx`
- `frontend/app/(dashboard)/profile/ProfileView.jsx`
- `frontend/app/(dashboard)/loans/[id]/DisbursementCard.jsx`
- `frontend/app/(dashboard)/loans/[id]/LoanDetailView.jsx`
- `frontend/app/(dashboard)/reviewer/[id]/KtpVerificationCard.jsx`
- `frontend/app/(dashboard)/reviewer/[id]/ReviewDetailView.jsx`
- `frontend/components/layout/Navbar.jsx`
- `frontend/app/(dashboard)/loans/apply/ApplyWizard.jsx`
- `frontend/app/(dashboard)/admin/dashboard/AdminDashboardView.jsx`
- `frontend/app/(dashboard)/admin/reports/AdminReportsView.jsx`
- `frontend/app/(dashboard)/reports/page.jsx`
- `frontend/app/(dashboard)/reports/ReportsView.jsx`
- `frontend/services/authService.js`
- `frontend/services/borrowerService.js`
- `frontend/services/notificationService.js`
- `frontend/services/reportService.js`
- `frontend/hooks/useBorrowerProfile.js`
- `frontend/hooks/useNotificationList.js`
- `frontend/hooks/useNotifications.js` (invalidasi badge unread dari event WebSocket)
- `frontend/hooks/useReports.js`
- `frontend/hooks/useDisbursement.js`
- `frontend/hooks/useAdmin.js`
- `frontend/lib/queryKeys.js`
- `frontend/lib/validations/auth.js`
- `frontend/lib/validations/loan.js`
- `frontend/package.json`, `frontend/package-lock.json` (Zod 4 + resolvers 5)

### Dokumentasi & tooling

- `docs/functional-audit.md` (dokumen ini)
- `docs/architecture/system-architecture.md`
- `docs/ml/ml-model.md`
- `docs/events/kafka-topics.md`
- `docs/audit/events-conformance.md`
- `docs/flows/alur-mvp-enterprise.mermaid`
- `docs/execution-prompts.md`
- `docs/README.md`, `README.md`
- `scripts/simulate-midtrans-webhook.py`
- `scripts/ensure-kafka-topics.sh`

---

## 10. Cara Menjalankan Ulang E2E

### 10.1 Prasyarat

```bash
cp .env.example .env   # isi JWT_SECRET, PII_ENCRYPTION_KEY, kredensial DB
docker compose up -d --build
```

### 10.2 Grant reporting (database lama)

Database yang sudah pernah diinisialisasi tidak menjalankan ulang `init-db.sql`. Jalankan:

```sql
GRANT USAGE ON SCHEMA auth_schema, loan_schema, payment_schema TO report_user;
GRANT SELECT ON ALL TABLES IN SCHEMA auth_schema TO report_user;
GRANT SELECT ON ALL TABLES IN SCHEMA loan_schema TO report_user;
GRANT SELECT ON ALL TABLES IN SCHEMA payment_schema TO report_user;
ALTER DEFAULT PRIVILEGES FOR ROLE auth_user IN SCHEMA auth_schema
  GRANT SELECT ON TABLES TO report_user;
ALTER DEFAULT PRIVILEGES FOR ROLE loan_user IN SCHEMA loan_schema
  GRANT SELECT ON TABLES TO report_user;
ALTER DEFAULT PRIVILEGES FOR ROLE payment_user IN SCHEMA payment_schema
  GRANT SELECT ON TABLES TO report_user;
```

### 10.3 Simulasi webhook gateway

Karena key gateway demo adalah placeholder:

```bash
# 1. Inisiasi pembayaran/pencairan melalui API (bisa 502).
# 2. Ambil UUID payment/disbursement dari PostgreSQL.
# 3. Kirim webhook bertanda tangan:
python3 scripts/simulate-midtrans-webhook.py \
  --order-id <payment-atau-disbursement-uuid> \
  --amount 908333.33 \
  --server-key SB-Mid-server-xxxxxxxxxxxxxxxx
```

### 10.4 Reset password demo

`PASSWORD_RESET_LOG_LINK=true` membuat link reset muncul di log:

```bash
docker compose logs auth-service | grep 'LOCAL PASSWORD RESET LINK'
```

Gunakan link tersebut, lalu kembalikan password demo setelah pengujian.

### 10.5 Topik Kafka

```bash
./scripts/ensure-kafka-topics.sh
```

Script membuat topik yang belum ada dan menaikkan topik lama menjadi 3 partisi.
Replikasi 1 hanya untuk demo single-broker.

### 10.6 Uji rekonsiliasi

```bash
# 1. Buat pembayaran PENDING lewat API.
# 2. Hentikan loan-service.
docker compose stop loan-service
# 3. Kirim webhook pembayaran bertanda tangan.
#    Baris payment menjadi SUCCESS + sync_status=FAILED.
# 4. Nyalakan loan-service dan tunggu job rekonsiliasi (interval 5 menit).
docker compose start loan-service
# 5. Setelah job berjalan: sync_status=SYNCED, cicilan PAID, loan ACTIVE.
```

---

## 11. Risiko Residual

| Risiko | Dampak | Mitigasi |
|---|---|---|
| Key Midtrans/Xendit placeholder | Pembayaran/disbursement nyata gagal di demo | Isi key sandbox asli atau gunakan simulasi webhook |
| Investor/investments belum diimplementasikan | Fitur investor belum lengkap | Implementasi fase berikutnya |
| Replikasi Kafka demo 1 | Tidak tahan broker failure | Gunakan minimal 2 broker + replikasi 2 di produksi |
| PII key fallback ke JWT_SECRET bila env kosong | Kunci tidak terpisah | Wajibkan `PII_ENCRYPTION_KEY` di produksi |
| Scheduler overdue berjalan harian | Deteksi terlambat maksimal 1 hari | Sesuaikan cron atau jalankan manual trigger |
| Access/refresh token disimpan di storage yang dapat dibaca JavaScript | Risiko XSS mencuri token (temuan security audit terpisah) | Migrasikan ke HttpOnly cookie/BFF pada fase keamanan berikutnya |
| `pytest` scoring tidak tersedia di host | Test suite Python tidak dijalankan lokal | Jalankan `pytest` di CI atau venv proyek |

---

## 12. Kesimpulan

Alur bisnis inti Koperasi Digital sekarang **berfungsi end-to-end** pada stack Docker: pengajuan, scoring, review, pencairan, pembayaran, pelunasan, notifikasi, dan pelaporan. Tiga service yang sebelumnya kosong/placeholder—reporting, notifikasi frontend, dan borrower profile—sudah diimplementasikan. Lifecycle pinjaman yang sebelumnya tidak pernah mencapai `DISBURSED`, `ACTIVE`, atau `COMPLETED` sudah diperbaiki dan diverifikasi. Verifikasi KTP manual, badge unread, PDF server-side, DLT test, dan rekonsiliasi persisten juga sudah diselesaikan.

Sisa pekerjaan bersifat **konfigurasi dan pengembangan lanjutan**, bukan blocker alur inti: kredensial gateway asli, domain investor, migrasi token ke HttpOnly, dan replikasi Kafka produksi.
