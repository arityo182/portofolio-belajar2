import ForgotPasswordForm from "./ForgotPasswordForm";

export const metadata = {
  title: "Lupa Kata Sandi",
  description: "Atur ulang kata sandi akun Koperasi Digital Anda dengan tautan aman.",
  robots: { index: false, follow: false },
};

export default function ForgotPasswordPage() {
  return <ForgotPasswordForm />;
}
