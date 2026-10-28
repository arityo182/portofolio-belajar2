# API Contract
## Platform Koperasi Digital / P2P Lending untuk UMKM Indonesia

| Atribut | Nilai |
|---|---|
| Dokumen | API Contract (OpenAPI 3.0 style) |
| Versi | 1.0 |
| Tanggal | 2026-10-24 |
| Base Path | `/api/v1/` |
| Auth | JWT Bearer Token (Spring Security) |

---

## Konvensi Umum

### Authentication
Endpoint yang memerlukan autentikasi membutuhkan header:
```
Authorization: Bearer <accessToken>
```

### Success Response Wrapper
```json
{
  "success": true,
  "data": {},
  "message": "string",
  "timestamp": "2026-10-24T08:15:30.123Z",
  "traceId": "a1b2c3d4-e5f6-4789-90ab-cdef12345678"
}
```

### Error Response
```json
{
  "success": false,
  "code": "ERROR_CODE",
  "message": "Deskripsi error",
  "details": [],
  "timestamp": "2026-10-24T08:15:30.123Z"
}
```

### HTTP Status Codes (global)
| Code | Makna |
|---|---|
| 200 | OK |
| 201 | Created |
| 204 | No Content |
| 400 | Bad Request (payload tidak valid) |
| 401 | Unauthorized (token tidak ada/invalid) |
| 403 | Forbidden (role tidak memadai) |
| 404 | Not Found |
| 409 | Conflict (duplikasi/idempotency/state invalid) |
| 422 | Unprocessable Entity (validasi bisnis gagal) |
| 500 | Internal Server Error |

---

# 1. auth-service — `/api/v1/auth`

## 1.1 POST /api/v1/auth/register
- **Required role:** public
- **Headers:** `Content-Type: application/json`
- **Request Body:**

| Field | Tipe | Required | Keterangan |
|---|---|---|---|
| email | string (email) | ✅ | unik, max 255 |
| password | string | ✅ | min 12, kompleks |
| phone | string | ✅ | format +62 |
| role | string(enum) | ❌ | BORROWER \| INVESTOR (default BORROWER) |

- **Possible status:** 201, 400, 409, 422, 500

**Contoh Request:**
```json
{
  "email": "budi.umkm@example.id",
  "password": "Koperasi#2026Aman",
  "phone": "+6281234567890",
  "role": "BORROWER"
}
```
**Contoh Response (201):**
```json
{
  "success": true,
  "data": {
    "userId": "3f9c1a2b-7d4e-4c8a-9f10-2b3c4d5e6f70",
    "email": "budi.umkm@example.id",
    "status": "PENDING_VERIFICATION",
    "roles": ["BORROWER"]
  },
  "message": "Registrasi berhasil, silakan verifikasi akun",
  "timestamp": "2026-10-24T08:15:30.123Z",
  "traceId": "a1b2c3d4-e5f6-4789-90ab-cdef12345678"
}
```
**Contoh Error (409):**
```json
{
  "success": false,
  "code": "EMAIL_ALREADY_EXISTS",
  "message": "Email sudah terdaftar",
  "details": [],
  "timestamp": "2026-10-24T08:15:30.123Z"
}
```

## 1.2 POST /api/v1/auth/login
- **Required role:** public
- **Headers:** `Content-Type: application/json`
- **Request Body:**

| Field | Tipe | Required |
|---|---|---|
| email | string(email) | ✅ |
| password | string | ✅ |

- **Response Body:** `{ accessToken, refreshToken, tokenType, expiresIn }`
- **Possible status:** 200, 400, 401, 422, 500

