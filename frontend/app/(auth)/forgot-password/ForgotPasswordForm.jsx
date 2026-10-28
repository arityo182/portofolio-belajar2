"use client";

import { useState } from "react";
import Link from "next/link";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { AlertCircle, ArrowLeft, MailCheck } from "lucide-react";
import AuthShell from "@/components/auth/AuthShell";
import { authService } from "@/services/authService";
import { forgotPasswordSchema } from "@/lib/validations/auth";
import { getApiErrorMessage } from "@/lib/utils";

export default function ForgotPasswordForm() {
  const [submittedEmail, setSubmittedEmail] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formError, setFormError] = useState("");

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(forgotPasswordSchema),
    mode: "onTouched",
    defaultValues: { email: "" },
  });

  async function onSubmit(values) {
    setFormError("");
    setIsSubmitting(true);
    try {
      // The endpoint is intentionally idempotent: the UI never reveals whether an email exists.
      await authService.forgotPassword(values.email.trim());
      setSubmittedEmail(values.email.trim());
    } catch (error) {
      setFormError(
        getApiErrorMessage(error, "Tautan reset belum dapat dikirim. Coba lagi beberapa saat lagi."),
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <AuthShell active="login">
      {submittedEmail ? (
        <div className="text-center">
          <MailCheck size={48} className="mx-auto mb-4 text-blue-600" aria-hidden="true" />
          <h1 className="mb-2 text-3xl font-bold text-blue-600">Email Terkirim!</h1>
          <p className="mb-6 text-sm text-gray-500">
            Tautan reset telah dikirim ke email Anda. Periksa folder spam jika tidak ada.
          </p>
          <button
            type="button"
            onClick={() => {
              setSubmittedEmail("");
              reset();
            }}
            className="h-14 w-full rounded-xl border border-blue-600 text-base font-semibold text-blue-600 transition-colors hover:bg-blue-50"
          >
            Kirim Ulang
          </button>
          <Link
            href="/login"
            className="mt-5 flex items-center justify-center gap-2 text-sm text-gray-500 transition-colors hover:text-gray-700"
          >
            <ArrowLeft size={14} aria-hidden="true" />
            Kembali ke halaman masuk
          </Link>
        </div>
      ) : (
        <>
          <h1 className="mb-2 text-3xl font-bold text-blue-600">Lupa Kata Sandi?</h1>
          <p className="mb-8 text-sm leading-relaxed text-gray-500">
            Masukkan email terdaftar. Kami akan mengirim tautan untuk membuat kata sandi baru.
          </p>

          <form onSubmit={handleSubmit(onSubmit)} method="post" noValidate>
            {formError ? (
              <div
                role="alert"
                className="mb-4 flex items-start gap-2.5 rounded-xl border border-red-200 bg-red-50 px-3.5 py-3 text-sm leading-6 text-red-700"
              >
                <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
                <span>{formError}</span>
              </div>
            ) : null}
            <div className="mb-6">
              <input
                id="email"
                required
                type="email"
                aria-label="Email"
                autoComplete="email"
                placeholder="Email"
                className="h-14 w-full rounded-xl border border-gray-200 px-4 text-sm text-gray-900 placeholder:text-gray-400 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
                {...register("email")}
              />
              {errors.email?.message ? (
                <p role="alert" className="mt-1.5 text-xs text-red-500">
                  {errors.email.message}
                </p>
              ) : null}
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className="h-14 w-full rounded-xl bg-blue-600 text-base font-semibold text-white transition-colors hover:bg-blue-800 disabled:opacity-70"
            >
              {isSubmitting ? "Mengirim..." : "Kirim Tautan Reset"}
            </button>
          </form>

          <Link
            href="/login"
            className="mt-5 flex items-center justify-center gap-2 text-sm text-gray-500 transition-colors hover:text-gray-700"
          >
            <ArrowLeft size={14} aria-hidden="true" />
            Kembali ke halaman masuk
          </Link>
        </>
      )}
    </AuthShell>
  );
}
