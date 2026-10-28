<!-- FILE: docs/development/testing-strategy.md -->
# Testing Strategy
## Platform Koperasi Digital / P2P Lending untuk UMKM Indonesia

| Atribut | Nilai |
|---|---|
| Dokumen | Testing Strategy |
| Versi | 1.0 |
| Tanggal | 2026-10-24 |
| Backend | JUnit 5, Mockito, TestContainers |
| Frontend | Jest, React Testing Library, MSW |

---

## 1. Testing Pyramid

```
        /\        E2E (10%)  — alur kritikal end-to-end
       /  \
      /----\      Integration (20%) — service + DB/Kafka/Redis nyata
     /      \
    /--------\    Unit (70%) — logika bisnis terisolasi
```

| Level | Proporsi | Fokus |
|---|---|---|
| Unit | **70%** | Logika bisnis, validasi, state machine, util |
| Integration | **20%** | Interaksi service ↔ DB/messaging/cache |
| E2E | **10%** | Alur kritikal (apply → scoring → approve → disburse → pay) |

### Coverage Targets
| Jenis | Target |
|---|---|
| Unit coverage | **≥ 80%** |
| Integration coverage | **≥ 60%** |
| Frontend (Jest/Istanbul) | **≥ 75% statements** |

---

## 2. JUnit 5 + Mockito (Backend)

### 2.1 Test Naming
Format **`{MethodName}_{Scenario}_{ExpectedResult}`**:
```java
approve_whenScoreBelowThreshold_throwsInsufficientScoreException()
createApplication_withDuplicateIdempotencyKey_returnsCachedResponse()
transition_fromPendingToActive_throwsInvalidStateTransitionException()
```

### 2.2 Structure (Given-When-Then)
```java
@Test
void approve_whenApplicationUnderReview_updatesStatusToApproved() {
    // given
    var app = LoanApplication.builder().status(UNDER_REVIEW).version(2L).build();
    when(repository.findById("loan-1")).thenReturn(Optional.of(app));

    // when
    var result = service.approve("loan-1", new ApproveRequest(2L));

    // then
    assertThat(result.status()).isEqualTo("APPROVED");
    verify(eventPublisher).publish(any(LoanApprovedEvent.class));
}
```

### 2.3 Mock vs Spy
| Gunakan | Kapan |
|---|---|
| **`@Mock`** | Dependency eksternal yang di-*stub* penuh (repository, client, publisher). Default & paling umum. |
| **`@Spy`** | Object nyata yang sebagian method-nya ingin dipertahankan, hanya sebagian di-*override*. Hati-hati, gunakan terbatas (mis. util dengan satu method yang perlu di-stub). |
| **`@InjectMocks`** | Class under test yang menerima mock via konstruktor. |

### 2.4 Test Slice Selection
| Anotasi | Lingkup | Kecepatan |
|---|---|---|
| `@ExtendWith(MockitoExtension.class)` | Unit murni, tanpa Spring context | Cepat |
| `@WebMvcTest` | Controller layer saja | Sedang |
| `@DataJpaTest` | Repository layer saja | Sedang |
| `@SpringBootTest` | Full context + TestContainers | Lambat |

> Prinsip: pakai **Mockito murni** untuk service/unit; gunakan Spring slice hanya saat menguji integrasi lapisan terkait.

---

## 3. Spring Boot Test Layers

### 3.1 @WebMvcTest (Controller)
```java
@WebMvcTest(LoanApplicationController.class)
class LoanApplicationControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean LoanApplicationService service;

    @Test
    void getById_whenNotFound_returns404() throws Exception {
        when(service.getById("x")).thenThrow(new LoanNotFoundException("x"));
        mockMvc.perform(get("/api/v1/loans/applications/x"))
               .andExpect(status().isNotFound())
               .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
}
```

### 3.2 @DataJpaTest (Repository)
```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)   // pakai TestContainers PostgreSQL
class LoanApplicationRepositoryTest {
    @Autowired LoanApplicationRepository repository;

    @Test
    void findByStatus_returnsMatchingApplications() {
        repository.save(anApplication(UNDER_REVIEW));
        var result = repository.findByStatus(UNDER_REVIEW);
        assertThat(result).hasSize(1);
    }
}
```

