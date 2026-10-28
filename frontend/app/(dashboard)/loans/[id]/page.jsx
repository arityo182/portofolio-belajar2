import LoanDetailView from "./LoanDetailView";

export const metadata = {
  title: "Detail Pinjaman",
  description: "Detail status, jadwal cicilan, dan riwayat pinjaman Anda.",
};

export default async function LoanDetailPage({ params, searchParams }) {
  const { id } = await params;
  const query = (await searchParams) || {};

  return <LoanDetailView loanId={id} submitted={query.submitted === "1"} />;
}
