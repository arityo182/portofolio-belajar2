# Koperasi Digital — P2P Lending Platform

[![Build](https://img.shields.io/badge/build-passing-brightgreen)](#)
[![Coverage](https://img.shields.io/badge/coverage-80%25-yellowgreen)](#)
[![Docker](https://img.shields.io/badge/docker-ready-blue)](#)
[![License](https://img.shields.io/badge/license-MIT-blue)](#license)

Platform koperasi digital dan P2P lending untuk UMKM Indonesia: AI credit scoring,
pengelolaan siklus pinjaman, pembayaran digital, notifikasi real-time, dan pelaporan
yang siap audit.

## Problem Statement

Sekitar 60% UMKM di Indonesia ditolak oleh bank karena tidak memiliki riwayat kredit
formal, agunan yang memadai, atau laporan keuangan yang terstruktur. Akibatnya banyak
usaha produktif kesulitan mendapatkan modal kerja dengan biaya yang wajar, sementara
pemberi dana kesulitan menemukan portofolio UMKM yang terukur risikonya.

Koperasi Digital menjawab masalah tersebut melalui platform P2P lending yang
menghubungkan peminjam UMKM dengan investor, dilengkapi AI credit scoring untuk menilai
kelayakan secara objektif, transparansi jadwal cicilan, serta integrasi payment gateway
(Midtrans dan Xendit). Tata kelola RBAC, audit trail, enkripsi data sensitif, dan
kepatuhan UU PDP menjadi fondasi kepercayaan bagi seluruh pihak.

## Tech Stack

| Layer | Technology |
|---|---|
| Language (backend) | Java 21 |
| Backend framework | Spring Boot 3.4, Spring Security, Spring Data JPA, Spring Data Redis, Spring Kafka |
| Edge | Nginx (TLS, routing, rate limit) |
| Database | PostgreSQL 15 (schema per module), Flyway |
| Cache / token store | Redis 7 |
| Messaging | Apache Kafka (KRaft) |
| AI scoring | Python 3.11, FastAPI, TensorFlow, scikit-learn |
| Frontend | Next.js 15, React 19, JavaScript |
| Build | Gradle 8.12 multi-project, npm |
| Infrastructure | Docker, Docker Compose, Nginx, Prometheus, Grafana |
| Testing | JUnit 5, Mockito, Testcontainers, Pytest, Jest, React Testing Library, MSW |
| Security | JWT, BCrypt (cost 12), AES-256-GCM, TLS 1.3, RBAC |

## Demo Accounts (Local Only)

Jalankan stack lokal lalu buka **https://localhost**. Akun demo memakai password yang sama:

| Peran | Email | Password | Halaman |
|---|---|---|---|
| Admin | `admin@koperasi.id` | `Koperasi#2026Aman` | `/admin/dashboard` |
| Reviewer | `rina.reviewer@example.id` | `Koperasi#2026Aman` | `/reviewer/queue` |
| Borrower | `budi.umkm@example.id` | `Koperasi#2026Aman` | `/dashboard` |
| Investor | `sari.investor@example.id` | `Koperasi#2026Aman` | `/dashboard` |

> Akun ini hanya untuk pengembangan lokal. Ganti password, `JWT_SECRET`, dan seluruh
> kredensial default sebelum deployment. Untuk database Compose yang baru, isi akun demo
> dengan perintah pada bagian [Quick Start](#quick-start).

## Architecture Overview

Arsitektur sistem, batasan service, alur data, dan keputusan desain dijelaskan pada
[docs/architecture/system-architecture.md](docs/architecture/system-architecture.md).
Kebijakan keamanan, alur JWT, RBAC, dan strategi enkripsi tersedia pada
[docs/architecture/security-architecture.md](docs/architecture/security-architecture.md).
Kontrak API ada pada [docs/api/api-contract.md](docs/api/api-contract.md).

```text
Client (Next.js) → Nginx (TLS + routing) → Auth / Core / Payment
                                   │              │
                                   ├── Redis       ├── PostgreSQL (schema per module)
                                   ├── Kafka       └── Scoring Service (FastAPI + TensorFlow)
```

## Services

| Service | Port | Responsibility |
|---|---:|---|
| auth-service (identity) | 8081 | Registrasi, login, refresh rotation, RBAC, profil peminjam, enkripsi PII |
| loan-service (core) | 8082 | Produk pinjaman, pengajuan, state machine, cicilan, **notifikasi**, **reporting** |
| scoring-service | 8083 | Inferensi AI credit scoring (FastAPI + TensorFlow) |
| payment-service | 8084 | Disbursement, pembayaran cicilan, webhook Midtrans/Xendit, retry |
| frontend | 3000 | Aplikasi web Next.js (React 19) |

> Arsitektur ini adalah **modular monolith + 2 service pendukung**. Modul notifikasi dan
> reporting tetap terpisah secara paket dan datasource (reporting memakai `report_user`
> read-only), tetapi berjalan dalam satu deployable untuk menekan biaya operasional.
> Keputusan dan trade-off ada di [`docs/architecture/adr/0001-consolidate-to-four-services.md`](docs/architecture/adr/0001-consolidate-to-four-services.md).

## Project Structure

```text
koperasi/
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew
├── gradle/wrapper/
├── docker-compose.yml
├── init-db.sql
├── .env.example
├── .editorconfig
├── .gitignore
├── README.md
├── scripts/
│   ├── ensure-kafka-topics.sh
│   ├── seed-demo-accounts.sql
│   └── simulate-midtrans-webhook.py
├── docs/
│   ├── README.md
│   ├── srs-koperasi.md
│   ├── execution-prompts.md
│   ├── architecture/
│   ├── api/
│   ├── database/
│   ├── deployment/
│   ├── design/
│   ├── development/
│   ├── compliance/
│   ├── events/
│   ├── flows/
│   └── ml/
├── services/
│   ├── auth-service/
│   ├── loan-service/
│   ├── payment-service/
│   └── scoring-service/
├── frontend/
├── nginx/
├── prometheus/
└── grafana/
```

## Docs Navigation

| Dokumen | Path | Deskripsi |
|---|---|---|
| SRS Koperasi | [docs/srs-koperasi.md](docs/srs-koperasi.md) | Kebutuhan fungsional, non-fungsional, aktor, dan batasan sistem |
| System Architecture | [docs/architecture/system-architecture.md](docs/architecture/system-architecture.md) | Arsitektur microservices, diagram komponen, dan alur integrasi |
| Security Architecture | [docs/architecture/security-architecture.md](docs/architecture/security-architecture.md) | Alur JWT, RBAC, enkripsi, audit, rate limit, dan CORS |
| Architecture Decision Records | [docs/architecture/adr.md](docs/architecture/adr.md) | Catatan keputusan arsitektur beserta konsekuensinya |
| ERD MVP | [docs/database/erd-mvp.mermaid](docs/database/erd-mvp.mermaid) | Entity relationship diagram untuk cakupan MVP |
| ERD Enterprise | [docs/database/erd-enterprise.mermaid](docs/database/erd-enterprise.mermaid) | ERD skala enterprise dan perluasan domain |
| API Contract | [docs/api/api-contract.md](docs/api/api-contract.md) | Kontrak endpoint, request/response, dan kode error |
| Kafka Topics | [docs/events/kafka-topics.md](docs/events/kafka-topics.md) | Skema event, topic Kafka, retry, dan DLT |
| Design System | [docs/design/design-system.md](docs/design/design-system.md) | Token desain, komponen UI, dan pedoman visual |
| Alur MVP Enterprise | [docs/flows/alur-mvp-enterprise.mermaid](docs/flows/alur-mvp-enterprise.mermaid) | Diagram alur bisnis MVP hingga enterprise |
| Loan Lifecycle | [docs/flows/loan-lifecycle.mermaid](docs/flows/loan-lifecycle.mermaid) | Diagram transisi status siklus pinjaman |
| ML Model | [docs/ml/ml-model.md](docs/ml/ml-model.md) | Fitur, pelatihan, evaluasi, dan deployment model scoring |
| Testing Strategy | [docs/development/testing-strategy.md](docs/development/testing-strategy.md) | Piramida test, coverage target, dan Testcontainers |
| Audit Akhir | [docs/audit/final-report.md](docs/audit/final-report.md) | Verdict audit Fase 0–11 dan gap register |
| Security Audit v2 | [docs/security-audit-v2.md](docs/security-audit-v2.md) | Audit keamanan arsitektur 4 service |
| Functional Audit v2 | [docs/functional-audit-v2.md](docs/functional-audit-v2.md) | Audit fungsional 4 service + bukti E2E Docker |
| Coding Conventions | [docs/development/coding-conventions.md](docs/development/coding-conventions.md) | Standar penamaan, struktur paket, response wrapper, logging |
| Gitflow | [docs/development/gitflow.md](docs/development/gitflow.md) | Strategi branch, commit conventional, dan alur PR |
| UU PDP Checklist | [docs/compliance/uu-pdp-checklist.md](docs/compliance/uu-pdp-checklist.md) | Kepatuhan UU PDP No. 27/2022 dan kontrol privasi |
| Env Variables | [docs/deployment/env-variables.md](docs/deployment/env-variables.md) | Referensi environment variable per service |
| Cara Run Docker Lokal | [docs/deployment/cara-run-docker-local.md](docs/deployment/cara-run-docker-local.md) | Panduan menjalankan stack secara lokal |
| Docker AWS | [docs/deployment/docker-aws.md](docs/deployment/docker-aws.md) | Strategi deployment container ke AWS |
| CI/CD GitHub Actions | [docs/deployment/ci-cd-github-actions.md](docs/deployment/ci-cd-github-actions.md) | Pipeline build, test, scan, dan deploy |
| Execution Prompts | [docs/execution-prompts.md](docs/execution-prompts.md) | Rencana eksekusi per fase pengembangan |

## Quick Start

Prasyarat: Docker Compose v2, RAM minimal 8 GB (disarankan 16 GB), dan port `80/443`
serta `3000`, `8081–8084`, `5432`, `6379`, `9090` tidak dipakai proses lain.

```bash
cp .env.example .env

# Wajib: buat secret lokal yang unik sebelum Compose dijalankan
JWT_SECRET="$(openssl rand -base64 48)"
INTERNAL_CALLBACK_TOKEN="$(openssl rand -hex 32)"
sed -i "s|^JWT_SECRET=.*|JWT_SECRET=${JWT_SECRET}|" .env
sed -i "s|^INTERNAL_CALLBACK_TOKEN=.*|INTERNAL_CALLBACK_TOKEN=${INTERNAL_CALLBACK_TOKEN}|" .env
chmod 600 .env

docker compose up -d --build
docker compose ps
```

Jika port default bentrok, ubah nilai berikut di `.env` sebelum `docker compose up`:

```env
POSTGRES_PORT=5436
REDIS_PORT=6381
KAFKA_PORT=29093
NOTIFICATION_PORT=8095
PROMETHEUS_PORT=9091
GRAFANA_PORT=3001
```

Buka **https://localhost** (sertifikat self-signed, pilih *Advanced → Proceed*).
Jangan gunakan `http://localhost:3000` untuk login karena API frontend dilayani
melalui Nginx pada origin yang sama.

Setelah `auth-service` sehat, isi akun demo ke database Compose:

```bash
docker compose exec -T postgres psql -U koperasi -d koperasi < scripts/seed-demo-accounts.sql
```

Perintah operasional:

```bash
docker compose logs -f auth-service
docker compose stop
docker compose down
docker compose down -v   # hapus container + volume database
```

> Jangan gunakan nilai default pada `.env.example` untuk lingkungan shared atau produksi.
> Ganti seluruh password, `JWT_SECRET`, dan kredensial payment gateway.

## Contributing

1. Baca [docs/development/gitflow.md](docs/development/gitflow.md) untuk strategi branch
   dan format Conventional Commits.
2. Ikuti [docs/development/coding-conventions.md](docs/development/coding-conventions.md)
   untuk standar Java/Spring Boot, React, penamaan, dan response wrapper.
3. Jalankan test dan pastikan coverage memenuhi target pada
   [docs/development/testing-strategy.md](docs/development/testing-strategy.md).
4. Buka Pull Request dengan checklist yang disediakan dan lakukan self-review.

## License

Dirilis di bawah lisensi [MIT](LICENSE).
