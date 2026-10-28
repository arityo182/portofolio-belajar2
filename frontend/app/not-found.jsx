import Link from "next/link";
import { Compass } from "lucide-react";
import { buttonStyles } from "@/lib/buttonStyles";

export const metadata = {
  title: "Halaman tidak ditemukan",
  robots: { index: false, follow: false },
};

export default function NotFound() {
  return (
    <main id="main-content" className="flex min-h-screen items-center justify-center bg-surface px-4 py-16">
      <div className="w-full max-w-md rounded-2xl border border-line bg-surface-raised p-8 text-center shadow-card">
        <span className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-primary-50 text-primary-700 dark:bg-primary-950/60 dark:text-primary-300">
          <Compass className="h-7 w-7" aria-hidden="true" />
        </span>
        <p className="numeric mt-4 text-sm font-semibold text-primary-600 dark:text-primary-400">404</p>
        <h1 className="mt-1 text-2xl font-bold tracking-[-0.02em] text-content">
          Halaman tidak ditemukan
        </h1>
        <p className="mt-2 text-sm leading-6 text-content-muted">
          Tautan yang Anda buka mungkin sudah dipindahkan atau tidak lagi tersedia.
        </p>
        <Link href="/dashboard" className={buttonStyles({ className: "mt-6 w-full" })}>
          Kembali ke dasbor
        </Link>
      </div>
    </main>
  );
}
