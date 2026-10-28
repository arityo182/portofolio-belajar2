"use client";

import { useState } from "react";
import Link from "next/link";
import { Check, LogOut, Monitor, Moon, ShieldCheck, Sun } from "lucide-react";
import PageHeader from "@/components/ui/PageHeader";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { buttonStyles } from "@/lib/buttonStyles";
import { useTheme } from "@/components/providers/ThemeProvider";
import { useAuth } from "@/hooks/useAuth";
import { cn, formatDate } from "@/lib/utils";

function ThemeOption({ mode, title, description, icon: Icon, active, onSelect }) {
  return (
    <button
      type="button"
      onClick={() => onSelect(mode)}
      aria-pressed={active}
      className={cn(
        "group flex w-full flex-col gap-3 rounded-xl border p-4 text-left transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600 focus-visible:ring-offset-2 focus-visible:ring-offset-surface",
        active
          ? "border-primary-600 bg-primary-50/60 dark:border-primary-400 dark:bg-primary-950/40"
          : "border-line bg-surface-raised hover:bg-surface-subtle",
      )}
    >
      <div className="flex items-center justify-between gap-3">
        <span className="flex items-center gap-2 text-sm font-semibold text-content">
          <Icon className="h-4 w-4 text-content-muted" aria-hidden="true" />
          {title}
        </span>
        {active ? (
          <span className="flex h-5 w-5 items-center justify-center rounded-full bg-primary-600 text-white">
            <Check className="h-3 w-3" aria-hidden="true" />
          </span>
        ) : null}
      </div>

      <div
        className={cn(
          "flex h-16 items-end gap-1.5 overflow-hidden rounded-lg border p-2",
          mode === "dark"
            ? "border-neutral-700 bg-neutral-900"
            : "border-neutral-200 bg-neutral-50",
        )}
        aria-hidden="true"
      >
        <span className={cn("h-full flex-1 rounded", mode === "dark" ? "bg-neutral-800" : "bg-neutral-200")} />
        <span className="h-full w-1/2 rounded bg-primary-600" />
      </div>

      <span className="text-xs leading-5 text-content-muted">{description}</span>
    </button>
  );
}

export default function SettingsView() {
  const { theme, mounted, setTheme } = useTheme();
  const { user, logout } = useAuth();
  const [isLoggingOut, setIsLoggingOut] = useState(false);

  async function handleLogout() {
    setIsLoggingOut(true);
    await logout();
  }

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Preferensi"
        title="Pengaturan"
        description="Sesuaikan tampilan dan kelola keamanan akun Anda."
        actions={
          <Link href="/profile" className={buttonStyles({ variant: "secondary" })}>
            Lihat profil
          </Link>
        }
      />

      <Card>
        <CardHeader>
          <CardTitle>Tampilan</CardTitle>
          <CardDescription>
            Pilih tema yang nyaman untuk mata Anda. Preferensi tersimpan di perangkat ini.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid gap-3 sm:grid-cols-2">
            <ThemeOption
              mode="light"
              title="Mode terang"
              description="Latar putih bersih dengan kontras tinggi untuk aktivitas siang hari."
              icon={Sun}
              active={mounted && theme === "light"}
              onSelect={setTheme}
            />
            <ThemeOption
              mode="dark"
              title="Mode gelap"
              description="Latar gelap yang nyaman untuk penggunaan malam hari."
              icon={Moon}
              active={mounted && theme === "dark"}
              onSelect={setTheme}
            />
          </div>

          <p className="mt-3 flex items-start gap-2 text-xs leading-5 text-content-subtle">
            <Monitor className="mt-0.5 h-3.5 w-3.5 shrink-0" aria-hidden="true" />
            Jika belum pernah memilih, tampilan mengikuti preferensi sistem operasi Anda. Perubahan
            diterapkan segera tanpa memuat ulang halaman.
          </p>
        </CardContent>
      </Card>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Keamanan</CardTitle>
            <CardDescription>Ringkasan perlindungan akun dan sesi Anda.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="flex items-start gap-3 rounded-xl border border-line px-4 py-3">
              <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-surface-subtle text-content-muted">
                <ShieldCheck className="h-4 w-4" aria-hidden="true" />
              </span>
              <div>
                <p className="text-sm font-medium text-content">Pembaruan token otomatis</p>
                <p className="mt-0.5 text-xs leading-5 text-content-muted">
                  Token akses berlaku 15 menit dan diperbarui otomatis saat kedaluwarsa. Refresh token
                  berlaku 7 hari.
                </p>
              </div>
            </div>

            <dl className="rounded-xl border border-line px-4 py-3 text-xs">
              <div className="flex items-center justify-between gap-3 py-1">
                <dt className="text-content-muted">Email</dt>
                <dd className="truncate font-medium text-content">{user?.email || "—"}</dd>
              </div>
              <div className="flex items-center justify-between gap-3 py-1">
                <dt className="text-content-muted">Anggota sejak</dt>
                <dd className="text-content">{formatDate(user?.createdAt)}</dd>
              </div>
            </dl>

            <Button
              variant="danger"
              fullWidth
              onClick={handleLogout}
              isLoading={isLoggingOut}
              loadingText="Keluar..."
              leftIcon={<LogOut className="h-4 w-4" />}
            >
              Keluar dari akun
            </Button>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Aksesibilitas</CardTitle>
            <CardDescription>Platform dirancang mengikuti pedoman WCAG 2.1 AA.</CardDescription>
          </CardHeader>
          <CardContent>
            <ul className="space-y-3 text-sm">
              <li className="rounded-xl border border-line px-4 py-3">
                <p className="font-medium text-content">Navigasi keyboard</p>
                <p className="mt-0.5 text-xs leading-5 text-content-muted">
                  Gunakan Tab untuk berpindah elemen dan Enter atau Spasi untuk mengaktifkannya.
                  Tautan “Lewati ke konten utama” tersedia di awal halaman.
                </p>
              </li>
              <li className="rounded-xl border border-line px-4 py-3">
                <p className="font-medium text-content">Kontras warna</p>
                <p className="mt-0.5 text-xs leading-5 text-content-muted">
                  Teks dan komponen interaktif memenuhi rasio kontras minimal 4,5:1 pada mode terang
                  maupun gelap.
                </p>
              </li>
              <li className="rounded-xl border border-line px-4 py-3">
                <p className="font-medium text-content">Gerak minimal</p>
                <p className="mt-0.5 text-xs leading-5 text-content-muted">
                  Animasi dinonaktifkan secara otomatis ketika perangkat mengaktifkan pengurangan
                  gerak.
                </p>
              </li>
            </ul>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
