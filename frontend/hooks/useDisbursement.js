"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { loanService } from "@/services/loanService";
import { loanKeys } from "@/lib/queryKeys";

export function useInitiateDisbursement() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ payload, idempotencyKey }) =>
      loanService.initiateDisbursement(payload, idempotencyKey),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: loanKeys.all });
      if (variables?.payload?.loanApplicationId) {
        queryClient.invalidateQueries({
          queryKey: loanKeys.detail(variables.payload.loanApplicationId),
        });
      }
    },
  });
}