### 3.3 @SpringBootTest + TestContainers (Full Integration)
```java
@SpringBootTest
@Testcontainers
class LoanDisbursementIT {
    @Container static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16");
    @Container static GenericContainer<?> redis = new GenericContainer<>("redis:7").withExposedPorts(6379);
    @Container static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", pg::getJdbcUrl);
        r.add("spring.data.redis.host", redis::getHost);
        r.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Test
    void disbursement_publishesCompletedEvent_andActivatesLoan() { /* ... */ }
}
```

### 3.4 TestContainers Setup
| Service | Image | Kegunaan |
|---|---|---|
| PostgreSQL | `postgres:16` | Repository & full integration |
| Redis | `redis:7` | Distributed lock, cache, idempotency |
| Kafka | `confluentinc/cp-kafka:7.6.0` | Producer/consumer event |

> Container di-*reuse* antar test class (`withReuse(true)`) untuk mempercepat suite di lokal.

---

## 4. Jest + React Testing Library (Frontend)

### 4.1 Component Testing (render + interaction + assertion)
```jsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { LoanApplicationForm } from './LoanApplicationForm';

test('menampilkan error saat amount melebihi batas produk', async () => {
  render(<LoanApplicationForm maxAmount={50000000} />);
  await userEvent.type(screen.getByLabelText(/jumlah/i), '60000000');
  await userEvent.click(screen.getByRole('button', { name: /ajukan/i }));
  expect(await screen.findByText(/melebihi batas/i)).toBeInTheDocument();
});
```

### 4.2 Hook Testing
```jsx
import { renderHook, waitFor } from '@testing-library/react';
import { useLoanApplication } from './useLoanApplication';

test('mengambil detail pinjaman', async () => {
  const { result } = renderHook(() => useLoanApplication('loan-1'), { wrapper });
  await waitFor(() => expect(result.current.isSuccess).toBe(true));
  expect(result.current.data.status).toBe('ACTIVE');
});
```

### 4.3 Mock Strategy — MSW
```js
// mocks/handlers.js
import { http, HttpResponse } from 'msw';
export const handlers = [
  http.get('/api/v1/loans/applications/:id', ({ params }) =>
    HttpResponse.json({ success: true, data: { loanId: params.id, status: 'ACTIVE' } })
  ),
];
```
- **MSW** mencegat panggilan API pada level jaringan → test dekat dengan perilaku nyata, tanpa mock Axios manual.

### 4.4 Coverage (Istanbul)
- Target **≥ 75% statements**; branch & function dipantau.
- Dikonfigurasi di `jest.config.js`:
```js
coverageThreshold: {
  global: { statements: 75, branches: 70, functions: 75, lines: 75 }
}
```

---

## 5. CI Testing Gates

| Gate | Aturan | Aksi bila gagal |
|---|---|---|
| Build | Kompilasi backend & frontend | Fail build |
| Unit test | Semua lulus | Fail build |
| Integration test | TestContainers suite lulus | Fail build |
| **Coverage threshold** | Unit ≥ 80%, FE ≥ 75% | **Fail build** |
| Lint/Format | Checkstyle/Spotless + ESLint/Prettier | Fail build |
| SAST | Pemindaian keamanan | Fail/warn sesuai severity |
| **Mutation testing (PITest)** | Mutation score ≥ 60% pada modul kritikal (loan, payment, scoring) | Fail build (modul kritikal) |

### 5.1 Mutation Testing (PITest)
- Menguji **kualitas** test (bukan sekadar coverage) dengan menyuntik mutasi kode dan memastikan test gagal.
- Fokus pada modul finansial/kritikal: state machine pinjaman, idempotency, kalkulasi cicilan.
- Target **mutation score ≥ 60%** untuk modul kritikal.

```groovy
// build.gradle
plugins { id 'info.solidsoft.pitest' version '1.15.0' }
pitest {
    targetClasses = ['com.koperasi.loan.service.*', 'com.koperasi.payment.service.*']
    mutationThreshold = 60
    timestampedReports = false
}
```

### 5.2 Pipeline (GitHub Actions) — ringkas
```yaml
jobs:
  test:
    steps:
      - run: ./gradlew test jacocoTestCoverageVerification pitest
      - run: npm ci && npm run test:coverage
      # Build gagal otomatis bila threshold coverage / mutation tidak terpenuhi
```
