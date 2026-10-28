"use client";

import {
  PolarAngleAxis,
  RadialBar,
  RadialBarChart,
  ResponsiveContainer,
} from "recharts";

const RISK_COLORS = {
  LOW: "#16A34A",
  MEDIUM: "#F59E0B",
  HIGH: "#DC2626",
};

export default function ScoreGauge({ score = 0, riskCategory = "MEDIUM", modelVersion, height = 190 }) {
  const numericScore = Math.max(0, Math.min(100, Number(score) || 0));
  const color = RISK_COLORS[riskCategory] || "#2563EB";
  const data = [{ name: "Skor kredit", value: numericScore, fill: color }];

  return (
    <div
      className="relative mx-auto w-full max-w-[260px]"
      style={{ height }}
      role="img"
      aria-label={`Skor kredit ${numericScore.toFixed(1)} dari 100, kategori ${riskCategory}`}
    >
      <ResponsiveContainer width="100%" height="100%">
        <RadialBarChart
          data={data}
          cx="50%"
          cy="78%"
          innerRadius="66%"
          outerRadius="100%"
          startAngle={180}
          endAngle={0}
          barSize={16}
        >
          <PolarAngleAxis type="number" domain={[0, 100]} angleAxisId={0} tick={false} />
          <RadialBar
            dataKey="value"
            cornerRadius={10}
            background={{ fill: "var(--color-surface-subtle)" }}
            isAnimationActive={false}
          />
        </RadialBarChart>
      </ResponsiveContainer>

      <div className="pointer-events-none absolute inset-x-0 bottom-8 flex flex-col items-center">
        <p className="numeric text-4xl font-semibold tracking-[-0.03em] text-content">
          {numericScore.toFixed(1)}
        </p>
        <p className="mt-1 text-2xs uppercase tracking-[0.14em] text-content-subtle">dari 100</p>
        {modelVersion ? (
          <p className="mt-1 text-2xs text-content-subtle">{modelVersion}</p>
        ) : null}
      </div>
    </div>
  );
}
