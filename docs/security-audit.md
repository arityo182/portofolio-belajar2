> **Arsip:** temuan pada dokumen ini diambil sebelum konsolidasi 4 service.
> Untuk arsitektur terkini, lihat [security-audit-v2.md](security-audit-v2.md).

# Security Audit — Koperasi Digital (Portofolio2)

Tanggal: 2026-10-26  
Standar: OWASP Top 10:2025  
Scope: seluruh repository `/home/arie/Belajar/Portofolio2`

## 1. Executive Summary

| Severity | Total | Fixed | Open / Accepted |
|---|---:|---:|---:|
| 🔴 CRITICAL | 0 | 0 | 0 |
| 🟠 HIGH | 5 | 3 | 2 |
| 🟡 MEDIUM | 10 | 1 | 9 |
| 🟢 LOW | 5 | 1 | 4 |

**Verdict:** tidak ada temuan Critical. Dua temuan High masih terbuka dan memerlukan
perubahan arsitektur atau upgrade dependency mayor: penyimpanan token yang dapat dibaca
JavaScript dan advisory `zod`. Keduanya memiliki mitigasi kompensasi yang tercatat di
bawah. Untuk production penuh, keduanya harus ditutup.

### Catatan scope

- Project ini **hanya** berisi Next.js + Spring Boot. Tidak ada project React + Vite,
  sehingga seluruh checklist Vite (VITE_ env, vite.config define, sourcemap, dev server
  host) berstatus **N/A**.
- Tidak ada route `app/api/**` atau `pages/api/**`; frontend memanggil Java REST API.
- Repository ini bukan Git repository, sehingga pemeriksaan `git log` untuk secret
  historis tidak dapat dijalankan. File `.env` sudah masuk `.gitignore`.
- Tidak ada field NIK/NPWP/rekening pada schema saat ini. Borrower profile/upload belum
  diimplementasikan, sehingga kontrol enkripsi khusus field tersebut belum applicable.

## 2. Top 5 Risiko

1. **Pencairan dana memakai nominal/status dari request reviewer** — dapat mencairkan
   nominal berbeda dari nilai pinjaman. **Fixed** (H-1).
2. **Refresh/access token disimpan di cookie yang dapat dibaca JavaScript** — XSS dapat
   mencuri sesi. **Open, dikompensasi** (H-3).
3. **JWT access token dikirim di query string WebSocket** — berpotensi bocor ke access
   log/proxy. **Fixed** (H-2).
4. **Default JWT/internal secret di docker-compose** — jika dipakai apa adanya, token
   dapat dipalsukan. **Fixed** (H-4).
5. **Advisory `zod` high** — memory exhaustion pada validasi collection besar.
   **Open, dampak terbatas ke client** (H-5).

---

## 3. HIGH

### H-1 — Disbursement amount/status tidak divalidasi

- **Severity:** 🟠 HIGH
- **Stack:** Spring Boot
- **Category:** OWASP A01 Broken Access Control / A06 Insecure Design
- **File:** `services/payment-service/src/main/java/com/koperasi/payment/service/PaymentServiceImpl.java`
- **Line:** 165–206 (sebelum fix)
- **Status:** ✅ Fixed

**Issue:** `initiateDisbursement` menerima `amount` dan `loanApplicationId` dari request
reviewer lalu langsung membuat `LoanDisbursement`. Tidak ada pengecekan bahwa nominal
sama dengan nilai pinjaman atau bahwa status pinjaman `APPROVED`. Reviewer yang
terautentikasi dapat mencairkan nominal arbitrer.

**Code (sebelum):**
```java
LoanDisbursement disbursement = LoanDisbursement.create(
        request.loanApplicationId(),
        request.amount(),
        request.paymentGateway(),
        idempotencyKey
);
```

**Fix (sesudah):**
```java
LoanApplicationInfo loan = loanServiceClient.getLoanApplication(request.loanApplicationId());
if (!"APPROVED".equalsIgnoreCase(loan.status())) {
    throw new BusinessValidationException(
            "Pencairan hanya dapat dilakukan untuk pinjaman berstatus APPROVED");
}
if (loan.amount() == null || request.amount().compareTo(loan.amount()) != 0) {
    throw new BusinessValidationException(
            "Nominal pencairan tidak sesuai dengan nilai pinjaman");
}
```

