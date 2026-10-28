"use client";

import { useState } from "react";
import { AlertCircle, BarChart3, RefreshCw } from "lucide-react";
import PageHeader from "@/components/ui/PageHeader";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/Card";
import DateRangePicker from "@/components/DateRangePicker";
import DataTable from "@/components/DataTable";
import Badge from "@/components/ui/Badge";
import EmptyState from "@/components/ui/EmptyState";
import { Button } from "@/components/ui/Button";
import { useReportDashboard, useReportLoans } from "@/hooks/useReports";
import {
  formatCompactCurrency,
  formatCurrency,
  formatDateShort,
  formatNumber,
  localDateKey,
} from "@/lib/utils";

function daysAgo(days) {
  const date = new Date();
  date.setDate(date.getDate() - days);
  return localDateKey(date);
}

function shortId(value) {
  return value ? String(value).slice(0, 8).toUpperCase() : "—";
}

export default function ReportsView() {
  const [from, setFrom] = useState(daysAgo(29));
  const [to, setTo] = useState(localDateKey());
  const [applied, setApplied] = useState({ from: daysAgo(29), to: localDateKey() });
  const [generated, setGenerated] = useState(false);

  const dashboardQuery = useReportDashboard({
    from: applied.from,
    to: applied.to,
    enabled: generated,
  });
  const loansQuery = useReportLoans({
    from: applied.from,
    to: applied.to,
    page: 0,
    size: 100,
    enabled: generated,
  });
  const dashboard = dashboardQuery.data;
  const rows = loansQuery.data?.content || [];
  const isError = dashboardQuery.isError || loansQuery.isError;
  const isLoading = dashboardQuery.isLoading || loansQuery.isLoading;

  const columns = [
    {
      key: "borrowerName",
      header: "Peminjam",
      sortable: true,
      render: (row) => (
        <div>
          <p className="text-sm font-semibold text-content">{row.borrowerName || "Peminjam"}</p>
          <p className="numeric text-2xs text-content-subtle">#{shortId(row.loanId)}</p>
        </div>
      ),
      exportValue: (row) => row.borrowerName,
    },
    {
      key: "amount",
      header: "Nominal",
      sortable: true,
      align: "right",
      render: (row) => (
        <span className="numeric font-semibold text-content">{formatCurrency(row.amount)}</span>
      ),
      exportValue: (row) => row.amount,
    },
    {
      key: "status",
      header: "Status",
      sortable: true,
      render: (row) => <Badge status={row.status} dot />,
      exportValue: (row) => row.status,
    },
    {
      key: "disbursedAt",
      header: "Dicairkan",
      sortable: true,
      render: (row) => (
        <span className="whitespace-nowrap text-sm text-content-muted">
          {row.disbursedAt ? formatDateShort(row.disbursedAt) : "—"}
        </span>
      ),
      exportValue: (row) => row.disbursedAt,
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Analitik"
        title="Laporan"
        description="Tinjau kinerja pinjaman, kualitas portofolio, dan rasio risiko koperasi."
      />

      <Card>
        <CardHeader>
          <CardTitle>Periode laporan</CardTitle>
          <CardDescription>Pilih rentang tanggal pengajuan yang ingin ditinjau.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <DateRangePicker
            from={from}
            to={to}
            onChange={(nextFrom, nextTo) => {
              setFrom(nextFrom || "");
              setTo(nextTo || "");
            }}
          />
          <div className="flex justify-end">
            <Button
              onClick={() => setGenerated(true)}
              disabled={Boolean(from && to && from > to)}
              leftIcon={<RefreshCw className="h-4 w-4" />}
            >
              Generate laporan
            </Button>
          </div>
        </CardContent>
      </Card>

      {!generated ? (
        <EmptyState
          icon={BarChart3}
          title="Belum ada laporan"
          description="Pilih periode lalu tekan Generate laporan untuk menampilkan ringkasan."
        />
      ) : isError ? (
        <Card>
          <CardContent className="py-10 text-center">
            <AlertCircle className="mx-auto h-6 w-6 text-danger-600" aria-hidden="true" />
            <p className="mt-2 text-sm font-semibold text-content">Gagal memuat laporan</p>
            <Button
              variant="secondary"
              className="mt-4"
              onClick={() => {
                dashboardQuery.refetch();
                loansQuery.refetch();
              }}
            >
              Coba lagi
            </Button>
          </CardContent>
        </Card>
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            {[
              { label: "Total pinjaman", value: formatNumber(dashboard?.totalLoans || 0), helper: "pengajuan" },
              {
                label: "Total dicairkan",
                value: formatCompactCurrency(dashboard?.totalDisbursed || 0),
                helper: formatCurrency(dashboard?.totalDisbursed || 0),
              },
              { label: "Pinjaman aktif", value: formatNumber(dashboard?.activeLoans || 0), helper: "pinjaman" },
              {
                label: "NPL",
                value: `${Number(dashboard?.nplRatio || 0).toFixed(2)}%`,
                helper: `${formatNumber(dashboard?.defaultedLoans || 0)} gagal bayar`,
              },
            ].map((item) => (
              <div key={item.label} className="rounded-2xl border border-line bg-surface-raised p-5 shadow-card">
                <p className="text-sm text-content-muted">{item.label}</p>
                <p className="numeric mt-2 text-2xl font-semibold text-content">{item.value}</p>
                <p className="mt-1 text-xs text-content-subtle">{item.helper}</p>
              </div>
            ))}
          </div>

          <DataTable
            columns={columns}
            data={rows}
            rowKey={(row) => row.loanId}
            loading={isLoading}
            caption="Daftar pinjaman pada periode terpilih"
            searchable
            searchPlaceholder="Cari peminjam atau ID pinjaman..."
            searchKeys={["borrowerName", "loanId"]}
            pageSize={10}
            emptyTitle="Tidak ada pinjaman pada periode ini"
            emptyDescription="Ubah rentang tanggal lalu generate ulang laporan."
          />
        </>
      )}
    </div>
  );
}
