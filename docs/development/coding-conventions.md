<!-- FILE: docs/development/coding-conventions.md -->
# Coding Conventions
## Platform Koperasi Digital / P2P Lending untuk UMKM Indonesia

| Atribut | Nilai |
|---|---|
| Dokumen | Coding Conventions |
| Versi | 1.0 |
| Tanggal | 2026-10-24 |
| Backend | Java 21, Spring Boot 3.x, Gradle |
| Frontend | React 19 + JavaScript, Next.js |

---

## A. Java / Spring Boot

### A.1 Package Structure
Setiap service mengikuti struktur paket konsisten `com.koperasi.{service}`:

```
com.koperasi.{service}.controller        // REST controllers (@RestController)
com.koperasi.{service}.service           // business logic (interface + impl)
com.koperasi.{service}.repository        // Spring Data JPA repositories
com.koperasi.{service}.domain.entity     // JPA entities (@Entity)
com.koperasi.{service}.domain.dto        // request/response DTO (records)
com.koperasi.{service}.domain.enums      // enum domain (LoanStatus, RiskCategory, dll)
com.koperasi.{service}.config            // Spring config (@Configuration)
com.koperasi.{service}.exception         // custom exceptions + @RestControllerAdvice
com.koperasi.{service}.event             // Kafka producers/consumers
com.koperasi.{service}.util              // utilitas/helper stateless
```

Contoh: `com.koperasi.loan.controller.LoanApplicationController`.

### A.2 Naming Conventions
| Elemen | Konvensi | Contoh |
|---|---|---|
| Class | PascalCase, kata benda | `LoanApplicationService` |
| Interface | PascalCase (tanpa prefix `I`) | `ScoringClient` |
| Implementation | `{Interface}Impl` | `ScoringClientImpl` |
| Method | camelCase, kata kerja | `approveApplication()` |
| Variable | camelCase | `loanApplication` |
| Constant | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| Enum value | UPPER_SNAKE_CASE | `UNDER_REVIEW` |
| Test class | `{ClassUnderTest}Test` | `LoanApplicationServiceTest` |
| Integration test | `{ClassUnderTest}IT` | `LoanApplicationControllerIT` |
| REST path | kebab-case, plural | `/api/v1/loan-applications` |
| DB table/column | snake_case | `loan_applications`, `created_at` |

### A.3 Exception Hierarchy
```
KoperasiException (abstract, RuntimeException)
├── BusinessException (abstract, HTTP 4xx)
│   ├── LoanNotFoundException            (404)
│   ├── InsufficientScoreException       (422)
│   ├── DuplicateApplicationException    (409)
│   ├── PaymentGatewayException          (502)
│   ├── ProfileNotVerifiedException      (422)
│   ├── InvalidStateTransitionException  (422)
│   └── ConcurrentModificationException  (409)
└── TechnicalException (abstract, HTTP 5xx)
    ├── DatabaseException                (500)
    ├── KafkaPublishException            (500)
    └── ExternalServiceException         (503)
```

```java
public abstract class KoperasiException extends RuntimeException {
    private final String code;
    private final HttpStatus status;
    protected KoperasiException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }
    public String getCode() { return code; }
    public HttpStatus getStatus() { return status; }
}

public class LoanNotFoundException extends BusinessException {
    public LoanNotFoundException(String loanId) {
        super("RESOURCE_NOT_FOUND", "Pinjaman tidak ditemukan: " + loanId, HttpStatus.NOT_FOUND);
    }
}
```

### A.4 Standard Response Wrapper
```java
public record ApiResponse<T>(
    boolean success,
    T data,
    String message,
    String timestamp,   // ISO8601
    String traceId
) {
    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message,
            Instant.now().toString(), MDC.get("traceId"));
    }
}
```

