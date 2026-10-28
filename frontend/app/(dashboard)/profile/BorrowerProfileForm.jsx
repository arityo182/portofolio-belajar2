"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { AlertCircle, CheckCircle2, FileUp, IdCard, Loader2 } from "lucide-react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/Card";
import Badge from "@/components/ui/Badge";
import Input from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import {
  useBorrowerProfile,
  useSaveBorrowerProfile,
  useUploadBorrowerDocuments,
} from "@/hooks/useBorrowerProfile";
import { getApiErrorMessage } from "@/lib/utils";

const profileSchema = z.object({
  nik: z.string().trim().regex(/^[\d*]{16}$/, "NIK harus 16 digit"),
  npwp: z
    .string()
    .trim()
    .optional()
    .refine((value) => !value || /^[\d*]{15,16}$/.test(value), "NPWP harus 15-16 digit"),
  fullName: z.string().trim().min(1, "Nama lengkap wajib diisi").max(255),
  dateOfBirth: z.string().min(1, "Tanggal lahir wajib diisi"),
  gender: z.enum(["M", "F"], { error: "Pilih jenis kelamin" }),
  address: z.string().trim().min(1, "Alamat wajib diisi").max(500),
  city: z.string().trim().min(1, "Kota wajib diisi").max(100),
  province: z.string().trim().min(1, "Provinsi wajib diisi").max(100),
  bankName: z.string().trim().min(1, "Nama bank wajib diisi").max(100),
  bankAccount: z
    .string()
    .trim()
    .regex(/^[\d*]{4,32}$/, "Nomor rekening harus berupa digit"),
  incomeMonthly: z.coerce.number().min(0, "Pendapatan tidak boleh negatif"),
  businessType: z.string().trim().min(1, "Jenis usaha wajib diisi").max(100),
  businessName: z.string().trim().min(1, "Nama usaha wajib diisi").max(255),
});

const GENDERS = [
  { value: "M", label: "Laki-laki" },
  { value: "F", label: "Perempuan" },
];

const BUSINESS_TYPES = ["KULINER", "RETAIL", "JASA", "PERTANIAN", "LAINNYA"];

function defaultsFor(profile) {
  return {
    nik: profile?.nik || "",
    npwp: profile?.npwp || "",
    fullName: profile?.fullName || "",
    dateOfBirth: profile?.dateOfBirth || "",
    gender: profile?.gender || "M",
    address: profile?.address || "",
    city: profile?.city || "",
    province: profile?.province || "",
    bankName: profile?.bankName || "",
    bankAccount: profile?.bankAccount || "",
    incomeMonthly: profile?.incomeMonthly ?? 0,
    businessType: profile?.businessType || "KULINER",
    businessName: profile?.businessName || "",
  };
}

