> **Arsip:** dokumen audit ini disusun untuk arsitektur 7-service. Lihat
> [functional-audit-v2.md](../functional-audit-v2.md) dan
> [security-audit-v2.md](../security-audit-v2.md) untuk arsitektur terkini.

# DevOps Audit

## Terverifikasi
- `docker compose config --quiet` sukses; logging json-file; healthcheck; volume.
- K8s: `kubectl kustomize` + `kubeconform` → 42 resource valid.
- Nginx `nginx -t` sukses setelah pemisahan `conf.d/koperasi.conf`, gzip, security header.
- GitHub Actions 4 workflow, YAML valid.
- Terraform `init -backend=false` + `validate` sukses; 5 modul.
- Prometheus scrape 8 service + alert rules; Grafana dashboard.
- `scripts/verify-health.sh` valid.

## Gap
| Gap | Severity | Rekomendasi |
|---|---|---|
| `docker compose up` full healthy belum dijalankan | High | Jalankan smoke/staging |
| Terraform `plan/apply` belum diuji ke AWS | High | Plan di akun sandbox |
| K8s PVC storageClass/backup belum dispesifikkan | Medium | StorageClass + Velero |
| CI workflow belum dieksekusi di GitHub | Medium | Push repo/runner |
| Prometheus belum diuji dengan promtool | Low | Tambah `promtool check config` di CI |
| Nginx image hanya diuji config, belum trafik penuh | Medium | Smoke Nginx → gateway |
| Tidak ada alertmanager/on-call | Medium | Tambahkan Alertmanager |
