import { forwardRef } from "react";
import { cn } from "@/lib/utils";

const Card = forwardRef(function Card(
  { children, className, variant = "default", as: Component = "section", ...props },
  ref,
) {
  return (
    <Component
      ref={ref}
      className={cn(
        "rounded-xl border border-line bg-surface-raised",
        variant === "default" && "shadow-card",
        variant === "flat" && "shadow-none",
        variant === "interactive" &&
          "shadow-card transition-shadow duration-200 ease-standard hover:shadow-card-hover",
        className,
      )}
      {...props}
    >
      {children}
    </Component>
  );
});

function CardHeader({ children, className, ...props }) {
  return (
    <div className={cn("flex flex-col gap-1 px-5 pt-5 sm:px-6 sm:pt-6", className)} {...props}>
      {children}
    </div>
  );
}

function CardTitle({ children, className, as: Component = "h2", ...props }) {
  return (
    <Component className={cn("text-base font-semibold text-content", className)} {...props}>
      {children}
    </Component>
  );
}

function CardDescription({ children, className, ...props }) {
  return (
    <p className={cn("text-sm leading-6 text-content-muted", className)} {...props}>
      {children}
    </p>
  );
}

function CardContent({ children, className, ...props }) {
  return (
    <div className={cn("px-5 py-5 sm:px-6", className)} {...props}>
      {children}
    </div>
  );
}

function CardFooter({ children, className, ...props }) {
  return (
    <div
      className={cn(
        "flex flex-wrap items-center justify-end gap-3 border-t border-line px-5 py-4 sm:px-6",
        className,
      )}
      {...props}
    >
      {children}
    </div>
  );
}

export { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter };
export default Card;
