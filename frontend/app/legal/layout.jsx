import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import Logo from "@/components/ui/Logo";

export const metadata = {
  title: "Informasi Legal",
};

export default function LegalLayout({ children }) {
  return (
    <div className="min-h-screen bg-surface">
      <header className="border-b border-line bg-surface-raised">
        <div className="mx-auto flex h-16 max-w-4xl items-center justify-between px-4 sm:px-6">
          <Logo href="/" size="sm" />
          <Link
            href="/login"
            className="inline-flex items-center gap-1.5 rounded text-sm font-medium text-content-muted hover:text-content focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600"
          >
            <ArrowLeft className="h-4 w-4" aria-hidden="true" />
            Kembali
          </Link>
        </div>
      </header>

      <main id="main-content" className="mx-auto max-w-4xl px-4 py-10 sm:px-6 sm:py-14">
        <article className="rounded-2xl border border-line bg-surface-raised px-5 py-8 shadow-card sm:px-10 sm:py-10">
          {children}
        </article>
      </main>

      <footer className="border-t border-line py-6">
        <p className="mx-auto max-w-4xl px-4 text-xs text-content-subtle sm:px-6">
          © {new Date().getFullYear()} Koperasi Digital. Dokumen ini berlaku efektif sejak 1 Januari
          2026.
        </p>
      </footer>
    </div>
  );
}
