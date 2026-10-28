"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { AlertCircle, ChevronLeft, ChevronRight, Plus, Wallet } from "lucide-react";
import PageHeader from "@/components/ui/PageHeader";
import { Card, CardContent } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { buttonStyles } from "@/lib/buttonStyles";
import EmptyState from "@/components/ui/EmptyState";
import LoanFilters from "@/components/loans/LoanFilters";
import LoanTable from "@/components/loans/LoanTable";
import LoadingSkeleton from "@/components/ui/LoadingSkeleton";
import { useLoanApplications } from "@/hooks/useLoanApplication";
import { useAuth } from "@/hooks/useAuth";
import { formatNumber } from "@/lib/utils";

const PAGE_SIZE = 10;

export default function LoansView() {
  const { user } = useAuth();
  const isBorrower = (user?.roles || []).includes("BORROWER");
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);

  const query = useLoanApplications({ status, page, size: PAGE_SIZE });
  const data = query.data;
  const applications = data?.content || [];
  const totalPages = data?.totalPages || 0;
  const totalElements = data?.totalElements || 0;

  useEffect(() => {
    if (totalPages > 0 && page >= totalPages) {
      setPage(totalPages - 1);
    }
  }, [page, totalPages]);

  function handleStatusChange(nextStatus) {
    setStatus(nextStatus);
    setPage(0);
  }

  const from = totalElements === 0 ? 0 : page * PAGE_SIZE + 1;
  const to = Math.min(totalElements, page * PAGE_SIZE + applications.length);

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Pembiayaan"
        title="Pinjaman"
        description="Pantau status pengajuan, jadwal angsuran, dan sisa kewajiban pinjaman Anda."
        actions={
          isBorrower ? (
            <Link href="/loans/apply" className={buttonStyles()}>
              <Plus className="h-4 w-4" aria-hidden="true" />
              Ajukan pinjaman
            </Link>
          ) : null
        }
      />

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <LoanFilters value={status} onChange={handleStatusChange} disabled={query.isFetching} />
        <p className="text-xs text-content-subtle" aria-live="polite">
          {query.isFetching ? "Memperbarui data..." : `${formatNumber(totalElements)} pengajuan`}
        </p>
      </div>

      {query.isLoading ? (
        <LoadingSkeleton variant="table" rows={6} columns={5} />
      ) : query.isError ? (
        <Card>
          <CardContent className="py-10 text-center">
            <span className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-danger-50 text-danger-600 dark:bg-danger-700/15 dark:text-danger-200">
              <AlertCircle className="h-6 w-6" aria-hidden="true" />
            </span>
            <p className="mt-3 text-sm font-semibold text-content">Gagal memuat daftar pinjaman</p>
            <p className="mt-1 text-xs text-content-muted">
              Periksa koneksi Anda lalu coba lagi.
            </p>
            <Button variant="secondary" className="mt-4" onClick={() => query.refetch()}>
              Coba lagi
            </Button>
          </CardContent>
        </Card>
      ) : applications.length === 0 ? (
        <EmptyState
          icon={Wallet}
          title={status ? "Tidak ada pinjaman dengan status ini" : "Belum ada pengajuan pinjaman"}
          description={
            status
              ? "Ubah filter status atau kembali ke semua pinjaman."
              : "Pengajuan pinjaman yang Anda buat akan tampil di sini beserta status penilaian dan jadwal pembayarannya."
          }
          action={
            isBorrower && !status ? (
              <Link href="/loans/apply" className={buttonStyles()}>
                <Plus className="h-4 w-4" aria-hidden="true" />
                Ajukan pinjaman pertama
              </Link>
            ) : status ? (
              <Button variant="secondary" onClick={() => handleStatusChange("")}>
                Tampilkan semua
              </Button>
            ) : null
          }
        />
      ) : (
        <Card variant="flat" className="overflow-hidden">
          <LoanTable applications={applications} caption="Daftar seluruh pinjaman" />
          <div className="flex flex-col gap-3 border-t border-line px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
            <p className="text-xs text-content-muted">
              Menampilkan <span className="numeric">{from}</span>–
              <span className="numeric">{to}</span> dari{" "}
              <span className="numeric">{totalElements}</span> pengajuan
            </p>
            <div className="flex items-center gap-2">
              <Button
                variant="secondary"
                size="sm"
                onClick={() => setPage((current) => Math.max(0, current - 1))}
                disabled={page <= 0 || query.isFetching}
                leftIcon={<ChevronLeft className="h-4 w-4" />}
              >
                Sebelumnya
              </Button>
              <span className="numeric px-1 text-xs text-content-muted">
                {page + 1} / {Math.max(1, totalPages)}
              </span>
              <Button
                variant="secondary"
                size="sm"
                onClick={() => setPage((current) => current + 1)}
                disabled={page + 1 >= totalPages || query.isFetching}
                rightIcon={<ChevronRight className="h-4 w-4" />}
              >
                Berikutnya
              </Button>
            </div>
          </div>
        </Card>
      )}
    </div>
  );
}