### A.5 Error Response Format
```java
public record ErrorResponse(
    boolean success,       // selalu false
    String code,
    String message,
    List<String> details,
    String timestamp
) {}
```
Ditangani terpusat via `@RestControllerAdvice`:
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(ex.getStatus())
            .body(new ErrorResponse(false, ex.getCode(), ex.getMessage(),
                List.of(), Instant.now().toString()));
    }
}
```

### A.6 Logging (SLF4J + MDC)
- Gunakan **SLF4J** (`private static final Logger log = LoggerFactory.getLogger(...)`), bukan `System.out`.
- **MDC wajib** mengandung `traceId`, `userId`, `serviceId` pada setiap log (di-*set* oleh filter/interceptor, dibersihkan di `finally`).
- Jangan log data sensitif (NIK, NPWP, password, token) — *mask* bila perlu.

```java
MDC.put("traceId", traceId);
MDC.put("userId", userId);
MDC.put("serviceId", "loan-service");
try {
    log.info("Approving loan application id={}", loanId);
} finally {
    MDC.clear();
}
```
Pola pesan: `log.<level>("Verb object key={}", value)` dengan parameterized logging (bukan konkatenasi string).

### A.7 @Transactional Rules
| Aturan | Panduan |
|---|---|
| Letak | Di **service layer**, bukan controller/repository |
| Read-only | `@Transactional(readOnly = true)` untuk query |
| Propagation | Default `REQUIRED`; `REQUIRES_NEW` untuk operasi audit/log independen |
| Isolation | Default `READ_COMMITTED`; `REPEATABLE_READ` untuk operasi finansial kritikal |
| Rollback | Rollback pada `KoperasiException` (unchecked); hindari *catch-and-swallow* |
| Scope | Transaksi sependek mungkin; **jangan** panggil REST/Kafka eksternal di dalam transaksi DB (gunakan outbox pattern) |

```java
@Transactional(isolation = Isolation.REPEATABLE_READ, propagation = Propagation.REQUIRED)
public LoanApplicationResponse approve(String id, ApproveRequest req) { ... }
```

### A.8 DTO Naming
- Request: `{Verb}{Entity}Request` → `CreateLoanApplicationRequest`, `ApproveLoanRequest`.
- Response: `{Entity}Response` → `LoanApplicationResponse`, `CreditScoreResponse`.
- Gunakan **Java `record`** untuk DTO (immutable); validasi via Bean Validation annotations.
- Jangan ekspos entity JPA langsung ke controller.

---

## B. React / JavaScript

### B.1 Folder Structure (Feature-Based)
```
src/
├── features/
│   └── loan/
│       ├── components/     // LoanApplicationForm.jsx, LoanStatusBadge.jsx
│       ├── hooks/          // useLoanApplication.js
│       ├── services/       // loanService.js (axios)
│       └── store/          // useLoanStore.js (Zustand)
├── shared/
│   ├── components/         // Button.jsx, Modal.jsx
│   ├── hooks/              // useDebounce.js
│   ├── lib/                // axiosClient.js, queryClient.js
│   └── utils/              // formatters.js
└── app/                    // Next.js app router pages
```

### B.2 Naming
| Elemen | Konvensi | Contoh |
|---|---|---|
| Component | PascalCase + `.jsx` | `LoanApplicationForm.jsx` |
| Custom hook | `use{Name}` | `useLoanApplication`, `useAuth` |
| Zustand store | `use{Name}Store` | `useAuthStore`, `useLoanStore` |
| Service file | `{domain}Service.js` | `loanService.js` |
| Util | camelCase | `formatCurrency` |
| Constant | UPPER_SNAKE_CASE | `DEFAULT_PAGE_SIZE` |

### B.3 Custom Hooks
```js
// features/loan/hooks/useLoanApplication.js
export function useLoanApplication(loanId) {
  return useQuery({
    queryKey: loanKeys.detail(loanId),
    queryFn: () => loanService.getById(loanId),
    enabled: !!loanId,
  });
}
```

### B.4 Zustand Store
```js
// features/auth/store/useAuthStore.js
import { create } from 'zustand';

