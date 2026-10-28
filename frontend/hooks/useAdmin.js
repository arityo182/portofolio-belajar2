"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { adminService } from "@/services/adminService";
import { adminKeys, loanKeys } from "@/lib/queryKeys";
import { useReportDashboard } from "@/hooks/useReports";

export function useAdminUsers(filters = {}) {
  return useQuery({
    queryKey: adminKeys.users(filters),
    queryFn: () => adminService.getUsers(filters),
    placeholderData: (previous) => previous,
    staleTime: 20_000,
  });
}

export function useAdminProducts() {
  return useQuery({
    queryKey: adminKeys.products(),
    queryFn: adminService.getProducts,
    staleTime: 30_000,
  });
}

export function useAdminLoans() {
  return useQuery({
    queryKey: adminKeys.loans({ size: 100 }),
    queryFn: adminService.getLoans,
    staleTime: 20_000,
  });
}

export function useAdminDashboard() {
  const loansQuery = useAdminLoans();
  const reportQuery = useReportDashboard();
  const report = reportQuery.data;

  const metrics = {
    totalAum: Number(report?.activeAmount || 0),
    activeLoanCount: Number(report?.activeLoans || 0),
    nplRate: Number(report?.nplRatio || 0),
    defaultedAmount: Number(report?.defaultedAmount || 0),
    disbursedTodayAmount: Number(report?.disbursedTodayAmount || 0),
    disbursedTodayCount: 0,
    totalLoans: Number(report?.totalLoans || 0),
    pendingReview: Number(report?.pendingReview || 0),
    monthlyDisbursement: (report?.monthlyDisbursement || []).map((item) => ({
      label: item.label,
      disbursed: Number(item.disbursed || 0),
    })),
    monthlyDecisions: (report?.monthlyDecisions || []).map((item) => ({
      label: item.label,
      approved: Number(item.approved || 0),
      rejected: Number(item.rejected || 0),
    })),
    riskDistribution: (report?.riskDistribution || []).filter((item) => Number(item.value) > 0),
  };

  return {
    loans: loansQuery.data || [],
    metrics,
    isLoading: loansQuery.isLoading || reportQuery.isLoading,
    isError: loansQuery.isError || reportQuery.isError,
    error: loansQuery.error || reportQuery.error,
    refetch: () => Promise.all([loansQuery.refetch(), reportQuery.refetch()]),
  };
}

export function useCreateUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: adminService.createUser,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: adminKeys.usersAll() }),
  });
}

export function useUpdateUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ userId, payload }) => adminService.updateUser(userId, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: adminKeys.usersAll() }),
  });
}

export function useDeactivateUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: adminService.deactivateUser,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: adminKeys.usersAll() }),
  });
}

export function useCreateProduct() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: adminService.createProduct,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminKeys.products() });
      queryClient.invalidateQueries({ queryKey: loanKeys.products() });
    },
  });
}

export function useUpdateProduct() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ productId, payload }) => adminService.updateProduct(productId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminKeys.products() });
      queryClient.invalidateQueries({ queryKey: loanKeys.products() });
    },
  });
}

export function useDeactivateProduct() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: adminService.deactivateProduct,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminKeys.products() });
      queryClient.invalidateQueries({ queryKey: loanKeys.products() });
    },
  });
}
