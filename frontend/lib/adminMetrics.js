import { localDateKey } from "@/lib/utils";

const ACTIVE_STATUSES = ["DISBURSED", "ACTIVE"];

function monthKey(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return null;
  }
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`;
}

function lastMonthKeys(count) {
  const now = new Date();
  const months = [];
  for (let index = count - 1; index >= 0; index -= 1) {
    const date = new Date(now.getFullYear(), now.getMonth() - index, 1);
    months.push({
      key: `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`,
      label: new Intl.DateTimeFormat("id-ID", { month: "short", year: "2-digit" }).format(date),
    });
  }
  return months;
}

export function buildAdminMetrics(loans = [], scoresByLoan = {}) {
  const activeLoans = loans.filter((loan) => ACTIVE_STATUSES.includes(loan.status));
  const defaultedLoans = loans.filter((loan) => loan.status === "DEFAULT");
  const activeAmount = activeLoans.reduce((sum, loan) => sum + Number(loan.amount || 0), 0);
  const defaultedAmount = defaultedLoans.reduce((sum, loan) => sum + Number(loan.amount || 0), 0);
  const today = localDateKey();
  const disbursedToday = loans.filter(
    (loan) => loan.disbursedAt && localDateKey(new Date(loan.disbursedAt)) === today,
  );

  const months = lastMonthKeys(6);
  const monthMap = new Map(
    months.map((month) => [
      month.key,
      { ...month, disbursed: 0, approved: 0, rejected: 0 },
    ]),
  );

  for (const loan of loans) {
    if (loan.disbursedAt) {
      const key = monthKey(loan.disbursedAt);
      if (monthMap.has(key)) {
        monthMap.get(key).disbursed += Number(loan.amount || 0);
      }
    }
    if (loan.reviewedAt) {
      const key = monthKey(loan.reviewedAt);
      if (monthMap.has(key)) {
        if (loan.status === "APPROVED" || ["DISBURSED", "ACTIVE", "COMPLETED"].includes(loan.status)) {
          monthMap.get(key).approved += 1;
        } else if (loan.status === "REJECTED") {
          monthMap.get(key).rejected += 1;
        }
      }
    }
  }

  const riskCounts = { LOW: 0, MEDIUM: 0, HIGH: 0, UNKNOWN: 0 };
  for (const loan of activeLoans.slice(0, 20)) {
    const score = scoresByLoan[loan.loanId];
    const category = score?.riskCategory;
    if (category && Object.prototype.hasOwnProperty.call(riskCounts, category)) {
      riskCounts[category] += 1;
    } else {
      riskCounts.UNKNOWN += 1;
    }
  }

  const nplDenominator = activeAmount + defaultedAmount;

  return {
    totalAum: activeAmount,
    activeLoanCount: activeLoans.length,
    nplRate: nplDenominator > 0 ? (defaultedAmount / nplDenominator) * 100 : 0,
    defaultedAmount,
    disbursedTodayAmount: disbursedToday.reduce((sum, loan) => sum + Number(loan.amount || 0), 0),
    disbursedTodayCount: disbursedToday.length,
    totalLoans: loans.length,
    monthlyDisbursement: Array.from(monthMap.values()),
    monthlyDecisions: Array.from(monthMap.values()),
    riskDistribution: [
      { name: "Risiko rendah", key: "LOW", value: riskCounts.LOW },
      { name: "Risiko sedang", key: "MEDIUM", value: riskCounts.MEDIUM },
      { name: "Risiko tinggi", key: "HIGH", value: riskCounts.HIGH },
      { name: "Belum dinilai", key: "UNKNOWN", value: riskCounts.UNKNOWN },
    ].filter((item) => item.value > 0),
  };
}