**Contoh Request:**
```json
{ "email": "budi.umkm@example.id", "password": "Koperasi#2026Aman" }
```
**Contoh Response (200):**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900
  },
  "message": "Login berhasil",
  "timestamp": "2026-10-24T08:15:30.123Z",
  "traceId": "b2c3d4e5-f6a7-4890-ab12-cdef23456789"
}
```
**Contoh Error (401):**
```json
{
  "success": false,
  "code": "INVALID_CREDENTIALS",
  "message": "Email atau kata sandi salah",
  "details": [],
  "timestamp": "2026-10-24T08:15:30.123Z"
}
```

## 1.3 POST /api/v1/auth/refresh
- **Required role:** public (butuh refresh token valid)
- **Headers:** `Content-Type: application/json`
- **Request Body:**

| Field | Tipe | Required |
|---|---|---|
| refreshToken | string | ✅ |

- **Response Body:** `{ accessToken, refreshToken, tokenType, expiresIn }` (rotation)
- **Possible status:** 200, 400, 401, 500

**Contoh Request:**
```json
{ "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." }
```
**Contoh Response (200):**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJ...newAccess",
    "refreshToken": "eyJ...newRefresh",
    "tokenType": "Bearer",
    "expiresIn": 900
  },
  "message": "Token diperbarui",
  "timestamp": "2026-10-24T08:16:00.000Z",
  "traceId": "c3d4e5f6-a7b8-4901-bc23-def345678901"
}
```

## 1.4 POST /api/v1/auth/logout
- **Required role:** AUTHENTICATED
- **Headers:** `Authorization: Bearer <accessToken>`
- **Request Body:** (kosong) atau `{ "refreshToken": "..." }`
- **Possible status:** 204, 401, 500

**Contoh Response (204):** (no content)

## 1.5 GET /api/v1/auth/me
- **Required role:** AUTHENTICATED
- **Headers:** `Authorization: Bearer <accessToken>`
- **Response Body:** profil pengguna + roles
- **Possible status:** 200, 401, 500

**Contoh Response (200):**
```json
{
  "success": true,
  "data": {
    "userId": "3f9c1a2b-7d4e-4c8a-9f10-2b3c4d5e6f70",
    "email": "budi.umkm@example.id",
    "phone": "+6281234567890",
    "status": "ACTIVE",
    "roles": ["BORROWER"],
    "createdAt": "2026-10-01T09:00:00.000Z"
  },
  "message": "OK",
  "timestamp": "2026-10-24T08:17:00.000Z",
  "traceId": "d4e5f6a7-b8c9-4012-cd34-ef4567890123"
}
```

---

# 2. borrower-profile — `/api/v1/borrowers`

## 2.1 POST /api/v1/borrowers/profile
- **Required role:** BORROWER (create/update)
- **Headers:** `Authorization: Bearer <accessToken>`, `Content-Type: application/json`
- **Request Body:**

| Field | Tipe | Required | Keterangan |
|---|---|---|---|
| nik | string | ✅ | 16 digit (dienkripsi) |
| npwp | string | ❌ | 15–16 digit (dienkripsi) |
| fullName | string | ✅ | max 255 |
| dateOfBirth | string(date) | ✅ | YYYY-MM-DD, masa lalu |
| gender | string(enum) | ✅ | M \| F |
| address | string | ✅ | max 500 |
| city | string | ✅ | max 100 |
| province | string | ✅ | max 100 |
| bankName | string | ✅ | max 100 |
| bankAccount | string | ✅ | dienkripsi |
| incomeMonthly | number(decimal) | ✅ | >= 0 |
| businessType | string | ✅ | max 100 |
| businessName | string | ✅ | max 255 |

- **Possible status:** 200, 201, 400, 401, 403, 422, 500

**Contoh Request:**
```json
{
  "nik": "3273012509900001",
  "npwp": "091234567890000",
  "fullName": "Budi Santoso",
  "dateOfBirth": "1990-09-25",
  "gender": "M",
  "address": "Jl. Merdeka No. 10, RT 01/RW 02",
  "city": "Bandung",
  "province": "Jawa Barat",
  "bankName": "BCA",
  "bankAccount": "1234567890",
  "incomeMonthly": 12000000.00,
  "businessType": "Kuliner",
  "businessName": "Warung Budi Jaya"
}
```
**Contoh Response (201):**
```json
{
  "success": true,
  "data": {
    "profileId": "7a8b9c0d-1e2f-4a3b-8c4d-5e6f7a8b9c0d",
    "fullName": "Budi Santoso",
    "isVerified": false
  },
  "message": "Profil tersimpan, data sensitif dienkripsi",
  "timestamp": "2026-10-24T08:20:00.000Z",
  "traceId": "e5f6a7b8-c9d0-4123-de45-f56789012345"
}
```

