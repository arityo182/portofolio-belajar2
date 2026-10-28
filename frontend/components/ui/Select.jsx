"use client";

import { forwardRef, useId } from "react";
import { ChevronDown } from "lucide-react";
import { cn } from "@/lib/utils";

const SELECT_STYLES =
  "h-11 w-full appearance-none rounded-lg border border-line bg-surface-raised px-3.5 pr-10 text-sm text-content shadow-sm outline-none transition-colors focus:border-primary-500 focus:ring-2 focus:ring-primary-600/25 disabled:cursor-not-allowed disabled:bg-surface-subtle disabled:text-content-muted aria-[invalid=true]:border-danger-500";

const Select = forwardRef(function Select(
  {
    label,
    error,
    helper,
    id,
    className,
    containerClassName,
    required = false,
    children,
    ...props
  },
  ref,
) {
  const generatedId = useId().replace(/[:]/g, "");
  const fieldId = id || `select-${generatedId}`;
  const errorId = `${fieldId}-error`;
  const helperId = `${fieldId}-helper`;
  const describedBy = error ? errorId : helper ? helperId : undefined;

  return (
    <div className={cn("w-full", containerClassName)}>
      {label ? (
        <label htmlFor={fieldId} className="mb-1.5 block text-sm font-medium text-content">
          {label}
          {required ? (
            <>
              <span className="ml-0.5 text-danger-600" aria-hidden="true">
                *
              </span>
              <span className="sr-only"> (wajib)</span>
            </>
          ) : null}
        </label>
      ) : null}

      <div className="relative">
        <select
          ref={ref}
          id={fieldId}
          required={required || undefined}
          aria-invalid={error ? "true" : undefined}
          aria-describedby={describedBy}
          className={cn(SELECT_STYLES, className)}
          {...props}
        >
          {children}
        </select>
        <ChevronDown
          className="pointer-events-none absolute right-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-content-subtle"
          aria-hidden="true"
        />
      </div>

      {error ? (
        <p id={errorId} role="alert" className="mt-1.5 text-xs text-danger-600 dark:text-danger-300">
          {error}
        </p>
      ) : helper ? (
        <p id={helperId} className="mt-1.5 text-xs leading-5 text-content-muted">
          {helper}
        </p>
      ) : null}
    </div>
  );
});

export default Select;
