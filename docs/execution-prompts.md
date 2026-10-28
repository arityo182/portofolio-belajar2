<!-- FILE: docs/execution-prompts.md -->

> **Arsip:** dokumen ini adalah rencana pembangunan historis 7 service.
> Setelah ADR-0001, sebagian langkah tidak lagi relevan. Lihat [ADR-0001](architecture/adr/0001-consolidate-to-four-services.md).

# Execution Prompts — Koperasi Digital P2P Lending
## 11 Prompt Eksekusi (Fase 0–10) siap copy-paste ke DeepSeek Flash 4.1

> Setiap prompt self-contained. Salin satu blok fase ke DeepSeek Flash 4.1, jalankan sampai selesai, lalu lanjut ke fase berikutnya.

============================================================

## 🎯 FASE 0 — Project Setup & Scaffolding

### PROJECT CONTEXT
Platform Koperasi Digital / P2P Lending untuk UMKM Indonesia. 60% UMKM ditolak bank karena tak punya credit history formal; solusinya koperasi digital + AI credit scoring + payment gateway. Arsitektur microservices: 7 Spring Boot service (api-gateway, auth, loan, payment, notification, reporting) + scoring-service (FastAPI/TensorFlow) + frontend (Next.js). Roles: BORROWER, INVESTOR, REVIEWER, ADMIN. Loan lifecycle: PENDING→SCORING→UNDER_REVIEW→APPROVED/REJECTED→DISBURSED→ACTIVE→COMPLETED/DEFAULT. Infra: PostgreSQL (schema per service), Redis, Kafka+ZooKeeper, RabbitMQ, Nginx, Prometheus, Grafana. Payment: Midtrans, Xendit. Security: JWT, BCrypt, AES-256-GCM, TLS 1.3, UU PDP.

### STACK FASE INI
Gradle multi-project (Java 21, Spring Boot 3.x BOM), Python 3.11 (FastAPI), Next.js (React 19 + JavaScript), Docker, Docker Compose, Nginx, PostgreSQL 15, Redis 7, Kafka, ZooKeeper, RabbitMQ 3, Prometheus, Grafana.

### TASK
Buat struktur & scaffolding lengkap:
- Struktur folder: `services/{api-gateway,auth-service,loan-service,payment-service,notification-service,reporting-service}` (Spring Boot), `services/scoring-service` (Python), `frontend/` (Next.js).
- `settings.gradle` (rootProject `koperasi` + include 6 subproject Spring Boot).
- Root `build.gradle` (shared: Java 21 toolchain, Spring Boot plugin + dependency-management BOM, plugin umum, repositori).
- `services/{service}/build.gradle` per service (dependency spesifik: web, data-jpa, security, redis, kafka, validation, actuator, flyway, postgresql, testcontainers; rabbitmq untuk payment & notification; websocket untuk notification).
- `Dockerfile` per Spring Boot service (multi-stage: builder gradle + runtime `eclipse-temurin:21-jre` slim). `Dockerfile` scoring-service (`python:3.11-slim` + tensorflow + fastapi).
- `docker-compose.yml` LENGKAP: 8 service aplikasi + PostgreSQL 15 (+init-db.sql) + Redis 7 + Kafka + ZooKeeper + RabbitMQ (UI 15672) + Nginx (80/443) + Prometheus (9090) + Grafana (3001). Network `koperasi-network`, volume `postgres_data`,`redis_data`, healthcheck + depends_on.
- `.env.example` (semua variable: DB, Redis, Kafka, RabbitMQ, JWT_SECRET, MIDTRANS_*, XENDIT_*, ML paths, CORS, logging).
- `nginx/nginx.conf` reverse proxy ke semua service (`/api/*`→gateway, `/ws/*`→notification, `/`→frontend).
- `init-db.sql` (CREATE SCHEMA auth_schema, loan_schema, scoring_schema, payment_schema, notification_schema, reporting_schema, investor_schema + user per schema).
- `.gitignore`, `.editorconfig`.
- Root `README.md` dengan section: judul `# Koperasi Digital — P2P Lending Platform`; badge placeholder `[Build] [Coverage] [Docker] [License]`; **Problem Statement** (2 paragraf: masalah UMKM + solusi platform); **Tech Stack** (tabel Layer→Technology); **Architecture Overview** (link ke `docs/architecture/system-architecture.md`); **Microservices** (tabel Service→Port→Responsibility); **Project Structure** (tree folder root); **Docs Navigation** (tabel Dokumen→Path→Deskripsi untuk 19 dokumen docs/); **Quick Start — Coming Soon**; **Contributing** (link `docs/development/gitflow.md` + `docs/development/coding-conventions.md`); **License**.
- `docs/README.md` (navigasi docs/: tabel lengkap semua dokumen + link relatif + deskripsi 1 baris).

