import { z } from "zod";

const EMAIL_MESSAGE = "Masukkan alamat email yang valid";
const PHONE_REGEX = /^\+?62[0-9]{9,13}$/;
const PASSWORD_REGEX =
  /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&#^()_+\-=[\]{};:'",.<>/\\|`~]).+$/;

export const loginSchema = z.object({
  email: z
    .string({ error: "Email wajib diisi" })
    .trim()
    .min(1, "Email wajib diisi")
    .email(EMAIL_MESSAGE),
  password: z
    .string({ error: "Kata sandi wajib diisi" })
    .min(1, "Kata sandi wajib diisi"),
});

export const registerSchema = z
  .object({
    email: z
      .string({ error: "Email wajib diisi" })
      .trim()
      .min(1, "Email wajib diisi")
      .email(EMAIL_MESSAGE)
      .max(255, "Email maksimal 255 karakter"),
    phone: z
      .string({ error: "Nomor WhatsApp wajib diisi" })
      .trim()
      .min(1, "Nomor WhatsApp wajib diisi")
      .regex(PHONE_REGEX, "Gunakan format Indonesia, contoh +6281234567890"),
    password: z
      .string({ error: "Kata sandi wajib diisi" })
      .min(12, "Kata sandi minimal 12 karakter")
      .max(72, "Kata sandi maksimal 72 karakter")
      .regex(
        PASSWORD_REGEX,
        "Kata sandi harus memuat huruf besar, huruf kecil, angka, dan karakter khusus",
      ),
    confirmPassword: z
      .string({ error: "Konfirmasi kata sandi wajib diisi" })
      .min(1, "Konfirmasi kata sandi wajib diisi"),
    role: z.enum(["BORROWER", "INVESTOR"], {
      error: "Pilih salah satu peran akun",
    }),
    acceptTerms: z
      .boolean()
      .refine((value) => value === true, "Anda harus menyetujui syarat dan ketentuan"),
  })
  .refine((data) => data.password === data.confirmPassword, {
    path: ["confirmPassword"],
    message: "Konfirmasi kata sandi tidak cocok",
  });

export const forgotPasswordSchema = z.object({
  email: z
    .string({ error: "Email wajib diisi" })
    .trim()
    .min(1, "Email wajib diisi")
    .email(EMAIL_MESSAGE),
});

export const resetPasswordSchema = z
  .object({
    newPassword: z
      .string({ error: "Kata sandi baru wajib diisi" })
      .min(12, "Kata sandi minimal 12 karakter")
      .max(72, "Kata sandi maksimal 72 karakter")
      .regex(
        PASSWORD_REGEX,
        "Kata sandi harus memuat huruf besar, huruf kecil, angka, dan karakter khusus",
      ),
    confirmPassword: z
      .string({ error: "Konfirmasi kata sandi wajib diisi" })
      .min(1, "Konfirmasi kata sandi wajib diisi"),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    path: ["confirmPassword"],
    message: "Konfirmasi kata sandi tidak cocok",
  });

export function getPasswordStrength(password = "") {
  const checks = {
    length: password.length >= 12,
    lower: /[a-z]/.test(password),
    upper: /[A-Z]/.test(password),
    number: /\d/.test(password),
    symbol: /[@$!%*?&#^()_+\-=[\]{};:'",.<>/\\|`~]/.test(password),
  };
  const score = Object.values(checks).filter(Boolean).length;
  const labels = ["Sangat lemah", "Lemah", "Cukup", "Kuat", "Sangat kuat", "Sangat kuat"];
  return {
    checks,
    score,
    label: password.length === 0 ? "Belum diisi" : labels[score],
    valid: score === 5,
  };
}
