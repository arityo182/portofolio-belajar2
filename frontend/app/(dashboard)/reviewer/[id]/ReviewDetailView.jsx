"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import {
  AlertCircle,
  ArrowLeft,
  CheckCircle2,
  ShieldCheck,
  XCircle,
} from "lucide-react";
import PageHeader from "@/components/ui/PageHeader";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/Card";
import Badge from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { buttonStyles } from "@/lib/buttonStyles";
import Textarea from "@/components/ui/Textarea";
import Select from "@/components/ui/Select";
import LoadingSkeleton from "@/components/ui/LoadingSkeleton";
import EmptyState from "@/components/ui/EmptyState";
import ScoreGauge from "@/components/ScoreGauge";
import RiskBadge from "@/components/RiskBadge";
import ConfirmModal from "@/components/ConfirmModal";
import LoanTimeline from "@/components/loans/LoanTimeline";
import KtpVerificationCard from "./KtpVerificationCard";
import { useReviewApplication, useReviewDecision } from "@/hooks/useReviewer";
import {
  getAccountStatusLabel,
  getLoanStatusLabel,
  getRoleLabel,
} from "@/lib/constants";
import { formatCurrency, formatDateShort, formatDateTime, getApiErrorMessage } from "@/lib/utils";

const REJECTION_REASONS = [
  "Risiko kredit terlalu tinggi",
  "Kapasitas pembayaran tidak memadai",
  "Dokumen tidak valid atau tidak lengkap",
  "Data pengajuan tidak sesuai",
  "Tujuan pinjaman tidak sesuai ketentuan",
  "Lainnya",
];

const RECOMMENDATION_LABELS = {
  APPROVE: "Disarankan disetujui",
  REVIEW: "Perlu review manual",
  REJECT: "Disarankan ditolak",
};

function formatFeatureLabel(key) {
  return String(key)
    .replace(/[_-]+/g, " ")
    .replace(/\b\w/g, (character) => character.toUpperCase());
}

function formatFeatureValue(value) {
  const number = Number(value);
  if (!Number.isFinite(number)) {
    return String(value ?? "—");
  }
  return Number.isInteger(number) ? String(number) : number.toFixed(2);
}