### ACCEPTANCE CRITERIA
- `./gradlew projects` menampilkan root + 6 subproject.
- `docker compose config` valid tanpa error.
- Semua Dockerfile build-able (multi-stage).
- `init-db.sql` membuat 7 schema.
- README.md & docs/README.md lengkap dengan semua section & tabel navigasi.

### INSTRUKSI EKSEKUSI
Tulis semua kode lengkap dan production-ready. Jangan beri penjelasan. Jangan tanya konfirmasi. Mulai langsung dari file pertama.

============================================================

## 🎯 FASE 1 — Auth Service

### PROJECT CONTEXT
Koperasi Digital P2P Lending, microservices. auth-service menangani registrasi, login, RBAC (BORROWER/INVESTOR/REVIEWER/ADMIN), JWT (access 15 menit, refresh 7 hari), BCrypt cost 12. Redis untuk token blacklist & refresh token store. Kafka untuk audit event. PostgreSQL schema `auth_schema`. Response wrapper `{success,data,message,timestamp,traceId}`; error `{success:false,code,message,details,timestamp}`. Base path `/api/v1/auth`.

### STACK FASE INI
Java 21, Spring Boot 3.x, Spring Security, Spring Data JPA, Spring Data Redis, Spring Kafka, Flyway, PostgreSQL, Gradle, JUnit 5, Mockito, TestContainers.

### TASK
Buat auth-service lengkap di `services/auth-service` (paket `com.koperasi.auth.*`):
- Entity: `User`(id UUID, email unik, password_hash, phone, status enum ACTIVE/SUSPENDED/PENDING_VERIFICATION, timestamps), `Role`, `UserRole`(PK komposit), `RefreshToken`(token_hash, device_info, expires_at, revoked_at).
- Flyway `V1__create_auth_tables.sql` (schema auth_schema, semua tabel + index + seed role).
- JWT util: generate access/refresh, validate, extract claims, sertakan `jti`.
- Spring Security: `SecurityFilterChain`, JWT auth filter, `BCryptPasswordEncoder(12)`, stateless.
- Repository: `UserRepository`, `RoleRepository`, `RefreshTokenRepository`.
- Service: `AuthService` (register, login, refresh dengan rotation + reuse detection, logout, getUserById).
- Controller: `AuthController` → `POST /register, /login, /refresh, /logout`, `GET /me`.
- DTO (record + Bean Validation): `RegisterRequest, LoginRequest, LoginResponse, RefreshRequest, UserResponse`.
- Exception: `UserAlreadyExistsException, InvalidCredentialsException, TokenExpiredException` + hierarki `KoperasiException→BusinessException`.
- Redis: blacklist `auth:blacklist:{jti}` (TTL=sisa exp), refresh store `auth:refresh:{userId}:{tokenId}`.
- Kafka producer: publish `audit.event.created` saat register/login/logout.
- Global `@RestControllerAdvice`, `ApiResponse<T>` wrapper, MDC traceId.
- `application.yml` (semua config `${ENV_VAR}`).
- Unit test `AuthServiceTest` (register success/duplicate, login success/wrong-password/not-found, refresh success/expired, logout blacklist).
- Integration test `AuthControllerIT` (@SpringBootTest + TestContainers PostgreSQL + Redis + MockMvc).

### ACCEPTANCE CRITERIA
- `POST /api/v1/auth/login` → 200 + access & refresh token.
- `POST /api/v1/auth/refresh` → access token baru (rotation).
- `POST /api/v1/auth/logout` → token masuk blacklist Redis.
- `GET /api/v1/auth/me` → profil user.
- Unit test coverage > 80%, integration test pass.

### INSTRUKSI EKSEKUSI
Tulis semua kode lengkap dan production-ready. Jangan beri penjelasan. Jangan tanya konfirmasi. Mulai langsung dari file pertama.

============================================================

## 🎯 FASE 2 — Loan Service

### PROJECT CONTEXT
Koperasi Digital P2P Lending. loan-service mengelola produk pinjaman, pengajuan, siklus hidup pinjaman, skor kredit (referensi), pencairan (referensi), jadwal cicilan. Lifecycle: PENDING→SCORING→UNDER_REVIEW→APPROVED/REJECTED→DISBURSED→ACTIVE→COMPLETED/DEFAULT. PostgreSQL `loan_schema`, Redis (distributed lock + idempotency), Kafka (event per transisi + consume scoring.completed). Optimistic locking via `@Version`. Base path `/api/v1/loans`.

