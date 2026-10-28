"use client";

import { useState } from "react";
import Link from "next/link";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { AlertCircle, ArrowLeft, CheckCircle2, KeyRound, ShieldAlert } from "lucide-react";
import AuthShell from "@/components/auth/AuthShell";
import { PasswordInput } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import { authService } from "@/services/authService";
import { resetPasswordSchema } from "@/lib/validations/auth";
import { getApiErrorMessage } from "@/lib/utils";

export default function ResetPasswordForm({ token }) {
  const [formError, setFormError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [completed, setCompleted] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(resetPasswordSchema),
    mode: "onTouched",
    defaultValues: { newPassword: "", confirmPassword: "" },
  });

  async function onSubmit(values) {
    setFormError("");
    setIsSubmitting(true);
    try {
      await authService.resetPassword({ token, newPassword: values.newPassword });
      setCompleted(true);
    } catch (error) {
      setFormError(
        getApiErrorMessage(
          error,
          "Kata sandi belum dapat diperbarui. Tautan mungkin sudah kedaluwarsa.",
        ),
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  if (!token) {
    return (
      <AuthShell active="login">
        <div className="text-center">
          <ShieldAlert size={48} className="mx-auto mb-4 text-amber-500" aria-hidden="true" />
          <h1 className="mb-2 text-3xl font-bold text-blue-600">Tautan Tidak Valid</h1>
          <p className="mb-6 text-sm text-gray-500">
            Tautan reset tidak lengkap atau sudah kedaluwarsa. Minta tautan baru untuk melanjutkan.
          </p>
          <Link
            href="/forgot-password"
            className="flex h-14 w-full items-center justify-center rounded-xl bg-blue-600 text-base font-semibold text-white transition-colors hover:bg-blue-800"
          >
            Minta tautan baru
          </Link>
          <Link
            href="/login"
            className="mt-5 flex items-center justify-center gap-2 text-sm text-gray-500 transition-colors hover:text-gray-700"
          >
            <ArrowLeft size={14} aria-hidden="true" />
            Kembali ke halaman masuk
          </Link>
        </div>
      </AuthShell>
    );
  }

  if (completed) {
    return (
      <AuthShell active="login">
        <div className="text-center">
          <CheckCircle2 size={48} className="mx-auto mb-4 text-emerald-600" aria-hidden="true" />
          <h1 className="mb-2 text-3xl font-bold text-blue-600">Kata Sandi Diperbarui</h1>
          <p className="mb-6 text-sm text-gray-500">
            Kata sandi baru Anda sudah aktif. Silakan masuk kembali menggunakan kata sandi tersebut.
          </p>
          <Link
            href="/login"
            className="flex h-14 w-full items-center justify-center rounded-xl bg-blue-600 text-base font-semibold text-white transition-colors hover:bg-blue-800"
          >
            Masuk ke akun
          </Link>
        </div>
      </AuthShell>
    );
  }

  return (
    <AuthShell active="login">
      <div className="mb-2 flex items-center gap-2 text-blue-600">
        <KeyRound size={20} aria-hidden="true" />
        <span className="text-2xs font-semibold uppercase tracking-[0.16em]">Keamanan akun</span>
      </div>
      <h1 className="mb-2 text-3xl font-bold text-blue-600">Buat Kata Sandi Baru</h1>
      <p className="mb-6 text-sm leading-relaxed text-gray-500">
        Gunakan minimal 12 karakter dengan huruf besar, huruf kecil, angka, dan karakter khusus.
      </p>

      {formError ? (
        <div
          role="alert"
          className="mb-4 flex items-start gap-2.5 rounded-xl border border-red-200 bg-red-50 px-3.5 py-3 text-sm leading-6 text-red-700"
        >
          <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
          <span>{formError}</span>
        </div>
      ) : null}

      <form onSubmit={handleSubmit(onSubmit)} method="post" noValidate className="space-y-4">
        <PasswordInput
          label="Kata sandi baru"
          autoComplete="new-password"
          placeholder="Kata sandi baru"
          error={errors.newPassword?.message}
          {...register("newPassword")}
        />
        <PasswordInput
          label="Konfirmasi kata sandi"
          autoComplete="new-password"
          placeholder="Ulangi kata sandi baru"
          error={errors.confirmPassword?.message}
          {...register("confirmPassword")}
        />
        <Button type="submit" fullWidth isLoading={isSubmitting} loadingText="Menyimpan...">
          Simpan kata sandi
        </Button>
      </form>

      <Link
        href="/login"
        className="mt-5 flex items-center justify-center gap-2 text-sm text-gray-500 transition-colors hover:text-gray-700"
      >
        <ArrowLeft size={14} aria-hidden="true" />
        Kembali ke halaman masuk
      </Link>
    </AuthShell>
  );
}