export default function ReviewDetailView({ loanId }) {
  const { applicationQuery, scoreQuery, borrowerQuery, application, score, borrower, isLoading } =
    useReviewApplication(loanId);
  const decision = useReviewDecision();

  const [action, setAction] = useState("approve");
  const [notes, setNotes] = useState("");
  const [reason, setReason] = useState(REJECTION_REASONS[0]);
  const [overrideScore, setOverrideScore] = useState(false);
  const [formError, setFormError] = useState("");
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [successMessage, setSuccessMessage] = useState("");

  const features = useMemo(() => {
    if (!score?.features) {
      return [];
    }
    return Object.entries(score.features)
      .filter(([, value]) => Number.isFinite(Number(value)))
      .sort((left, right) => Number(right[1]) - Number(left[1]))
      .slice(0, 6);
  }, [score]);

  const canReview = application?.status === "UNDER_REVIEW";

  function openConfirmation() {
    if (action === "reject" && !reason.trim()) {
      setFormError("Pilih alasan penolakan terlebih dahulu.");
      return;
    }
    setFormError("");
    setConfirmOpen(true);
  }

  async function submitDecision() {
    setFormError("");
    try {
      await decision.mutateAsync({
        loanId,
        payload: {
          status: action === "approve" ? "APPROVED" : "REJECTED",
          rejectionReason: action === "reject" ? reason.trim() : null,
          overrideScore: action === "approve" ? overrideScore : false,
          notes: notes.trim() || null,
          version: application.version,
        },
      });
      setConfirmOpen(false);
      setSuccessMessage(
        action === "approve"
          ? "Pengajuan berhasil disetujui."
          : "Pengajuan berhasil ditolak.",
      );
    } catch (error) {
      setConfirmOpen(false);
      setFormError(
        getApiErrorMessage(error, "Keputusan review gagal diproses. Muat ulang data lalu coba lagi."),
      );
    }
  }

  if (isLoading) {
    return <LoadingSkeleton variant="page" />;
  }

  if (applicationQuery.isError) {
    const status = applicationQuery.error?.response?.status;
    return (
      <EmptyState
        icon={AlertCircle}
        title={status === 403 ? "Tidak memiliki akses" : "Pengajuan tidak ditemukan"}
        description={
          status === 403
            ? "Akun Anda tidak berwenang melihat detail pengajuan ini."
            : "Data pengajuan tidak tersedia atau telah dihapus."
        }
        action={
          <Link href="/reviewer/queue" className={buttonStyles()}>
            Kembali ke antrian
          </Link>
        }
      />
    );
  }

  return (
    <div className="space-y-6">
      <Link
        href="/reviewer/queue"
        className="inline-flex items-center gap-1.5 rounded text-xs font-semibold text-content-muted underline-offset-2 hover:text-content hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600"
      >
        <ArrowLeft className="h-3.5 w-3.5" aria-hidden="true" />
        Antrian review
      </Link>

      <PageHeader
        eyebrow="Review pengajuan"
        title={application.loanProductName || "Pinjaman"}
        description={`ID pengajuan #${String(application.loanId).slice(0, 8).toUpperCase()}`}
        actions={<Badge status={application.status} size="md" dot />}
      />

      {successMessage ? (
        <div
          role="status"
          className="flex items-center gap-2.5 rounded-2xl border border-success-200 bg-success-50 px-4 py-3.5 text-sm font-medium text-success-700 dark:border-success-700/40 dark:bg-success-700/15 dark:text-success-200"
        >
          <CheckCircle2 className="h-4 w-4 shrink-0" aria-hidden="true" />
          {successMessage}
        </div>
      ) : null}

      {formError ? (
        <div
          role="alert"
          className="flex items-start gap-2.5 rounded-xl border border-danger-200 bg-danger-50 px-3.5 py-3 text-sm leading-6 text-danger-700 dark:border-danger-700/40 dark:bg-danger-700/15 dark:text-danger-200"
        >
          <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
          {formError}
        </div>
      ) : null}

      <div className="grid gap-6 xl:grid-cols-[1.5fr_1fr]">
        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>Ringkasan pengajuan</CardTitle>
              <CardDescription>Data pinjaman yang diajukan peminjam.</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="flex flex-wrap items-end justify-between gap-3">
                <div>
                  <p className="text-xs text-content-muted">Nominal diajukan</p>
                  <p className="numeric mt-1 text-3xl font-semibold text-content">
                    {formatCurrency(application.amount)}
                  </p>
                </div>
                <Badge status={application.status} size="md" dot />
              </div>
              <dl className="mt-5 grid gap-x-8 border-t border-line pt-4 sm:grid-cols-2">
                <div className="flex justify-between gap-4 py-2 text-sm">
                  <dt className="text-content-muted">Tenor</dt>
                  <dd className="numeric font-medium text-content">{application.tenorMonths} bulan</dd>
                </div>
                <div className="flex justify-between gap-4 py-2 text-sm">
                  <dt className="text-content-muted">Diajukan</dt>
                  <dd className="text-content">{formatDateShort(application.submittedAt)}</dd>
                </div>
                <div className="flex justify-between gap-4 py-2 text-sm sm:col-span-2">
                  <dt className="text-content-muted">Tujuan</dt>
                  <dd className="max-w-[70%] text-right font-medium text-content">
                    {application.purpose}
                  </dd>
                </div>
              </dl>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Informasi peminjam</CardTitle>
              <CardDescription>Data akun peminjam yang terhubung dengan pengajuan.</CardDescription>
            </CardHeader>
            <CardContent>
              {borrowerQuery.isLoading ? (
                <div className="skeleton h-20 w-full" />
              ) : borrower ? (
                <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <p className="text-sm font-semibold text-content">{borrower.email}</p>
                    <p className="mt-0.5 text-xs text-content-muted">
                      {borrower.phone || "Nomor telepon belum tersedia"}
                    </p>
                    <p className="numeric mt-1 text-2xs text-content-subtle">
                      ID #{String(borrower.userId).slice(0, 8).toUpperCase()}
                    </p>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <Badge variant="neutral">{getAccountStatusLabel(borrower.status)}</Badge>
                    {(borrower.roles || []).map((role) => (
                      <Badge key={role} variant="primary">
                        {getRoleLabel(role)}
                      </Badge>
                    ))}
                  </div>
                </div>
              ) : (
                <p className="text-sm text-content-muted">
                  Data profil peminjam tidak dapat dimuat. Gunakan ID peminjam{" "}
                  <span className="numeric">{application.borrowerId}</span>.
                </p>
              )}
            </CardContent>
          </Card>

          <KtpVerificationCard borrowerId={application.borrowerId} canVerify={canReview} />

          {canReview ? (
            <Card>
              <CardHeader>
                <CardTitle>Keputusan review</CardTitle>
                <CardDescription>
                  Pilih tindakan, tambahkan catatan, lalu konfirmasi melalui dialog.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div role="tablist" aria-label="Jenis keputusan" className="flex gap-2">
                  <button
                    type="button"
                    role="tab"
                    aria-selected={action === "approve"}
                    onClick={() => setAction("approve")}
                    className={
                      action === "approve"
                        ? "inline-flex items-center gap-1.5 rounded-xl border border-success-600 bg-success-50 px-3.5 py-2 text-sm font-semibold text-success-700 dark:bg-success-700/15 dark:text-success-200"
                        : "inline-flex items-center gap-1.5 rounded-xl border border-line bg-surface-raised px-3.5 py-2 text-sm font-medium text-content-muted hover:bg-surface-subtle"
                    }
                  >
                    <CheckCircle2 className="h-4 w-4" aria-hidden="true" />
                    Setujui
                  </button>
                  <button
                    type="button"
                    role="tab"
                    aria-selected={action === "reject"}
                    onClick={() => setAction("reject")}
                    className={
                      action === "reject"
                        ? "inline-flex items-center gap-1.5 rounded-xl border border-danger-600 bg-danger-50 px-3.5 py-2 text-sm font-semibold text-danger-700 dark:bg-danger-700/15 dark:text-danger-200"
                        : "inline-flex items-center gap-1.5 rounded-xl border border-line bg-surface-raised px-3.5 py-2 text-sm font-medium text-content-muted hover:bg-surface-subtle"
                    }
                  >
                    <XCircle className="h-4 w-4" aria-hidden="true" />
                    Tolak
                  </button>
                </div>

                {action === "reject" ? (
                  <Select
                    label="Alasan penolakan"
                    value={reason}
                    onChange={(event) => setReason(event.target.value)}
                    required
                  >
                    {REJECTION_REASONS.map((option) => (
                      <option key={option} value={option}>
                        {option}
                      </option>
                    ))}
                  </Select>
                ) : (
                  <label className="flex items-start gap-2.5 rounded-xl border border-line bg-surface-raised p-3.5">
                    <input
                      type="checkbox"
                      checked={overrideScore}
                      onChange={(event) => setOverrideScore(event.target.checked)}
                      className="mt-0.5 h-4 w-4 rounded border-neutral-300 text-primary-600 focus:ring-2 focus:ring-primary-600 dark:border-neutral-600 dark:bg-neutral-800"
                    />
                    <span className="text-xs leading-5 text-content-muted">
                      Override rekomendasi skor AI. Gunakan hanya jika Anda memiliki bukti
                      tambahan yang tidak tercakup model.
                    </span>
                  </label>
                )}

                <Textarea
                  label="Catatan reviewer"
                  value={notes}
                  onChange={(event) => setNotes(event.target.value)}
                  maxLength={1000}
                  showCounter
                  rows={3}
                  placeholder="Tambahkan pertimbangan atau catatan internal..."
                  helper="Catatan disimpan bersama keputusan dan hanya terlihat oleh tim internal."
                />

                <div className="flex justify-end">
                  <Button
                    type="button"
                    variant={action === "reject" ? "danger" : "primary"}
                    onClick={openConfirmation}
                    leftIcon={
                      action === "reject" ? (
                        <XCircle className="h-4 w-4" />
                      ) : (
                        <CheckCircle2 className="h-4 w-4" />
                      )
                    }
                  >
                    {action === "reject" ? "Tolak pengajuan" : "Setujui pengajuan"}
                  </Button>
                </div>
              </CardContent>
            </Card>
          ) : (
            <Card>
              <CardContent className="py-5">
                <p className="text-sm font-semibold text-content">
                  Pengajuan sudah diputuskan: {getLoanStatusLabel(application.status)}
                </p>
                <p className="mt-1 text-xs text-content-muted">
                  Keputusan tercatat pada {formatDateTime(application.reviewedAt)}.
                  {application.reviewNotes ? ` Catatan: ${application.reviewNotes}` : ""}
                </p>
                {application.rejectionReason ? (
                  <p className="mt-2 text-xs text-danger-700 dark:text-danger-300">
                    Alasan penolakan: {application.rejectionReason}
                  </p>
                ) : null}
              </CardContent>
            </Card>
          )}

          <Card>
            <CardHeader>
              <CardTitle>Riwayat status</CardTitle>
            </CardHeader>
            <CardContent>
              <LoanTimeline application={application} />
            </CardContent>
          </Card>
        </div>

        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>Skor kredit AI</CardTitle>
              <CardDescription>Hasil penilaian model untuk pengajuan ini.</CardDescription>
            </CardHeader>
            <CardContent>
              {scoreQuery.isLoading ? (
                <div className="skeleton h-44 w-full" />
              ) : score ? (
                <>
                  <ScoreGauge
                    score={score.score}
                    riskCategory={score.riskCategory}
                    modelVersion={score.modelVersion}
                  />
                  <div className="mt-2 flex flex-wrap items-center justify-center gap-2">
                    <RiskBadge riskCategory={score.riskCategory} />
                    <Badge variant="neutral">
                      {RECOMMENDATION_LABELS[score.approvalRecommendation] ||
                        score.approvalRecommendation}
                    </Badge>
                  </div>

                  <div className="mt-5 border-t border-line pt-4">
                    <p className="text-xs font-semibold uppercase tracking-wide text-content-subtle">
                      Fitur utama
                    </p>
                    {features.length > 0 ? (
                      <ul className="mt-3 space-y-2.5">
                        {features.map(([key, value]) => (
                          <li key={key}>
                            <div className="flex items-center justify-between gap-3 text-xs">
                              <span className="truncate text-content-muted">
                                {formatFeatureLabel(key)}
                              </span>
                              <span className="numeric font-semibold text-content">
                                {formatFeatureValue(value)}
                              </span>
                            </div>
                            {Number(value) >= 0 && Number(value) <= 1 ? (
                              <div className="mt-1 h-1.5 overflow-hidden rounded-full bg-surface-subtle">
                                <div
                                  className="h-full rounded-full bg-primary-600"
                                  style={{
                                    width: `${Math.min(100, Math.max(4, Number(value) * 100))}%`,
                                  }}
                                />
                              </div>
                            ) : null}
                          </li>
                        ))}
                      </ul>
                    ) : (
                      <p className="mt-2 text-xs text-content-muted">
                        Rincian fitur tidak tersedia pada respons skor.
                      </p>
                    )}
                  </div>

                  <dl className="mt-4 space-y-1.5 border-t border-line pt-3 text-xs">
                    <div className="flex justify-between gap-3">
                      <dt className="text-content-muted">Model</dt>
                      <dd className="numeric text-content">{score.modelVersion}</dd>
                    </div>
                    <div className="flex justify-between gap-3">
                      <dt className="text-content-muted">Waktu proses</dt>
                      <dd className="numeric text-content">{score.processingTimeMs} ms</dd>
                    </div>
                    <div className="flex justify-between gap-3">
                      <dt className="text-content-muted">Dinilai pada</dt>
                      <dd className="text-content">{formatDateShort(score.scoredAt)}</dd>
                    </div>
                  </dl>
                </>
              ) : (
                <div className="rounded-xl border border-warning-200 bg-warning-50 px-4 py-4 text-sm text-warning-700 dark:border-warning-700/40 dark:bg-warning-700/15 dark:text-warning-200">
                  <p className="flex items-start gap-2">
                    <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
                    Skor AI belum tersedia. Anda tetap dapat mengambil keputusan berdasarkan
                    dokumen dan kebijakan koperasi.
                  </p>
                </div>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardContent className="flex items-start gap-3 py-5">
              <ShieldCheck className="mt-0.5 h-5 w-5 shrink-0 text-primary-600 dark:text-primary-400" aria-hidden="true" />
              <div>
                <p className="text-sm font-semibold text-content">Kebijakan review</p>
                <p className="mt-1 text-xs leading-5 text-content-muted">
                  Keputusan harus mempertimbangkan kapasitas bayar, kualitas dokumen, dan skor AI.
                  Override skor wajib disertai catatan pertimbangan.
                </p>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>

      <ConfirmModal
        open={confirmOpen}
        title={action === "approve" ? "Setujui pengajuan?" : "Tolak pengajuan?"}
        description={`${application.loanProductName} · ${formatCurrency(application.amount)}`}
        confirmLabel={action === "approve" ? "Ya, setujui" : "Ya, tolak"}
        variant={action === "reject" ? "danger" : "primary"}
        loading={decision.isPending}
        onConfirm={submitDecision}
        onClose={() => setConfirmOpen(false)}
      >
        <div className="space-y-2">
          <p>
            {action === "approve"
              ? "Pengajuan akan disetujui dan jadwal cicilan dibuat."
              : "Pengajuan akan ditolak dan peminjam menerima alasannya."}
          </p>
          {action === "reject" ? (
            <p>
              Alasan: <span className="font-medium text-content">{reason}</span>
            </p>
          ) : null}
          {notes ? (
            <p>
              Catatan: <span className="font-medium text-content">{notes}</span>
            </p>
          ) : null}
        </div>
      </ConfirmModal>
    </div>
  );
}
