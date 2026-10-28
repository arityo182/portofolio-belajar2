"use client";

import Link from "next/link";
import {
  AlertCircle,
  ArrowLeft,
  CalendarDays,
  CheckCircle2,
  CreditCard,
  FileText,
  Info,
} from "lucide-react";
import PageHeader from "@/components/ui/PageHeader";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/Card";
import Badge from "@/components/ui/Badge";
import { buttonStyles } from "@/lib/buttonStyles";
import EmptyState from "@/components/ui/EmptyState";
import LoadingSkeleton from "@/components/ui/LoadingSkeleton";
import InstallmentTable from "@/components/loans/InstallmentTable";
import LoanTimeline from "@/components/loans/LoanTimeline";
import OverdueAlert from "@/components/loans/OverdueAlert";
import RepaymentProgress from "@/components/loans/RepaymentProgress";
import { useInstallments } from "@/hooks/useInstallments";
import { useLoanApplication } from "@/hooks/useLoanApplication";
import { useAuth } from "@/hooks/useAuth";
import { getLoanStatusLabel, LOAN_PAYABLE_STATUSES } from "@/lib/constants";
import { formatCurrency, formatDateShort, formatDateTime, isDateBeforeToday } from "@/lib/utils";
import DisbursementCard from "./DisbursementCard";

function DetailRow({ label, value, mono = false }) {
  return (
    <div className="flex items-start justify-between gap-4 border-b border-line py-2.5 last:border-b-0 last:pb-0 first:pt-0">
      <dt className="text-xs text-content-muted">{label}</dt>
      <dd className={`max-w-[65%] text-right text-sm font-medium text-content ${mono ? "numeric" : ""}`}>
        {value}
      </dd>
    </div>
  );
}

