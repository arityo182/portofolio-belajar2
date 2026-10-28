import DashboardShell from "@/components/layout/DashboardShell";

export const metadata = {
  title: {
    default: "Dasbor",
    template: "%s | Koperasi Digital",
  },
  robots: { index: false, follow: false },
};

export default function DashboardLayout({ children }) {
  return <DashboardShell>{children}</DashboardShell>;
}
