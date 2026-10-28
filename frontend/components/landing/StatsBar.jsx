const stats = [
  { value: "Rp 2,4 T", label: "Total dana tersalurkan" },
  { value: "48.000+", label: "UMKM telah didanai" },
  { value: "97,8%", label: "Tingkat keberhasilan bayar (TKB90)" },
  { value: "34", label: "Provinsi terjangkau" },
];

export function StatsBar() {
  return (
    <section aria-label="Statistik KoperasiDigital" className="relative z-10 -mt-[60px] px-4 md:px-8">
      <dl className="mx-auto grid max-w-7xl grid-cols-2 overflow-hidden rounded-2xl bg-white shadow-2xl shadow-landing-navy/10 ring-1 ring-black/5 lg:grid-cols-4">
        {stats.map((stat, index) => (
          <div
            key={stat.label}
            className={`flex flex-col items-center gap-1 border-landing-border px-6 py-7 text-center md:px-8 md:py-8 ${
              index > 0 ? "border-l" : ""
            } ${index >= 2 ? "border-t lg:border-t-0" : ""}`}
          >
            <dt className="order-2 text-sm leading-relaxed text-landing-body">{stat.label}</dt>
            <dd className="order-1 text-2xl font-extrabold tracking-tight text-landing-navy md:text-3xl">
              {stat.value}
            </dd>
          </div>
        ))}
      </dl>
    </section>
  );
}