export default function LoanDetailView({ loanId, submitted = false }) {
  const applicationQuery = useLoanApplication(loanId);
  const installmentsQuery = useInstallments(loanId, {
    enabled: applicationQuery.isSuccess,
  });
  const { hasRole } = useAuth();

  const application = applicationQuery.data;
  const installments = installmentsQuery.data || [];
  const unpaid = installments.filter((installment) => installment.status !== "PAID");
  const overdue = unpaid.filter(
    (installment) => installment.status === "OVERDUE" || isDateBeforeToday(installment.dueDate),
  );
  const isBorrower = hasRole("BORROWER");
  const canPay =
    isBorrower && LOAN_PAYABLE_STATUSES.includes(application?.status) && unpaid.length > 0;
  const canDisburse =
    !isBorrower &&
    (hasRole("ADMIN") || hasRole("REVIEWER")) &&
    application?.status === "APPROVED";
  const nextInstallment = unpaid[0];

  if (applicationQuery.isLoading) {
    return <LoadingSkeleton variant="page" />;
  }

  if (applicationQuery.isError) {
    const status = applicationQuery.error?.response?.status;
    const forbidden = status === 403;
    return (
      <EmptyState
        icon={forbidden ? AlertCircle : FileText}
        title={forbidden ? "Tidak memiliki akses" : "Pinjaman tidak ditemukan"}
        description={
          forbidden
            ? "Pinjaman ini bukan milik akun Anda. Kembali ke daftar pinjaman untuk melihat data yang tersedia."
            : "Data pinjaman tidak tersedia atau telah dihapus. Kembali ke daftar pinjaman untuk melanjutkan."
        }
        action={
          <Link href="/loans" className={buttonStyles()}>
            Kembali ke daftar pinjaman
          </Link>
        }
      />
    );
  }

  const overdueAlertItem = overdue[0]
    ? { ...overdue[0], loanId }
    : null;

  return (
    <div className="space-y-6">
      <Link
        href="/loans"
        className="inline-flex items-center gap-1.5 rounded text-xs font-semibold text-content-muted underline-offset-2 hover:text-content hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600"
      >
        <ArrowLeft className="h-3.5 w-3.5" aria-hidden="true" />
        Semua pinjaman
      </Link>

      <PageHeader
        eyebrow="Detail pinjaman"
        title={application.loanProductName || "Pinjaman"}
        description={`ID pengajuan #${String(application.loanId).slice(0, 8).toUpperCase()}`}
        actions={
          <>
            <Badge status={application.status} size="md" dot />
            {canPay ? (
              <Link
                href={`/loans/${loanId}/pay${nextInstallment ? `?installment=${nextInstallment.installmentId}` : ""}`}
                className={buttonStyles()}
              >
                <CreditCard className="h-4 w-4" aria-hidden="true" />
                Bayar cicilan
              </Link>
            ) : null}
          </>
        }
      />

      {submitted ? (
        <div
          role="status"
          className="flex items-start gap-2.5 rounded-2xl border border-success-200 bg-success-50 px-4 py-3.5 text-sm text-success-700 dark:border-success-700/40 dark:bg-success-700/15 dark:text-success-200"
        >
          <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
          <span>
            Pengajuan berhasil dikirim. Proses penilaian AI dan review biasanya selesai dalam
            beberapa menit. Anda tidak perlu mengirim ulang.
          </span>
        </div>
      ) : null}

      {overdueAlertItem ? (
        <OverdueAlert count={overdue.length} amount={overdue.reduce((sum, item) => sum + Number(item.amount || 0), 0)} item={overdueAlertItem} />
      ) : null}

      {canDisburse ? (
        <DisbursementCard loanId={application.loanId} amount={application.amount} />
      ) : null}

      {application.status === "REJECTED" && application.rejectionReason ? (
        <div
          role="alert"
          className="flex items-start gap-2.5 rounded-2xl border border-danger-200 bg-danger-50 px-4 py-3.5 text-sm text-danger-700 dark:border-danger-700/40 dark:bg-danger-700/15 dark:text-danger-200"
        >
          <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
          <span>
            <strong>Pengajuan ditolak.</strong> Alasan: {application.rejectionReason}
          </span>
        </div>
      ) : null}

      <div className="grid gap-6 xl:grid-cols-[1.6fr_1fr]">
        <div className="space-y-6">
          <Card>
            <CardContent className="py-6">
              <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                <div>
                  <p className="text-xs text-content-muted">Nominal pinjaman</p>
                  <p className="numeric mt-1 text-3xl font-semibold tracking-[-0.02em] text-content">
                    {formatCurrency(application.amount)}
                  </p>
                  <p className="mt-1 text-xs text-content-muted">
                    Tenor {application.tenorMonths} bulan · diajukan{" "}
                    {formatDateShort(application.submittedAt)}
                  </p>
                </div>
                <Badge status={application.status} size="md" dot />
              </div>

              <dl className="mt-5 grid gap-x-8 border-t border-line pt-4 sm:grid-cols-2">
                <DetailRow label="Produk" value={application.loanProductName || "—"} />
                <DetailRow label="Tenor" value={`${application.tenorMonths} bulan`} mono />
                <DetailRow
                  label="Tanggal pengajuan"
                  value={formatDateShort(application.submittedAt)}
                />
                <DetailRow
                  label="Keputusan reviewer"
                  value={application.reviewedAt ? formatDateShort(application.reviewedAt) : "—"}
                />
                <div className="sm:col-span-2">
                  <DetailRow label="Tujuan pinjaman" value={application.purpose || "—"} />
                </div>
              </dl>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Progres pembayaran</CardTitle>
              <CardDescription>
                Status cicilan dan kewajiban yang masih berjalan untuk pinjaman ini.
              </CardDescription>
            </CardHeader>
            <CardContent>
              {installmentsQuery.isLoading ? (
                <LoadingSkeleton variant="table" rows={3} columns={4} />
              ) : (
                <RepaymentProgress installments={installments} />
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Jadwal cicilan</CardTitle>
              <CardDescription>
                {canPay
                  ? "Pilih cicilan yang ingin dibayar. Pembayaran diproses melalui gateway terpilih."
                  : "Rincian pokok dan bunga setiap cicilan."}
              </CardDescription>
            </CardHeader>
            <CardContent className="px-0 pb-0 sm:px-0">
              {installmentsQuery.isLoading ? (
                <div className="px-5 sm:px-6">
                  <LoadingSkeleton variant="table" rows={4} columns={5} />
                </div>
              ) : (
                <InstallmentTable
                  installments={installments}
                  loanId={loanId}
                  canPay={canPay}
                />
              )}
            </CardContent>
          </Card>
        </div>

        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>Riwayat status</CardTitle>
              <CardDescription>Perjalanan pengajuan Anda dari awal hingga saat ini.</CardDescription>
            </CardHeader>
            <CardContent>
              <LoanTimeline application={application} installments={installments} />
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Informasi pinjaman</CardTitle>
            </CardHeader>
            <CardContent>
              <dl className="space-y-0">
                <DetailRow label="Status" value={getLoanStatusLabel(application.status)} />
                <DetailRow
                  label="Diajukan"
                  value={application.submittedAt ? formatDateTime(application.submittedAt) : "—"}
                />
                <DetailRow
                  label="Disetujui / ditolak"
                  value={application.reviewedAt ? formatDateTime(application.reviewedAt) : "—"}
                />
                <DetailRow
                  label="Dicairkan"
                  value={application.disbursedAt ? formatDateTime(application.disbursedAt) : "—"}
                />
                <DetailRow
                  label="Selesai"
                  value={application.completedAt ? formatDateTime(application.completedAt) : "—"}
                />
                <DetailRow
                  label="Metode pembayaran"
                  value={
                    installments.length > 0
                      ? "Transfer melalui payment gateway"
                      : "Menunggu pencairan"
                  }
                />
              </dl>

              <p className="mt-4 flex items-start gap-2 rounded-xl border border-line bg-surface-subtle px-3.5 py-3 text-2xs leading-5 text-content-muted">
                <Info className="mt-0.5 h-3.5 w-3.5 shrink-0" aria-hidden="true" />
                Pembayaran cicilan hanya tersedia setelah dana pinjaman dicairkan. Status akan
                diperbarui otomatis setelah gateway mengonfirmasi pembayaran.
              </p>
            </CardContent>
          </Card>

          {application.status === "APPROVED" ? (
            <Card>
              <CardContent className="flex items-start gap-3 py-5">
                <CalendarDays className="mt-0.5 h-5 w-5 shrink-0 text-primary-600 dark:text-primary-400" aria-hidden="true" />
                <div>
                  <p className="text-sm font-semibold text-content">Menunggu pencairan</p>
                  <p className="mt-1 text-xs leading-5 text-content-muted">
                    Pengajuan telah disetujui. Tim operasional akan mencairkan dana ke rekening
                    terdaftar dan jadwal cicilan akan aktif.
                  </p>
                </div>
              </CardContent>
            </Card>
          ) : null}
        </div>
      </div>

      {application.status === "COMPLETED" ? (
        <div className="rounded-2xl border border-success-200 bg-success-50 px-4 py-4 text-center text-sm font-medium text-success-700 dark:border-success-700/40 dark:bg-success-700/15 dark:text-success-200">
          Pinjaman ini telah lunas. Terima kasih telah menjaga riwayat pembayaran yang baik.
        </div>
      ) : null}

      {installmentsQuery.isError ? (
        <div className="rounded-xl border border-danger-200 bg-danger-50 px-4 py-3 text-sm text-danger-700 dark:border-danger-700/40 dark:bg-danger-700/15 dark:text-danger-200">
          Gagal memuat jadwal cicilan.{" "}
          <button
            type="button"
            onClick={() => installmentsQuery.refetch()}
            className="font-semibold underline underline-offset-2"
          >
            Coba lagi
          </button>
        </div>
      ) : null}

      {!canPay && application.status === "REJECTED" ? (
        <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-line bg-surface-raised px-4 py-4">
          <p className="text-sm text-content-muted">
            Anda dapat mengajukan pinjaman baru dengan nominal atau tenor yang berbeda.
          </p>
          <Link href="/loans/apply" className={buttonStyles({ variant: "secondary" })}>
            Ajukan pinjaman baru
          </Link>
        </div>
      ) : null}
    </div>
  );
}
