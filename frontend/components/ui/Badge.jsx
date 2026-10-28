import { cn } from "@/lib/utils";
import { LOAN_STATUS, getLoanStatusLabel } from "@/lib/constants";

const VARIANTS = {
  neutral: "border-line bg-surface-subtle text-content-muted",
  primary: "border-primary-200 bg-primary-50 text-primary-700 dark:border-primary-800 dark:bg-primary-950/50 dark:text-primary-300",
  success: "border-success-200 bg-success-50 text-success-700 dark:border-success-700/50 dark:bg-success-700/15 dark:text-success-200",
  warning: "border-warning-200 bg-warning-50 text-warning-700 dark:border-warning-700/50 dark:bg-warning-700/15 dark:text-warning-200",
  danger: "border-danger-200 bg-danger-50 text-danger-700 dark:border-danger-700/50 dark:bg-danger-700/15 dark:text-danger-200",
  info: "border-info-200 bg-info-50 text-info-700 dark:border-info-700/50 dark:bg-info-700/15 dark:text-info-200",
  outline: "border-line bg-transparent text-content-muted",
};

const STATUS_CLASSES = {
  pending: "status-pending border",
  scoring: "status-scoring border",
  review: "status-review border",
  approved: "status-approved border",
  rejected: "status-rejected border",
  disbursed: "status-disbursed border",
  active: "status-active border",
  completed: "status-completed border",
  defaulted: "status-defaulted border",
  overdue: "status-overdue border",
};

const SIZES = {
  sm: "px-2.5 py-0.5 text-2xs gap-1.5",
  md: "px-3 py-1 text-xs gap-1.5",
};

function resolveStatusKey(status) {
  if (!status) {
    return null;
  }
  const upper = String(status).toUpperCase();
  if (LOAN_STATUS[upper]) {
    return LOAN_STATUS[upper];
  }
  const lower = String(status).toLowerCase();
  return Object.prototype.hasOwnProperty.call(STATUS_CLASSES, lower) ? lower : null;
}

export default function Badge({
  children,
  variant = "neutral",
  status,
  size = "sm",
  dot = false,
  icon,
  className,
  ...props
}) {
  const statusKey = resolveStatusKey(status);
  const content = children ?? (status ? getLoanStatusLabel(status) : null);

  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full border font-medium whitespace-nowrap",
        SIZES[size] || SIZES.sm,
        statusKey ? STATUS_CLASSES[statusKey] : VARIANTS[variant] || VARIANTS.neutral,
        className,
      )}
      {...props}
    >
      {dot ? (
        <span className="h-1.5 w-1.5 shrink-0 rounded-full bg-current" aria-hidden="true" />
      ) : null}
      {icon ? (
        <span aria-hidden="true" className="shrink-0">
          {icon}
        </span>
      ) : null}
      {content}
    </span>
  );
}
