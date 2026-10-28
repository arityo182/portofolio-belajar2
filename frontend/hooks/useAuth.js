"use client";

import { useCallback } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/stores/authStore";
import { authService } from "@/services/authService";
import { getRoleLabel } from "@/lib/constants";

export function useAuth() {
  const router = useRouter();
  const token = useAuthStore((state) => state.token);
  const refreshToken = useAuthStore((state) => state.refreshToken);
  const user = useAuthStore((state) => state.user);
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const isReady = useAuthStore((state) => state.isReady);

  const login = useCallback(async (credentials) => {
    const tokens = await authService.login(credentials);
    if (!tokens?.accessToken) {
      throw new Error("Respons login tidak valid");
    }

    useAuthStore.getState().setAuth({
      accessToken: tokens.accessToken,
      refreshToken: tokens.refreshToken,
      expiresIn: tokens.expiresIn,
    });

    try {
      const profile = await authService.getMe();
      useAuthStore.getState().setUser(profile);
      useAuthStore.getState().setReady();
      return { tokens, user: profile };
    } catch (error) {
      useAuthStore.getState().logout();
      throw error;
    }
  }, []);

  const register = useCallback(async (payload) => {
    const result = await authService.register(payload);
    return result;
  }, []);

  const logout = useCallback(
    async ({ redirect = true } = {}) => {
      const currentRefreshToken = useAuthStore.getState().refreshToken;
      try {
        if (currentRefreshToken) {
          await authService.logout(currentRefreshToken);
        }
      } catch {
        // Local session must always be cleared, even when the API is unreachable.
      } finally {
        useAuthStore.getState().logout();
        if (redirect) {
          router.replace("/login");
        }
      }
    },
    [router],
  );

  const refreshUser = useCallback(async () => {
    const profile = await authService.getMe();
    useAuthStore.getState().setUser(profile);
    return profile;
  }, []);

  const hasRole = useCallback(
    (role) => {
      const roles = user?.roles || [];
      return roles.map((item) => String(item).toUpperCase()).includes(String(role).toUpperCase());
    },
    [user],
  );

  const roles = (user?.roles || []).map((role) => ({
    value: role,
    label: getRoleLabel(role),
  }));

  return {
    token,
    refreshToken,
    user,
    roles,
    isAuthenticated,
    isReady,
    isLoading: !isReady,
    hasRole,
    login,
    register,
    logout,
    refreshUser,
  };
}
