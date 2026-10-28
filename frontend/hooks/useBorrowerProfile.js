"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { borrowerService } from "@/services/borrowerService";
import { borrowerKeys } from "@/lib/queryKeys";

export function useBorrowerProfile({ enabled = true } = {}) {
  return useQuery({
    queryKey: borrowerKeys.profile(),
    queryFn: borrowerService.getProfile,
    enabled,
    retry: (failureCount, error) => {
      const status = error?.response?.status;
      if (status === 404 || status === 403) {
        return false;
      }
      return failureCount < 2;
    },
    staleTime: 30_000,
  });
}

export function useBorrowerProfileByUser(userId, { enabled = true } = {}) {
  return useQuery({
    queryKey: borrowerKeys.profileByUser(userId),
    queryFn: () => borrowerService.getProfileByUser(userId),
    enabled: Boolean(userId) && enabled,
    retry: (failureCount, error) => {
      const status = error?.response?.status;
      if (status === 404 || status === 403) {
        return false;
      }
      return failureCount < 2;
    },
    staleTime: 30_000,
  });
}

export function useVerifyBorrowerProfile(userId) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => borrowerService.verifyProfile(userId),
    onSuccess: (data) => {
      queryClient.setQueryData(borrowerKeys.profileByUser(userId), data);
      queryClient.invalidateQueries({ queryKey: borrowerKeys.all });
    },
  });
}

export function useBorrowerDocument(userId, kind, { enabled = true } = {}) {
  return useQuery({
    queryKey: borrowerKeys.document(userId, kind),
    queryFn: () => borrowerService.getDocument(userId, kind),
    enabled: Boolean(userId) && Boolean(kind) && enabled,
    retry: false,
    staleTime: 5 * 60_000,
  });
}

export function useSaveBorrowerProfile() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: borrowerService.upsertProfile,
    onSuccess: (data) => {
      queryClient.setQueryData(borrowerKeys.profile(), data);
      queryClient.invalidateQueries({ queryKey: borrowerKeys.all });
    },
  });
}

export function useUploadBorrowerDocuments() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: borrowerService.uploadDocuments,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: borrowerKeys.all });
    },
  });
}
