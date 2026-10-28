"use client";

import Link from "next/link";
import {
  AlertTriangle,
  ArrowRight,
  Bell,
  CreditCard,
  Gauge,
  ShieldCheck,
  Sparkles,
  TrendingUp,
  UserRoundCheck,
  Wallet,
} from "lucide-react";
import PageHeader from "@/components/ui/PageHeader";
import StatsCard from "@/components/ui/StatsCard";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/Card";
import Badge from "@/components/ui/Badge";
import EmptyState from "@/components/ui/EmptyState";
import Avatar from "@/components/ui/Avatar";
import LoadingSkeleton from "@/components/ui/LoadingSkeleton";
import BorrowerDashboard from "@/components/dashboard/BorrowerDashboard";
import { buttonStyles } from "@/lib/buttonStyles";
import { useAuth } from "@/hooks/useAuth";
import { getAccountStatusLabel, getRoleLabel } from "@/lib/constants";
import { formatDate } from "@/lib/utils";

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

function primaryActionFor(roles = []) {
  if (roles.includes("ADMIN") || roles.includes("REVIEWER")) {
    return { href: "/reports", label: "Lihat laporan" };
  }
  if (roles.includes("INVESTOR")) {
    return { href: "/investments", label: "Lihat portofolio" };
  }
  return { href: "/loans", label: "Ajukan pinjaman" };
}

