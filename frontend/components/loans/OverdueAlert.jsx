import Link from "next/link";
import { AlertTriangle, ArrowRight } from "lucide-react";
import { formatCurrency } from "@/lib/utils";

export default function OverdueAlert({ count = 0, amount = 0, item }) {
  if (!count || count < 1) {
    return null;
  }

  const href =
    item?.loanId && item?.installmentId
      ? `/loans/${item.loanId}/pay?installment=${item.installmentId}`
      : "/loans";

  return (
    <div
      role="alert"
      className="flex flex-col gap-3 rounded-2xl border border-danger-200 bg-danger-50 px-4 py-3.5 text-sm text-danger-700 dark:border-danger-700/40 dark:bg-danger-700/15 dark:text-danger-200 sm:flex-row sm:items-center sm:justify-between"
    >
      <p className="flex items-start gap-2.5">
        <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
        <span>
          <strong>{count} cicilan terlambat</strong> dengan total{" "}
          <span className="numeric font-semibold">{formatCurrency(amount)}</span>. Segera lakukan
          pembayaran untuk menghindari denda.
        </span>
      </p>
      <Link
        href={href}
        className="inline-flex shrink-0 items-center gap-1 rounded text-xs font-semibold underline-offset-2 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-danger-600"
      >
        Bayar sekarang
        <ArrowRight className="h-3.5 w-3.5" aria-hidden="true" />
      </Link>
    </div>
  );
}