### STACK FASE INI
Java 21, Spring Boot 3.x, Spring Data JPA, Spring Data Redis, Spring Kafka, Flyway, PostgreSQL, Gradle, JUnit 5, Mockito, TestContainers.

### TASK
Buat loan-service lengkap di `services/loan-service` (paket `com.koperasi.loan.*`):
- Entity: `LoanProduct`, `LoanApplication`(@Version, idempotency_key unik, reviewer_id nullable), `CreditScore`, `LoanDisbursement`, `LoanInstallment` (sesuai ERD).
- Flyway `V1__create_loan_tables.sql` (loan_schema + index + seed loan_products).
- State machine (custom `LoanStateMachine`): validasi semua transisi valid + tolak transisi invalid (throw `InvalidStateTransitionException`).
- Distributed lock (Redis SETNX via RedisTemplate, token + Lua release): cegah duplicate submit per borrower+idempotency.
- Idempotency: cek `idempotency_key` sebelum simpan aplikasi baru (return cached bila ada).
- Optimistic locking `@Version`: tangani concurrent status update (throw `ConcurrentModificationException`→409).
- Kafka producer: publish event di setiap transisi (`loan.application.created`, `loan.scoring.requested`, `loan.application.approved/rejected`, dst).
- Kafka consumer: `loan.scoring.completed` → update status ke UNDER_REVIEW + simpan CreditScore.
- Installment calculator (`InstallmentService`): hitung jadwal cicilan (flat rate + annuity).
- REST sesuai API contract: `GET /products`, `POST /applications`, `GET /applications`, `GET /applications/{id}`, `PATCH /applications/{id}/status`, `GET /applications/{id}/installments`, `GET /applications/{id}/score`.
- Service: `LoanService, LoanProductService, InstallmentService`. Global exception handler, `ApiResponse<T>`.
- `application.yml`.
- Unit test `LoanServiceTest` (state transitions, duplicate idempotency, installment calc), `LoanStateMachineTest`, `InstallmentCalculatorTest`.
- Integration test `LoanControllerIT` (TestContainers).

### ACCEPTANCE CRITERIA
- Apply loan → status PENDING + event `loan.application.created` + `loan.scoring.requested` published.
- Consume `loan.scoring.completed` → status UNDER_REVIEW.
- `PATCH .../status` oleh reviewer → APPROVED; reject wajib reason.
- Setelah APPROVED/DISBURSED → jadwal cicilan ter-generate.
- Duplicate idempotency key → cached response; concurrent update → 409.
- Unit coverage > 80%.

### INSTRUKSI EKSEKUSI
Tulis semua kode lengkap dan production-ready. Jangan beri penjelasan. Jangan tanya konfirmasi. Mulai langsung dari file pertama.

============================================================

## 🎯 FASE 3 — Scoring Service (FastAPI + TensorFlow)

### PROJECT CONTEXT
Koperasi Digital P2P Lending. scoring-service menilai kelayakan kredit UMKM tanpa credit history bank. Output: risk_score 0–100, risk_category (LOW≥70/MEDIUM 40-69/HIGH<40), approval_recommendation (APPROVE/REVIEW/REJECT), confidence 0–1. 18+ fitur dari PostgreSQL (financial behavior, business profile, loan request, history, completeness). Dipicu via Kafka `loan.scoring.requested`, kirim hasil balik ke loan-service. PostgreSQL `scoring_schema`. Base path `/api/v1/scoring`.

### STACK FASE INI
Python 3.11, FastAPI, TensorFlow 2.x/Keras, asyncpg/SQLAlchemy async, Pydantic v2, httpx, pytest, Docker.

### TASK
Buat scoring-service lengkap di `services/scoring-service`:
- `main.py`: FastAPI app + `lifespan` (warm-up: load model + scaler + encoder, 5 dummy prediction).
- `model/`: `credit_model_v1.0.0.h5` (dummy Keras model untuk dev, buat & simpan via script), `model_loader.py`, `preprocessing.py` (StandardScaler Group A, MinMaxScaler Group C, one-hot).
- `feature_extractor.py`: query PostgreSQL async, hitung 18 fitur sesuai ml-model (payment_consistency_score, debt_to_income_ratio, business_age_months, dll).
- Pydantic schema: `ScoreRequest(loan_application_id, borrower_id, features?)`, `ScoreResponse(risk_score, risk_category, approval_recommendation, confidence, features_used, model_version, processing_time_ms)`.
- Router: `POST /api/v1/scoring/score`, `GET /api/v1/scoring/health`, `GET /api/v1/scoring/model-info`.
- Async PostgreSQL client (asyncpg).
- Error: `ModelNotLoadedException`, `FeatureExtractionException` + handler.
- Callback async (BackgroundTasks + httpx) ke loan-service setelah scoring.
- `Dockerfile` (`python:3.11-slim`), `requirements.txt`.
- `config.py` (Pydantic BaseSettings), `.env`.
- `tests/test_scoring.py` (pytest, mock model + DB).

