"use client";

import { useMemo } from "react";
import Link from "next/link";
import {
  AlertCircle,
  ArrowRight,
  Bell,
  CalendarClock,
  CreditCard,
  Gauge,
  Plus,
  ShieldCheck,
  Wallet,
} from "lucide-react";
import PageHeader from "@/components/ui/PageHeader";
import StatsCard from "@/components/ui/StatsCard";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/Card";
import Badge from "@/components/ui/Badge";
import Avatar from "@/components/ui/Avatar";
import EmptyState from "@/components/ui/EmptyState";
import LoadingSkeleton from "@/components/ui/LoadingSkeleton";
import { Button } from "@/components/ui/Button";
import { buttonStyles } from "@/lib/buttonStyles";
import LoanTable from "@/components/loans/LoanTable";
import OverdueAlert from "@/components/loans/OverdueAlert";
import { useAuth } from "@/hooks/useAuth";
import { useBorrowerDashboard } from "@/hooks/useBorrowerDashboard";
import {
  getAccountStatusLabel,
  getRoleLabel,
} from "@/lib/constants";
import {
  formatCompactCurrency,
  formatCurrency,
  formatDateShort,
} from "@/lib/utils";

const RISK_LABELS = {
  LOW: "Risiko rendah",
  MEDIUM: "Risiko sedang",
  HIGH: "Risiko tinggi",
};

const ACCOUNT_STATUS_VARIANT = {
  ACTIVE: "success",
  PENDING_VERIFICATION: "warning",
  SUSPENDED: "danger",
  LOCKED: "danger",
};

function displayNameFor(user) {
  if (!user?.email) {
    return "Pengguna";
  }
  const raw = user.email.split("@")[0].replace(/[._-]+/g, " ");
  return raw
    .replace(/\bumkm\b/gi, "UMKM")
    .replace(/\b\w/g, (character) => character.toUpperCase());
}

