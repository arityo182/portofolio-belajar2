"use client";

import { useMutation, useQuery } from "@tanstack/react-query";
import { reportService } from "@/services/reportService";
import { reportKeys } from "@/lib/queryKeys";

export function useReportDashboard({ from, to, enabled = true } = {}) {
  return useQuery({
    queryKey: reportKeys.dashboard({ from: from || null, to: to || null }),
    queryFn: () => reportService.dashboard({ from, to }),
    enabled,
    staleTime: 30_000,
  });
}

export function useReportLoans({ from, to, status, page = 0, size = 50, enabled = true } = {}) {
  return useQuery({
    queryKey: reportKeys.loans({ from: from || null, to: to || null, status: status || null, page, size }),
    queryFn: () => reportService.loans({ from, to, status, page, size }),
    enabled,
    placeholderData: (previous) => previous,
    staleTime: 20_000,
  });
}

export function useExportLoansReport() {
  return useMutation({
    mutationFn: reportService.exportLoans,
  });
}
