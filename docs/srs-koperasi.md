# Software Requirements Specification (SRS)
## Platform Koperasi Digital / P2P Lending untuk UMKM Indonesia

| Atribut | Nilai |
|---|---|
| Dokumen | Software Requirements Specification |
| Standar | ISO/IEC/IEEE 29148:2018 |
| Versi | 1.0 |
| Status | Baseline |
| Tanggal | 2026-10-24 |
| Klasifikasi | Internal / Confidential |

---

## 1. Introduction

### 1.1 Purpose
Dokumen Software Requirements Specification (SRS) ini mendefinisikan kebutuhan fungsional dan non-fungsional dari **Platform Koperasi Digital / P2P Lending untuk UMKM Indonesia** (selanjutnya disebut "Sistem" atau "Platform"). Dokumen disusun mengikuti standar **ISO/IEC/IEEE 29148:2018**.

Tujuan dokumen:
- Menjadi acuan tunggal (*single source of truth*) bagi pemangku kepentingan: tim pengembang, QA, arsitek, *product owner*, tim keamanan, auditor OJK, dan Dewan Pengawas Koperasi.
- Menjadi basis untuk desain, implementasi, pengujian, dan verifikasi.
- Mendukung ketertelusuran (*traceability*) antara kebutuhan bisnis, kebutuhan fungsional, dan pengujian.

**Audiens:** Backend Engineer, Frontend Engineer, ML Engineer, DevOps/SRE, QA Engineer, Security Officer, Compliance Officer, Product Manager, Auditor.

### 1.2 Scope
Sistem adalah platform koperasi digital yang mempertemukan **peminjam (borrower)** UMKM dengan **investor** melalui mekanisme *peer-to-peer lending*, dilengkapi *AI credit scoring* untuk menilai kelayakan kredit UMKM yang 60% di antaranya ditolak bank karena tidak memiliki *credit history* formal.

**Dalam cakupan (In Scope):**
- Registrasi, autentikasi, dan otorisasi berbasis peran (RBAC).
- Manajemen profil peminjam dan verifikasi identitas (KTP/NIK).
- Pengajuan pinjaman dan siklus hidup pinjaman (*loan lifecycle*).
- *AI credit scoring* menggunakan TensorFlow/Keras melalui layanan FastAPI.
- Review dan persetujuan pinjaman dengan kemampuan *override* skor AI.
- Pencairan (*disbursement*) melalui *payment gateway* (Midtrans/Xendit).
- Pembayaran cicilan dan rekonsiliasi.
- Manajemen portfolio investor.
- Notifikasi *real-time* (WebSocket + SSE).
- Laporan dan dashboard admin.

**Di luar cakupan (Out of Scope) untuk rilis ini:**
- Pasar sekunder (jual-beli porsi pinjaman antar investor).
- Produk asuransi kredit pihak ketiga.
- Aplikasi mobile native (hanya *responsive web*).

**Manfaat:** inklusi keuangan UMKM, penilaian kredit alternatif berbasis data, transparansi bagi investor, dan kepatuhan terhadap regulasi Indonesia (UU PDP & OJK).

### 1.3 Definitions, Acronyms, Abbreviations

| Istilah | Definisi |
|---|---|
| **AES-256-GCM** | Algoritma enkripsi simetris 256-bit mode Galois/Counter untuk data *at-rest*. |
| **BCrypt** | Fungsi *hashing* kata sandi adaptif. |
| **Borrower** | Peminjam, pelaku UMKM yang mengajukan pinjaman. |
| **CSRF** | *Cross-Site Request Forgery*. |
| **Disbursement** | Pencairan dana pinjaman ke rekening peminjam. |
| **Dukcapil** | Direktorat Jenderal Kependudukan dan Pencatatan Sipil. |
| **Idempotency Key** | Kunci unik untuk menjamin operasi tidak dieksekusi ganda. |
| **Investor** | Pemberi dana yang berinvestasi pada pinjaman. |
| **JWT** | *JSON Web Token*. |
| **KTP** | Kartu Tanda Penduduk. |
| **NIK** | Nomor Induk Kependudukan. |
| **NPWP** | Nomor Pokok Wajib Pajak. |
| **OJK** | Otoritas Jasa Keuangan. |
| **OWASP** | *Open Worldwide Application Security Project*. |
| **p95** | Persentil ke-95 dari distribusi waktu respons. |
| **P2P Lending** | *Peer-to-peer lending*. |
| **RBAC** | *Role-Based Access Control*. |
| **Reviewer** | Petugas yang me-review dan menyetujui/menolak aplikasi pinjaman. |
| **SLA** | *Service Level Agreement*. |
| **SSE** | *Server-Sent Events*. |
| **TLS** | *Transport Layer Security*. |
| **UMKM** | Usaha Mikro, Kecil, dan Menengah. |
| **UU PDP** | Undang-Undang Pelindungan Data Pribadi (UU No. 27 Tahun 2022). |

