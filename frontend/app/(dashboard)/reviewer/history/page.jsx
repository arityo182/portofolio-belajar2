import ReviewHistoryView from "./ReviewHistoryView";
import RoleGuard from "@/components/auth/RoleGuard";

export const metadata = {
  title: "Riwayat Review",
  description: "Riwayat keputusan review pengajuan pinjaman.",
};

export default function ReviewHistoryPage() {
  return (
    <RoleGuard roles={["REVIEWER", "ADMIN"]}>
      <ReviewHistoryView />
    </RoleGuard>
  );
}