## 2.2 GET /api/v1/borrowers/profile
- **Required role:** BORROWER (own)
- **Headers:** `Authorization: Bearer <accessToken>`
- **Response Body:** profil (data sensitif di-*mask*)
- **Possible status:** 200, 401, 403, 404, 500

**Contoh Response (200):**
```json
{
  "success": true,
  "data": {
    "profileId": "7a8b9c0d-1e2f-4a3b-8c4d-5e6f7a8b9c0d",
    "fullName": "Budi Santoso",
    "nik": "327301********0001",
    "npwp": "09********0000",
    "bankName": "BCA",
    "bankAccount": "******7890",
    "city": "Bandung",
    "province": "Jawa Barat",
    "businessName": "Warung Budi Jaya",
    "incomeMonthly": 12000000.00,
    "isVerified": true
  },
  "message": "OK",
  "timestamp": "2026-10-24T08:21:00.000Z",
  "traceId": "f6a7b8c9-d0e1-4234-ef56-6789012345ab"
}
```

## 2.3 POST /api/v1/borrowers/profile/verify-ktp
- **Required role:** BORROWER (upload KTP)
- **Headers:** `Authorization: Bearer <accessToken>`, `Content-Type: multipart/form-data`
- **Request Body (multipart):**

| Field | Tipe | Required | Keterangan |
|---|---|---|---|
| ktpPhoto | file (image) | ✅ | JPEG/PNG, max 5MB |
| selfiePhoto | file (image) | ✅ | JPEG/PNG, max 5MB |

- **Possible status:** 202, 400, 401, 403, 413, 422, 500

**Contoh Response (202):**
```json
{
  "success": true,
  "data": {
    "verificationId": "9c0d1e2f-3a4b-4c5d-9e6f-7a8b9c0d1e2f",
    "status": "PENDING",
    "ktpPhotoUrl": "s3://koperasi-docs/ktp/enc-xxxx",
    "selfiePhotoUrl": "s3://koperasi-docs/selfie/enc-yyyy"
  },
  "message": "Dokumen diunggah, menunggu verifikasi",
  "timestamp": "2026-10-24T08:22:00.000Z",
  "traceId": "a7b8c9d0-e1f2-4345-fa67-789012345abc"
}
```

---

# 3. loan-service — `/api/v1/loans`

## 3.1 GET /api/v1/loans/products
- **Required role:** public
- **Query params:** `?active=true`
- **Possible status:** 200, 500

**Contoh Response (200):**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "Modal Usaha Mikro",
      "minAmount": 1000000.00,
      "maxAmount": 50000000.00,
      "interestRateAnnual": 18.00,
      "minTenorMonths": 3,
      "maxTenorMonths": 24,
      "isActive": true
    }
  ],
  "message": "OK",
  "timestamp": "2026-10-24T08:25:00.000Z",
  "traceId": "b8c9d0e1-f2a3-4456-ab78-89012345abcd"
}
```

## 3.2 POST /api/v1/loans/applications
- **Required role:** BORROWER
- **Headers:** `Authorization: Bearer <accessToken>`, `Content-Type: application/json`, `Idempotency-Key: <key>`
- **Request Body:**

| Field | Tipe | Required | Keterangan |
|---|---|---|---|
| loanProductId | integer | ✅ | referensi produk |
| amount | number(decimal) | ✅ | dalam rentang produk |
| tenorMonths | integer | ✅ | 1–60, dalam rentang produk |
| purpose | string | ✅ | max 1000 |

- **Possible status:** 201, 400, 401, 403, 409, 422, 500

**Contoh Request:**
```json
{
  "loanProductId": 1,
  "amount": 15000000.00,
  "tenorMonths": 12,
  "purpose": "Pembelian peralatan dapur untuk ekspansi warung"
}
```
**Contoh Response (201):**
```json
{
  "success": true,
  "data": {
    "loanId": "c9d0e1f2-a3b4-4567-bc89-9012345abcde",
    "status": "PENDING",
    "amount": 15000000.00,
    "tenorMonths": 12,
    "submittedAt": "2026-10-24T08:30:00.000Z"
  },
  "message": "Pengajuan diterima dan masuk antrian scoring",
  "timestamp": "2026-10-24T08:30:00.000Z",
  "traceId": "c9d0e1f2-a3b4-4567-bc89-9012345abcde"
}
```
**Contoh Error (409 - idempotency replay / profil belum verifikasi):**
```json
{
  "success": false,
  "code": "PROFILE_NOT_VERIFIED",
  "message": "Profil peminjam belum terverifikasi",
  "details": ["Lengkapi verifikasi KTP sebelum mengajukan pinjaman"],
  "timestamp": "2026-10-24T08:30:00.000Z"
}
```

## 3.3 GET /api/v1/loans/applications
- **Required role:** BORROWER (own), REVIEWER (all), ADMIN (all)
- **Headers:** `Authorization: Bearer <accessToken>`
- **Query params:** `?status=UNDER_REVIEW&page=0&size=20`
- **Possible status:** 200, 401, 403, 500

**Contoh Response (200):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "loanId": "c9d0e1f2-a3b4-4567-bc89-9012345abcde",
        "borrowerName": "Budi Santoso",
        "amount": 15000000.00,
        "status": "UNDER_REVIEW",
        "submittedAt": "2026-10-24T08:30:00.000Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  },
  "message": "OK",
  "timestamp": "2026-10-24T08:35:00.000Z",
  "traceId": "d0e1f2a3-b4c5-4678-cd90-012345abcdef"
}
```

