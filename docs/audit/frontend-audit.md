> **Arsip:** dokumen audit ini disusun untuk arsitektur 7-service. Lihat
> [functional-audit-v2.md](../functional-audit-v2.md) dan
> [security-audit-v2.md](../security-audit-v2.md) untuk arsitektur terkini.

# Frontend Audit

## Sesuai
- Design system biru #2563EB, Inter/JetBrains Mono, dark/light token.
- Auth guard middleware, auto-refresh Axios, Zustand, React Query.
- Borrower: dashboard, apply 4 langkah, list/detail, bayar, WS invalidation.
- Reviewer: queue, detail skor/gauge, approve/reject, history.
- Admin: dashboard charts, users CRUD, loans, products CRUD, reports CSV/PDF.
- WCAG: skip link, focus ring, aria, keyboard modal, kontras.
- Mobile responsive tanpa horizontal overflow (diverifikasi 390px).

## Gap
| Gap | Severity | Rekomendasi |
|---|---|---|
| Dokumen KTP/selfie hanya preview lokal | High | Integrasi upload service |
| Data peminjam di review hanya email/phone, bukan profil usaha | Medium | Integrasi borrower-profile |
| Dashboard admin memakai sampel 100 pinjaman + skor 20 | Medium | Endpoint reporting/agregat |
| Forgot password belum memanggil API | High | Sambungkan endpoint reset |
| Chart bundle besar (Recharts) | Low | Dynamic import chart per halaman |
