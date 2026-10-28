import Image from "next/image";
import { Check } from "lucide-react";
import { SectionHeading } from "./SectionHeading";

const points = [
  "Didirikan oleh praktisi perbankan dan data scientist Indonesia",
  "Bermitra dengan 120+ koperasi dan komunitas UMKM daerah",
  "Model kredit dilatih dengan data usaha mikro lokal",
];

export function About() {
  return (
    <section id="tentang" className="scroll-mt-20 py-14 md:py-16">
      <div className="mx-auto grid max-w-7xl items-center gap-16 px-8 lg:grid-cols-2">
        <div className="grid grid-cols-2 gap-4">
          <div className="flex flex-col gap-4">
            <div className="relative aspect-[4/5] overflow-hidden rounded-2xl">
              <Image
                src="/images/about-warung.png"
                alt="Pemilik toko batik tersenyum sambil memegang ponsel di tokonya"
                fill
                sizes="(min-width: 1024px) 25vw, 50vw"
                className="object-cover object-top"
              />
            </div>
            <div className="relative aspect-square overflow-hidden rounded-2xl">
              <Image
                src="/images/about-coffee.png"
                alt="Pemilik usaha kopi mengemas biji kopi"
                fill
                sizes="(min-width: 1024px) 25vw, 50vw"
                className="object-cover object-top"
              />
            </div>
          </div>
          <div className="flex flex-col gap-4 pt-12">
            <div className="relative aspect-square overflow-hidden rounded-2xl">
              <Image
                src="/images/about-team.png"
                alt="Tim KoperasiDigital berdiskusi di kantor Jakarta"
                fill
                sizes="(min-width: 1024px) 25vw, 50vw"
                className="object-cover object-top"
              />
            </div>
            <div className="relative aspect-[4/5] overflow-hidden rounded-2xl">
              <Image
                src="/images/about-market.png"
                alt="Pedagang kuliner menerima pembayaran QRIS dari pelanggan"
                fill
                sizes="(min-width: 1024px) 25vw, 50vw"
                className="object-cover object-top"
              />
            </div>
          </div>
        </div>

        <div className="flex flex-col gap-8">
          <SectionHeading
            eyebrow="Tentang Kami"
            title="Semangat gotong royong koperasi, dengan teknologi masa kini."
            description="Lebih dari 64 juta UMKM menggerakkan ekonomi Indonesia, namun banyak yang belum tersentuh layanan keuangan formal. Kami hadir untuk mengubah itu — menyalurkan modal secara cepat, adil, dan bertanggung jawab."
          />

          <ul className="flex flex-col gap-4">
            {points.map((point) => (
              <li key={point} className="flex items-start gap-3">
                <span className="mt-0.5 flex size-6 shrink-0 items-center justify-center rounded-full bg-landing-accent text-landing-primary">
                  <Check className="size-3.5" aria-hidden="true" />
                </span>
                <span className="leading-relaxed text-landing-navy">{point}</span>
              </li>
            ))}
          </ul>

          <a
            href="#cara-kerja"
            className="w-fit text-sm font-semibold text-landing-primary hover:underline"
          >
            Pelajari cara kerja kami →
          </a>
        </div>
      </div>
    </section>
  );
}
