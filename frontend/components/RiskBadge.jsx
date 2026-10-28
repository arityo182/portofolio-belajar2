import Badge from "@/components/ui/Badge";

const RISK = {
  LOW: { label: "Risiko rendah", variant: "success" },
  MEDIUM: { label: "Risiko sedang", variant: "warning" },
  HIGH: { label: "Risiko tinggi", variant: "danger" },
};

export default function RiskBadge({ riskCategory, showDot = true, size = "sm" }) {
  const config = RISK[riskCategory] || { label: "Belum dinilai", variant: "neutral" };
  return (
    <Badge variant={config.variant} dot={showDot} size={size}>
      {config.label}
    </Badge>
  );
}
