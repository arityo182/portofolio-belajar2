# ML Model Specification — AI Credit Scoring
## Platform Koperasi Digital / P2P Lending untuk UMKM Indonesia

| Atribut | Nilai |
|---|---|
| Dokumen | ML Model & Serving Specification |
| Versi | 1.0 |
| Tanggal | 2026-10-24 |
| Stack | TensorFlow 2.x / Keras, FastAPI, PostgreSQL |
| Service | scoring-service (:8083) |

---

## 1. Problem Framing

### 1.1 Task Type
Model **multi-output** (hybrid regression + classification):
- **Regression head:** memprediksi `risk_score` kontinu 0.0–100.0.
- **Classification head:** memprediksi probabilitas gagal bayar (*default*) untuk menurunkan `risk_category` dan `approval_recommendation`.

Score kontinu memberi granularitas bagi reviewer, sementara klasifikasi default memberi kalibrasi probabilistik yang dapat dipetakan ke keputusan bisnis.

### 1.2 Label Definition & Business Rationale
- **Label utama (classification):** `is_default` biner — 1 bila peminjam gagal bayar (tunggakan melewati ambang DEFAULT, mis. ≥90 hari), 0 bila `COMPLETED` atau lancar.
- **Label turunan (regression):** `risk_score = (1 - P(default)) × 100`, dikalibrasi dengan performa pembayaran historis.
- **Rationale:** 60% UMKM tidak punya *credit history* bank formal, sehingga label berbasis **perilaku pembayaran internal platform + proksi kelayakan usaha** menjadi sinyal alternatif yang relevan dan dapat diaudit.

### 1.3 Success Metrics (Bisnis)
- **Target NPL (Non-Performing Loan) < 5%** pada portofolio yang disetujui model.
- **Approval rate** sehat (menghindari over-rejection yang merusak inklusi keuangan).
- **Default rate** pada kelas APPROVE < 3%.
- Keputusan **explainable** untuk kepatuhan OJK & UU PDP.

---

## 2. Feature Engineering (20 fitur mentah → 28 input model)

> Semua fitur dihitung dari PostgreSQL (loan_schema, payment_schema, auth_schema) dan disimpan di `features_json` untuk explainability.
>
> **Catatan serving:** `BASE_FEATURE_ORDER` berisi **20 fitur mentah**. Dua fitur kategorikal
> (`business_type_code`, `loan_purpose_code`) di-one-hot menjadi 5 kategori bisnis + 5 kategori
> tujuan, sehingga vektor yang diterima model adalah **18 fitur numerik + 10 fitur one-hot =
> 28 input** (`models/model_metadata.json` → `n_features: 28`).

### Group A — Financial Behavior (StandardScaler)

| Fitur | Deskripsi | Tipe | Range | Kalkulasi dari DB |
|---|---|---|---|---|
| `payment_consistency_score` | Rasio pembayaran tepat waktu | float | 0–100 | `(COUNT(payments PAID tepat waktu) / COUNT(installments jatuh tempo)) × 100` |
| `avg_monthly_transaction_amount` | Rata-rata transaksi/bulan (6 bln) | float | ≥0 | `SUM(payments.amount 6 bln) / 6` |
| `income_stability_index` | Variansi income bulanan (rendah = stabil) | float | ≥0 | `VARIANCE(monthly_income)` dinormalisasi; sumber transaksi/laporan |
| `debt_to_income_ratio` | Total cicilan existing / income | float | 0–∞ | `SUM(cicilan aktif) / borrower_profiles.income_monthly` |
| `loan_to_income_ratio` | Amount diminta / income tahunan | float | 0–∞ | `loan_applications.amount / (income_monthly × 12)` |

### Group B — Business Profile

| Fitur | Deskripsi | Tipe | Range | Kalkulasi dari DB |
|---|---|---|---|---|
| `business_age_months` | Umur usaha (bulan) | int | ≥0 | Dari profil/laporan usaha; `MONTHS_BETWEEN(now, business_start)` |
| `business_type_encoded` | One-hot tipe usaha | vector | {0,1} | One-hot `borrower_profiles.business_type` |
| `has_npwp` | Kepemilikan NPWP | int | 0/1 | `npwp_encrypted IS NOT NULL ? 1 : 0` |
| `has_formal_address` | Alamat formal terverifikasi | int | 0/1 | Verifikasi alamat; `1` bila valid |
| `monthly_revenue_to_income_ratio` | Omset vs income dilaporkan | float | ≥0 | `monthly_revenue / income_monthly` |

### Group C — Loan Request (MinMaxScaler)

