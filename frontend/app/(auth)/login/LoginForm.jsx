"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { AlertCircle, ArrowRight, CheckCircle2, Info, Lock, Mail } from "lucide-react";
import Input, { PasswordInput } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import { loginSchema } from "@/lib/validations/auth";
import { getApiErrorMessage, safeRedirectPath } from "@/lib/utils";
import { useAuth } from "@/hooks/useAuth";

function Alert({ tone = "danger", title, children, alertRef }) {
  const styles = {
    danger:
      "border-danger-200 bg-danger-50 text-danger-700 dark:border-danger-700/40 dark:bg-danger-700/15 dark:text-danger-200",
    success:
      "border-success-200 bg-success-50 text-success-700 dark:border-success-700/40 dark:bg-success-700/15 dark:text-success-200",
    info: "border-info-200 bg-info-50 text-info-700 dark:border-info-700/40 dark:bg-info-700/15 dark:text-info-200",
  };
  const Icon = tone === "danger" ? AlertCircle : tone === "success" ? CheckCircle2 : Info;

  return (
    <div
      ref={alertRef}
      tabIndex={alertRef ? -1 : undefined}
      role={tone === "danger" ? "alert" : "status"}
      className={`flex items-start gap-2.5 rounded-xl border px-3.5 py-3 text-sm leading-6 focus:outline-none ${styles[tone]}`}
    >
      <Icon className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
      <div>
        {title ? <p className="font-semibold">{title}</p> : null}
        <div>{children}</div>
      </div>
    </div>
  );
}

export default function LoginForm({
  initialEmail = "",
  registered = false,
  expired = false,
  nextPath = null,
}) {
  const router = useRouter();
  const { login } = useAuth();
  const [formError, setFormError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const errorRef = useRef(null);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(loginSchema),
    mode: "onTouched",
    defaultValues: {
      email: initialEmail,
      password: "",
    },
  });

  useEffect(() => {
    if (formError) {
      errorRef.current?.focus();
    }
  }, [formError]);

  async function onSubmit(values) {
    setFormError("");
    setIsSubmitting(true);
    try {
      await login(values);
      router.replace(safeRedirectPath(nextPath));
    } catch (error) {
      setFormError(getApiErrorMessage(error, "Email atau kata sandi salah."));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="rounded-2xl border border-line bg-surface-raised p-6 shadow-card sm:p-8">
      <div className="mb-6">
        <p className="text-2xs font-semibold uppercase tracking-[0.16em] text-primary-600 dark:text-primary-400">
          Selamat datang kembali
        </p>
        <h1 className="mt-1.5 text-2xl font-bold tracking-[-0.02em] text-content">
          Masuk ke akun Anda
        </h1>
        <p className="mt-1.5 text-sm leading-6 text-content-muted">
          Kelola pinjaman, pembayaran, dan portofolio investasi dalam satu tempat.
        </p>
      </div>

      <div className="space-y-3">
        {registered ? (
          <Alert tone="success" title="Registrasi berhasil">
            Akun Anda sudah dibuat. Silakan masuk untuk melanjutkan.
          </Alert>
        ) : null}
        {expired ? (
          <Alert tone="info" title="Sesi berakhir">
            Sesi Anda telah berakhir. Silakan masuk kembali untuk melanjutkan.
          </Alert>
        ) : null}
        {formError ? (
          <Alert tone="danger" alertRef={errorRef}>
            {formError}
          </Alert>
        ) : null}
      </div>

      <form onSubmit={handleSubmit(onSubmit)} method="post" noValidate className="mt-5 space-y-4">
        <Input
          label="Email"
          type="email"
          autoComplete="email"
          inputMode="email"
          placeholder="nama@usaha.id"
          leftIcon={<Mail className="h-4 w-4" />}
          error={errors.email?.message}
          required
          {...register("email")}
        />

        <div>
          <PasswordInput
            label="Kata sandi"
            autoComplete="current-password"
            placeholder="Masukkan kata sandi"
            leftIcon={<Lock className="h-4 w-4" />}
            error={errors.password?.message}
            required
            {...register("password")}
          />
          <div className="mt-2 text-right">
            <Link
              href="/forgot-password"
              className="rounded text-xs font-semibold text-primary-700 underline-offset-2 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600 dark:text-primary-300"
            >
              Lupa kata sandi?
            </Link>
          </div>
        </div>

        <Button
          type="submit"
          size="lg"
          fullWidth
          isLoading={isSubmitting}
          loadingText="Memproses..."
          rightIcon={<ArrowRight className="h-4 w-4" />}
        >
          Masuk
        </Button>
      </form>

      <p className="mt-6 text-center text-sm text-content-muted">
        Belum memiliki akun?{" "}
        <Link
          href="/register"
          className="rounded font-semibold text-primary-700 underline-offset-2 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600 dark:text-primary-300"
        >
          Daftar sekarang
        </Link>
      </p>

      <div className="mt-6 border-t border-line pt-5">
        <p className="flex items-start gap-2 text-xs leading-5 text-content-subtle">
          <Lock className="mt-0.5 h-3.5 w-3.5 shrink-0" aria-hidden="true" />
          Koneksi dilindungi enkripsi. Jangan bagikan kata sandi atau kode OTP kepada siapa pun.
        </p>
      </div>
    </div>
  );
}

