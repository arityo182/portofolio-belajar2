<!-- FILE: docs/design/design-system.md -->
# Design System
## Platform Koperasi Digital / P2P Lending untuk UMKM Indonesia

| Atribut | Nilai |
|---|---|
| Dokumen | Design System |
| Versi | 1.0 |
| Tanggal | 2026-10-24 |
| Stack | React 19 + JavaScript, Next.js, Tailwind CSS, CSS Custom Properties |
| Referensi Visual | careers.traveloka.com (clean corporate, trust blue) |
| Adaptasi | Fintech serius, dashboard-heavy, angka monospace |

**Prinsip desain:** bersih & terpercaya (trust finansial), biru sebagai *trust color*, whitespace generous, angka finansial memakai monospace (JetBrains Mono) untuk keterbacaan & alignment.

---

## 1. Color Tokens

### 1.1 CSS Custom Properties (`:root`)
```css
:root {
  /* Primary Blue (trust color, Traveloka-inspired) */
  --color-primary-50:  #EFF6FF;
  --color-primary-100: #DBEAFE;
  --color-primary-200: #BFDBFE;
  --color-primary-300: #93C5FD;
  --color-primary-400: #60A5FA;
  --color-primary-500: #3B82F6;
  --color-primary-600: #2563EB;  /* main button */
  --color-primary-700: #1D4ED8;  /* hover */
  --color-primary-800: #1E40AF;
  --color-primary-900: #1E3A8A;  /* dark heading */

  /* Neutral scale */
  --color-neutral-50:  #F9FAFB;
  --color-neutral-100: #F3F4F6;
  --color-neutral-200: #E5E7EB;
  --color-neutral-300: #D1D5DB;
  --color-neutral-400: #9CA3AF;
  --color-neutral-500: #6B7280;
  --color-neutral-600: #4B5563;
  --color-neutral-700: #374151;
  --color-neutral-800: #1F2937;
  --color-neutral-900: #111827;
  --color-neutral-950: #030712;

  /* Semantic: base + light bg + dark text */
  --color-success:      #16A34A;
  --color-success-bg:   #DCFCE7;
  --color-success-text: #166534;
  --color-warning:      #D97706;
  --color-warning-bg:   #FEF3C7;
  --color-warning-text: #92400E;
  --color-error:        #DC2626;
  --color-error-bg:     #FEE2E2;
  --color-error-text:   #991B1B;
  --color-info:         #0EA5E9;
  --color-info-bg:      #E0F2FE;
  --color-info-text:    #075985;

  /* Surface */
  --color-bg-page:       #FFFFFF;
  --color-bg-section:    #F9FAFB;   /* alternating subtle gray */
  --color-bg-card:       #FFFFFF;
  --color-bg-input:      #FFFFFF;
  --color-border:        #E5E7EB;
  --color-border-strong: #D1D5DB;
  --color-text-primary:   #111827;
  --color-text-secondary: #4B5563;
  --color-text-muted:     #9CA3AF;

  /* Loan status (badge) */
  --status-pending-bg:      #F3F4F6; --status-pending-text:      #374151;  /* neutral */
  --status-scoring-bg:      #DBEAFE; --status-scoring-text:      #1D4ED8;  /* blue-light */
  --status-under-review-bg: #FEF3C7; --status-under-review-text: #92400E;  /* yellow */
  --status-approved-bg:     #DCFCE7; --status-approved-text:     #166534;  /* green */
  --status-rejected-bg:     #FEE2E2; --status-rejected-text:     #991B1B;  /* red */
  --status-disbursed-bg:    #EDE9FE; --status-disbursed-text:    #6D28D9;  /* purple */
  --status-active-bg:       #D1FAE5; --status-active-text:       #065F46;  /* green-dark */
  --status-completed-bg:    #E5E7EB; --status-completed-text:    #1F2937;  /* gray */
  --status-default-bg:      #FECACA; --status-default-text:      #7F1D1D;  /* red-dark */
}
```

