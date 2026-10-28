"use client";

import Link from "next/link";
import { AlertCircle, ArrowRight, CalendarClock, CheckCircle2, CreditCard, Wallet } from "lucide-react";
import PageHeader from "@/components/ui/PageHeader";
import StatsCard from "@/components/ui/StatsCard";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/Card";
import Badge from "@/components/ui/Badge";
import EmptyState from "@/components/ui/EmptyState";
import LoadingSkeleton from "@/components/ui/LoadingSkeleton";
import { Button } from "@/components/ui/Button";
import { buttonStyles } from "@/lib/buttonStyles";
import OverdueAlert from "@/components/loans/OverdueAlert";
import { useBorrowerDashboard } from "@/hooks/useBorrowerDashboard";
import { getInstallmentStatusLabel, LOAN_PAYABLE_STATUSES } from "@/lib/constants";
import {
  formatCompactCurrency,
  formatCurrency,
  formatDateShort,
} from "@/lib/utils";

export default function PaymentsView() {
  const { summary, isLoading, isError, refetch } = useBorrowerDashboard();

  if (isLoading) {
    return <LoadingSkeleton variant="page" />;
  }

  if (isError) {
    return (
      <Card>
        <CardContent className="py-12 text-center">
          <span className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-danger-50 text-danger-600 dark:bg-danger-700/15 dark:text-danger-200">
            <AlertCircle className="h-6 w-6" aria-hidden="true" />
          </span>
          <h2 className="mt-3 text-base font-semibold text-content">Gagal memuat pembayaran</h2>
          <p className="mt-1 text-sm text-content-muted">
            Periksa koneksi Anda, lalu muat ulang data cicilan.
          </p>
          <Button variant="secondary" className="mt-4" onClick={refetch}>
            Coba lagi
          </Button>
        </CardContent>
      </Card>
    );
  }

  const payable = summary.installments.filter((installment) =>
    LOAN_PAYABLE_STATUSES.includes(installment.loanStatus),
  );
  const unpaid = payable
    .filter((installment) => installment.status !== "PAID")
    .sort((left, right) => String(left.dueDate).localeCompare(String(right.dueDate)));
  const paid = summary.installments
    .filter((installment) => installment.status === "PAID")
    .sort((left, right) => new Date(right.paidAt || 0) - new Date(left.paidAt || 0))
    .slice(0, 5);

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Transaksi"
        title="Pembayaran"
        description="Lihat tagihan berikutnya, riwayat pembayaran, dan status verifikasi transaksi."
        actions={
          <Link href="/loans" className={buttonStyles({ variant: "secondary" })}>
            Lihat pinjaman
          </Link>
        }
      />

      <OverdueAlert
        count={summary.overdueCount}
        amount={summary.overdueAmount}
        item={summary.overdueItems[0]}
      />

      <section aria-label="Ringkasan pembayaran">
        <div className="grid gap-4 sm:grid-cols-3">
          <StatsCard
            icon={CalendarClock}
            label="Jatuh tempo bulan ini"
            value={formatCompactCurrency(summary.dueThisMonthAmount)}
            helper={`${summary.dueThisMonthCount} cicilan`}
            tone="warning"
            valueClassName="text-xl sm:text-2xl"
          />
          <StatsCard
            icon={CreditCard}
            label="Total dibayar"
            value={formatCompactCurrency(summary.totalPaid)}
            helper={`${summary.paidCount} cicilan lunas`}
            tone="success"
            valueClassName="text-xl sm:text-2xl"
          />
          <StatsCard
            icon={Wallet}
            label="Sisa kewajiban"
            value={formatCompactCurrency(summary.outstandingAmount)}
            helper={`${unpaid.length} cicilan belum dibayar`}
            tone="primary"
            valueClassName="text-xl sm:text-2xl"
          />
        </div>
      </section>

      <Card>
        <CardHeader>
          <CardTitle>Tagihan berikutnya</CardTitle>
          <CardDescription>
            Cicilan yang belum dibayar, diurutkan berdasarkan tanggal jatuh tempo.
          </CardDescription>
        </CardHeader>
        <CardContent className="px-0 pb-0">
          {unpaid.length === 0 ? (
            <div className="px-5 pb-5 sm:px-6">
              <EmptyState
                compact
                icon={CheckCircle2}
                title="Semua cicilan lunas"
                description="Tidak ada tagihan yang perlu dibayar saat ini."
              />
            </div>
          ) : (
            <ul className="divide-y divide-line">
              {unpaid.map((installment) => (
                <li
                  key={installment.installmentId}
                  className="flex flex-col gap-3 px-4 py-4 sm:flex-row sm:items-center sm:justify-between sm:px-5"
                >
                  <div className="min-w-0">
                    <p className="truncate text-sm font-semibold text-content">
                      {installment.loanProductName || "Pinjaman"}
                    </p>
                    <p className="mt-0.5 text-xs text-content-muted">
                      Cicilan #{installment.installmentNumber} · jatuh tempo{" "}
                      {formatDateShort(installment.dueDate)}
                    </p>
                  </div>
                  <div className="flex flex-wrap items-center gap-3 sm:justify-end">
                    <span className="numeric text-sm font-semibold text-content">
                      {formatCurrency(installment.amount)}
                    </span>
                    <Badge
                      variant={installment.status === "OVERDUE" ? "danger" : "warning"}
                      dot
                    >
                      {getInstallmentStatusLabel(installment.status)}
                    </Badge>
                    <Link
                      href={`/loans/${installment.loanId}/pay?installment=${installment.installmentId}`}
                      className={buttonStyles({ size: "sm" })}
                    >
                      Bayar
                      <ArrowRight className="h-3.5 w-3.5" aria-hidden="true" />
                    </Link>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Pembayaran terakhir</CardTitle>
          <CardDescription>Lima pembayaran cicilan terakhir yang berhasil.</CardDescription>
        </CardHeader>
        <CardContent className="px-0 pb-0">
          {paid.length === 0 ? (
            <div className="px-5 pb-5 sm:px-6">
              <EmptyState
                compact
                icon={CreditCard}
                title="Belum ada pembayaran"
                description="Riwayat pembayaran akan tampil setelah cicilan pertama dibayar."
              />
            </div>
          ) : (
            <ul className="divide-y divide-line">
              {paid.map((installment) => (
                <li
                  key={installment.installmentId}
                  className="flex flex-col gap-2 px-4 py-3.5 sm:flex-row sm:items-center sm:justify-between sm:px-5"
                >
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium text-content">
                      {installment.loanProductName || "Pinjaman"} · Cicilan #
                      {installment.installmentNumber}
                    </p>
                    <p className="mt-0.5 text-xs text-content-muted">
                      Dibayar {formatDateShort(installment.paidAt)}
                    </p>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="numeric text-sm font-semibold text-content">
                      {formatCurrency(installment.amount)}
                    </span>
                    <Badge variant="success" dot>
                      Lunas
                    </Badge>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
