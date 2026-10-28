<!-- FILE: docs/events/kafka-topics.md -->
# Event Streaming & Messaging Specification
## Platform Koperasi Digital / P2P Lending untuk UMKM Indonesia

| Atribut | Nilai |
|---|---|
| Dokumen | Kafka Topics Specification |
| Versi | 1.0 |
| Tanggal | 2026-10-24 |
| Naming Convention | `{service}.{entity}.{action}` |
| Default Retention | 7 hari |
| Default Replication Factor | 2 |

---

## SECTION A — Kafka Topics

> Konvensi global: setiap event memiliki **envelope standar** (`eventId`, `eventType`, `timestamp`, `traceId`, `version`, `payload`). Partisi berdasarkan key agar urutan per entitas terjaga.

---

### A.1 `loan.application.created`
- **Producer(s):** loan-service
- **Consumer(s):** scoring-service
- **Partition key:** `loanApplicationId`
- **Partitions:** 6 (high-volume) · **Replication:** 2 · **Retention:** 7 hari

```json
{
  "eventId": "UUID",
  "eventType": "loan.application.created",
  "timestamp": "ISO8601",
  "traceId": "UUID",
  "version": "1.0",
  "payload": {
    "loanApplicationId": "UUID",
    "borrowerId": "UUID",
    "loanProductId": "integer",
    "amount": "decimal",
    "tenorMonths": "integer",
    "purpose": "string",
    "status": "string (PENDING)",
    "idempotencyKey": "string",
    "submittedAt": "ISO8601"
  }
}
```

### A.2 `loan.scoring.requested`
- **Producer(s):** loan-service
- **Consumer(s):** scoring-service
- **Partition key:** `loanApplicationId`
- **Partitions:** 3 · **Replication:** 2 · **Retention:** 7 hari

```json
{
  "eventId": "UUID",
  "eventType": "loan.scoring.requested",
  "timestamp": "ISO8601",
  "traceId": "UUID",
  "version": "1.0",
  "payload": {
    "loanApplicationId": "UUID",
    "borrowerId": "UUID",
    "features": "object (map<string, number>)",
    "requestedAt": "ISO8601"
  }
}
```

### A.3 `loan.scoring.completed`
- **Producer(s):** scoring-service
- **Consumer(s):** loan-service
- **Partition key:** `loanApplicationId`
- **Partitions:** 3 · **Replication:** 2 · **Retention:** 7 hari

```json
{
  "eventId": "UUID",
  "eventType": "loan.scoring.completed",
  "timestamp": "ISO8601",
  "traceId": "UUID",
  "version": "1.0",
  "payload": {
    "loanApplicationId": "UUID",
    "borrowerId": "UUID",
    "riskScore": "decimal (0-100)",
    "riskCategory": "string (LOW|MEDIUM|HIGH)",
    "approvalRecommendation": "string (APPROVE|REVIEW|REJECT)",
    "confidence": "decimal (0-1)",
    "modelVersion": "string",
    "processingTimeMs": "integer",
    "scoringStatus": "string (SUCCESS|FALLBACK)",
    "scoredAt": "ISO8601"
  }
}
```

### A.4 `loan.application.approved`
- **Producer(s):** loan-service
- **Consumer(s):** payment-service, core-service (notification)
- **Partition key:** `loanApplicationId`
- **Partitions:** 3 · **Replication:** 2 · **Retention:** 7 hari

```json
{
  "eventId": "UUID",
  "eventType": "loan.application.approved",
  "timestamp": "ISO8601",
  "traceId": "UUID",
  "version": "1.0",
  "payload": {
    "loanApplicationId": "UUID",
    "borrowerId": "UUID",
    "amount": "decimal",
    "tenorMonths": "integer",
    "reviewerId": "UUID",
    "overrideScore": "boolean",
    "approvedAt": "ISO8601"
  }
}
```

### A.5 `loan.application.rejected`
- **Producer(s):** loan-service
- **Consumer(s):** core-service (notification)
- **Partition key:** `loanApplicationId`
- **Partitions:** 3 · **Replication:** 2 · **Retention:** 7 hari

