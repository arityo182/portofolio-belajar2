"use client";

import { LOAN_FILTER_OPTIONS } from "@/lib/constants";
import { cn } from "@/lib/utils";

export default function LoanFilters({ value, onChange, disabled = false }) {
  return (
    <div>
      <span id="loan-filter-label" className="sr-only">
        Filter status pinjaman
      </span>
      <div
        role="group"
        aria-labelledby="loan-filter-label"
        className="-mx-1 flex gap-2 overflow-x-auto px-1 pb-1"
      >
        {LOAN_FILTER_OPTIONS.map((option) => {
          const active = (value || "") === option.value;
          return (
            <button
              key={option.value || "all"}
              type="button"
              onClick={() => onChange(option.value)}
              aria-pressed={active}
              disabled={disabled}
              className={cn(
                "shrink-0 rounded-full border px-3.5 py-1.5 text-xs font-semibold transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600 focus-visible:ring-offset-2 focus-visible:ring-offset-surface disabled:opacity-60",
                active
                  ? "border-primary-600 bg-primary-600 text-white"
                  : "border-line bg-surface-raised text-content-muted hover:bg-surface-subtle hover:text-content",
              )}
            >
              {option.label}
            </button>
          );
        })}
      </div>
    </div>
  );
}
