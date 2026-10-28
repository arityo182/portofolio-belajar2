"use client";

import { Check, Percent, Wallet } from "lucide-react";
import { cn, formatCurrency } from "@/lib/utils";

export default function ProductCard({ product, selected, onSelect }) {
  return (
    <button
      type="button"
      onClick={() => onSelect(product)}
      aria-pressed={selected}
      className={cn(
        "group relative flex h-full flex-col rounded-2xl border p-5 text-left transition-all duration-200 ease-standard focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600 focus-visible:ring-offset-2 focus-visible:ring-offset-surface",
        selected
          ? "border-primary-600 bg-primary-50/60 shadow-card dark:border-primary-400 dark:bg-primary-950/40"
          : "border-line bg-surface-raised shadow-card hover:border-primary-300 hover:shadow-card-hover dark:hover:border-primary-700",
      )}
    >
      <div className="flex items-start justify-between gap-3">
        <span
          className={cn(
            "flex h-11 w-11 shrink-0 items-center justify-center rounded-xl",
            selected
              ? "bg-primary-600 text-white"
              : "bg-surface-subtle text-content-muted group-hover:text-primary-700 dark:group-hover:text-primary-300",
          )}
          aria-hidden="true"
        >
          <Wallet className="h-5 w-5" />
        </span>
        <span
          className={cn(
            "flex h-6 w-6 items-center justify-center rounded-full border",
            selected
              ? "border-primary-600 bg-primary-600 text-white"
              : "border-line bg-surface-raised text-transparent",
          )}
          aria-hidden="true"
        >
          <Check className="h-3.5 w-3.5" />
        </span>
      </div>

      <h3 className="mt-4 text-base font-semibold text-content">{product.name}</h3>

      <div className="mt-3 flex items-baseline gap-1">
        <span className="numeric text-2xl font-semibold text-content">
          {Number(product.interestRateAnnual).toFixed(2)}
        </span>
        <span className="flex items-center gap-0.5 text-xs text-content-muted">
          <Percent className="h-3 w-3" aria-hidden="true" />
          /tahun
        </span>
      </div>

      <dl className="mt-4 space-y-1.5 border-t border-line pt-3 text-xs">
        <div className="flex justify-between gap-3">
          <dt className="text-content-muted">Plafon</dt>
          <dd className="numeric text-right font-medium text-content">
            {formatCurrency(product.minAmount)} – {formatCurrency(product.maxAmount)}
          </dd>
        </div>
        <div className="flex justify-between gap-3">
          <dt className="text-content-muted">Tenor</dt>
          <dd className="numeric font-medium text-content">
            {product.minTenorMonths}–{product.maxTenorMonths} bulan
          </dd>
        </div>
      </dl>

      <p className="mt-3 text-2xs leading-5 text-content-subtle">
        {selected ? "Produk dipilih" : "Klik untuk memilih produk ini"}
      </p>
    </button>
  );
}
