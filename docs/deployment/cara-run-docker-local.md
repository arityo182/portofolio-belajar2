<!-- FILE: docs/deployment/cara-run-docker-local.md -->
# Cara Menjalankan di Docker (Lokal)
## Platform Koperasi Digital / P2P Lending untuk UMKM Indonesia

| Atribut | Nilai |
|---|---|
| Dokumen | Local Development Setup (Docker Compose) |
| Versi | 1.0 |
| Tanggal | 2026-10-24 |

---

## 1. Prerequisites

| Kebutuhan | Versi / Spesifikasi |
|---|---|
| Docker Desktop | 4.x+ (Docker Engine 24+, Compose v2) |
| RAM | **Minimal 8 GB** (disarankan 16 GB; alokasi Docker ≥ 6 GB) |
| Disk | Minimal 20 GB kosong |
| OS | Linux / macOS / Windows (WSL2) |
| Git | 2.x+ |

### Port yang Digunakan (pastikan bebas)
| Port | Layanan |
|---|---|
| 3000 | frontend |
| 8081–8084 | auth, loan (core), scoring, payment |
| 5432 | PostgreSQL |
| 6379 | Redis |
| 29092 | Kafka (KRaft) |
| 80 / 443 | Nginx |
| 9090 / 3001 | Prometheus / Grafana |

---

## 2. docker-compose.yml

Compose resmi proyek ada di [`docker-compose.yml`](../../docker-compose.yml) dan sudah
mencakup seluruh service, healthcheck, volume, serta jaringan. Dokumen ini tidak lagi
menyalin isi file tersebut agar tidak terjadi duplikasi yang basi.

Service yang berjalan:

| Service | Container | Port host |
|---|---|---:|
| auth-service (identity) | `koperasi-auth-service` | 8081 |
| loan-service (core: pinjaman + notifikasi + reporting) | `koperasi-loan-service` | 8082 |
| scoring-service | `koperasi-scoring-service` | 8083 |
| payment-service | `koperasi-payment-service` | 8084 |
| frontend | `koperasi-frontend` | 3000 |
| nginx | `koperasi-nginx` | 80/443 |
| postgres | `koperasi-postgres` | 5432 |
| redis | `koperasi-redis` | 6379 |
| kafka (KRaft) | `koperasi-kafka` | 29092 |
| prometheus | `koperasi-prometheus` | 9090 |
| grafana | `koperasi-grafana` | 3001 |

> Tidak ada lagi `api-gateway`, `notification-service`, `reporting-service`, `rabbitmq`,
> atau `zookeeper`. Lihat [ADR-0001](../architecture/adr/0001-consolidate-to-four-services.md).

Perintah dasar:

```bash
cp .env.example .env
docker compose up -d --build
docker compose ps
```

---

## 3. Step by Step

### Step 1 — Clone repo
```bash
git clone https://github.com/koperasi/platform.git
cd platform
```

### Step 2 — Copy & isi environment
```bash
cp .env.example .env
# edit .env: isi JWT_SECRET, MIDTRANS_*, XENDIT_*, password DB/Redis, dll
```

### Step 3 — Build semua images
```bash
docker compose build
```

### Step 4 — Jalankan semua services
```bash
docker compose up -d
docker compose ps        # cek status container
```

### Step 5 — Verifikasi health semua service
```bash
for p in 8081 8082 8083 8084; do
  echo "port $p:"; curl -s http://localhost:$p/actuator/health; echo
done
curl -s http://localhost:8083/health    # scoring (FastAPI)
curl -s http://localhost:3000           # frontend
```

### Step 6 — Database migration (Flyway)
```bash
# tiap service menjalankan Flyway saat startup, atau manual:
docker compose exec loan ./gradlew flywayMigrate
# atau jalankan job migrasi khusus bila disediakan
```

### Step 7 — Seed data awal
```bash
docker compose exec postgres psql -U koperasi -d koperasi -f /docker-entrypoint-initdb.d/seed.sql
# seed: loan_products, role (BORROWER/INVESTOR/REVIEWER/ADMIN), 1 admin user
```

---

## 4. Health Check Commands per Service

| Service | Command |
|---|---|
| auth | `curl http://localhost:8081/actuator/health` |
| loan | `curl http://localhost:8082/actuator/health` |
| scoring | `curl http://localhost:8083/health` |
| payment | `curl http://localhost:8084/actuator/health` |
| frontend | `curl -I http://localhost:3000` |
| postgres | `docker compose exec postgres pg_isready -U koperasi` |
| redis | `docker compose exec redis redis-cli -a s3cr3t ping` |
| kafka | `docker compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list` |

---

## 5. Common Errors + Solusi

| # | Masalah | Gejala | Solusi |
|---|---|---|---|
| 1 | **Port sudah dipakai** | `bind: address already in use` | Cari proses: `lsof -i :8082` lalu hentikan, atau ubah mapping port di compose |
| 2 | **Service start sebelum DB siap** | Error koneksi DB saat boot | Pastikan `depends_on` + `condition: service_healthy`; Flyway retry; cek `docker compose logs postgres` |
| 3 | **Kafka tidak terjangkau** | `Connection to node -1 could not be established` | Gunakan `kafka:9092` dari dalam network (bukan `localhost`); tunggu healthcheck Kafka hijau |
| 4 | **Out of Memory / container killed** | Exit code 137 | Naikkan alokasi RAM Docker Desktop ≥ 6 GB; jalankan subset service saja |
| 5 | **Redis NOAUTH / auth gagal** | `NOAUTH Authentication required` | Set `REDIS_PASSWORD` di `.env` sama dengan command Redis; sertakan `-a` saat `redis-cli` |

---

## 6. Useful Docker Commands

```bash
# Lihat logs satu service (follow)
docker compose logs -f loan

# Lihat logs semua service
docker compose logs -f

# Masuk ke shell container
docker compose exec loan sh
docker compose exec postgres psql -U koperasi -d koperasi

# Restart satu service
docker compose restart payment

# Rebuild & jalankan ulang satu service saja
docker compose up -d --build scoring

# Hentikan semua (tanpa hapus volume)
docker compose down

# Hentikan + hapus volume (reset total data)
docker compose down -v

# Lihat resource usage
docker stats
```
