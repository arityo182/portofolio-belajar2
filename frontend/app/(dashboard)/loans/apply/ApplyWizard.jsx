"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  AlertCircle,
  ArrowLeft,
  ArrowRight,
  Calculator,
  CheckCircle2,
  Info,
  Send,
  ShieldCheck,
  Wallet,
} from "lucide-react";
import PageHeader from "@/components/ui/PageHeader";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { buttonStyles } from "@/lib/buttonStyles";
import Input from "@/components/ui/Input";
import Select from "@/components/ui/Select";
import Textarea from "@/components/ui/Textarea";
import EmptyState from "@/components/ui/EmptyState";
import Badge from "@/components/ui/Badge";
import ApplyStepper from "@/components/loans/apply/ApplyStepper";
import ProductCard from "@/components/loans/apply/ProductCard";
import DocumentUpload from "@/components/loans/apply/DocumentUpload";
import { useCreateLoanApplication, useLoanProducts } from "@/hooks/useLoanApplication";
import { borrowerService } from "@/services/borrowerService";
import { useLoanStore } from "@/stores/loanStore";
import { buildTenorOptions, calculateSimulation } from "@/lib/loanSimulator";
import {
  createLoanApplySchema,
  formatFileSize,
  validateDocumentFile,
} from "@/lib/validations/loan";
import { getApiErrorMessage, formatCurrency } from "@/lib/utils";

const applySchema = createLoanApplySchema(null);

const STEP_COPY = {
  1: {
    title: "Pilih produk pembiayaan",
    description: "Sesuaikan produk dengan kebutuhan modal usaha Anda.",
  },
  2: {
    title: "Atur nominal dan tenor",
    description: "Tentukan jumlah pinjaman, tenor, dan tujuan penggunaan dana.",
  },
  3: {
    title: "Unggah dokumen pendukung",
    description: "Siapkan foto KTP dan selfie untuk proses verifikasi.",
  },
  4: {
    title: "Periksa pengajuan Anda",
    description: "Pastikan seluruh data sudah benar sebelum dikirim.",
  },
};

