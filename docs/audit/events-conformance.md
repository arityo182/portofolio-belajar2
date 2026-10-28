> **Arsip:** dokumen audit ini disusun untuk arsitektur 7-service. Lihat
> [functional-audit-v2.md](../functional-audit-v2.md) dan
> [security-audit-v2.md](../security-audit-v2.md) untuk arsitektur terkini.

# Events Conformance

## Sesuai
- Envelope `eventId,eventType,timestamp,traceId,version,payload`.
- Producer/consumer loan, scoring, payment, notification.
- `notification.email.queue` + DLQ.
- Event utama: application created/under_review/approved/rejected/disbursed/active/completed,
  scoring requested/completed, payment received/failed, disbursement initiated/completed,
  installment overdue, loan.defaulted, notification.send.requested, audit.event.created.
- Retry + DLT (`<topic>.DLT`) dikonfigurasi untuk consumer loan-service dan notification-service.

## Gap
| Gap | Severity | Status | Bukti/Dampak |
|---|---|---|---|
| `loan.installment.overdue` belum diproduksi | High | ✅ Fixed | `InstallmentOverdueScheduler` memproduksi event harian dan notification-service memetakannya. |
| `loan.status.changed` dipakai notification tetapi tidak ada di katalog event | Medium | ✅ Fixed | Referensi dokumentasi diganti dengan event konkret `loan.application.*`; consumer memakai event tersebut. |
| reporting-service belum menjadi consumer | Medium | ✅ By design | Reporting memakai koneksi read-only ke schema operasional, bukan consumer Kafka. |
| Event `loan.application.disbursed`/`under_review` tambahan tidak tercantum | Low | ✅ Fixed | Katalog `docs/events/kafka-topics.md` lampiran D mencantumkan under_review, disbursed, active, dan completed. |
| Tidak ada skema event versioning/registry formal | Low | ⚠️ Open | Risiko breaking change; perlu registry/schema validation sebelum produksi. |
