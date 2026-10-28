import Link from "next/link";
import { ArrowRight } from "lucide-react";

export function CtaBanner() {
  return (
    <section id="ajukan" className="scroll-mt-20 py-14">
      <div className="mx-auto flex max-w-7xl flex-col items-start gap-8 rounded-3xl bg-gradient-to-br from-landing-primary via-landing-primary to-landing-navy px-8 py-14 text-white md:flex-row md:items-center md:justify-between md:px-14 md:py-16">
        <div className="flex max-w-xl flex-col gap-4">
          <h2 className="text-balance text-3xl font-extrabold tracking-tight md:text-4xl">
            Siap mengembangkan usaha Anda?
          </h2>
          <p className="text-pretty text-lg leading-relaxed text-white/80">
            Cek skor kredit gratis dalam 5 menit. Tidak memengaruhi riwayat SLIK OJK Anda.
          </p>
        </div>
        <div className="flex w-full flex-col gap-3 sm:w-auto sm:flex-row">
          <Link
            href="/register"
            className="inline-flex items-center justify-center gap-2 rounded-lg bg-white px-6 py-3.5 text-base font-semibold text-landing-primary transition-colors hover:bg-white/90"
          >
            Ajukan Sekarang
            <ArrowRight className="size-4" aria-hidden="true" />
          </Link>
          <a
            href="#faq"
            className="inline-flex items-center justify-center rounded-lg border border-white/30 px-6 py-3.5 text-base font-semibold text-white transition-colors hover:bg-white/10"
          >
            Hubungi Kami
          </a>
        </div>
      </div>
    </section>
  );
}
