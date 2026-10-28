import ApplyWizard from "./ApplyWizard";
import RoleGuard from "@/components/auth/RoleGuard";

export const metadata = {
  title: "Ajukan Pinjaman",
  description: "Ajukan pinjaman modal usaha dengan simulasi cicilan transparan.",
};

export default function LoanApplyPage() {
  return (
    <RoleGuard roles={["BORROWER"]}>
      <ApplyWizard />
    </RoleGuard>
  );
}
