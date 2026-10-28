"use client";

import { AlertTriangle, CheckCircle2 } from "lucide-react";
import Modal from "@/components/Modal";
import { Button } from "@/components/ui/Button";

export default function ConfirmModal({
  open,
  title = "Konfirmasi tindakan",
  description,
  confirmLabel = "Konfirmasi",
  cancelLabel = "Batal",
  variant = "primary",
  loading = false,
  onConfirm,
  onClose,
  children,
}) {
  const Icon = variant === "danger" ? AlertTriangle : CheckCircle2;

  return (
    <Modal
      open={open}
      onClose={loading ? () => {} : onClose}
      title={title}
      description={description}
      size="sm"
      footer={
        <>
          <Button type="button" variant="secondary" onClick={onClose} disabled={loading}>
            {cancelLabel}
          </Button>
          <Button
            type="button"
            variant={variant === "danger" ? "danger" : "primary"}
            onClick={onConfirm}
            isLoading={loading}
            loadingText="Memproses..."
          >
            {confirmLabel}
          </Button>
        </>
      }
    >
      <div className="flex items-start gap-3">
        <span
          className={
            variant === "danger"
              ? "flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-danger-50 text-danger-600 dark:bg-danger-700/15 dark:text-danger-200"
              : "flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-primary-50 text-primary-700 dark:bg-primary-950/60 dark:text-primary-300"
          }
          aria-hidden="true"
        >
          <Icon className="h-5 w-5" />
        </span>
        <div className="min-w-0 text-sm leading-6 text-content-muted">
          {children || (
            <p>
              Pastikan Anda sudah memeriksa data. Tindakan ini akan langsung diproses oleh sistem.
            </p>
          )}
        </div>
      </div>
    </Modal>
  );
}
