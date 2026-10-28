import Link from "next/link";
import { APP_NAME } from "@/lib/constants";

const FOOTER_LINKS = [
  { href: "/settings", label: "Bantuan" },
  { href: "/settings", label: "Keamanan" },
  { href: "/profile", label: "Akun" },
  { href: "/dashboard", label: "Dasbor" },
];

export default function Footer() {
  return (
    <footer className="border-t border-line bg-surface-raised">
      <div className="mx-auto flex w-full max-w-7xl flex-col gap-4 px-4 py-6 sm:flex-row sm:items-center sm:justify-between sm:px-6 lg:px-8">
        <div className="min-w-0">
          <p className="text-sm font-semibold text-content">
            © {new Date().getFullYear()} {APP_NAME}
          </p>
          <p className="mt-1 text-xs leading-5 text-content-muted">
            Platform P2P lending UMKM. Diawasi sesuai POJK No. 77/POJK.01/2016.
          </p>
        </div>

        <nav aria-label="Tautan footer">
          <ul className="flex flex-wrap items-center gap-x-4 gap-y-2">
            {FOOTER_LINKS.map((link) => (
              <li key={link.label}>
                <Link
                  href={link.href}
                  className="rounded text-xs font-medium text-content-muted transition-colors hover:text-primary-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600 dark:hover:text-primary-300"
                >
                  {link.label}
                </Link>
              </li>
            ))}
          </ul>
        </nav>
      </div>
    </footer>
  );
}
