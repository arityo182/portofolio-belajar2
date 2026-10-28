export const metadata = {
  title: "Kebijakan Privasi",
  description: "Kebijakan privasi dan pemrosesan data pribadi Koperasi Digital.",
  robots: { index: true, follow: true },
};

const SECTIONS = [
  {
    title: "1. Data yang kami kumpulkan",
    body: "Data identitas (nama, NIK, NPWP), data kontak (email, nomor telepon), data usaha dan keuangan, data perangkat, serta riwayat transaksi di platform.",
  },
  {
    title: "2. Tujuan pemrosesan",
    body: "Verifikasi identitas, penilaian kredit, pemrosesan pembayaran, pencegahan penipuan dan pencucian uang, layanan pelanggan, serta pemenuhan kewajiban regulasi.",
  },
  {
    title: "3. Dasar hukum",
    body: "Persetujuan Anda, pelaksanaan perjanjian, kewajiban hukum, dan kepentingan sah koperasi untuk menjaga keamanan layanan.",
  },
  {
    title: "4. Pembagian data",
    body: "Data dapat dibagikan kepada penyedia pembayaran, penyedia verifikasi identitas, dan otoritas berwenang apabila diwajibkan hukum. Kami tidak menjual data pribadi Anda.",
  },
  {
    title: "5. Retensi dan keamanan",
    body: "Data disimpan selama hubungan hukum berlangsung dan sesuai kewajiban retensi. Kami menerapkan enkripsi, kontrol akses berbasis peran, dan pemantauan akses.",
  },
  {
    title: "6. Hak Anda",
    body: "Anda berhak mengakses, memperbaiki, atau meminta penghapusan data sejauh tidak bertentangan dengan kewajiban hukum. Ajukan permohonan melalui layanan pelanggan.",
  },
];

export default function PrivacyPage() {
  return (
    <>
      <p className="text-2xs font-semibold uppercase tracking-[0.16em] text-primary-600 dark:text-primary-400">
        Informasi legal
      </p>
      <h1 className="mt-2 text-3xl font-bold tracking-[-0.02em] text-content">Kebijakan Privasi</h1>
      <p className="mt-3 text-sm leading-7 text-content-muted">
        Koperasi Digital menghormati privasi Anda. Kebijakan ini menjelaskan data yang kami proses,
        alasannya, dan hak yang Anda miliki.
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
