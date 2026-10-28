> **Arsip:** dokumen audit ini disusun untuk arsitektur 7-service. Lihat
> [functional-audit-v2.md](../functional-audit-v2.md) dan
> [security-audit-v2.md](../security-audit-v2.md) untuk arsitektur terkini.

# Test Evidence

## Backend (terakhir)
| Service | Tests | Failures | Line coverage |
|---|---:|---:|---:|
| auth-service | 69 | 0 | ≥93% |
| loan-service | 120 | 0 | ≥90% |
| payment-service | 78 | 0 | ≥86% |
| notification-service | 74 | 0 | ≥80% |
| api-gateway | build only | 0 | — |
| reporting-service | build only | 0 | — |

Perintah: `./gradlew build --no-daemon`.

## Scoring
- `pytest -q` (24 test dari Fase 3; verifikasi ulang diperlukan setelah image final).

## Frontend
- `npm run lint` bersih.
- `npm test -- --runInBand` → 4 suite, 6 test lulus (LoginForm, ApplyWizard, StatsCard, AuthService/MSW).
- `npm run build` sukses.
- Headless browser Fase 8: approve/reject, gauge, charts, DataTable, CRUD, CSV/PDF (mock API).

## Belum
- OWASP Dependency Check belum dijalankan penuh.
- `docker compose up` end-to-end belum.
- Jest coverage report belum ditetapkan threshold.
