<!-- FILE: docs/deployment/env-variables.md -->
# Environment Variables Reference
## Platform Koperasi Digital / P2P Lending untuk UMKM Indonesia

| Atribut | Nilai |
|---|---|
| Dokumen | Environment Variables Reference |
| Versi | 1.1 |
| Tanggal | 2026-10-27 |

> Variabel sensitif (secret, password, key) **tidak boleh** di-commit. Di lokal pakai `.env`; di AWS pakai Secrets Manager. Nilai contoh di bawah hanya ilustrasi.

---

## Common / Shared Variables
Berlaku lintas service (nilai berbeda per service bila relevan).

| Variable | Description | Example Value | Required |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Profil aktif Spring | `prod` | ✅ |
| `SERVER_PORT` | Port service | `8082` | ✅ |
| `LOG_LEVEL` | Level logging root | `INFO` | ✅ |
| `LOG_LEVEL_APP` | Level logging paket aplikasi | `DEBUG` | ❌ |
| `TZ` | Timezone | `Asia/Jakarta` | ❌ |
| `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE` | Actuator endpoints | `health,info,prometheus,metrics` | ✅ |
| `MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED` | Liveness/readiness probe | `true` | ✅ |
| `CORS_ALLOWED_ORIGINS` | Origin diizinkan (CSV) | `https://app.koperasi.example.id` | ✅ |

---

## 2. auth-service (:8081)

| Variable | Description | Example Value | Required |
|---|---|---|---|
| `SERVER_PORT` | Port service | `8081` | ✅ |
| `DB_URL` | JDBC URL PostgreSQL | `jdbc:postgresql://postgres:5432/koperasi?currentSchema=auth_schema` | ✅ |
| `DB_USERNAME` | User DB | `auth_user` | ✅ |
| `DB_PASSWORD` | Password DB | `authP@ss` | ✅ |
| `DB_SCHEMA` | Schema | `auth_schema` | ✅ |
| `REDIS_HOST` | Host Redis (blacklist/refresh) | `redis` | ✅ |
| `REDIS_PORT` | Port Redis | `6379` | ✅ |
| `REDIS_PASSWORD` | Auth Redis | `s3cr3t` | ✅ |
| `KAFKA_BOOTSTRAP_SERVERS` | Broker Kafka | `kafka:9092` | ✅ |
| `JWT_SECRET` | Secret penandatangan JWT | `base64:xxxxx` | ✅ |
| `JWT_ACCESS_TOKEN_TTL` | TTL access token (detik) | `900` | ✅ |
| `JWT_REFRESH_TOKEN_TTL` | TTL refresh token (detik) | `604800` | ✅ |
| `JWT_ISSUER` | Issuer | `koperasi-auth` | ✅ |
| `BCRYPT_COST_FACTOR` | Cost BCrypt | `12` | ✅ |
| `PII_ENCRYPTION_KEY` | Kunci AES-256-GCM untuk NIK/NPWP/rekening | `base64:xxxxx` | ✅ |
| `PASSWORD_RESET_URL` | Base URL halaman reset password | `https://app.koperasi.example.id/reset-password` | ❌ |
| `PASSWORD_RESET_LOG_LINK` | Tampilkan link reset di log (khusus demo) | `false` | ❌ |
| `CORS_ALLOWED_ORIGINS` | Origin diizinkan | `https://app.koperasi.example.id` | ✅ |

---

## 3. loan-service (:8082)

| Variable | Description | Example Value | Required |
|---|---|---|---|
| `SERVER_PORT` | Port service | `8082` | ✅ |
| `DB_URL` | JDBC URL | `jdbc:postgresql://postgres:5432/koperasi?currentSchema=loan_schema` | ✅ |
| `DB_USERNAME` | User DB | `loan_user` | ✅ |
| `DB_PASSWORD` | Password DB | `loanP@ss` | ✅ |
| `DB_SCHEMA` | Schema | `loan_schema` | ✅ |
| `REDIS_HOST` | Host Redis | `redis` | ✅ |
| `REDIS_PASSWORD` | Auth Redis | `s3cr3t` | ✅ |
| `KAFKA_BOOTSTRAP_SERVERS` | Broker Kafka | `kafka:9092` | ✅ |
| `KAFKA_CONSUMER_GROUP` | Consumer group | `loan-service-group` | ✅ |
| `SCORING_SERVICE_URL` | URL scoring (REST) | `http://scoring:8083` | ✅ |
| `PAYMENT_SERVICE_URL` | URL payment (REST) | `http://payment:8084` | ✅ |
| `LOAN_DEFAULT_OVERDUE_DAYS` | Ambang DEFAULT (hari) | `90` | ❌ |
| `JWT_SECRET` | Verifikasi JWT | `base64:xxxxx` | ✅ |
| `CORS_ALLOWED_ORIGINS` | Origin diizinkan | `https://app.koperasi.example.id` | ✅ |