### 1.2 Dark Mode (`[data-theme="dark"]`)
```css
[data-theme="dark"] {
  --color-primary-600: #3B82F6;  /* sedikit lebih terang agar kontras di gelap */
  --color-primary-700: #60A5FA;

  --color-bg-page:       #0B1220;
  --color-bg-section:    #111827;
  --color-bg-card:       #1F2937;
  --color-bg-input:      #1F2937;
  --color-border:        #374151;
  --color-border-strong: #4B5563;
  --color-text-primary:   #F9FAFB;
  --color-text-secondary: #D1D5DB;
  --color-text-muted:     #9CA3AF;

  --color-success-bg: #14321F; --color-success-text: #86EFAC;
  --color-warning-bg: #3A2A0A; --color-warning-text: #FCD34D;
  --color-error-bg:   #3A1414; --color-error-text:   #FCA5A5;
  --color-info-bg:    #0A2A3A; --color-info-text:    #7DD3FC;

  --status-pending-bg:      #1F2937; --status-pending-text:      #D1D5DB;
  --status-scoring-bg:      #1E3A8A; --status-scoring-text:      #BFDBFE;
  --status-under-review-bg: #3A2A0A; --status-under-review-text: #FCD34D;
  --status-approved-bg:     #14321F; --status-approved-text:     #86EFAC;
  --status-rejected-bg:     #3A1414; --status-rejected-text:     #FCA5A5;
  --status-disbursed-bg:    #2E1065; --status-disbursed-text:    #DDD6FE;
  --status-active-bg:       #064E3B; --status-active-text:       #6EE7B7;
  --status-completed-bg:    #374151; --status-completed-text:    #E5E7EB;
  --status-default-bg:      #450A0A; --status-default-text:      #FCA5A5;
}
```

---

## 2. Typography

```css
:root {
  --font-sans: 'Inter', system-ui, -apple-system, sans-serif;
  --font-mono: 'JetBrains Mono', 'SFMono-Regular', monospace;  /* nominal uang & angka */

  /* size / line-height / letter-spacing */
  --text-xs:   0.75rem;   /* 12 / 16 */
  --text-sm:   0.875rem;  /* 14 / 20 */
  --text-base: 1rem;      /* 16 / 24 */
  --text-lg:   1.125rem;  /* 18 / 28 */
  --text-xl:   1.25rem;   /* 20 / 28 */
  --text-2xl:  1.5rem;    /* 24 / 32 */
  --text-3xl:  1.875rem;  /* 30 / 36 */
  --text-4xl:  2.25rem;   /* 36 / 40 */
  --text-5xl:  3rem;      /* 48 / 52 */
  --text-hero: 4rem;      /* 64 / 68 */

  --font-normal: 400;
  --font-medium: 500;
  --font-semibold: 600;
  --font-bold: 700;
}
```

| Token | px | rem | line-height | letter-spacing | Penggunaan |
|---|---|---|---|---|---|
| `text-xs` | 12 | 0.75 | 16px | 0.01em | Caption, hint |
| `text-sm` | 14 | 0.875 | 20px | 0 | Label, secondary |
| `text-base` | 16 | 1 | 24px | 0 | Body |
| `text-lg` | 18 | 1.125 | 28px | 0 | Body besar |
| `text-xl` | 20 | 1.25 | 28px | -0.01em | Subheading |
| `text-2xl` | 24 | 1.5 | 32px | -0.01em | Card title |
| `text-3xl` | 30 | 1.875 | 36px | -0.02em | Section title |
| `text-4xl` | 36 | 2.25 | 40px | -0.02em | Stats number |
| `text-5xl` | 48 | 3 | 52px | -0.02em | Page title |
| `text-hero` | 64 | 4 | 68px | -0.03em | Hero headline |

> **Angka finansial** (nominal rupiah, score, saldo) **selalu** memakai `--font-mono` dengan `font-variant-numeric: tabular-nums` agar sejajar.

---

## 3. Spacing & Layout

```css
:root {
  --space-1: 4px;   --space-2: 8px;   --space-3: 12px;  --space-4: 16px;
  --space-5: 20px;  --space-6: 24px;  --space-8: 32px;  --space-10: 40px;
  --space-12: 48px; --space-16: 64px; --space-20: 80px; --space-24: 96px;

  --container-max: 1280px;
  --container-pad-mobile: 24px;
  --container-pad-desktop: 48px;
  --section-pad-desktop: 80px;
  --section-pad-mobile: 48px;
}
```

