import Link from "next/link";
import {
  Instagram,
  Linkedin,
  Mail,
  MapPin,
  Phone,
  Twitter,
  Youtube,
} from "lucide-react";

const productLinks = [
  { label: "Pinjaman Modal Usaha", href: "/loans/apply" },
  { label: "Pendanaan", href: "/register" },
  { label: "Skor Kredit AI", href: "#fitur" },
  { label: "Aplikasi Mobile", href: "#fitur" },
];

const companyLinks = [
  { label: "Tentang Kami", href: "#tentang" },
  { label: "Karir", href: "#tentang" },
  { label: "Blog", href: "#fitur" },
  { label: "Mitra Koperasi", href: "#tentang" },
];

const helpLinks = [
  { label: "Pusat Bantuan", href: "#faq" },
  { label: "Syarat & Ketentuan", href: "/legal/terms" },
  { label: "Kebijakan Privasi", href: "/legal/privacy" },
  { label: "Pengaduan", href: "mailto:halo@koperasidigital.id" },
];

function FooterLink({ href, children }) {
  if (href.startsWith("/")) {
    return (
      <Link
        href={href}
        className="block text-sm text-gray-500 transition-colors hover:text-white"
      >
        {children}
      </Link>
    );
  }
  return (
    <a
      href={href}
      className="block text-sm text-gray-500 transition-colors hover:text-white"
    >
      {children}
    </a>
  );
}

export function SiteFooter() {
  return (
    <footer className="overflow-hidden rounded-t-[2rem] bg-gray-900">
      <div className="mx-auto max-w-7xl px-8 pb-6 pt-10">
        <div className="grid grid-cols-1 gap-8 border-b border-gray-800 pb-8 sm:grid-cols-2 lg:grid-cols-5">
          <div className="sm:col-span-2">
            <div className="mb-4 flex items-center gap-2">
              <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-blue-600 text-sm font-black text-white">
                K
              </span>
              <span className="font-bold text-white">
                Koperasi<span className="text-blue-400">Digital</span>
              </span>
            </div>

            <p className="mb-3 max-w-xs text-sm leading-relaxed text-gray-400">
              Platform pinjaman P2P untuk UMKM Indonesia. Berizin dan diawasi oleh Otoritas
              Jasa Keuangan.
            </p>

            <div className="space-y-1.5">
              <div className="flex items-start gap-2.5">
                <MapPin
                  size={14}
                  className="mt-0.5 flex-shrink-0 text-gray-500"
                  aria-hidden="true"
                />
                <span className="text-sm text-gray-400">
                  Jl. Jend. Sudirman Kav. 52, Jakarta Selatan 12190
                </span>
              </div>
              <div className="flex items-start gap-2.5">
                <Phone
                  size={14}
                  className="mt-0.5 flex-shrink-0 text-gray-500"
                  aria-hidden="true"
                />
                <span className="text-sm text-gray-400">(021) 5000-1234</span>
              </div>
              <div className="flex items-start gap-2.5">
                <Mail
                  size={14}
                  className="mt-0.5 flex-shrink-0 text-gray-500"
                  aria-hidden="true"
                />
                <span className="text-sm text-gray-400">halo@koperasidigital.id</span>
              </div>
            </div>
          </div>

          <div>
            <h2 className="mb-3 text-xs font-bold uppercase tracking-widest text-gray-400">
              PRODUK
            </h2>
            <div className="space-y-2">
              {productLinks.map((link) => (
                <FooterLink key={link.label} href={link.href}>
                  {link.label}
                </FooterLink>
              ))}
            </div>
          </div>

          <div>
            <h2 className="mb-3 text-xs font-bold uppercase tracking-widest text-gray-400">
              PERUSAHAAN
            </h2>
            <div className="space-y-2">
              {companyLinks.map((link) => (
                <FooterLink key={link.label} href={link.href}>
                  {link.label}
                </FooterLink>
              ))}
            </div>
          </div>

          <div>
            <h2 className="mb-3 text-xs font-bold uppercase tracking-widest text-gray-400">
              BANTUAN
            </h2>
            <div className="space-y-2">
              {helpLinks.map((link) => (
                <FooterLink key={link.label} href={link.href}>
                  {link.label}
                </FooterLink>
              ))}
            </div>
          </div>
        </div>
      </div>

      <div className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-4 px-8 pt-5">
        <p className="text-xs text-gray-600">
          © 2026 PT Koperasi Digital Nusantara. Hak cipta dilindungi.
        </p>

        <div className="flex items-center gap-6">
          <Link
            href="/legal/terms"
            className="text-xs text-gray-500 transition-colors hover:text-gray-300"
          >
            Syarat &amp; Ketentuan
          </Link>
          <Link
            href="/legal/privacy"
            className="text-xs text-gray-500 transition-colors hover:text-gray-300"
          >
            Kebijakan Privasi
          </Link>
          <a
            href="mailto:halo@koperasidigital.id"
            className="text-xs text-gray-500 transition-colors hover:text-gray-300"
          >
            Pengaduan
          </a>
        </div>

        <div className="flex items-center gap-4">
          <a
            href="#"
            aria-label="Instagram KoperasiDigital"
            className="cursor-pointer text-gray-500 transition-colors hover:text-white"
          >
            <Instagram size={16} aria-hidden="true" />
          </a>
          <a
            href="#"
            aria-label="LinkedIn KoperasiDigital"
            className="cursor-pointer text-gray-500 transition-colors hover:text-white"
          >
            <Linkedin size={16} aria-hidden="true" />
          </a>
          <a
            href="#"
            aria-label="YouTube KoperasiDigital"
            className="cursor-pointer text-gray-500 transition-colors hover:text-white"
          >
            <Youtube size={16} aria-hidden="true" />
          </a>
          <a
            href="#"
            aria-label="Twitter KoperasiDigital"
            className="cursor-pointer text-gray-500 transition-colors hover:text-white"
          >
            <Twitter size={16} aria-hidden="true" />
          </a>
        </div>
      </div>

      <div className="mx-auto mt-8 max-w-7xl border-t border-gray-800 px-8 py-4">
        <p className="text-xs leading-relaxed text-gray-600">
          KoperasiDigital adalah platform teknologi finansial yang beroperasi di bawah izin dan
          pengawasan Otoritas Jasa Keuangan (OJK) sesuai POJK No. 77/POJK.01/2016. Pemberian
          pinjaman mengandung risiko. Pemberi dana wajib memahami risiko sebelum memberikan
          pinjaman.
        </p>
      </div>
    </footer>
  );
}
