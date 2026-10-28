import { ArrowDownRight, ArrowUpRight, Minus } from "lucide-react";
import { cn } from "@/lib/utils";

const TONES = {
  primary: "bg-primary-50 text-primary-700 dark:bg-primary-950/60 dark:text-primary-300",
  success: "bg-success-50 text-success-700 dark:bg-success-700/15 dark:text-success-200",
  warning: "bg-warning-50 text-warning-700 dark:bg-warning-700/15 dark:text-warning-200",
  danger: "bg-danger-50 text-danger-700 dark:bg-danger-700/15 dark:text-danger-200",
  info: "bg-info-50 text-info-700 dark:bg-info-700/15 dark:text-info-200",
  neutral: "bg-surface-subtle text-content-muted",
};

function resolveTrend(trend) {
  if (trend === null || trend === undefined) {
    return null;
  }
  if (typeof trend === "number") {
    return {
      direction: trend > 0 ? "up" : trend < 0 ? "down" : "flat",
      value: `${trend > 0 ? "+" : ""}${trend}%`,
      label: null,
    };
  }
  if (typeof trend === "object") {
    return {
      direction: trend.direction || "flat",
      value: trend.value ?? null,
      label: trend.label ?? null,
    };
  }
  return null;
}

export default function StatsCard({
  icon: Icon,
  label,
  value,
  helper,
  trend = null,
  tone = "primary",
  loading = false,
  className,
  valueClassName,
}) {
  const resolvedTrend = resolveTrend(trend);
  const TrendIcon =
    resolvedTrend?.direction === "up"
      ? ArrowUpRight
      : resolvedTrend?.direction === "down"
        ? ArrowDownRight
        : Minus;

  const trendColor =
    resolvedTrend?.direction === "up"
      ? "text-success-700 dark:text-success-300"
      : resolvedTrend?.direction === "down"
        ? "text-danger-700 dark:text-danger-300"
        : "text-content-muted";

  return (
    <div
      className={cn(
        "rounded-xl border border-line bg-surface-raised p-5 shadow-card transition-shadow duration-200 ease-standard hover:shadow-card-hover",
        className,
      )}
    >
      <div className="flex items-start justify-between gap-4">
        <div className="min-w-0 flex-1">
          <p className="text-sm font-medium text-content-muted">{label}</p>

          {loading ? (
            <div className="skeleton mt-2.5 h-9 w-28" aria-hidden="true" />
          ) : (
            <p
              className={cn(
                "numeric mt-2 truncate text-2xl font-semibold text-content sm:text-3xl",
                valueClassName,
              )}
            >
              {value ?? "—"}
            </p>
          )}

          {resolvedTrend ? (
            <p className={cn("mt-2 flex items-center gap-1.5 text-xs font-medium", trendColor)}>
              <TrendIcon className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />
              {resolvedTrend.value ? <span className="numeric">{resolvedTrend.value}</span> : null}
              {resolvedTrend.label ? <span className="font-normal text-content-muted">{resolvedTrend.label}</span> : null}
            </p>
          ) : helper ? (
            <p className="mt-2 text-xs leading-5 text-content-muted">{helper}</p>
          ) : null}
        </div>

        {Icon ? (
          <span
            className={cn(
              "flex h-11 w-11 shrink-0 items-center justify-center rounded-xl",
              TONES[tone] || TONES.primary,
            )}
            aria-hidden="true"
          >
            <Icon className="h-5 w-5" />
          </span>
        ) : null}
      </div>

      {resolvedTrend && helper ? (
        <p className="mt-3 border-t border-line pt-3 text-xs leading-5 text-content-muted">{helper}</p>
      ) : null}
    </div>
  );
}
