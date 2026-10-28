import { Check, Circle, Clock, X } from "lucide-react";
import { cn, formatDateShort } from "@/lib/utils";

function buildTimeline(application, installments = []) {
  const status = application?.status;
  const reviewed = ["APPROVED", "REJECTED", "DISBURSED", "ACTIVE", "COMPLETED", "DEFAULT"].includes(
    status,
  );
  const disbursed = ["DISBURSED", "ACTIVE", "COMPLETED", "DEFAULT"].includes(status);
  const paidCount = installments.filter((installment) => installment.status === "PAID").length;

  return [
    {
      key: "submitted",
      title: "Pengajuan dibuat",
      description: "Data pengajuan dan tujuan pinjaman diterima sistem.",
      date: application?.submittedAt,
      state: "done",
    },
    {
      key: "scoring",
      title: "Penilaian kredit AI",
      description:
        status === "PENDING" || status === "SCORING"
          ? "Model sedang menilai profil dan kapasitas usaha Anda."
          : "Penilaian selesai dan hasilnya diteruskan ke reviewer.",
      date: null,
      state: status === "PENDING" || status === "SCORING" ? "active" : "done",
    },
    {
      key: "review",
      title: status === "REJECTED" ? "Pengajuan ditolak" : "Keputusan reviewer",
      description:
        status === "REJECTED"
          ? application?.rejectionReason || "Pengajuan belum dapat disetujui."
          : status === "UNDER_REVIEW"
            ? "Tim reviewer sedang memeriksa pengajuan Anda."
            : reviewed
              ? "Pengajuan telah diputuskan oleh reviewer."
              : "Menunggu penilaian selesai.",
      date: application?.reviewedAt,
      state:
        status === "UNDER_REVIEW"
          ? "active"
          : status === "REJECTED"
            ? "danger"
            : reviewed
              ? "done"
              : "pending",
    },
    {
      key: "disbursement",
      title: "Pencairan dana",
      description:
        status === "APPROVED"
          ? "Pengajuan disetujui. Dana akan dicairkan ke rekening terdaftar."
          : disbursed
            ? "Dana pinjaman telah dicairkan."
            : status === "REJECTED"
              ? "Tidak berlaku karena pengajuan ditolak."
              : "Menunggu persetujuan.",
      date: application?.disbursedAt,
      state:
        status === "APPROVED"
          ? "active"
          : disbursed
            ? "done"
            : status === "REJECTED"
              ? "muted"
              : "pending",
    },
    {
      key: "repayment",
      title: "Pembayaran cicilan",
      description:
        status === "COMPLETED"
          ? `Seluruh ${installments.length || ""} cicilan telah lunas.`
          : disbursed
            ? installments.length > 0
              ? `${paidCount} dari ${installments.length} cicilan telah dibayar.`
              : "Jadwal cicilan sedang disiapkan."
            : "Jadwal cicilan muncul setelah dana dicairkan.",
      date: application?.completedAt,
      state:
        status === "COMPLETED"
          ? "done"
          : status === "DEFAULT"
            ? "danger"
            : disbursed
              ? "active"
              : "pending",
    },
  ];
}

const STATE_STYLES = {
  done: {
    icon: Check,
    iconClass: "bg-success-600 text-white",
    titleClass: "text-content",
    lineClass: "bg-success-200 dark:bg-success-700/40",
  },
  active: {
    icon: Clock,
    iconClass: "bg-primary-600 text-white",
    titleClass: "text-content",
    lineClass: "bg-line",
  },
  danger: {
    icon: X,
    iconClass: "bg-danger-600 text-white",
    titleClass: "text-danger-700 dark:text-danger-300",
    lineClass: "bg-danger-200 dark:bg-danger-700/40",
  },
  pending: {
    icon: Circle,
    iconClass: "border border-line bg-surface-raised text-content-subtle",
    titleClass: "text-content-muted",
    lineClass: "bg-line",
  },
  muted: {
    icon: Circle,
    iconClass: "border border-line bg-surface-subtle text-content-subtle",
    titleClass: "text-content-subtle",
    lineClass: "bg-line",
  },
};

export default function LoanTimeline({ application, installments = [] }) {
  const events = buildTimeline(application, installments);

  return (
    <ol className="space-y-0" aria-label="Riwayat status pinjaman">
      {events.map((event, index) => {
        const style = STATE_STYLES[event.state] || STATE_STYLES.pending;
        const Icon = style.icon;
        const isLast = index === events.length - 1;
        return (
          <li key={event.key} className="relative flex gap-3.5 pb-5 last:pb-0">
            {!isLast ? (
              <span
                className={cn("absolute left-[13px] top-7 h-[calc(100%-1.25rem)] w-0.5", style.lineClass)}
                aria-hidden="true"
              />
            ) : null}
            <span
              className={cn(
                "relative z-10 mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-full",
                style.iconClass,
              )}
              aria-hidden="true"
            >
              <Icon className="h-3.5 w-3.5" />
            </span>
            <div className="min-w-0 pt-0.5">
              <div className="flex flex-wrap items-baseline gap-x-2 gap-y-0.5">
                <p className={cn("text-sm font-semibold", style.titleClass)}>{event.title}</p>
                {event.date ? (
                  <span className="text-2xs text-content-subtle">{formatDateShort(event.date)}</span>
                ) : null}
              </div>
              <p className="mt-0.5 text-xs leading-5 text-content-muted">{event.description}</p>
            </div>
          </li>
        );
      })}
    </ol>
  );
}
