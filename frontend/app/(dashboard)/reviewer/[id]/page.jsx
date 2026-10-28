import ReviewDetailView from "./ReviewDetailView";
import RoleGuard from "@/components/auth/RoleGuard";

export const metadata = {
  title: "Detail Review",
  description: "Detail pengajuan, skor AI, dan keputusan reviewer.",
};

export default async function ReviewDetailPage({ params }) {
  const { id } = await params;
  return (
    <RoleGuard roles={["REVIEWER", "ADMIN"]}>
      <ReviewDetailView loanId={id} />
    </RoleGuard>
  );
}
