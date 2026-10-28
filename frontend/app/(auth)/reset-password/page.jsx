import ResetPasswordForm from "./ResetPasswordForm";

export const metadata = {
  title: "Atur Ulang Kata Sandi",
  description: "Buat kata sandi baru untuk akun Koperasi Digital Anda.",
  robots: { index: false, follow: false },
};

export default async function ResetPasswordPage({ searchParams }) {
  const query = (await searchParams) || {};
  const token = typeof query.token === "string" ? query.token : "";

  return <ResetPasswordForm token={token} />;
}
