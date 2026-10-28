import {
  Bell,
  ClipboardCheck,
  CreditCard,
  FileBarChart,
  FilePlus2,
  Gauge,
  History,
  LayoutDashboard,
  Package,
  PieChart,
  Settings,
  UserRound,
  Users,
  Wallet,
} from "lucide-react";

export const ALL_ROLES = ["BORROWER", "INVESTOR", "REVIEWER", "ADMIN"];

const NAVIGATION_SECTIONS = [
  {
    title: "Utama",
    items: [
      {
        label: "Dasbor",
        href: "/dashboard",
        icon: LayoutDashboard,
        roles: ["BORROWER", "INVESTOR", "REVIEWER"],
      },
      {
        label: "Pinjaman",
        href: "/loans",
        icon: Wallet,
        roles: ["BORROWER"],
        exclude: ["/loans/apply"],
      },
      { label: "Ajukan Pinjaman", href: "/loans/apply", icon: FilePlus2, roles: ["BORROWER"] },
      { label: "Portofolio", href: "/investments", icon: PieChart, roles: ["INVESTOR"] },
      { label: "Pembayaran", href: "/payments", icon: CreditCard, roles: ["BORROWER"] },
    ],
  },
  {
    title: "Review",
    items: [
      {
        label: "Antrian Review",
        href: "/reviewer/queue",
        icon: ClipboardCheck,
        roles: ["REVIEWER", "ADMIN"],
      },
      {
        label: "Riwayat Review",
        href: "/reviewer/history",
        icon: History,
        roles: ["REVIEWER", "ADMIN"],
      },
    ],
  },
  {
    title: "Administrasi",
    items: [
      {
        label: "Dashboard Admin",
        href: "/admin/dashboard",
        icon: Gauge,
        roles: ["ADMIN"],
      },
      { label: "Manajemen Pengguna", href: "/admin/users", icon: Users, roles: ["ADMIN"] },
      { label: "Semua Pinjaman", href: "/admin/loans", icon: Wallet, roles: ["ADMIN"] },
      {
        label: "Produk Pinjaman",
        href: "/admin/loan-products",
        icon: Package,
        roles: ["ADMIN"],
      },
      { label: "Laporan", href: "/admin/reports", icon: FileBarChart, roles: ["ADMIN"] },
    ],
  },
  {
    title: "Akun",
    items: [
      { label: "Notifikasi", href: "/notifications", icon: Bell, roles: ALL_ROLES },
      { label: "Profil", href: "/profile", icon: UserRound, roles: ALL_ROLES },
      { label: "Pengaturan", href: "/settings", icon: Settings, roles: ALL_ROLES },
    ],
  },
];

const PRIMARY_ITEMS = [
  {
    label: "Dasbor",
    href: "/dashboard",
    roles: ["BORROWER", "INVESTOR", "REVIEWER"],
  },
  { label: "Pinjaman", href: "/loans", roles: ["BORROWER"], exclude: ["/loans/apply"] },
  { label: "Pembayaran", href: "/payments", roles: ["BORROWER"] },
  {
    label: "Antrian Review",
    href: "/reviewer/queue",
    roles: ["REVIEWER", "ADMIN"],
  },
  {
    label: "Dashboard Admin",
    href: "/admin/dashboard",
    roles: ["ADMIN"],
  },
  { label: "Pengguna", href: "/admin/users", roles: ["ADMIN"] },
  { label: "Laporan", href: "/admin/reports", roles: ["ADMIN"] },
  { label: "Laporan", href: "/reports", roles: ["REVIEWER"] },
];

function normalizeRoles(roles = []) {
  if (!Array.isArray(roles) || roles.length === 0) {
    return [];
  }
  return roles.map((role) => String(role).toUpperCase());
}

function isAllowed(item, normalized) {
  return (
    item.roles === ALL_ROLES ||
    item.roles.some((role) => normalized.includes(role))
  );
}

export function getNavigationForRoles(roles = []) {
  const normalized = normalizeRoles(roles);
  return NAVIGATION_SECTIONS.map((section) => {
    const seen = new Set();
    const items = section.items.filter((item) => {
      const allowed = isAllowed(item, normalized);
      if (!allowed || seen.has(item.href)) {
        return false;
      }
      seen.add(item.href);
      return true;
    });
    return { ...section, items };
  }).filter((section) => section.items.length > 0);
}

export function getPrimaryNavigation(roles = []) {
  const normalized = normalizeRoles(roles);
  const seen = new Set();
  return PRIMARY_ITEMS.filter((item) => {
    const allowed = isAllowed(item, normalized);
    if (!allowed || seen.has(item.href)) {
      return false;
    }
    seen.add(item.href);
    return true;
  });
}

export function isActivePath(pathname, href, exclude = []) {
  if (!pathname || !href) {
    return false;
  }
  const excluded = exclude.some(
    (path) => pathname === path || pathname.startsWith(`${path}/`),
  );
  if (excluded) {
    return false;
  }
  return pathname === href || pathname.startsWith(`${href}/`);
}

export function getCurrentNavigationLabel(pathname) {
  if (!pathname) {
    return null;
  }
  const items = NAVIGATION_SECTIONS.flatMap((section) => section.items);
  const match = items
    .filter((item) => item.href !== "/dashboard" && isActivePath(pathname, item.href, item.exclude))
    .sort((left, right) => right.href.length - left.href.length)[0];
  if (match) {
    return match.label;
  }
  if (pathname === "/dashboard" || pathname.startsWith("/dashboard/")) {
    return "Dasbor";
  }
  return null;
}
