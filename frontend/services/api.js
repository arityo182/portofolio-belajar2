"use client";

import axios from "axios";
import { useAuthStore } from "@/stores/authStore";
import { readAuthCookies } from "@/lib/authCookies";

export const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "/api/v1";

const AUTH_ENDPOINT_PATTERN = /\/auth\/(login|register|refresh|forgot-password|reset-password)/;

export const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 20000,
  headers: {
    Accept: "application/json",
    "Content-Type": "application/json",
  },
});

const refreshClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: {
    Accept: "application/json",
    "Content-Type": "application/json",
  },
});

api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token || readAuthCookies().accessToken;
  if (token) {
    config.headers = config.headers || {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let refreshPromise = null;

function isAuthEndpoint(url = "") {
  return AUTH_ENDPOINT_PATTERN.test(url);
}

function redirectToLogin() {
  if (typeof window === "undefined") {
    return;
  }
  const { pathname, search } = window.location;
  if (
    pathname.startsWith("/login") ||
    pathname.startsWith("/register") ||
    pathname.startsWith("/forgot-password")
  ) {
    return;
  }
  const next = encodeURIComponent(`${pathname}${search}`);
  window.location.assign(`/login?expired=1&next=${next}`);
}

function handleSessionExpired() {
  useAuthStore.getState().logout();
  redirectToLogin();
}

export async function refreshAccessToken() {
  if (refreshPromise) {
    return refreshPromise;
  }

  refreshPromise = (async () => {
    const refreshToken =
      useAuthStore.getState().refreshToken || readAuthCookies().refreshToken;
    if (!refreshToken) {
      const error = new Error("Refresh token tidak tersedia");
      error.code = "NO_REFRESH_TOKEN";
      throw error;
    }

    const response = await refreshClient.post("/auth/refresh", { refreshToken });
    const payload = response.data?.data ?? response.data;
    if (!payload?.accessToken) {
      throw new Error("Respons refresh token tidak valid");
    }

    useAuthStore.getState().setAuth({
      accessToken: payload.accessToken,
      refreshToken: payload.refreshToken,
      expiresIn: payload.expiresIn,
    });
    return payload.accessToken;
  })().finally(() => {
    refreshPromise = null;
  });

  return refreshPromise;
}

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const { response, config } = error;

    if (
      response?.status !== 401 ||
      !config ||
      config._retry ||
      config.skipAuthRefresh ||
      isAuthEndpoint(config.url)
    ) {
      return Promise.reject(error);
    }

    config._retry = true;
    try {
      const accessToken = await refreshAccessToken();
      if (typeof config.headers?.set === "function") {
        config.headers.set("Authorization", `Bearer ${accessToken}`);
      } else {
        config.headers = {
          ...(config.headers || {}),
          Authorization: `Bearer ${accessToken}`,
        };
      }
      return api(config);
    } catch (refreshError) {
      const refreshStatus = refreshError?.response?.status;
      if (refreshError?.code === "NO_REFRESH_TOKEN" || refreshStatus === 401 || refreshStatus === 403) {
        handleSessionExpired();
      }
      return Promise.reject(error);
    }
  },
);

export function unwrap(response) {
  const body = response?.data;
  if (body && typeof body === "object" && Object.prototype.hasOwnProperty.call(body, "data")) {
    return body.data;
  }
  return body;
}

export function unwrapList(response, fallback = []) {
  const data = unwrap(response);
  if (Array.isArray(data)) {
    return data;
  }
  if (Array.isArray(data?.content)) {
    return data.content;
  }
  return fallback;
}
