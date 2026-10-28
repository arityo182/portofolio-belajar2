"use client";

import { useMemo, useState } from "react";
import {
  AlertCircle,
  BarChart3,
  CheckCircle2,
  Download,
  FileText,
  RefreshCw,
} from "lucide-react";
import PageHeader from "@/components/ui/PageHeader";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/Card";
import DateRangePicker from "@/components/DateRangePicker";
import DataTable from "@/components/DataTable";
import Badge from "@/components/ui/Badge";
import EmptyState from "@/components/ui/EmptyState";
import { Button } from "@/components/ui/Button";
import { useExportLoansReport, useReportDashboard, useReportLoans } from "@/hooks/useReports";
import { reportService } from "@/services/reportService";
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

export default function AdminReportsView() {
  const [from, setFrom] = useState(daysAgo(29));
  const [to, setTo] = useState(localDateKey());
  const [applied, setApplied] = useState({ from: daysAgo(29), to: localDateKey() });
  const [generated, setGenerated] = useState(false);
  const [pdfLoading, setPdfLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [errorMessage, setErrorMessage] = useState("");

  const rangeInvalid = Boolean(from && to && from > to);
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
  const exportMutation = useExportLoansReport();

  const rows = loansQuery.data?.content || [];
  const dashboard = dashboardQuery.data;

  const summary = useMemo(
    () => ({
      count: Number(dashboard?.totalLoans || 0),
      amount: Number(dashboard?.totalDisbursed || 0),
      approved: Number(dashboard?.loansByStatus?.APPROVED || 0),
      disbursed:
        Number(dashboard?.loansByStatus?.DISBURSED || 0) +
        Number(dashboard?.loansByStatus?.ACTIVE || 0) +
        Number(dashboard?.loansByStatus?.COMPLETED || 0),
    }),
    [dashboard],
  );

  function generate() {
    if (rangeInvalid) {
      return;
    }
    setApplied({ from, to });
    setGenerated(true);
    setMessage("Laporan berhasil dibuat dari data terbaru.");
    setErrorMessage("");
  }

  async function exportCsv() {
    setErrorMessage("");
    setMessage("");
    try {
      await exportMutation.mutateAsync({ from: applied.from, to: applied.to });
      setMessage("CSV berhasil diunduh.");
    } catch {
      setErrorMessage("Gagal mengunduh CSV. Coba lagi.");
    }
  }

  async function exportPdf() {
    setPdfLoading(true);
    setMessage("");
    setErrorMessage("");
    try {
      await reportService.exportPdf({ from: applied.from, to: applied.to });
      setMessage("PDF berhasil diunduh.");
    } catch {
      setErrorMessage("Gagal mengunduh PDF. Coba lagi.");
    } finally {
      setPdfLoading(false);
    }
  }

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
    {
      key: "outstandingAmount",
      header: "Sisa kewajiban",
      sortable: true,
      align: "right",
      render: (row) => (
        <span className="numeric text-sm text-content-muted">
          {formatCurrency(row.outstandingAmount)}
        </span>
      ),
      exportValue: (row) => row.outstandingAmount,
    },
  ];

  const isLoading = dashboardQuery.isLoading || loansQuery.isLoading;
  const isError = dashboardQuery.isError || loansQuery.isError;

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Administrasi"
        title="Laporan"
        description="Buat laporan pinjaman berdasarkan periode dan unduh dalam format CSV atau PDF."
        actions={
          <>
            <Button
              variant="secondary"
              onClick={exportCsv}
              isLoading={exportMutation.isPending}
              loadingText="Menyiapkan CSV..."
              leftIcon={<Download className="h-4 w-4" />}
              disabled={!generated || rows.length === 0}
            >
              Export CSV
            </Button>
            <Button
              variant="secondary"
              onClick={exportPdf}
              isLoading={pdfLoading}
              loadingText="Menyiapkan PDF..."
              leftIcon={<FileText className="h-4 w-4" />}
              disabled={!generated || rows.length === 0}
            >
              Export PDF
            </Button>
          </>
        }
      />

      {message ? (
        <div
          role="status"
          className="flex items-center gap-2 rounded-xl border border-success-200 bg-success-50 px-4 py-3 text-sm text-success-700 dark:border-success-700/40 dark:bg-success-700/15 dark:text-success-200"
        >
          <CheckCircle2 className="h-4 w-4" aria-hidden="true" />
          {message}
        </div>
      ) : null}
      {errorMessage ? (
        <div
          role="alert"
          className="flex items-center gap-2 rounded-xl border border-danger-200 bg-danger-50 px-4 py-3 text-sm text-danger-700 dark:border-danger-700/40 dark:bg-danger-700/15 dark:text-danger-200"
        >
          <AlertCircle className="h-4 w-4" aria-hidden="true" />
          {errorMessage}
        </div>
      ) : null}

      <Card>
        <CardHeader>
          <CardTitle>Periode laporan</CardTitle>
          <CardDescription>Pilih rentang tanggal pengajuan yang ingin dilaporkan.</CardDescription>
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
            <Button onClick={generate} disabled={rangeInvalid} leftIcon={<RefreshCw className="h-4 w-4" />}>
              Generate laporan
            </Button>
          </div>
        </CardContent>
      </Card>

      {!generated ? (
        <EmptyState
          icon={BarChart3}
          title="Belum ada laporan"
          description="Pilih periode lalu tekan Generate laporan untuk menampilkan data."
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
              { label: "Total pinjaman", value: formatNumber(summary.count), helper: "pengajuan" },
              {
                label: "Total nominal",
                value: formatCompactCurrency(summary.amount),
                helper: formatCurrency(summary.amount),
              },
              { label: "Disetujui", value: formatNumber(summary.approved), helper: "pengajuan" },
              { label: "Dicairkan", value: formatNumber(summary.disbursed), helper: "pinjaman" },
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
            caption="Laporan pinjaman berdasarkan periode"
            searchable
            searchPlaceholder="Cari peminjam atau ID pinjaman..."
            searchKeys={["borrowerName", "loanId"]}
            pageSize={10}
            exportFileName={`laporan-pinjaman-${applied.from || "awal"}-${applied.to || "akhir"}.csv`}
            emptyTitle="Tidak ada pinjaman pada periode ini"
            emptyDescription="Ubah rentang tanggal lalu generate ulang laporan."
          />
        </>
      )}
    </div>
  );
}
