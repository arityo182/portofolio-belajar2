<!-- FILE: docs/architecture/adr.md -->
# Architecture Decision Records (ADR)
## Platform Koperasi Digital / P2P Lending untuk UMKM Indonesia

| Atribut | Nilai |
|---|---|
| Dokumen | Architecture Decision Records |
| Versi | 1.0 |
| Tanggal | 2026-10-24 |

---

> **Catatan (2026-10-27):** arsitektur telah dikonsolidasikan menjadi 4 service.
> Lihat [ADR-0001](adr/0001-consolidate-to-four-services.md). ADR lama di bawah tetap
> dipertahankan sebagai catatan historis; keputusan RabbitMQ sudah tidak berlaku.

## ADR-001: Kafka sebagai Primary Event Bus (vs pure RabbitMQ)
- **Status**: Accepted
- **Date**: 2026-10-24
- **Context**: Platform membutuhkan backbone komunikasi antar-microservice untuk event domain (siklus pinjaman, pembayaran, scoring, audit). Diperlukan *event streaming* dengan throughput tinggi, retensi event untuk *event sourcing*/replay, dan banyak konsumen independen (fan-out) per event. RabbitMQ unggul sebagai *task queue* namun kurang ideal untuk *event log* persisten, replay historis, dan konsumsi paralel skala besar.
- **Decision**: Menggunakan **Apache Kafka sebagai primary event bus** untuk seluruh event domain (`{service}.{entity}.{action}`), dengan retensi 7 hari, partisi berbasis key (ordering per entitas), dan multiple consumer group. **RabbitMQ dipertahankan khusus** untuk *task queue*, retry dengan backoff, dan DLQ (mis. retry pembayaran, email async).
- **Consequences**:
  - **Positif:** throughput tinggi & horizontal scaling via partisi; replay event untuk recovery & membangun read model (reporting); decoupling kuat antar-service; audit log natural lewat event log.
  - **Negatif:** kompleksitas operasional (ZooKeeper/KRaft, tuning partisi); dua teknologi messaging harus dikelola; *eventual consistency* menambah kompleksitas penalaran.

---

## ADR-002: Single PostgreSQL Instance dengan Schema Isolation (vs Database per Service)
- **Status**: Accepted
- **Date**: 2026-10-24
- **Context**: Setiap microservice butuh isolasi data, namun organisasi masih tahap awal (MVP → enterprise) dengan sumber daya operasional terbatas. Database-per-service murni menambah overhead infrastruktur (backup, HA, koneksi, biaya) yang berat di fase ini, sementara data tetap perlu batas kepemilikan yang jelas.
- **Decision**: Menggunakan **satu instance PostgreSQL dengan schema per service** (`auth_schema`, `loan_schema`, `scoring_schema`, `payment_schema`, `notification_schema`, `reporting_schema`). Setiap service punya kredensial & schema terpisah (least privilege), tanpa JOIN lintas-schema; pertukaran data via API/event.
- **Consequences**:
  - **Positif:** isolasi logis dengan biaya operasional rendah; backup/HA terpusat; migrasi per service (Flyway/Liquibase); jalur migrasi mudah ke database-per-service di masa depan.
  - **Negatif:** *single point of failure* & *shared resource contention* (noisy neighbor); batas isolasi lebih lemah dari database terpisah; scaling tulis terbatas pada satu instance (dimitigasi read replica & partitioning).

---

## ADR-003: Redis SETNX untuk Distributed Lock (vs ZooKeeper / Database Lock)
- **Status**: Accepted
- **Date**: 2026-10-24
- **Context**: Operasi finansial kritikal (disbursement, pembayaran) rawan *race condition* dan eksekusi ganda saat service di-scale horizontal. Dibutuhkan *distributed lock* yang cepat, ringan, dan ber-TTL. ZooKeeper menambah komponen infrastruktur baru; *database lock* (SELECT FOR UPDATE) membebani PostgreSQL dan rawan *lock contention* / deadlock.
- **Decision**: Menggunakan **Redis `SET key value NX PX <ttl>`** (SETNX + expiry) untuk distributed lock, dengan pola token unik per holder dan rilis aman via skrip Lua (compare-and-delete). Redis sudah dipakai untuk cache/session sehingga tidak menambah komponen baru. Untuk kebutuhan ketat, dapat ditingkatkan ke Redlock.
- **Consequences**:
  - **Positif:** latensi rendah & sederhana; TTL mencegah *deadlock* bila holder crash; memanfaatkan infrastruktur Redis yang sudah ada; tidak membebani DB.
  - **Negatif:** jaminan korektness lebih lemah pada *failover*/partisi Redis dibanding ZooKeeper; perlu disiplin token + Lua untuk rilis aman; wajib dikombinasikan **idempotency key** sebagai pertahanan berlapis untuk operasi finansial.

---

## ADR-004: FastAPI Python untuk ML Serving (vs Spring + DL4J)
- **Status**: Accepted
- **Date**: 2026-10-24
- **Context**: Model credit scoring dilatih dengan TensorFlow/Keras (ekosistem Python). Backend utama berbasis Java/Spring. Menyajikan model dari Java (mis. DL4J atau konversi model) menimbulkan friksi: dukungan operator TensorFlow terbatas, risiko ketidaksesuaian hasil train/serve, dan tooling data science Python tak tersedia.
- **Decision**: Menyajikan model melalui **service terpisah berbasis FastAPI (Python)** yang memuat model Keras native (`.h5`/SavedModel) + pipeline preprocessing identik dengan training. Integrasi dengan loan-service via REST sinkron + event Kafka. Opsi TensorFlow Serving untuk inferensi skala besar.
- **Consequences**:
  - **Positif:** paritas train/serve (preprocessing & model konsisten); akses penuh ekosistem ML Python; iterasi model cepat; isolasi beban inferensi & scaling independen (termasuk GPU).
  - **Negatif:** menambah *polyglot stack* (Java + Python) → beban DevOps & standar observability ganda; latensi jaringan antar-service; perlu kontrak API & versioning model yang disiplin.

---

## ADR-005: Idempotency Key Pattern untuk Semua Operasi Finansial
- **Status**: Accepted
- **Date**: 2026-10-24
- **Context**: Operasi finansial (pengajuan, disbursement, pembayaran cicilan) dapat ter-*retry* oleh klien, gateway, atau mekanisme retry messaging. Tanpa proteksi, hal ini berisiko **dana ganda** (double disbursement/double payment) — risiko fatal di fintech dengan konsekuensi regulasi.
- **Decision**: Menerapkan **idempotency key wajib** pada semua operasi finansial via header `Idempotency-Key`. Format `{service}-{userId}-{action}-{timestamp}-{nonce}`, disimpan di tabel `idempotency_keys` + cache Redis (TTL 24 jam). Request dengan key `COMPLETED` mengembalikan *cached response* (replay aman); key `PROCESSING` → HTTP 409. Dikombinasikan dengan Redis distributed lock (ADR-003).
- **Consequences**:
  - **Positif:** mencegah eksekusi ganda & dana ganda; retry menjadi aman (safe to retry); auditabilitas & rekonsiliasi lebih mudah; selaras kepatuhan OJK.
  - **Negatif:** klien wajib men-*generate* & mengirim key yang benar; penyimpanan tambahan + TTL cleanup; kompleksitas penanganan konflik (PROCESSING vs COMPLETED) pada sisi service.