function DefaultDashboard() {
  const { user, isReady } = useAuth();

  if (!isReady) {
    return <LoadingSkeleton variant="page" />;
  }

  const roles = user?.roles || [];
  const primaryAction = primaryActionFor(roles);
  const name = displayNameFor(user);
  const accountStatus = user?.status || "ACTIVE";

  const nextSteps = [
    {
      title: "Lengkapi profil usaha",
      description: "Data identitas dan rekening diperlukan sebelum pencairan.",
      href: "/profile",
      icon: UserRoundCheck,
    },
    {
      title: roles.includes("INVESTOR") ? "Jelajahi peluang pendanaan" : "Ajukan pinjaman pertama",
      description: roles.includes("INVESTOR")
        ? "Tinjau UMKM yang terverifikasi dan mulai diversifikasi portofolio."
        : "Dapatkan keputusan berbasis AI dengan proses yang transparan.",
      href: roles.includes("INVESTOR") ? "/investments" : "/loans",
      icon: roles.includes("INVESTOR") ? TrendingUp : Wallet,
    },
    {
      title: "Atur preferensi notifikasi",
      description: "Pastikan pembaruan status pinjaman dan pembayaran tidak terlewat.",
      href: "/notifications",
      icon: Bell,
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Ringkasan akun"
        title={`Halo, ${name}`}
        description="Pantau aktivitas pinjaman, pembayaran, dan keamanan akun Anda dalam satu tampilan."
        actions={
          <Link href={primaryAction.href} className={buttonStyles({ size: "default" })}>
            {primaryAction.label}
            <ArrowRight className="h-4 w-4" aria-hidden="true" />
          </Link>
        }
      />

      {accountStatus !== "ACTIVE" ? (
        <div className="flex flex-col gap-3 rounded-2xl border border-warning-200 bg-warning-50 px-4 py-3.5 text-sm text-warning-700 dark:border-warning-700/40 dark:bg-warning-700/15 dark:text-warning-200 sm:flex-row sm:items-center sm:justify-between">
          <p className="flex items-start gap-2.5">
            <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
            <span>
              Status akun Anda <strong>{getAccountStatusLabel(accountStatus)}</strong>. Lengkapi
              verifikasi agar seluruh layanan dapat digunakan.
            </span>
          </p>
          <Link
            href="/profile"
            className="inline-flex shrink-0 items-center gap-1 rounded text-xs font-semibold underline-offset-2 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-warning-600"
          >
            Lengkapi profil
            <ArrowRight className="h-3.5 w-3.5" aria-hidden="true" />
          </Link>
        </div>
      ) : null}

      <section aria-labelledby="statistik-ringkasan">
        <h2 id="statistik-ringkasan" className="sr-only">
          Statistik ringkasan
        </h2>
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <StatsCard
            icon={Wallet}
            label="Pinjaman aktif"
            value="0"
            helper="Belum ada pinjaman aktif"
            tone="primary"
          />
          <StatsCard
            icon={CreditCard}
            label="Total pembayaran"
            value="Rp 0"
            helper="Belum ada pembayaran tercatat"
            tone="success"
          />
          <StatsCard
            icon={Gauge}
            label="Skor kredit"
            value="—"
            helper="Tersedia setelah pengajuan pertama"
            tone="info"
          />
          <StatsCard
            icon={Bell}
            label="Notifikasi baru"
            value="0"
            helper="Tidak ada notifikasi belum dibaca"
            tone="warning"
          />
        </div>
      </section>

      <div className="grid gap-6 xl:grid-cols-[1.5fr_1fr]">
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between gap-3">
              <div>
                <CardTitle>Langkah berikutnya</CardTitle>
                <CardDescription>Selesaikan langkah ini untuk memaksimalkan akun Anda.</CardDescription>
              </div>
              <Sparkles className="h-5 w-5 shrink-0 text-primary-600 dark:text-primary-400" aria-hidden="true" />
            </div>
          </CardHeader>
          <CardContent className="pt-3">
            <ul className="divide-y divide-line">
              {nextSteps.map((step, index) => {
                const Icon = step.icon;
                return (
                  <li key={step.title}>
                    <Link
                      href={step.href}
                      className="group flex items-center gap-3.5 rounded-xl px-2 py-3.5 transition-colors hover:bg-surface-subtle focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600"
                    >
                      <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-surface-subtle text-content-muted transition-colors group-hover:bg-primary-50 group-hover:text-primary-700 dark:group-hover:bg-primary-950/60 dark:group-hover:text-primary-300">
                        <Icon className="h-5 w-5" aria-hidden="true" />
                      </span>
                      <span className="min-w-0 flex-1">
                        <span className="block text-sm font-semibold text-content">{step.title}</span>
                        <span className="mt-0.5 block text-xs leading-5 text-content-muted">
                          {step.description}
                        </span>
                      </span>
                      <span className="flex items-center gap-2">
                        <span className="hidden rounded-full bg-surface-subtle px-2 py-0.5 text-2xs font-medium text-content-subtle sm:inline">
                          Langkah {index + 1}
                        </span>
                        <ArrowRight
                          className="h-4 w-4 text-content-subtle transition-transform group-hover:translate-x-0.5 group-hover:text-primary-600"
                          aria-hidden="true"
                        />
                      </span>
                    </Link>
                  </li>
                );
              })}
            </ul>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Keamanan akun</CardTitle>
            <CardDescription>Status sesi dan perlindungan akun Anda.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center gap-3">
              <Avatar email={user?.email} size="lg" />
              <div className="min-w-0">
                <p className="truncate text-sm font-semibold text-content">{user?.email || "—"}</p>
                <p className="mt-0.5 text-xs text-content-muted">
                  Bergabung {formatDate(user?.createdAt)}
                </p>
                <p className="mt-1.5">
                  <Badge variant={ACCOUNT_STATUS_VARIANT[accountStatus] || "neutral"} size="sm" dot>
                    {getAccountStatusLabel(accountStatus)}
                  </Badge>
                </p>
              </div>
            </div>

            <dl className="space-y-2.5 border-t border-line pt-4 text-sm">
              <div className="flex items-center justify-between gap-3">
                <dt className="text-content-muted">Peran</dt>
                <dd className="flex flex-wrap justify-end gap-1">
                  {roles.length > 0 ? (
                    roles.map((role) => (
                      <Badge key={role} variant="primary" size="sm">
                        {getRoleLabel(role)}
                      </Badge>
                    ))
                  ) : (
                    <span className="text-content-subtle">—</span>
                  )}
                </dd>
              </div>
              <div className="flex items-center justify-between gap-3">
                <dt className="text-content-muted">Perlindungan sesi</dt>
                <dd className="flex items-center gap-1.5 text-xs font-medium text-success-700 dark:text-success-300">
                  <ShieldCheck className="h-4 w-4" aria-hidden="true" />
                  Aktif
                </dd>
              </div>
              <div className="flex items-center justify-between gap-3">
                <dt className="text-content-muted">Pembaruan token</dt>
                <dd className="text-xs text-content">Otomatis</dd>
              </div>
            </dl>

            <Link
              href="/settings"
              className="inline-flex items-center gap-1.5 rounded text-xs font-semibold text-primary-700 underline-offset-2 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600 dark:text-primary-300"
            >
              Kelola pengaturan keamanan
              <ArrowRight className="h-3.5 w-3.5" aria-hidden="true" />
            </Link>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Aktivitas terbaru</CardTitle>
          <CardDescription>Transaksi dan pembaruan akun akan muncul di sini.</CardDescription>
        </CardHeader>
        <CardContent>
          <EmptyState
            compact
            icon={Bell}
            title="Belum ada aktivitas"
            description="Setelah Anda mengajukan pinjaman atau melakukan pembayaran, riwayatnya akan tampil di sini."
          />
        </CardContent>
      </Card>
    </div>
  );
}

export default function DashboardView() {
  const { user, isReady } = useAuth();

  if (!isReady) {
    return <LoadingSkeleton variant="page" />;
  }

  const isBorrower = (user?.roles || []).some(
    (role) => String(role).toUpperCase() === "BORROWER",
  );

  return isBorrower ? <BorrowerDashboard /> : <DefaultDashboard />;
}
