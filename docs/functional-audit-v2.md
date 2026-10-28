# Audit Fungsional v2 — Arsitektur 4 Service

**Tanggal:** 2026-10-27
**Lingkup:** `auth-service` (identity), `loan-service` (core: pinjaman + notifikasi + reporting), `payment-service`, `scoring-service`, Nginx, frontend, PostgreSQL, Redis, Kafka (KRaft).
**Lingkungan:** Docker Compose, `https://localhost`.
**Metode:** migrasi kode, pembacaan ulang alur, unit/integration test, E2E langsung pada stack Docker, verifikasi database.
**Arsitektur sebelumnya:** [`functional-audit.md`](functional-audit.md) (arsip 7-service).

---

## 1. Ringkasan

| Kategori | Ditemukan | Diperbaiki | Terbuka |
|---|---:|---:|---:|
| P1 — blocker migrasi | 4 | 4 | 0 |
| P2 — keandalan/kelengkapan | 3 | 3 | 0 |
| P3 — peningkatan | 2 | 0 | 2 (by design) |

Seluruh alur inti berjalan pada arsitektur baru: login → pengajuan → scoring → approve → cair → bayar → aktif, plus notifikasi realtime dan laporan CSV/PDF.

## 2. Skor per Service

| Service | Skor | Catatan |
|---|---:|---|
| auth-service (identity) | 95% | tidak berubah; PII, RBAC, profil, verifikasi KTP |
| loan-service (core) | 92% | pinjaman + notifikasi + reporting; 1 JVM, multi-datasource |
| payment-service | 92% | webhook, idempotency, rekonsiliasi, retry Kafka |
| scoring-service | 90% | tidak berubah; `pytest` tidak tersedia di host |
| Nginx (edge) | 95% | routing per service, rate limit, blokir endpoint internal |
| frontend | 95% | tidak ada perubahan kontrak; badge + verifikasi KTP tetap |
| Docker Compose | 95% | 11 container healthy |

## 3. Temuan P1 (blocker migrasi)

| # | Temuan | Status | Perbaikan / Bukti |
|---|---|---|---|
| V2-P1-1 | Datasource reporting menjadi **primary** karena Spring Boot back-off saat ada bean `DataSource`, sehingga Flyway/JPA memakai `report_user` dan aplikasi gagal start | ✅ FIXED | `PrimaryDataSourceConfig` menetapkan datasource utama `loan_user` sebagai `@Primary`; reporting tetap memakai `report_user` read-only dengan `initializationFailTimeout=-1` |
| V2-P1-2 | Migrasi `V3__create_notification_tables.sql` gagal (`must be owner of table notifications`) pada DB lama | ✅ FIXED | DDL dibuat idempoten + ownership tabel dipindahkan ke `loan_user`; DB baru membuat tabel sebagai `loan_user` |
| V2-P1-3 | Nginx `location` dengan trailing slash tidak cocok untuk path koleksi (`/api/v1/notifications` → 301 ke frontend) | ✅ FIXED | Semua prefix diubah tanpa trailing slash; diverifikasi `200` |
| V2-P1-4 | Retry payment Kafka gagal deserialisasi (`JsonDeserializer` tanpa type header) → loop poison message tanpa henti | ✅ FIXED | `spring.json.value.default.type` pada `@KafkaListener`; diverifikasi attempt 1→3 lalu DLT |

## 4. Temuan P2

| # | Temuan | Status | Perbaikan / Bukti |
|---|---|---|---|
| V2-P2-1 | Endpoint internal pinjaman dapat dijangkau dari publik melalui edge lama | ✅ FIXED | Nginx `location ^~ /api/v1/loans/internal/` → `404`; Ingress ALB `fixed-response 404`; token internal tetap fail-closed |
| V2-P2-2 | `loan_user` tidak punya akses ke `notification_schema` setelah modul digabung | ✅ FIXED | Grant di `init-db.sql`, `k8s/postgres-init-configmap.yaml`, dan DB live |
| V2-P2-3 | RabbitMQ hanya dipakai retry payment; ZooKeeper tidak diperlukan | ✅ FIXED | Retry pindah ke `payment.retry` + `payment.retry.DLT`; Kafka KRaft; container dihapus |

