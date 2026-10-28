import { render, screen } from "@testing-library/react";
import { Wallet } from "lucide-react";
import StatsCard from "@/components/ui/StatsCard";

describe("StatsCard", () => {
  it("renders label, value, helper, and trend", () => {
    render(
      <StatsCard
        icon={Wallet}
        label="Pinjaman aktif"
        value="12"
        helper="Sisa kewajiban Rp 14.750.000"
        trend={{ direction: "up", value: "+2", label: "bulan ini" }}
        tone="primary"
      />,
    );

    expect(screen.getByText("Pinjaman aktif")).toBeInTheDocument();
    expect(screen.getByText("12")).toBeInTheDocument();
    expect(screen.getByText("+2")).toBeInTheDocument();
    expect(screen.getByText("bulan ini")).toBeInTheDocument();
    expect(screen.getByText("Sisa kewajiban Rp 14.750.000")).toBeInTheDocument();
  });
});