| Fitur | Deskripsi | Tipe | Range | Kalkulasi dari DB |
|---|---|---|---|---|
| `requested_amount_normalized` | Amount / max produk | float | 0–1 | `amount / loan_products.max_amount` |
| `tenor_normalized` | Tenor / max tenor | float | 0–1 | `tenor_months / loan_products.max_tenor_months` |
| `loan_purpose_encoded` | One-hot tujuan pinjaman | vector | {0,1} | One-hot kategori `purpose` |
| `amount_to_annual_income_ratio` | Amount / income tahunan | float | 0–∞ | `amount / (income_monthly × 12)` |

### Group D — History

| Fitur | Deskripsi | Tipe | Range | Kalkulasi dari DB |
|---|---|---|---|---|
| `previous_loan_count` | Jumlah pinjaman sebelumnya | int | ≥0 | `COUNT(loan_applications WHERE borrower_id = x AND status IN terminal)` |
| `previous_default_count` | Jumlah default sebelumnya | int | ≥0 | `COUNT(... status = DEFAULT)` |
| `previous_ontime_payment_rate` | % cicilan lama tepat waktu | float | 0–100 | `(ontime_installments / total_past_installments) × 100` |

### Group E — Completeness

| Fitur | Deskripsi | Tipe | Range | Kalkulasi dari DB |
|---|---|---|---|---|
| `verification_completeness_score` | % dokumen terverifikasi | float | 0–100 | `(VERIFIED docs / required docs) × 100` (document_verifications) |
| `profile_completeness_score` | % profil terisi | float | 0–100 | `(field terisi / total field wajib) × 100` |

---

## 3. Data Preprocessing Pipeline

### 3.1 Missing Value Strategy
| Grup | Strategi |
|---|---|
| A — Financial Behavior | Imputasi median; `payment_consistency_score` default 50 untuk pemohon baru (netral) |
| B — Business Profile | `has_npwp`/`has_formal_address` → 0; kategorik → kategori "UNKNOWN" |
| C — Loan Request | Tidak boleh null (validasi di API); fallback error |
| D — History | Pemohon baru → 0 untuk count & `previous_ontime_payment_rate` = 0 (ditandai flag `is_new_borrower`) |
| E — Completeness | Dihitung langsung; tidak ada missing |

### 3.2 Normalization
- **Group C → `MinMaxScaler`** (fitur sudah ternormalisasi 0–1 dan rasio bounded).
- **Group A → `StandardScaler`** (distribusi lebar, perlu zero-mean/unit-variance).
- **Group D & E** → MinMaxScaler ke 0–1.
- Scaler di-*fit* pada train set, disimpan (`scaler.pkl`) dan di-*load* saat serving (konsistensi train/serve).

### 3.3 One-Hot Encoding
- `business_type_encoded`, `loan_purpose_encoded` → `OneHotEncoder(handle_unknown='ignore')`.
- Kategori di-*freeze* dari train set; kategori baru → vektor nol.

### 3.4 Class Imbalance
- Default jauh lebih sedikit dari lancar → **SMOTE** dengan `sampling_strategy=0.3` (minoritas menjadi 30% mayoritas) diterapkan **hanya pada train set**.
- Dikombinasikan `class_weight` pada classification head untuk stabilitas.

```python
from imblearn.over_sampling import SMOTE
smote = SMOTE(sampling_strategy=0.3, random_state=42)
X_train_res, y_train_res = smote.fit_resample(X_train, y_train)
```

---

## 4. Model Architecture (Keras Functional / Sequential)

```python
import tensorflow as tf
from tensorflow.keras import layers, Model, Input

inp = Input(shape=(N_FEATURES,), name="features")           # 18+ fitur (termasuk one-hot)

x = layers.Dense(128, activation="relu")(inp)
x = layers.BatchNormalization()(x)
x = layers.Dropout(0.3)(x)

x = layers.Dense(64, activation="relu")(x)
x = layers.BatchNormalization()(x)
x = layers.Dropout(0.2)(x)

x = layers.Dense(32, activation="relu")(x)

# Classification head: P(default)
clf = layers.Dense(1, activation="sigmoid", name="default_prob")(x)
# Regression head: risk_score 0-1 (dikali 100 saat inferensi)
reg = layers.Dense(1, activation="sigmoid", name="risk_score")(x)

model = Model(inputs=inp, outputs=[reg, clf])

model.compile(
    optimizer=tf.keras.optimizers.Adam(learning_rate=0.001),
    loss={"risk_score": "mse", "default_prob": "binary_crossentropy"},
    loss_weights={"risk_score": 0.4, "default_prob": 0.6},
    metrics={"default_prob": [tf.keras.metrics.AUC(name="auc"),
                               tf.keras.metrics.Precision(name="precision"),
                               tf.keras.metrics.Recall(name="recall")]}
)
```

- **Output score:** `risk_score_sigmoid × 100` → skala 0.0–100.0.
- **Loss gabungan:** MSE (regresi) + binary_crossentropy (klasifikasi) berbobot (0.4/0.6, memprioritaskan diskriminasi default).

