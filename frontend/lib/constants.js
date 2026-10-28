export const APP_NAME = "Koperasi Digital";
export const APP_TAGLINE = "Modal usaha yang adil untuk UMKM Indonesia";

export const ROLES = {
  BORROWER: "BORROWER",
  INVESTOR: "INVESTOR",
  REVIEWER: "REVIEWER",
  ADMIN: "ADMIN",
};

export const ROLE_LABELS = {
  BORROWER: "Peminjam",
  INVESTOR: "Investor",
  REVIEWER: "Reviewer",
  ADMIN: "Administrator",
};

export const ACCOUNT_STATUS_LABELS = {
  ACTIVE: "Aktif",
  PENDING_VERIFICATION: "Menunggu verifikasi",
  SUSPENDED: "Ditangguhkan",
  LOCKED: "Terkunci",
};

export const LOAN_STATUS = {
  PENDING: "pending",
  SCORING: "scoring",
  UNDER_REVIEW: "review",
  APPROVED: "approved",
  REJECTED: "rejected",
  DISBURSED: "disbursed",
  ACTIVE: "active",
  COMPLETED: "completed",
  DEFAULT: "defaulted",
  DEFAULTED: "defaulted",
  OVERDUE: "overdue",
};

export const LOAN_STATUS_LABELS = {
  PENDING: "Menunggu",
  SCORING: "Penilaian",
  UNDER_REVIEW: "Ditinjau",
  APPROVED: "Disetujui",
  REJECTED: "Ditolak",
  DISBURSED: "Dicairkan",
  ACTIVE: "Aktif",
  COMPLETED: "Selesai",
  DEFAULT: "Gagal bayar",
  DEFAULTED: "Gagal bayar",
  OVERDUE: "Terlambat",
};

export const NOTIFICATION_TYPE_LABELS = {
  LOAN_STATUS: "Status pinjaman",
  PAYMENT: "Pembayaran",
  SYSTEM: "Sistem",
};

export const getRoleLabel = (role) => ROLE_LABELS[role] || role || "Pengguna";

export const getLoanStatusLabel = (status) =>
  LOAN_STATUS_LABELS[String(status || "").toUpperCase()] || status || "—";

export const getAccountStatusLabel = (status) =>
  ACCOUNT_STATUS_LABELS[String(status || "").toUpperCase()] || status || "—";

export const INSTALLMENT_STATUS_LABELS = {
  PENDING: "Belum dibayar",
  PAID: "Lunas",
  OVERDUE: "Terlambat",
};

export const PAYMENT_GATEWAY_LABELS = {
  MIDTRANS: "Midtrans",
  XENDIT: "Xendit",
};

export const LOAN_ACTIVE_STATUSES = ["APPROVED", "DISBURSED", "ACTIVE"];
export const LOAN_OUTSTANDING_STATUSES = ["DISBURSED", "ACTIVE"];
export const LOAN_PAYABLE_STATUSES = ["DISBURSED", "ACTIVE", "DEFAULT"];
export const LOAN_FINANCIAL_STATUSES = ["DISBURSED", "ACTIVE", "DEFAULT"];

export const LOAN_FILTER_OPTIONS = [
  { value: "", label: "Semua" },
  { value: "PENDING", label: "Menunggu" },
  { value: "SCORING", label: "Penilaian" },
  { value: "UNDER_REVIEW", label: "Ditinjau" },
  { value: "APPROVED", label: "Disetujui" },
  { value: "DISBURSED", label: "Dicairkan" },
  { value: "ACTIVE", label: "Aktif" },
  { value: "COMPLETED", label: "Selesai" },
  { value: "REJECTED", label: "Ditolak" },
  { value: "DEFAULT", label: "Gagal bayar" },
];

export const getInstallmentStatusLabel = (status) =>
  INSTALLMENT_STATUS_LABELS[String(status || "").toUpperCase()] || status || "—";

export const getPaymentGatewayLabel = (gateway) =>
  PAYMENT_GATEWAY_LABELS[String(gateway || "").toUpperCase()] || gateway || "—";
