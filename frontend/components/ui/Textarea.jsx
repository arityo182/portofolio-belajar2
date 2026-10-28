"use client";

import { forwardRef, useId } from "react";
import { cn } from "@/lib/utils";

const TEXTAREA_STYLES =
  "min-h-28 w-full resize-y rounded-lg border border-line bg-surface-raised px-3.5 py-3 text-sm text-content shadow-sm outline-none transition-colors placeholder:text-content-subtle focus:border-primary-500 focus:ring-2 focus:ring-primary-600/25 disabled:cursor-not-allowed disabled:bg-surface-subtle aria-[invalid=true]:border-danger-500 aria-[invalid=true]:focus:border-danger-500 aria-[invalid=true]:focus:ring-danger-500/25";

const Textarea = forwardRef(function Textarea(
  {
    label,
    error,
    helper,
    id,
    className,
    containerClassName,
    required = false,
    rows = 4,
    maxLength,
    showCounter = false,
    value,
    ...props
  },
  ref,
) {
  const generatedId = useId().replace(/[:]/g, "");
  const fieldId = id || `textarea-${generatedId}`;
  const errorId = `${fieldId}-error`;
  const helperId = `${fieldId}-helper`;
  const describedBy = error ? errorId : helper ? helperId : undefined;
  const length = typeof value === "string" ? value.length : 0;

  return (
    <div className={cn("w-full", containerClassName)}>
      {label ? (
        <div className="mb-1.5 flex items-baseline justify-between gap-3">
          <label htmlFor={fieldId} className="block text-sm font-medium text-content">
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
          {showCounter && maxLength ? (
            <span className="numeric text-2xs text-content-subtle">
              {length}/{maxLength}
            </span>
          ) : null}
        </div>
      ) : null}

      <textarea
        ref={ref}
        id={fieldId}
        rows={rows}
        maxLength={maxLength}
        required={required || undefined}
        aria-invalid={error ? "true" : undefined}
        aria-describedby={describedBy}
        className={cn(TEXTAREA_STYLES, className)}
        value={value}
        {...props}
      />

      {error ? (
        <p id={errorId} role="alert" className="mt-1.5 flex items-start gap-1.5 text-xs text-danger-600 dark:text-danger-300">
          <span aria-hidden="true" className="mt-px font-semibold">
            !
          </span>
          <span>{error}</span>
        </p>
      ) : helper ? (
        <p id={helperId} className="mt-1.5 text-xs leading-5 text-content-muted">
          {helper}
        </p>
      ) : null}
    </div>
  );
});

export default Textarea;
