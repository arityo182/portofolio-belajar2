"use client";

import { useState } from "react";
import Link from "next/link";
import { AlertCircle, Bell, CheckCheck, ChevronLeft, ChevronRight, Inbox } from "lucide-react";
import PageHeader from "@/components/ui/PageHeader";
import { Card, CardContent } from "@/components/ui/Card";
import Badge from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import EmptyState from "@/components/ui/EmptyState";
import LoadingSkeleton from "@/components/ui/LoadingSkeleton";
import {
  useMarkAllNotificationsRead,
  useMarkNotificationRead,
  useNotificationList,
} from "@/hooks/useNotificationList";
import { formatDateTime } from "@/lib/utils";

const PAGE_SIZE = 10;

const TYPE_LABELS = {
  LOAN_STATUS: "Status pinjaman",
  PAYMENT: "Pembayaran",
  SYSTEM: "Sistem",
};

export default function NotificationsView() {
  const [filter, setFilter] = useState("all");
  const [page, setPage] = useState(0);
  const isRead = filter === "unread" ? false : undefined;
  const query = useNotificationList({ isRead, page, size: PAGE_SIZE });
  const markRead = useMarkNotificationRead();
  const markAllRead = useMarkAllNotificationsRead();

  const data = query.data;
  const notifications = data?.content || [];
  const unreadCount = data?.unreadCount || 0;

  if (query.isLoading) {
    return <LoadingSkeleton variant="page" />;
  }

  if (query.isError) {
    return (
      <Card>
        <CardContent className="py-12 text-center">
          <AlertCircle className="mx-auto h-6 w-6 text-danger-600" aria-hidden="true" />
          <p className="mt-2 text-sm font-semibold text-content">Gagal memuat notifikasi</p>
          <Button variant="secondary" className="mt-4" onClick={() => query.refetch()}>
            Coba lagi
          </Button>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Pemberitahuan"
        title="Notifikasi"
        description="Pembaruan status pinjaman, pembayaran, dan informasi akun Anda."
        actions={
          <Button
            variant="secondary"
            size="sm"
            onClick={() => markAllRead.mutate()}
            isLoading={markAllRead.isPending}
            disabled={unreadCount === 0}
            leftIcon={<CheckCheck className="h-4 w-4" />}
          >
            Tandai semua dibaca
          </Button>
        }
      />

      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="inline-flex rounded-xl border border-line bg-surface-raised p-1">
          {[
            { value: "all", label: "Semua" },
            { value: "unread", label: `Belum dibaca${unreadCount ? ` (${unreadCount})` : ""}` },
          ].map((item) => (
            <button
              key={item.value}
              type="button"
              onClick={() => {
                setFilter(item.value);
                setPage(0);
              }}
              className={`rounded-lg px-3 py-1.5 text-xs font-semibold transition-colors ${
                filter === item.value
                  ? "bg-primary-600 text-white"
                  : "text-content-muted hover:bg-surface-subtle hover:text-content"
              }`}
            >
              {item.label}
            </button>
          ))}
        </div>
        <p className="text-xs text-content-muted">
          {data?.totalElements || 0} notifikasi
        </p>
      </div>

      {notifications.length === 0 ? (
        <EmptyState
          icon={Inbox}
          title={filter === "unread" ? "Tidak ada notifikasi belum dibaca" : "Belum ada notifikasi"}
          description="Pembaruan status pengajuan, jatuh tempo, dan konfirmasi pembayaran akan muncul di sini."
        />
      ) : (
        <Card>
          <CardContent className="p-0">
            <ul className="divide-y divide-line">
              {notifications.map((notification) => (
                <li
                  key={notification.id}
                  className={`flex flex-col gap-2 px-4 py-4 sm:flex-row sm:items-start sm:justify-between sm:px-5 ${
                    notification.isRead ? "" : "bg-primary-50/50 dark:bg-primary-950/20"
                  }`}
                >
                  <div className="flex min-w-0 items-start gap-3">
                    <span
                      className={`mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-xl ${
                        notification.isRead
                          ? "bg-surface-subtle text-content-muted"
                          : "bg-primary-100 text-primary-700 dark:bg-primary-950/60 dark:text-primary-300"
                      }`}
                    >
                      <Bell className="h-4 w-4" aria-hidden="true" />
                    </span>
                    <div className="min-w-0">
                      <div className="flex flex-wrap items-center gap-2">
                        <p className="text-sm font-semibold text-content">{notification.title}</p>
                        {!notification.isRead ? <Badge variant="primary">Baru</Badge> : null}
                        <Badge variant="neutral">
                          {TYPE_LABELS[notification.type] || notification.type}
                        </Badge>
                      </div>
                      <p className="mt-1 text-sm leading-6 text-content-muted">{notification.body}</p>
                      <p className="mt-1 text-2xs text-content-subtle">
                        {formatDateTime(notification.createdAt)}
                      </p>
                    </div>
                  </div>
                  <div className="flex shrink-0 items-center gap-2 sm:pl-4">
                    {notification.type === "LOAN_STATUS" && notification.referenceId ? (
                      <Link
                        href={`/loans/${notification.referenceId}`}
                        className="text-xs font-semibold text-primary-700 underline-offset-2 hover:underline dark:text-primary-300"
                      >
                        Lihat pinjaman
                      </Link>
                    ) : null}
                    {!notification.isRead ? (
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => markRead.mutate(notification.id)}
                        isLoading={markRead.isPending}
                      >
                        Tandai dibaca
                      </Button>
                    ) : null}
                  </div>
                </li>
              ))}
            </ul>
          </CardContent>
        </Card>
      )}

      {(data?.totalPages || 1) > 1 ? (
        <div className="flex items-center justify-between">
          <p className="text-xs text-content-muted">
            Halaman {(data?.page || 0) + 1} dari {data?.totalPages}
          </p>
          <div className="flex gap-2">
            <Button
              variant="secondary"
              size="sm"
              disabled={page === 0}
              onClick={() => setPage((current) => Math.max(0, current - 1))}
              leftIcon={<ChevronLeft className="h-4 w-4" />}
            >
              Sebelumnya
            </Button>
            <Button
              variant="secondary"
              size="sm"
              disabled={page + 1 >= (data?.totalPages || 1)}
              onClick={() => setPage((current) => current + 1)}
              rightIcon={<ChevronRight className="h-4 w-4" />}
            >
              Berikutnya
            </Button>
          </div>
        </div>
      ) : null}
    </div>
  );
}