### ACCEPTANCE CRITERIA
- `POST /score` → JSON lengkap dalam < 2 detik.
- `GET /health` → 200 setelah warm-up (model_loaded true).
- `GET /model-info` → versi + metrics.
- Model warm-up saat startup (dummy prediction).
- Decision logic: ≥70 APPROVE/LOW, 40-69 REVIEW/MEDIUM, <40 REJECT/HIGH.

### INSTRUKSI EKSEKUSI
Tulis semua kode lengkap dan production-ready. Jangan beri penjelasan. Jangan tanya konfirmasi. Mulai langsung dari file pertama.

============================================================

## 🎯 FASE 4 — Payment Service

### PROJECT CONTEXT
Koperasi Digital P2P Lending. payment-service menangani disbursement (pencairan) & pembayaran cicilan via Midtrans (utama) & Xendit (fallback). Wajib idempotent (tabel idempotency_keys + Redis) & distributed lock untuk cegah dana ganda. Webhook gateway diverifikasi signature/HMAC. RabbitMQ untuk retry (max 3x) + DLQ. Kafka untuk publish event. PostgreSQL `payment_schema`. Base path `/api/v1/payments`.

### STACK FASE INI
Java 21, Spring Boot 3.x, Spring Data JPA, Spring Data Redis, Spring Kafka, Spring AMQP (RabbitMQ), Flyway, PostgreSQL, Gradle, JUnit 5, Mockito, TestContainers.

### TASK
Buat payment-service lengkap di `services/payment-service` (paket `com.koperasi.payment.*`):
- Entity: `LoanDisbursement`, `Payment`, `WebhookEvent`, `IdempotencyKey` (sesuai ERD).
- Flyway `V1__create_payment_tables.sql`.
- `MidtransClient`: initiate disbursement, create payment link, verify webhook signature (SHA512).
- `XenditClient`: fallback + verify callback token.
- Idempotency: cek `idempotency_keys` + Redis `pay:idem:{key}` (TTL 24h) sebelum operasi finansial; return cached bila COMPLETED, 409 bila PROCESSING.
- Distributed lock (Redis `lock:disburse:{loanId}`): cegah double disbursement.
- Webhook handler: `POST /webhook/midtrans`, `POST /webhook/xendit` (verify signature → update status + simpan payload).
- RabbitMQ producer: `payment.retry.queue` saat gagal; consumer: retry exponential backoff → DLQ setelah 3x.
- Kafka producer: `payment.disbursement.initiated/completed`, `payment.installment.received/failed`.
- REST: `POST /installments/{id}/pay`, `GET /installments/{id}/status`.
- `application.yml`, global exception handler, `ApiResponse<T>`.
- Unit test `PaymentServiceTest` (idempotency, double-disbursement prevention), `WebhookVerificationTest` (valid/invalid signature), `IdempotencyTest`.
- Integration test `PaymentControllerIT` (mock Midtrans + TestContainers).

### ACCEPTANCE CRITERIA
- Initiate disbursement → idempotency check → call Midtrans → save PENDING.
- Webhook valid → verify signature → update COMPLETED + publish Kafka event.
- Webhook invalid signature → 400.
- Duplicate idempotency key → cached response (tanpa call gateway ulang).
- Payment gagal → masuk retry queue → DLQ setelah 3x.

### INSTRUKSI EKSEKUSI
Tulis semua kode lengkap dan production-ready. Jangan beri penjelasan. Jangan tanya konfirmasi. Mulai langsung dari file pertama.

============================================================

## 🎯 FASE 5 — Notification Service

### PROJECT CONTEXT
Koperasi Digital P2P Lending. notification-service meng-consume event Kafka (loan.*, payment.*, loan.defaulted) → simpan notifikasi + push real-time via WebSocket (STOMP) & SSE ke user yang tepat. RabbitMQ `notification.email.queue` untuk email async (mock). PostgreSQL `notification_schema`. Koneksi real-time diautentikasi JWT. Base path `/api/v1/notifications`.

