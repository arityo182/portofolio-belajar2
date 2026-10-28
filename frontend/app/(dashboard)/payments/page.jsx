import PaymentsView from "./PaymentsView";
import RoleGuard from "@/components/auth/RoleGuard";

export const metadata = {
  title: "Pembayaran",
  description: "Jadwal tagihan dan riwayat pembayaran cicilan pinjaman Anda.",
};

export default function PaymentsPage() {
  return (
    <RoleGuard roles={["BORROWER"]}>
      <PaymentsView />
    </RoleGuard>
  );
}
