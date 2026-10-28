export const INSTALLMENT_METHOD =
  (process.env.NEXT_PUBLIC_LOAN_INSTALLMENT_METHOD || "FLAT").toUpperCase() === "ANNUITY"
    ? "ANNUITY"
    : "FLAT";

function roundMoney(value) {
  return Math.round((Number(value) + Number.EPSILON) * 100) / 100;
}

function toNumber(value) {
  const number = Number(value);
  return Number.isFinite(number) ? number : 0;
}

export function calculateFlatSimulation({ amount, annualRatePercent, tenorMonths }) {
  const principal = toNumber(amount);
  const rate = toNumber(annualRatePercent);
  const months = Math.max(1, Math.round(toNumber(tenorMonths)));
  if (principal <= 0) {
    return {
      method: "FLAT",
      monthlyInstallment: 0,
      monthlyPrincipal: 0,
      monthlyInterest: 0,
      totalInterest: 0,
      totalPayment: 0,
      months,
    };
  }

  const monthlyPrincipal = roundMoney(principal / months);
  const monthlyInterest = roundMoney((principal * rate) / 100 / 12);
  const totalInterest = roundMoney(monthlyInterest * months);
  const totalPayment = roundMoney(principal + totalInterest);

  return {
    method: "FLAT",
    monthlyInstallment: roundMoney(monthlyPrincipal + monthlyInterest),
    monthlyPrincipal,
    monthlyInterest,
    totalInterest,
    totalPayment,
    months,
  };
}

export function calculateAnnuitySimulation({ amount, annualRatePercent, tenorMonths }) {
  const principal = toNumber(amount);
  const rate = toNumber(annualRatePercent);
  const months = Math.max(1, Math.round(toNumber(tenorMonths)));
  if (principal <= 0) {
    return {
      method: "ANNUITY",
      monthlyInstallment: 0,
      monthlyPrincipal: 0,
      monthlyInterest: 0,
      totalInterest: 0,
      totalPayment: 0,
      months,
    };
  }

  const monthlyRate = rate / 100 / 12;
  if (monthlyRate === 0) {
    return calculateFlatSimulation({ amount: principal, annualRatePercent: 0, tenorMonths: months });
  }

  const factor = Math.pow(1 + monthlyRate, months);
  const monthlyInstallment = roundMoney((principal * monthlyRate * factor) / (factor - 1));
  const totalPayment = roundMoney(monthlyInstallment * months);
  const totalInterest = roundMoney(totalPayment - principal);

  return {
    method: "ANNUITY",
    monthlyInstallment,
    monthlyPrincipal: roundMoney(principal / months),
    monthlyInterest: roundMoney(monthlyInstallment - principal / months),
    totalInterest,
    totalPayment,
    months,
  };
}

export function calculateSimulation(input) {
  if (INSTALLMENT_METHOD === "ANNUITY") {
    return calculateAnnuitySimulation(input);
  }
  return calculateFlatSimulation(input);
}

export function buildTenorOptions(minTenor, maxTenor) {
  const min = Math.max(1, Math.round(toNumber(minTenor)));
  const max = Math.max(min, Math.round(toNumber(maxTenor)));
  const preferred = [3, 6, 9, 12, 18, 24, 36, 48, 60];
  const values = new Set([min, max]);

  for (const tenor of preferred) {
    if (tenor >= min && tenor <= max) {
      values.add(tenor);
    }
  }

  if (max - min <= 12) {
    for (let tenor = min; tenor <= max; tenor += 1) {
      values.add(tenor);
    }
  }

  return Array.from(values).sort((left, right) => left - right);
}

export function getOutstandingAmount(installments = []) {
  return roundMoney(
    installments
      .filter((installment) => installment.status !== "PAID")
      .reduce((total, installment) => total + toNumber(installment.amount), 0),
  );
}
