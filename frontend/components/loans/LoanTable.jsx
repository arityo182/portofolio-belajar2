import Link from "next/link";
import { ArrowUpRight } from "lucide-react";
import Badge from "@/components/ui/Badge";
import { formatCurrency, formatDateShort } from "@/lib/utils";

function shortId(value) {
  return value ? String(value).slice(0, 8).toUpperCase() : "—";
}

export default function LoanTable({ applications = [], caption = "Daftar pinjaman" }) {
  if (applications.length === 0) {
    return null;
  }

  return (
    <>
      <div className="hidden overflow-x-auto md:block">
        <table className="w-full min-w-[600px] border-collapse text-left">
          <caption className="sr-only">{caption}</caption>
          <thead>
            <tr className="border-b border-line text-2xs uppercase tracking-[0.12em] text-content-subtle">
              <th scope="col" className="px-4 py-3 font-semibold">
                Pinjaman
              </th>
              <th scope="col" className="px-4 py-3 font-semibold">
                Nominal
              </th>
              <th scope="col" className="px-4 py-3 font-semibold">
                Status
              </th>
              <th scope="col" className="px-4 py-3 font-semibold">
                Diajukan
              </th>
              <th scope="col" className="px-4 py-3 text-right font-semibold">
                Aksi
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-line">
            {applications.map((application) => (
              <tr key={application.loanId} className="transition-colors hover:bg-surface-subtle">
                <td className="px-4 py-3.5">
                  <Link
                    href={`/loans/${application.loanId}`}
                    className="group inline-flex flex-col rounded focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600"
                  >
                    <span className="max-w-[200px] truncate text-sm font-semibold text-content group-hover:text-primary-700 dark:group-hover:text-primary-300">
                      {application.loanProductName || "Pinjaman"}
                    </span>
                    <span className="numeric mt-0.5 text-2xs text-content-subtle">
                      #{shortId(application.loanId)}
                    </span>
                  </Link>
                </td>
                <td className="numeric whitespace-nowrap px-4 py-3.5 text-sm font-semibold text-content">
                  {formatCurrency(application.amount)}
                </td>
                <td className="px-4 py-3.5">
                  <Badge status={application.status} dot />
                </td>
                <td className="whitespace-nowrap px-4 py-3.5 text-sm text-content-muted">
                  {formatDateShort(application.submittedAt)}
                </td>
                <td className="px-4 py-3.5 text-right">
                  <Link
                    href={`/loans/${application.loanId}`}
                    aria-label={`Lihat detail pinjaman ${application.loanProductName || shortId(application.loanId)}`}
                    className="inline-flex h-8 w-8 items-center justify-center rounded-lg text-content-muted transition-colors hover:bg-surface-subtle hover:text-primary-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600 dark:hover:text-primary-300"
                  >
                    <ArrowUpRight className="h-4 w-4" aria-hidden="true" />
                  </Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <ul className="divide-y divide-line md:hidden">
        {applications.map((application) => (
          <li key={application.loanId}>
            <Link
              href={`/loans/${application.loanId}`}
              className="flex flex-col gap-2.5 px-4 py-4 transition-colors hover:bg-surface-subtle focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-primary-600"
            >
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <p className="truncate text-sm font-semibold text-content">
                    {application.loanProductName || "Pinjaman"}
                  </p>
                  <p className="numeric mt-0.5 text-2xs text-content-subtle">
                    #{shortId(application.loanId)}
                  </p>
                </div>
                <Badge status={application.status} dot />
              </div>
              <div className="flex flex-wrap items-center justify-between gap-2">
                <span className="numeric text-base font-semibold text-content">
                  {formatCurrency(application.amount)}
                </span>
                <span className="text-xs text-content-muted">
                  {application.tenorMonths} bulan · {formatDateShort(application.submittedAt)}
                </span>
              </div>
            </Link>
          </li>
        ))}
      </ul>
    </>
  );
}
