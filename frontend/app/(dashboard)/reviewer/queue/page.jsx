import ReviewQueueView from "./ReviewQueueView";
import RoleGuard from "@/components/auth/RoleGuard";

export const metadata = {
  title: "Antrian Review",
  description: "Antrian pengajuan pinjaman yang menunggu keputusan reviewer.",
};

export default function ReviewQueuePage() {
  return (
    <RoleGuard roles={["REVIEWER", "ADMIN"]}>
      <ReviewQueueView />
    </RoleGuard>
  );
}