Tambahan yang diterapkan:
- `LoanApplicationOwnerResponse` sekarang mengirim `amount`.
- `payment-service` menambah `LoanApplicationInfo` dan
  `LoanServiceClient.getLoanApplication()` dengan header `X-Internal-Token`.
- Unit/IT test disbursement diperbarui; dua test baru untuk nominal salah dan status
  bukan `APPROVED`.

### H-2 — JWT access token di query string WebSocket

- **Severity:** 🟠 HIGH
- **Stack:** Next.js + Spring Boot
- **Category:** OWASP A07 Authentication Failures / A02 Security Misconfiguration
- **File:** `frontend/hooks/useNotifications.js`, `services/notification-service/src/main/java/com/koperasi/notification/websocket/JwtHandshakeInterceptor.java`
- **Line:** frontend 13–29 (sebelum), interceptor 26–43 (sebelum)
- **Status:** ✅ Fixed

**Issue:** URL WebSocket memakai `?token=<access-token>`. Token dapat tercatat di access
log Nginx, browser history, atau proxy. Query token juga diterima handshake.

**Code (sebelum):**
```js
const url = new URL(base);
url.searchParams.set("token", token);
```

**Fix (sesudah):**
```js
// URL tidak lagi membawa token.
client.connectHeaders = { Authorization: `Bearer ${token}` };
```
```java
String token = request.getHeaders().getFirst("Authorization");
if (token == null || !token.startsWith("Bearer ")) {
    // Handshake tanpa token; STOMP CONNECT wajib membawa Authorization.
    return true;
}
```
`WebSocketAuthInterceptor` sudah memvalidasi native header `Authorization` pada STOMP
CONNECT dan menolak subscription ke topik user lain. Query token tidak lagi dipakai.

### H-3 — Token sesi disimpan di cookie yang dapat dibaca JavaScript

- **Severity:** 🟠 HIGH
- **Stack:** Next.js
- **Category:** OWASP A07 Authentication Failures / A04 Cryptographic Failures
- **File:** `frontend/lib/authCookies.js`
- **Line:** 16–30, 62–79
- **Status:** ⚠️ Open — compensated control

**Issue:** Access token dan refresh token ditulis lewat `document.cookie` tanpa
`HttpOnly`, sehingga JavaScript (termasuk payload XSS) dapat membacanya. Token juga
disimpan di Zustand untuk header Authorization. `localStorage` hanya dipakai untuk tema
dan preferensi sidebar, bukan token.

**Code:**
```js
document.cookie = `${name}=${encodeURIComponent(value)}; Path=/; Max-Age=...; SameSite=Lax; Secure`;
```

**Mitigasi yang sudah ada:**
- Cookie `Secure` pada HTTPS dan `SameSite=Lax`.
- Access token berumur 15 menit; refresh rotation + reuse detection.
- CSP baru, tidak ada `dangerouslySetInnerHTML` dari input user, tidak ada `eval`/
  `innerHTML` dengan input user, dan React melakukan escaping.
- `npm audit`/review finding XSS tidak menemukan sink aktif.

**Remediation yang disarankan (belum diterapkan):** pindahkan penerbitan token ke BFF/
Next Route Handler dengan cookie `HttpOnly; Secure; SameSite=Strict`, atau set cookie
HttpOnly dari auth-service dan ubah frontend agar tidak menyimpan refresh token.
Perubahan ini menyentuh alur login, refresh, middleware, dan CORS sehingga tidak
dilakukan otomatis dalam audit ini.

### H-4 — Default secret JWT dan internal token di Compose

- **Severity:** 🟠 HIGH
- **Stack:** Spring Boot / Infrastructure
- **Category:** OWASP A02 Security Misconfiguration / A05 Injection
- **File:** `docker-compose.yml`, `.env.example`
- **Line:** 167, 217, 266, 268, 325, 389, 399, 459, 461, 504
- **Status:** ✅ Fixed

**Issue:** Compose memakai default yang dapat diprediksi:
`local-development-only-jwt-secret-change-me-32bytes` dan
`local-internal-callback-token`. Siapa pun yang tahu nilai ini dapat memalsukan JWT atau
memanggil endpoint internal loan-service.