### STACK FASE INI
Java 21, Spring Boot 3.x, Spring WebSocket (STOMP), Spring Data JPA, Spring Kafka, Spring AMQP, Spring Data Redis (pub/sub fanout), Flyway, PostgreSQL, Gradle, JUnit 5, Mockito.

### TASK
Buat notification-service lengkap di `services/notification-service` (paket `com.koperasi.notification.*`):
- Entity: `Notification`(user_id, title, body, type enum LOAN_STATUS/PAYMENT/SYSTEM, reference_id, is_read).
- Flyway `V1__create_notification_tables.sql`.
- Kafka consumer: consume `loan.application.*`, `loan.installment.overdue`, `loan.defaulted`, `payment.*`, `notification.send.requested` → simpan notifikasi + push real-time.
- WebSocket config (STOMP): endpoint `/ws`, topic `/topic/notifications/{userId}`, handshake JWT auth.
- SSE: `GET /api/v1/notifications/stream` (SseEmitter per userId) + heartbeat.
- Redis Pub/Sub: fanout notifikasi antar replika (untuk scaling WebSocket).
- REST: `GET /api/v1/notifications`, `PATCH /{id}/read`, `PATCH /read-all`.
- RabbitMQ consumer: `notification.email.queue` → log email (mock send).
- `application.yml`, `ApiResponse<T>`, global exception handler.
- Unit test `NotificationServiceTest` (kafka consumer processing, SSE emitter, routing ke user yang tepat).

### ACCEPTANCE CRITERIA
- Kafka event consumed → notification tersimpan + WebSocket message terkirim ke user yang tepat.
- SSE stream berfungsi (emit + heartbeat).
- `PATCH /read` & `/read-all` memperbarui status.
- Email queue consumer memproses pesan (mock log).

### INSTRUKSI EKSEKUSI
Tulis semua kode lengkap dan production-ready. Jangan beri penjelasan. Jangan tanya konfirmasi. Mulai langsung dari file pertama.

============================================================

## 🎯 FASE 6 — Frontend Auth + Base Layout

### PROJECT CONTEXT
Koperasi Digital P2P Lending. Frontend Next.js (React 19 + JavaScript, BUKAN TypeScript). Desain Traveloka-inspired: biru primary `#2563EB`, putih bersih, whitespace generous, clean corporate; angka finansial pakai JetBrains Mono. Backend via API gateway `/api/v1`. JWT access 15 menit + refresh 7 hari (auto-refresh). Roles: BORROWER/INVESTOR/REVIEWER/ADMIN. Dark/light mode via CSS custom properties.

### STACK FASE INI
Next.js (App Router), React 19 + JavaScript, Tailwind CSS, TanStack Query, Axios, React Hook Form + Zod, Zustand, Lucide React.

### TASK
Buat frontend auth + base layout di `frontend/`:
- Setup: `next.config.js`, `tailwind.config.js` (token design system: colors primary/neutral/semantic, fontFamily Inter+JetBrains Mono, fontSize scale, spacing, boxShadow, darkMode:'class'), `postcss.config.js`.
- `app/layout.jsx` (preload font Inter + JetBrains Mono, script no-flash dark mode, QueryClientProvider, ThemeProvider).
- `app/globals.css` (CSS custom properties light + `[data-theme="dark"]`, loan status colors).
- `middleware.js` (route protection: redirect `/login` bila tak auth).
- UI components: `Button.jsx`(primary/secondary/ghost/danger; sm/default/lg), `Input.jsx`(label, error, helper), `Card.jsx`, `Badge.jsx`(loan status colors), `LoadingSkeleton.jsx`, `ErrorBoundary.jsx`, `StatsCard.jsx`(icon + number JetBrains Mono + label + trend).
- Layout: `Navbar.jsx`(logo + nav + user menu + theme toggle), `Sidebar.jsx`(collapsible, role-based menu, Lucide icons), `Footer.jsx`.
- Pages: `/login`(email+password, RHF+Zod, redirect), `/register`(multi-field step 1, validation), `/forgot-password`(email + success state).
- State/data: `stores/authStore.js`(Zustand: token, user, setAuth, logout), `services/api.js`(Axios instance + interceptor auto-refresh token), `services/authService.js`(login/register/refresh/logout/getMe), `hooks/useAuth.js`.
- WCAG 2.1 AA: focus ring 2px primary, kontras, skip nav, aria-label. Responsive mobile-first + hamburger.

### ACCEPTANCE CRITERIA
- `/login` render + submit → redirect `/dashboard`.
- Auto-refresh token saat access token expired (interceptor).
- Dark mode toggle berfungsi tanpa flash.
- Route terproteksi redirect ke `/login`.
- WCAG: focus ring & skip nav ada, kontras lulus.

