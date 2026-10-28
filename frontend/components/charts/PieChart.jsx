"use client";

import {
  Cell,
  Legend,
  Pie,
  PieChart as RechartsPieChart,
  ResponsiveContainer,
  Tooltip,
} from "recharts";

const TOOLTIP_STYLE = {
  backgroundColor: "var(--color-surface-raised)",
  border: "1px solid var(--color-border)",
  borderRadius: "0.75rem",
  fontSize: "12px",
  color: "var(--color-content)",
  boxShadow: "0 12px 32px -8px rgba(15, 23, 42, 0.25)",
};

const DEFAULT_COLORS = ["#16A34A", "#F59E0B", "#DC2626", "#64748B"];

export default function PieChart({
  data = [],
  dataKey = "value",
  nameKey = "name",
  height = 280,
  colors = DEFAULT_COLORS,
  valueFormatter = (value) => Number(value).toLocaleString("id-ID"),
}) {
  return (
    <div style={{ width: "100%", height }}>
      <ResponsiveContainer width="100%" height="100%">
        <RechartsPieChart>
          <Pie
            data={data}
            dataKey={dataKey}
            nameKey={nameKey}
            innerRadius="52%"
            outerRadius="78%"
            paddingAngle={3}
            stroke="var(--color-surface-raised)"
            strokeWidth={2}
            isAnimationActive={false}
          >
            {data.map((entry, index) => (
              <Cell key={entry[nameKey] || index} fill={colors[index % colors.length]} />
            ))}
          </Pie>
          <Tooltip
            contentStyle={TOOLTIP_STYLE}
            formatter={(value) => valueFormatter(value)}
          />
          <Legend
            verticalAlign="bottom"
            iconType="circle"
            wrapperStyle={{ fontSize: "12px", color: "var(--color-content-muted)" }}
          />
        </RechartsPieChart>
      </ResponsiveContainer>
    </div>
  );
}
