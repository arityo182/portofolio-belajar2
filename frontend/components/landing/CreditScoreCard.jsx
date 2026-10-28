import { BadgeCheck, Sparkles, TrendingUp } from "lucide-react";

const SCORE = 82.5;
const RADIUS = 52;
const CIRCUMFERENCE = 2 * Math.PI * RADIUS;

const factors = [
  { label: "Arus kas bulanan", value: 88 },
  { label: "Konsistensi transaksi", value: 84 },
  { label: "Reputasi digital", value: 76 },
];

export function CreditScoreCard() {
  const offset = CIRCUMFERENCE * (1 - SCORE / 100);

  return (
    <div className="relative w-full max-w-md animate-float">
      <div className="rounded-2xl bg-white p-6 text-landing-navy shadow-2xl shadow-landing-primary/20 ring-1 ring-landing-border md:p-7">
        <div className="flex items-start justify-between gap-4">
          <div>
            <p className="text-xs font-medium uppercase tracking-wider text-landing-body">
              Skor Kredit AI
            </p>
            <p className="mt-1 font-semibold">Toko Batik Sekar Ayu</p>
          </div>
          <span className="inline-flex items-center gap-1 rounded-full bg-landing-accent px-2.5 py-1 text-xs font-semibold text-landing-primary">
            <Sparkles className="size-3.5" aria-hidden="true" />
            AI
          </span>
        </div>

        <div className="mt-6 flex items-center gap-6">
          <div className="relative size-32 shrink-0">
            <svg viewBox="0 0 120 120" className="size-full -rotate-90" aria-hidden="true">
              <circle
                cx="60"
                cy="60"
                r={RADIUS}
                fill="none"
                strokeWidth="10"
                className="stroke-landing-muted"
              />
              <circle
                cx="60"
                cy="60"
                r={RADIUS}
                fill="none"
                strokeWidth="10"
                strokeLinecap="round"
                strokeDasharray={CIRCUMFERENCE}
                strokeDashoffset={offset}
                className="stroke-landing-primary"
              />
            </svg>
            <div className="absolute inset-0 flex flex-col items-center justify-center">
              <span className="text-3xl font-extrabold tracking-tight">{SCORE}</span>
              <span className="text-xs text-landing-body">/100</span>
            </div>
            <span className="sr-only">{`Skor kredit ${SCORE} dari 100`}</span>
          </div>

          <div className="flex flex-col gap-2">
            <span className="inline-flex w-fit items-center gap-1 rounded-md bg-landing-accent px-2 py-0.5 text-xs font-semibold text-landing-primary">
              Sangat Baik
            </span>
            <p className="text-sm leading-relaxed text-landing-body">
              Limit yang direkomendasikan
            </p>
            <p className="text-xl font-bold">Rp 150.000.000</p>
          </div>
        </div>

        <ul className="mt-6 flex flex-col gap-4 border-t border-landing-border pt-6">
          {factors.map((factor) => (
            <li key={factor.label} className="flex flex-col gap-1.5">
              <div className="flex items-center justify-between text-sm">
                <span className="text-landing-body">{factor.label}</span>
                <span className="font-semibold">{factor.value}</span>
              </div>
              <div className="h-1.5 overflow-hidden rounded-full bg-landing-muted">
                <div
                  className="h-full rounded-full bg-landing-primary"
                  style={{ width: `${factor.value}%` }}
                />
              </div>
            </li>
          ))}
        </ul>

        <div className="mt-6 flex items-center gap-2 rounded-lg bg-landing-muted px-3 py-2.5 text-sm">
          <TrendingUp className="size-4 text-landing-primary" aria-hidden="true" />
          <span className="text-landing-body">
            Naik <span className="font-semibold text-landing-navy">+6,2 poin</span> dalam 3 bulan
          </span>
        </div>
      </div>

      <div className="absolute -bottom-6 -left-4 hidden items-center gap-3 rounded-xl bg-white px-4 py-3 text-landing-navy shadow-xl ring-1 ring-landing-border sm:flex md:-left-10">
        <span className="flex size-9 items-center justify-center rounded-full bg-landing-primary text-white">
          <BadgeCheck className="size-5" aria-hidden="true" />
        </span>
        <div>
          <p className="text-sm font-semibold">Pinjaman disetujui</p>
          <p className="text-xs text-landing-body">Dana cair dalam 24 jam</p>
        </div>
      </div>
    </div>
  );
}
