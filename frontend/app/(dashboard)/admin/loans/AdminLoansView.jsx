"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { AlertCircle, ArrowUpRight } from "lucide-react";
import PageHeader from "@/components/ui/PageHeader";
import { Card, CardContent } from "@/components/ui/Card";
import DataTable from "@/components/DataTable";
import DateRangePicker from "@/components/DateRangePicker";
import Badge from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { useAdminLoans } from "@/hooks/useAdmin";
import { LOAN_FILTER_OPTIONS } from "@/lib/constants";
import { formatCurrency, formatDateShort, localDateKey } from "@/lib/utils";

function shortId(value) {
  return value ? String(value).slice(0, 8).toUpperCase() : "—";
}

export default function AdminLoansView() {
  const loansQuery = useAdminLoans();
  const [status, setStatus] = useState("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");

  const filtered = useMemo(() => {
    const data = loansQuery.data || [];
    return data.filter((loan) => {
      if (status && loan.status !== status) {
        return false;
      }
      if (from || to) {
        if (!loan.submittedAt) {
          return false;
        }
        const date = localDateKey(new Date(loan.submittedAt));
        if (from && date < from) {
          return false;
        }
        if (to && date > to) {
          return false;
        }
      }
      return true;
    });
  }, [from, loansQuery.data, status, to]);

  const columns = [
    {
      key: "loanProductName",
      header: "Pinjaman",
      sortable: true,
      render: (row) => (
        <Link
          href={`/loans/${row.loanId}`}
          className="group inline-flex flex-col rounded focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600"
        >
          <span className="text-sm font-semibold text-content group-hover:text-primary-700 dark:group-hover:text-primary-300">
            {row.loanProductName || "Pinjaman"}
          </span>
          <span className="numeric mt-0.5 text-2xs text-content-subtle">#{shortId(row.loanId)}</span>
        </Link>
      ),
      exportValue: (row) => row.loanProductName,
    },
    {
      key: "borrowerId",
      header: "Peminjam",
      render: (row) => (
        <span className="numeric text-xs text-content-muted">#{shortId(row.borrowerId)}</span>
      ),
      exportValue: (row) => row.borrowerId,
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
      key: "submittedAt",
      header: "Diajukan",
      sortable: true,
      render: (row) => (
        <span className="whitespace-nowrap text-sm text-content-muted">
          {formatDateShort(row.submittedAt)}
        </span>
      ),
      exportValue: (row) => row.submittedAt,
    },
    {
      key: "reviewedAt",
      header: "Diputuskan",
      render: (row) => (
        <span className="whitespace-nowrap text-sm text-content-muted">
          {row.reviewedAt ? formatDateShort(row.reviewedAt) : "—"}
        </span>
      ),
      exportValue: (row) => row.reviewedAt,
    },
    {
      key: "actions",
      header: "Aksi",
      align: "right",
      render: (row) => (
        <Link
          href={`/loans/${row.loanId}`}
          className="inline-flex h-8 w-8 items-center justify-center rounded-lg text-content-muted transition-colors hover:bg-surface-subtle hover:text-primary-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600 dark:hover:text-primary-300"
          aria-label="Lihat detail pinjaman"
        >
          <ArrowUpRight className="h-4 w-4" aria-hidden="true" />
        </Link>
      ),
      exportable: false,
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Administrasi"
        title="Semua pinjaman"
        description="Telusuri seluruh pengajuan pinjaman, status, dan tanggal keputusan."
        actions={
          <Link
            href="/admin/reports"
            className="inline-flex h-10 items-center rounded-xl border border-line bg-surface-raised px-4 text-sm font-semibold text-content shadow-sm hover:bg-surface-subtle focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600"
          >
            Buat laporan
          </Link>
        }
      />

      <Card>
        <CardContent className="space-y-3 py-4">
          <div className="flex flex-wrap items-center gap-2">
            <label className="inline-flex items-center gap-2 text-xs text-content-muted">
              <span className="sr-only">Filter status pinjaman</span>
              <select
                value={status}
                onChange={(event) => setStatus(event.target.value)}
                className="h-10 rounded-xl border border-line bg-surface-raised px-3 text-sm text-content shadow-sm outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-600/25"
              >
                {LOAN_FILTER_OPTIONS.map((option) => (
                  <option key={option.value || "all"} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
          </div>
          <DateRangePicker
            from={from}
            to={to}
            onChange={(nextFrom, nextTo) => {
              setFrom(nextFrom || "");
              setTo(nextTo || "");
            }}
            label="Tanggal pengajuan"
          />
        </CardContent>
      </Card>

      {loansQuery.isError ? (
        <Card>
          <CardContent className="py-10 text-center">
            <AlertCircle className="mx-auto h-6 w-6 text-danger-600" aria-hidden="true" />
            <p className="mt-2 text-sm font-semibold text-content">Gagal memuat data pinjaman</p>
            <Button variant="secondary" className="mt-4" onClick={() => loansQuery.refetch()}>
              Coba lagi
            </Button>
          </CardContent>
        </Card>
      ) : (
        <DataTable
          columns={columns}
          data={filtered}
          rowKey={(row) => row.loanId}
          loading={loansQuery.isLoading}
          caption="Daftar seluruh pinjaman"
          searchable
          searchPlaceholder="Cari produk atau ID peminjam..."
          searchKeys={["loanProductName", "loanId", "borrowerId"]}
          pageSize={10}
          selectable
          exportFileName="semua-pinjaman.csv"
          emptyTitle="Tidak ada pinjaman"
          emptyDescription="Ubah filter status atau rentang tanggal untuk melihat data lain."
        />
      )}
    </div>
  );
}