### 1.4 References
1. ISO/IEC/IEEE 29148:2018 — *Systems and software engineering — Life cycle processes — Requirements engineering*.
2. Undang-Undang Republik Indonesia No. 27 Tahun 2022 tentang Pelindungan Data Pribadi.
3. POJK No. 10/POJK.05/2022 tentang Layanan Pendanaan Bersama Berbasis Teknologi Informasi (LPBBTI).
4. OWASP Top 10 (2021) & OWASP ASVS 4.0.
5. RFC 7519 — *JSON Web Token (JWT)*; RFC 6749 — *OAuth 2.0*.
6. RFC 8446 — *TLS 1.3*.
7. Dokumentasi Midtrans Core API & Xendit API.
8. Dokumentasi TensorFlow Serving.

### 1.5 Overview
Bagian 2 menjelaskan deskripsi umum sistem. Bagian 3 merinci fitur sistem (SRS-F-001 s/d SRS-F-010). Bagian 4 memuat kebutuhan antarmuka eksternal. Bagian 5 memuat kebutuhan non-fungsional. Bagian 6 menyajikan *state machine* siklus pinjaman. Bagian 7 memuat klasifikasi data menurut UU PDP. Bagian 8 adalah lampiran dan matriks ketertelusuran.

---

## 2. Overall Description

### 2.1 Product Perspective
Sistem merupakan platform baru (*greenfield*) dengan arsitektur **modular monolith + layanan pendukung**. Empat service backend: `auth-service` (identity), `loan-service` (core: pinjaman, notifikasi, reporting), `payment-service`, dan `scoring-service` (FastAPI + TensorFlow). Frontend Next.js/React, *payment gateway* eksternal, serta infrastruktur PostgreSQL, Redis, dan Kafka (KRaft). Keputusan konsolidasi tercatat di ADR-0001.

```mermaid
flowchart LR
    subgraph Client
        W[Web App Next.js/React 19]
    end
    subgraph Edge
        NG[Nginx: TLS + routing]
    end
    subgraph Backend
        API[Spring Boot Services]
        WS[WebSocket/SSE Gateway]
    end
    subgraph ML
        FA[FastAPI Scoring Service]
        TF[TensorFlow Serving]
    end
    subgraph Data
        PG[(PostgreSQL)]
        RD[(Redis)]
        KF[[Kafka (KRaft)]]
    end
    subgraph External
        MT[Midtrans]
        XD[Xendit]
        DK[Dukcapil/KYC]
    end
    W <--> NG --> API
    NG <--> WS
    API --> PG
    API --> RD
    API <--> KF
    API <--> MQ
    API --> FA --> TF
    API <--> MT
    API <--> XD
    API --> DK
    API --> WS
```

### 2.2 Product Functions
Ringkasan fungsi utama:
- Registrasi, autentikasi JWT, dan otorisasi RBAC 4 peran.
- Pengelolaan profil & verifikasi KTP peminjam.
- Pengajuan pinjaman dan orkestrasi siklus hidup.
- Penilaian kredit otomatis berbasis AI.
- Review manual & *override* skor.
- Pencairan & pembayaran melalui *payment gateway*.
- Manajemen portfolio investor.
- Notifikasi *real-time* & pelaporan.

### 2.3 User Classes and Characteristics

