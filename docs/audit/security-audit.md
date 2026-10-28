> **Arsip:** dokumen audit ini disusun untuk arsitektur 7-service. Lihat
> [functional-audit-v2.md](../functional-audit-v2.md) dan
> [security-audit-v2.md](../security-audit-v2.md) untuk arsitektur terkini.

# Security Audit

## Terpasang / Sesuai
- JWT access 15 menit, refresh 7 hari, rotasi + reuse detection, blacklist/jti.
- BCrypt cost 12.
- RBAC per endpoint.
- Security headers filter di service MVC + gateway; Nginx HSTS/CSP/frame/nosniff.
- CORS domain frontend; rate limit Nginx + Bucket4j gateway; Idempotency-Key.
- Internal token antar-service; scoring diblokir di edge.
- Input validation Jakarta/Zod; JPA parameter binding.

## Gap
| Gap | Severity | Rekomendasi |
|---|---|---|
| Gateway belum memvalidasi JWT di edge | High | Global filter/introspection |
| Rate limit Bucket4j in-memory, belum Redis terdistribusi | Medium | Bucket4j Redis backend |
| OWASP Dependency Check plugin terpasang tetapi scan belum dijalankan | High | Jalankan di CI, fail CVSS ≥9 |
| DSR/penghapusan data UU PDP belum otomatis | High | Endpoint DSR + workflow |
| Secret K8s template masih placeholder | High | Sealed Secrets/External Secrets |
| CSP API longgar? | Low | Evaluasi per service |
| Payment webhook signature sudah diverifikasi | — | Pertahankan |