export const useAuthStore = create((set) => ({
  user: null,
  accessToken: null,
  setAuth: ({ user, accessToken }) => set({ user, accessToken }),
  logout: () => set({ user: null, accessToken: null }),
}));
```

### B.5 TanStack Query Key Factory
```js
// features/loan/hooks/loanKeys.js
export const loanKeys = {
  all: ['loans'],
  lists: () => [...loanKeys.all, 'list'],
  list: (filters) => [...loanKeys.lists(), filters],
  details: () => [...loanKeys.all, 'detail'],
  detail: (id) => [...loanKeys.details(), id],
};
```

### B.6 Axios Service Layer
- **Tidak boleh** `fetch`/`axios` langsung di dalam component. Semua panggilan API lewat service layer.
```js
// shared/lib/axiosClient.js
import axios from 'axios';
export const axiosClient = axios.create({ baseURL: '/api/v1', timeout: 10000 });
axiosClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// features/loan/services/loanService.js
import { axiosClient } from '@/shared/lib/axiosClient';
export const loanService = {
  getById: (id) => axiosClient.get(`/loans/applications/${id}`).then((r) => r.data.data),
  create: (payload, idempotencyKey) =>
    axiosClient.post('/loans/applications', payload,
      { headers: { 'Idempotency-Key': idempotencyKey } }).then((r) => r.data.data),
};
```

### B.7 Error Handling
- **Error Boundary** global untuk menangkap render error → tampilkan fallback UI.
- **Centralized error toast** via interceptor Axios + handler TanStack Query.
```js
axiosClient.interceptors.response.use(
  (res) => res,
  (error) => {
    const msg = error.response?.data?.message ?? 'Terjadi kesalahan';
    toast.error(msg);
    return Promise.reject(error);
  }
);
```

---

## C. Git Commit (Conventional Commits)

### C.1 Format
```
{type}({scope}): {description}

[optional body]

[optional footer]
```

### C.2 Types
| Type | Penggunaan |
|---|---|
| `feat` | Fitur baru |
| `fix` | Perbaikan bug |
| `docs` | Perubahan dokumentasi |
| `style` | Format/whitespace (tanpa ubah logika) |
| `refactor` | Refactor tanpa ubah perilaku |
| `test` | Menambah/memperbaiki test |
| `chore` | Maintenance (dependency, config) |
| `perf` | Peningkatan performa |
| `ci` | Perubahan pipeline CI/CD |

### C.3 Contoh
```
feat(loan): add state machine transition validation
fix(payment): handle duplicate midtrans webhook via idempotency key
docs(api): update loan application endpoint contract
perf(scoring): cache feature vector in redis
```

### C.4 Breaking Change
Tanda `!` setelah scope, dan/atau footer `BREAKING CHANGE:`:
```
feat(auth)!: change JWT payload structure

BREAKING CHANGE: field `roles` kini array of object, bukan array of string.
```

### C.5 Body & Footer
```
fix(loan): prevent double disbursement on retry

Menambahkan pengecekan idempotency key sebelum memanggil payment gateway
untuk mencegah pencairan ganda saat request di-retry.

Closes #123
```

---

## D. Pull Request Template

```markdown
## Deskripsi
<!-- Ringkas perubahan & alasannya -->

## Tipe Perubahan
- [ ] feat (fitur baru)
- [ ] fix (perbaikan bug)
- [ ] refactor
- [ ] docs
- [ ] test
- [ ] chore/ci

## Ticket Terkait
Closes KOPERASI-###

## Checklist
- [ ] Kode mengikuti coding conventions (Java/React)
- [ ] Commit mengikuti Conventional Commits
- [ ] Unit test ditambahkan/diperbarui (coverage ≥ 80%)
- [ ] Integration test (bila relevan)
- [ ] Tidak ada data sensitif di log (NIK/NPWP/token)
- [ ] Operasi finansial memakai idempotency key
- [ ] Dokumentasi/API contract diperbarui bila perlu
- [ ] Tidak ada breaking change TANPA penandaan `!`
- [ ] Self-review dilakukan
- [ ] CI pipeline hijau (build, test, lint, coverage)

## Cara Pengujian
<!-- Langkah verifikasi manual/otomatis -->

## Screenshot / Bukti (bila UI)
<!-- opsional -->

## Catatan Reviewer
<!-- Area yang perlu perhatian khusus -->
```