| Kelas Pengguna | Deskripsi | Karakteristik | Hak Akses Utama |
|---|---|---|---|
| **BORROWER** | Pelaku UMKM peminjam | Literasi digital menengah, akses via web/mobile browser | Daftar, lengkapi profil, apply pinjaman, bayar cicilan, lihat status |
| **INVESTOR** | Pemberi dana | Literasi finansial menengah-tinggi | Top-up dana, investasi ke pinjaman, lihat portfolio & imbal hasil |
| **REVIEWER** | Analis kredit koperasi | Terlatih dalam penilaian risiko | Review aplikasi, approve/reject, override AI score |
| **ADMIN** | Administrator sistem | Teknis, kewenangan penuh | Kelola user, konfigurasi sistem, laporan keuangan, audit |

### 2.4 Operating Environment
- **Server:** Linux container (Docker) diorkestrasi Kubernetes di AWS; Nginx sebagai *reverse proxy*.
- **Runtime:** Java 21 (Spring Boot 3.x), Python 3.11+ (FastAPI), Node.js (Next.js).
- **Data store:** PostgreSQL (database utama & satu-satunya untuk data persisten), Redis (cache/session), Kafka (event streaming, termasuk retry payment).
- **Client:** Browser modern (Chrome, Firefox, Safari, Edge versi terbaru) dengan dukungan ES2020+, WebSocket, dan SSE.

### 2.5 Design and Implementation Constraints
- **DB-C-1:** PostgreSQL adalah satu-satunya basis data relasional persisten.
- **DB-C-2:** *Optimistic locking* (kolom `version`) wajib pada entitas transaksional.
- **SEC-C-1:** Data pribadi khusus wajib dienkripsi AES-256-GCM *at-rest*.
- **SEC-C-2:** Seluruh komunikasi eksternal wajib TLS 1.3.
- **ARCH-C-1:** Operasi pembayaran/pencairan wajib *idempotent* menggunakan *idempotency key*.
- **REG-C-1:** Wajib patuh UU PDP dan ketentuan OJK (LPBBTI).
- **TECH-C-1:** Frontend menggunakan React 19 + JavaScript (bukan TypeScript), Next.js, Tailwind CSS, TanStack Query, Zustand.

### 2.6 Assumptions and Dependencies
- **A-1:** Peminjam memiliki NIK/KTP valid yang dapat diverifikasi.
- **A-2:** *Payment gateway* Midtrans/Xendit tersedia dan mendukung *disbursement* & *webhook*.
- **A-3:** Model ML tersedia dan di-*serve* via TensorFlow Serving.
- **D-1:** Ketersediaan API verifikasi identitas (Dukcapil/pihak KYC).
- **D-2:** Konektivitas dan kuota *payment gateway*.
- **D-3:** Infrastruktur AWS & Kubernetes beroperasi normal.

---

## 3. System Features

> Setiap kebutuhan fungsional memiliki ID `SRS-F-xxx-FR-yy`, *verifiable*, dan dapat diuji.

### SRS-F-001: Registrasi & Autentikasi
- **Description:** Pengguna dapat mendaftar, login, refresh token, dan logout dengan JWT serta RBAC.
- **Priority:** HIGH
- **Stimulus/Response Sequences:**
  - *Stimulus:* Pengguna submit email+password → *Response:* Sistem buat akun status `PENDING_VERIFICATION`, kirim notifikasi verifikasi.
  - *Stimulus:* Login dengan kredensial valid → *Response:* Sistem mengembalikan *access token* (JWT) + *refresh token*.
  - *Stimulus:* Login gagal 5x → *Response:* Sistem terapkan *rate limit*/lockout sementara.
- **Functional Requirements:**
  - **SRS-F-001-FR-01:** Sistem HARUS memvalidasi keunikan email saat registrasi.
  - **SRS-F-001-FR-02:** Sistem HARUS menyimpan kata sandi menggunakan BCrypt (cost ≥ 10).
  - **SRS-F-001-FR-03:** Sistem HARUS menerbitkan JWT *access token* (masa berlaku singkat) dan *refresh token* (disimpan sebagai *hash*).
  - **SRS-F-001-FR-04:** Sistem HARUS mendukung *refresh token rotation* dan *revocation*.
  - **SRS-F-001-FR-05:** Sistem HARUS menegakkan RBAC untuk setiap *endpoint*.
  - **SRS-F-001-FR-06:** Sistem HARUS menerapkan *rate limiting* pada endpoint autentikasi.

