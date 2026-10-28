"use client";

import { useRef, useState } from "react";
import { AlertCircle, Banknote, CheckCircle2 } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { useInitiateDisbursement } from "@/hooks/useDisbursement";
import { formatCurrency, getApiErrorMessage } from "@/lib/utils";

const GATEWAYS = [
  { value: "MIDTRANS", label: "Midtrans" },
  { value: "XENDIT", label: "Xendit" },
];

function newIdempotencyKey() {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }
  return `disburse-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}

export default function DisbursementCard({ loanId, amount }) {
  const mutation = useInitiateDisbursement();
  const [gateway, setGateway] = useState("MIDTRANS");
  const [message, setMessage] = useState("");
  const [errorMessage, setErrorMessage] = useState("");
  const keyRef = useRef(null);

  async function handleDisburse() {
    setMessage("");
    setErrorMessage("");
    if (!keyRef.current) {
      keyRef.current = newIdempotencyKey();
    }
    try {
      await mutation.mutateAsync({
        payload: {
          loanApplicationId: loanId,
          amount: Number(amount),
          paymentGateway: gateway,
        },
        idempotencyKey: keyRef.current,
      });
      keyRef.current = null;
      setMessage(
        "Pencairan diinisiasi. Status pinjaman diperbarui otomatis setelah gateway mengonfirmasi.",
      );
    } catch (error) {
      setErrorMessage(
        getApiErrorMessage(error, "Pencairan belum dapat diproses. Periksa konfigurasi gateway."),
      );
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Pencairan dana</CardTitle>
        <CardDescription>
          Pinjaman telah disetujui. Inisiasi pencairan ke rekening borrower melalui payment gateway.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        {message ? (
          <div
            role="status"
            className="flex items-start gap-2 rounded-xl border border-success-200 bg-success-50 px-3.5 py-3 text-sm text-success-700 dark:border-success-700/40 dark:bg-success-700/15 dark:text-success-200"
          >
            <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
            {message}
          </div>
        ) : null}
        {errorMessage ? (
          <div
            role="alert"
            className="flex items-start gap-2 rounded-xl border border-danger-200 bg-danger-50 px-3.5 py-3 text-sm text-danger-700 dark:border-danger-700/40 dark:bg-danger-700/15 dark:text-danger-200"
          >
            <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
            {errorMessage}
          </div>
        ) : null}

        <div className="grid gap-3 sm:grid-cols-[1fr_auto] sm:items-end">
          <label className="block text-sm">
            <span className="mb-1.5 block font-medium text-content">Gateway pencairan</span>
            <select
              value={gateway}
              onChange={(event) => setGateway(event.target.value)}
              className="h-11 w-full rounded-lg border border-line bg-surface-raised px-3.5 text-sm text-content shadow-sm outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-600/25"
            >
              {GATEWAYS.map((item) => (
                <option key={item.value} value={item.value}>
                  {item.label}
                </option>
              ))}
            </select>
          </label>
          <Button
            onClick={handleDisburse}
            isLoading={mutation.isPending}
            loadingText="Memproses..."
            leftIcon={<Banknote className="h-4 w-4" />}
          >
            Cairkan {formatCurrency(amount)}
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}
