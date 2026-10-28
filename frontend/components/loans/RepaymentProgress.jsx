import ProgressBar from "@/components/ui/ProgressBar";
import { formatCurrency } from "@/lib/utils";

export default function RepaymentProgress({ installments = [] }) {
  const total = installments.length;
  const paidInstallments = installments.filter((installment) => installment.status === "PAID");
  const paidCount = paidInstallments.length;
  const paidAmount = paidInstallments.reduce(
    (sum, installment) => sum + Number(installment.amount || 0),
    0,
  );
  const totalAmount = installments.reduce(
    (sum, installment) => sum + Number(installment.amount || 0),
    0,
  );
  const percentage = total > 0 ? (paidCount / total) * 100 : 0;

  return (
    <div className="space-y-4">
      <ProgressBar
        value={percentage}
        label={`${paidCount} dari ${total || 0} cicilan terbayar`}
        showValue
        tone={percentage === 100 ? "success" : "primary"}
      />

      <dl className="grid gap-3 sm:grid-cols-3">
        <div className="rounded-xl border border-line bg-surface-subtle px-3.5 py-3">
          <dt className="text-2xs uppercase tracking-wide text-content-subtle">Terbayar</dt>
          <dd className="numeric mt-1 text-sm font-semibold text-content">
            {formatCurrency(paidAmount)}
          </dd>
        </div>
        <div className="rounded-xl border border-line bg-surface-subtle px-3.5 py-3">
          <dt className="text-2xs uppercase tracking-wide text-content-subtle">Total tagihan</dt>
          <dd className="numeric mt-1 text-sm font-semibold text-content">
            {formatCurrency(totalAmount)}
          </dd>
        </div>
        <div className="rounded-xl border border-line bg-surface-subtle px-3.5 py-3">
          <dt className="text-2xs uppercase tracking-wide text-content-subtle">Sisa</dt>
          <dd className="numeric mt-1 text-sm font-semibold text-content">
            {formatCurrency(Math.max(0, totalAmount - paidAmount))}
          </dd>
        </div>
      </dl>
    </div>
  );
}
