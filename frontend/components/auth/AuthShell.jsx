import Link from "next/link";
import {
  BriefcaseBusiness,
  Globe,
  MessageCircle,
  PlayCircle,
  Send,
} from "lucide-react";
import { cn } from "@/lib/utils";

const NAV_LINKS = [
  { label: "Beranda", href: "/" },
  { label: "Tentang Kami", href: "/#tentang" },
  { label: "Cara Kerja", href: "/#cara-kerja" },
  { label: "Bantuan", href: "/#faq" },
];

export default function AuthShell({ children, active = "login" }) {
  return (
    <main
      className="relative flex min-h-screen flex-col overflow-hidden bg-cover bg-center"
      style={{ backgroundImage: "url('/images/hero-team.jpg')" }}
    >
      <div className="absolute inset-0 bg-black/40" aria-hidden="true" />

      <nav className="relative z-10 h-16 shrink-0 rounded-b-3xl bg-white/95 backdrop-blur-sm">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-8">
        <Link
          href="/login"
          className="flex items-center gap-2.5"
          aria-label="KoperasiDigital beranda"
        >
          <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-blue-600">
            <span className="text-base font-black text-white">K</span>
          </span>
          <span className="flex flex-col">
            <span className="text-base font-bold leading-none text-gray-900">
              Koperasi<span className="text-blue-600">Digital</span>
            </span>
            <span className="mt-0.5 text-[10px] leading-none text-gray-400">P2P Lending</span>
          </span>
        </Link>

        <div className="hidden items-center gap-6 md:flex">
          {NAV_LINKS.map((link) => (
            <Link
              key={link.label}
              href={link.href}
              className="text-sm text-gray-600 transition-colors hover:text-blue-600"
            >
              {link.label}
            </Link>
          ))}
        </div>

        <div className="flex items-center gap-4">
          <Link
            href="/register"
            className={cn(
              "text-sm transition-colors",
              active === "register"
                ? "border-b-2 border-blue-600 pb-1 font-semibold text-blue-600"
                : "text-gray-600 hover:text-blue-600",
            )}
          >
            Daftar
          </Link>
          <Link
            href="/login"
            className={cn(
              "text-sm transition-colors",
              active === "login"
                ? "border-b-2 border-blue-600 pb-1 font-semibold text-blue-600"
                : "text-gray-600 hover:text-blue-600",
            )}
          >
            Masuk
          </Link>
        </div>
        </div>
      </nav>

      <div className="relative z-10 mx-auto flex w-full max-w-7xl flex-1 items-center justify-center px-8 py-8 md:justify-end">
        <section className="w-full max-w-[520px] rounded-2xl bg-white p-10 shadow-[0_8px_48px_rgba(0,0,0,0.20)] sm:p-12">
          {children}
        </section>
      </div>

      <footer className="relative z-10 rounded-t-[2rem] bg-gray-900 py-6">
        <div className="mx-auto flex max-w-7xl flex-col gap-5 px-8 md:flex-row md:items-center md:justify-between">
        <div>
          <p className="mb-2 text-xs text-gray-400">Temukan kami di:</p>
          <div className="flex gap-3 text-gray-500">
            <Globe size={16} className="transition-colors hover:text-white" aria-hidden="true" />
            <MessageCircle size={16} className="transition-colors hover:text-white" aria-hidden="true" />
            <Send size={16} className="transition-colors hover:text-white" aria-hidden="true" />
            <BriefcaseBusiness size={16} className="transition-colors hover:text-white" aria-hidden="true" />
            <PlayCircle size={16} className="transition-colors hover:text-white" aria-hidden="true" />
          </div>
        </div>

        <p className="text-center text-xs text-gray-500">
          2026 - KoperasiDigital. Platform P2P Lending UMKM Indonesia
        </p>

        <div className="flex gap-4">
          <Link
            href="/legal/terms"
            className="text-xs text-gray-400 transition-colors hover:text-white"
          >
            Syarat dan Ketentuan
          </Link>
          <Link
            href="/legal/privacy"
            className="text-xs text-gray-400 transition-colors hover:text-white"
          >
            Kebijakan Privasi
          </Link>
        </div>
        </div>
      </footer>
    </main>
  );
}