## 3.4 GET /api/v1/loans/applications/{id}
- **Required role:** BORROWER (own), REVIEWER, ADMIN
- **Headers:** `Authorization: Bearer <accessToken>`
- **Possible status:** 200, 401, 403, 404, 500

**Contoh Response (200):**
```json
{
  "success": true,
  "data": {
    "loanId": "c9d0e1f2-a3b4-4567-bc89-9012345abcde",
    "borrowerId": "7a8b9c0d-1e2f-4a3b-8c4d-5e6f7a8b9c0d",
    "loanProductId": 1,
    "amount": 15000000.00,
    "tenorMonths": 12,
    "purpose": "Pembelian peralatan dapur",
    "status": "UNDER_REVIEW",
    "reviewerId": null,
    "submittedAt": "2026-10-24T08:30:00.000Z",
    "version": 2
  },
  "message": "OK",
  "timestamp": "2026-10-24T08:36:00.000Z",
  "traceId": "e1f2a3b4-c5d6-4789-de01-12345abcdef0"
}
```

## 3.5 PATCH /api/v1/loans/applications/{id}/status
- **Required role:** REVIEWER, ADMIN
- **Headers:** `Authorization: Bearer <accessToken>`, `Content-Type: application/json`
- **Request Body:**

| Field | Tipe | Required | Keterangan |
|---|---|---|---|
| status | string(enum) | ✅ | APPROVED \| REJECTED |
| rejectionReason | string | ❌* | wajib bila REJECTED, max 500 |
| overrideScore | boolean | ❌ | tandai override AI |
| version | integer | ✅ | optimistic lock |

- **Possible status:** 200, 400, 401, 403, 404, 409, 422, 500

**Contoh Request:**
```json
{ "status": "APPROVED", "overrideScore": false, "version": 2 }
```
**Contoh Response (200):**
```json
{
  "success": true,
  "data": {
    "loanId": "c9d0e1f2-a3b4-4567-bc89-9012345abcde",
    "status": "APPROVED",
    "reviewerId": "11112222-3333-4444-5555-666677778888",
    "reviewedAt": "2026-10-24T09:00:00.000Z",
    "version": 3
  },
  "message": "Status pinjaman diperbarui",
  "timestamp": "2026-10-24T09:00:00.000Z",
  "traceId": "f2a3b4c5-d6e7-4890-ef12-2345abcdef01"
}
```
**Contoh Error (409 - optimistic lock):**
```json
{
  "success": false,
  "code": "CONCURRENT_MODIFICATION",
  "message": "Data telah diubah oleh proses lain, muat ulang",
  "details": ["expectedVersion=2, actualVersion=3"],
  "timestamp": "2026-10-24T09:00:00.000Z"
}
```

## 3.6 GET /api/v1/loans/applications/{id}/installments
- **Required role:** BORROWER (own), REVIEWER, ADMIN
- **Headers:** `Authorization: Bearer <accessToken>`
- **Possible status:** 200, 401, 403, 404, 500