### SRS-F-002: Manajemen Profil Peminjam & Verifikasi KTP
- **Description:** Peminjam melengkapi profil, mengunggah KTP & selfie, dan menjalani verifikasi identitas.
- **Priority:** HIGH
- **Stimulus/Response Sequences:**
  - *Stimulus:* Peminjam submit NIK, NPWP, data diri, foto KTP, selfie → *Response:* Sistem enkripsi data sensitif, set `is_verified=false`, antrikan verifikasi.
  - *Stimulus:* Verifikasi KYC berhasil → *Response:* Sistem set `is_verified=true`, kirim notifikasi.
- **Functional Requirements:**
  - **SRS-F-002-FR-01:** Sistem HARUS mengenkripsi NIK, NPWP, dan nomor rekening dengan AES-256-GCM sebelum disimpan.
  - **SRS-F-002-FR-02:** Sistem HARUS memvalidasi format NIK (16 digit) dan NPWP.
  - **SRS-F-002-FR-03:** Sistem HARUS menyimpan URL foto KTP & selfie pada penyimpanan objek terenkripsi dengan akses terbatas.
  - **SRS-F-002-FR-04:** Sistem HARUS mencegah pengajuan pinjaman bila `is_verified=false`.
  - **SRS-F-002-FR-05:** Sistem HARUS mencatat *audit log* setiap perubahan data profil.

### SRS-F-003: Pengajuan Pinjaman
- **Description:** Peminjam terverifikasi mengajukan pinjaman terhadap produk pinjaman tertentu.
- **Priority:** HIGH
- **Stimulus/Response Sequences:**
  - *Stimulus:* Peminjam pilih produk, jumlah, tenor, tujuan → *Response:* Sistem validasi batas produk, buat aplikasi status `PENDING`, trigger `SCORING`.
- **Functional Requirements:**
  - **SRS-F-003-FR-01:** Sistem HARUS memvalidasi `amount` dalam rentang `min_amount`–`max_amount` produk.
  - **SRS-F-003-FR-02:** Sistem HARUS memvalidasi `tenor_months` dalam rentang produk.
  - **SRS-F-003-FR-03:** Sistem HARUS menolak pengajuan ganda melalui `idempotency_key` unik.
  - **SRS-F-003-FR-04:** Sistem HARUS menginisialisasi status aplikasi ke `PENDING` lalu transisi ke `SCORING`.
  - **SRS-F-003-FR-05:** Sistem HARUS menggunakan *optimistic locking* (`version`) pada pembaruan aplikasi.

### SRS-F-004: AI Credit Scoring
- **Description:** Sistem menilai kelayakan kredit peminjam via model TensorFlow/Keras melalui FastAPI.
- **Priority:** HIGH
- **Stimulus/Response Sequences:**
  - *Stimulus:* Aplikasi masuk status `SCORING` → *Response:* Sistem kirim fitur ke layanan ML, terima `score`, `risk_category`, `approval_recommendation`, simpan, transisi ke `UNDER_REVIEW`.
- **Functional Requirements:**
  - **SRS-F-004-FR-01:** Sistem HARUS mengumpulkan fitur dari profil & riwayat lalu mengirim ke layanan scoring.
  - **SRS-F-004-FR-02:** Sistem HARUS menyimpan `score`, `risk_category` (LOW/MEDIUM/HIGH), `approval_recommendation` (APPROVE/REVIEW/REJECT), `features_json`, `model_version`, `processing_time_ms`.
  - **SRS-F-004-FR-03:** Sistem HARUS menangani timeout/kegagalan layanan ML dengan *retry* dan *fallback* ke `UNDER_REVIEW` manual.
  - **SRS-F-004-FR-04:** Sistem HARUS menyimpan versi model untuk auditabilitas.
  - **SRS-F-004-FR-05:** Keputusan AI HARUS dapat dijelaskan (*explainability*) melalui `features_json`.

