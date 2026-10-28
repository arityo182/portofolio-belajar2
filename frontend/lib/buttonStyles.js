import { cn } from "@/lib/utils";

const BASE_STYLES =
  "inline-flex items-center justify-center gap-2 rounded-lg font-semibold tracking-[-0.01em] transition-colors duration-150 ease-standard focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600 focus-visible:ring-offset-2 focus-visible:ring-offset-surface disabled:pointer-events-none disabled:opacity-55";

const VARIANTS = {
  primary:
    "bg-primary-600 text-white shadow-sm hover:bg-primary-700 active:bg-primary-800 dark:focus-visible:ring-primary-400 dark:focus-visible:ring-offset-surface",
  secondary:
    "border border-line bg-surface-raised text-content shadow-sm hover:border-neutral-300 hover:bg-surface-subtle active:bg-neutral-100 dark:hover:border-neutral-600 dark:active:bg-neutral-800",
  ghost:
    "text-content-muted hover:bg-surface-subtle hover:text-content active:bg-neutral-100 dark:active:bg-neutral-800",
  danger:
    "bg-danger-600 text-white shadow-sm hover:bg-danger-700 active:bg-danger-700 dark:focus-visible:ring-danger-500",
  outline:
    "border border-primary-600 bg-transparent text-primary-700 hover:bg-primary-50 dark:text-primary-300 dark:hover:bg-primary-950/40",
};

const SIZES = {
  sm: "h-9 px-3.5 text-sm",
  default: "h-11 px-5 text-sm",
  lg: "h-12 px-6 text-base",
};

export function buttonStyles({ variant = "primary", size = "default", className } = {}) {
  return cn(BASE_STYLES, VARIANTS[variant] || VARIANTS.primary, SIZES[size] || SIZES.default, className);
}

export { BASE_STYLES, VARIANTS, SIZES };
