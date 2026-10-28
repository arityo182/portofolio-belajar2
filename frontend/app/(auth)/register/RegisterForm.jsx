"use client";

import { useRef, useState } from "react";
import Link from "next/link";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { AlertCircle, CheckCircle2, Loader2 } from "lucide-react";
import AuthShell from "@/components/auth/AuthShell";
import { registerSchema, getPasswordStrength } from "@/lib/validations/auth";
import { cn, getApiErrorMessage, maskEmail } from "@/lib/utils";
import { useAuth } from "@/hooks/useAuth";

const STEPS = [
  { id: 1, title: "Data Akun", description: "Email dan nomor WhatsApp" },
  { id: 2, title: "Keamanan", description: "Peran dan kata sandi" },
];

const ROLE_OPTIONS = [
  {
    value: "BORROWER",
    title: "Peminjam",
    description: "Ajukan modal usaha untuk UMKM Anda.",
  },
  {
    value: "INVESTOR",
    title: "Investor",
    description: "Danai UMKM pilihan dan pantau portofolio.",
  },
];

const INPUT_CLASS =
  "h-14 w-full rounded-xl border border-gray-200 px-4 text-sm text-gray-900 placeholder:text-gray-400 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500";

function GoogleIcon() {
  return (
    <span className="text-xl font-bold" aria-hidden="true">
      <span className="text-[#4285F4]">G</span>
    </span>
  );
}

function StepIndicator({ currentStep }) {
  return (
    <div className="mb-6 flex items-center" aria-label="Tahapan pendaftaran">
      {STEPS.map((step, index) => {
        const isActive = currentStep === step.id;
        const isComplete = currentStep > step.id;
        return (
          <div key={step.id} className="flex flex-1 items-center">
            <span
              className={cn(
                "flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-xs font-bold",
                isActive || isComplete
                  ? "bg-blue-600 text-white"
                  : "bg-gray-100 text-gray-400",
              )}
            >
              {isComplete ? <CheckCircle2 size={14} aria-hidden="true" /> : step.id}
            </span>
            <span
              className={cn(
                "ml-2 text-sm",
                isActive ? "font-medium text-gray-900" : "text-gray-400",
              )}
            >
              {step.title}
            </span>
            {index < STEPS.length - 1 ? (
              <span className="mx-4 h-px flex-1 bg-gray-200" aria-hidden="true" />
            ) : null}
          </div>
        );
      })}
    </div>
  );
}

