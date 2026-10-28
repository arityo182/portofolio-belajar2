import { PieChart } from "lucide-react";
import ModulePlaceholder from "@/components/dashboard/ModulePlaceholder";

export const metadata = {
  title: "Portofolio",
  description: "Portofolio pendanaan dan kinerja investasi Anda.",
};

export default function InvestmentsPage() {
  return (
    <ModulePlaceholder
      eyebrow="Investasi"
      title="Portofolio"
      description="Pantau alokasi pendanaan, estimasi imbal hasil, dan status pembayaran dari UMKM yang Anda danai."
      icon={PieChart}
      emptyTitle="Portofolio Anda masih kosong"
      emptyDescription="Pendanaan yang Anda lakukan akan tampil di sini lengkap dengan progres pembayaran dan estimasi imbal hasil."
    />
  );
}
