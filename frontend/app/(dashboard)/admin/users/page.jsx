import AdminUsersView from "./AdminUsersView";
import RoleGuard from "@/components/auth/RoleGuard";

export const metadata = {
  title: "Manajemen Pengguna",
  description: "Kelola akun, peran, dan status pengguna platform.",
};

export default function AdminUsersPage() {
  return (
    <RoleGuard roles={["ADMIN"]}>
      <AdminUsersView />
    </RoleGuard>
  );
}
