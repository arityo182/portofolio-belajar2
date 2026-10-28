import AdminLoansView from "./AdminLoansView";
import RoleGuard from "@/components/auth/RoleGuard";

export const metadata = {
  title: "Semua Pinjaman",
  description: "Daftar seluruh pengajuan dan pinjaman di platform.",
};

export default function AdminLoansPage() {
  return (
    <RoleGuard roles={["ADMIN"]}>
      <AdminLoansView />
    </RoleGuard>
  );
}
