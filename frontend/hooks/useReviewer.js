"use client";

import { useMemo } from "react";
import { useMutation, useQueries, useQuery, useQueryClient } from "@tanstack/react-query";
import { reviewerService } from "@/services/reviewerService";
import { loanKeys, reviewerKeys, adminKeys } from "@/lib/queryKeys";

export function useReviewQueue() {
  const queueQuery = useQuery({
    queryKey: reviewerKeys.queue(),
    queryFn: reviewerService.getQueue,
    staleTime: 20_000,
  });

  const applications = useMemo(() => queueQuery.data || [], [queueQuery.data]);
  const scoreQueries = useQueries({
    queries: applications.map((application) => ({
      queryKey: loanKeys.score(application.loanId),
      queryFn: () => reviewerService.getScore(application.loanId),
      retry: false,
      staleTime: 60_000,
    })),
  });

  const data = useMemo(
    () =>
      applications.map((application, index) => {
        const score = scoreQueries[index]?.data;
        return {
          ...application,
          score: score?.score ?? null,
          riskCategory: score?.riskCategory ?? null,
          approvalRecommendation: score?.approvalRecommendation ?? null,
        };
      }),
    [applications, scoreQueries],
  );

  const isLoading =
    queueQuery.isLoading || scoreQueries.some((query) => query.isLoading);

  return {
    data,
    isLoading,
    isError: queueQuery.isError,
    error: queueQuery.error,
    refetch: queueQuery.refetch,
  };
}

export function useReviewHistory() {
  return useQuery({
    queryKey: reviewerKeys.history(),
    queryFn: reviewerService.getHistory,
    staleTime: 30_000,
  });
}

export function useReviewApplication(loanId) {
  const applicationQuery = useQuery({
    queryKey: loanKeys.detail(loanId),
    queryFn: () => reviewerService.getApplication(loanId),
    enabled: Boolean(loanId),
    staleTime: 10_000,
  });

  const scoreQuery = useQuery({
    queryKey: loanKeys.score(loanId),
    queryFn: () => reviewerService.getScore(loanId),
    enabled: Boolean(loanId),
    retry: false,
    staleTime: 60_000,
  });

  const borrowerId = applicationQuery.data?.borrowerId;
  const borrowerQuery = useQuery({
    queryKey: reviewerKeys.borrower(borrowerId),
    queryFn: () => reviewerService.getBorrower(borrowerId),
    enabled: Boolean(borrowerId),
    retry: false,
    staleTime: 60_000,
  });

  return {
    applicationQuery,
    scoreQuery,
    borrowerQuery,
    application: applicationQuery.data,
    score: scoreQuery.data,
    borrower: borrowerQuery.data,
    isLoading: applicationQuery.isLoading || scoreQuery.isLoading,
  };
}

export function useReviewDecision() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ loanId, payload }) => reviewerService.decide(loanId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: loanKeys.all });
      queryClient.invalidateQueries({ queryKey: reviewerKeys.all });
      queryClient.invalidateQueries({ queryKey: adminKeys.all });
    },
  });
}