| Breakpoint | Lebar |
|---|---|
| `sm` | 640px |
| `md` | 768px |
| `lg` | 1024px |
| `xl` | 1280px |

- **Container:** `max-width: 1280px`, padding 24px (mobile) / 48px (desktop), `margin: 0 auto`.
- **Section:** padding vertikal 80px desktop / 48px mobile; section berselang menggunakan `--color-bg-section`.

---

## 4. Component Tokens

```css
:root {
  /* Card */
  --card-radius: 12px;
  --card-shadow: 0 2px 12px rgba(0,0,0,0.06);
  --card-shadow-hover: 0 4px 20px rgba(0,0,0,0.10);
  --card-padding: 24px;

  /* Button */
  --btn-radius: 8px;
  --btn-pad-x: 20px;
  --btn-transition: 150ms ease;
  --btn-height-sm: 36px;
  --btn-height: 44px;
  --btn-height-lg: 52px;

  /* Input */
  --input-height: 48px;
  --input-radius: 8px;
  --input-border-width: 1.5px;
  --focus-ring-width: 2px;

  /* Badge */
  --badge-height: 24px;
  --badge-radius: 9999px;
  --badge-pad-x: 8px;
}
```

### 4.1 Button Variants
```jsx
// shared/components/Button.jsx
const base = "inline-flex items-center justify-center font-semibold rounded-lg " +
  "transition-[background,color,box-shadow] duration-150 focus-visible:outline-none " +
  "focus-visible:ring-2 focus-visible:ring-primary-600 focus-visible:ring-offset-2 " +
  "disabled:opacity-50 disabled:cursor-not-allowed";

const variants = {
  primary:   "bg-primary-600 text-white hover:bg-primary-700",
  secondary: "border-1.5 border-primary-600 text-primary-600 bg-transparent hover:bg-primary-50",
  ghost:     "text-primary-600 bg-transparent hover:bg-primary-50",
  danger:    "bg-error text-white hover:brightness-95",
};
const sizes = { sm: "h-9 px-4 text-sm", default: "h-11 px-5 text-base", lg: "h-13 px-6 text-lg" };

export function Button({ variant = "primary", size = "default", className = "", ...props }) {
  return <button className={`${base} ${variants[variant]} ${sizes[size]} ${className}`} {...props} />;
}
```

### 4.2 Input
```jsx
export function Input({ className = "", ...props }) {
  return (
    <input
      className={`h-12 w-full rounded-lg border-[1.5px] border-[var(--color-border)]
        bg-[var(--color-bg-input)] px-4 text-base text-[var(--color-text-primary)]
        placeholder:text-[var(--color-text-muted)]
        focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600
        focus-visible:border-primary-600 ${className}`}
      {...props}
    />
  );
}
```

### 4.3 Status Badge & Stats Card
```jsx
const statusMap = {
  PENDING:'pending', SCORING:'scoring', UNDER_REVIEW:'under-review', APPROVED:'approved',
  REJECTED:'rejected', DISBURSED:'disbursed', ACTIVE:'active', COMPLETED:'completed', DEFAULT:'default',
};
export function StatusBadge({ status }) {
  const s = statusMap[status] ?? 'pending';
  return (
    <span className="inline-flex items-center h-6 rounded-full px-2 text-xs font-medium"
      style={{ background:`var(--status-${s}-bg)`, color:`var(--status-${s}-text)` }}>
      {status}
    </span>
  );
}

export function StatsCard({ icon, value, label }) {
  return (
    <div className="rounded-xl bg-[var(--color-bg-card)] p-6 shadow-card">
      <div className="mb-2 text-primary-600">{icon}</div>
      <div className="font-mono text-4xl font-bold tabular-nums text-[var(--color-text-primary)]">{value}</div>
      <div className="mt-1 text-sm text-[var(--color-text-secondary)]">{label}</div>
    </div>
  );
}
```

---

## 5. Tailwind Config Extend