---

## 4. scoring-service (:8083, Python/FastAPI)

| Variable | Description | Example Value | Required |
|---|---|---|---|
| `PORT` | Port service | `8083` | ✅ |
| `DB_HOST` | Host PostgreSQL | `postgres` | ✅ |
| `DB_PORT` | Port DB | `5432` | ✅ |
| `DB_NAME` | Nama database | `koperasi` | ✅ |
| `DB_USER` | User DB | `scoring_user` | ✅ |
| `DB_PASSWORD` | Password DB | `scoreP@ss` | ✅ |
| `DB_SCHEMA` | Schema | `scoring_schema` | ✅ |
| `REDIS_HOST` | Host Redis (cache fitur) | `redis` | ✅ |
| `REDIS_PASSWORD` | Auth Redis | `s3cr3t` | ✅ |
| `KAFKA_BOOTSTRAP_SERVERS` | Broker Kafka | `kafka:9092` | ✅ |
| `ML_MODEL_PATH` | Path artefak model | `/app/models/credit_model_v2.3.1.h5` | ✅ |
| `ML_SCALER_PATH` | Path scaler | `/app/models/scaler.pkl` | ✅ |
| `ML_ENCODER_PATH` | Path encoder | `/app/models/encoder.pkl` | ✅ |
| `MODEL_VERSION` | Versi model aktif | `v2.3.1` | ✅ |
| `CANARY_TRAFFIC_PCT` | % traffic canary | `10` | ❌ |
| `LOG_LEVEL` | Level log | `INFO` | ✅ |
| `CORS_ALLOWED_ORIGINS` | Origin diizinkan | `https://app.koperasi.example.id` | ✅ |

---

## 5. payment-service (:8084)

| Variable | Description | Example Value | Required |
|---|---|---|---|
| `SERVER_PORT` | Port service | `8084` | ✅ |
| `DB_URL` | JDBC URL | `jdbc:postgresql://postgres:5432/koperasi?currentSchema=payment_schema` | ✅ |
| `DB_USERNAME` | User DB | `payment_user` | ✅ |
| `DB_PASSWORD` | Password DB | `payP@ss` | ✅ |
| `DB_SCHEMA` | Schema | `payment_schema` | ✅ |
| `REDIS_HOST` | Host Redis (lock/idempotency) | `redis` | ✅ |
| `REDIS_PASSWORD` | Auth Redis | `s3cr3t` | ✅ |
| `KAFKA_BOOTSTRAP_SERVERS` | Broker Kafka | `kafka:9092` | ✅ |
| `MIDTRANS_SERVER_KEY` | Server key Midtrans | `SB-Mid-server-xxxx` | ✅ |
| `MIDTRANS_CLIENT_KEY` | Client key Midtrans | `SB-Mid-client-xxxx` | ✅ |
| `MIDTRANS_WEBHOOK_SECRET` | Secret verifikasi webhook | `xxxx` | ✅ |
| `MIDTRANS_IS_PRODUCTION` | Mode produksi | `false` | ✅ |
| `XENDIT_API_KEY` | API key Xendit | `xnd_development_xxxx` | ✅ |
| `XENDIT_WEBHOOK_TOKEN` | Callback token Xendit | `xxxx` | ✅ |
| `IDEMPOTENCY_TTL_HOURS` | TTL idempotency key | `24` | ❌ |
| `LOAN_SERVICE_URL` | Base URL loan-service untuk callback internal | `http://loan-service:8082` | ✅ |
| `INTERNAL_CALLBACK_TOKEN` | Bearer token endpoint internal loan-service | `change-me` | ❌ |
| `LOAN_CONNECT_TIMEOUT_MS` | Timeout koneksi ke loan-service (ms) | `2000` | ❌ |
| `LOAN_READ_TIMEOUT_MS` | Timeout baca ke loan-service (ms) | `3000` | ❌ |
| `JWT_SECRET` | Verifikasi JWT | `base64:xxxxx` | ✅ |
| `CORS_ALLOWED_ORIGINS` | Origin diizinkan | `https://app.koperasi.example.id` | ✅ |

