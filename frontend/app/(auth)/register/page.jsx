import RegisterForm from "./RegisterForm";

export const metadata = {
  title: "Daftar",
  description:
    "Buat akun Koperasi Digital sebagai peminjam atau investor dan mulai kelola modal usaha UMKM.",
  robots: { index: false, follow: false },
};

export default function RegisterPage() {
  return <RegisterForm />;
}
