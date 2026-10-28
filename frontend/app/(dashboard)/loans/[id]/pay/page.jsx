import PayInstallmentView from "./PayInstallmentView";
import RoleGuard from "@/components/auth/RoleGuard";

export const metadata = {
  title: "Bayar Cicilan",
  description: "Pilih cicilan dan selesaikan pembayaran melalui payment gateway.",
};

export default async function PayInstallmentPage({ params, searchParams }) {
  const { id } = await params;
  const query = (await searchParams) || {};
  const installmentId =
    typeof query.installment === "string" ? query.installment : null;

  return (
    <RoleGuard roles={["BORROWER"]}>
      <PayInstallmentView loanId={id} initialInstallmentId={installmentId} />
    </RoleGuard>
  );
}