### SRS-F-005: Review & Approval Pinjaman
- **Description:** Reviewer meninjau aplikasi & rekomendasi AI, lalu approve/reject dengan kemampuan *override*.
- **Priority:** HIGH
- **Stimulus/Response Sequences:**
  - *Stimulus:* Reviewer membuka aplikasi `UNDER_REVIEW` → *Response:* Sistem tampilkan detail + skor AI.
  - *Stimulus:* Reviewer approve → *Response:* status `APPROVED`, `reviewed_at` terisi, trigger disbursement.
  - *Stimulus:* Reviewer reject → *Response:* status `REJECTED`, `rejection_reason` wajib terisi.
- **Functional Requirements:**
  - **SRS-F-005-FR-01:** Hanya peran REVIEWER/ADMIN yang BOLEH approve/reject.
  - **SRS-F-005-FR-02:** Sistem HARUS mewajibkan `rejection_reason` saat reject.
  - **SRS-F-005-FR-03:** Sistem HARUS mencatat *override* skor AI pada *audit log* dengan identitas reviewer.
  - **SRS-F-005-FR-04:** Sistem HARUS menetapkan `reviewer_id` dan `reviewed_at`.
  - **SRS-F-005-FR-05:** Transisi status HARUS mematuhi *state machine* (Bagian 6).

### SRS-F-006: Disbursement via Payment Gateway
- **Description:** Pencairan dana ke rekening peminjam melalui Midtrans/Xendit setelah `APPROVED`.
- **Priority:** HIGH
- **Stimulus/Response Sequences:**
  - *Stimulus:* Aplikasi `APPROVED` → *Response:* Sistem buat *disbursement* `PENDING`, panggil gateway, update `PROCESSING`→`COMPLETED`, transisi aplikasi ke `DISBURSED`→`ACTIVE`, generate jadwal cicilan.
- **Functional Requirements:**
  - **SRS-F-006-FR-01:** Sistem HARUS menggunakan `idempotency_key` unik untuk setiap pencairan.
  - **SRS-F-006-FR-02:** Sistem HARUS menyimpan `external_transaction_id` & `payment_gateway`.
  - **SRS-F-006-FR-03:** Sistem HARUS memproses *webhook* status gateway dengan verifikasi tanda tangan.
  - **SRS-F-006-FR-04:** Pada kegagalan, sistem HARUS men-set status `FAILED` dan memberi notifikasi tanpa mengubah dana ganda.
  - **SRS-F-006-FR-05:** Sistem HARUS menghasilkan jadwal cicilan setelah pencairan sukses.

### SRS-F-007: Pembayaran Cicilan
- **Description:** Peminjam membayar cicilan terjadwal melalui *payment gateway*.
- **Priority:** HIGH
- **Stimulus/Response Sequences:**
  - *Stimulus:* Peminjam bayar cicilan → *Response:* Sistem buat pembayaran `PENDING`, verifikasi via webhook, set cicilan `PAID`.
  - *Stimulus:* Jatuh tempo terlewat → *Response:* Sistem set cicilan `OVERDUE`, kirim notifikasi.
- **Functional Requirements:**
  - **SRS-F-007-FR-01:** Sistem HARUS mencatat `principal` & `interest` tiap cicilan.
  - **SRS-F-007-FR-02:** Sistem HARUS menggunakan `idempotency_key` pada pembayaran.
  - **SRS-F-007-FR-03:** Sistem HARUS menyimpan `webhook_payload` untuk rekonsiliasi & audit.
  - **SRS-F-007-FR-04:** Sistem HARUS menandai aplikasi `COMPLETED` ketika seluruh cicilan `PAID`.
  - **SRS-F-007-FR-05:** Sistem HARUS menandai aplikasi `DEFAULT` sesuai aturan tunggakan.

### SRS-F-008: Manajemen Portfolio Investor
- **Description:** Investor melakukan top-up, investasi ke pinjaman, dan memantau portfolio.
- **Priority:** MEDIUM
- **Stimulus/Response Sequences:**
  - *Stimulus:* Investor top-up → *Response:* Saldo bertambah setelah pembayaran terkonfirmasi.
  - *Stimulus:* Investor investasi ke pinjaman → *Response:* Dana dialokasikan, portfolio diperbarui.
