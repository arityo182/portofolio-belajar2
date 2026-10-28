"use client";

import Link from "next/link";
import {
  AlertCircle,
  ArrowRight,
  ArrowUpRight,
  Banknote,
  CalendarCheck,
  ClipboardCheck,
  Percent,
  ShieldCheck,
  Users,
  Wallet,
} from "lucide-react";
import PageHeader from "@/components/ui/PageHeader";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/Card";
import LoadingSkeleton from "@/components/ui/LoadingSkeleton";
import EmptyState from "@/components/ui/EmptyState";
import ProgressBar from "@/components/ui/ProgressBar";
import { Button } from "@/components/ui/Button";
import { buttonStyles } from "@/lib/buttonStyles";
import AreaChart from "@/components/charts/AreaChart";
import BarChart from "@/components/charts/BarChart";
import PieChart from "@/components/charts/PieChart";
import LoanTable from "@/components/loans/LoanTable";
import { useAdminDashboard } from "@/hooks/useAdmin";
import { formatCompactCurrency, formatCurrency, formatNumber } from "@/lib/utils";

function percent(value) {
  return `${Number(value || 0).toFixed(1).replace(".", ",")}%`;
}

export default function AdminDashboardView() {
  const { loans = [], metrics, isLoading, isError, refetch } = useAdminDashboard();

  if (isLoading) {
    return <LoadingSkeleton variant="page" />;
  }

  if (isError) {
    return (
      <Card>
        <CardContent className="py-12 text-center">
          <AlertCircle className="mx-auto h-6 w-6 text-danger-600" aria-hidden="true" />
          <p className="mt-2 text-sm font-semibold text-content">Gagal memuat dashboard admin</p>
          <p className="mt-1 text-xs text-content-muted">Periksa koneksi lalu coba lagi.</p>
          <Button variant="secondary" className="mt-4" onClick={refetch}>
            Coba lagi
          </Button>
        </CardContent>
      </Card>
    );
  }

  const approvedDecisions = metrics.monthlyDecisions.reduce(
    (total, month) => total + Number(month.approved || 0),
    0,
  );
  const rejectedDecisions = metrics.monthlyDecisions.reduce(
    (total, month) => total + Number(month.rejected || 0),
    0,
  );
  const decisionTotal = approvedDecisions + rejectedDecisions;
  const approvalRate = decisionTotal > 0 ? (approvedDecisions / decisionTotal) * 100 : 0;
  const activeShare =
    metrics.totalLoans > 0 ? (metrics.activeLoanCount / metrics.totalLoans) * 100 : 0;
  const pendingReview = Number(metrics.pendingReview || 0);
  const recentLoans = loans.slice(0, 5);

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Administrasi"
        title="Dashboard admin"
        description="Ringkasan kinerja portofolio, pencairan, dan kualitas kredit koperasi."
        actions={
          <>
            <Link href="/admin/reports" className={buttonStyles({ variant: "secondary" })}>
              Lihat laporan
            </Link>
            <Link href="/admin/loans" className={buttonStyles()}>
              Semua pinjaman
              <ArrowRight className="h-4 w-4" aria-hidden="true" />
            </Link>
          </>
        }
      />

      <section
        aria-label="Ringkasan portofolio"
        className="relative overflow-hidden rounded-2xl border border-primary-500/30 bg-gradient-to-br from-primary-800 via-primary-700 to-primary-600 text-white shadow-card"
      >
        <div
          className="pointer-events-none absolute inset-0 bg-grid-pattern opacity-30 [background-size:40px_40px]"
          aria-hidden="true"
        />
        <div
          className="pointer-events-none absolute -right-24 -top-24 h-72 w-72 rounded-full bg-white/10 blur-3xl"
          aria-hidden="true"
        />
        <div
          className="pointer-events-none absolute -bottom-28 left-24 h-64 w-64 rounded-full bg-info-400/20 blur-3xl"
          aria-hidden="true"
        />

        <div className="relative grid gap-8 p-6 sm:p-8 lg:grid-cols-[1.25fr_1fr] lg:items-end">
          <div>
            <p className="text-2xs font-semibold uppercase tracking-[0.18em] text-blue-100">
              Total aset terkelola (AUM)
            </p>
            <p className="numeric mt-3 text-4xl font-bold tracking-[-0.03em] sm:text-5xl">
              {formatCompactCurrency(metrics.totalAum)}
            </p>
            <p className="mt-2 text-sm text-blue-100">
              {formatCurrency(metrics.totalAum)} dari {formatNumber(metrics.activeLoanCount)} pinjaman
              aktif
            </p>

            <div className="mt-5 flex flex-wrap gap-2 text-xs font-medium">
              <span className="inline-flex items-center gap-1.5 rounded-full border border-white/20 bg-white/10 px-3 py-1.5">
                <Percent className="h-3.5 w-3.5" aria-hidden="true" />
                NPL {percent(metrics.nplRate)}
              </span>
              <span className="inline-flex items-center gap-1.5 rounded-full border border-white/20 bg-white/10 px-3 py-1.5">
                <CalendarCheck className="h-3.5 w-3.5" aria-hidden="true" />
                Pencairan hari ini {formatCompactCurrency(metrics.disbursedTodayAmount)}
              </span>
              <span className="inline-flex items-center gap-1.5 rounded-full border border-white/20 bg-white/10 px-3 py-1.5">
                <ClipboardCheck className="h-3.5 w-3.5" aria-hidden="true" />
                {formatNumber(pendingReview)} menunggu review
              </span>
            </div>
          </div>

          <dl className="grid grid-cols-2 gap-3 rounded-xl border border-white/15 bg-white/10 p-4 backdrop-blur-sm sm:grid-cols-3">
            {[
              { label: "Pengajuan", value: formatNumber(metrics.totalLoans) },
              { label: "Aktif", value: formatNumber(metrics.activeLoanCount) },
              { label: "Gagal bayar", value: formatCompactCurrency(metrics.defaultedAmount) },
            ].map((item, index) => (
              <div key={item.label} className={index === 2 ? "col-span-2 sm:col-span-1" : undefined}>
                <dt className="text-2xs uppercase tracking-[0.12em] text-blue-100">{item.label}</dt>
                <dd className="numeric mt-1.5 text-lg font-semibold">{item.value}</dd>
              </div>
            ))}
          </dl>
        </div>
      </section>

      <div className="grid gap-6 xl:grid-cols-[1.55fr_1fr]">
        <Card className="overflow-hidden">
          <CardHeader className="flex-row items-start justify-between gap-3">
            <div>
              <CardTitle>Arus pencairan</CardTitle>
              <CardDescription>Dana yang dicairkan dalam enam bulan terakhir.</CardDescription>
            </div>
            <span className="inline-flex shrink-0 items-center gap-1.5 rounded-full bg-primary-50 px-2.5 py-1 text-2xs font-semibold text-primary-700 dark:bg-primary-950/60 dark:text-primary-300">
              <Banknote className="h-3.5 w-3.5" aria-hidden="true" />
              6 bulan
            </span>
          </CardHeader>
          <CardContent className="pt-3">
            <AreaChart
              data={metrics.monthlyDisbursement}
              xKey="label"
              height={300}
              series={[{ key: "disbursed", name: "Pencairan", color: "#2563EB" }]}
            />
          </CardContent>
        </Card>

        <Card className="overflow-hidden">
          <CardHeader>
            <CardTitle>Komposisi risiko</CardTitle>
            <CardDescription>Distribusi skor pinjaman aktif yang sudah dinilai AI.</CardDescription>
          </CardHeader>
          <CardContent className="pt-0">
            {metrics.riskDistribution.length > 0 ? (
              <PieChart data={metrics.riskDistribution} height={300} />
            ) : (
              <EmptyState
                compact
                icon={Percent}
                title="Belum ada skor risiko"
                description="Skor akan tampil setelah pinjaman dinilai model AI."
              />
            )}
          </CardContent>
        </Card>
      </div>

      <div className="grid gap-6 xl:grid-cols-[1.55fr_1fr]">
        <Card>
          <CardHeader>
            <CardTitle>Keputusan review</CardTitle>
            <CardDescription>Perbandingan pengajuan disetujui dan ditolak per bulan.</CardDescription>
          </CardHeader>
          <CardContent className="pt-3">
            <BarChart
              data={metrics.monthlyDecisions}
              xKey="label"
              height={260}
              series={[
                { key: "approved", name: "Disetujui", color: "#16A34A" },
                { key: "rejected", name: "Ditolak", color: "#DC2626" },
              ]}
            />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Kualitas portofolio</CardTitle>
            <CardDescription>Indikator utama kesehatan pinjaman.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-5">
            <ProgressBar
              label="Tingkat persetujuan"
              value={approvalRate}
              showValue
              tone="success"
            />
            <ProgressBar
              label="Porsi pinjaman aktif"
              value={activeShare}
              showValue
              tone="primary"
            />
            <ProgressBar
              label="Rasio gagal bayar (NPL)"
              value={Math.min(metrics.nplRate, 100)}
              showValue
              tone="danger"
            />

            <div className="flex items-start gap-2.5 border-t border-line pt-4 text-xs leading-5 text-content-muted">
              <ShieldCheck className="mt-0.5 h-4 w-4 shrink-0 text-success-600" aria-hidden="true" />
              <p>
                Angka dihitung dari maksimum 100 pengajuan terbaru. Risiko memakai sampel 20 pinjaman
                aktif terbaru.
              </p>
            </div>
          </CardContent>
        </Card>
      </div>

      <Card variant="flat" className="overflow-hidden">
        <CardHeader className="flex-row items-start justify-between gap-3">
          <div>
            <CardTitle>Pengajuan terbaru</CardTitle>
            <CardDescription>Lima pengajuan terakhir yang masuk ke sistem.</CardDescription>
          </div>
          <Link
            href="/admin/loans"
            className="inline-flex shrink-0 items-center gap-1 rounded text-xs font-semibold text-primary-700 underline-offset-2 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600 dark:text-primary-300"
          >
            Lihat semua
            <ArrowUpRight className="h-3.5 w-3.5" aria-hidden="true" />
          </Link>
        </CardHeader>
        <CardContent className="px-0 pb-0">
          {recentLoans.length > 0 ? (
            <LoanTable applications={recentLoans} caption="Lima pengajuan terbaru" />
          ) : (
            <div className="px-5 pb-5 sm:px-6">
              <EmptyState
                compact
                icon={Wallet}
                title="Belum ada pengajuan"
                description="Pengajuan baru akan muncul di sini setelah peminjam mengirimkannya."
              />
            </div>
          )}
        </CardContent>
      </Card>

      <div className="grid gap-4 sm:grid-cols-3">
        {[
          {
            href: "/admin/users",
            icon: Users,
            title: "Kelola pengguna",
            description: "Peran, status, dan akses akun.",
          },
          {
            href: "/admin/loan-products",
            icon: Wallet,
            title: "Produk pinjaman",
            description: "Plafon, bunga, dan tenor.",
          },
          {
            href: "/reviewer/queue",
            icon: ClipboardCheck,
            title: "Antrian review",
            description: "Pengajuan menunggu keputusan.",
          },
        ].map((item) => {
          const Icon = item.icon;
          return (
            <Link
              key={item.href}
              href={item.href}
              className="group flex items-start gap-3 rounded-xl border border-line bg-surface-raised p-4 shadow-card transition-[border-color,box-shadow] hover:border-primary-200 hover:shadow-card-hover focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600 dark:hover:border-primary-800"
            >
              <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-primary-50 text-primary-700 dark:bg-primary-950/60 dark:text-primary-300">
                <Icon className="h-5 w-5" aria-hidden="true" />
              </span>
              <span className="min-w-0">
                <span className="flex items-center gap-1 text-sm font-semibold text-content group-hover:text-primary-700 dark:group-hover:text-primary-300">
                  {item.title}
                  <ArrowUpRight className="h-3.5 w-3.5" aria-hidden="true" />
                </span>
                <span className="mt-0.5 block text-xs leading-5 text-content-muted">
                  {item.description}
                </span>
              </span>
            </Link>
          );
        })}
      </div>
    </div>
  );
}
