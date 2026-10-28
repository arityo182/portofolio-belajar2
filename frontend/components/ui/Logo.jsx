import Link from "next/link";
import { cn } from "@/lib/utils";

export default function Logo({
  href = "/dashboard",
  showText = true,
  inverted = false,
  size = "md",
  className,
}) {
  const markSize = size === "sm" ? "h-8 w-8" : size === "lg" ? "h-11 w-11" : "h-9 w-9";
  const textSize = size === "sm" ? "text-sm" : size === "lg" ? "text-lg" : "text-base";

  const content = (
    <span className={cn("inline-flex items-center gap-2.5", className)}>
      <span
        className={cn(
          "relative flex shrink-0 items-center justify-center overflow-hidden rounded-xl bg-primary-600 text-white shadow-sm",
          markSize,
        )}
        aria-hidden="true"
      >
        <svg viewBox="0 0 32 32" className="h-full w-full" fill="none">
          <path d="M8 7.5h4.4v7.2l6.6-7.2h5.3l-7.2 7.7 7.6 9.3h-5.5L13.9 17l-1.5 1.6v6.1H8V7.5Z" fill="currentColor" />
          <circle cx="24.5" cy="9.5" r="2.2" fill="#93C5FD" />
        </svg>
      </span>
      {showText ? (
        <span className="flex flex-col leading-none">
          <span
            className={cn(
              "font-bold tracking-[-0.02em]",
              textSize,
              inverted ? "text-white" : "text-content",
            )}
          >
            <span className={inverted ? "text-white" : undefined}>Koperasi</span>
            <span
              className={cn(
                inverted ? "text-blue-200" : "text-primary-600 dark:text-primary-400",
              )}
            >
              Digital
            </span>
          </span>
          <span
            className={cn(
              "mt-1 text-2xs font-medium uppercase tracking-[0.18em]",
              inverted ? "text-white/60" : "text-content-subtle",
            )}
          >
            P2P Lending
          </span>
        </span>
      ) : null}
    </span>
  );

  if (!href) {
    return content;
  }

  return (
    <Link
      href={href}
      className="inline-flex rounded-xl focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600 focus-visible:ring-offset-2 focus-visible:ring-offset-surface"
      aria-label="Koperasi Digital — kembali ke dasbor"
    >
      {content}
    </Link>
  );
}
