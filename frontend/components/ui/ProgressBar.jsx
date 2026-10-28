import { cn } from "@/lib/utils";

export default function ProgressBar({
  value = 0,
  label,
  showValue = false,
  tone = "primary",
  className,
}) {
  const clamped = Math.min(100, Math.max(0, Number(value) || 0));
  const toneClass =
    tone === "success"
      ? "bg-success-600"
      : tone === "warning"
        ? "bg-warning-500"
        : tone === "danger"
          ? "bg-danger-600"
          : "bg-primary-600";

  return (
    <div className={cn("w-full", className)}>
      {label || showValue ? (
        <div className="mb-1.5 flex items-center justify-between gap-3 text-xs">
          {label ? <span className="font-medium text-content-muted">{label}</span> : <span />}
          {showValue ? <span className="numeric font-semibold text-content">{clamped.toFixed(0)}%</span> : null}
        </div>
      ) : null}
      <div
        className="h-2.5 w-full overflow-hidden rounded-full bg-surface-subtle"
        role="progressbar"
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={Math.round(clamped)}
        aria-label={label || "Progres"}
      >
        <div
          className={cn("h-full rounded-full transition-[width] duration-300 ease-standard", toneClass)}
          style={{ width: `${clamped}%` }}
        />
      </div>
    </div>
  );
}
