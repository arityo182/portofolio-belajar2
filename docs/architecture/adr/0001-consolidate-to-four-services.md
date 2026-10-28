# ADR-0001 — Konsolidasi 7 Service Menjadi 4 Service

| Atribut | Nilai |
|---|---|
| Status | Accepted |
| Tanggal | 2026-10-27 |
| Pengganti | Arsitektur 7-service (api-gateway, auth, loan, payment, notification, reporting, scoring) |

## Konteks

Arsitektur awal memecah platform menjadi tujuh deployable backend. Setelah audit
fungsional, ditemukan bahwa:

- satu instance PostgreSQL dipakai bersama dengan schema terpisah, sehingga isolasi data
  bersifat logis, bukan fisik;
- `notification-service` (1.862 LOC) dan `reporting-service` (1.052 LOC) relatif kecil dan
  tidak memiliki transaksi tulis ke domain inti;
- `api-gateway` hanya 321 LOC dan fitur routing-nya sudah tersedia di Nginx;
- `RabbitMQ` hanya dipakai untuk retry payment, sementara Kafka sudah menjadi tulang
  punggung event;
- ZooKeeper tidak lagi diperlukan pada Kafka 3.x (KRaft);
- biaya runtime tujuh JVM mencapai ±3,6 GB RAM untuk beban demo.

## Keputusan

Konsolidasi menjadi **empat service backend**:

| Service | Isi |
|---|---|
| `auth-service` (identity) | auth, RBAC, reset password, borrower profile, enkripsi PII |
| `loan-service` (core) | pinjaman + **modul notification** + **modul reporting** |
| `payment-service` | gateway, webhook, disbursement, idempotency, retry |
| `scoring-service` | inferensi ML (FastAPI/TensorFlow) |

Infrastruktur: Nginx (TLS + routing + rate limit), PostgreSQL, Redis, Kafka (KRaft),
Prometheus/Grafana opsional.

Dihapus: `api-gateway`, `notification-service`, `reporting-service`, `RabbitMQ`, `ZooKeeper`.

## Konsekuensi

### Positif
- Deployable 7 → 4; container 15 → 11; RAM aplikasi ±3,6 GB → ±1,5 GB.
- Notifikasi dan reporting berbagi transaksi/JVM dengan lending tanpa dual-write.
- Jalur debug dan pipeline build lebih sedikit.
- Kontrak API tidak berubah; frontend tidak perlu diubah.

### Negatif / risiko
- Blast radius `auth-service` dan `loan-service` lebih besar.
- Reporting berbagi JVM dengan lending; query berat dapat memengaruhi latensi API.
- Rate limit per-user di gateway hilang; Nginx membatasi per IP.
- `loan_user` kini memiliki akses tulis ke `notification_schema`
  (least privilege dikompromikan untuk kesederhanaan).
- Retry payment memakai scheduler in-process untuk backoff; retry yang tertunda hilang
  jika pod restart (produksi perlu tabel retry persisten).

### Mitigasi
- Reporting tetap memakai datasource `report_user` **read-only**.
- Endpoint internal (`/api/v1/loans/internal/**`) diblokir di Nginx dan Ingress.
- Modul dipisahkan per paket (`com.koperasi.notification`, `com.koperasi.reporting`).
- Resource limit dan `-Xmx` ditetapkan per service.

## Alternatif yang ditolak

| Alternatif | Alasan ditolak |
|---|---|
| Tetap 7 service | biaya operasional tidak sepadan untuk ukuran tim/proyek |
| 3 service (auth dilebur ke core) | merge auth paling berisiko, manfaatnya hanya ±400 MB |
| Modular monolith penuh (1 service) | payment dan scoring lebih baik terisolasi (gateway eksternal & runtime ML) |

## Bukti verifikasi

Lihat `docs/functional-audit-v2.md` dan `docs/security-audit-v2.md`.