**Contoh Response (200):**
```json
{
  "success": true,
  "data": [
    {
      "installmentId": "aa11bb22-cc33-4dd4-8ee5-ff6677889900",
      "installmentNumber": 1,
      "dueDate": "2026-11-24",
      "amount": 1375000.00,
      "principal": 1250000.00,
      "interest": 125000.00,
      "status": "PENDING",
      "paidAt": null
    }
  ],
  "message": "OK",
  "timestamp": "2026-10-24T09:05:00.000Z",
  "traceId": "a3b4c5d6-e7f8-4901-fa23-345abcdef012"
}
```

## 3.7 GET /api/v1/loans/applications/{id}/score
- **Required role:** BORROWER (own), REVIEWER, ADMIN
- **Headers:** `Authorization: Bearer <accessToken>`
- **Possible status:** 200, 401, 403, 404, 500

**Contoh Response (200):**
```json
{
  "success": true,
  "data": {
    "scoreId": "bb22cc33-dd44-4ee5-9ff6-001122334455",
    "loanApplicationId": "c9d0e1f2-a3b4-4567-bc89-9012345abcde",
    "score": 78.50,
    "riskCategory": "MEDIUM",
    "approvalRecommendation": "REVIEW",
    "modelVersion": "v2.3.1",
    "processingTimeMs": 420,
    "features": {
      "income_to_loan_ratio": 0.8,
      "business_age_months": 36,
      "payment_history_score": 0.72
    },
    "scoredAt": "2026-10-24T08:32:00.000Z"
  },
  "message": "OK",
  "timestamp": "2026-10-24T08:40:00.000Z",
  "traceId": "b4c5d6e7-f8a9-4012-ab34-45abcdef0123"
}
```

---

# 4. payment-service — `/api/v1/payments`

## 4.1 POST /api/v1/payments/installments/{id}/pay
- **Required role:** BORROWER
- **Headers:** `Authorization: Bearer <accessToken>`, `Content-Type: application/json`, `Idempotency-Key: <key>`
- **Request Body:**

| Field | Tipe | Required | Keterangan |
|---|---|---|---|
| paymentGateway | string(enum) | ✅ | MIDTRANS \| XENDIT |
| amount | number(decimal) | ✅ | sesuai cicilan |

- **Possible status:** 201, 400, 401, 403, 404, 409, 422, 500

**Contoh Request:**
```json
{ "paymentGateway": "MIDTRANS", "amount": 1375000.00 }
```
**Contoh Response (201):**
```json
{
  "success": true,
  "data": {
    "paymentId": "cc33dd44-ee55-4ff6-a007-112233445566",
    "installmentId": "aa11bb22-cc33-4dd4-8ee5-ff6677889900",
    "status": "PENDING",
    "paymentGateway": "MIDTRANS",
    "paymentUrl": "https://app.midtrans.com/snap/v2/vtweb/xxxx",
    "externalTransactionId": "MID-TXN-99887766"
  },
  "message": "Pembayaran diinisiasi, lanjutkan ke payment gateway",
  "timestamp": "2026-10-24T10:00:00.000Z",
  "traceId": "c5d6e7f8-a9b0-4123-bc45-56abcdef0124"
}
```

## 4.2 GET /api/v1/payments/installments/{id}/status
- **Required role:** BORROWER, REVIEWER
- **Headers:** `Authorization: Bearer <accessToken>`
- **Possible status:** 200, 401, 403, 404, 500

**Contoh Response (200):**
```json
{
  "success": true,
  "data": {
    "installmentId": "aa11bb22-cc33-4dd4-8ee5-ff6677889900",
    "installmentStatus": "PAID",
    "paymentStatus": "SUCCESS",
    "paidAt": "2026-10-24T10:05:00.000Z",
    "externalTransactionId": "MID-TXN-99887766"
  },
  "message": "OK",
  "timestamp": "2026-10-24T10:10:00.000Z",
  "traceId": "d6e7f8a9-b0c1-4234-cd56-67abcdef0125"
}
```

## 4.3 POST /api/v1/payments/webhook/midtrans
- **Required role:** public (Midtrans server, verifikasi signature)
- **Headers:** `Content-Type: application/json`, header signature Midtrans
- **Request Body:** payload notifikasi Midtrans (bervariasi)
- **Possible status:** 200, 400, 401 (signature invalid), 500

