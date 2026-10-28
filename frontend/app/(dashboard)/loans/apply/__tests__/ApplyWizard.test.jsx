import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import ApplyWizard from "@/app/(dashboard)/loans/apply/ApplyWizard";
import { useLoanStore } from "@/stores/loanStore";

const push = jest.fn();
const product = {
  id: 1,
  name: "Modal Usaha Mikro",
  minAmount: 1000000,
  maxAmount: 50000000,
  interestRateAnnual: 18,
  minTenorMonths: 3,
  maxTenorMonths: 24,
  isActive: true,
};
const mutateAsync = jest.fn().mockResolvedValue({ loanId: "loan-1" });

jest.mock("next/navigation", () => ({
  useRouter: () => ({ replace: push }),
}));

jest.mock("@/hooks/useLoanApplication", () => ({
  useLoanProducts: () => ({
    data: [product],
    isLoading: false,
    isError: false,
    refetch: jest.fn(),
  }),
  useCreateLoanApplication: () => ({ mutateAsync, isPending: false }),
}));

describe("ApplyWizard", () => {
  beforeEach(() => {
    useLoanStore.getState().resetDraft();
    mutateAsync.mockClear();
    push.mockClear();
  });

  it("navigates steps and validates the details step", async () => {
    const user = userEvent.setup();
    render(<ApplyWizard />);

    await user.click(screen.getByRole("button", { name: /Modal Usaha Mikro/ }));
    await user.click(screen.getByRole("button", { name: "Lanjutkan" }));

    expect(await screen.findByText("Atur nominal dan tenor")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Lanjutkan" }));
    expect(await screen.findByText(/Nominal minimal/)).toBeInTheDocument();

    await user.type(screen.getByLabelText(/Nominal pinjaman/), "12000000");
    await user.selectOptions(screen.getByLabelText(/Tenor/), "12");
    await user.type(
      screen.getByLabelText(/Tujuan pinjaman/),
      "Pembelian bahan baku dan peralatan warung",
    );
    expect(screen.getByText(/Rp\s*1\.180\.000/)).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Lanjutkan" }));
    expect(await screen.findByText("Unggah dokumen pendukung")).toBeInTheDocument();
  });
});