```json
{
  "eventId": "UUID",
  "eventType": "loan.application.rejected",
  "timestamp": "ISO8601",
  "traceId": "UUID",
  "version": "1.0",
  "payload": {
    "loanApplicationId": "UUID",
    "borrowerId": "UUID",
    "reviewerId": "UUID",
    "rejectionReason": "string",
    "rejectedAt": "ISO8601"
  }
}
```

### A.6 `payment.disbursement.initiated`
- **Producer(s):** payment-service
- **Consumer(s):** loan-service
- **Partition key:** `loanApplicationId`
- **Partitions:** 3 · **Replication:** 2 · **Retention:** 7 hari

```json
{
  "eventId": "UUID",
  "eventType": "payment.disbursement.initiated",
  "timestamp": "ISO8601",
  "traceId": "UUID",
  "version": "1.0",
  "payload": {
    "disbursementId": "UUID",
    "loanApplicationId": "UUID",
    "amount": "decimal",
    "paymentGateway": "string (MIDTRANS|XENDIT)",
    "idempotencyKey": "string",
    "initiatedAt": "ISO8601"
  }
}
```

### A.7 `payment.disbursement.completed`
- **Producer(s):** payment-service
- **Consumer(s):** loan-service, core-service (notification)
- **Partition key:** `loanApplicationId`
- **Partitions:** 3 · **Replication:** 2 · **Retention:** 7 hari

```json
{
  "eventId": "UUID",
  "eventType": "payment.disbursement.completed",
  "timestamp": "ISO8601",
  "traceId": "UUID",
  "version": "1.0",
  "payload": {
    "disbursementId": "UUID",
    "loanApplicationId": "UUID",
    "amount": "decimal",
    "paymentGateway": "string (MIDTRANS|XENDIT)",
    "externalTransactionId": "string",
    "status": "string (COMPLETED)",
    "disbursedAt": "ISO8601"
  }
}
```

### A.8 `payment.installment.received`
- **Producer(s):** payment-service
- **Consumer(s):** loan-service, core-service (notification)
- **Partition key:** `loanApplicationId`
- **Partitions:** 6 (high-volume) · **Replication:** 2 · **Retention:** 7 hari

```json
{
  "eventId": "UUID",
  "eventType": "payment.installment.received",
  "timestamp": "ISO8601",
  "traceId": "UUID",
  "version": "1.0",
  "payload": {
    "paymentId": "UUID",
    "installmentId": "UUID",
    "loanApplicationId": "UUID",
    "borrowerId": "UUID",
    "installmentNumber": "integer",
    "amount": "decimal",
    "paymentGateway": "string (MIDTRANS|XENDIT)",
    "externalTransactionId": "string",
    "paidAt": "ISO8601"
  }
}
```

### A.9 `payment.installment.failed`
- **Producer(s):** payment-service
- **Consumer(s):** loan-service, core-service (notification)
- **Partition key:** `loanApplicationId`
- **Partitions:** 3 · **Replication:** 2 · **Retention:** 7 hari

```json
{
  "eventId": "UUID",
  "eventType": "payment.installment.failed",
  "timestamp": "ISO8601",
  "traceId": "UUID",
  "version": "1.0",
  "payload": {
    "paymentId": "UUID",
    "installmentId": "UUID",
    "loanApplicationId": "UUID",
    "borrowerId": "UUID",
    "amount": "decimal",
    "failureReason": "string",
    "retryCount": "integer",
    "failedAt": "ISO8601"
  }
}
```

### A.10 `loan.installment.overdue`
- **Producer(s):** loan-service (scheduler)
- **Consumer(s):** core-service (notification)
- **Partition key:** `loanApplicationId`
- **Partitions:** 3 · **Replication:** 2 · **Retention:** 7 hari

```json
{
  "eventId": "UUID",
  "eventType": "loan.installment.overdue",
  "timestamp": "ISO8601",
  "traceId": "UUID",
  "version": "1.0",
  "payload": {
    "installmentId": "UUID",
    "loanApplicationId": "UUID",
    "borrowerId": "UUID",
    "installmentNumber": "integer",
    "dueDate": "date",
    "daysOverdue": "integer",
    "amount": "decimal",
    "detectedAt": "ISO8601"
  }
}
```

### A.11 `loan.defaulted`
- **Producer(s):** loan-service
- **Consumer(s):** core-service (notification)
- **Partition key:** `loanApplicationId`
- **Partitions:** 3 · **Replication:** 2 · **Retention:** 7 hari