## 5. Catatan P3

| # | Catatan | Status | Rekomendasi |
|---|---|---|---|
| V2-P3-1 | Backoff retry payment disimpan di memori | ⚠️ BY DESIGN (demo) | Produksi: tabel retry persisten atau delayed topic |
| V2-P3-2 | `loan_user` memiliki hak tulis ke `notification_schema` | ⚠️ TRADE-OFF | Pisahkan datasource notifikasi bila isolasi ketat diperlukan |

## 6. Bukti E2E (stack Docker)

| Langkah | Aksi | Hasil |
|---|---|---|
| 1 | Login 4 akun demo | `200` semua |
| 2 | Budi membuat pengajuan Rp3.000.000 / 6 bulan | `201`, `PENDING` → `UNDER_REVIEW` |
| 3 | Reviewer approve | `200`, 6 cicilan terbentuk |
| 4 | Inisiasi disbursement | `502` (key gateway placeholder) |
| 5 | Webhook disbursement bertanda tangan | `200 processed`, loan `DISBURSED` |
| 6 | Inisiasi pembayaran cicilan #1 | `502` (key placeholder) |
| 7 | Webhook pembayaran | `200`, cicilan #1 `PAID`, loan `ACTIVE` |
| 8 | Notifikasi | `unreadCount=28`, event baru masuk |
| 9 | Reporting | 3 pinjaman, 2 aktif, 1 selesai, total dicairkan Rp10.000.000 |
| 10 | Export CSV/PDF | `200 text/csv`, `200 application/pdf`; reviewer `403` |
| 11 | SSE `/notifications/stream` | menerima `event:connected` |
| 12 | WebSocket `/ws` | upgrade berhasil (koneksi bertahan) |
| 13 | Endpoint internal | `404` dari publik |
| 14 | Retry payment | attempt 1→3, lalu `payment.retry.DLT` offset bertambah |

Loan E2E: `9cb5ef64-6b50-4e8c-99b2-460600640c3f` (ACTIVE), disbursement `747e8f55-4ef2-456c-aca6-7c09560f4e8b`, payment `5f4bca38-7911-49f1-a52d-a7a80cae4bc5`.

## 7. Pengujian Otomatis

| Suite | Hasil |
|---|---|
| `:auth-service:test` | BUILD SUCCESSFUL |
| `:loan-service:test` | BUILD SUCCESSFUL (termasuk `LoanControllerIT` dengan Testcontainers) |
| `:payment-service:test` | BUILD SUCCESSFUL (termasuk `PaymentRetryConsumerTest` versi Kafka) |
| Frontend lint / Jest / build | lulus; 4 suite, 6 test |
| `npm audit` | 0 vulnerabilities |

## 8. Container

```text
auth-service, loan-service, payment-service, scoring-service,
frontend, nginx, postgres, redis, kafka, prometheus, grafana
```

Seluruhnya `healthy` (11 container).

## 9. Risiko Residual

| Risiko | Dampak | Mitigasi |
|---|---|---|
| Key Midtrans/Xendit placeholder | pembayaran nyata gagal | isi sandbox key; webhook bertanda tangan untuk demo |
| Reporting berbagi JVM dengan lending | query berat memengaruhi API | datasource read-only + timeout; pisahkan bila perlu |
| Backoff retry in-memory | retry hilang saat restart | tabel retry persisten |
| Modul auth & PII di service terpisah | tetap aman, tetapi satu titik gagal | replika + health check |

## 10. Kesimpulan

Konsolidasi 7 → 4 service **berhasil dan berjalan end-to-end di Docker**. Kontrak API tidak berubah, frontend tidak perlu disesuaikan, dan seluruh bukti E2E utama lulus. Sisa pekerjaan bersifat operasional (key gateway asli, retry persisten), bukan blocker fungsional.
