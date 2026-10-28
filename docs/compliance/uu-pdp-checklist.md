<!-- FILE: docs/compliance/uu-pdp-checklist.md -->
# Checklist Kepatuhan UU PDP
## Platform Koperasi Digital / P2P Lending untuk UMKM Indonesia

| Atribut | Nilai |
|---|---|
| Dokumen | UU PDP Compliance Checklist |
| Versi | 1.0 |
| Tanggal | 2026-10-24 |
| Regulasi | UU No. 27 Tahun 2022 tentang Pelindungan Data Pribadi |
| Klasifikasi | Confidential / Legal |

> Catatan: dokumen ini acuan kepatuhan teknis-operasional, bukan nasihat hukum final. Validasi akhir dengan penasihat hukum & DPO.

---

## 1. Data Inventory

| Entity | Field | Klasifikasi | Treatment | Retensi |
|---|---|---|---|---|
| users | email | Pribadi Umum | Plain (unik) | Selama akun aktif + 5 th |
| users | phone | Pribadi Umum | Plain | Selama akun aktif + 5 th |
| users | password_hash | Kredensial | Hash (BCrypt cost 12) | Selama akun aktif |
| borrower_profiles | nik_encrypted | **Pribadi Khusus** | **Enkripsi AES-256-GCM** | 10 th (kewajiban OJK) |
| borrower_profiles | npwp_encrypted | **Pribadi Khusus** | **Enkripsi AES-256-GCM** | 10 th |
| borrower_profiles | bank_account_encrypted | **Pribadi Khusus** (finansial) | **Enkripsi AES-256-GCM** | 10 th |
| borrower_profiles | ktp_photo_url | **Pribadi Khusus** (biometrik/identitas) | Objek terenkripsi (S3 SSE-KMS), URL ber-signed | 10 th |
| borrower_profiles | selfie_photo_url | **Pribadi Khusus** (biometrik) | Objek terenkripsi (S3 SSE-KMS) | 10 th |
| borrower_profiles | full_name | Pribadi Umum | Plain (mask di UI publik) | 10 th |
| borrower_profiles | date_of_birth | Pribadi Umum | Plain | 10 th |
| borrower_profiles | gender | Pribadi Umum | Plain | 10 th |
| borrower_profiles | address | Pribadi Umum | Plain (mask parsial) | 10 th |
| borrower_profiles | income_monthly | **Pribadi Khusus** (finansial) | Plain (akses terbatas) | 10 th |
| borrower_profiles | business_name/type | Pribadi Umum | Plain | 10 th |
| loan_applications | amount, tenor, purpose | Finansial/Operasional | Plain (akses RBAC) | 10 th |
| credit_scores | score, risk_category, features_json | Finansial (profiling) | Plain (akses REVIEWER/ADMIN) | 10 th |
| loan_disbursements | external_transaction_id | Finansial | Plain | 10 th |
| payments | webhook_payload | Finansial | Plain (akses terbatas) | 10 th |
| investor_profiles | available_balance, total_invested | **Pribadi Khusus** (finansial) | Plain (akses terbatas) | 10 th |
| investment_transactions | amount, balance_before/after | Finansial | Plain | 10 th |
| audit_logs | ip_address, user_agent, actor_id | Pribadi Umum (metadata) | Plain, append-only (hash chain) | 7 th |
| refresh_tokens | token_hash | Kredensial | Hash | Sampai expired/revoked |

Legenda treatment: **Enkripsi** = terenkripsi at-rest; **Hash** = one-way; **Plain** = tidak dienkripsi di kolom tetapi dilindungi RBAC + TLS + enkripsi disk.

---

## 2. Pemetaan 8 Prinsip UU PDP → Implementasi Teknis

