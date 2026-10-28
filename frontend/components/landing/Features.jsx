import { BrainCircuit, Clock, HandCoins, Lock, Percent, Users } from "lucide-react";
import { SectionHeading } from "./SectionHeading";

const features = [
  {
    icon: BrainCircuit,
    title: "Skor Kredit AI",
    description:
      "Penilaian berbasis data transaksi nyata, sehingga usaha tanpa riwayat bank tetap punya peluang.",
  },
  {
    icon: Clock,
    title: "Pencairan 24 Jam",
    description:
      "Dari pengajuan hingga dana masuk rekening dalam satu hari kerja setelah didanai.",
  },
  {
    icon: Percent,
    title: "Bunga Transparan",
    description:
      "Mulai 0,9% per bulan. Semua biaya ditampilkan di awal — tanpa biaya tersembunyi.",
  },
  {
    icon: HandCoins,
    title: "Tanpa Agunan",
    description:
      "Tidak perlu menjaminkan sertifikat atau BPKB. Kinerja usaha Anda adalah jaminannya.",
  },
  {
    icon: Lock,
    title: "Keamanan Data",
    description:
      "Enkripsi end-to-end dan sertifikasi ISO 27001. Data Anda tidak pernah dijual ke pihak ketiga.",
  },
  {
    icon: Users,
    title: "Pendampingan Usaha",
    description:
      "Akses kelas literasi keuangan dan mentor bisnis dari jaringan koperasi mitra kami.",
  },
];

export function Features() {
  return (
    <section id="fitur" className="scroll-mt-20 py-14 md:py-16">
      <div className="mx-auto flex max-w-7xl flex-col gap-16 px-8">
        <SectionHeading
          eyebrow="Fitur"
          title="Dirancang khusus untuk kebutuhan UMKM"
          description="Semua yang Anda perlukan untuk mendapatkan modal dan mengelola pinjaman, dalam satu aplikasi."
        />

        <ul className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {features.map((feature) => {
            const Icon = feature.icon;
            return (
              <li
                key={feature.title}
                className="group flex flex-col gap-4 rounded-2xl border border-landing-border bg-white p-8 transition-all hover:-translate-y-1 hover:border-landing-primary/40 hover:shadow-lg hover:shadow-landing-primary/5"
              >
                <span className="flex size-12 items-center justify-center rounded-xl bg-landing-accent text-landing-primary transition-colors group-hover:bg-landing-primary group-hover:text-white">
                  <Icon className="size-6" aria-hidden="true" />
                </span>
                <h3 className="text-lg font-bold text-landing-navy">{feature.title}</h3>
                <p className="leading-relaxed text-landing-body">{feature.description}</p>
              </li>
            );
          })}
        </ul>
      </div>
    </section>
  );
}
