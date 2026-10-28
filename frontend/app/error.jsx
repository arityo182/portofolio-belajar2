"use client";

import { useEffect } from "react";
import { AlertTriangle, RotateCcw } from "lucide-react";
import { Button } from "@/components/ui/Button";

export default function GlobalError({ error, reset }) {
  useEffect(() => {
    console.error("Application error", error);
  }, [error]);

  return (
    <main id="main-content" className="flex min-h-screen items-center justify-center bg-surface px-4 py-16">
      <div
        role="alert"
        className="w-full max-w-md rounded-2xl border border-line bg-surface-raised p-8 text-center shadow-card"
      >
        <span className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-danger-50 text-danger-600 dark:bg-danger-700/15 dark:text-danger-200">
          <AlertTriangle className="h-7 w-7" aria-hidden="true" />
        </span>
        <h1 className="mt-4 text-2xl font-bold tracking-[-0.02em] text-content">
          Terjadi kesalahan
        </h1>
        <p className="mt-2 text-sm leading-6 text-content-muted">
          Kami tidak dapat menampilkan halaman ini. Silakan coba lagi. Jika masalah berlanjut, hubungi
          tim dukungan koperasi.
        </p>
        <Button
          className="mt-6"
          fullWidth
          onClick={reset}
          leftIcon={<RotateCcw className="h-4 w-4" />}
        >
          Muat ulang halaman
        </Button>
      </div>
    </main>
  );
}