| # | Prinsip (Pasal) | Implementasi Teknis |
|---|---|---|
| 1 | **Pembatasan tujuan** (Ps. 16) | Data hanya diproses untuk penilaian kredit, pencairan, penagihan, kepatuhan. Tujuan dicatat di Privacy Notice & consent record; tidak ada pemrosesan sekunder tanpa dasar hukum baru. |
| 2 | **Minimisasi data** (Ps. 17) | Hanya field yang relevan untuk scoring/kepatuhan dikumpulkan; form membatasi input; tidak menyimpan data berlebih; feature store hanya memuat fitur yang dibutuhkan model. |
| 3 | **Akurasi data** (Ps. 18) | Hak koreksi via API (PATCH); verifikasi KTP/KYC; validasi format (NIK 16 digit, NPWP); timestamp `updated_at`; audit perubahan. |
| 4 | **Penyimpanan terbatas** (Ps. 19) | Kebijakan retensi per field (lihat §1); job penghapusan/anonimisasi otomatis saat retensi berakhir; arsip cold storage immutable. |
| 5 | **Keamanan data** (Ps. 20) | AES-256-GCM at-rest, TLS 1.3 in-transit, BCrypt, KMS envelope encryption, RBAC least-privilege, backup terenkripsi, WAF, rate limiting. |
| 6 | **Akuntabilitas** (Ps. 21) | Audit log tamper-evident (hash chaining), DPO ditunjuk, DPA dengan prosesor pihak ketiga, dokumentasi ROPA (Record of Processing Activities), log keputusan AI (explainability). |
| 7 | **Transparansi** (Ps. 22) | Privacy Notice jelas sebelum pengumpulan; informasi tujuan, dasar hukum, retensi, hak subjek data; notifikasi perubahan kebijakan. |
| 8 | **Pembatasan akses (RBAC)** | Spring Security RBAC (BORROWER/INVESTOR/REVIEWER/ADMIN), need-to-know, data sensitif di-*mask* di UI, akses dekripsi tercatat di audit log, prinsip least privilege pada DB (user per schema). |

---

## 3. Hak Subjek Data + Implementasi API

| Hak (Pasal) | Endpoint | Method | SLA | Catatan Implementasi |
|---|---|---|---|---|
| **Hak akses** (Ps. 5) | `/api/v1/users/my-data` | GET | **14 hari** | Mengembalikan seluruh data pribadi subjek (data sensitif didekripsi untuk pemilik, tercatat di audit) |
| **Hak koreksi** (Ps. 6) | `/api/v1/users/my-data` | PATCH | **14 hari** | Memperbarui data; re-verifikasi bila field identitas berubah; audit trail |
| **Hak hapus** (Ps. 8) | `/api/v1/users/my-data` | DELETE | **30 hari** | **Constraint:** tidak dapat dihapus bila ada pinjaman AKTIF/belum lunas; data wajib regulasi di-*retain* (soft-delete/anonimisasi parsial), bukan hard-delete |
| **Hak portabilitas** (Ps. 9) | `/api/v1/users/export-data` | GET | 14 hari | Ekspor data dalam format terstruktur **JSON/CSV** yang dapat dibaca mesin |
| **Hak keberatan** (Ps. 10) | `/api/v1/users/objection` | POST | 14 hari | Mengajukan keberatan atas pemrosesan/profiling otomatis; eskalasi ke DPO untuk peninjauan |

### Contoh: DELETE dengan constraint
```json
// Response 409 bila ada pinjaman aktif
{
  "success": false,
  "code": "DELETION_BLOCKED_ACTIVE_LOAN",
  "message": "Data tidak dapat dihapus karena terdapat pinjaman aktif",
  "details": ["Lunasi atau selesaikan pinjaman sebelum pengajuan penghapusan"],
  "timestamp": "2026-10-24T08:00:00.000Z"
}
```
Catatan: untuk kepatuhan OJK, data transaksi finansial tetap disimpan selama periode retensi wajib meski subjek meminta penghapusan — subjek diberitahu dasar hukumnya.

---

## 4. Technical Implementation Checklist

Legenda: ✅ Selesai · 🔄 Sedang berjalan · ❌ Belum

