"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import {
  AlertCircle,
  ArrowLeft,
  Building2,
  CheckCircle2,
  CreditCard,
  Info,
  Lock,
  Wallet,
} from "lucide-react";
import PageHeader from "@/components/ui/PageHeader";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/Card";
import Badge from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { buttonStyles } from "@/lib/buttonStyles";
import EmptyState from "@/components/ui/EmptyState";
import LoadingSkeleton from "@/components/ui/LoadingSkeleton";
import { useInstallments, usePayInstallment } from "@/hooks/useInstallments";
import { useLoanApplication } from "@/hooks/useLoanApplication";
import {
  getInstallmentStatusLabel,
  getPaymentGatewayLabel,
  LOAN_PAYABLE_STATUSES,
} from "@/lib/constants";
import { cn, formatCurrency, formatDateShort, getApiErrorMessage } from "@/lib/utils";

const GATEWAYS = [
  {
    value: "MIDTRANS",
    title: "Midtrans",
    description: "Kartu kredit/debit, virtual account, dan e-wallet.",
    icon: CreditCard,
  },
  {
    value: "XENDIT",
    title: "Xendit",
    description: "Virtual account dan kanal pembayaran populer.",
    icon: Building2,
  },
];

function newIdempotencyKey() {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }
  return `pay-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}

export default function PayInstallmentView({ loanId, initialInstallmentId = null }) {
  const applicationQuery = useLoanApplication(loanId);
  const installmentsQuery = useInstallments(loanId);
  const payMutation = usePayInstallment();

  const [selectedId, setSelectedId] = useState(initialInstallmentId || "");
  const [gateway, setGateway] = useState("MIDTRANS");
  const [formError, setFormError] = useState("");
  const [pendingPayment, setPendingPayment] = useState(null);
  const idempotencyRef = useRef({ signature: "", key: "" });

  const application = applicationQuery.data;
  const installments = useMemo(() => installmentsQuery.data || [], [installmentsQuery.data]);
  const unpaidInstallments = useMemo(
    () => installments.filter((installment) => installment.status !== "PAID"),
    [installments],
  );

  useEffect(() => {
    if (unpaidInstallments.length === 0) {
      setSelectedId("");
      return;
    }
    const selected = unpaidInstallments.find(
      (installment) => installment.installmentId === selectedId,
    );
    if (!selected) {
      setSelectedId(unpaidInstallments[0].installmentId);
    }
  }, [selectedId, unpaidInstallments]);

  const selectedInstallment =
    unpaidInstallments.find((installment) => installment.installmentId === selectedId) || null;

  function idempotencyKeyFor(payload) {
    const signature = JSON.stringify(payload);
    if (idempotencyRef.current.signature !== signature || !idempotencyRef.current.key) {
      idempotencyRef.current = {
        signature,
        key: newIdempotencyKey(),
      };
    }
    return idempotencyRef.current.key;
  }

  async function handlePay() {
    if (!selectedInstallment) {
      return;
    }
    setFormError("");
    const payload = {
      paymentGateway: gateway,
      amount: Number(selectedInstallment.amount),
    };

    try {
      const payment = await payMutation.mutateAsync({
        installmentId: selectedInstallment.installmentId,
        payload,
        idempotencyKey: idempotencyKeyFor(payload),
      });

      if (payment?.paymentUrl) {
        window.location.assign(payment.paymentUrl);
        return;
      }
      setPendingPayment(payment);
    } catch (error) {
      setFormError(
        getApiErrorMessage(
          error,
          "Pembayaran tidak dapat diproses. Periksa kembali data cicilan Anda.",
        ),
      );
    }
  }

  if (applicationQuery.isLoading || installmentsQuery.isLoading) {
    return <LoadingSkeleton variant="page" />;
  }

  if (applicationQuery.isError || installmentsQuery.isError) {
    return (
      <EmptyState
        icon={AlertCircle}
        title="Data pembayaran tidak tersedia"
        description="Kami tidak dapat memuat detail pinjaman atau jadwal cicilan. Coba lagi beberapa saat lagi."
        action={
          <Button
            variant="secondary"
            onClick={() => {
              applicationQuery.refetch();
              installmentsQuery.refetch();
            }}
          >
            Coba lagi
          </Button>
        }
      />
    );
  }

  if (pendingPayment) {
    return (
      <div className="space-y-6">
        <PageHeader
          eyebrow="Pembayaran"
          title="Pembayaran sedang diproses"
          description="Instruksi pembayaran telah dibuat. Selesaikan pembayaran sebelum batas waktu."
        />
        <Card>
          <CardContent className="py-8 text-center">
            <span className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-primary-50 text-primary-700 dark:bg-primary-950/60 dark:text-primary-300">
              <CreditCard className="h-7 w-7" aria-hidden="true" />
            </span>
            <h2 className="mt-4 text-xl font-bold text-content">Menunggu pembayaran</h2>
            <p className="mx-auto mt-2 max-w-md text-sm leading-6 text-content-muted">
              Pembayaran berhasil dibuat dengan ID{" "}
              <span className="numeric font-medium text-content">{pendingPayment.paymentId}</span>.
              Status cicilan akan diperbarui otomatis setelah gateway mengonfirmasi.
            </p>
            <div className="mt-5 flex justify-center">
              <Link href={`/loans/${loanId}`} className={buttonStyles()}>
                Kembali ke detail pinjaman
              </Link>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (!LOAN_PAYABLE_STATUSES.includes(application?.status)) {
    return (
      <div className="space-y-6">
        <Link
          href={`/loans/${loanId}`}
          className="inline-flex items-center gap-1.5 rounded text-xs font-semibold text-content-muted underline-offset-2 hover:text-content hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600"
        >
          <ArrowLeft className="h-3.5 w-3.5" aria-hidden="true" />
          Kembali ke detail pinjaman
        </Link>
        <EmptyState
          icon={Wallet}
          title="Pembayaran belum tersedia"
          description="Cicilan dapat dibayar setelah pinjaman disetujui dan dana dicairkan."
          action={
            <Link href={`/loans/${loanId}`} className={buttonStyles({ variant: "secondary" })}>
              Lihat detail pinjaman
            </Link>
          }
        />
      </div>
    );
  }

  if (unpaidInstallments.length === 0) {
    return (
      <div className="space-y-6">
        <PageHeader eyebrow="Pembayaran" title="Semua cicilan lunas" />
        <EmptyState
          icon={CheckCircle2}
          title="Tidak ada cicilan yang perlu dibayar"
          description="Seluruh cicilan untuk pinjaman ini telah dibayar. Terima kasih."
          action={
            <Link href={`/loans/${loanId}`} className={buttonStyles()}>
              Kembali ke detail pinjaman
            </Link>
          }
        />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <Link
        href={`/loans/${loanId}`}
        className="inline-flex items-center gap-1.5 rounded text-xs font-semibold text-content-muted underline-offset-2 hover:text-content hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600"
      >
        <ArrowLeft className="h-3.5 w-3.5" aria-hidden="true" />
        Kembali ke detail pinjaman
      </Link>

      <PageHeader
        eyebrow="Pembayaran cicilan"
        title="Bayar cicilan"
        description={`${application?.loanProductName || "Pinjaman"} · ID #${String(loanId).slice(0, 8).toUpperCase()}`}
      />

      {formError ? (
        <div
          role="alert"
          className="flex items-start gap-2.5 rounded-xl border border-danger-200 bg-danger-50 px-3.5 py-3 text-sm leading-6 text-danger-700 dark:border-danger-700/40 dark:bg-danger-700/15 dark:text-danger-200"
        >
          <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
          <span>{formError}</span>
        </div>
      ) : null}

      <div className="grid gap-6 lg:grid-cols-[1.4fr_0.9fr]">
        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>Pilih cicilan</CardTitle>
              <CardDescription>
                Pilih satu cicilan yang ingin dibayar. Pembayaran tidak dapat digabung dalam satu
                transaksi.
              </CardDescription>
            </CardHeader>
            <CardContent className="max-h-[26rem] space-y-2.5 overflow-y-auto">
              {unpaidInstallments.map((installment) => {
                const active = installment.installmentId === selectedId;
                return (
                  <label
                    key={installment.installmentId}
                    className={cn(
                      "flex cursor-pointer items-start gap-3 rounded-xl border p-3.5 transition-colors focus-within:ring-2 focus-within:ring-primary-600 focus-within:ring-offset-2 focus-within:ring-offset-surface",
                      active
                        ? "border-primary-600 bg-primary-50/60 dark:border-primary-400 dark:bg-primary-950/40"
                        : "border-line bg-surface-raised hover:bg-surface-subtle",
                    )}
                  >
                    <input
                      type="radio"
                      name="installment"
                      value={installment.installmentId}
                      checked={active}
                      onChange={() => setSelectedId(installment.installmentId)}
                      className="mt-0.5 h-4 w-4 border-neutral-300 text-primary-600 focus:ring-2 focus:ring-primary-600 dark:border-neutral-600 dark:bg-neutral-800"
                    />
                    <span className="min-w-0 flex-1">
                      <span className="flex flex-wrap items-center justify-between gap-2">
                        <span className="text-sm font-semibold text-content">
                          Cicilan #{installment.installmentNumber}
                        </span>
                        <Badge
                          variant={installment.status === "OVERDUE" ? "danger" : "warning"}
                          dot
                        >
                          {getInstallmentStatusLabel(installment.status)}
                        </Badge>
                      </span>
                      <span className="mt-0.5 block text-xs text-content-muted">
                        Jatuh tempo {formatDateShort(installment.dueDate)}
                      </span>
                      <span className="numeric mt-1 block text-base font-semibold text-content">
                        {formatCurrency(installment.amount)}
                      </span>
                    </span>
                  </label>
                );
              })}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Metode pembayaran</CardTitle>
              <CardDescription>Pilih gateway yang ingin digunakan.</CardDescription>
            </CardHeader>
            <CardContent className="grid gap-2.5 sm:grid-cols-2">
              {GATEWAYS.map((option) => {
                const Icon = option.icon;
                const active = gateway === option.value;
                return (
                  <label
                    key={option.value}
                    className={cn(
                      "flex cursor-pointer items-start gap-3 rounded-xl border p-3.5 transition-colors focus-within:ring-2 focus-within:ring-primary-600 focus-within:ring-offset-2 focus-within:ring-offset-surface",
                      active
                        ? "border-primary-600 bg-primary-50/60 dark:border-primary-400 dark:bg-primary-950/40"
                        : "border-line bg-surface-raised hover:bg-surface-subtle",
                    )}
                  >
                    <input
                      type="radio"
                      name="gateway"
                      value={option.value}
                      checked={active}
                      onChange={() => setGateway(option.value)}
                      className="sr-only"
                    />
                    <span
                      className={cn(
                        "flex h-9 w-9 shrink-0 items-center justify-center rounded-lg",
                        active
                          ? "bg-primary-600 text-white"
                          : "bg-surface-subtle text-content-muted",
                      )}
                      aria-hidden="true"
                    >
                      <Icon className="h-4 w-4" />
                    </span>
                    <span className="min-w-0">
                      <span className="block text-sm font-semibold text-content">{option.title}</span>
                      <span className="mt-0.5 block text-xs leading-5 text-content-muted">
                        {option.description}
                      </span>
                    </span>
                  </label>
                );
              })}
            </CardContent>
          </Card>
        </div>

        <aside className="h-fit space-y-4 lg:sticky lg:top-24">
          <Card>
            <CardHeader>
              <CardTitle>Ringkasan pembayaran</CardTitle>
            </CardHeader>
            <CardContent>
              {selectedInstallment ? (
                <>
                  <div className="rounded-xl border border-line bg-surface-subtle p-4">
                    <p className="text-2xs uppercase tracking-wide text-content-subtle">
                      Total yang dibayar
                    </p>
                    <p className="numeric mt-1 text-2xl font-semibold text-content">
                      {formatCurrency(selectedInstallment.amount)}
                    </p>
                    <p className="mt-1 text-xs text-content-muted">
                      Cicilan #{selectedInstallment.installmentNumber} · jatuh tempo{" "}
                      {formatDateShort(selectedInstallment.dueDate)}
                    </p>
                  </div>

                  <dl className="mt-4 space-y-2 text-xs">
                    <div className="flex justify-between gap-3">
                      <dt className="text-content-muted">Pokok</dt>
                      <dd className="numeric font-medium text-content">
                        {formatCurrency(selectedInstallment.principal)}
                      </dd>
                    </div>
                    <div className="flex justify-between gap-3">
                      <dt className="text-content-muted">Bunga</dt>
                      <dd className="numeric font-medium text-content">
                        {formatCurrency(selectedInstallment.interest)}
                      </dd>
                    </div>
                    <div className="flex justify-between gap-3 border-t border-line pt-2">
                      <dt className="text-content-muted">Gateway</dt>
                      <dd className="font-medium text-content">
                        {getPaymentGatewayLabel(gateway)}
                      </dd>
                    </div>
                  </dl>
                </>
              ) : (
                <p className="text-sm text-content-muted">Pilih cicilan terlebih dahulu.</p>
              )}

              <Button
                type="button"
                size="lg"
                fullWidth
                className="mt-5"
                onClick={handlePay}
                isLoading={payMutation.isPending}
                loadingText="Membuat pembayaran..."
                disabled={!selectedInstallment}
                leftIcon={<Lock className="h-4 w-4" />}
              >
                Lanjutkan ke {getPaymentGatewayLabel(gateway)}
              </Button>

              <p className="mt-3 flex items-start gap-1.5 text-2xs leading-4 text-content-subtle">
                <Info className="mt-0.5 h-3.5 w-3.5 shrink-0" aria-hidden="true" />
                Anda akan diarahkan ke halaman pembayaran gateway. Jangan menutup halaman sampai
                transaksi selesai.
              </p>
            </CardContent>
          </Card>

          <div className="rounded-2xl border border-line bg-surface-raised px-4 py-3.5 text-xs leading-5 text-content-muted">
            <p className="flex items-start gap-2">
              <Lock className="mt-0.5 h-3.5 w-3.5 shrink-0" aria-hidden="true" />
              Data pembayaran diproses melalui gateway berizin. Koperasi Digital tidak menyimpan
              nomor kartu atau kredensial perbankan Anda.
            </p>
          </div>
        </aside>
      </div>
    </div>
  );
}
