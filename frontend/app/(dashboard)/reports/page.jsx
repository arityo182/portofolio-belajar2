import RoleGuard from "@/components/auth/RoleGuard";
import ReportsView from "./ReportsView";

export const metadata = {
  title: "Laporan",
  description: "Laporan kinerja pinjaman dan portofolio koperasi.",
};

export default function ReportsPage() {
  return (
    <RoleGuard roles={["REVIEWER"]}>
      <ReportsView />
    </RoleGuard>
  );
}