- **Functional Requirements:**
  - **SRS-F-008-FR-01:** Sistem HARUS menampilkan daftar pinjaman yang dapat didanai beserta profil risiko.
  - **SRS-F-008-FR-02:** Sistem HARUS mencatat alokasi dana investor per pinjaman.
  - **SRS-F-008-FR-03:** Sistem HARUS menghitung dan menampilkan imbal hasil & status portfolio.
  - **SRS-F-008-FR-04:** Sistem HARUS menjaga konsistensi saldo dengan transaksi atomik.

### SRS-F-009: Notifikasi Real-time (WebSocket + SSE)
- **Description:** Sistem mengirim notifikasi *real-time* perubahan status pinjaman, pembayaran, dan sistem.
- **Priority:** MEDIUM
- **Stimulus/Response Sequences:**
  - *Stimulus:* Perubahan status pinjaman/pembayaran → *Response:* Sistem publish event dan push ke klien via WebSocket/SSE serta simpan notifikasi.
- **Functional Requirements:**
  - **SRS-F-009-FR-01:** Sistem HARUS mendukung kanal WebSocket untuk notifikasi dua arah.
  - **SRS-F-009-FR-02:** Sistem HARUS menyediakan SSE sebagai *fallback* satu arah.
  - **SRS-F-009-FR-03:** Sistem HARUS menyimpan notifikasi dengan `type` (LOAN_STATUS/PAYMENT/SYSTEM) & `is_read`.
  - **SRS-F-009-FR-04:** Sistem HARUS mengautentikasi koneksi *real-time* dengan JWT.

### SRS-F-010: Laporan & Dashboard Admin
- **Description:** Admin mengakses dashboard operasional dan laporan keuangan.
- **Priority:** MEDIUM
- **Stimulus/Response Sequences:**
  - *Stimulus:* Admin membuka dashboard → *Response:* Sistem menampilkan metrik agregat (volume pinjaman, NPL, disbursement, kolektibilitas).
- **Functional Requirements:**
  - **SRS-F-010-FR-01:** Sistem HARUS menyajikan metrik pinjaman per status.
  - **SRS-F-010-FR-02:** Sistem HARUS menghasilkan laporan keuangan yang dapat diekspor.
  - **SRS-F-010-FR-03:** Sistem HARUS membatasi akses dashboard pada peran ADMIN.
  - **SRS-F-010-FR-04:** Sistem HARUS menyediakan akses *audit log* bagi ADMIN.

---

## 4. External Interface Requirements

### 4.1 User Interfaces
- Web responsif (Next.js/React 19 + Tailwind CSS) untuk BORROWER, INVESTOR, REVIEWER, ADMIN.
- Antarmuka wajib memenuhi aksesibilitas dasar (kontras, navigasi keyboard) dan dilokalisasi Bahasa Indonesia.
- Pesan galat jelas, tanpa membocorkan informasi sensitif.

### 4.2 Hardware Interfaces
- Tidak ada antarmuka perangkat keras khusus. Sistem berjalan pada infrastruktur cloud AWS berbasis container. Akses klien melalui perangkat standar dengan browser.

### 4.3 Software Interfaces
| Sistem | Antarmuka | Tujuan |
|---|---|---|
| **Midtrans** | REST API + Webhook | Disbursement & payment collection |
| **Xendit** | REST API + Webhook | Disbursement & payment collection (alternatif) |
| **Kafka** | Producer/Consumer | Event streaming (status pinjaman, scoring, notifikasi) |
| **Redis** | Client protocol | Cache, session, rate limiting |
| **TensorFlow Serving** | gRPC/REST (via FastAPI) | Inferensi *credit scoring* |
| **PostgreSQL** | JDBC | Persistensi data |

### 4.4 Communication Interfaces
- REST/HTTPS (TLS 1.3) untuk API klien–backend.
- WebSocket (WSS) & SSE untuk notifikasi *real-time*.
- Webhook masuk dari *payment gateway* dengan verifikasi tanda tangan.
- Kafka & AMQP untuk komunikasi antar-layanan internal.

---

## 5. Non-Functional Requirements

