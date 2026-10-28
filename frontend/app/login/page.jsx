"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowRight, Eye, EyeOff, Loader2 } from "lucide-react";
import AuthShell from "@/components/auth/AuthShell";
import { useAuth } from "@/hooks/useAuth";
import { getApiErrorMessage, safeRedirectPath } from "@/lib/utils";

function GoogleIcon() {
  return (
    <span className="text-xl font-bold" aria-hidden="true">
      <span className="text-[#4285F4]">G</span>
    </span>
  );
}

export default function LoginPage() {
  const router = useRouter();
  const { login } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [sessionExpired, setSessionExpired] = useState(false);
  const [registered, setRegistered] = useState(false);
  const [formError, setFormError] = useState("");
  const [nextPath, setNextPath] = useState(null);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    setSessionExpired(params.get("expired") === "1");
    setRegistered(params.get("registered") === "1");
    const requestedEmail = params.get("email");
    if (requestedEmail) {
      setEmail(requestedEmail);
    }
    const requestedNext = params.get("next");
    if (requestedNext) {
      setNextPath(safeRedirectPath(requestedNext));
    }
  }, []);

  async function handleSubmit(event) {
    event.preventDefault();
    if (!email.trim() || !password) {
      setFormError("Email dan kata sandi wajib diisi.");
      return;
    }
    setFormError("");
    setIsLoading(true);
    try {
      const result = await login({ email: email.trim(), password });
      const roles = (result?.user?.roles || []).map((role) => String(role).toUpperCase());
      const roleHome = roles.includes("ADMIN")
        ? "/admin/dashboard"
        : roles.includes("REVIEWER")
          ? "/reviewer/queue"
          : "/dashboard";
      router.replace(nextPath || roleHome);
    } catch (error) {
      setFormError(getApiErrorMessage(error, "Email atau kata sandi salah."));
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <AuthShell active="login">
      <h1 className="mb-6 text-3xl font-bold leading-tight text-blue-600">
        Selamat Datang di
        <br />
        KoperasiDigital
      </h1>

      {sessionExpired ? (
        <p className="mb-4 rounded-lg border border-blue-200 bg-blue-50 px-3 py-2 text-xs text-blue-700">
          Sesi Anda telah berakhir. Silakan masuk kembali untuk melanjutkan.
        </p>
      ) : null}

      {registered ? (
        <p className="mb-4 rounded-lg border border-blue-200 bg-blue-50 px-3 py-2 text-xs text-blue-700">
          Akun berhasil dibuat. Silakan masuk untuk melanjutkan.
        </p>
      ) : null}

      {formError ? (
        <p role="alert" className="mb-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-xs text-red-600">
          {formError}
        </p>
      ) : null}

      <button
        type="button"
        onClick={() =>
          setFormError("Masuk dengan Google belum tersedia. Gunakan email dan kata sandi.")
        }
        className="flex h-14 w-full items-center justify-center gap-3 rounded-xl border border-gray-200 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50"
      >
        <GoogleIcon />
        Masuk dengan Google
      </button>

      <div className="my-5 flex items-center gap-4">
        <span className="h-px flex-1 bg-gray-200" />
        <span className="text-xs text-gray-400">atau</span>
        <span className="h-px flex-1 bg-gray-200" />
      </div>

      <form onSubmit={handleSubmit} noValidate>
        <input
          id="email"
          type="email"
          name="email"
          aria-label="Email"
          autoComplete="email"
          placeholder="Email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          className="mb-3 h-14 w-full rounded-xl border border-gray-200 px-4 text-sm text-gray-900 placeholder:text-gray-400 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
        />

        <div className="relative">
          <input
            id="password"
            type={showPassword ? "text" : "password"}
            name="password"
            aria-label="Kata Sandi"
            autoComplete="current-password"
            placeholder="Kata Sandi"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            className="h-14 w-full rounded-xl border border-gray-200 px-4 pr-12 text-sm text-gray-900 placeholder:text-gray-400 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
          <button
            type="button"
            onClick={() => setShowPassword((visible) => !visible)}
            className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
            aria-label={showPassword ? "Sembunyikan kata sandi" : "Tampilkan kata sandi"}
          >
            {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
          </button>
        </div>

        <div className="mb-5 mt-3 flex items-center justify-between">
          <label className="flex cursor-pointer items-center gap-2 text-sm text-gray-600">
            <input type="checkbox" className="h-4 w-4 cursor-pointer accent-blue-600" />
            Ingat Saya
          </label>
          <Link
            href="/forgot-password"
            className="text-sm text-blue-600 transition-colors hover:text-blue-700"
          >
            Lupa Kata Sandi?
          </Link>
        </div>

        <button
          type="submit"
          disabled={isLoading}
          className="flex h-14 w-full items-center justify-center gap-2 rounded-xl bg-blue-600 text-base font-semibold text-white transition-colors hover:bg-blue-800 disabled:opacity-70"
        >
          {isLoading ? (
            <>
              <Loader2 size={16} className="animate-spin" aria-hidden="true" />
              Memproses...
            </>
          ) : (
            <>
              Masuk
              <ArrowRight size={16} aria-hidden="true" />
            </>
          )}
        </button>
      </form>

      <p className="mt-5 text-center text-sm text-gray-500">
        Tidak punya akun?{" "}
        <Link
          href="/register"
          className="font-semibold text-blue-600 transition-colors hover:text-blue-700"
        >
          Buat Akun
        </Link>
      </p>
    </AuthShell>
  );
}
