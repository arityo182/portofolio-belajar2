import { FileText, ScanSearch, Wallet } from "lucide-react";
import { SectionHeading } from "./SectionHeading";

const steps = [
  {
    icon: FileText,
    title: "Daftar & hubungkan data usaha",
    description:
      "Isi profil usaha, unggah KTP & NIB, lalu hubungkan rekening, e-wallet, atau akun marketplace Anda. Hanya 10 menit.",
  },
  {
    icon: ScanSearch,
    title: "AI menilai kelayakan kredit",
    description:
      "Model kami menganalisis arus kas dan pola transaksi untuk menghasilkan skor kredit serta limit yang sesuai kemampuan bayar.",
  },
  {
    icon: Wallet,
    title: "Dana cair ke rekening",
    description:
      "Setelah didanai oleh pemberi dana, modal langsung ditransfer ke rekening Anda dalam 1×24 jam kerja.",
  },
];

export function HowItWorks() {
  return (
    <section id="cara-kerja" className="scroll-mt-20 bg-landing-muted py-14 md:py-16">
      <div className="mx-auto flex max-w-7xl flex-col gap-16 px-8">
        <SectionHeading
          align="center"
          eyebrow="Cara Kerja"
          title="Tiga langkah menuju modal usaha"
          description="Proses sepenuhnya online, tanpa antre dan tanpa tumpukan dokumen."
        />

        <ol className="grid gap-6 md:grid-cols-3">
          {steps.map((step, index) => {
            const Icon = step.icon;
            return (
              <li
                key={step.title}
                className="relative flex flex-col gap-5 rounded-2xl bg-white p-8 ring-1 ring-landing-border"
              >
                <div className="flex items-center justify-between">
                  <span className="flex size-12 items-center justify-center rounded-xl bg-landing-primary text-white">
                    <Icon className="size-6" aria-hidden="true" />
                  </span>
                  <span className="text-sm font-semibold text-landing-body">
                    Langkah {index + 1}
                  </span>
                </div>
                <h3 className="text-xl font-bold text-landing-navy">{step.title}</h3>
                <p className="leading-relaxed text-landing-body">{step.description}</p>
              </li>
            );
          })}
        </ol>
      </div>
    </section>
  );
}