### 5.1 Performance
- **SRS-NFR-PERF-01:** Waktu respons API p95 < **200 ms** pada kondisi beban normal.
- **SRS-NFR-PERF-02:** Sistem HARUS mendukung **1000 pengguna konkuren** tanpa degradasi SLA.
- **SRS-NFR-PERF-03:** Inferensi *credit scoring* p95 < 2 detik.

### 5.2 Safety
- **SRS-NFR-SAFE-01:** Operasi finansial (disbursement/pembayaran) HARUS *idempotent* dan transaksional untuk mencegah dana ganda.
- **SRS-NFR-SAFE-02:** Sistem HARUS menyediakan *backup* harian & *point-in-time recovery* PostgreSQL.
- **SRS-NFR-SAFE-03:** Transisi status pinjaman HARUS tervalidasi terhadap *state machine*.

### 5.3 Security
- **SRS-NFR-SEC-01:** Autentikasi via JWT (RFC 7519) dengan masa berlaku singkat.
- **SRS-NFR-SEC-02:** Kata sandi di-*hash* dengan BCrypt.
- **SRS-NFR-SEC-03:** Data pribadi khusus dienkripsi AES-256-GCM *at-rest*.
- **SRS-NFR-SEC-04:** Seluruh transport wajib TLS 1.3.
- **SRS-NFR-SEC-05:** Sistem HARUS memitigasi OWASP Top 10 (injeksi, broken access control, XSS, CSRF, SSRF, dll).
- **SRS-NFR-SEC-06:** Otorisasi berbasis RBAC pada seluruh *endpoint* dan prinsip *least privilege*.
- **SRS-NFR-SEC-07:** Seluruh akses data sensitif & perubahan kritikal HARUS tercatat di *audit log*.

### 5.4 Availability
- **SRS-NFR-AVL-01:** SLA ketersediaan **99.9%** per bulan.
- **SRS-NFR-AVL-02:** Arsitektur HARUS *stateless* pada layer aplikasi untuk *horizontal scaling*.
- **SRS-NFR-AVL-03:** Sistem HARUS menerapkan *health check*, *graceful degradation*, dan *failover*.

### 5.5 Maintainability
- **SRS-NFR-MNT-01:** Kode HARUS modular (modul terpisah per domain di dalam core) dengan cakupan uji ≥ 80% pada modul kritikal.
- **SRS-NFR-MNT-02:** Observabilitas: *logging* terstruktur, metrik, dan *tracing* terdistribusi.
- **SRS-NFR-MNT-03:** CI/CD via GitHub Actions dengan *automated testing* & *linting*.

### 5.6 Portability
- **SRS-NFR-PRT-01:** Seluruh layanan HARUS *containerized* (Docker) dan dapat dideploy di Kubernetes.
- **SRS-NFR-PRT-02:** Konfigurasi HARUS dieksternalisasi (env/secret) tanpa *hardcode*.

### 5.7 Compliance
- **SRS-NFR-CMP-01:** Sistem HARUS patuh UU PDP (UU No. 27/2022): persetujuan, minimisasi data, hak subjek data, retensi.
- **SRS-NFR-CMP-02:** Sistem HARUS patuh ketentuan OJK (POJK LPBBTI): pelaporan, keterbukaan, perlindungan konsumen.
- **SRS-NFR-CMP-03:** Jejak audit HARUS disimpan sesuai periode retensi regulasi.

---

## 6. Loan Lifecycle State Machine

```mermaid
stateDiagram-v2
    [*] --> PENDING : Aplikasi dibuat (SRS-F-003)
    PENDING --> SCORING : Trigger AI scoring
    SCORING --> UNDER_REVIEW : Skor tersedia (SRS-F-004)
    SCORING --> UNDER_REVIEW : Fallback (ML timeout/gagal)
    UNDER_REVIEW --> APPROVED : Reviewer approve (SRS-F-005)
    UNDER_REVIEW --> REJECTED : Reviewer reject (+reason)
    APPROVED --> DISBURSED : Pencairan sukses (SRS-F-006)
    DISBURSED --> ACTIVE : Jadwal cicilan dibuat
    ACTIVE --> COMPLETED : Seluruh cicilan PAID (SRS-F-007)
    ACTIVE --> DEFAULT : Tunggakan melewati ambang batas
    REJECTED --> [*]
    COMPLETED --> [*]
    DEFAULT --> [*]

    note right of SCORING
        AI credit scoring via FastAPI + TensorFlow Serving
    end note
    note right of UNDER_REVIEW
        Reviewer dapat override skor AI
    end note
```

