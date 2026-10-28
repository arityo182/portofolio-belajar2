"use client";

import { forwardRef } from "react";
import { Loader2 } from "lucide-react";
import { cn } from "@/lib/utils";
import { buttonStyles } from "@/lib/buttonStyles";

export const Button = forwardRef(function Button(
  {
    children,
    className,
    variant = "primary",
    size = "default",
    type = "button",
    isLoading = false,
    loadingText,
    leftIcon,
    rightIcon,
    disabled,
    fullWidth = false,
    ...props
  },
  ref,
) {
  return (
    <button
      ref={ref}
      type={type}
      className={buttonStyles({ variant, size, className: cn(fullWidth && "w-full", className) })}
      disabled={disabled || isLoading}
      aria-busy={isLoading || undefined}
      {...props}
    >
      {isLoading ? (
        <>
          <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />
          <span>{loadingText || children}</span>
        </>
      ) : (
        <>
          {leftIcon ? <span aria-hidden="true">{leftIcon}</span> : null}
          <span>{children}</span>
          {rightIcon ? <span aria-hidden="true">{rightIcon}</span> : null}
        </>
      )}
    </button>
  );
});

export default Button;