### INSTRUKSI EKSEKUSI
Tulis semua kode lengkap dan production-ready. Jangan beri penjelasan. Jangan tanya konfirmasi. Mulai langsung dari file pertama.

============================================================

## 🎯 FASE 7 — Frontend Borrower Flow

### PROJECT CONTEXT
Koperasi Digital P2P Lending, frontend Next.js (React 19 + JavaScript). Melanjutkan Fase 6 (biru primary, card-based, StatsCard, angka JetBrains Mono). Halaman untuk BORROWER: dashboard, apply pinjaman multi-step, daftar & detail pinjaman, bayar cicilan. Status pinjaman update real-time via WebSocket. API via `/api/v1`.

### STACK FASE INI
Next.js (App Router), React 19 + JavaScript, Tailwind CSS, TanStack Query, Axios, React Hook Form, Zustand, Lucide React, WebSocket (STOMP client).

### TASK
Buat halaman BORROWER di `frontend/`:
- `/dashboard`: StatsCard (pinjaman aktif, cicilan jatuh tempo bulan ini, total dibayar, skor kredit terakhir) + tabel pinjaman terbaru + alert cicilan overdue.
- `/loans/apply`: multi-step form 4 langkah — Step1 pilih produk (card grid), Step2 jumlah+tenor+tujuan+simulasi cicilan real-time, Step3 upload KTP+selfie (preview), Step4 review+disclaimer+submit. RHF per step, progress indicator, validation, `Idempotency-Key`.
- `/loans`: tabel semua pinjaman (status badge, amount JetBrains Mono, tanggal) + filter by status.
- `/loans/[id]`: header (status badge besar + amount + progress bar cicilan) + status timeline vertikal + tabel jadwal cicilan + CTA "Bayar Cicilan" bila PENDING/OVERDUE.
- `/loans/[id]/pay`: pilih cicilan → summary → redirect Midtrans.
- Data: `services/loanService.js`(getApplications, getApplicationById, createApplication, getInstallments, initiatePayment), `hooks/useLoanApplication.js`, `hooks/useInstallments.js` (TanStack Query + key factory), `stores/loanStore.js`(draft multi-step).
- `hooks/useNotifications.js`: connect `/ws`, subscribe `/topic/notifications/{userId}`, update TanStack Query cache saat status berubah.

### ACCEPTANCE CRITERIA
- Apply form submit → API call → redirect `/loans/{id}`.
- Simulasi cicilan terhitung real-time di Step 2.
- Status pinjaman update real-time via WebSocket (tanpa reload).
- Tabel pinjaman filter by status berfungsi.
- Bayar cicilan → redirect ke payment gateway.

### INSTRUKSI EKSEKUSI
Tulis semua kode lengkap dan production-ready. Jangan beri penjelasan. Jangan tanya konfirmasi. Mulai langsung dari file pertama.

============================================================

## 🎯 FASE 8 — Frontend Reviewer + Admin

### PROJECT CONTEXT
Koperasi Digital P2P Lending, frontend Next.js (React 19 + JavaScript). Melanjutkan Fase 6–7. Halaman REVIEWER (antrian review, detail review + AI score, approve/reject, history) & ADMIN (dashboard metrics + charts, kelola user/pinjaman/produk, laporan). Dashboard-heavy dengan DataTable & chart (Recharts) memakai warna design system. API via `/api/v1`.

### STACK FASE INI
Next.js (App Router), React 19 + JavaScript, Tailwind CSS, TanStack Query, Axios, React Hook Form, Zustand, Lucide React, Recharts.

### TASK
Buat halaman REVIEWER & ADMIN di `frontend/`:
REVIEWER:
- `/reviewer/queue`: tabel antrian UNDER_REVIEW (sortable tanggal/amount/risk, filter risk, pagination), action "Review".
- `/reviewer/[id]`: borrower info + dokumen, AI Score display (gauge chart + risk badge + fitur utama), approve form (notes) + reject form (reason dropdown + notes), confirm modal.
- `/reviewer/history`: tabel riwayat review + filter tanggal.
ADMIN:
- `/admin/dashboard`: metrics cards (Total AUM, Pinjaman Aktif, NPL Rate %, Disbursement Hari Ini) + charts Recharts (LineChart disbursement bulanan, BarChart approval vs rejection, PieChart portfolio by risk).
- `/admin/users`: DataTable (search, filter role, sort, pagination) + CRUD modal.
- `/admin/loans`: DataTable semua pinjaman (filter status/date range, sort) + export CSV.
- `/admin/loan-products`: CRUD produk (add/edit/deactivate).
- `/admin/reports`: DateRange picker + generate + export PDF/CSV.
Reusable: `components/DataTable.jsx`(columns config, sorting, pagination, selection, export), `components/Charts/`(AreaChart, BarChart, PieChart wrapper design-system colors), `components/ConfirmModal.jsx`, `components/DateRangePicker.jsx`.
Services: `services/adminService.js`, `services/reviewerService.js`.

