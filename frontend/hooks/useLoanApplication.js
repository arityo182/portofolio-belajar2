"use client";

import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { loanService } from "@/services/loanService";
import { loanKeys } from "@/lib/queryKeys";

export function useLoanProducts() {
  return useQuery({
    queryKey: loanKeys.products(),
    queryFn: loanService.getProducts,
    staleTime: 5 * 60_000,
  });
}

export function useLoanApplications({ status, page = 0, size = 20 } = {}) {
  return useQuery({
    queryKey: loanKeys.list({ status: status || null, page, size }),
    queryFn: () => loanService.getApplications({ status, page, size }),
    placeholderData: keepPreviousData,
    staleTime: 20_000,
  });
}

export function useLoanApplication(loanId) {
  return useQuery({
    queryKey: loanKeys.detail(loanId),
    queryFn: () => loanService.getApplicationById(loanId),
    enabled: Boolean(loanId),
    staleTime: 15_000,
  });
}

export function useLoanScore(loanId, { enabled = true } = {}) {
  return useQuery({
    queryKey: loanKeys.score(loanId),
    queryFn: () => loanService.getScore(loanId),
    enabled: Boolean(loanId) && enabled,
    retry: false,
    staleTime: 60_000,
  });
}

export function useCreateLoanApplication() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ payload, idempotencyKey }) =>
      loanService.createApplication(payload, idempotencyKey),
    onSuccess: (application) => {
      if (application?.loanId) {
        queryClient.setQueryData(loanKeys.detail(application.loanId), application);
      }
      queryClient.invalidateQueries({ queryKey: loanKeys.all });
    },
  });
}
