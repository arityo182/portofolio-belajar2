"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { AlertCircle, ArrowUpRight, History } from "lucide-react";
import PageHeader from "@/components/ui/PageHeader";
import { Card, CardContent } from "@/components/ui/Card";
import DataTable from "@/components/DataTable";
import DateRangePicker from "@/components/DateRangePicker";
import Badge from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { buttonStyles } from "@/lib/buttonStyles";
import { useReviewHistory } from "@/hooks/useReviewer";
import { formatCurrency, formatDateShort, localDateKey } from "@/lib/utils";

function shortId(value) {
  return value ? String(value).slice(0, 8).toUpperCase() : "—";
}

export default function ReviewHistoryView() {
  const historyQuery = useReviewHistory();
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");

  const filtered = useMemo(() => {
    const data = historyQuery.data || [];
    if (!from && !to) {
      return data;
    }
    return data.filter((item) => {
      if (!item.reviewedAt) {
        return false;
      }
      const date = localDateKey(new Date(item.reviewedAt));
      if (from && date < from) {
        return false;
      }
      if (to && date > to) {
        return false;
      }
      return true;
    });
  }, [from, historyQuery.data, to]);

  const columns = [
    {
      key: "loanProductName",
      header: "Pengajuan",
      sortable: true,
      render: (row) => (
        <div>
          <p className="text-sm font-semibold text-content">{row.loanProductName || "Pinjaman"}</p>
          <p className="numeric mt-0.5 text-2xs text-content-subtle">#{shortId(row.loanId)}</p>
        </div>
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
      header: "Keputusan",
      sortable: true,
      render: (row) => (
        <Badge status={row.status} dot>
          {row.status === "APPROVED" ? "Disetujui" : "Ditolak"}
        </Badge>
      ),
      exportValue: (row) => row.status,
    },
    {
      key: "reviewedAt",
      header: "Direview",
      sortable: true,
      render: (row) => (
        <span className="whitespace-nowrap text-sm text-content-muted">
          {formatDateShort(row.reviewedAt)}
        </span>
      ),
      exportValue: (row) => row.reviewedAt,
    },
    {
      key: "reviewerId",
      header: "Reviewer",
      render: (row) => (
        <span className="numeric text-xs text-content-muted">{shortId(row.reviewerId)}</span>
      ),
      exportValue: (row) => row.reviewerId,
    },
    {
      key: "actions",
      header: "Aksi",
      align: "right",
      render: (row) => (
        <Link
          href={`/reviewer/${row.loanId}`}
          className="inline-flex h-8 w-8 items-center justify-center rounded-lg text-content-muted transition-colors hover:bg-surface-subtle hover:text-primary-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600 dark:hover:text-primary-300"
          aria-label="Lihat detail review"
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
        eyebrow="Review pengajuan"
        title="Riwayat review"
        description="Keputusan review yang telah dibuat beserta tanggal dan reviewernya."
        actions={
          <Link href="/reviewer/queue" className={buttonStyles({ variant: "secondary" })}>
            <History className="h-4 w-4" aria-hidden="true" />
            Antrian review
          </Link>
        }
      />

      <Card>
        <CardContent className="py-4">
          <DateRangePicker from={from} to={to} onChange={(nextFrom, nextTo) => {
            setFrom(nextFrom || "");
            setTo(nextTo || "");
          }} />
        </CardContent>
      </Card>

      {historyQuery.isError ? (
        <Card>
          <CardContent className="py-10 text-center">
            <AlertCircle className="mx-auto h-6 w-6 text-danger-600" aria-hidden="true" />
            <p className="mt-2 text-sm font-semibold text-content">Gagal memuat riwayat review</p>
            <Button variant="secondary" className="mt-4" onClick={() => historyQuery.refetch()}>
              Coba lagi
            </Button>
          </CardContent>
        </Card>
      ) : (
        <DataTable
          columns={columns}
          data={filtered}
          rowKey={(row) => row.loanId}
          loading={historyQuery.isLoading}
          caption="Riwayat keputusan review"
          searchable
          searchPlaceholder="Cari produk atau ID peminjam..."
          searchKeys={["loanProductName", "loanId", "borrowerId"]}
          pageSize={10}
          exportFileName="riwayat-review.csv"
          emptyTitle="Belum ada riwayat review"
          emptyDescription="Keputusan review akan tampil di sini setelah pengajuan diputuskan."
        />
      )}
    </div>
  );
}