> `LOAN_CONNECT_TIMEOUT_MS`/`LOAN_READ_TIMEOUT_MS` mencegah webhook gateway menggantung saat
> loan-service tidak tersedia. Baris pembayaran/pencairan tetap tersimpan dengan
> `sync_status=FAILED` lalu direkonsiliasi otomatis.

---

## 6. Modul notification (berjalan di loan-service)

| Variable | Description | Example Value | Required |
|---|---|---|---|
| `SERVER_PORT` | Port service | `8085` | ✅ |
| `DB_URL` | JDBC URL | `jdbc:postgresql://postgres:5432/koperasi?currentSchema=notification_schema` | ✅ |
| `DB_USERNAME` | User DB | `notif_user` | ✅ |
| `DB_PASSWORD` | Password DB | `notifP@ss` | ✅ |
| `DB_SCHEMA` | Schema | `notification_schema` | ✅ |
| `REDIS_HOST` | Host Redis (pub/sub fanout) | `redis` | ✅ |
| `REDIS_PASSWORD` | Auth Redis | `s3cr3t` | ✅ |
| `KAFKA_BOOTSTRAP_SERVERS` | Broker Kafka | `kafka:9092` | ✅ |
| `KAFKA_CONSUMER_GROUP` | Consumer group (dipakai modul notifikasi di core) | `loan-service-group` | ✅ |
| `SMTP_HOST` | Host SMTP email | `email-smtp.ap-southeast-1.amazonaws.com` | ✅ |
| `SMTP_PORT` | Port SMTP | `587` | ✅ |
| `SMTP_USERNAME` | User SMTP | `AKIAxxxx` | ✅ |
| `SMTP_PASSWORD` | Password SMTP | `xxxx` | ✅ |
| `WEBSOCKET_ALLOWED_ORIGINS` | Origin WS | `https://app.koperasi.example.id` | ✅ |
| `JWT_SECRET` | Auth koneksi WS/SSE | `base64:xxxxx` | ✅ |

---

## 7. Modul reporting (berjalan di loan-service)

| Variable | Description | Example Value | Required |
|---|---|---|---|
| `SERVER_PORT` | Port service | `8086` | ✅ |
| `DB_URL` | JDBC URL | `jdbc:postgresql://postgres:5432/koperasi?currentSchema=reporting_schema` | ✅ |
| `DB_USERNAME` | User DB | `report_user` | ✅ |
| `DB_PASSWORD` | Password DB | `reportP@ss` | ✅ |
| `DB_SCHEMA` | Schema | `reporting_schema` | ✅ |
| `REDIS_HOST` | Host Redis (cache dashboard) | `redis` | ✅ |
| `REDIS_PASSWORD` | Auth Redis | `s3cr3t` | ✅ |
| `KAFKA_BOOTSTRAP_SERVERS` | Broker Kafka | `kafka:9092` | ✅ |
| `REPORT_EXPORT_BUCKET` | S3 bucket ekspor | `koperasi-reports` | ❌ |
| `JWT_SECRET` | Verifikasi JWT | `base64:xxxxx` | ✅ |
| `CORS_ALLOWED_ORIGINS` | Origin diizinkan | `https://app.koperasi.example.id` | ✅ |

---

## 8. frontend (:3000, Next.js)

| Variable | Description | Example Value | Required |
|---|---|---|---|
| `PORT` | Port frontend | `3000` | ✅ |
| `NEXT_PUBLIC_API_BASE_URL` | Base URL API gateway | `https://api.koperasi.example.id/api/v1` | ✅ |
| `NEXT_PUBLIC_WS_URL` | URL WebSocket notifikasi | `wss://api.koperasi.example.id/ws/notifications` | ✅ |
| `NEXT_PUBLIC_MIDTRANS_CLIENT_KEY` | Client key Midtrans (Snap) | `SB-Mid-client-xxxx` | ✅ |
| `NEXT_PUBLIC_ENV` | Environment label | `production` | ❌ |
| `NODE_ENV` | Mode Node | `production` | ✅ |
