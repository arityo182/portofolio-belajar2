"use client";

import { useEffect } from "react";
import { useAuthStore } from "@/stores/authStore";
import { authService } from "@/services/authService";
import { readAuthCookies } from "@/lib/authCookies";
import { isAuthError } from "@/lib/utils";

export function AuthProvider({ children }) {
  useEffect(() => {
    let active = true;

    async function bootstrap() {
      const cookies = readAuthCookies();

      if (!cookies.accessToken && !cookies.refreshToken) {
        useAuthStore.getState().setReady();
        return;
      }

      useAuthStore.getState().hydrate(cookies);

      try {
        const profile = await authService.getMe();
        if (active) {
          useAuthStore.getState().setUser(profile);
        }
      } catch (error) {
        if (active && (isAuthError(error) || error?.code === "NO_REFRESH_TOKEN")) {
          useAuthStore.getState().logout();
        }
      } finally {
        if (active) {
          useAuthStore.getState().setReady();
        }
      }
    }

    bootstrap();
    return () => {
      active = false;
    };
  }, []);

  return children;
}
