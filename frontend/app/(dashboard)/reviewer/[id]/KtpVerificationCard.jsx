"use client";

import { useEffect, useState } from "react";
import {
  AlertCircle,
  CheckCircle2,
  FileWarning,
  IdCard,
  Loader2,
  ShieldCheck,
} from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/Card";
import Badge from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import {
  useBorrowerDocument,
  useBorrowerProfileByUser,
  useVerifyBorrowerProfile,
} from "@/hooks/useBorrowerProfile";
import { formatDateTime, getApiErrorMessage } from "@/lib/utils";

function DocumentPreview({ userId, kind, label }) {
  const query = useBorrowerDocument(userId, kind);
  const [objectUrl, setObjectUrl] = useState(null);

  useEffect(() => {
    if (!query.data) {
      setObjectUrl(null);
      return undefined;
    }
    const url = URL.createObjectURL(query.data);
    setObjectUrl(url);
    return () => URL.revokeObjectURL(url);
  }, [query.data]);

  return (
    <div className="overflow-hidden rounded-xl border border-line bg-surface-subtle">
      <div className="flex items-center justify-between gap-2 border-b border-line bg-surface-raised px-3 py-2">
        <p className="text-xs font-semibold text-content">{label}</p>
        {objectUrl ? (
          <a
            href={objectUrl}
            target="_blank"
            rel="noreferrer"
            className="text-2xs font-semibold text-primary-700 underline-offset-2 hover:underline dark:text-primary-300"
          >
            Buka
          </a>
        ) : null}
      </div>
      {query.isLoading ? (
        <div className="flex h-36 items-center justify-center text-xs text-content-muted">
          <Loader2 className="mr-2 h-4 w-4 animate-spin" aria-hidden="true" />
          Memuat dokumen...
        </div>
      ) : objectUrl ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img
          src={objectUrl}
          alt={label}
          className="h-36 w-full bg-neutral-100 object-contain dark:bg-neutral-900"
        />
      ) : (
        <div className="flex h-36 flex-col items-center justify-center gap-1 text-xs text-content-muted">
          <FileWarning className="h-6 w-6 text-content-subtle" aria-hidden="true" />
          Dokumen belum diunggah
        </div>
      )}
    </div>
  );
}

export default function KtpVerificationCard({ borrowerId, canVerify = false }) {
  const profileQuery = useBorrowerProfileByUser(borrowerId);
  const verifyMutation = useVerifyBorrowerProfile(borrowerId);
  const [message, setMessage] = useState("");
  const [errorMessage, setErrorMessage] = useState("");

  const profile = profileQuery.data;
  const notFound = profileQuery.error?.response?.status === 404;
  const hasDocuments = Boolean(profile?.ktpPhotoUrl && profile?.selfiePhotoUrl);

  async function handleVerify() {
    setMessage("");
    setErrorMessage("");
    try {
      await verifyMutation.mutateAsync();
      setMessage("Dokumen borrower berhasil diverifikasi.");
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, "Verifikasi dokumen belum dapat diproses."));
    }
  }

  return (
    <Card>
      <CardHeader className="flex-row items-start justify-between gap-3">
        <div>
          <CardTitle>Dokumen pendukung</CardTitle>
          <CardDescription>Foto KTP dan selfie yang diunggah peminjam.</CardDescription>
        </div>
        <Badge variant={profile?.isVerified ? "success" : "warning"} dot>
          {profile?.isVerified ? "Terverifikasi" : "Belum terverifikasi"}
        </Badge>
      </CardHeader>
      <CardContent className="space-y-4">
        {message ? (
          <div
            role="status"
            className="flex items-start gap-2 rounded-xl border border-success-200 bg-success-50 px-3.5 py-3 text-sm text-success-700 dark:border-success-700/40 dark:bg-success-700/15 dark:text-success-200"
          >
            <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
            {message}
          </div>
        ) : null}
        {errorMessage ? (
          <div
            role="alert"
            className="flex items-start gap-2 rounded-xl border border-danger-200 bg-danger-50 px-3.5 py-3 text-sm text-danger-700 dark:border-danger-700/40 dark:bg-danger-700/15 dark:text-danger-200"
          >
            <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
            {errorMessage}
          </div>
        ) : null}

        {profileQuery.isLoading ? (
          <div className="flex h-36 items-center justify-center text-sm text-content-muted">
            <Loader2 className="mr-2 h-4 w-4 animate-spin" aria-hidden="true" />
            Memuat profil peminjam...
          </div>
        ) : notFound ? (
          <div className="rounded-xl border border-dashed border-line bg-surface-subtle px-4 py-5 text-sm text-content-muted">
            Peminjam belum mengisi profil usaha dan belum mengunggah dokumen identitas.
          </div>
        ) : profileQuery.isError ? (
          <div className="rounded-xl border border-danger-200 bg-danger-50 px-4 py-3 text-sm text-danger-700">
            Profil peminjam tidak dapat dimuat.
          </div>
        ) : (
          <>
            <dl className="grid gap-3 rounded-xl border border-line bg-surface-raised p-4 text-sm sm:grid-cols-2">
              <div>
                <dt className="text-xs text-content-muted">Nama lengkap</dt>
                <dd className="mt-0.5 font-medium text-content">{profile.fullName || "—"}</dd>
              </div>
              <div>
                <dt className="text-xs text-content-muted">NIK</dt>
                <dd className="numeric mt-0.5 font-medium text-content">{profile.nik || "—"}</dd>
              </div>
              <div>
                <dt className="text-xs text-content-muted">Usaha</dt>
                <dd className="mt-0.5 font-medium text-content">{profile.businessName || "—"}</dd>
              </div>
              <div>
                <dt className="text-xs text-content-muted">Diverifikasi pada</dt>
                <dd className="mt-0.5 font-medium text-content">
                  {profile.verifiedAt ? formatDateTime(profile.verifiedAt) : "Belum diverifikasi"}
                </dd>
              </div>
            </dl>

            <div className="grid gap-3 sm:grid-cols-2">
              <DocumentPreview userId={borrowerId} kind="ktp" label="Foto KTP" />
              <DocumentPreview userId={borrowerId} kind="selfie" label="Selfie dengan KTP" />
            </div>

            <div className="flex flex-wrap items-center justify-between gap-3">
              <p className="flex items-start gap-1.5 text-2xs leading-5 text-content-subtle">
                <ShieldCheck className="mt-0.5 h-3.5 w-3.5 shrink-0" aria-hidden="true" />
                Pastikan foto KTP jelas dan wajah pada selfie sesuai dengan data profil.
              </p>
              <Button
                type="button"
                variant={profile.isVerified ? "secondary" : "primary"}
                onClick={handleVerify}
                disabled={!canVerify || !hasDocuments || profile.isVerified}
                isLoading={verifyMutation.isPending}
                loadingText="Memverifikasi..."
                leftIcon={<IdCard className="h-4 w-4" />}
              >
                {profile.isVerified ? "Sudah terverifikasi" : "Verifikasi dokumen"}
              </Button>
            </div>
            {!hasDocuments ? (
              <p className="text-xs text-warning-700 dark:text-warning-300">
                Tombol verifikasi aktif setelah KTP dan selfie diunggah.
              </p>
            ) : null}
          </>
        )}
      </CardContent>
    </Card>
  );
}
