import AdminDashboardView from "./AdminDashboardView";
import RoleGuard from "@/components/auth/RoleGuard";

export const metadata = {
  title: "Dashboard Admin",
  description: "Metrik portofolio dan kinerja pinjaman koperasi.",
};

export default function AdminDashboardPage() {
  return (
    <RoleGuard roles={["ADMIN"]}>
      <AdminDashboardView />
    </RoleGuard>
  );
}
