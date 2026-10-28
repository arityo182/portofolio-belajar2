"use client";

import Link from "next/link";
import { ShieldAlert } from "lucide-react";
import { useAuth } from "@/hooks/useAuth";
import LoadingSkeleton from "@/components/ui/LoadingSkeleton";
import EmptyState from "@/components/ui/EmptyState";
import { buttonStyles } from "@/lib/buttonStyles";

export default function RoleGuard({ roles = [], children }) {
  const { user, isReady } = useAuth();

  if (!isReady) {
    return <LoadingSkeleton variant="page" />;
  }

  const userRoles = (user?.roles || []).map((role) => String(role).toUpperCase());
  const allowed = roles.some((role) => userRoles.includes(String(role).toUpperCase()));

  if (!allowed) {
    return (
      <EmptyState
        icon={ShieldAlert}
        title="Halaman ini khusus peminjam"
        description="Akun Anda tidak memiliki akses ke alur pengajuan pinjaman. Kembali ke dasbor untuk melanjutkan."
        action={
          <Link href="/dashboard" className={buttonStyles()}>
            Kembali ke dasbor
          </Link>
        }
      />
    );
  }

  return children;
}
