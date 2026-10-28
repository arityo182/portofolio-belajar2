import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import LoginForm from "@/app/(auth)/login/LoginForm";

const replace = jest.fn();
const login = jest.fn();

jest.mock("next/navigation", () => ({
  useRouter: () => ({ replace }),
}));

jest.mock("@/hooks/useAuth", () => ({
  useAuth: () => ({ login }),
}));

describe("LoginForm", () => {
  beforeEach(() => {
    replace.mockReset();
    login.mockReset();
    login.mockResolvedValue({ user: { email: "budi@example.id" } });
  });

  it("shows validation errors for empty credentials", async () => {
    const user = userEvent.setup();
    render(<LoginForm />);

    await user.click(screen.getByRole("button", { name: "Masuk" }));

    expect(await screen.findByText("Email wajib diisi")).toBeInTheDocument();
    expect(screen.getByText("Kata sandi wajib diisi")).toBeInTheDocument();
    expect(login).not.toHaveBeenCalled();
  });

  it("submits valid credentials and redirects to dashboard", async () => {
    const user = userEvent.setup();
    render(<LoginForm />);

    await user.type(screen.getByLabelText(/Email/), "budi@example.id");
    await user.type(screen.getByLabelText(/Kata sandi/), "Koperasi#2026Aman");
    await user.click(screen.getByRole("button", { name: "Masuk" }));

    await waitFor(() => expect(login).toHaveBeenCalledWith({
      email: "budi@example.id",
      password: "Koperasi#2026Aman",
    }));
    await waitFor(() => expect(replace).toHaveBeenCalledWith("/dashboard"));
  });

  it("shows API error message on failed login", async () => {
    const user = userEvent.setup();
    login.mockRejectedValueOnce({
      response: { data: { message: "Email atau kata sandi salah" } },
    });
    render(<LoginForm />);

    await user.type(screen.getByLabelText(/Email/), "budi@example.id");
    await user.type(screen.getByLabelText(/Kata sandi/), "Salah#2026Aman");
    await user.click(screen.getByRole("button", { name: "Masuk" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Email atau kata sandi salah");
  });
});
