import AdminLoanProductsView from "./AdminLoanProductsView";
import RoleGuard from "@/components/auth/RoleGuard";

export const metadata = {
  title: "Produk Pinjaman",
  description: "Kelola produk pembiayaan dan ketentuan pinjaman.",
};

export default function AdminLoanProductsPage() {
  return (
    <RoleGuard roles={["ADMIN"]}>
      <AdminLoanProductsView />
    </RoleGuard>
  );
}
