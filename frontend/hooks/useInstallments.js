"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { loanService } from "@/services/loanService";
import { loanKeys, paymentKeys } from "@/lib/queryKeys";

export function useInstallments(loanId, { enabled = true } = {}) {
  return useQuery({
    queryKey: loanKeys.installments(loanId),
    queryFn: () => loanService.getInstallments(loanId),
    enabled: Boolean(loanId) && enabled,
    staleTime: 15_000,
  });
}

export function usePayInstallment() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ installmentId, payload, idempotencyKey }) =>
      loanService.initiatePayment(installmentId, payload, idempotencyKey),
    onSuccess: (payment) => {
      if (payment?.installmentId) {
        queryClient.invalidateQueries({
          queryKey: paymentKeys.status(payment.installmentId),
        });
      }
      queryClient.invalidateQueries({ queryKey: loanKeys.all });
    },
  });
}