export default function ApplyWizard() {
  const router = useRouter();
  const createApplication = useCreateLoanApplication();
  const productsQuery = useLoanProducts();
  const {
    step,
    product,
    amount: draftAmount,
    tenorMonths: draftTenor,
    purpose: draftPurpose,
    setStep,
    selectProduct,
    updateDraft,
    setDocument,
    clearDocument,
    ensureIdempotencyKey,
    resetDraft,
  } = useLoanStore();

  const [formError, setFormError] = useState("");
  const [redirecting, setRedirecting] = useState(false);
  const [documents, setDocuments] = useState({ ktp: null, selfie: null });
  const [documentErrors, setDocumentErrors] = useState({ ktp: null, selfie: null });
  const errorRef = useRef(null);

  const {
    register,
    handleSubmit,
    trigger,
    setValue,
    setError,
    clearErrors,
    getValues,
    watch,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(applySchema),
    mode: "onTouched",
    defaultValues: {
      loanProductId: product?.id || "",
      amount: draftAmount || "",
      tenorMonths: draftTenor || "",
      purpose: draftPurpose || "",
      acceptTerms: false,
    },
  });

  const amountValue = watch("amount");
  const tenorValue = watch("tenorMonths");
  const purposeValue = watch("purpose");
  const acceptTerms = watch("acceptTerms");

  const minAmount = Number(product?.minAmount || 0);
  const maxAmount = Number(product?.maxAmount || 0);
  const tenorOptions = useMemo(
    () => (product ? buildTenorOptions(product.minTenorMonths, product.maxTenorMonths) : []),
    [product],
  );
  const simulation = useMemo(
    () =>
      calculateSimulation({
        amount: Number(amountValue) || 0,
        annualRatePercent: Number(product?.interestRateAnnual) || 0,
        tenorMonths: Number(tenorValue) || 1,
      }),
    [amountValue, product?.interestRateAnnual, tenorValue],
  );

  useEffect(() => {
    if (formError) {
      errorRef.current?.focus();
    }
  }, [formError]);

  useEffect(() => {
    if (!product && step !== 1) {
      setStep(1);
    }
  }, [product, setStep, step]);

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: "smooth" });
  }, [step]);

  function goToStep(nextStep) {
    setFormError("");
    setStep(nextStep);
  }

  function handleProductSelect(nextProduct) {
    clearErrors("loanProductId");
    selectProduct(nextProduct);
    setValue("loanProductId", nextProduct.id, { shouldValidate: true });
    setValue("amount", "");
    setValue("tenorMonths", nextProduct.minTenorMonths);
    updateDraft({
      product: nextProduct,
      amount: "",
      tenorMonths: nextProduct.minTenorMonths,
    });
  }

  async function handleProductNext() {
    const valid = await trigger(["loanProductId"]);
    if (!valid || !product) {
      return;
    }
    goToStep(2);
  }

  async function handleDetailsNext() {
    const valid = await trigger(["amount", "tenorMonths", "purpose"]);
    if (!valid) {
      return;
    }

    const amount = Number(getValues("amount"));
    const tenor = Number(getValues("tenorMonths"));

    if (amount < minAmount || amount > maxAmount) {
      setError("amount", {
        type: "manual",
        message: `Nominal harus antara ${formatCurrency(minAmount)} dan ${formatCurrency(maxAmount)}`,
      });
      return;
    }
    if (tenor < Number(product.minTenorMonths) || tenor > Number(product.maxTenorMonths)) {
      setError("tenorMonths", {
        type: "manual",
        message: `Tenor harus antara ${product.minTenorMonths} dan ${product.maxTenorMonths} bulan`,
      });
      return;
    }

    updateDraft({
      amount: String(amount),
      tenorMonths: String(tenor),
      purpose: String(getValues("purpose")).trim(),
    });
    goToStep(3);
  }

  function handleDocument(kind, file) {
    const validationError = validateDocumentFile(file);
    setDocumentErrors((current) => ({ ...current, [kind]: validationError }));
    if (validationError) {
      setDocuments((current) => ({ ...current, [kind]: null }));
      clearDocument(kind);
      return;
    }
    setDocuments((current) => ({ ...current, [kind]: file }));
    setDocument(kind, {
      name: file.name,
      size: file.size,
      type: file.type,
    });
  }

  function removeDocument(kind) {
    setDocuments((current) => ({ ...current, [kind]: null }));
    setDocumentErrors((current) => ({ ...current, [kind]: null }));
    clearDocument(kind);
  }

  function handleDocumentsNext() {
    const nextErrors = {
      ktp: documents.ktp ? validateDocumentFile(documents.ktp) : "Unggah foto KTP terlebih dahulu",
      selfie: documents.selfie
        ? validateDocumentFile(documents.selfie)
        : "Unggah foto selfie terlebih dahulu",
    };
    setDocumentErrors(nextErrors);
    if (nextErrors.ktp || nextErrors.selfie) {
      return;
    }
    goToStep(4);
  }

  async function onSubmit(values) {
    setFormError("");
    const idempotencyKey = ensureIdempotencyKey();
    try {
      if (!documents.ktp || !documents.selfie) {
        setFormError("Foto KTP dan selfie wajib diunggah sebelum pengajuan dikirim.");
        return;
      }
      await borrowerService.uploadDocuments({
        ktpPhoto: documents.ktp,
        selfiePhoto: documents.selfie,
      });
      const application = await createApplication.mutateAsync({
        idempotencyKey,
        payload: {
          loanProductId: Number(product.id),
          amount: Number(values.amount),
          tenorMonths: Number(values.tenorMonths),
          purpose: String(values.purpose).trim(),
        },
      });
      setRedirecting(true);
      resetDraft();
      router.replace(`/loans/${application.loanId}?submitted=1`);
    } catch (error) {
      setFormError(
        getApiErrorMessage(error, "Pengajuan gagal dikirim. Periksa kembali data Anda."),
      );
    }
  }

  if (redirecting) {
    return (
      <div className="flex min-h-[50vh] flex-col items-center justify-center gap-3" role="status">
        <span
          className="h-9 w-9 animate-spin rounded-full border-2 border-primary-200 border-t-primary-600 dark:border-neutral-700 dark:border-t-primary-400"
          aria-hidden="true"
        />
        <p className="text-sm text-content-muted">Mengalihkan ke detail pengajuan...</p>
      </div>
    );
  }

  const stepCopy = STEP_COPY[step] || STEP_COPY[1];

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Pengajuan pinjaman"
        title="Ajukan pinjaman"
        description="Empat langkah singkat. Data Anda digunakan hanya untuk penilaian kredit dan verifikasi."
        actions={
          <Link href="/loans" className={buttonStyles({ variant: "secondary" })}>
            Riwayat pinjaman
          </Link>
        }
      />

      <Card>
        <CardContent className="py-4">
          <ApplyStepper currentStep={step} />
        </CardContent>
      </Card>

      {formError ? (
        <div
          ref={errorRef}
          tabIndex={-1}
          role="alert"
          className="flex items-start gap-2.5 rounded-xl border border-danger-200 bg-danger-50 px-3.5 py-3 text-sm leading-6 text-danger-700 focus:outline-none dark:border-danger-700/40 dark:bg-danger-700/15 dark:text-danger-200"
        >
          <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
          <span>{formError}</span>
        </div>
      ) : null}

      <Card>
        <CardHeader>
          <p className="text-2xs font-semibold uppercase tracking-[0.16em] text-primary-600 dark:text-primary-400">
            Langkah {step} dari 4
          </p>
          <CardTitle as="h2">{stepCopy.title}</CardTitle>
          <CardDescription>{stepCopy.description}</CardDescription>
        </CardHeader>

        <CardContent>
          <form onSubmit={handleSubmit(onSubmit)} method="post" noValidate>
            {step === 1 ? (
              <div className="space-y-5">
                {productsQuery.isLoading ? (
                  <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3" aria-hidden="true">
                    {Array.from({ length: 3 }).map((_, index) => (
                      <div
                        key={index}
                        className="h-56 rounded-2xl border border-line bg-surface-raised p-5"
                      >
                        <div className="skeleton h-11 w-11 rounded-xl" />
                        <div className="skeleton mt-4 h-4 w-2/3" />
                        <div className="skeleton mt-3 h-7 w-1/2" />
                        <div className="skeleton mt-4 h-3 w-full" />
                      </div>
                    ))}
                  </div>
                ) : productsQuery.isError ? (
                  <div className="rounded-xl border border-danger-200 bg-danger-50 px-4 py-5 text-center dark:border-danger-700/40 dark:bg-danger-700/15">
                    <p className="text-sm font-medium text-danger-700 dark:text-danger-200">
                      Gagal memuat produk pinjaman.
                    </p>
                    <Button
                      type="button"
                      variant="secondary"
                      size="sm"
                      className="mt-3"
                      onClick={() => productsQuery.refetch()}
                    >
                      Coba lagi
                    </Button>
                  </div>
                ) : (productsQuery.data || []).length === 0 ? (
                  <EmptyState
                    icon={Wallet}
                    title="Belum ada produk pinjaman"
                    description="Produk pembiayaan belum tersedia saat ini. Silakan coba lagi nanti."
                  />
                ) : (
                  <>
                    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
                      {productsQuery.data.map((item) => (
                        <ProductCard
                          key={item.id}
                          product={item}
                          selected={product?.id === item.id}
                          onSelect={handleProductSelect}
                        />
                      ))}
                    </div>
                    {errors.loanProductId ? (
                      <p role="alert" className="text-xs text-danger-600 dark:text-danger-300">
                        {errors.loanProductId.message}
                      </p>
                    ) : null}
                  </>
                )}

                <div className="flex justify-end">
                  <Button
                    type="button"
                    size="lg"
                    onClick={handleProductNext}
                    disabled={!product || productsQuery.isLoading}
                    rightIcon={<ArrowRight className="h-4 w-4" />}
                  >
                    Lanjutkan
                  </Button>
                </div>
              </div>
            ) : null}

            {step === 2 && product ? (
              <div className="grid gap-6 lg:grid-cols-[1.25fr_0.75fr]">
                <div className="space-y-5">
                  <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-line bg-surface-subtle px-4 py-3">
                    <div>
                      <p className="text-xs text-content-muted">Produk dipilih</p>
                      <p className="mt-0.5 text-sm font-semibold text-content">{product.name}</p>
                    </div>
                    <button
                      type="button"
                      onClick={() => goToStep(1)}
                      className="rounded text-xs font-semibold text-primary-700 underline-offset-2 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600 dark:text-primary-300"
                    >
                      Ganti produk
                    </button>
                  </div>

                  <div>
                    <Input
                      label="Nominal pinjaman"
                      type="number"
                      inputMode="numeric"
                      min={minAmount}
                      max={maxAmount}
                      step={100000}
                      placeholder="Contoh: 10000000"
                      leftIcon={<span className="text-xs font-semibold">Rp</span>}
                      className="numeric"
                      error={errors.amount?.message}
                      helper={
                        Number(amountValue) > 0
                          ? `Nilai ${formatCurrency(amountValue)} · rentang ${formatCurrency(minAmount)} – ${formatCurrency(maxAmount)}`
                          : `Rentang ${formatCurrency(minAmount)} – ${formatCurrency(maxAmount)}`
                      }
                      required
                      {...register("amount")}
                    />
                    <div className="mt-2 flex flex-wrap gap-2">
                      {[minAmount, 5000000, 10000000, 25000000]
                        .filter(
                          (value, index, list) =>
                            value >= minAmount &&
                            value <= maxAmount &&
                            list.indexOf(value) === index,
                        )
                        .map((value) => (
                          <button
                            key={value}
                            type="button"
                            onClick={() => setValue("amount", value, { shouldValidate: true })}
                            className="rounded-full border border-line bg-surface-raised px-3 py-1 text-2xs font-medium text-content-muted transition-colors hover:border-primary-300 hover:text-content focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600"
                          >
                            {formatCurrency(value)}
                          </button>
                        ))}
                    </div>
                  </div>

                  <Select
                    label="Tenor"
                    error={errors.tenorMonths?.message}
                    helper="Pilih jangka waktu pembayaran yang sesuai kemampuan usaha."
                    required
                    {...register("tenorMonths")}
                  >
                    <option value="">Pilih tenor</option>
                    {tenorOptions.map((tenor) => (
                      <option key={tenor} value={tenor}>
                        {tenor} bulan
                      </option>
                    ))}
                  </Select>

                  <Textarea
                    label="Tujuan pinjaman"
                    placeholder="Contoh: Pembelian peralatan dapur untuk ekspansi warung"
                    maxLength={1000}
                    showCounter
                    value={purposeValue}
                    error={errors.purpose?.message}
                    helper="Jelaskan penggunaan dana secara singkat dan jelas."
                    required
                    {...register("purpose")}
                  />

                  <div className="flex flex-col-reverse gap-2.5 sm:flex-row sm:justify-between">
                    <Button
                      type="button"
                      variant="secondary"
                      size="lg"
                      onClick={() => goToStep(1)}
                      leftIcon={<ArrowLeft className="h-4 w-4" />}
                    >
                      Kembali
                    </Button>
                    <Button
                      type="button"
                      size="lg"
                      onClick={handleDetailsNext}
                      rightIcon={<ArrowRight className="h-4 w-4" />}
                    >
                      Lanjutkan
                    </Button>
                  </div>
                </div>

                <aside className="h-fit rounded-2xl border border-primary-200 bg-primary-50/60 p-5 lg:sticky lg:top-24 dark:border-primary-800 dark:bg-primary-950/40">
                  <p className="flex items-center gap-2 text-sm font-semibold text-primary-800 dark:text-primary-200">
                    <Calculator className="h-4 w-4" aria-hidden="true" />
                    Simulasi cicilan
                  </p>
                  <p className="mt-1 text-2xs text-primary-700/80 dark:text-primary-300/80">
                    Metode {simulation.method === "ANNUITY" ? "anuitas" : "flat"} · bunga{" "}
                    {Number(product.interestRateAnnual).toFixed(2)}% per tahun
                  </p>

                  <div className="mt-4 rounded-xl bg-surface-raised p-4 shadow-sm">
                    <p className="text-2xs uppercase tracking-wide text-content-subtle">
                      Estimasi per bulan
                    </p>
                    <p className="numeric mt-1 text-2xl font-semibold text-content">
                      {formatCurrency(simulation.monthlyInstallment)}
                    </p>
                    <p className="mt-1 text-2xs text-content-muted">
                      {simulation.months} kali pembayaran
                    </p>
                  </div>

                  <dl className="mt-4 space-y-2 text-xs">
                    <div className="flex justify-between gap-3">
                      <dt className="text-primary-800/80 dark:text-primary-200/80">Pokok per bulan</dt>
                      <dd className="numeric font-medium text-primary-900 dark:text-primary-100">
                        {formatCurrency(simulation.monthlyPrincipal)}
                      </dd>
                    </div>
                    <div className="flex justify-between gap-3">
                      <dt className="text-primary-800/80 dark:text-primary-200/80">Bunga per bulan</dt>
                      <dd className="numeric font-medium text-primary-900 dark:text-primary-100">
                        {formatCurrency(simulation.monthlyInterest)}
                      </dd>
                    </div>
                    <div className="flex justify-between gap-3 border-t border-primary-200/70 pt-2 dark:border-primary-800/70">
                      <dt className="text-primary-800/80 dark:text-primary-200/80">Total bunga</dt>
                      <dd className="numeric font-medium text-primary-900 dark:text-primary-100">
                        {formatCurrency(simulation.totalInterest)}
                      </dd>
                    </div>
                    <div className="flex justify-between gap-3">
                      <dt className="font-semibold text-primary-900 dark:text-primary-100">
                        Total pembayaran
                      </dt>
                      <dd className="numeric font-semibold text-primary-900 dark:text-primary-100">
                        {formatCurrency(simulation.totalPayment)}
                      </dd>
                    </div>
                  </dl>

                  <p className="mt-4 flex items-start gap-1.5 text-2xs leading-4 text-primary-700/80 dark:text-primary-300/80">
                    <Info className="mt-0.5 h-3.5 w-3.5 shrink-0" aria-hidden="true" />
                    Simulasi bersifat estimasi. Nilai final mengikuti persetujuan dan jadwal dari
                    sistem koperasi.
                  </p>
                </aside>
              </div>
            ) : null}

            {step === 3 ? (
              <div className="space-y-5">
                <div className="grid gap-5 sm:grid-cols-2">
                  <DocumentUpload
                    id="ktp-document"
                    label="Foto KTP"
                    description="Pastikan seluruh tepi KTP terlihat dan tidak buram"
                    file={documents.ktp}
                    error={documentErrors.ktp}
                    onFileChange={(file) => handleDocument("ktp", file)}
                    onRemove={() => removeDocument("ktp")}
                    required
                  />
                  <DocumentUpload
                    id="selfie-document"
                    label="Foto selfie dengan KTP"
                    description="Wajah dan KTP harus terlihat jelas dalam satu foto"
                    file={documents.selfie}
                    error={documentErrors.selfie}
                    onFileChange={(file) => handleDocument("selfie", file)}
                    onRemove={() => removeDocument("selfie")}
                    required
                  />
                </div>

                <div className="flex items-start gap-2.5 rounded-xl border border-info-200 bg-info-50 px-4 py-3 text-xs leading-5 text-info-700 dark:border-info-700/40 dark:bg-info-700/15 dark:text-info-200">
                  <ShieldCheck className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
                  Dokumen hanya dipratinjau di perangkat Anda dan tidak dikirim ke server pada
                  tahap ini. Verifikasi dokumen dilakukan melalui layanan profil peminjam.
                </div>

                <div className="flex flex-col-reverse gap-2.5 sm:flex-row sm:justify-between">
                  <Button
                    type="button"
                    variant="secondary"
                    size="lg"
                    onClick={() => goToStep(2)}
                    leftIcon={<ArrowLeft className="h-4 w-4" />}
                  >
                    Kembali
                  </Button>
                  <Button
                    type="button"
                    size="lg"
                    onClick={handleDocumentsNext}
                    rightIcon={<ArrowRight className="h-4 w-4" />}
                  >
                    Lanjutkan
                  </Button>
                </div>
              </div>
            ) : null}

            {step === 4 && product ? (
              <div className="space-y-6">
                <div className="grid gap-4 sm:grid-cols-2">
                  <div className="rounded-xl border border-line bg-surface-subtle p-4">
                    <p className="text-2xs uppercase tracking-wide text-content-subtle">Produk</p>
                    <p className="mt-1 text-sm font-semibold text-content">{product.name}</p>
                    <p className="numeric mt-0.5 text-xs text-content-muted">
                      Bunga {Number(product.interestRateAnnual).toFixed(2)}% per tahun
                    </p>
                  </div>
                  <div className="rounded-xl border border-line bg-surface-subtle p-4">
                    <p className="text-2xs uppercase tracking-wide text-content-subtle">Nominal</p>
                    <p className="numeric mt-1 text-sm font-semibold text-content">
                      {formatCurrency(getValues("amount"))}
                    </p>
                    <p className="mt-0.5 text-xs text-content-muted">
                      Tenor {getValues("tenorMonths")} bulan
                    </p>
                  </div>
                  <div className="rounded-xl border border-line bg-surface-subtle p-4 sm:col-span-2">
                    <p className="text-2xs uppercase tracking-wide text-content-subtle">
                      Tujuan pinjaman
                    </p>
                    <p className="mt-1 text-sm leading-6 text-content">{getValues("purpose")}</p>
                  </div>
                  <div className="rounded-xl border border-line bg-surface-subtle p-4">
                    <p className="text-2xs uppercase tracking-wide text-content-subtle">
                      Estimasi cicilan
                    </p>
                    <p className="numeric mt-1 text-sm font-semibold text-content">
                      {formatCurrency(simulation.monthlyInstallment)} / bulan
                    </p>
                  </div>
                  <div className="rounded-xl border border-line bg-surface-subtle p-4">
                    <p className="text-2xs uppercase tracking-wide text-content-subtle">
                      Total pembayaran
                    </p>
                    <p className="numeric mt-1 text-sm font-semibold text-content">
                      {formatCurrency(simulation.totalPayment)}
                    </p>
                  </div>
                </div>

                <div className="rounded-xl border border-line p-4">
                  <p className="text-sm font-semibold text-content">Dokumen</p>
                  <div className="mt-2 flex flex-wrap gap-2">
                    <Badge variant={documents.ktp ? "success" : "warning"} dot>
                      KTP {documents.ktp ? `· ${formatFileSize(documents.ktp.size)}` : "belum ada"}
                    </Badge>
                    <Badge variant={documents.selfie ? "success" : "warning"} dot>
                      Selfie {documents.selfie ? `· ${formatFileSize(documents.selfie.size)}` : "belum ada"}
                    </Badge>
                  </div>
                </div>

                <div>
                  <label className="flex cursor-pointer items-start gap-2.5 rounded-xl border border-line bg-surface-raised p-3.5 transition-colors hover:bg-surface-subtle">
                    <input
                      type="checkbox"
                      className="mt-0.5 h-4 w-4 shrink-0 rounded border-neutral-300 text-primary-600 focus:ring-2 focus:ring-primary-600 focus:ring-offset-2 focus:ring-offset-surface dark:border-neutral-600 dark:bg-neutral-800"
                      aria-invalid={errors.acceptTerms ? "true" : undefined}
                      {...register("acceptTerms")}
                    />
                    <span className="text-xs leading-5 text-content-muted">
                      Saya menyatakan data yang saya berikan benar dan menyetujui{" "}
                      <Link
                        href="/legal/terms"
                        onClick={(event) => event.stopPropagation()}
                        className="font-semibold text-primary-700 underline-offset-2 hover:underline dark:text-primary-300"
                      >
                        Syarat & Ketentuan
                      </Link>{" "}
                      serta pemrosesan data untuk penilaian kredit dan verifikasi.
                    </span>
                  </label>
                  {errors.acceptTerms ? (
                    <p role="alert" className="mt-1.5 text-xs text-danger-600 dark:text-danger-300">
                      {errors.acceptTerms.message}
                    </p>
                  ) : null}
                </div>

                <div className="flex flex-col-reverse gap-2.5 sm:flex-row sm:justify-between">
                  <Button
                    type="button"
                    variant="secondary"
                    size="lg"
                    onClick={() => goToStep(3)}
                    leftIcon={<ArrowLeft className="h-4 w-4" />}
                  >
                    Kembali
                  </Button>
                  <Button
                    type="submit"
                    size="lg"
                    isLoading={createApplication.isPending}
                    loadingText="Mengirim pengajuan..."
                    leftIcon={<Send className="h-4 w-4" />}
                    disabled={!acceptTerms}
                  >
                    Kirim pengajuan
                  </Button>
                </div>
              </div>
            ) : null}
          </form>
        </CardContent>
      </Card>

      {step === 4 ? (
        <p className="flex items-start justify-center gap-1.5 text-center text-2xs text-content-subtle">
          <CheckCircle2 className="mt-0.5 h-3.5 w-3.5 shrink-0" aria-hidden="true" />
          Setelah dikirim, pengajuan masuk ke antrian penilaian AI dan review.
        </p>
      ) : null}
    </div>
  );
}