```js
// tailwind.config.js
/** @type {import('tailwindcss').Config} */
module.exports = {
  darkMode: 'class',
  content: ['./app/**/*.{js,jsx}', './src/**/*.{js,jsx}', './components/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        primary: {
          50:'#EFF6FF',100:'#DBEAFE',200:'#BFDBFE',300:'#93C5FD',400:'#60A5FA',
          500:'#3B82F6',600:'#2563EB',700:'#1D4ED8',800:'#1E40AF',900:'#1E3A8A',
        },
        neutral: {
          50:'#F9FAFB',100:'#F3F4F6',200:'#E5E7EB',300:'#D1D5DB',400:'#9CA3AF',
          500:'#6B7280',600:'#4B5563',700:'#374151',800:'#1F2937',900:'#111827',950:'#030712',
        },
        success:'#16A34A', warning:'#D97706', error:'#DC2626', info:'#0EA5E9',
      },
      fontFamily: {
        sans: ['Inter','system-ui','sans-serif'],
        mono: ['"JetBrains Mono"','SFMono-Regular','monospace'],
      },
      fontSize: {
        xs:   ['0.75rem', { lineHeight:'16px', letterSpacing:'0.01em' }],
        sm:   ['0.875rem',{ lineHeight:'20px' }],
        base: ['1rem',    { lineHeight:'24px' }],
        lg:   ['1.125rem',{ lineHeight:'28px' }],
        xl:   ['1.25rem', { lineHeight:'28px', letterSpacing:'-0.01em' }],
        '2xl':['1.5rem',  { lineHeight:'32px', letterSpacing:'-0.01em' }],
        '3xl':['1.875rem',{ lineHeight:'36px', letterSpacing:'-0.02em' }],
        '4xl':['2.25rem', { lineHeight:'40px', letterSpacing:'-0.02em' }],
        '5xl':['3rem',    { lineHeight:'52px', letterSpacing:'-0.02em' }],
        hero: ['4rem',    { lineHeight:'68px', letterSpacing:'-0.03em' }],
      },
      spacing: {
        1:'4px',2:'8px',3:'12px',4:'16px',5:'20px',6:'24px',
        8:'32px',10:'40px',12:'48px',13:'52px',16:'64px',20:'80px',24:'96px',
      },
      borderRadius: { lg:'8px', xl:'12px', full:'9999px' },
      boxShadow: {
        card:'0 2px 12px rgba(0,0,0,0.06)',
        'card-hover':'0 4px 20px rgba(0,0,0,0.10)',
      },
      screens: { sm:'640px', md:'768px', lg:'1024px', xl:'1280px' },
      maxWidth: { container:'1280px' },
      borderWidth: { '1.5':'1.5px' },
    },
  },
  plugins: [],
};
```

---

## 6. WCAG 2.1 AA

### 6.1 Contrast Ratio (min 4.5:1 normal · 3:1 large text)
| Foreground | Background | Rasio | Normal | Large |
|---|---|---|---|---|
| primary-600 `#2563EB` | white `#FFFFFF` | 4.56:1 | ✅ | ✅ |
| primary-700 `#1D4ED8` | white | 6.33:1 | ✅ | ✅ |
| white | primary-600 `#2563EB` | 4.56:1 | ✅ | ✅ |
| neutral-600 `#4B5563` | white | 7.56:1 | ✅ | ✅ |
| neutral-500 `#6B7280` | white | 4.83:1 | ✅ | ✅ |
| neutral-900 `#111827` | white | 17.4:1 | ✅ | ✅ |
| success-text `#166534` | success-bg `#DCFCE7` | 7.1:1 | ✅ | ✅ |
| warning-text `#92400E` | warning-bg `#FEF3C7` | 6.8:1 | ✅ | ✅ |
| error-text `#991B1B` | error-bg `#FEE2E2` | 7.4:1 | ✅ | ✅ |
| info-text `#075985` | info-bg `#E0F2FE` | 6.9:1 | ✅ | ✅ |
| neutral-400 `#9CA3AF` | white | 2.6:1 | ❌ | ✅ (large only) |

> `neutral-400` hanya untuk teks besar/placeholder non-esensial, bukan body text.

### 6.2 Focus Ring
```css
:focus-visible {
  outline: 2px solid var(--color-primary-600);
  outline-offset: 2px;
  border-radius: 4px;
}
```

### 6.3 Skip Navigation Link
```jsx
// visually hidden, visible on focus
<a href="#main-content"
   className="sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-4 focus:z-50
              focus:rounded-lg focus:bg-primary-600 focus:px-4 focus:py-2 focus:text-white">
  Lewati ke konten utama
</a>
```
Tambahan A11y: semua interaktif dapat diakses keyboard, `aria-label` pada icon button, `aria-live` untuk toast/status, kontras status badge terverifikasi.

