"use client";

import { CalendarDays } from "lucide-react";
import Input from "@/components/ui/Input";
import { localDateKey } from "@/lib/utils";
import { cn } from "@/lib/utils";

function shiftDate(days) {
  const date = new Date();
  date.setDate(date.getDate() + days);
  return localDateKey(date);
}

const PRESETS = [
  { label: "7 hari", days: 7 },
  { label: "30 hari", days: 30 },
  { label: "90 hari", days: 90 },
];

export default function DateRangePicker({
  from,
  to,
  onChange,
  className,
  showPresets = true,
  label = "Rentang tanggal",
}) {
  const invalid = Boolean(from && to && from > to);

  function applyPreset(days) {
    onChange(shiftDate(-(days - 1)), shiftDate(0));
  }

  function applyThisMonth() {
    const now = new Date();
    const first = new Date(now.getFullYear(), now.getMonth(), 1);
    onChange(localDateKey(first), localDateKey(now));
  }

  return (
    <div className={cn("space-y-2", className)}>
      <div className="grid gap-2 sm:grid-cols-2">
        <Input
          label={`${label} — dari`}
          type="date"
          value={from || ""}
          max={to || localDateKey()}
          onChange={(event) => onChange(event.target.value, to)}
          error={invalid ? "Tanggal awal tidak boleh melewati tanggal akhir" : undefined}
        />
        <Input
          label={`${label} — sampai`}
          type="date"
          value={to || ""}
          min={from || undefined}
          max={localDateKey()}
          onChange={(event) => onChange(from, event.target.value)}
        />
      </div>

      {showPresets ? (
        <div className="flex flex-wrap items-center gap-2">
          <CalendarDays className="h-3.5 w-3.5 text-content-subtle" aria-hidden="true" />
          {PRESETS.map((preset) => (
            <button
              key={preset.days}
              type="button"
              onClick={() => applyPreset(preset.days)}
              className="rounded-full border border-line bg-surface-raised px-2.5 py-1 text-2xs font-medium text-content-muted transition-colors hover:border-primary-300 hover:text-content focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600"
            >
              {preset.label}
            </button>
          ))}
          <button
            type="button"
            onClick={applyThisMonth}
            className="rounded-full border border-line bg-surface-raised px-2.5 py-1 text-2xs font-medium text-content-muted transition-colors hover:border-primary-300 hover:text-content focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600"
          >
            Bulan ini
          </button>
          {from || to ? (
            <button
              type="button"
              onClick={() => onChange("", "")}
              className="rounded-full px-2 py-1 text-2xs font-semibold text-primary-700 underline-offset-2 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600 dark:text-primary-300"
            >
              Reset
            </button>
          ) : null}
        </div>
      ) : null}
    </div>
  );
}
