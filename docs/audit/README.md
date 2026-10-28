> **Arsip:** dokumen audit ini disusun untuk arsitektur 7-service. Lihat
> [functional-audit-v2.md](../functional-audit-v2.md) dan
> [security-audit-v2.md](../security-audit-v2.md) untuk arsitektur terkini.

# Audit Akhir Koperasi Digital

Audit ini memverifikasi implementasi Fase 0–10 terhadap seluruh dokumen di `docs/`.
Dokumen di folder ini adalah bukti audit, bukan spesifikasi baru.

## Metode
1. Inventaris dokumen dan requirement.
2. Bandingkan kode/konfigurasi dengan dokumen.
3. Jalankan verifikasi otomatis yang tersedia.
4. Catat gap, severity, bukti, dan rekomendasi.
5. Berikan verdict dan waiver bila diperlukan.

## Daftar Dokumen
| Dokumen | Isi |
|---|---|
| [traceability-matrix.md](traceability-matrix.md) | Requirement → implementasi → bukti |
| [api-conformance.md](api-conformance.md) | Kontrak API vs controller/DTO |
| [events-conformance.md](events-conformance.md) | Kafka/RabbitMQ |
| [database-audit.md](database-audit.md) | Schema, migrasi, index, grant |
| [security-audit.md](security-audit.md) | JWT, RBAC, header, UU PDP, OWASP |
| [frontend-audit.md](frontend-audit.md) | Design system, role flow, WCAG |
| [devops-audit.md](devops-audit.md) | Compose, K8s, Nginx, CI/CD, Terraform |
| [test-evidence.md](test-evidence.md) | Perintah dan hasil test |
| [gap-register.md](gap-register.md) | Daftar gap + severity |
| [final-report.md](final-report.md) | Verdict dan sign-off |

## Cara Menjalankan Ulang
```bash
./gradlew build --no-daemon
cd frontend && npm ci && npm run lint && npm test -- --runInBand && npm run build
cd ../services/scoring-service && pytest -q
docker compose config --quiet
kubectl kustomize k8s > /tmp/k8s.yaml && kubeconform -strict -summary /tmp/k8s.yaml
cd terraform && terraform init -backend=false && terraform validate
```
