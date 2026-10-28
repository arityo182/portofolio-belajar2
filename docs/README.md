# Dokumentasi Koperasi Digital

Pusat navigasi dokumentasi teknis dan produk untuk Platform Koperasi Digital /
P2P Lending UMKM Indonesia. Mulai dari [README proyek](../README.md) untuk gambaran
umum, lalu masuk ke dokumen sesuai peran Anda.

## Daftar Dokumen

| No | Dokumen | Path | Deskripsi |
|---:|---|---|---|
| 1 | SRS Koperasi | [srs-koperasi.md](srs-koperasi.md) | Kebutuhan fungsional dan non-fungsional, aktor, serta batasan sistem |
| 2 | Execution Prompts | [execution-prompts.md](execution-prompts.md) | Rencana eksekusi per fase beserta acceptance criteria |
| 3 | System Architecture | [architecture/system-architecture.md](architecture/system-architecture.md) | Arsitektur 4 service, diagram komponen, dan alur integrasi |
| 4 | Security Architecture | [architecture/security-architecture.md](architecture/security-architecture.md) | Alur JWT, RBAC, enkripsi, audit logging, rate limit, dan CORS |
| 5 | Architecture Decision Records | [architecture/adr.md](architecture/adr.md) | Keputusan arsitektur penting; lihat juga [ADR-0001](architecture/adr/0001-consolidate-to-four-services.md) |
| 6 | ERD MVP | [database/erd-mvp.mermaid](database/erd-mvp.mermaid) | Entity relationship diagram cakupan MVP |
| 7 | ERD Enterprise | [database/erd-enterprise.mermaid](database/erd-enterprise.mermaid) | ERD skala enterprise dan perluasan domain |
| 8 | API Contract | [api/api-contract.md](api/api-contract.md) | Kontrak endpoint, request/response, dan kode error |
| 9 | Kafka Topics | [events/kafka-topics.md](events/kafka-topics.md) | Skema event, topic Kafka, retry, dan DLT |
| 10 | Design System | [design/design-system.md](design/design-system.md) | Token desain, komponen UI, dan pedoman visual |
| 11 | Alur MVP Enterprise | [flows/alur-mvp-enterprise.mermaid](flows/alur-mvp-enterprise.mermaid) | Diagram alur bisnis dari MVP hingga enterprise |
| 12 | Loan Lifecycle | [flows/loan-lifecycle.mermaid](flows/loan-lifecycle.mermaid) | Diagram transisi status siklus pinjaman |
| 13 | ML Model | [ml/ml-model.md](ml/ml-model.md) | Fitur, pelatihan, evaluasi, dan deployment model scoring |
| 14 | Testing Strategy | [development/testing-strategy.md](development/testing-strategy.md) | Piramida test, coverage target, dan penggunaan Testcontainers |
| 15 | Coding Conventions | [development/coding-conventions.md](development/coding-conventions.md) | Standar penamaan, struktur paket, response wrapper, dan logging |
| 16 | Gitflow | [development/gitflow.md](development/gitflow.md) | Strategi branch, Conventional Commits, dan alur Pull Request |
| 17 | UU PDP Checklist | [compliance/uu-pdp-checklist.md](compliance/uu-pdp-checklist.md) | Kepatuhan UU PDP No. 27/2022 dan kontrol privasi |
| 18 | Environment Variables | [deployment/env-variables.md](deployment/env-variables.md) | Referensi environment variable untuk setiap service |
| 19 | Cara Run Docker Lokal | [deployment/cara-run-docker-local.md](deployment/cara-run-docker-local.md) | Panduan menjalankan stack secara lokal dengan Docker Compose |
| 20 | Docker AWS | [deployment/docker-aws.md](deployment/docker-aws.md) | Strategi deployment container ke AWS |
| 21 | CI/CD GitHub Actions | [deployment/ci-cd-github-actions.md](deployment/ci-cd-github-actions.md) | Pipeline build, test, security scan, dan deploy |
| 22 | Audit Akhir | [audit/README.md](audit/README.md) | Traceability, gap register, dan verdict audit Fase 0–11 |
| 23 | Security Audit | [security-audit.md](security-audit.md) | Audit OWASP Top 10:2025, temuan, fix, dan sisa risiko |
| 24 | Functional Audit (arsitektur 7-service) | [functional-audit.md](functional-audit.md) | Arsip audit 2026-10-27 untuk arsitektur lama |
| 25 | Functional Audit v2 (4-service) | [functional-audit-v2.md](functional-audit-v2.md) | Audit fungsional arsitektur terkonsolidasi + bukti E2E Docker |
| 26 | Security Audit v2 (4-service) | [security-audit-v2.md](security-audit-v2.md) | Audit keamanan arsitektur terkonsolidasi |

## Mulai Cepat

| Kebutuhan | Dokumen |
|---|---|
| Memahami masalah dan ruang lingkup produk | [SRS](srs-koperasi.md) |
| Memahami arsitektur dan batas service | [System Architecture](architecture/system-architecture.md) |
| Mengimplementasikan endpoint | [API Contract](api/api-contract.md) + [Coding Conventions](development/coding-conventions.md) |
| Menyiapkan environment lokal | [Environment Variables](deployment/env-variables.md) + [Cara Run Docker Lokal](deployment/cara-run-docker-local.md) |
| Menulis dan mengevaluasi test | [Testing Strategy](development/testing-strategy.md) |
| Menyiapkan pipeline rilis | [CI/CD GitHub Actions](deployment/ci-cd-github-actions.md) |
| Memeriksa kepatuhan privasi | [UU PDP Checklist](compliance/uu-pdp-checklist.md) |

## Konvensi Dokumentasi

- Gunakan Markdown dengan heading hierarkis dan tabel untuk referensi.
- Diagram alur dan ERD menggunakan [Mermaid](https://mermaid.js.org/).
- Setiap perubahan kontrak API atau event wajib memperbarui dokumen terkait dalam
  Pull Request yang sama.
- Jangan menuliskan secret, token, atau data pribadi nyata di dokumentasi.
