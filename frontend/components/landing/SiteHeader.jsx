"use client";

import { useState } from "react";
import Link from "next/link";
import { Menu, X } from "lucide-react";
import { Logo } from "./Logo";

const navLinks = [
  { href: "#tentang", label: "Tentang Kami" },
  { href: "#cara-kerja", label: "Cara Kerja" },
  { href: "#fitur", label: "Fitur" },
  { href: "#faq", label: "FAQ" },
];

export function SiteHeader() {
  const [open, setOpen] = useState(false);

  return (
    <header className="sticky top-0 z-50 rounded-b-[2rem] border-b border-landing-border bg-white text-landing-navy shadow-[0_4px_24px_rgba(0,0,0,0.08)]">
      <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-8">
        <Link href="/" aria-label="KoperasiDigital beranda">
          <Logo />
        </Link>

        <nav aria-label="Navigasi utama" className="hidden md:block">
          <ul className="flex items-center gap-8">
            {navLinks.map((link) => (
              <li key={link.href}>
                <a
                  href={link.href}
                  className="text-sm font-medium text-landing-navy/75 transition-colors hover:text-landing-navy"
                >
                  {link.label}
                </a>
              </li>
            ))}
          </ul>
        </nav>

        <div className="hidden items-center gap-3 md:flex">
          <Link
            href="/login"
            className="rounded-lg px-4 py-2 text-sm font-semibold text-landing-navy/85 transition-colors hover:text-landing-navy"
          >
            Masuk
          </Link>
          <Link
            href="/register"
            className="rounded-lg bg-landing-primary px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-landing-primary/90"
          >
            Ajukan Pinjaman
          </Link>
        </div>

        <button
          type="button"
          className="inline-flex size-10 items-center justify-center rounded-lg md:hidden"
          aria-expanded={open}
          aria-controls="mobile-nav"
          aria-label={open ? "Tutup menu" : "Buka menu"}
          onClick={() => setOpen((value) => !value)}
        >
          {open ? <X className="size-5" /> : <Menu className="size-5" />}
        </button>
      </div>

      {open ? (
        <nav
          id="mobile-nav"
          aria-label="Navigasi seluler"
          className="border-t border-landing-border px-4 pb-6 md:hidden"
        >
          <ul className="flex flex-col py-2">
            {navLinks.map((link) => (
              <li key={link.href}>
                <a
                  href={link.href}
                  onClick={() => setOpen(false)}
                  className="block py-3 text-base font-medium text-landing-navy/85"
                >
                  {link.label}
                </a>
              </li>
            ))}
          </ul>
          <Link
            href="/register"
            onClick={() => setOpen(false)}
            className="block rounded-lg bg-landing-primary px-4 py-3 text-center text-sm font-semibold text-white"
          >
            Ajukan Pinjaman
          </Link>
        </nav>
      ) : null}
    </header>
  );
}