**Fix:**
```yaml
JWT_SECRET: ${JWT_SECRET:?JWT_SECRET must be set in .env}
INTERNAL_CALLBACK_TOKEN: ${INTERNAL_CALLBACK_TOKEN:?INTERNAL_CALLBACK_TOKEN must be set in .env}
```
- `.env` lokal dirotasi dengan nilai acak (`openssl rand`), permission `600`.
- `.env.example` sekarang berisi placeholder `CHANGE_ME...` dan bukan nilai yang bisa
  dipakai langsung.
- `services/scoring-service/.env` disinkronkan dengan internal token baru.
- K8s sudah memakai `k8s/secret.yaml` dengan placeholder `REPLACE_ME`.

### H-5 — Advisory dependency `zod` (high)

- **Severity:** 🟠 HIGH (npm audit)
- **Stack:** Next.js
- **Category:** OWASP A03 Software Supply Chain Failures
- **File:** `frontend/package.json`
- **Line:** 28
- **Status:** ⚠️ Open — mitigated

**Issue:** `zod < 4.6.6` memiliki advisory memory exhaustion pada validasi constrained
collection (GHSA-mrhx-c7xc-m3w5). Perbaikan memerlukan Zod 4, yang breaking.

**Dampak aktual:** Zod hanya dipakai untuk validasi form di client. Tidak ada endpoint
server yang memakai Zod. Upaya upgrade ke 4.6.7 menyebabkan 4 test frontend gagal pada
audit ini, sehingga upgrade dibatalkan agar tidak merusak alur registrasi/login.
Validasi server tetap memakai Jakarta Bean Validation dan DTO whitelist.

**Remediation:** migrasi ke Zod 4 pada pekerjaan terpisah, perbarui schema
`required_error`/`errorMap`, lalu jalankan seluruh test frontend.
`postcss` high sudah diperbaiki dengan override ke `8.5.31`.

---

## 4. MEDIUM

### M-1 — Security headers Next.js belum lengkap

- **Severity:** 🟡 MEDIUM
- **Stack:** Next.js
- **Category:** OWASP A02 Security Misconfiguration
- **File:** `frontend/next.config.js`
- **Line:** 26–43 sebelum fix
- **Status:** ✅ Fixed

