import AdminReportsView from "./AdminReportsView";
import RoleGuard from "@/components/auth/RoleGuard";

export const metadata = {
  title: "Laporan Admin",
  description: "Buat dan unduh laporan pinjaman koperasi.",
};

export default function AdminReportsPage() {
  return (
    <RoleGuard roles={["ADMIN"]}>
      <AdminReportsView />
    </RoleGuard>
  );
}
