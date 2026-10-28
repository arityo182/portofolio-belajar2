"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { LifeBuoy, PanelLeftClose, PanelLeftOpen, X } from "lucide-react";
import Logo from "@/components/ui/Logo";
import { useAuth } from "@/hooks/useAuth";
import { getNavigationForRoles, isActivePath } from "@/lib/navigation";
import { cn } from "@/lib/utils";

function SidebarLink({ item, collapsed, onNavigate }) {
  const pathname = usePathname();
  const active = isActivePath(pathname, item.href, item.exclude);
  const Icon = item.icon;

  return (
    <li>
      <Link
        href={item.href}
        onClick={onNavigate}
        aria-current={active ? "page" : undefined}
        title={collapsed ? item.label : undefined}
        className={cn(
          "group relative flex items-center gap-3 rounded-xl py-2.5 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600 focus-visible:ring-offset-2 focus-visible:ring-offset-surface",
          collapsed ? "justify-center px-0" : "px-3",
          active
            ? "bg-primary-50 text-primary-700 dark:bg-primary-950/60 dark:text-primary-300"
            : "text-content-muted hover:bg-surface-subtle hover:text-content",
        )}
      >
        {active ? (
          <span
            className="absolute inset-y-2 left-0 w-1 rounded-full bg-primary-600 dark:bg-primary-400"
            aria-hidden="true"
          />
        ) : null}
        <Icon className="h-5 w-5 shrink-0" aria-hidden="true" />
        <span className={cn("truncate", collapsed && "sr-only")}>{item.label}</span>
      </Link>
    </li>
  );
}

function SidebarContent({ collapsed, onToggleCollapsed, onNavigate, onClose }) {
  const { user } = useAuth();
  const sections = getNavigationForRoles(user?.roles || []);

  return (
    <div className="flex h-full min-h-0 flex-col">
      <div className="flex h-16 items-center justify-between gap-2 border-b border-line px-4 lg:hidden">
        <Logo href="/dashboard" size="sm" />
        <button
          type="button"
          onClick={onClose}
          className="inline-flex h-9 w-9 items-center justify-center rounded-lg text-content-muted hover:bg-surface-subtle hover:text-content focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600"
          aria-label="Tutup menu navigasi"
        >
          <X className="h-5 w-5" aria-hidden="true" />
        </button>
      </div>

      <nav
        aria-label="Navigasi samping"
        className="flex-1 overflow-y-auto overflow-x-hidden px-3 py-5"
      >
        {sections.map((section, sectionIndex) => (
          <div key={section.title} className={sectionIndex > 0 ? "mt-6" : undefined}>
            {collapsed ? (
              sectionIndex > 0 ? <div className="mx-2 mb-3 h-px bg-line" aria-hidden="true" /> : null
            ) : (
              <p className="mb-2 px-3 text-2xs font-semibold uppercase tracking-[0.16em] text-content-subtle">
                {section.title}
              </p>
            )}
            <ul className="space-y-1">
              {section.items.map((item) => (
                <SidebarLink
                  key={`${section.title}-${item.href}`}
                  item={item}
                  collapsed={collapsed}
                  onNavigate={onNavigate}
                />
              ))}
            </ul>
          </div>
        ))}
      </nav>

      <div className="border-t border-line p-3">
        {!collapsed ? (
          <div className="mb-3 rounded-xl border border-line bg-surface-subtle p-3">
            <div className="flex items-start gap-2.5">
              <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-primary-50 text-primary-700 dark:bg-primary-950/60 dark:text-primary-300">
                <LifeBuoy className="h-4 w-4" aria-hidden="true" />
              </span>
              <div className="min-w-0">
                <p className="text-xs font-semibold text-content">Butuh bantuan?</p>
                <p className="mt-0.5 text-2xs leading-4 text-content-muted">
                  Tim koperasi siap membantu hari kerja 08.00–17.00 WIB.
                </p>
                <Link
                  href="/settings"
                  className="mt-2 inline-flex text-2xs font-semibold text-primary-700 underline-offset-2 hover:underline dark:text-primary-300"
                >
                  Hubungi dukungan
                </Link>
              </div>
            </div>
          </div>
        ) : null}

        <button
          type="button"
          onClick={onToggleCollapsed}
          className={cn(
            "hidden w-full items-center gap-2.5 rounded-xl px-3 py-2.5 text-sm font-medium text-content-muted transition-colors hover:bg-surface-subtle hover:text-content focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600 focus-visible:ring-offset-2 focus-visible:ring-offset-surface lg:flex",
            collapsed && "justify-center px-0",
          )}
          aria-label={collapsed ? "Perluas menu samping" : "Ciutkan menu samping"}
          aria-expanded={!collapsed}
        >
          {collapsed ? (
            <PanelLeftOpen className="h-5 w-5" aria-hidden="true" />
          ) : (
            <PanelLeftClose className="h-5 w-5" aria-hidden="true" />
          )}
          <span className={cn(collapsed && "sr-only")}>{collapsed ? "Perluas" : "Ciutkan menu"}</span>
        </button>
      </div>

    </div>
  );
}

export default function Sidebar({ collapsed, onToggleCollapsed, mobileOpen, onCloseMobile }) {
  return (
    <>
      <aside
        className={cn(
          "sticky top-16 hidden h-[calc(100vh-4rem)] shrink-0 border-r border-line bg-surface-raised transition-[width] duration-200 ease-standard lg:flex lg:flex-col",
          collapsed ? "w-[4.75rem]" : "w-60",
        )}
      >
        <SidebarContent
          collapsed={collapsed}
          onToggleCollapsed={onToggleCollapsed}
        />
      </aside>

      {mobileOpen ? (
        <div className="fixed inset-0 z-50 lg:hidden">
          <button
            type="button"
            className="absolute inset-0 h-full w-full cursor-default bg-neutral-950/50"
            onClick={onCloseMobile}
            aria-label="Tutup menu navigasi"
            tabIndex={-1}
          />
          <div
            id="mobile-sidebar"
            role="dialog"
            aria-modal="true"
            aria-label="Menu navigasi"
            className="absolute inset-y-0 left-0 w-72 max-w-[85%] border-r border-line bg-surface-raised shadow-modal animate-fade-in"
          >
            <SidebarContent
              collapsed={false}
              onToggleCollapsed={onToggleCollapsed}
              onNavigate={onCloseMobile}
              onClose={onCloseMobile}
            />
          </div>
        </div>
      ) : null}
    </>
  );
}