```json
{
  "eventId": "UUID",
  "eventType": "loan.defaulted",
  "timestamp": "ISO8601",
  "traceId": "UUID",
  "version": "1.0",
  "payload": {
    "loanApplicationId": "UUID",
    "borrowerId": "UUID",
    "totalOutstanding": "decimal",
    "daysOverdue": "integer",
    "defaultedAt": "ISO8601"
  }
}
```

### A.12 `notification.send.requested`
- **Producer(s):** loan-service, payment-service, auth-service, scoring-service
- **Consumer(s):** core-service (notification)
- **Partition key:** `userId`
- **Partitions:** 6 (high-volume) · **Replication:** 2 · **Retention:** 7 hari

```json
{
  "eventId": "UUID",
  "eventType": "notification.send.requested",
  "timestamp": "ISO8601",
  "traceId": "UUID",
  "version": "1.0",
  "payload": {
    "userId": "UUID",
    "title": "string",
    "body": "string",
    "type": "string (LOAN_STATUS|PAYMENT|SYSTEM)",
    "channel": "string (WEBSOCKET|SSE|EMAIL|SMS)",
    "referenceId": "string",
    "requestedAt": "ISO8601"
  }
}
```

### A.13 `audit.event.created`
- **Producer(s):** semua service
- **Consumer(s):** reporting-service (audit sink)
- **Partition key:** `entityId`
- **Partitions:** 6 (high-volume) · **Replication:** 2 · **Retention:** 7 hari (lalu diarsipkan ke cold storage 7 tahun)

```json
{
  "eventId": "UUID",
  "eventType": "audit.event.created",
  "timestamp": "ISO8601",
  "traceId": "UUID",
  "version": "1.0",
  "payload": {
    "actorId": "UUID (nullable)",
    "actorRole": "string",
    "action": "string",
    "entityType": "string",
    "entityId": "string",
    "oldValue": "object (nullable)",
    "newValue": "object (nullable)",
    "ipAddress": "string",
    "userAgent": "string",
    "service": "string",
    "result": "string (SUCCESS|FAILURE)",
    "createdAt": "ISO8601"
  }
}
```

---

## SECTION B — Retry (Kafka)

RabbitMQ sudah dihapus. Retry payment memakai topik Kafka:

| Atribut | Nilai |
|---|---|
| Topic retry | `payment.retry` (3 partisi) |
| Topic DLT | `payment.retry.DLT` |
| Max attempt | 3 (`PAYMENT_RETRY_MAX_ATTEMPTS`) |
| Backoff | in-process scheduler: 1 dtk → 2 dtk (`PaymentProperties`) |
| Consumer | `payment-service` (`PaymentRetryConsumer`) |
| Setelah habis | `DefaultErrorHandler` mempublikasikan ke `payment.retry.DLT` |

> Keterbatasan demo: jadwal backoff disimpan di memori. Jika pod restart sebelum retry
> berjalan, retry hilang; produksi memerlukan tabel retry persisten atau delayed topic.

---

## SECTION C — Full Event Schema Examples

### C.1 `loan.application.created`
```json
{
  "eventId": "e1a2b3c4-d5e6-4789-90ab-cdef12345678",
  "eventType": "loan.application.created",
  "timestamp": "2026-10-24T08:30:00.000Z",
  "traceId": "a1b2c3d4-e5f6-4789-90ab-cdef12345678",
  "version": "1.0",
  "payload": {
    "loanApplicationId": "c9d0e1f2-a3b4-4567-bc89-9012345abcde",
    "borrowerId": "7a8b9c0d-1e2f-4a3b-8c4d-5e6f7a8b9c0d",
    "loanProductId": 1,
    "amount": 15000000.00,
    "tenorMonths": 12,
    "purpose": "Pembelian peralatan dapur untuk ekspansi warung",
    "status": "PENDING",
    "idempotencyKey": "loan-7a8b-apply-1730000000-a1b2c3d4",
    "submittedAt": "2026-10-24T08:30:00.000Z"
  }
}
```