export default function BorrowerDashboard() {
  const { user } = useAuth();
  const dashboard = useBorrowerDashboard(user?.userId);
  const { summary, applications, recentApplications, isLoading, isError, refetch } = dashboard;
  const name = displayNameFor(user);
  const accountStatus = user?.status || "ACTIVE";
  const roles = user?.roles || [];

  const nextInstallment = useMemo(
    () =>
      summary.installments
        .filter((installment) => installment.status !== "PAID")
        .sort((left, right) => new Date(left.dueDate) - new Date(right.dueDate))[0] || null,
    [summary.installments],
  );

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
          <h2 className="mt-3 text-base font-semibold text-content">Gagal memuat dasbor</h2>
          <p className="mt-1 text-sm text-content-muted">
            Periksa koneksi Anda, lalu muat ulang data pinjaman.
          </p>
          <Button variant="secondary" className="mt-4" onClick={refetch}>
            Coba lagi
          </Button>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Ringkasan peminjam"
        title={`Halo, ${name}`}
        description="Pantau pinjaman aktif, cicilan, dan skor kredit terakhir Anda."
        actions={
          <>
            <Link href="/loans/apply" className={buttonStyles()}>
              <Plus className="h-4 w-4" aria-hidden="true" />
              Ajukan pinjaman
            </Link>
            <Link href="/loans" className={buttonStyles({ variant: "secondary" })}>
              Semua pinjaman
            </Link>
          </>
        }
      />

      {accountStatus !== "ACTIVE" ? (
        <div className="flex flex-col gap-3 rounded-2xl border border-warning-200 bg-warning-50 px-4 py-3.5 text-sm text-warning-700 dark:border-warning-700/40 dark:bg-warning-700/15 dark:text-warning-200 sm:flex-row sm:items-center sm:justify-between">
          <p className="flex items-start gap-2.5">
            <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
            <span>
              Status akun Anda <strong>{getAccountStatusLabel(accountStatus)}</strong>. Lengkapi
              verifikasi agar seluruh layanan dapat digunakan.
            </span>
          </p>
          <Link
            href="/profile"
            className="inline-flex shrink-0 items-center gap-1 rounded text-xs font-semibold underline-offset-2 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-warning-600"
          >
            Lengkapi profil
            <ArrowRight className="h-3.5 w-3.5" aria-hidden="true" />
          </Link>
        </div>
      ) : null}

      <OverdueAlert
        count={summary.overdueCount}
        amount={summary.overdueAmount}
        item={summary.overdueItems[0]}
      />

      {nextInstallment ? (
        <section
          aria-label="Cicilan berikutnya"
          className="overflow-hidden rounded-2xl border border-primary-200/70 bg-surface-raised shadow-card dark:border-primary-800/60"
        >
          <div className="grid md:grid-cols-[1.7fr_1fr]">
            <div className="p-5 sm:p-6">
              <p className="text-2xs font-semibold uppercase tracking-[0.16em] text-primary-700 dark:text-primary-300">
                Cicilan berikutnya
              </p>
              <div className="mt-2 flex flex-wrap items-end gap-x-4 gap-y-1">
                <p className="numeric text-3xl font-bold tracking-[-0.02em] text-content">
                  {formatCurrency(nextInstallment.amount)}
                </p>
                <p className="text-sm text-content-muted">
                  Cicilan ke-{nextInstallment.installmentNumber}
                  {nextInstallment.loanProductName
                    ? ` · ${nextInstallment.loanProductName}`
                    : ""}
                </p>
              </div>
              <p className="mt-2 inline-flex items-center gap-1.5 text-sm text-content-muted">
                <CalendarClock className="h-4 w-4 text-primary-600 dark:text-primary-300" aria-hidden="true" />
                Jatuh tempo {formatDateShort(nextInstallment.dueDate)}
              </p>
              <div className="mt-4 flex flex-wrap gap-2">
                <Link
                  href={`/loans/${nextInstallment.loanId}/pay`}
                  className={buttonStyles()}
                >
                  Bayar cicilan
                  <ArrowRight className="h-4 w-4" aria-hidden="true" />
                </Link>
                <Link
                  href={`/loans/${nextInstallment.loanId}`}
                  className={buttonStyles({ variant: "secondary" })}
                >
                  Lihat detail pinjaman
                </Link>
              </div>
            </div>

            <div className="relative hidden overflow-hidden bg-primary-700 p-6 text-white md:flex md:flex-col md:justify-between">
              <div
                className="pointer-events-none absolute inset-0 bg-grid-pattern opacity-30 [background-size:32px_32px]"
                aria-hidden="true"
              />
              <div className="relative">
                <p className="text-2xs font-semibold uppercase tracking-[0.16em] text-blue-100">
                  Total kewajiban
                </p>
                <p className="numeric mt-2 text-2xl font-semibold">
                  {formatCurrency(summary.outstandingAmount)}
                </p>
              </div>
              <p className="relative mt-6 text-xs leading-5 text-blue-100">
                Bayar tepat waktu membantu menjaga skor kredit dan akses modal berikutnya.
              </p>
            </div>
          </div>
        </section>
      ) : null}

      <section aria-labelledby="statistik-ringkasan">
        <h2 id="statistik-ringkasan" className="sr-only">
          Statistik ringkasan peminjam
        </h2>
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <StatsCard
            icon={Wallet}
            label="Pinjaman aktif"
            value={String(summary.activeLoans)}
            helper={
              summary.activeLoans > 0
                ? `Sisa kewajiban ${formatCurrency(summary.outstandingAmount)}`
                : "Belum ada pinjaman aktif"
            }
            tone="primary"
          />
          <StatsCard
            icon={CalendarClock}
            label="Jatuh tempo bulan ini"
            value={formatCompactCurrency(summary.dueThisMonthAmount)}
            helper={
              summary.dueThisMonthCount > 0
                ? `${summary.dueThisMonthCount} cicilan · ${formatCurrency(summary.dueThisMonthAmount)}`
                : "Tidak ada tagihan bulan ini"
            }
            tone="warning"
            valueClassName="text-xl sm:text-2xl"
          />
          <StatsCard
            icon={CreditCard}
            label="Total dibayar"
            value={formatCompactCurrency(summary.totalPaid)}
            helper={
              summary.paidCount > 0
                ? `${summary.paidCount} cicilan lunas · ${formatCurrency(summary.totalPaid)}`
                : "Belum ada pembayaran"
            }
            tone="success"
            valueClassName="text-xl sm:text-2xl"
          />
          <StatsCard
            icon={Gauge}
            label="Skor kredit terakhir"
            value={
              summary.latestScore ? Number(summary.latestScore.score).toFixed(1) : "—"
            }
            helper={
              summary.latestScore
                ? `${RISK_LABELS[summary.latestScore.riskCategory] || summary.latestScore.riskCategory} · ${summary.latestScore.modelVersion}`
                : "Tersedia setelah pengajuan pertama dinilai"
            }
            tone="info"
          />
        </div>
      </section>

      <div className="grid gap-6 xl:grid-cols-[1.6fr_1fr]">
        <Card variant="flat" className="overflow-hidden">
          <CardHeader className="flex-row items-start justify-between gap-3">
            <div>
              <CardTitle>Pinjaman terbaru</CardTitle>
              <CardDescription>Lima pengajuan terakhir Anda.</CardDescription>
            </div>
            {applications.length > 0 ? (
              <Link
                href="/loans"
                className="inline-flex shrink-0 items-center gap-1 rounded text-xs font-semibold text-primary-700 underline-offset-2 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600 dark:text-primary-300"
              >
                Lihat semua
                <ArrowRight className="h-3.5 w-3.5" aria-hidden="true" />
              </Link>
            ) : null}
          </CardHeader>
          <CardContent className="px-0 pb-0">
            {recentApplications.length > 0 ? (
              <LoanTable applications={recentApplications} caption="Lima pinjaman terbaru" />
            ) : (
              <div className="px-5 pb-5 sm:px-6">
                <EmptyState
                  compact
                  icon={Wallet}
                  title="Belum ada pengajuan pinjaman"
                  description="Ajukan pinjaman pertama Anda dan dapatkan keputusan berbasis AI dengan simulasi cicilan yang transparan."
                  action={
                    <Link href="/loans/apply" className={buttonStyles()}>
                      <Plus className="h-4 w-4" aria-hidden="true" />
                      Ajukan pinjaman
                    </Link>
                  }
                />
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Keamanan akun</CardTitle>
            <CardDescription>Status akun dan perlindungan sesi Anda.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center gap-3">
              <Avatar email={user?.email} size="lg" />
              <div className="min-w-0">
                <p className="truncate text-sm font-semibold text-content">{user?.email || "—"}</p>
                <p className="mt-0.5 text-xs text-content-muted">
                  Bergabung {formatDateShort(user?.createdAt)}
                </p>
                <p className="mt-1.5">
                  <Badge variant={ACCOUNT_STATUS_VARIANT[accountStatus] || "neutral"} size="sm" dot>
                    {getAccountStatusLabel(accountStatus)}
                  </Badge>
                </p>
              </div>
            </div>

            <dl className="space-y-2.5 border-t border-line pt-4 text-sm">
              <div className="flex items-center justify-between gap-3">
                <dt className="text-content-muted">Peran</dt>
                <dd className="flex flex-wrap justify-end gap-1">
                  {roles.map((role) => (
                    <Badge key={role} variant="primary" size="sm">
                      {getRoleLabel(role)}
                    </Badge>
                  ))}
                </dd>
              </div>
              <div className="flex items-center justify-between gap-3">
                <dt className="text-content-muted">Perlindungan sesi</dt>
                <dd className="flex items-center gap-1.5 text-xs font-medium text-success-700 dark:text-success-300">
                  <ShieldCheck className="h-4 w-4" aria-hidden="true" />
                  Aktif
                </dd>
              </div>
              <div className="flex items-center justify-between gap-3">
                <dt className="text-content-muted">Notifikasi real-time</dt>
                <dd className="flex items-center gap-1.5 text-xs font-medium text-content-muted">
                  <Bell className="h-4 w-4" aria-hidden="true" />
                  WebSocket
                </dd>
              </div>
            </dl>

            <Link
              href="/settings"
              className="inline-flex items-center gap-1.5 rounded text-xs font-semibold text-primary-700 underline-offset-2 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600 dark:text-primary-300"
            >
              Kelola pengaturan keamanan
              <ArrowRight className="h-3.5 w-3.5" aria-hidden="true" />
            </Link>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
