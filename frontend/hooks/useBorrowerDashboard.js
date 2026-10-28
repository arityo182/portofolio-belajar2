"use client";

import { useMemo } from "react";
import { useQueries } from "@tanstack/react-query";
import { useLoanApplications } from "@/hooks/useLoanApplication";
import { loanService } from "@/services/loanService";
import { loanKeys } from "@/lib/queryKeys";
import { LOAN_FINANCIAL_STATUSES, LOAN_OUTSTANDING_STATUSES } from "@/lib/constants";
import { isDateBeforeToday, isDateInMonth } from "@/lib/utils";

export function useBorrowerDashboard(userId) {
  const listQuery = useLoanApplications({ page: 0, size: 100 });
  const applications = useMemo(() => listQuery.data?.content ?? [], [listQuery.data]);

  const activeApplications = useMemo(
    () => applications.filter((application) => LOAN_OUTSTANDING_STATUSES.includes(application.status)),
    [applications],
  );

  const installmentApplications = useMemo(
    () =>
      applications.filter((application) =>
        ["APPROVED", "DISBURSED", "ACTIVE", "COMPLETED", "DEFAULT"].includes(
          application.status,
        ),
      ),
    [applications],
  );

  const installmentQueries = useQueries({
    queries: installmentApplications.map((application) => ({
      queryKey: loanKeys.installments(application.loanId),
      queryFn: () => loanService.getInstallments(application.loanId),
      staleTime: 20_000,
    })),
  });

  const scoreCandidates = useMemo(
    () =>
      applications
        .filter((application) => !["PENDING", "REJECTED"].includes(application.status))
        .slice(0, 5),
    [applications],
  );

  const scoreQueries = useQueries({
    queries: scoreCandidates.map((application) => ({
      queryKey: loanKeys.score(application.loanId),
      queryFn: () => loanService.getScore(application.loanId),
      retry: false,
      staleTime: 60_000,
    })),
  });

  const summary = useMemo(() => {
    const activeLoanIds = new Set(
      installmentApplications
        .filter((application) => LOAN_FINANCIAL_STATUSES.includes(application.status))
        .map((application) => application.loanId),
    );
    const installments = installmentQueries.flatMap((query, index) => {
      const application = installmentApplications[index];
      return (query.data || []).map((installment) => ({
        ...installment,
        loanId: application?.loanId,
        loanProductName: application?.loanProductName,
        loanStatus: application?.status,
      }));
    });

    const paid = installments.filter((installment) => installment.status === "PAID");
    const activeUnpaid = installments.filter(
      (installment) =>
        activeLoanIds.has(installment.loanId) && installment.status !== "PAID",
    );
    const dueThisMonth = activeUnpaid.filter((installment) => isDateInMonth(installment.dueDate));
    const overdue = activeUnpaid.filter(
      (installment) =>
        installment.status === "OVERDUE" || isDateBeforeToday(installment.dueDate),
    );

    const sumAmount = (items) =>
      items.reduce((total, item) => total + Number(item.amount || 0), 0);

    const latestScore =
      scoreQueries
        .map((query) => query.data)
        .filter(Boolean)
        .sort((left, right) => new Date(right.scoredAt) - new Date(left.scoredAt))[0] || null;

    return {
      activeLoans: activeApplications.length,
      dueThisMonthAmount: sumAmount(dueThisMonth),
      dueThisMonthCount: dueThisMonth.length,
      totalPaid: sumAmount(paid),
      paidCount: paid.length,
      outstandingAmount: sumAmount(activeUnpaid),
      overdueCount: overdue.length,
      overdueAmount: sumAmount(overdue),
      overdueItems: overdue,
      latestScore,
      installments,
    };
  }, [activeApplications, installmentApplications, installmentQueries, scoreQueries]);

  const isLoading =
    listQuery.isLoading ||
    installmentQueries.some((query) => query.isLoading) ||
    scoreQueries.some((query) => query.isLoading);

  const isFetching =
    listQuery.isFetching ||
    installmentQueries.some((query) => query.isFetching) ||
    scoreQueries.some((query) => query.isFetching);

  const refetch = async () => {
    await listQuery.refetch();
    await Promise.all([
      ...installmentQueries.map((query) => query.refetch()),
      ...scoreQueries.map((query) => query.refetch()),
    ]);
  };

  return {
    userId,
    applications,
    recentApplications: applications.slice(0, 5),
    summary,
    isLoading,
    isFetching,
    isError: listQuery.isError,
    error: listQuery.error,
    refetch,
  };
}
