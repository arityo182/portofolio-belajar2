export default function Loading() {
  return (
    <div
      className="flex min-h-[60vh] items-center justify-center bg-surface px-4"
      role="status"
      aria-live="polite"
      aria-label="Memuat halaman"
    >
      <div className="flex flex-col items-center gap-3">
        <span
          className="h-9 w-9 animate-spin rounded-full border-2 border-primary-200 border-t-primary-600 dark:border-neutral-700 dark:border-t-primary-400"
          aria-hidden="true"
        />
        <p className="text-sm text-content-muted">Memuat...</p>
      </div>
    </div>
  );
}
