"use client";

import Link from "next/link";
import { Bell, X } from "lucide-react";

export default function NotificationToast({ notification, onClose }) {
  if (!notification) {
    return null;
  }

  return (
    <div
      role="status"
      aria-live="polite"
      className="fixed bottom-4 right-4 z-50 w-[calc(100%-2rem)] max-w-sm animate-fade-in rounded-2xl border border-line bg-surface-raised p-4 shadow-modal"
    >
      <div className="flex items-start gap-3">
        <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-primary-50 text-primary-700 dark:bg-primary-950/60 dark:text-primary-300">
          <Bell className="h-4 w-4" aria-hidden="true" />
        </span>
        <div className="min-w-0 flex-1">
          <p className="text-sm font-semibold text-content">{notification.title || "Notifikasi baru"}</p>
          <p className="mt-0.5 line-clamp-3 text-xs leading-5 text-content-muted">
            {notification.body}
          </p>
          <Link
            href="/notifications"
            onClick={onClose}
            className="mt-2 inline-flex rounded text-xs font-semibold text-primary-700 underline-offset-2 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600 dark:text-primary-300"
          >
            Lihat notifikasi
          </Link>
        </div>
        <button
          type="button"
          onClick={onClose}
          className="inline-flex h-7 w-7 shrink-0 items-center justify-center rounded-lg text-content-subtle transition-colors hover:bg-surface-subtle hover:text-content focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600"
          aria-label="Tutup notifikasi"
        >
          <X className="h-4 w-4" aria-hidden="true" />
        </button>
      </div>
    </div>
  );
}
