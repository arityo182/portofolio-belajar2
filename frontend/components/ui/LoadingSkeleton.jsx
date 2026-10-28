import { cn } from "@/lib/utils";

export function Skeleton({ className, ...props }) {
  return <div className={cn("skeleton h-4 w-full", className)} aria-hidden="true" {...props} />;
}

export function StatsSkeleton({ count = 4 }) {
  return (
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4" aria-hidden="true">
      {Array.from({ length: count }).map((_, index) => (
        <div key={index} className="rounded-2xl border border-line bg-surface-raised p-5 shadow-card">
          <div className="flex items-start justify-between gap-4">
            <div className="flex-1 space-y-3">
              <Skeleton className="h-3 w-24" />
              <Skeleton className="h-8 w-32" />
              <Skeleton className="h-3 w-20" />
            </div>
            <Skeleton className="h-11 w-11 rounded-xl" />
          </div>
        </div>
      ))}
    </div>
  );
}

export function TableSkeleton({ rows = 5, columns = 4 }) {
  return (
    <div className="overflow-hidden rounded-2xl border border-line bg-surface-raised shadow-card" aria-hidden="true">
      <div className="border-b border-line bg-surface-subtle px-5 py-3">
        <Skeleton className="h-3 w-32" />
      </div>
      <div className="divide-y divide-line">
        {Array.from({ length: rows }).map((_, rowIndex) => (
          <div key={rowIndex} className="grid gap-4 px-5 py-4" style={{ gridTemplateColumns: `repeat(${columns}, minmax(0, 1fr))` }}>
            {Array.from({ length: columns }).map((__, columnIndex) => (
              <Skeleton key={columnIndex} className={cn("h-4", columnIndex === 0 ? "w-4/5" : "w-3/5")} />
            ))}
          </div>
        ))}
      </div>
    </div>
  );
}

export function CardSkeleton({ lines = 3 }) {
  return (
    <div className="rounded-2xl border border-line bg-surface-raised p-6 shadow-card" aria-hidden="true">
      <Skeleton className="mb-4 h-3 w-28" />
      <div className="space-y-3">
        {Array.from({ length: lines }).map((_, index) => (
          <Skeleton key={index} className={index === 0 ? "h-5 w-2/3" : "h-3.5 w-full"} />
        ))}
      </div>
    </div>
  );
}

export default function LoadingSkeleton({ variant = "page", rows = 5, columns = 4, lines = 3 }) {
  if (variant === "stats") {
    return <StatsSkeleton count={columns} />;
  }
  if (variant === "table") {
    return <TableSkeleton rows={rows} columns={columns} />;
  }
  if (variant === "card") {
    return <CardSkeleton lines={lines} />;
  }

  return (
    <div className="space-y-6" role="status" aria-live="polite" aria-label="Memuat konten">
      <div className="space-y-3">
        <Skeleton className="h-7 w-56" />
        <Skeleton className="h-4 w-80 max-w-full" />
      </div>
      <StatsSkeleton />
      <TableSkeleton rows={rows} columns={columns} />
    </div>
  );
}