**Contoh Request:**
```json
{
  "transaction_time": "2026-10-24 10:05:00",
  "transaction_status": "settlement",
  "transaction_id": "MID-TXN-99887766",
  "order_id": "cc33dd44-ee55-4ff6-a007-112233445566",
  "gross_amount": "1375000.00",
  "signature_key": "sha512-hash...",
  "status_code": "200"
}
```
**Contoh Response (200):**
```json
{
  "success": true,
  "data": { "processed": true },
  "message": "Webhook diproses",
  "timestamp": "2026-10-24T10:05:01.000Z",
  "traceId": "e7f8a9b0-c1d2-4345-de67-78abcdef0126"
}
```

## 4.4 POST /api/v1/payments/webhook/xendit
- **Required role:** public (Xendit server, verifikasi callback token)
- **Headers:** `Content-Type: application/json`, `X-Callback-Token`
- **Request Body:** payload callback Xendit (bervariasi)
- **Possible status:** 200, 400, 401, 500

**Contoh Request:**
```json
{
  "id": "XEN-TXN-55443322",
  "external_id": "cc33dd44-ee55-4ff6-a007-112233445566",
  "status": "PAID",
  "amount": 1375000,
  "paid_at": "2026-10-24T10:05:00.000Z"
}
```
**Contoh Response (200):**
```json
{
  "success": true,
  "data": { "processed": true },
  "message": "Webhook diproses",
  "timestamp": "2026-10-24T10:05:01.000Z",
  "traceId": "f8a9b0c1-d2e3-4456-ef78-89abcdef0127"
}
```

---

# 5. core-service (modul notification) — `/api/v1/notifications`

## 5.1 GET /api/v1/notifications
- **Required role:** AUTHENTICATED
- **Headers:** `Authorization: Bearer <accessToken>`
- **Query params:** `?isRead=false&page=0&size=20`
- **Possible status:** 200, 401, 500

**Contoh Response (200):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "a9b0c1d2-e3f4-4567-fa89-9abcdef01280",
        "title": "Pinjaman Disetujui",
        "body": "Pengajuan pinjaman Anda telah disetujui",
        "type": "LOAN_STATUS",
        "referenceId": "c9d0e1f2-a3b4-4567-bc89-9012345abcde",
        "isRead": false,
        "createdAt": "2026-10-24T09:00:05.000Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "unreadCount": 1
  },
  "message": "OK",
  "timestamp": "2026-10-24T09:10:00.000Z",
  "traceId": "a9b0c1d2-e3f4-4567-fa89-9abcdef01280"
}
```

## 5.2 PATCH /api/v1/notifications/{id}/read
- **Required role:** AUTHENTICATED (own)
- **Headers:** `Authorization: Bearer <accessToken>`
- **Possible status:** 200, 401, 403, 404, 500

**Contoh Response (200):**
```json
{
  "success": true,
  "data": { "id": "a9b0c1d2-e3f4-4567-fa89-9abcdef01280", "isRead": true },
  "message": "Notifikasi ditandai dibaca",
  "timestamp": "2026-10-24T09:11:00.000Z",
  "traceId": "b0c1d2e3-f4a5-4678-ab90-abcdef012801"
}
```

## 5.3 PATCH /api/v1/notifications/read-all
- **Required role:** AUTHENTICATED
- **Headers:** `Authorization: Bearer <accessToken>`
- **Possible status:** 200, 401, 500

**Contoh Response (200):**
```json
{
  "success": true,
  "data": { "markedCount": 5 },
  "message": "Semua notifikasi ditandai dibaca",
  "timestamp": "2026-10-24T09:12:00.000Z",
  "traceId": "c1d2e3f4-a5b6-4789-bc01-bcdef0128012"
}
```

## 5.4 GET /api/v1/notifications/stream
- **Required role:** AUTHENTICATED (SSE endpoint)
- **Headers:** `Authorization: Bearer <accessToken>`, `Accept: text/event-stream`
- **Response:** `text/event-stream` (koneksi persisten)
- **Possible status:** 200 (stream), 401, 500

**Contoh Response Stream:**
```
event: notification
data: {"id":"a9b0c1d2...","title":"Pembayaran Diterima","type":"PAYMENT","createdAt":"2026-10-24T10:05:10.000Z"}