---

## 5. Training Strategy

- **Split:** 70% train / 15% validation / 15% test, **stratified** pada `is_default`.
- **Batch size:** 32 · **Max epochs:** 100 · **Early stopping:** `patience=10` (monitor `val_default_prob_auc`, mode max).
- **LR scheduler:** `ReduceLROnPlateau(factor=0.5, patience=5, min_lr=1e-6)`.

```python
callbacks = [
    tf.keras.callbacks.EarlyStopping(monitor="val_default_prob_auc", mode="max",
                                     patience=10, restore_best_weights=True),
    tf.keras.callbacks.ReduceLROnPlateau(monitor="val_loss", factor=0.5,
                                         patience=5, min_lr=1e-6),
    tf.keras.callbacks.ModelCheckpoint("credit_model_best.h5",
                                       monitor="val_default_prob_auc", mode="max",
                                       save_best_only=True),
]
history = model.fit(X_train_res, {"risk_score": s_train, "default_prob": y_train_res},
                    validation_data=(X_val, {"risk_score": s_val, "default_prob": y_val}),
                    epochs=100, batch_size=32, callbacks=callbacks,
                    class_weight={"default_prob": {0: 1.0, 1: 3.0}})
```

---

## 6. Evaluation Metrics

| Metrik | Target | Catatan |
|---|---|---|
| AUC-ROC | **≥ 0.80** | Diskriminasi default vs lancar |
| Precision (APPROVE) | **≥ 0.85** | Minimalkan pinjaman buruk yang disetujui |
| Recall (REJECT) | **≥ 0.75** | Tangkap mayoritas calon default |
| Gini coefficient | ≥ 0.60 | `2 × AUC − 1` |
| KS statistic | ≥ 0.40 | Pemisahan distribusi good/bad |
| NPL (bisnis) | < 5% | Validasi pada portofolio nyata |

- **Confusion matrix per threshold** (0.3 / 0.4 / 0.5 / 0.6) dievaluasi untuk kalibrasi.
- Kalibrasi probabilitas diperiksa (reliability diagram / Brier score).

```python
from sklearn.metrics import roc_auc_score, precision_score, recall_score
auc = roc_auc_score(y_test, p_default)
gini = 2 * auc - 1
```

---

## 7. Threshold & Decision Logic

Pemetaan `risk_score` → kategori & rekomendasi:

| risk_score | risk_category | approval_recommendation | Justifikasi Bisnis |
|---|---|---|---|
| **≥ 70** | LOW | **APPROVE** | Probabilitas default rendah; aman disetujui otomatis/fast-track |
| **40–69** | MEDIUM | **REVIEW** | Ambigu; wajib penilaian manual reviewer (human-in-the-loop) |
| **< 40** | HIGH | **REJECT** | Risiko tinggi; tolak untuk menjaga NPL < 5% |

```python
def decide(risk_score: float, p_default: float):
    if risk_score >= 70:   cat, rec = "LOW", "APPROVE"
    elif risk_score >= 40: cat, rec = "MEDIUM", "REVIEW"
    else:                  cat, rec = "HIGH", "REJECT"
    confidence = max(p_default, 1 - p_default)   # jarak dari 0.5
    return cat, rec, round(confidence, 4)
```

- **Confidence** = kepastian prediksi (jarak probabilitas dari 0.5).
- Zona MEDIUM selalu diarahkan ke reviewer → mitigasi risiko model & kepatuhan (keputusan finansial tidak sepenuhnya otomatis).
- Reviewer dapat **override** (tercatat di audit log).

---

## 8. FastAPI Serving Endpoint Design

### 8.1 POST /score
```python
from pydantic import BaseModel, Field
from typing import Dict

class ScoreRequest(BaseModel):
    loan_application_id: str
    borrower_id: str
    features: Dict[str, float]  # 20 fitur mentah; scoring-service melakukan one-hot

class ScoreResponse(BaseModel):
    loan_application_id: str
    risk_score: float = Field(..., ge=0.0, le=100.0)
    risk_category: str            # LOW | MEDIUM | HIGH
    approval_recommendation: str  # APPROVE | REVIEW | REJECT
    confidence: float = Field(..., ge=0.0, le=1.0)
    model_version: str
    processing_time_ms: int
    features_used: Dict[str, float]
```

**Contoh Response:**
```json
{
  "loan_application_id": "c9d0e1f2-a3b4-4567-bc89-9012345abcde",
  "risk_score": 78.5,
  "risk_category": "MEDIUM",
  "approval_recommendation": "REVIEW",
  "confidence": 0.82,
  "model_version": "v2.3.1",
  "processing_time_ms": 42,
  "features_used": { "payment_consistency_score": 85.0, "debt_to_income_ratio": 0.35 }
}
```