| Item | Status | Keterangan |
|---|---|---|
| Enkripsi at-rest — NIK | ✅ | AES-256-GCM + KMS |
| Enkripsi at-rest — NPWP | ✅ | AES-256-GCM + KMS |
| Enkripsi at-rest — nomor rekening | ✅ | AES-256-GCM + KMS |
| Enkripsi objek — foto KTP & selfie | ✅ | S3 SSE-KMS + signed URL |
| Enkripsi in-transit (TLS 1.3) | ✅ | ALB/Nginx enforce TLS 1.3, HSTS |
| Access control (RBAC + need-to-know) | ✅ | Spring Security, mask UI |
| Audit trail (who/what/when/where) | ✅ | audit_logs + hash chain + MDC traceId |
| Data retention policy | 🔄 | Kebijakan ditetapkan; otomasi penghapusan sebagian berjalan |
| Automated deletion/anonymization job | 🔄 | Scheduler untuk data kedaluwarsa dalam pengembangan |
| Anonymization untuk analytics | 🔄 | Pseudonimisasi di reporting; perlu validasi re-identifikasi |
| Backup encryption | ✅ | RDS snapshot terenkripsi KMS |
| Key rotation (KMS) | ✅ | Rotasi 90 hari |
| Penetration test schedule | 🔄 | Rencana kuartalan + sebelum rilis major; vendor TBD |
| Vulnerability scanning (SAST/DAST/Trivy) | ✅ | Terintegrasi CI/CD |
| Consent record storage | 🔄 | Penyimpanan consent + versioning dalam pengembangan |

---

## 5. Operational Checklist

### 5.1 Privacy Notice — Konten Wajib
- Identitas & kontak pengendali data (koperasi) + kontak DPO.
- Jenis data pribadi yang dikumpulkan (termasuk data khusus: NIK, NPWP, biometrik).
- Tujuan & dasar hukum pemrosesan.
- Penerima/pihak ketiga (Midtrans, Xendit, AWS) & transfer data.
- Periode retensi per kategori data.
- Hak subjek data & cara menggunakannya.
- Keberadaan pengambilan keputusan otomatis (AI scoring) & logikanya.
- Mekanisme pengaduan & kontak Lembaga PDP.

### 5.2 Consent Management
- **Kapan:** persetujuan eksplisit diminta saat registrasi & sebelum pengumpulan data khusus (opt-in, bukan pre-checked).
- **Bagaimana:** consent tercatat (user_id, versi notice, timestamp, IP) dan dapat ditarik.
- **Re-consent:** diminta ulang saat perubahan material tujuan/kebijakan atau pemrosesan baru (mis. fitur analytics baru).
- Consent granular memisahkan pemrosesan wajib (kontrak/kepatuhan) dari opsional (marketing).

### 5.3 Data Breach Notification (Pasal 46)
Prosedur **72 jam**:
1. **Deteksi & triase** (IDS/alert) → aktivasi tim IR, containment.
2. **Investigasi** ≤24 jam: lingkup data, subjek terdampak, root cause.
3. **Notifikasi ≤72 jam** ke **Lembaga PDP** (c.q. instansi berwenang, koordinasi BSSN untuk insiden siber) dan **subjek data** terdampak, memuat: jenis data, dampak, langkah mitigasi.
4. **Dokumentasi & remediasi:** laporan insiden, bukti forensik, perbaikan kontrol, post-mortem.

### 5.4 DPO (Data Protection Officer)
- Ditunjuk resmi; kontak dipublikasikan di Privacy Notice.
- Tanggung jawab: mengawasi kepatuhan UU PDP, menangani permintaan hak subjek data, titik kontak Lembaga PDP, memimpin DPIA & respons breach, edukasi internal.

### 5.5 Third-Party Data Processing Agreements (DPA)
- DPA tertulis dengan **Midtrans, Xendit, AWS** mencakup: tujuan & batasan pemrosesan, kewajiban keamanan, sub-prosesor, lokasi data, bantuan hak subjek data, notifikasi breach, dan audit.

