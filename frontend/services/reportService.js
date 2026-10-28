import { api, unwrap } from "@/services/api";

function downloadBlob(blob, filename) {
  const url = window.URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  window.URL.revokeObjectURL(url);
}

export const reportService = {
  async dashboard({ from, to } = {}) {
    const response = await api.get("/reports/dashboard", {
      params: { from: from || undefined, to: to || undefined },
    });
    return unwrap(response);
  },

  async loans({ from, to, status, page = 0, size = 50 } = {}) {
    const response = await api.get("/reports/loans", {
      params: {
        from: from || undefined,
        to: to || undefined,
        status: status || undefined,
        page,
        size,
      },
    });
    return unwrap(response);
  },

  async exportLoans({ from, to, status } = {}) {
    const response = await api.get("/reports/loans/export", {
      params: {
        from: from || undefined,
        to: to || undefined,
        status: status || undefined,
        format: "csv",
      },
      responseType: "blob",
    });
    const filename = `laporan-pinjaman-${from || "awal"}-${to || "akhir"}.csv`;
    downloadBlob(response.data, filename);
    return filename;
  },

  async exportPdf({ from, to, status } = {}) {
    const response = await api.get("/reports/loans/export", {
      params: {
        from: from || undefined,
        to: to || undefined,
        status: status || undefined,
        format: "pdf",
      },
      responseType: "blob",
    });
    const filename = `laporan-pinjaman-${from || "awal"}-${to || "akhir"}.pdf`;
    downloadBlob(response.data, filename);
    return filename;
  },
};
