"use client";

import { useState } from "react";
import { ChevronDown } from "lucide-react";
import { cn } from "@/lib/utils";
import { SectionHeading } from "./SectionHeading";

const faqs = [
  {
    question: "Siapa saja yang bisa mengajukan pinjaman?",
    answer:
      "WNI berusia 21–60 tahun yang memiliki usaha aktif minimal 6 bulan, memiliki KTP, NIB (Nomor Induk Berusaha), dan rekening bank atas nama pribadi atau usaha.",
  },
  {
    question: "Bagaimana AI menghitung skor kredit saya?",
    answer:
      "Model kami menganalisis lebih dari 200 variabel, seperti arus kas, konsistensi penjualan, perilaku pembayaran, dan reputasi digital usaha. Skor diperbarui secara berkala sehingga dapat meningkat seiring pertumbuhan usaha Anda.",
  },
  {
    question: "Berapa bunga dan biaya yang dikenakan?",
    answer:
      "Bunga mulai 0,9% per bulan tergantung skor kredit dan tenor. Biaya layanan maksimal 3% dari pokok pinjaman. Seluruh rincian biaya ditampilkan sebelum Anda menandatangani perjanjian.",
  },
  {
    question: "Apakah KoperasiDigital terdaftar dan diawasi OJK?",
    answer:
      "Ya. KoperasiDigital adalah penyelenggara Layanan Pendanaan Bersama Berbasis Teknologi Informasi (LPBBTI) yang berizin dan diawasi oleh Otoritas Jasa Keuangan.",
  },
  {
    question: "Bagaimana cara menjadi pemberi dana?",
    answer:
      "Daftar sebagai pemberi dana, lengkapi verifikasi identitas, lalu pilih portofolio UMKM sesuai profil risiko Anda. Pendanaan dapat dimulai dari Rp 100.000 dengan potensi imbal hasil hingga 14% per tahun.",
  },
];

export function Faq() {
  const [openIndex, setOpenIndex] = useState(0);

  return (
    <section id="faq" className="scroll-mt-20 bg-landing-muted py-14 md:py-16">
      <div className="mx-auto grid max-w-7xl gap-12 px-8 lg:grid-cols-[1fr_1.5fr] lg:gap-16">
        <SectionHeading
          eyebrow="FAQ"
          title="Pertanyaan yang sering diajukan"
          description="Belum menemukan jawaban? Tim kami siap membantu setiap hari pukul 08.00–21.00 WIB."
        />

        <ul className="flex flex-col gap-3">
          {faqs.map((faq, index) => {
            const isOpen = openIndex === index;
            const panelId = `faq-panel-${index}`;
            const buttonId = `faq-button-${index}`;
            return (
              <li key={faq.question} className="rounded-xl bg-white ring-1 ring-landing-border">
                <h3>
                  <button
                    id={buttonId}
                    type="button"
                    aria-expanded={isOpen}
                    aria-controls={panelId}
                    onClick={() => setOpenIndex(isOpen ? null : index)}
                    className="flex w-full items-center justify-between gap-4 rounded-xl px-6 py-5 text-left text-base font-semibold text-landing-navy focus-visible:outline focus-visible:outline-2 focus-visible:outline-landing-primary"
                  >
                    {faq.question}
                    <ChevronDown
                      className={cn(
                        "size-5 shrink-0 text-landing-body transition-transform duration-200",
                        isOpen && "rotate-180 text-landing-primary",
                      )}
                      aria-hidden="true"
                    />
                  </button>
                </h3>
                <div
                  id={panelId}
                  role="region"
                  aria-labelledby={buttonId}
                  className={cn(
                    "grid transition-[grid-template-rows] duration-200 ease-out",
                    isOpen ? "grid-rows-[1fr]" : "grid-rows-[0fr]",
                  )}
                >
                  <div className="overflow-hidden" inert={!isOpen}>
                    <p className="px-6 pb-6 leading-relaxed text-landing-body">{faq.answer}</p>
                  </div>
                </div>
              </li>
            );
          })}
        </ul>
      </div>
    </section>
  );
}