export default function BorrowerProfileForm() {
  const profileQuery = useBorrowerProfile();
  const saveMutation = useSaveBorrowerProfile();
  const uploadMutation = useUploadBorrowerDocuments();
  const [message, setMessage] = useState("");
  const [formError, setFormError] = useState("");
  const [ktpFile, setKtpFile] = useState(null);
  const [selfieFile, setSelfieFile] = useState(null);
  const [uploadError, setUploadError] = useState("");
  const ktpRef = useRef(null);
  const selfieRef = useRef(null);

  const profile = profileQuery.data;
  const defaults = useMemo(() => defaultsFor(profile), [profile]);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(profileSchema),
    mode: "onTouched",
    defaultValues: defaults,
  });

  useEffect(() => {
    reset(defaults);
  }, [defaults, reset]);

  async function onSubmit(values) {
    setFormError("");
    setMessage("");
    try {
      await saveMutation.mutateAsync({
        ...values,
        npwp: values.npwp || null,
        incomeMonthly: Number(values.incomeMonthly),
      });
      setMessage("Profil usaha berhasil disimpan.");
    } catch (error) {
      setFormError(getApiErrorMessage(error, "Profil belum dapat disimpan."));
    }
  }

  async function uploadDocuments() {
    setUploadError("");
    setMessage("");
    if (!ktpFile || !selfieFile) {
      setUploadError("Pilih foto KTP dan selfie terlebih dahulu.");
      return;
    }
    try {
      await uploadMutation.mutateAsync({ ktpPhoto: ktpFile, selfiePhoto: selfieFile });
      setMessage("Dokumen berhasil diunggah dan menunggu verifikasi.");
      setKtpFile(null);
      setSelfieFile(null);
      if (ktpRef.current) ktpRef.current.value = "";
      if (selfieRef.current) selfieRef.current.value = "";
    } catch (error) {
      setUploadError(getApiErrorMessage(error, "Dokumen belum dapat diunggah."));
    }
  }

  if (profileQuery.isLoading) {
    return (
      <Card>
        <CardContent className="flex items-center justify-center py-10 text-sm text-content-muted">
          <Loader2 className="mr-2 h-4 w-4 animate-spin" aria-hidden="true" />
          Memuat profil usaha...
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader className="flex-row items-start justify-between gap-4">
        <div>
          <CardTitle>Profil usaha & verifikasi</CardTitle>
          <CardDescription>
            Data ini dipakai untuk penilaian kredit dan pencairan pinjaman.
          </CardDescription>
        </div>
        <Badge variant={profile?.isVerified ? "success" : "warning"} dot>
          {profile?.isVerified ? "Terverifikasi" : "Belum terverifikasi"}
        </Badge>
      </CardHeader>
      <CardContent className="space-y-6">
        {profileQuery.isError && profileQuery.error?.response?.status !== 404 ? (
          <div className="flex items-start gap-2 rounded-xl border border-red-200 bg-red-50 px-3.5 py-3 text-sm text-red-700">
            <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
            Profil usaha tidak dapat dimuat. Periksa koneksi lalu coba lagi.
          </div>
        ) : null}

        {message ? (
          <div
            role="status"
            className="flex items-start gap-2 rounded-xl border border-emerald-200 bg-emerald-50 px-3.5 py-3 text-sm text-emerald-700"
          >
            <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
            {message}
          </div>
        ) : null}
        {formError ? (
          <div role="alert" className="flex items-start gap-2 rounded-xl border border-red-200 bg-red-50 px-3.5 py-3 text-sm text-red-700">
            <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
            {formError}
          </div>
        ) : null}

        <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <Input label="Nama lengkap" required error={errors.fullName?.message} {...register("fullName")} />
            <Input
              label="NIK"
              required
              inputMode="numeric"
              helper="16 digit. Nilai bertopeng dipertahankan jika tidak diubah."
              error={errors.nik?.message}
              {...register("nik")}
            />
            <Input
              label="NPWP (opsional)"
              inputMode="numeric"
              error={errors.npwp?.message}
              {...register("npwp")}
            />
            <Input
              label="Tanggal lahir"
              type="date"
              required
              error={errors.dateOfBirth?.message}
              {...register("dateOfBirth")}
            />
            <div className="w-full">
              <label htmlFor="gender" className="mb-1.5 block text-sm font-medium text-content">
                Jenis kelamin
              </label>
              <select
                id="gender"
                className="h-11 w-full rounded-lg border border-line bg-surface-raised px-3.5 text-sm text-content shadow-sm outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-600/25"
                {...register("gender")}
              >
                {GENDERS.map((item) => (
                  <option key={item.value} value={item.value}>
                    {item.label}
                  </option>
                ))}
              </select>
              {errors.gender?.message ? (
                <p className="mt-1.5 text-xs text-danger-600">{errors.gender.message}</p>
              ) : null}
            </div>
            <Input
              label="Pendapatan bulanan"
              type="number"
              min="0"
              step="0.01"
              required
              error={errors.incomeMonthly?.message}
              {...register("incomeMonthly")}
            />
          </div>

          <Input label="Alamat" required error={errors.address?.message} {...register("address")} />
          <div className="grid gap-4 sm:grid-cols-2">
            <Input label="Kota" required error={errors.city?.message} {...register("city")} />
            <Input label="Provinsi" required error={errors.province?.message} {...register("province")} />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <Input label="Nama bank" required error={errors.bankName?.message} {...register("bankName")} />
            <Input
              label="Nomor rekening"
              required
              inputMode="numeric"
              helper="Nilai bertopeng dipertahankan jika tidak diubah."
              error={errors.bankAccount?.message}
              {...register("bankAccount")}
            />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div className="w-full">
              <label htmlFor="businessType" className="mb-1.5 block text-sm font-medium text-content">
                Jenis usaha
              </label>
              <select
                id="businessType"
                className="h-11 w-full rounded-lg border border-line bg-surface-raised px-3.5 text-sm text-content shadow-sm outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-600/25"
                {...register("businessType")}
              >
                {BUSINESS_TYPES.map((type) => (
                  <option key={type} value={type}>
                    {type.charAt(0) + type.slice(1).toLowerCase()}
                  </option>
                ))}
              </select>
            </div>
            <Input label="Nama usaha" required error={errors.businessName?.message} {...register("businessName")} />
          </div>

          <div className="flex justify-end">
            <Button type="submit" isLoading={isSubmitting || saveMutation.isPending} loadingText="Menyimpan...">
              Simpan profil usaha
            </Button>
          </div>
        </form>

        <div className="rounded-xl border border-line bg-surface-subtle p-4">
          <div className="flex items-start gap-3">
            <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-surface-raised text-primary-700 dark:text-primary-300">
              <IdCard className="h-5 w-5" aria-hidden="true" />
            </span>
            <div className="min-w-0 flex-1">
              <p className="text-sm font-semibold text-content">Dokumen verifikasi KTP</p>
              <p className="mt-1 text-xs leading-5 text-content-muted">
                Unggah foto KTP dan selfie dengan KTP. Format JPEG/PNG, maksimal 5MB per file.
              </p>
              {profile?.ktpPhotoUrl ? (
                <div className="mt-2 flex flex-wrap gap-3 text-xs">
                  <a className="font-semibold text-primary-700 underline" href={profile.ktpPhotoUrl} target="_blank" rel="noreferrer">
                    Lihat KTP
                  </a>
                  <a className="font-semibold text-primary-700 underline" href={profile.selfiePhotoUrl} target="_blank" rel="noreferrer">
                    Lihat selfie
                  </a>
                </div>
              ) : null}
              <div className="mt-3 grid gap-3 sm:grid-cols-2">
                <label className="flex cursor-pointer items-center gap-2 rounded-lg border border-dashed border-line bg-surface-raised px-3 py-2.5 text-xs text-content-muted">
                  <FileUp className="h-4 w-4" aria-hidden="true" />
                  <span className="truncate">{ktpFile ? ktpFile.name : "Pilih foto KTP"}</span>
                  <input
                    ref={ktpRef}
                    type="file"
                    accept="image/jpeg,image/png"
                    className="sr-only"
                    onChange={(event) => setKtpFile(event.target.files?.[0] || null)}
                  />
                </label>
                <label className="flex cursor-pointer items-center gap-2 rounded-lg border border-dashed border-line bg-surface-raised px-3 py-2.5 text-xs text-content-muted">
                  <FileUp className="h-4 w-4" aria-hidden="true" />
                  <span className="truncate">{selfieFile ? selfieFile.name : "Pilih foto selfie"}</span>
                  <input
                    ref={selfieRef}
                    type="file"
                    accept="image/jpeg,image/png"
                    className="sr-only"
                    onChange={(event) => setSelfieFile(event.target.files?.[0] || null)}
                  />
                </label>
              </div>
              {uploadError ? <p className="mt-2 text-xs text-danger-600">{uploadError}</p> : null}
              <div className="mt-3">
                <Button
                  type="button"
                  variant="secondary"
                  size="sm"
                  onClick={uploadDocuments}
                  isLoading={uploadMutation.isPending}
                  loadingText="Mengunggah..."
                >
                  Unggah dokumen
                </Button>
              </div>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