### 5.6 Employee Data Access Training
- Pelatihan wajib UU PDP & keamanan data saat onboarding + refresh tahunan.
- Kebijakan clean desk, phishing awareness, penanganan data sensitif, prosedur insiden.
- Akses data sensitif berdasarkan peran & dicatat; review akses berkala.

---

## 6. Third-Party Assessment

| Vendor | Data yang Dibagikan | DPA Status | Lokasi Server | Catatan |
|---|---|---|---|---|
| **Midtrans** | Nama, nomor rekening/VA, jumlah transaksi, order ID, email/phone (minimal untuk payment) | 🔄 Perlu ditandatangani/diperbarui | Indonesia (penyelenggara lokal, berlisensi BI) | Verifikasi signature webhook; data kartu tidak disimpan platform (PCI-DSS di sisi Midtrans) |
| **Xendit** | Nama, rekening tujuan disbursement, jumlah, external ID | 🔄 Perlu ditandatangani/diperbarui | Indonesia / regional | Callback token diverifikasi; minimisasi data dikirim |
| **AWS** | Seluruh data terenkripsi (hosting RDS/S3/ElastiCache/MSK) | ✅ AWS DPA (standar) berlaku | **ap-southeast-1 (Singapore)** / preferensi **ap-southeast-3 (Jakarta)** | SOC 2 Type II, ISO 27001; evaluasi lokalisasi data ke region Jakarta untuk minimisasi transfer lintas batas |

> Transfer data lintas batas (bila region Singapore) wajib memenuhi ketentuan UU PDP (tingkat perlindungan setara / safeguard kontraktual). Rekomendasi: prioritaskan region Jakarta.

---

## 7. Gap Analysis Template

| Requirement | Status | Implementasi Saat Ini | Action Required | PIC | Due Date |
|---|---|---|---|---|---|
| Enkripsi data khusus at-rest | Met | AES-256-GCM + KMS untuk NIK/NPWP/rekening | Audit cakupan enkripsi berkala | Security Lead | 2026-11-15 |
| TLS 1.3 in-transit | Met | ALB/Nginx enforce TLS 1.3 + HSTS | Monitoring cipher berkala | DevOps | 2026-11-30 |
| Automated deletion saat retensi berakhir | Partial | Kebijakan ada, otomasi sebagian | Bangun scheduler penghapusan/anonimisasi penuh | Backend Lead | 2026-12-31 |
| Consent management & versioning | Partial | Consent dasar tercatat | Implementasi consent granular + re-consent flow | Product + Backend | 2026-12-15 |
| Hak subjek data (API lengkap) | Partial | GET/PATCH tersedia | Lengkapi DELETE/export/objection + SLA tracking | Backend Lead | 2026-12-20 |
| Anonymization untuk analytics | Partial | Pseudonimisasi reporting | Uji risiko re-identifikasi + kebijakan k-anonymity | Data/ML Lead | 2026-12-31 |
| DPA pihak ketiga (Midtrans/Xendit) | Gap | Belum final | Finalisasi & tandatangani DPA | Legal + DPO | 2026-11-30 |
| Penetration test | Partial | Rencana kuartalan | Jadwalkan & kontrak vendor pentest | Security Lead | 2026-11-30 |
| DPO appointment resmi | Gap | Fungsi berjalan informal | Penunjukan resmi + publikasi kontak | Management | 2026-11-10 |
| Breach notification runbook (72 jam) | Partial | Prosedur terdokumentasi | Latihan tabletop + integrasi alert BSSN | Security + DPO | 2026-12-10 |
| ROPA (Record of Processing Activities) | Gap | Belum lengkap | Susun ROPA menyeluruh | DPO | 2026-12-20 |
| DPIA untuk AI credit scoring | Gap | Belum dilakukan | Lakukan DPIA profiling otomatis | DPO + ML Lead | 2026-12-15 |
| Data localization (region Jakarta) | Partial | Region Singapore | Evaluasi migrasi ke ap-southeast-3 | DevOps + Legal | 2027-01-31 |

> **Status:** Met = terpenuhi · Partial = sebagian · Gap = belum ada. Review gap analysis setiap kuartal bersama DPO & Security.
