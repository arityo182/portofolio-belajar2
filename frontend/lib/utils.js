import { clsx } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs) {
  return twMerge(clsx(inputs));
}

export function getInitials(source, fallback = "KD") {
  if (!source) {
    return fallback;
  }
  const base = String(source).split("@")[0].replace(/[._-]+/g, " ").trim();
  const parts = base.split(/\s+/).filter(Boolean);
  if (parts.length === 0) {
    return fallback;
  }
  if (parts.length === 1) {
    return parts[0].slice(0, 2).toUpperCase();
  }
  return `${parts[0][0]}${parts[parts.length - 1][0]}`.toUpperCase();
}

export function maskEmail(email) {
  if (!email || !email.includes("@")) {
    return email || "";
  }
  const [local, domain] = email.split("@");
  const visible = local.slice(0, Math.min(2, local.length));
  return `${visible}${"*".repeat(Math.max(3, local.length - visible.length))}@${domain}`;
}

export function formatDate(value, options = {}) {
  if (!value) {
    return "—";
  }
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "—";
  }
  return new Intl.DateTimeFormat("id-ID", {
    day: "2-digit",
    month: "long",
    year: "numeric",
    ...options,
  }).format(date);
}

export function formatNumber(value, options = {}) {
  const number = Number(value);
  if (!Number.isFinite(number)) {
    return "—";
  }
  return new Intl.NumberFormat("id-ID", {
    maximumFractionDigits: 0,
    ...options,
  }).format(number);
}

export function formatCurrency(value, options = {}) {
  const number = Number(value);
  if (!Number.isFinite(number)) {
    return "—";
  }
  return new Intl.NumberFormat("id-ID", {
    style: "currency",
    currency: "IDR",
    maximumFractionDigits: 0,
    ...options,
  }).format(number);
}

export function safeRedirectPath(candidate, fallback = "/dashboard") {
  if (typeof candidate !== "string" || candidate.length === 0) {
    return fallback;
  }
  if (!candidate.startsWith("/") || candidate.startsWith("//")) {
    return fallback;
  }
  if (candidate.startsWith("/login") || candidate.startsWith("/register")) {
    return fallback;
  }
  return candidate;
}

export function getApiErrorMessage(error, fallback = "Terjadi kesalahan. Silakan coba lagi.") {
  const payload = error?.response?.data;
  if (payload && typeof payload === "object" && typeof payload.message === "string") {
    return payload.message;
  }
  if (error?.code === "ECONNABORTED") {
    return "Permintaan melebihi batas waktu. Periksa koneksi Anda.";
  }
  if (!error?.response && error?.request) {
    return "Tidak dapat terhubung ke server. Periksa koneksi Anda.";
  }
  return error?.message || fallback;
}

export function isAuthError(error) {
  return error?.response?.status === 401 || error?.response?.status === 403;
}

export function formatDateShort(value) {
  if (!value) {
    return "—";
  }
  const date = value instanceof Date ? value : new Date(`${value}`.length === 10 ? `${value}T00:00:00` : value);
  if (Number.isNaN(date.getTime())) {
    return "—";
  }
  return new Intl.DateTimeFormat("id-ID", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  }).format(date);
}

export function formatDateTime(value) {
  if (!value) {
    return "—";
  }
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "—";
  }
  return new Intl.DateTimeFormat("id-ID", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

export function localDateKey(date = new Date()) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export function isDateInMonth(value, reference = new Date()) {
  if (!value || typeof value !== "string") {
    return false;
  }
  const [year, month] = value.split("-").map(Number);
  return year === reference.getFullYear() && month === reference.getMonth() + 1;
}

export function isDateBeforeToday(value) {
  if (!value || typeof value !== "string") {
    return false;
  }
  return value < localDateKey();
}

export function formatCompactCurrency(value) {
  const number = Number(value);
  if (!Number.isFinite(number)) {
    return "—";
  }
  const absolute = Math.abs(number);
  const format = (divisor, suffix, maximumFractionDigits) =>
    `Rp${(number / divisor).toLocaleString("id-ID", { maximumFractionDigits })} ${suffix}`;

  if (absolute >= 1_000_000_000) {
    return format(1_000_000_000, "M", 1);
  }
  if (absolute >= 1_000_000) {
    return format(1_000_000, "jt", 1);
  }
  if (absolute >= 100_000) {
    return format(1_000, "rb", 0);
  }
  return formatCurrency(number);
}
