import { z } from "zod";

export const MAX_DOCUMENT_BYTES = 5 * 1024 * 1024;
export const ACCEPTED_DOCUMENT_TYPES = ["image/jpeg", "image/png"];

export const STEP_FIELDS = {
  1: ["loanProductId"],
  2: ["amount", "tenorMonths", "purpose"],
  3: [],
  4: ["acceptTerms"],
};

export const INITIAL_APPLY_VALUES = {
  loanProductId: "",
  amount: "",
  tenorMonths: "",
  purpose: "",
  acceptTerms: false,
};

export function createLoanApplySchema(product) {
  const minAmount = Number(product?.minAmount ?? 1);
  const maxAmount = Number(product?.maxAmount ?? Number.MAX_SAFE_INTEGER);
  const minTenor = Number(product?.minTenorMonths ?? 1);
  const maxTenor = Number(product?.maxTenorMonths ?? 60);

  return z.object({
    loanProductId: z.coerce
      .number({ error: "Pilih produk pinjaman terlebih dahulu" })
      .int("Produk pinjaman tidak valid")
      .positive("Pilih produk pinjaman terlebih dahulu"),
    amount: z.coerce
      .number({ error: "Masukkan nominal pinjaman" })
      .min(minAmount, `Nominal minimal Rp ${minAmount.toLocaleString("id-ID")}`)
      .max(maxAmount, `Nominal maksimal Rp ${maxAmount.toLocaleString("id-ID")}`)
      .refine((value) => Number.isFinite(value) && value > 0, "Masukkan nominal pinjaman"),
    tenorMonths: z.coerce
      .number({ error: "Pilih tenor pinjaman" })
      .int("Tenor harus berupa bilangan bulat")
      .min(minTenor, `Tenor minimal ${minTenor} bulan`)
      .max(maxTenor, `Tenor maksimal ${maxTenor} bulan`),
    purpose: z
      .string()
      .trim()
      .min(10, "Tujuan pinjaman minimal 10 karakter")
      .max(1000, "Tujuan pinjaman maksimal 1000 karakter"),
    acceptTerms: z
      .boolean()
      .refine((value) => value === true, "Anda harus menyetujui pernyataan dan ketentuan"),
  });
}

export function validateDocumentFile(file) {
  if (!file) {
    return "Pilih berkas terlebih dahulu";
  }
  if (!ACCEPTED_DOCUMENT_TYPES.includes(file.type)) {
    return "Format harus JPEG atau PNG";
  }
  if (file.size > MAX_DOCUMENT_BYTES) {
    return "Ukuran berkas maksimal 5 MB";
  }
  return null;
}

export function formatFileSize(bytes) {
  if (!Number.isFinite(bytes) || bytes <= 0) {
    return "0 KB";
  }
  if (bytes < 1024 * 1024) {
    return `${Math.ceil(bytes / 1024)} KB`;
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}