export default function RegisterForm() {
  const { register: registerAccount } = useAuth();
  const [step, setStep] = useState(1);
  const [formError, setFormError] = useState("");
  const [submittedEmail, setSubmittedEmail] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const errorRef = useRef(null);

  const {
    register,
    handleSubmit,
    trigger,
    watch,
    reset,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(registerSchema),
    mode: "onTouched",
    defaultValues: {
      email: "",
      phone: "",
      password: "",
      confirmPassword: "",
      role: "BORROWER",
      acceptTerms: false,
    },
  });

  const password = watch("password") || "";
  const strength = getPasswordStrength(password);
  const selectedRole = watch("role");

  const strengthTone =
    strength.score <= 2
      ? "bg-red-500"
      : strength.score === 3
        ? "bg-amber-500"
        : "bg-emerald-500";

  async function goToNextStep() {
    setFormError("");
    const valid = await trigger(["email", "phone"]);
    if (valid) {
      setStep(2);
    }
  }

  async function onSubmit(values) {
    setFormError("");
    setIsSubmitting(true);
    try {
      await registerAccount({
        email: values.email.trim(),
        phone: values.phone.trim(),
        password: values.password,
        role: values.role,
      });
      setSubmittedEmail(values.email.trim());
      window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (error) {
      setFormError(getApiErrorMessage(error, "Registrasi gagal. Periksa kembali data Anda."));
      setTimeout(() => errorRef.current?.focus(), 0);
    } finally {
      setIsSubmitting(false);
    }
  }

  if (submittedEmail) {
    return (
      <AuthShell active="register">
        <div className="text-center">
          <CheckCircle2 size={48} className="mx-auto mb-4 text-blue-600" aria-hidden="true" />
          <h1 className="mb-2 text-3xl font-bold text-blue-600">Akun Berhasil Dibuat</h1>
          <p className="mb-6 text-sm leading-relaxed text-gray-500">
            Akun <span className="font-medium text-gray-700">{maskEmail(submittedEmail)}</span> sudah
            terdaftar. Masuk untuk melengkapi profil dan mulai menggunakan layanan koperasi.
          </p>
          <Link
            href={`/login?registered=1&email=${encodeURIComponent(submittedEmail)}`}
            className="flex h-14 w-full items-center justify-center rounded-xl bg-blue-600 text-base font-semibold text-white transition-colors hover:bg-blue-800"
          >
            Masuk ke Akun
          </Link>
          <button
            type="button"
            onClick={() => {
              setSubmittedEmail("");
              setStep(1);
              reset();
            }}
            className="mt-3 h-14 w-full rounded-xl border border-blue-600 text-base font-semibold text-blue-600 transition-colors hover:bg-blue-50"
          >
            Daftar dengan Email Lain
          </button>
        </div>
      </AuthShell>
    );
  }

  return (
    <AuthShell active="register">
      <h1 className="mb-2 text-3xl font-bold text-blue-600">Buat Akun Baru</h1>
      <p className="mb-6 text-sm text-gray-500">
        Bergabung dengan ribuan UMKM yang sudah mendapat pendanaan.
      </p>

      <button
        type="button"
        onClick={() =>
          setFormError("Pendaftaran dengan Google belum tersedia. Gunakan email dan nomor WhatsApp.")
        }
        className="flex h-14 w-full items-center justify-center gap-3 rounded-xl border border-gray-200 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50"
      >
        <GoogleIcon />
        Daftar dengan Google
      </button>

      <div className="my-5 flex items-center gap-4">
        <span className="h-px flex-1 bg-gray-200" />
        <span className="text-xs text-gray-400">atau</span>
        <span className="h-px flex-1 bg-gray-200" />
      </div>

      <StepIndicator currentStep={step} />

      {formError ? (
        <div
          ref={errorRef}
          tabIndex={-1}
          role="alert"
          className="mb-4 flex items-start gap-2 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-xs text-red-600 focus:outline-none"
        >
          <AlertCircle size={14} className="mt-0.5 shrink-0" aria-hidden="true" />
          <span>{formError}</span>
        </div>
      ) : null}

      <form
        onSubmit={(event) => {
          event.preventDefault();
          if (step === 1) {
            goToNextStep();
            return;
          }
          handleSubmit(onSubmit)(event);
        }}
        method="post"
        noValidate
      >
        <div className={step === 1 ? "space-y-1" : "hidden"} aria-hidden={step !== 1}>
          <label className="sr-only" htmlFor="email">
            Email
          </label>
          <input
            id="email"
            required
            type="email"
            autoComplete="email"
            placeholder="Email"
            className={INPUT_CLASS}
            {...register("email")}
          />
          {errors.email?.message ? (
            <p role="alert" className="mt-1 text-xs text-red-500">
              {errors.email.message}
            </p>
          ) : (
            <p className="mb-3 mt-1 text-xs text-gray-400">Gunakan email aktif Anda.</p>
          )}

          <label className="sr-only" htmlFor="whatsapp">
            WhatsApp
          </label>
          <input
            id="whatsapp"
            required
            type="tel"
            autoComplete="tel"
            placeholder="+62"
            className={INPUT_CLASS}
            {...register("phone")}
          />
          {errors.phone?.message ? (
            <p role="alert" className="mt-1 text-xs text-red-500">
              {errors.phone.message}
            </p>
          ) : (
            <p className="mb-5 mt-1 text-xs text-gray-400">Nomor WhatsApp untuk verifikasi.</p>
          )}

          <button
            type="button"
            onClick={goToNextStep}
            className="h-14 w-full rounded-xl bg-blue-600 text-base font-semibold text-white transition-colors hover:bg-blue-800"
          >
            Lanjutkan →
          </button>
        </div>

        <div className={step === 2 ? "space-y-4" : "hidden"} aria-hidden={step !== 2}>
          <div>
            <label className="sr-only" htmlFor="password">
              Kata sandi
            </label>
            <input
              id="password"
              type="password"
              autoComplete="new-password"
              placeholder="Kata Sandi"
              className={INPUT_CLASS}
              {...register("password")}
            />
            <div className="mt-2 flex items-center gap-3">
              <div className="flex flex-1 gap-1" aria-hidden="true">
                {Array.from({ length: 5 }).map((_, index) => (
                  <span
                    key={index}
                    className={cn(
                      "h-1 flex-1 rounded-full",
                      index < strength.score ? strengthTone : "bg-gray-200",
                    )}
                  />
                ))}
              </div>
              <span className="text-2xs font-medium text-gray-400">
                {password ? strength.label : "Belum diisi"}
              </span>
            </div>
            {errors.password?.message ? (
              <p role="alert" className="mt-1 text-xs text-red-500">
                {errors.password.message}
              </p>
            ) : (
              <p className="mt-1 text-xs text-gray-400">
                Minimal 12 karakter, memuat huruf besar, huruf kecil, angka, dan simbol.
              </p>
            )}
          </div>

          <div>
            <label className="sr-only" htmlFor="confirmPassword">
              Konfirmasi kata sandi
            </label>
            <input
              id="confirmPassword"
              type="password"
              autoComplete="new-password"
              placeholder="Konfirmasi Kata Sandi"
              className={INPUT_CLASS}
              {...register("confirmPassword")}
            />
            {errors.confirmPassword?.message ? (
              <p role="alert" className="mt-1 text-xs text-red-500">
                {errors.confirmPassword.message}
              </p>
            ) : null}
          </div>

          <fieldset>
            <legend className="mb-2 text-sm font-medium text-gray-700">Pilih peran akun</legend>
            <div className="grid gap-2 sm:grid-cols-2">
              {ROLE_OPTIONS.map((option) => {
                const active = selectedRole === option.value;
                return (
                  <label
                    key={option.value}
                    className={cn(
                      "cursor-pointer rounded-xl border p-3 transition-colors",
                      active
                        ? "border-blue-600 bg-blue-50"
                        : "border-gray-200 bg-white hover:bg-gray-50",
                    )}
                  >
                    <input
                      type="radio"
                      value={option.value}
                      className="sr-only"
                      {...register("role")}
                    />
                    <span className="block text-sm font-semibold text-gray-900">
                      {option.title}
                    </span>
                    <span className="mt-0.5 block text-xs leading-relaxed text-gray-500">
                      {option.description}
                    </span>
                  </label>
                );
              })}
            </div>
            {errors.role?.message ? (
              <p role="alert" className="mt-1 text-xs text-red-500">
                {errors.role.message}
              </p>
            ) : null}
          </fieldset>

          <div>
            <label className="flex cursor-pointer items-start gap-2.5 text-xs leading-relaxed text-gray-500">
              <input
                type="checkbox"
                className="mt-0.5 h-4 w-4 shrink-0 rounded border-gray-300 accent-blue-600"
                {...register("acceptTerms")}
              />
              <span>
                Saya menyetujui{" "}
                <Link href="/legal/terms" className="font-medium text-blue-600 hover:text-blue-700">
                  Syarat &amp; Ketentuan
                </Link>{" "}
                dan{" "}
                <Link href="/legal/privacy" className="font-medium text-blue-600 hover:text-blue-700">
                  Kebijakan Privasi
                </Link>{" "}
                Koperasi Digital.
              </span>
            </label>
            {errors.acceptTerms?.message ? (
              <p role="alert" className="mt-1 text-xs text-red-500">
                {errors.acceptTerms.message}
              </p>
            ) : null}
          </div>

          <div className="flex gap-2">
            <button
              type="button"
              onClick={() => setStep(1)}
              className="h-14 rounded-xl border border-blue-600 px-5 text-base font-semibold text-blue-600 transition-colors hover:bg-blue-50"
            >
              Kembali
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="flex h-14 flex-1 items-center justify-center gap-2 rounded-xl bg-blue-600 text-base font-semibold text-white transition-colors hover:bg-blue-800 disabled:opacity-70"
            >
              {isSubmitting ? (
                <>
                  <Loader2 size={16} className="animate-spin" aria-hidden="true" />
                  Membuat akun...
                </>
              ) : (
                "Buat Akun"
              )}
            </button>
          </div>
        </div>
      </form>

      <p className="mt-5 text-center text-sm text-gray-500">
        Sudah punya akun?{" "}
        <Link
          href="/login"
          className="font-semibold text-blue-600 transition-colors hover:text-blue-700"
        >
          Masuk
        </Link>
      </p>
    </AuthShell>
  );
}