### 8.2 GET /health
```json
{ "status": "UP", "model_loaded": true, "model_version": "v2.3.1", "uptime_seconds": 36000 }
```

### 8.3 GET /model-info
```json
{
  "model_version": "v2.3.1",
  "trained_at": "2026-09-30T10:00:00Z",
  "metrics": { "auc_roc": 0.83, "precision_approve": 0.87, "recall_reject": 0.78, "gini": 0.66, "ks": 0.44 },
  "n_features": 28,
  "feature_order": ["payment_consistency_score", "...", "business_type_LAINNYA",
                    "loan_purpose_LAINNYA"]
}
```

---

## 9. Model Warm-Up Strategy

Mencegah latensi tinggi pada request pertama (TensorFlow lazy graph init).

```python
from contextlib import asynccontextmanager
from fastapi import FastAPI

@asynccontextmanager
async def lifespan(app: FastAPI):
    # 1. Pre-load model + preprocessing pipeline
    app.state.model = tf.keras.models.load_model("credit_model_v2.3.1.h5")
    app.state.scaler = joblib.load("scaler.pkl")
    app.state.encoder = joblib.load("encoder.pkl")
    # 2. Dummy prediction (5 sample) untuk inisialisasi graph
    dummy = np.random.rand(5, N_FEATURES).astype("float32")
    _ = app.state.model.predict(dummy, verbose=0)
    print("Model warmed up & ready")
    yield
    # cleanup saat shutdown
    app.state.model = None

app = FastAPI(lifespan=lifespan)
```

- **5 dummy prediction** saat startup memicu kompilasi graph & alokasi memori.
- Pipeline preprocessing (scaler + encoder) dimuat sekali di `app.state`.
- `/health` baru mengembalikan `UP` setelah warm-up selesai (readiness probe K8s).

---

## 10. Model Versioning

### 10.1 File Naming
```
credit_model_v{major}.{minor}.{patch}.h5
contoh: credit_model_v2.3.1.h5
```
- **major:** perubahan arsitektur/fitur breaking.
- **minor:** retraining dengan data baru / fitur tambahan.
- **patch:** perbaikan kecil / rekalibrasi threshold.

### 10.2 Metadata — `model_registry` (PostgreSQL)
| Kolom | Tipe | Keterangan |
|---|---|---|
| id | UUID PK | |
| model_version | VARCHAR UNIQUE | mis. v2.3.1 |
| file_path | TEXT | lokasi artefak (S3) |
| trained_at | TIMESTAMP | |
| metrics_json | JSONB | AUC, precision, recall, gini, ks |
| feature_order | JSONB | urutan fitur |
| status | ENUM | TRAINING \| STAGING \| PRODUCTION \| RETIRED \| CANARY |
| traffic_percentage | INT | untuk canary |
| created_at | TIMESTAMP | |

### 10.3 A/B Testing (Canary)
- Model baru di-*deploy* sebagai **CANARY menerima 10% traffic**; 90% ke PRODUCTION.
- Routing berdasarkan hash `borrower_id` (konsisten per peminjam).
- Bandingkan metrik (AUC, approval rate, NPL proksi) selama periode observasi.
- Bila canary ≥ baseline → promosi ke PRODUCTION (100%); bila buruk → rollback otomatis.

---

## 11. Monitoring & Drift Detection

### 11.1 Input (Feature) Drift
- Monitor distribusi tiap fitur **setiap minggu** vs baseline train.
- Uji: **Population Stability Index (PSI)** & **Kolmogorov-Smirnov**.
- Alert bila **PSI > 0.2** (drift signifikan) pada fitur kunci.

### 11.2 Output Drift
- Monitor distribusi `risk_score` & proporsi kategori (LOW/MEDIUM/HIGH) mingguan.
- Alert bila pergeseran proporsi APPROVE/REJECT di luar ±10% dari baseline.

### 11.3 Retraining Trigger
- **AUC turun > 5%** dari baseline (dievaluasi pada data berlabel terbaru) → trigger retraining.
- Trigger tambahan: PSI > 0.25 berkelanjutan, NPL aktual mendekati/melewati 5%, atau jadwal berkala (kuartalan).
- Pipeline retraining: ekstrak data terbaru → preprocessing → train → evaluasi → canary → promosi.

```python
def should_retrain(current_auc, baseline_auc=0.83):
    return (baseline_auc - current_auc) / baseline_auc > 0.05
```

### 11.4 Observability
- Metrik di-*export* ke Prometheus: `scoring_latency_ms`, `scoring_requests_total`, `score_distribution`, `psi_per_feature`.
- Dashboard Grafana untuk tim ML & risk.
- Setiap inferensi dicatat (`credit_scores.features_json`, `model_version`) untuk audit & analisis drift.