---

## 7. Page Pattern Templates

### 7.a Hero Section
```jsx
<section className="w-full bg-white">
  <div className="mx-auto max-w-container px-6 py-12 md:px-12 md:py-20 text-center">
    <h1 className="text-4xl md:text-hero font-bold text-primary-900">
      Modal Usaha untuk UMKM Indonesia
    </h1>
    <p className="mx-auto mt-6 max-w-2xl text-lg text-neutral-600">
      Pinjaman cepat, transparan, dan terpercaya dengan penilaian kredit berbasis AI.
    </p>
    <div className="mt-8">
      <Button size="lg">Ajukan Pinjaman</Button>
    </div>
    {/* Stats bar */}
    <div className="mt-16 grid grid-cols-2 gap-8 md:grid-cols-4">
      <StatsBarItem value="Rp18,7 M" label="Total Tersalurkan" />
      <StatsBarItem value="1.240" label="UMKM Terbantu" />
      <StatsBarItem value="4,8%" label="NPL Ratio" />
      <StatsBarItem value="99,9%" label="Uptime SLA" />
    </div>
  </div>
</section>
```

### 7.b Dashboard Layout (sidebar 240px + main, collapsible)
```jsx
<div className="flex min-h-screen bg-[var(--color-bg-section)]">
  <aside className="hidden w-60 shrink-0 border-r border-[var(--color-border)] bg-[var(--color-bg-card)] lg:block">
    <nav className="p-4 space-y-1">{/* nav items */}</nav>
  </aside>
  {/* Mobile drawer: toggle dengan state, overlay */}
  <div className="flex min-w-0 flex-1 flex-col">
    <header className="flex h-16 items-center justify-between border-b border-[var(--color-border)]
                       bg-[var(--color-bg-card)] px-6">
      <button className="lg:hidden" aria-label="Buka menu">{/* hamburger */}</button>
      <ThemeToggle />
    </header>
    <main id="main-content" className="flex-1 p-6">{/* content */}</main>
  </div>
</div>
```

### 7.c Stats Bar (4-metric grid)
```jsx
<div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
  {metrics.map((m) => (
    <StatsCard key={m.label} icon={m.icon} value={m.value} label={m.label} />
  ))}
</div>
```

### 7.d Card Grid (3/2/1 responsive)
```jsx
<div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
  {items.map((it) => (
    <article key={it.id}
      className="rounded-xl bg-[var(--color-bg-card)] p-6 shadow-card transition-shadow hover:shadow-card-hover">
      <h3 className="text-2xl font-semibold text-[var(--color-text-primary)]">{it.title}</h3>
      <p className="mt-2 text-sm text-[var(--color-text-secondary)]">{it.desc}</p>
    </article>
  ))}
</div>
```

### 7.e Data Table (sortable + row actions + pagination)
```jsx
<div className="overflow-hidden rounded-xl border border-[var(--color-border)] bg-[var(--color-bg-card)]">
  <table className="w-full text-left text-sm">
    <thead className="bg-[var(--color-bg-section)] text-neutral-600">
      <tr>
        <th className="px-4 py-3">
          <button className="inline-flex items-center gap-1" aria-sort="ascending">Peminjam ↑</button>
        </th>
        <th className="px-4 py-3 text-right">Jumlah</th>
        <th className="px-4 py-3">Status</th>
        <th className="px-4 py-3 text-right">Aksi</th>
      </tr>
    </thead>
    <tbody className="divide-y divide-[var(--color-border)]">
      {rows.map((r) => (
        <tr key={r.id} className="hover:bg-[var(--color-bg-section)]">
          <td className="px-4 py-3">{r.borrowerName}</td>
          <td className="px-4 py-3 text-right font-mono tabular-nums">{r.amount}</td>
          <td className="px-4 py-3"><StatusBadge status={r.status} /></td>
          <td className="px-4 py-3 text-right">
            <Button variant="ghost" size="sm">Detail</Button>
          </td>
        </tr>
      ))}
    </tbody>
  </table>
  <div className="flex items-center justify-between px-4 py-3 text-sm text-neutral-600">
    <span>Menampilkan 1–20 dari 820</span>
    <div className="flex gap-2">
      <Button variant="secondary" size="sm">Sebelumnya</Button>
      <Button variant="secondary" size="sm">Berikutnya</Button>
    </div>
  </div>
</div>
```

