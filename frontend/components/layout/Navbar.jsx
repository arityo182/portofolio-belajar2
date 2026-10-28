"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  Bell,
  ChevronDown,
  ChevronRight,
  LogOut,
  Menu,
  Settings,
  UserRound,
} from "lucide-react";
import Logo from "@/components/ui/Logo";
import Avatar from "@/components/ui/Avatar";
import ThemeToggle from "@/components/layout/ThemeToggle";
import { useAuth } from "@/hooks/useAuth";
import { useUnreadNotificationCount } from "@/hooks/useNotificationList";
import { getCurrentNavigationLabel } from "@/lib/navigation";
import { getRoleLabel } from "@/lib/constants";
import { cn } from "@/lib/utils";

export default function Navbar({ onMenuClick }) {
  const pathname = usePathname();
  const { user, logout, isAuthenticated } = useAuth();
  const unreadQuery = useUnreadNotificationCount({ enabled: isAuthenticated });
  const unreadCount = Number(unreadQuery.data?.unreadCount || 0);
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const menuRef = useRef(null);
  const triggerRef = useRef(null);
  const firstItemRef = useRef(null);

  const navigationLabel = getCurrentNavigationLabel(pathname);
  const displayName = user?.email?.split("@")[0] || "Pengguna";
  const primaryRole = user?.roles?.[0];

  useEffect(() => {
    setIsMenuOpen(false);
  }, [pathname]);

  useEffect(() => {
    if (!isMenuOpen) {
      return undefined;
    }

    firstItemRef.current?.focus();

    function handlePointerDown(event) {
      if (
        menuRef.current &&
        !menuRef.current.contains(event.target) &&
        triggerRef.current &&
        !triggerRef.current.contains(event.target)
      ) {
        setIsMenuOpen(false);
      }
    }

    function handleKeyDown(event) {
      if (event.key === "Escape") {
        setIsMenuOpen(false);
        triggerRef.current?.focus();
      }
    }

    document.addEventListener("mousedown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("mousedown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [isMenuOpen]);

  async function handleLogout() {
    setIsLoggingOut(true);
    setIsMenuOpen(false);
    await logout();
  }

  return (
    <header className="sticky top-0 z-40 border-b border-line bg-surface-raised backdrop-blur">
      <div className="mx-auto flex h-16 w-full max-w-[1600px] items-center gap-2 px-4 sm:px-6 lg:px-8">
        <button
          type="button"
          onClick={onMenuClick}
          className="inline-flex h-10 w-10 items-center justify-center rounded-xl text-content-muted transition-colors hover:bg-surface-subtle hover:text-content focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600 focus-visible:ring-offset-2 focus-visible:ring-offset-surface lg:hidden"
          aria-label="Buka menu navigasi"
          aria-controls="mobile-sidebar"
        >
          <Menu className="h-5 w-5" aria-hidden="true" />
        </button>

        <Logo
          href="/dashboard"
          size="sm"
          showText={false}
          className="mx-1 shrink-0 sm:hidden"
        />
        <Logo href="/dashboard" size="sm" className="mr-2 hidden shrink-0 sm:inline-flex" />

        <nav aria-label="Breadcrumb" className="ml-4 hidden min-w-0 items-center gap-2 text-sm lg:flex">
          <span className="text-content-subtle">Ruang kerja</span>
          <ChevronRight className="h-3.5 w-3.5 shrink-0 text-content-subtle" aria-hidden="true" />
          <span className="truncate font-semibold text-content">
            {navigationLabel || "Koperasi Digital"}
          </span>
        </nav>

        <div className="ml-auto flex items-center gap-1.5 sm:gap-2">
          <Link
            href="/notifications"
            className="relative inline-flex h-10 w-10 items-center justify-center rounded-xl border border-line bg-surface-raised text-content-muted transition-colors hover:bg-surface-subtle hover:text-content focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600 focus-visible:ring-offset-2 focus-visible:ring-offset-surface"
            aria-label={
              unreadCount > 0
                ? `Lihat notifikasi, ${unreadCount} belum dibaca`
                : "Lihat notifikasi"
            }
          >
            <Bell className="h-[18px] w-[18px]" aria-hidden="true" />
            {unreadCount > 0 ? (
              <span
                className="absolute -right-1 -top-1 inline-flex h-5 min-w-5 items-center justify-center rounded-full bg-danger-600 px-1 text-2xs font-bold text-white ring-2 ring-surface-raised"
                aria-hidden="true"
              >
                {unreadCount > 9 ? "9+" : unreadCount}
              </span>
            ) : null}
          </Link>

          <ThemeToggle />

          <div className="relative">
            <button
              ref={triggerRef}
              type="button"
              onClick={() => setIsMenuOpen((open) => !open)}
              className="flex h-10 items-center gap-2 rounded-xl border border-line bg-surface-raised px-1.5 pr-2.5 text-left transition-colors hover:bg-surface-subtle focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600 focus-visible:ring-offset-2 focus-visible:ring-offset-surface"
              aria-expanded={isMenuOpen}
              aria-haspopup="menu"
              aria-controls="user-menu"
              aria-label="Buka menu akun"
            >
              <Avatar email={user?.email} size="sm" />
              <span className="hidden max-w-[9rem] truncate text-sm font-medium text-content sm:block">
                {displayName}
              </span>
              <ChevronDown
                className={cn(
                  "hidden h-4 w-4 text-content-subtle transition-transform sm:block",
                  isMenuOpen && "rotate-180",
                )}
                aria-hidden="true"
              />
            </button>

            {isMenuOpen ? (
              <div
                ref={menuRef}
                id="user-menu"
                role="menu"
                aria-label="Menu akun"
                className="absolute right-0 mt-2 w-64 origin-top-right rounded-2xl border border-line bg-surface-raised p-1.5 shadow-dropdown animate-fade-in"
              >
                <div className="flex items-center gap-3 rounded-xl px-3 py-3">
                  <Avatar email={user?.email} size="md" />
                  <div className="min-w-0">
                    <p className="truncate text-sm font-semibold text-content">{displayName}</p>
                    <p className="truncate text-xs text-content-muted">{user?.email || "Sesi aktif"}</p>
                    {primaryRole ? (
                      <p className="mt-1 inline-flex rounded-full bg-primary-50 px-2 py-0.5 text-2xs font-semibold uppercase tracking-wide text-primary-700 dark:bg-primary-950/60 dark:text-primary-300">
                        {getRoleLabel(primaryRole)}
                      </p>
                    ) : null}
                  </div>
                </div>

                <div className="my-1 h-px bg-line" role="separator" />

                <Link
                  ref={firstItemRef}
                  href="/profile"
                  role="menuitem"
                  className="flex items-center gap-2.5 rounded-lg px-3 py-2.5 text-sm text-content-muted transition-colors hover:bg-surface-subtle hover:text-content focus-visible:bg-surface-subtle focus-visible:outline-none"
                >
                  <UserRound className="h-4 w-4" aria-hidden="true" />
                  Profil saya
                </Link>
                <Link
                  href="/settings"
                  role="menuitem"
                  className="flex items-center gap-2.5 rounded-lg px-3 py-2.5 text-sm text-content-muted transition-colors hover:bg-surface-subtle hover:text-content focus-visible:bg-surface-subtle focus-visible:outline-none"
                >
                  <Settings className="h-4 w-4" aria-hidden="true" />
                  Pengaturan
                </Link>

                <div className="my-1 h-px bg-line" role="separator" />

                <button
                  type="button"
                  role="menuitem"
                  onClick={handleLogout}
                  disabled={isLoggingOut}
                  className="flex w-full items-center gap-2.5 rounded-lg px-3 py-2.5 text-left text-sm text-danger-600 transition-colors hover:bg-danger-50 focus-visible:bg-danger-50 focus-visible:outline-none disabled:opacity-60 dark:text-danger-300 dark:hover:bg-danger-700/15 dark:focus-visible:bg-danger-700/15"
                >
                  <LogOut className="h-4 w-4" aria-hidden="true" />
                  {isLoggingOut ? "Keluar..." : "Keluar"}
                </button>
              </div>
            ) : null}
          </div>
        </div>
      </div>
    </header>
  );
}
