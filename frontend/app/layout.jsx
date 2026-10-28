import "./globals.css";
import { Plus_Jakarta_Sans } from "next/font/google";
import { AppProviders } from "@/components/providers/AppProviders";

const jakarta = Plus_Jakarta_Sans({
  subsets: ["latin"],
  variable: "--font-jakarta",
  display: "swap",
});

export const metadata = {
  metadataBase: new URL(process.env.NEXT_PUBLIC_APP_URL || "https://localhost"),
  title: {
    default: "Koperasi Digital — P2P Lending UMKM Indonesia",
    template: "%s | Koperasi Digital",
  },
  description:
    "Platform koperasi digital dengan AI credit scoring dan pembayaran digital untuk UMKM Indonesia. Aman, transparan, dan berizin.",
  applicationName: "Koperasi Digital",
  keywords: ["koperasi digital", "p2p lending", "UMKM", "pinjaman usaha", "investasi"],
  authors: [{ name: "Koperasi Digital" }],
  creator: "Koperasi Digital",
  robots: {
    index: true,
    follow: true,
  },
  icons: {
    icon: "/favicon.svg",
  },
  openGraph: {
    type: "website",
    locale: "id_ID",
    siteName: "Koperasi Digital",
    title: "Koperasi Digital — P2P Lending UMKM Indonesia",
    description:
      "Akses modal usaha yang adil dengan AI credit scoring, transparansi cicilan, dan pembayaran digital yang aman.",
  },
  formatDetection: {
    email: false,
    telephone: false,
  },
};

export const viewport = {
  width: "device-width",
  initialScale: 1,
  maximumScale: 5,
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: "#F6F8FC" },
    { media: "(prefers-color-scheme: dark)", color: "#070B14" },
  ],
};

const THEME_INIT_SCRIPT = `
(function () {
  try {
    // Light-first: only an explicit user preference switches the app to dark mode.
    var stored = window.localStorage.getItem("koperasi-theme");
    var isDark = stored === "dark";
    var root = document.documentElement;
    root.classList.toggle("dark", isDark);
    root.dataset.theme = isDark ? "dark" : "light";
    root.style.colorScheme = isDark ? "dark" : "light";
  } catch (error) {
    document.documentElement.dataset.theme = "light";
  }
})();
`;

export default function RootLayout({ children }) {
  return (
    <html lang="id" suppressHydrationWarning className={jakarta.variable}>
      <head>
        <link
          rel="preload"
          href="/fonts/inter-latin-wght-normal.woff2"
          as="font"
          type="font/woff2"
          crossOrigin="anonymous"
        />
        <link
          rel="preload"
          href="/fonts/jetbrains-mono-latin-wght-normal.woff2"
          as="font"
          type="font/woff2"
          crossOrigin="anonymous"
        />
        <script
          id="koperasi-theme-init"
          dangerouslySetInnerHTML={{ __html: THEME_INIT_SCRIPT }}
        />
      </head>
      <body>
        <a href="#main-content" className="skip-link">
          Lewati ke konten utama
        </a>
        <AppProviders>{children}</AppProviders>
      </body>
    </html>
  );
}
