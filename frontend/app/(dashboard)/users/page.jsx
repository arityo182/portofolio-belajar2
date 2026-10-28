import { redirect } from "next/navigation";

export const metadata = {
  title: "Manajemen Pengguna",
  description: "Kelola akun, peran, dan status pengguna platform.",
};

export default function UsersPage() {
  redirect("/admin/users");
}
