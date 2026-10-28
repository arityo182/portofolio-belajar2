export const metadata = {
  title: "Syarat & Ketentuan",
  description: "Syarat dan ketentuan penggunaan platform Koperasi Digital.",
  robots: { index: true, follow: true },
};

const SECTIONS = [
  {
    title: "1. Ruang lingkup layanan",
    body: "Koperasi Digital menyediakan platform penghubung antara peminjam UMKM dan investor. Setiap pengajuan pinjaman tunduk pada penilaian kredit, verifikasi dokumen, dan kebijakan risiko koperasi.",
  },
  {
    title: "2. Akun dan keamanan",
    body: "Anda wajib memberikan informasi yang benar, menjaga kerahasiaan kata sandi, dan segera melaporkan akses tidak sah. Koperasi Digital dapat menangguhkan akun yang terindikasi melakukan penipuan atau pelanggaran hukum.",
  },
  {
    title: "3. Pinjaman dan pembayaran",
    body: "Nominal, tenor, bunga, dan biaya disajikan sebelum persetujuan. Keterlambatan pembayaran dapat menimbulkan denda sesuai perjanjian dan memengaruhi riwayat kredit Anda.",
  },
  {
    title: "4. Investasi",
    body: "Investasi mengandung risiko, termasuk risiko gagal bayar. Kinerja masa lalu tidak menjamin hasil di masa depan. Investor wajib memahami profil risiko sebelum mendanai.",
  },
  {
    title: "5. Perlindungan data",
    body: "Data pribadi diproses untuk verifikasi, penilaian kredit, pencegahan penipuan, dan kepatuhan regulasi. Rincian pemrosesan diatur dalam Kebijakan Privasi.",
  },
  {
    title: "6. Penyelesaian sengketa",
    body: "Sengketa diselesaikan terlebih dahulu melalui layanan pengaduan koperasi. Apabila belum tercapai kesepakatan, para pihak dapat menempuh mekanisme yang diatur peraturan perundang-undangan yang berlaku.",
  },
];

export default function TermsPage() {
  return (
    <>
      <p className="text-2xs font-semibold uppercase tracking-[0.16em] text-primary-600 dark:text-primary-400">
        Informasi legal
      </p>
      <h1 className="mt-2 text-3xl font-bold tracking-[-0.02em] text-content">Syarat & Ketentuan</h1>
      <p className="mt-3 text-sm leading-7 text-content-muted">
        Dengan mendaftar dan menggunakan layanan Koperasi Digital, Anda menyetujui ketentuan berikut.
        Bacalah dengan saksama sebelum mengajukan pinjaman atau melakukan pendanaan.
      </p>

      <div className="mt-8 space-y-7">
        {SECTIONS.map((section) => (
          <section key={section.title}>
            <h2 className="text-base font-semibold text-content">{section.title}</h2>
            <p className="mt-1.5 text-sm leading-7 text-content-muted">{section.body}</p>
          </section>
        ))}
      </div>
    </>
  );
}
