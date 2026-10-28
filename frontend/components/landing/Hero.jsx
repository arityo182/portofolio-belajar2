import Image from "next/image";
import { ArrowRight } from "lucide-react";

export function Hero() {
  return (
    <section className="relative flex min-h-[50vh] w-full items-end overflow-hidden bg-landing-navy text-white">
      <Image
        src="/images/hero-team.jpg"
        alt="Tim profesional berdiskusi dalam rapat"
        fill
        priority
        sizes="100vw"
        className="object-cover"
        style={{ objectPosition: "center 20%" }}
      />
      <div className="absolute inset-0 bg-gradient-to-r from-black/70 via-black/40 to-transparent" />

      <div className="relative z-10 mx-auto w-full max-w-7xl px-8 pb-16 sm:pb-20">
        <div className="max-w-xl">
          <h1 className="text-pretty text-5xl font-bold leading-tight tracking-tight sm:text-6xl">
            Wujudkan Potensi Bisnis Anda Bersama Kami
          </h1>
          <p className="mt-5 max-w-lg text-lg leading-relaxed text-white/70">
            KoperasiDigital menghubungkan UMKM dengan pendana melalui AI credit scoring yang adil
            dan transparan.
          </p>
          <a
            href="#ajukan"
            className="mt-8 inline-flex items-center gap-2 rounded-full border border-white/40 bg-white/20 px-8 py-3.5 font-semibold text-white backdrop-blur-sm transition-all hover:bg-white/30"
          >
            Ajukan Pinjaman
            <ArrowRight className="size-4" aria-hidden="true" />
          </a>
        </div>
      </div>
    </section>
  );
}