Sebelumnya hanya ada `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, dan
`Permissions-Policy`. Nginx sudah menambahkan HSTS/CSP, tetapi akses langsung ke port
frontend tidak mendapatkannya.

**Fix:** tambah `Strict-Transport-Security` dan `Content-Security-Policy`. CSP:
```js
const contentSecurityPolicy = [
  "default-src 'self'",
  `script-src ${isDev ? "'self' 'unsafe-inline' 'unsafe-eval'" : "'self' 'unsafe-inline'"}`,
  "style-src 'self' 'unsafe-inline'",
  "img-src 'self' data: blob:",
  "font-src 'self' data:",
  "connect-src 'self' https: wss: http://localhost:* ws://localhost:*",
  "object-src 'none'",
  "frame-ancestors 'none'",
  "base-uri 'self'",
  "form-action 'self'",
].join("; ");
```
`unsafe-inline` masih diperlukan karena Next.js menyisipkan script hydration dan theme
bootstrap. `unsafe-eval` hanya aktif di development.

### M-2 — Actuator metrics/prometheus terekspos

- **Severity:** 🟡 MEDIUM
- **Stack:** Spring Boot
- **Category:** OWASP A02 Security Misconfiguration
- **File:** `services/*/src/main/resources/application.yml`
- **Line:** 101–116 (auth), serupa di service lain
- **Status:** ⚠️ Accepted (monitoring requirement)

`management.endpoints.web.exposure.include` berisi `health,info,prometheus,metrics`.
Tidak ada `env`, `heapdump`, `shutdown`, atau `beans`. Metrics dibutuhkan Prometheus,
tetapi port service juga dipublikasikan ke host pada Compose. Untuk production, scrape
metrics hanya dari jaringan monitoring dan gunakan network policy.

### M-3 — Port internal Compose dipublikasikan ke 0.0.0.0

- **Severity:** 🟡 MEDIUM
- **Stack:** Infrastructure
- **Category:** OWASP A02 Security Misconfiguration / A01 Broken Access Control
- **File:** `docker-compose.yml`
- **Line:** port mappings `5436`, `6381`, `8081–8086`, `29093`, `9091`, dll.
- **Status:** ⚠️ Open (local development)

Semua service internal dapat diakses dari host/LAN. K8s memakai `ClusterIP` untuk service
internal. Untuk Compose, bind port ke `127.0.0.1` atau hapus mapping yang tidak perlu,
misalnya:
```yaml
ports:
  - "127.0.0.1:8082:8082"
```
Jangan lakukan ini untuk Nginx `80/443` jika akses LAN memang dibutuhkan.

### M-4 — Rate limit gateway masih in-memory

- **Severity:** 🟡 MEDIUM
- **Stack:** Spring Boot
- **Category:** OWASP A06 Insecure Design
- **File:** `services/api-gateway/src/main/java/com/koperasi/gateway/config/RateLimitFilter.java`
- **Status:** ⚠️ Open

Bucket4j disimpan di `ConcurrentHashMap`. Pada lebih dari satu instance gateway, limit
tidak konsisten. Gunakan Redis/Bucket4j distributed atau rate limit di ingress.

### M-5 — Audit trail belum mencakup payment/disbursement

- **Severity:** 🟡 MEDIUM
- **Stack:** Spring Boot
- **Category:** OWASP A09 Security Logging & Alerting Failures
- **File:** `services/payment-service/src/main/java/com/koperasi/payment/service/PaymentServiceImpl.java`
- **Status:** ⚠️ Open

Auth sudah mengirim `audit.event.created`, tetapi approval, disbursement, dan perubahan
status pembayaran belum memiliki audit record terpusat yang immutable. Event Kafka bukan
pengganti audit trail. Tambahkan audit publisher/table dengan aktor, aksi, entity, dan
hasil.

### M-6 — PII/log email pada notification-service

- **Severity:** 🟡 MEDIUM
- **Stack:** Spring Boot
- **Category:** OWASP A09 Security Logging & Alerting Failures
- **File:** `services/notification-service/src/main/java/com/koperasi/notification/event/NotificationEmailConsumer.java`
- **Line:** 22–24
- **Status:** ⚠️ Open

`log.info` mencatat alamat email dan `log.debug` mencatat body email. Body dapat memuat
tautan reset password. Masking alamat dan hapus body dari log; aktifkan debug hanya
secara lokal.

### M-7 — PII at rest belum dienkripsi di Compose

- **Severity:** 🟡 MEDIUM
- **Stack:** PostgreSQL / Spring Boot
- **Category:** OWASP A04 Cryptographic Failures
- **File:** `init-db.sql`, `services/auth-service/.../User.java`
- **Status:** ⚠️ Open (production RDS sudah `storage_encrypted = true`)

Email dan nomor telepon disimpan plaintext. Volume Docker lokal tidak terenkripsi.
Terraform RDS sudah memakai `storage_encrypted = true`, tetapi field-level encryption
(NIK/NPWP/rekening) belum ada karena schema tersebut belum diimplementasikan.

### M-8 — Reset password belum punya rate limit per email

- **Severity:** 🟡 MEDIUM
- **Stack:** Spring Boot / Gateway
- **Category:** OWASP A07 Authentication Failures
- **File:** `services/auth-service/.../PasswordResetService.java`
- **Status:** ⚠️ Open

Rate limit global gateway 100 request/menit per IP/user. Belum ada limit per alamat
email atau cooldown token. Tambahkan counter Redis per email (mis. 3 permintaan/jam).

### M-9 — Nginx berjalan sebagai root

- **Severity:** 🟡 MEDIUM
- **Stack:** Infrastructure
- **Category:** OWASP A02 Security Misconfiguration
- **File:** `nginx/Dockerfile`
- **Status:** ⚠️ Open

Master process Nginx berjalan sebagai root (worker sebagai `nginx`). Gunakan
`nginxinc/nginx-unprivileged` atau user non-root dengan port >1024.

### M-10 — CI belum menjalankan dependency/security scan

- **Severity:** 🟡 MEDIUM
- **Stack:** CI/CD
- **Category:** OWASP A03 Software Supply Chain Failures
- **File:** `.github/workflows/*.yml`
- **Status:** ⚠️ Open

Workflow build/test belum menjalankan `npm audit`, OWASP Dependency Check, atau
`pip-audit`. Plugin Gradle OWASP sudah terpasang tetapi belum dijadikan gate CI.

---

## 5. LOW

| ID | Severity | Stack | Category | File | Issue | Status |
|---|---|---|---|---|---|---|
| L-1 | 🟢 LOW | Infra | A02 | `.env`, `services/scoring-service/.env` | Permission file 664 | Fixed → `600` |
| L-2 | 🟢 LOW | Spring Boot | A02 | `application.yml` | `/actuator/info` terbuka | Accepted |
| L-3 | 🟢 LOW | Infra | A03 | Dockerfiles | Base image memakai tag, bukan digest | Open |
| L-4 | 🟢 LOW | Spring Boot | A09 | `PasswordResetEmailSender.java` | Reset link dapat di-log jika `PASSWORD_RESET_LOG_LINK=true` | Default `false`; tetap batasi ke local |
| L-5 | 🟢 LOW | Next.js | A02 | `next.config.js` | CSP masih memakai `unsafe-inline` | Accepted (kebutuhan Next hydration) |

---

## 6. Verified Controls (tidak menjadi temuan)

| Area | Hasil |
|---|---|
| JWT | Signature, issuer, expiry, dan `type=access/refresh` divalidasi; refresh rotation + reuse detection; logout blacklist + revoke semua refresh token |
| Password | BCrypt cost 12; password minimum 12 karakter dengan kompleksitas; dummy hash untuk mencegah user enumeration login |
| RBAC | `@PreAuthorize` pada admin, reviewer, borrower, payment; ownership check pada loan, installment, notification, payment |
| SQL injection | Semua query memakai JPQL parameter binding; tidak ada native query/concatenation |
| Command/XXE/SpEL | Tidak ada `Runtime.exec`, `ProcessBuilder`, XML parser, atau SpEL dari input user |
| Mass assignment | Controller memakai DTO record dengan validasi; tidak menerima entity langsung |
| Payment webhook | Midtrans SHA-512 signature, Xendit `X-Callback-Token`, constant-time compare, event dedupe, amount cicilan divalidasi, distributed lock, idempotency key |
| Payment double disbursement | Unique loan application + existing record check + Redis lock + idempotency |
| CORS | Origin eksplisit dari env, bukan `*`; credentials true dengan origin eksplisit |
| CSRF | Stateless bearer token; CSRF disable dapat diterima untuk API tanpa cookie auth — tetapi lihat H-3 jika cookie auth dipakai |
| Error handling | `include-message`/`include-stacktrace` = never; handler mengembalikan pesan generik |
| Security headers | Nginx + Next.js: HSTS, CSP, X-Frame-Options DENY, nosniff, Referrer-Policy, Permissions-Policy |
| Docker | Java/Python/Node runtime memakai user non-root (`USER app`/`nextjs`); Nginx masih root (M-9) |
| Frontend XSS | Tidak ada `innerHTML`/`eval`/`new Function` dengan input user; `dangerouslySetInnerHTML` hanya script tema statis |
| Storage | `localStorage` hanya tema/sidebar; tidak ada token/PII di localStorage |
| Internal endpoints | Gateway memblokir `/api/v1/loans/internal/**` dan `/api/v1/scoring/**`; endpoint internal memerlukan `X-Internal-Token` |
| Actuator | Tidak ada `env`, `heapdump`, `shutdown`, `beans`; hanya health/info/prometheus/metrics |
| Git | Bukan Git repository; `.env` sudah di `.gitignore`; tidak ada secret produksi hardcoded di source |

---

## 7. Dependency & Supply Chain

### npm audit (production)

```text
zod <4.6.6
Severity: high
GHSA-mrhx-c7xc-m3w5 — memory exhaustion on oversized constrained collections
1 high severity vulnerability
```

`postcss` high sudah diperbaiki dengan menaikkan direct dependency ke `8.5.31` dan
`overrides.postcss = $postcss`, sehingga Next.js tidak lagi memakai `postcss 8.4.31`.

Upgrade `zod` ke 4.6.7 dicoba, tetapi 4 test frontend gagal karena breaking change.
Upgrade dibatalkan dan dicatat sebagai H-5.

### Gradle

- Spring Boot 3.4.2, Java 21.
- JJWT 0.12.6.
- Tidak ada Log4j; logging memakai Logback bawaan Spring Boot.
- OWASP Dependency Check plugin terpasang, tetapi scan penuh belum dijalankan pada audit
  ini. Jalankan `./gradlew dependencyCheckAnalyze` di CI.

### Python

`services/scoring-service/requirements.txt` memakai versi pin. `pip-audit` belum
dijalankan pada audit ini; tambahkan ke CI.

---

## 8. Fix yang Diterapkan

| File | Perubahan |
|---|---|
| `services/payment-service/.../PaymentServiceImpl.java` | Validasi status `APPROVED` dan nominal disbursement terhadap loan-service |
| `services/payment-service/.../LoanServiceClient.java` | Client internal `getLoanApplication` |
| `services/payment-service/.../LoanApplicationInfo.java` | DTO baru |
| `services/loan-service/.../LoanApplicationOwnerResponse.java` | Kirim `amount` |
| `services/notification-service/.../JwtHandshakeInterceptor.java` | Handshake tidak lagi menerima token dari query string |
| `frontend/hooks/useNotifications.js` | Token dikirim lewat STOMP `Authorization`, bukan URL |
| `frontend/next.config.js` | HSTS + CSP |
| `docker-compose.yml` | JWT/internal token wajib dari `.env`, tanpa default; healthcheck frontend memakai `127.0.0.1` |
| `nginx/proxy_params_koperasi.conf`, `nginx/conf.d/koperasi.conf` | Deduplikasi security header upstream dan CSP `connect-src` untuk WebSocket |
| `.env` / `.env.example` / `services/scoring-service/.env` | Rotasi secret lokal, placeholder template, permission `600` |
| `frontend/package.json` | Override `postcss` ke versi patched |
| Test payment/loan/notification | Test baru/penyesuaian untuk validasi disbursement dan STOMP auth |

### Verifikasi

```text
./gradlew build --no-daemon                         BUILD SUCCESSFUL
npm run lint                                        PASS
npm test -- --runInBand                             4 suites, 6 tests PASS
npm run build                                       PASS
docker compose config --quiet                       PASS
docker compose ps                                   semua container healthy
npm audit --audit-level=high --omit=dev             1 high (zod, mitigated)
curl https://localhost/api/v1/auth/login            200 OK
```

---

## 9. OWASP Top 10:2025 Coverage

| Kategori | Status | Catatan |
|---|---|---|
| A01 Broken Access Control | ⚠️ | RBAC/ownership kuat; H-1 fixed; port internal M-3 |
| A02 Security Misconfiguration | ⚠️ | H-4, M-1 fixed; M-2/M-9/M-3 open |
| A03 Software Supply Chain Failures | ⚠️ | postcss fixed; zod H-5 open; CI scan M-10 |
| A04 Cryptographic Failures | ⚠️ | JWT/BCrypt kuat; token storage H-3 dan PII at rest M-7 |
| A05 Injection | ✅ | JPQL parameterized; tidak ada command/XXE/SpEL |
| A06 Insecure Design | ⚠️ | disbursement fixed; rate limit M-4 |
| A07 Authentication Failures | ⚠️ | JWT/refresh kuat; H-2 fixed; H-3 open; M-8 |
| A08 Software/Data Integrity | ✅ | webhook signature, idempotency, lock, event dedupe |
| A09 Logging & Alerting | ⚠️ | error generic; audit payment M-5; PII log M-6 |
| A10 Mishandling Exceptional Conditions | ✅ | stack trace tidak dikirim; webhook invalid ditolak; retry/DLQ |

---

## 10. Rekomendasi Sebelum Production

1. Tutup H-3 dengan BFF/httpOnly cookie; hapus refresh token dari JavaScript.
2. Selesaikan migrasi Zod 4 atau tambahkan compensating control di CI.
3. Batasi port Compose ke `127.0.0.1`, atau jalankan hanya lewat Nginx/reverse proxy.
4. Pindahkan metrics dan actuator ke jaringan monitoring; tambahkan NetworkPolicy.
5. Tambahkan rate limit per email untuk reset password dan login.
6. Tambahkan audit trail immutable untuk approval, disbursement, dan payment.
7. Jalankan `dependencyCheckAnalyze`, `npm audit`, dan `pip-audit` di CI sebagai gate.
8. Enkripsi PII at rest (minimal disk encryption + field-level encryption untuk data
   sensitif saat borrower profile ditambahkan).
9. Ganti Nginx ke image unprivileged/non-root.
10. Rotasi seluruh secret production melalui secret manager; jangan gunakan `.env` lokal.
