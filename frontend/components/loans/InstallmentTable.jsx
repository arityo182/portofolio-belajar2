import Link from "next/link";
import { CreditCard } from "lucide-react";
import Badge from "@/components/ui/Badge";
import { formatCurrency, formatDateShort } from "@/lib/utils";
import { getInstallmentStatusLabel } from "@/lib/constants";

const STATUS_VARIANT = {
  PAID: "success",
  PENDING: "warning",
  OVERDUE: "danger",
};

export default function InstallmentTable({ installments = [], loanId, canPay = false }) {
  if (installments.length === 0) {
    return (
      <div className="rounded-xl border border-dashed border-line px-4 py-8 text-center">
        <CreditCard className="mx-auto h-6 w-6 text-content-subtle" aria-hidden="true" />
        <p className="mt-2 text-sm font-medium text-content">Jadwal cicilan belum tersedia</p>
        <p className="mt-1 text-xs text-content-muted">
          Jadwal akan muncul setelah pinjaman disetujui dan dicairkan.
        </p>
      </div>
    );
  }

  return (
    <>
      <div className="hidden overflow-x-auto lg:block">
        <table className="w-full min-w-[760px] border-collapse text-left">
          <caption className="sr-only">Jadwal cicilan pinjaman</caption>
          <thead>
            <tr className="border-b border-line text-2xs uppercase tracking-[0.12em] text-content-subtle">
              <th scope="col" className="px-3 py-3 font-semibold">Cicilan</th>
              <th scope="col" className="px-3 py-3 font-semibold">Jatuh tempo</th>
              <th scope="col" className="px-3 py-3 font-semibold">Nominal</th>
              <th scope="col" className="px-3 py-3 font-semibold">Pokok</th>
              <th scope="col" className="px-3 py-3 font-semibold">Bunga</th>
              <th scope="col" className="px-3 py-3 font-semibold">Status</th>
              <th scope="col" className="px-3 py-3 text-right font-semibold">Aksi</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-line">
            {installments.map((installment) => {
              const paid = installment.status === "PAID";
              return (
                <tr key={installment.installmentId} className="transition-colors hover:bg-surface-subtle">
                  <td className="px-3 py-3.5 text-sm font-semibold text-content">
                    #{installment.installmentNumber}
                  </td>
                  <td className="whitespace-nowrap px-3 py-3.5 text-sm text-content-muted">
                    {formatDateShort(installment.dueDate)}
                  </td>
                  <td className="numeric whitespace-nowrap px-3 py-3.5 text-sm font-semibold text-content">
                    {formatCurrency(installment.amount)}
                  </td>
                  <td className="numeric whitespace-nowrap px-3 py-3.5 text-xs text-content-muted">
                    {formatCurrency(installment.principal)}
                  </td>
                  <td className="numeric whitespace-nowrap px-3 py-3.5 text-xs text-content-muted">
                    {formatCurrency(installment.interest)}
                  </td>
                  <td className="px-3 py-3.5">
                    <Badge variant={STATUS_VARIANT[installment.status] || "neutral"} dot>
                      {getInstallmentStatusLabel(installment.status)}
                    </Badge>
                    {paid && installment.paidAt ? (
                      <span className="mt-1 block text-2xs text-content-subtle">
                        {formatDateShort(installment.paidAt)}
                      </span>
                    ) : null}
                  </td>
                  <td className="px-3 py-3.5 text-right">
                    {!paid && canPay ? (
                      <Link
                        href={`/loans/${loanId}/pay?installment=${installment.installmentId}`}
                        className="inline-flex h-8 items-center gap-1.5 rounded-lg border border-primary-200 bg-primary-50 px-2.5 text-xs font-semibold text-primary-700 transition-colors hover:bg-primary-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600 dark:border-primary-800 dark:bg-primary-950/50 dark:text-primary-300"
                      >
                        <CreditCard className="h-3.5 w-3.5" aria-hidden="true" />
                        Bayar
                      </Link>
                    ) : (
                      <span className="text-xs text-content-subtle">{paid ? "Lunas" : "—"}</span>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      <ul className="divide-y divide-line lg:hidden">
        {installments.map((installment) => {
          const paid = installment.status === "PAID";
          return (
            <li key={installment.installmentId} className="px-4 py-4">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="text-sm font-semibold text-content">
                    Cicilan #{installment.installmentNumber}
                  </p>
                  <p className="mt-0.5 text-xs text-content-muted">
                    Jatuh tempo {formatDateShort(installment.dueDate)}
                  </p>
                </div>
                <Badge variant={STATUS_VARIANT[installment.status] || "neutral"} dot>
                  {getInstallmentStatusLabel(installment.status)}
                </Badge>
              </div>
              <p className="numeric mt-2.5 text-lg font-semibold text-content">
                {formatCurrency(installment.amount)}
              </p>
              <p className="mt-0.5 text-2xs text-content-subtle">
                Pokok {formatCurrency(installment.principal)} · Bunga {formatCurrency(installment.interest)}
              </p>
              {!paid && canPay ? (
                <Link
                  href={`/loans/${loanId}/pay?installment=${installment.installmentId}`}
                  className="mt-3 inline-flex h-9 items-center gap-1.5 rounded-lg border border-primary-200 bg-primary-50 px-3 text-xs font-semibold text-primary-700 transition-colors hover:bg-primary-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600 dark:border-primary-800 dark:bg-primary-950/50 dark:text-primary-300"
                >
                  <CreditCard className="h-3.5 w-3.5" aria-hidden="true" />
                  Bayar cicilan
                </Link>
              ) : null}
            </li>
          );
        })}
      </ul>
    </>
  );
}