### 7.f Multi-step Form (step indicator)
```jsx
<div className="mx-auto max-w-2xl">
  <ol className="mb-8 flex items-center gap-4">
    {steps.map((s, i) => (
      <li key={s} className="flex items-center gap-2">
        <span className={`flex h-8 w-8 items-center justify-center rounded-full text-sm font-semibold
          ${i <= current ? 'bg-primary-600 text-white' : 'bg-neutral-200 text-neutral-500'}`}>
          {i + 1}
        </span>
        <span className="text-sm">{s}</span>
      </li>
    ))}
  </ol>
  <form className="rounded-xl bg-[var(--color-bg-card)] p-6 shadow-card space-y-4">
    {/* fields per step */}
    <div className="flex justify-between pt-4">
      <Button variant="ghost" disabled={current === 0}>Kembali</Button>
      <Button>{current === steps.length - 1 ? 'Ajukan' : 'Lanjut'}</Button>
    </div>
  </form>
</div>
```

### 7.g Status Timeline (loan lifecycle)
```jsx
<ol className="relative border-l-2 border-[var(--color-border)] pl-6">
  {timeline.map((t) => (
    <li key={t.status} className="mb-8 last:mb-0">
      <span className="absolute -left-[9px] flex h-4 w-4 items-center justify-center rounded-full"
        style={{ background:`var(--status-${statusMap[t.status]}-text)` }} />
      <div className="flex items-center gap-2">
        <StatusBadge status={t.status} />
        <time className="font-mono text-xs text-neutral-500 tabular-nums">{t.date}</time>
      </div>
      <p className="mt-1 text-sm text-[var(--color-text-secondary)]">{t.note}</p>
    </li>
  ))}
</ol>
```

---

## 8. Dark Mode Implementation

### 8.1 Tailwind
`darkMode: 'class'` (lihat §5). Token tema dikendalikan via `[data-theme="dark"]` + class `dark` pada `<html>`.

### 8.2 No-Flash Script — `app/layout.js` (Next.js App Router)
```jsx
// app/layout.js
export default function RootLayout({ children }) {
  return (
    <html lang="id" suppressHydrationWarning>
      <head>
        {/* Set tema SEBELUM paint untuk mencegah flash (FOUC) */}
        <script
          dangerouslySetInnerHTML={{
            __html: `(function(){try{
              var t = localStorage.getItem('theme');
              var sys = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
              var theme = t || sys;
              document.documentElement.setAttribute('data-theme', theme);
              document.documentElement.classList.toggle('dark', theme === 'dark');
            }catch(e){}})();`,
          }}
        />
      </head>
      <body className="bg-[var(--color-bg-page)] text-[var(--color-text-primary)] antialiased">
        {children}
      </body>
    </html>
  );
}
```

> Catatan: proyek memakai **JavaScript** (bukan TypeScript), sehingga setup `_document.tsx` klasik digantikan oleh `app/layout.js` App Router. Script inline di `<head>` berjalan sebelum render untuk mencegah flash tema.

### 8.3 Theme Toggle Component
```jsx
// shared/components/ThemeToggle.jsx
'use client';
import { useEffect, useState } from 'react';

export function ThemeToggle() {
  const [theme, setTheme] = useState('light');

  useEffect(() => {
    setTheme(document.documentElement.getAttribute('data-theme') || 'light');
  }, []);

  const toggle = () => {
    const next = theme === 'dark' ? 'light' : 'dark';
    setTheme(next);
    localStorage.setItem('theme', next);
    document.documentElement.setAttribute('data-theme', next);
    document.documentElement.classList.toggle('dark', next === 'dark');
  };

  return (
    <button onClick={toggle} aria-label="Ganti tema"
      className="inline-flex h-10 w-10 items-center justify-center rounded-lg
                 text-neutral-600 hover:bg-[var(--color-bg-section)]
                 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600">
      {theme === 'dark' ? '☀️' : '🌙'}
    </button>
  );
}
```
