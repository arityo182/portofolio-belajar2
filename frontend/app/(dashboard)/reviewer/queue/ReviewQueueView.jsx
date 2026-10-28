"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { AlertCircle, ClipboardCheck } from "lucide-react";
import PageHeader from "@/components/ui/PageHeader";
import { Card, CardContent } from "@/components/ui/Card";
import DataTable from "@/components/DataTable";
import RiskBadge from "@/components/RiskBadge";
import { Button } from "@/components/ui/Button";
import { buttonStyles } from "@/lib/buttonStyles";
import { useReviewQueue } from "@/hooks/useReviewer";
import { formatCurrency, formatDateShort } from "@/lib/utils";
import { cn } from "@/lib/utils";

const RISK_RANK = { HIGH: 3, MEDIUM: 2, LOW: 1 };

const RISK_FILTERS = [
  { value: "", label: "Semua risiko" },
  { value: "HIGH", label: "Risiko tinggi" },
  { value: "MEDIUM", label: "Risiko sedang" },
  { value: "LOW", label: "Risiko rendah" },
  { value: "UNKNOWN", label: "Belum dinilai" },
];

function shortId(value) {
  return value ? String(value).slice(0, 8).toUpperCase() : "—";
}

export default function ReviewQueueView() {
  const { data, isLoading, isError, refetch } = useReviewQueue();
  const [risk, setRisk] = useState("");

  const filtered = useMemo(() => {
    if (!risk) {
      return data;
    }
    if (risk === "UNKNOWN") {
      return data.filter((item) => !item.riskCategory);
    }
    return data.filter((item) => item.riskCategory === risk);
  }, [data, risk]);

  const columns = [
    {
      key: "loanProductName",
      header: "Pengajuan",
      sortable: true,
      render: (row) => (
        <Link
          href={`/reviewer/${row.loanId}`}
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
      key: "riskCategory",
      header: "Risiko AI",
      sortable: true,
      accessor: (row) => RISK_RANK[row.riskCategory] || 0,
      render: (row) => <RiskBadge riskCategory={row.riskCategory} />,
      exportValue: (row) => row.riskCategory || "UNKNOWN",
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
      key: "actions",
      header: "Aksi",
      align: "right",
      render: (row) => (
        <Link href={`/reviewer/${row.loanId}`} className={buttonStyles({ size: "sm" })}>
          <ClipboardCheck className="h-3.5 w-3.5" aria-hidden="true" />
          Review
        </Link>
      ),
      exportable: false,
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Review pengajuan"
        title="Antrian review"
        description="Pengajuan yang telah dinilai AI dan menunggu keputusan reviewer."
        actions={
          <Link href="/reviewer/history" className={buttonStyles({ variant: "secondary" })}>
            Riwayat review
          </Link>
        }
      />

      <div className="grid gap-3 sm:grid-cols-3">
        {[
          { label: "Menunggu review", value: data.length, tone: "text-content" },
          {
            label: "Risiko tinggi",
            value: data.filter((item) => item.riskCategory === "HIGH").length,
            tone: "text-danger-700 dark:text-danger-300",
          },
          {
            label: "Belum ada skor",
            value: data.filter((item) => !item.riskCategory).length,
            tone: "text-content-muted",
          },
        ].map((item) => (
          <div key={item.label} className="rounded-2xl border border-line bg-surface-raised px-4 py-3.5 shadow-card">
            <p className="text-xs text-content-muted">{item.label}</p>
            <p className={cn("numeric mt-1 text-2xl font-semibold", item.tone)}>{item.value}</p>
          </div>
        ))}
      </div>

      {isError ? (
        <Card>
          <CardContent className="py-10 text-center">
            <AlertCircle className="mx-auto h-6 w-6 text-danger-600" aria-hidden="true" />
            <p className="mt-2 text-sm font-semibold text-content">Gagal memuat antrian review</p>
            <Button variant="secondary" className="mt-4" onClick={() => refetch()}>
              Coba lagi
            </Button>
          </CardContent>
        </Card>
      ) : (
        <DataTable
          columns={columns}
          data={filtered}
          rowKey={(row) => row.loanId}
          loading={isLoading}
          caption="Antrian pengajuan yang menunggu review"
          searchable
          searchPlaceholder="Cari produk atau ID peminjam..."
          searchKeys={["loanProductName", "loanId", "borrowerId"]}
          pageSize={10}
          exportFileName="antrian-review.csv"
          emptyTitle="Antrian review kosong"
          emptyDescription="Tidak ada pengajuan yang menunggu review saat ini."
          filters={
            <label className="inline-flex items-center gap-2 text-xs text-content-muted">
              <span className="sr-only">Filter risiko</span>
              <select
                value={risk}
                onChange={(event) => setRisk(event.target.value)}
                className="h-10 rounded-xl border border-line bg-surface-raised px-3 text-sm text-content shadow-sm outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-600/25"
              >
                {RISK_FILTERS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
          }
        />
      )}
    </div>
  );
}
