"use client";

import { useState } from "react";
import Link from "next/link";
import {
  Check,
  Copy,
  LogOut,
  Mail,
  Phone,
  ShieldCheck,
  Smartphone,
  UserRound,
} from "lucide-react";
import PageHeader from "@/components/ui/PageHeader";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/Card";
import Badge from "@/components/ui/Badge";
import Avatar from "@/components/ui/Avatar";
import LoadingSkeleton from "@/components/ui/LoadingSkeleton";
import { Button } from "@/components/ui/Button";
import { buttonStyles } from "@/lib/buttonStyles";
import { useAuth } from "@/hooks/useAuth";
import { getAccountStatusLabel, getRoleLabel } from "@/lib/constants";
import { formatDate } from "@/lib/utils";
import BorrowerProfileForm from "./BorrowerProfileForm";

const ACCOUNT_STATUS_VARIANT = {
  ACTIVE: "success",
  PENDING_VERIFICATION: "warning",
  SUSPENDED: "danger",
  LOCKED: "danger",
};

function displayNameFor(user) {
  if (!user?.email) {
    return "Pengguna";
  }
  const raw = user.email.split("@")[0].replace(/[._-]+/g, " ");
  return raw.replace(/\b\w/g, (character) => character.toUpperCase());
}

export default function ProfileView() {
  const { user, isReady, logout } = useAuth();
  const [copied, setCopied] = useState(false);
  const [isLoggingOut, setIsLoggingOut] = useState(false);

  if (!isReady) {
    return <LoadingSkeleton variant="page" />;
  }

  if (!user) {
    return (
      <Card>
        <CardContent className="py-10 text-center">
          <p className="text-sm text-content-muted">Data profil tidak tersedia. Silakan masuk kembali.</p>
          <Link href="/login" className={buttonStyles({ className: "mt-4" })}>
            Ke halaman masuk
          </Link>
        </CardContent>
      </Card>
    );
  }

  const roles = user.roles || [];
  const accountStatus = user.status || "ACTIVE";
  const name = displayNameFor(user);

  async function copyUserId() {
    try {
      await navigator.clipboard.writeText(user.userId || "");
      setCopied(true);
      window.setTimeout(() => setCopied(false), 2000);
    } catch {
      setCopied(false);
    }
  }

  async function handleLogout() {
    setIsLoggingOut(true);
    await logout();
  }

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Akun"
        title="Profil saya"
        description="Informasi akun dan keamanan sesi Anda pada platform Koperasi Digital."
        actions={
          <Link href="/settings" className={buttonStyles({ variant: "secondary" })}>
            Pengaturan akun
          </Link>
        }
      />

      <Card>
        <CardContent className="flex flex-col gap-5 py-6 sm:flex-row sm:items-center">
          <Avatar email={user.email} size="lg" className="h-16 w-16 text-xl" />
          <div className="min-w-0 flex-1">
            <h2 className="truncate text-xl font-bold tracking-[-0.01em] text-content">{name}</h2>
            <p className="mt-0.5 truncate text-sm text-content-muted">{user.email}</p>
            <div className="mt-2.5 flex flex-wrap items-center gap-2">
              <Badge variant={ACCOUNT_STATUS_VARIANT[accountStatus] || "neutral"} dot>
                {getAccountStatusLabel(accountStatus)}
              </Badge>
              {roles.map((role) => (
                <Badge key={role} variant="primary">
                  {getRoleLabel(role)}
                </Badge>
              ))}
            </div>
          </div>
          <div className="rounded-xl border border-line bg-surface-subtle px-4 py-3 text-xs text-content-muted">
            <p className="font-medium text-content">Anggota sejak</p>
            <p className="mt-0.5">{formatDate(user.createdAt)}</p>
          </div>
        </CardContent>
      </Card>

      {roles.includes("BORROWER") ? <BorrowerProfileForm /> : null}

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Informasi akun</CardTitle>
            <CardDescription>Data yang terdaftar pada akun koperasi Anda.</CardDescription>
          </CardHeader>
          <CardContent>
            <dl className="divide-y divide-line">
              <div className="flex items-start gap-3 py-3 first:pt-0">
                <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-surface-subtle text-content-muted">
                  <Mail className="h-4 w-4" aria-hidden="true" />
                </span>
                <div className="min-w-0">
                  <dt className="text-xs text-content-muted">Email</dt>
                  <dd className="mt-0.5 break-all text-sm font-medium text-content">{user.email}</dd>
                </div>
              </div>

              <div className="flex items-start gap-3 py-3">
                <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-surface-subtle text-content-muted">
                  <Phone className="h-4 w-4" aria-hidden="true" />
                </span>
                <div className="min-w-0">
                  <dt className="text-xs text-content-muted">Nomor WhatsApp</dt>
                  <dd className="mt-0.5 text-sm font-medium text-content">{user.phone || "—"}</dd>
                </div>
              </div>

              <div className="flex items-start gap-3 py-3">
                <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-surface-subtle text-content-muted">
                  <UserRound className="h-4 w-4" aria-hidden="true" />
                </span>
                <div className="min-w-0 flex-1">
                  <dt className="text-xs text-content-muted">ID pengguna</dt>
                  <dd className="mt-0.5 flex items-center gap-2">
                    <span className="numeric truncate text-xs text-content">{user.userId}</span>
                    <button
                      type="button"
                      onClick={copyUserId}
                      className="inline-flex h-7 w-7 shrink-0 items-center justify-center rounded-lg text-content-muted transition-colors hover:bg-surface-subtle hover:text-content focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600"
                      aria-label={copied ? "ID pengguna tersalin" : "Salin ID pengguna"}
                    >
                      {copied ? (
                        <Check className="h-3.5 w-3.5 text-success-600" aria-hidden="true" />
                      ) : (
                        <Copy className="h-3.5 w-3.5" aria-hidden="true" />
                      )}
                    </button>
                  </dd>
                  <span className="sr-only" role="status" aria-live="polite">
                    {copied ? "ID pengguna berhasil disalin" : ""}
                  </span>
                </div>
              </div>
            </dl>
          </CardContent>
        </Card>

        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>Verifikasi akun</CardTitle>
              <CardDescription>Status verifikasi memengaruhi limit dan layanan yang tersedia.</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="rounded-xl border border-line bg-surface-subtle p-4">
                <div className="flex items-start gap-3">
                  <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-surface-raised text-primary-700 dark:text-primary-300">
                    <ShieldCheck className="h-5 w-5" aria-hidden="true" />
                  </span>
                  <div>
                    <p className="text-sm font-semibold text-content">
                      {accountStatus === "ACTIVE" ? "Akun terverifikasi" : "Verifikasi belum selesai"}
                    </p>
                    <p className="mt-1 text-xs leading-5 text-content-muted">
                      {accountStatus === "ACTIVE"
                        ? "Seluruh layanan pinjaman dan pendanaan dapat digunakan."
                        : "Lengkapi data identitas dan rekening untuk mengaktifkan seluruh layanan."}
                    </p>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Keamanan sesi</CardTitle>
              <CardDescription>Kontrol akses perangkat Anda saat ini.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center gap-3 rounded-xl border border-line px-4 py-3">
                <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-surface-subtle text-content-muted">
                  <Smartphone className="h-4 w-4" aria-hidden="true" />
                </span>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium text-content">Sesi perangkat ini</p>
                  <p className="mt-0.5 text-xs text-content-muted">
                    Token akses diperbarui otomatis setiap 15 menit.
                  </p>
                </div>
                <Badge variant="success" dot>
                  Aktif
                </Badge>
              </div>

              <div className="flex items-center gap-3 rounded-xl border border-line px-4 py-3">
                <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-surface-subtle text-content-muted">
                  <ShieldCheck className="h-4 w-4" aria-hidden="true" />
                </span>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium text-content">Refresh token</p>
                  <p className="mt-0.5 text-xs text-content-muted">
                    Berlaku 7 hari dan dirotasi setiap kali diperbarui.
                  </p>
                </div>
              </div>

              <Button
                variant="danger"
                fullWidth
                onClick={handleLogout}
                isLoading={isLoggingOut}
                loadingText="Keluar..."
                leftIcon={<LogOut className="h-4 w-4" />}
              >
                Keluar dari perangkat ini
              </Button>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