### ACCEPTANCE CRITERIA
- Reviewer approve/reject dari UI → API call berhasil + confirm modal.
- AI score tervisualisasi (gauge + fitur).
- Admin dashboard chart render (mock data OK).
- DataTable sort + filter + paginate + export berfungsi.
- Produk pinjaman CRUD berfungsi.

### INSTRUKSI EKSEKUSI
Tulis semua kode lengkap dan production-ready. Jangan beri penjelasan. Jangan tanya konfirmasi. Mulai langsung dari file pertama.

============================================================

## 🎯 FASE 9 — DevOps

### PROJECT CONTEXT
Koperasi Digital P2P Lending, microservices (8 service aplikasi + infra). Deploy via Docker Compose (lokal), Kubernetes (prod), Nginx, GitHub Actions CI/CD, Prometheus+Grafana, Terraform (AWS ECS Fargate/RDS/ElastiCache/MSK/ALB). SLA 99.9%, horizontal scaling (loan & scoring). Secrets via AWS Secrets Manager.

### STACK FASE INI
Docker, Docker Compose, Kubernetes (Deployment/Service/Ingress/HPA/ConfigMap/Secret/PVC), Nginx, GitHub Actions, Prometheus, Grafana, Terraform (AWS).

### TASK
Buat konfigurasi deployment lengkap:
- Docker: `docker-compose.yml` FINAL (semua service + infra, health check, depends_on, restart policy, named volume, logging), `.dockerignore` per service, script bash verify health endpoint.
- `k8s/`: namespace `koperasi`; per service `Deployment.yaml`(2 replica, resource limits, liveness+readiness probe, env dari ConfigMap/Secret) + `Service.yaml`(ClusterIP); `ConfigMap.yaml`; `Secret.yaml`(template); `Ingress.yaml`(Nginx Ingress, SSL, rate limiting annotation); `HPA.yaml`(loan + scoring, min2 max5, CPU70%); `PersistentVolumeClaim.yaml`(PostgreSQL, Redis).
- Nginx: `nginx/nginx.conf`(upstream per service, proxy_pass, SSL termination, HSTS, rate limiting, gzip, security headers), `nginx/conf.d/koperasi.conf`.
- GitHub Actions: `.github/workflows/test.yml`, `build-push.yml`, `deploy-staging.yml`, `deploy-production.yml`(manual approval environment protection).
- Monitoring: `prometheus/prometheus.yml`(scrape semua service via `/actuator/prometheus` + Python `/metrics`), `grafana/dashboards/koperasi-overview.json`(JVM + API latency + loan metrics).
- Terraform `terraform/`: `main.tf, variables.tf, outputs.tf`, `modules/{ecs,rds,elasticache,networking,alb}`.

### ACCEPTANCE CRITERIA
- `docker compose up` → semua container healthy.
- `kubectl apply --dry-run=client` valid untuk semua manifest.
- GitHub Actions workflow syntax valid.
- Prometheus scrape config mencakup semua service.
- Terraform `terraform validate` lulus.

### INSTRUKSI EKSEKUSI
Tulis semua kode lengkap dan production-ready. Jangan beri penjelasan. Jangan tanya konfirmasi. Mulai langsung dari file pertama.

============================================================

## 🎯 FASE 10 — Testing & Hardening

### PROJECT CONTEXT
Koperasi Digital P2P Lending, semua service & frontend sudah dibangun (Fase 1–8). Fase ini melengkapi test suite (backend JUnit5+TestContainers, frontend Jest+RTL) & security hardening (OWASP Top 10, headers, rate limiting, caching, N+1 fix). Target coverage ≥80% (auth & loan), no critical vulnerability.

### STACK FASE INI
JUnit 5, Mockito, TestContainers (PostgreSQL/Redis/Kafka), Jest, React Testing Library, MSW, Bucket4j, Spring Security, Spring Cache (Redis), OWASP Dependency Check (Gradle).