event: heartbeat
data: {"ts":"2026-10-24T10:05:40.000Z"}
```

---

# 6. core-service (modul reporting) — `/api/v1/reports`

## 6.1 GET /api/v1/reports/dashboard
- **Required role:** ADMIN, REVIEWER
- **Headers:** `Authorization: Bearer <accessToken>`
- **Query params:** `?from=2026-01-01&to=2026-10-24`
- **Possible status:** 200, 401, 403, 500

**Contoh Response (200):**
```json
{
  "success": true,
  "data": {
    "totalLoans": 1240,
    "totalDisbursed": 18750000000.00,
    "activeLoans": 820,
    "completedLoans": 360,
    "defaultedLoans": 60,
    "nplRatio": 4.84,
    "loansByStatus": {
      "PENDING": 25, "SCORING": 10, "UNDER_REVIEW": 40,
      "APPROVED": 15, "REJECTED": 90, "DISBURSED": 20,
      "ACTIVE": 820, "COMPLETED": 360, "DEFAULT": 60
    }
  },
  "message": "OK",
  "timestamp": "2026-10-24T11:00:00.000Z",
  "traceId": "d2e3f4a5-b6c7-4890-cd12-cdef01280123"
}
```

## 6.2 GET /api/v1/reports/loans
- **Required role:** ADMIN, REVIEWER
- **Headers:** `Authorization: Bearer <accessToken>`
- **Query params:** `?status=ACTIVE&from=...&to=...&page=0&size=50`
- **Possible status:** 200, 401, 403, 500

**Contoh Response (200):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "loanId": "c9d0e1f2-a3b4-4567-bc89-9012345abcde",
        "borrowerName": "Budi Santoso",
        "amount": 15000000.00,
        "status": "ACTIVE",
        "disbursedAt": "2026-10-24T09:30:00.000Z",
        "outstandingAmount": 13750000.00
      }
    ],
    "page": 0,
    "size": 50,
    "totalElements": 820,
    "totalPages": 17
  },
  "message": "OK",
  "timestamp": "2026-10-24T11:05:00.000Z",
  "traceId": "e3f4a5b6-c7d8-4901-de23-def012801234"
}
```

## 6.3 GET /api/v1/reports/loans/export
- **Required role:** ADMIN
- **Headers:** `Authorization: Bearer <accessToken>`, `Accept: text/csv` atau `application/pdf`
- **Query params:** `?format=csv&status=ACTIVE&from=...&to=...`
- **Response:** file binary (CSV/PDF), `Content-Disposition: attachment`
- **Possible status:** 200 (file), 401, 403, 422, 500

**Contoh Response Headers (200):**
```
Content-Type: text/csv
Content-Disposition: attachment; filename="loans-report-2026-10-24.csv"
```
**Contoh Error (403):**
```json
{
  "success": false,
  "code": "FORBIDDEN",
  "message": "Hanya ADMIN yang dapat mengekspor laporan",
  "details": [],
  "timestamp": "2026-10-24T11:10:00.000Z"
}
```

---

## Appendix — Error Code Reference

| Code | HTTP | Deskripsi |
|---|---|---|
| `VALIDATION_ERROR` | 400/422 | Payload tidak valid |
| `INVALID_CREDENTIALS` | 401 | Email/password salah |
| `TOKEN_EXPIRED` | 401 | Access token kedaluwarsa |
| `TOKEN_REVOKED` | 401 | Token di-blacklist |
| `FORBIDDEN` | 403 | Role tidak memadai |
| `RESOURCE_NOT_FOUND` | 404 | Entitas tidak ditemukan |
| `EMAIL_ALREADY_EXISTS` | 409 | Email duplikat |
| `IDEMPOTENCY_CONFLICT` | 409 | Operasi sedang diproses |
| `CONCURRENT_MODIFICATION` | 409 | Optimistic lock gagal |
| `PROFILE_NOT_VERIFIED` | 422 | Profil belum terverifikasi |
| `INVALID_STATE_TRANSITION` | 422 | Transisi status tidak diizinkan |
| `PAYMENT_GATEWAY_ERROR` | 502/500 | Kesalahan payment gateway |
| `INTERNAL_ERROR` | 500 | Kesalahan server |