### C.2 `loan.application.approved`
```json
{
  "eventId": "f2b3c4d5-e6f7-4890-ab12-cdef23456789",
  "eventType": "loan.application.approved",
  "timestamp": "2026-10-24T09:00:00.000Z",
  "traceId": "b2c3d4e5-f6a7-4890-ab12-cdef23456789",
  "version": "1.0",
  "payload": {
    "loanApplicationId": "c9d0e1f2-a3b4-4567-bc89-9012345abcde",
    "borrowerId": "7a8b9c0d-1e2f-4a3b-8c4d-5e6f7a8b9c0d",
    "amount": 15000000.00,
    "tenorMonths": 12,
    "reviewerId": "11112222-3333-4444-5555-666677778888",
    "overrideScore": false,
    "approvedAt": "2026-10-24T09:00:00.000Z"
  }
}
```

### C.3 `payment.installment.received`
```json
{
  "eventId": "a3c4d5e6-f7a8-4901-bc23-def345678901",
  "eventType": "payment.installment.received",
  "timestamp": "2026-10-24T10:05:00.000Z",
  "traceId": "c3d4e5f6-a7b8-4901-bc23-def345678901",
  "version": "1.0",
  "payload": {
    "paymentId": "cc33dd44-ee55-4ff6-a007-112233445566",
    "installmentId": "aa11bb22-cc33-4dd4-8ee5-ff6677889900",
    "loanApplicationId": "c9d0e1f2-a3b4-4567-bc89-9012345abcde",
    "borrowerId": "7a8b9c0d-1e2f-4a3b-8c4d-5e6f7a8b9c0d",
    "installmentNumber": 1,
    "amount": 1375000.00,
    "paymentGateway": "MIDTRANS",
    "externalTransactionId": "MID-TXN-99887766",
    "paidAt": "2026-10-24T10:05:00.000Z"
  }
}
```

---

## Lampiran D — Event Lifecycle Tambahan

Event berikut diproduksi oleh loan-service setelah pembayaran cicilan dan dikonsumsi
core-service (modul notification).

### D.1 `loan.application.active`
- **Producer:** loan-service
- **Consumer:** core-service (notification)
- **Partition key:** `loanApplicationId`

```json
{
  "eventId": "UUID",
  "eventType": "loan.application.active",
  "timestamp": "ISO8601",
  "traceId": "UUID",
  "version": "1.0",
  "payload": {
    "loanApplicationId": "UUID",
    "borrowerId": "UUID",
    "previousStatus": "DISBURSED",
    "status": "ACTIVE",
    "changedAt": "ISO8601"
  }
}
```

### D.2 `loan.application.completed`
- **Producer:** loan-service
- **Consumer:** core-service (notification)
- **Partition key:** `loanApplicationId`

```json
{
  "eventId": "UUID",
  "eventType": "loan.application.completed",
  "timestamp": "ISO8601",
  "traceId": "UUID",
  "version": "1.0",
  "payload": {
    "loanApplicationId": "UUID",
    "borrowerId": "UUID",
    "previousStatus": "ACTIVE",
    "status": "COMPLETED",
    "completedAt": "ISO8601",
    "changedAt": "ISO8601"
  }
}
```

### D.3 `loan.application.under_review`
- **Producer:** loan-service
- **Consumer:** core-service (notification)
- **Partition key:** `loanApplicationId`

```json
{
  "eventId": "UUID",
  "eventType": "loan.application.under_review",
  "timestamp": "ISO8601",
  "traceId": "UUID",
  "version": "1.0",
  "payload": {
    "loanApplicationId": "UUID",
    "borrowerId": "UUID",
    "previousStatus": "PENDING",
    "status": "UNDER_REVIEW",
    "changedAt": "ISO8601"
  }
}
```

### D.4 `loan.application.disbursed`
- **Producer:** loan-service
- **Consumer:** core-service (notification)
- **Partition key:** `loanApplicationId`

```json
{
  "eventId": "UUID",
  "eventType": "loan.application.disbursed",
  "timestamp": "ISO8601",
  "traceId": "UUID",
  "version": "1.0",
  "payload": {
    "loanApplicationId": "UUID",
    "borrowerId": "UUID",
    "previousStatus": "APPROVED",
    "status": "DISBURSED",
    "disbursedAt": "ISO8601",
    "changedAt": "ISO8601"
  }
}
```