### TASK
Buat test suite + hardening:
Backend test:
- auth-service: `AuthServiceTest`(register success/duplicate, login success/wrong-password/not-found, refresh success/expired, logout blacklist), `AuthControllerIT`(@SpringBootTest + TestContainers PostgreSQL+Redis + MockMvc).
- loan-service: `LoanStateMachineTest`(semua valid + invalid transition), `LoanServiceTest`(apply success/duplicate idempotency/concurrent lock), `InstallmentCalculatorTest`(3/6/12 bulan flat rate), `LoanControllerIT`(TestContainers full flow).
- payment-service: `IdempotencyTest`(cached response), `WebhookVerificationTest`(valid→process, invalid→400), `PaymentServiceTest`(double disbursement prevention).
Frontend test (Jest+RTL+MSW):
- `auth/__tests__/LoginForm.test.jsx`(render, validation, submit success/error), `loans/__tests__/LoanApplyForm.test.jsx`(multi-step nav, validation per step), `dashboard/__tests__/StatsCard.test.jsx`(render props).
Security hardening:
- `SecurityHeadersFilter.java` semua service (HSTS, X-Frame-Options DENY, X-Content-Type-Options nosniff, CSP, Referrer-Policy).
- Rate limiting Bucket4j di api-gateway (per IP + per user).
- Input sanitization `HtmlUtils.htmlEscape`; CSP header; verify semua query JPA parameter binding (no concatenation).
- CORS final (hanya domain frontend); JWT `jti` claim untuk revocation.
- OWASP Dependency Check plugin di Gradle.
Performance:
- `@Cacheable("loanProducts")` di LoanProductService; `@Cacheable("userById", key="#userId")` + `@CacheEvict` (TTL 5 menit) di UserService.
- `@Transactional(readOnly=true)` di semua method read-only.
- Fix N+1: `@EntityGraph` di `LoanApplicationRepository.findByIdWithInstallments`.
- Script SQL verifikasi index (semua FK + status column).

### ACCEPTANCE CRITERIA
- Unit test coverage ≥ 80% di auth-service + loan-service.
- Integration test pass dengan TestContainers.
- Frontend test pass (Jest + RTL + MSW).
- Semua security header hadir di response.
- OWASP Dependency Check: no critical vulnerability.
- Cache & `@EntityGraph` mengurangi query (N+1 teratasi).

### INSTRUKSI EKSEKUSI
Tulis semua kode lengkap dan production-ready. Jangan beri penjelasan. Jangan tanya konfirmasi. Mulai langsung dari file pertama.

============================================================

============================================================

## 🎯 FASE 11 — Audit Akhir vs `docs/`

### PROJECT CONTEXT
Seluruh fase 0–10 selesai. Lakukan audit menyeluruh terhadap implementasi kode, konfigurasi, test, dan deployment menggunakan seluruh dokumen di `docs/` sebagai source of truth. Audit tidak hanya mengecek build hijau, tetapi memverifikasi kesesuaian kontrak API, event, database, keamanan, UI, DevOps, dan kepatuhan.

### STACK FASE INI
Markdown, Mermaid, shell, Gradle, JUnit, pytest, Jest, Docker, kubectl/kubeconform, Terraform, Prometheus.

### TASK
- Inventaris seluruh dokumen `docs/` dan buat traceability matrix requirement → implementasi → bukti.
- Audit API contract: path, method, role, status code, request/response, pagination, error, idempotency.
- Audit Kafka/RabbitMQ: topic, envelope, payload, producer/consumer, retry, DLQ.
- Audit database: 7 schema, role/grant, Flyway vs entity, index, FK, constraint, retensi.
- Audit keamanan: JWT/refresh/jti, BCrypt, RBAC, header, CORS, rate limit, sanitization, secret, UU PDP, OWASP.
- Audit frontend: design system, alur BORROWER/REVIEWER/ADMIN, WCAG 2.1 AA, responsive.
- Audit DevOps: Compose, K8s, Nginx, CI/CD, Terraform, Prometheus/Grafana, runbook.
- Audit testing & coverage; jalankan ulang perintah verifikasi.
- Buat gap register dengan severity Critical/High/Medium/Low dan rekomendasi.
- Tulis hasil di `docs/audit/` dan perbarui `docs/README.md`.

### ACCEPTANCE CRITERIA
- 100% dokumen `docs/` masuk inventory/traceability.
- Semua endpoint/topic/schema/halaman utama teraudit.
- Gap register lengkap dengan bukti dan severity.
- Tidak ada temuan Critical; High diperbaiki atau memiliki waiver.
- `docs/audit/final-report.md` berisi verdict dan sign-off.
- `docs/README.md` menavigasi folder audit.

### INSTRUKSI EKSEKUSI
Tulis semua dokumen audit lengkap. Jangan beri penjelasan. Jangan tanya konfirmasi. Mulai langsung dari file pertama.
