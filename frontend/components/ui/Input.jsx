"use client";

import { forwardRef, useId, useState } from "react";
import { Eye, EyeOff } from "lucide-react";
import { cn } from "@/lib/utils";

const FIELD_STYLES =
  "h-11 w-full rounded-lg border border-line bg-surface-raised px-3.5 text-sm text-content shadow-sm outline-none transition-colors placeholder:text-content-subtle focus:border-primary-500 focus:ring-2 focus:ring-primary-600/25 disabled:cursor-not-allowed disabled:bg-surface-subtle disabled:text-content-muted aria-[invalid=true]:border-danger-500 aria-[invalid=true]:focus:border-danger-500 aria-[invalid=true]:focus:ring-danger-500/25";

const Input = forwardRef(function Input(
  {
    label,
    error,
    helper,
    leftIcon,
    rightElement,
    id,
    type = "text",
    className,
    containerClassName,
    required = false,
    disabled = false,
    hideLabel = false,
    ...props
  },
  ref,
) {
  const generatedId = useId().replace(/[:]/g, "");
  const inputId = id || `field-${generatedId}`;
  const errorId = `${inputId}-error`;
  const helperId = `${inputId}-helper`;
  const describedBy = error ? errorId : helper ? helperId : undefined;
  const hasLeftIcon = Boolean(leftIcon);
  const hasRightElement = Boolean(rightElement);

  return (
    <div className={cn("w-full", containerClassName)}>
      {label ? (
        <label
          htmlFor={inputId}
          className={cn(
            "mb-1.5 block text-sm font-medium text-content",
            hideLabel && "sr-only",
          )}
        >
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
        {hasLeftIcon ? (
          <span
            className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3.5 text-content-subtle"
            aria-hidden="true"
          >
            {leftIcon}
          </span>
        ) : null}

        <input
          ref={ref}
          id={inputId}
          type={type}
          required={required || undefined}
          disabled={disabled}
          aria-invalid={error ? "true" : undefined}
          aria-describedby={describedBy}
          className={cn(
            FIELD_STYLES,
            hasLeftIcon && "pl-11",
            hasRightElement && "pr-12",
            className,
          )}
          {...props}
        />

        {hasRightElement ? (
          <span className="absolute inset-y-0 right-0 flex items-center pr-1.5">
            {rightElement}
          </span>
        ) : null}
      </div>

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

const PasswordInput = forwardRef(function PasswordInput({ rightElement, ...props }, ref) {
  const [visible, setVisible] = useState(false);

  return (
    <Input
      ref={ref}
      {...props}
      type={visible ? "text" : "password"}
      rightElement={
        rightElement || (
          <button
            type="button"
            onClick={() => setVisible((current) => !current)}
            aria-label={visible ? "Sembunyikan kata sandi" : "Tampilkan kata sandi"}
            aria-pressed={visible}
            className="flex h-8 w-9 items-center justify-center rounded-lg text-content-muted transition-colors hover:bg-surface-subtle hover:text-content focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600"
          >
            {visible ? (
              <EyeOff className="h-4 w-4" aria-hidden="true" />
            ) : (
              <Eye className="h-4 w-4" aria-hidden="true" />
            )}
          </button>
        )
      }
    />
  );
});

export { PasswordInput };
export default Input;