---

## 7. Data Classification per UU PDP

Sesuai UU No. 27 Tahun 2022, data pribadi dibedakan menjadi **data pribadi umum** dan **data pribadi yang bersifat spesifik** (khusus). Data spesifik wajib perlindungan lebih ketat (enkripsi, akses terbatas, dasar hukum pemrosesan).

| Kategori | Jenis Data | Contoh Field | Penanganan |
|---|---|---|---|
| **Data Pribadi Spesifik** | Data keuangan pribadi, data identitas sensitif, data biometrik/foto | `nik_encrypted`, `npwp_encrypted`, `bank_account_encrypted`, `ktp_photo_url`, `selfie_photo_url`, `income_monthly` | Enkripsi AES-256-GCM *at-rest*; akses *least privilege*; *masking* di UI; audit penuh |
| **Data Pribadi Umum** | Identitas umum & kontak | `full_name`, `email`, `phone`, `address`, `city`, `province`, `date_of_birth`, `gender` | Akses berbasis peran; TLS 1.3 *in-transit*; audit perubahan |
| **Data Non-Pribadi / Operasional** | Data transaksi & sistem | `loan_applications.amount`, `credit_scores.score`, `payments.status`, `audit_logs` | Kontrol akses standar; retensi sesuai OJK |

**Prinsip yang diterapkan:**
- **Persetujuan & dasar hukum:** pemrosesan data memerlukan persetujuan eksplisit subjek data.
- **Minimisasi data:** hanya data yang diperlukan untuk penilaian kredit yang dikumpulkan.
- **Hak subjek data:** akses, koreksi, dan penghapusan (dengan pengecualian kewajiban regulasi).
- **Retensi & penghapusan:** data dihapus/anonimisasi setelah periode retensi regulasi berakhir.
- **Keamanan:** enkripsi, kontrol akses, dan *audit trail* wajib untuk data spesifik.

---

## 8. Appendix

### 8.1 Traceability Matrix (ringkas)

| Fitur | Functional Req. | NFR Terkait | Verifikasi |
|---|---|---|---|
| SRS-F-001 | FR-01..06 | SEC-01,02,06 | Unit/Integration test, pentest |
| SRS-F-002 | FR-01..05 | SEC-03,07; CMP-01 | Integration test, audit |
| SRS-F-003 | FR-01..05 | SAFE-03 | Integration test |
| SRS-F-004 | FR-01..05 | PERF-03 | Model test, load test |
| SRS-F-005 | FR-01..05 | SEC-06,07 | Integration test, audit |
| SRS-F-006 | FR-01..05 | SAFE-01 | E2E test, reconciliation |
| SRS-F-007 | FR-01..05 | SAFE-01 | E2E test, reconciliation |
| SRS-F-008 | FR-01..04 | SAFE-01 | Integration test |
| SRS-F-009 | FR-01..04 | PERF-01; AVL-02 | Integration test |
| SRS-F-010 | FR-01..04 | SEC-06; CMP-02,03 | Integration test |

### 8.2 Loan Status Enum
`PENDING`, `SCORING`, `UNDER_REVIEW`, `APPROVED`, `REJECTED`, `DISBURSED`, `ACTIVE`, `COMPLETED`, `DEFAULT`.

### 8.3 Role Enum
`BORROWER`, `INVESTOR`, `REVIEWER`, `ADMIN`.

### 8.4 Revision History
| Versi | Tanggal | Deskripsi | Penulis |
|---|---|---|---|
| 1.0 | 2026-10-24 | Baseline awal sesuai ISO/IEC/IEEE 29148:2018 | Architecture Team |
| 1.1 | 2026-10-27 | Penyesuaian arsitektur 4 service (ADR-0001); requirement fungsional tidak berubah | Architecture Team |

### 8.5 Related Artifacts
- ERD MVP: `docs/database/erd-mvp.mermaid`
