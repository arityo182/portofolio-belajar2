"use client";

import { create } from "zustand";
import { clearAuthCookies, writeAuthCookies } from "@/lib/authCookies";

const initialState = {
  token: null,
  refreshToken: null,
  user: null,
  isAuthenticated: false,
  isReady: false,
};

export const useAuthStore = create((set, get) => ({
  ...initialState,

  setAuth: ({ accessToken, refreshToken, user, expiresIn }) => {
    const nextUser = user !== undefined ? user : get().user;
    writeAuthCookies({
      accessToken,
      refreshToken,
      roles: nextUser?.roles,
      expiresIn,
    });
    set({
      token: accessToken || null,
      refreshToken: refreshToken || get().refreshToken,
      user: nextUser || null,
      isAuthenticated: Boolean(accessToken || refreshToken),
      isReady: true,
    });
  },

  setUser: (user) => {
    const state = get();
    if (state.token || state.refreshToken) {
      writeAuthCookies({
        accessToken: state.token,
        refreshToken: state.refreshToken,
        roles: user?.roles,
      });
    }
    set({ user: user || null });
  },

  hydrate: ({ accessToken, refreshToken }) => {
    set({
      token: accessToken || null,
      refreshToken: refreshToken || null,
      isAuthenticated: Boolean(accessToken || refreshToken),
    });
  },

  setReady: () => set({ isReady: true }),

  logout: () => {
    clearAuthCookies();
    set({ ...initialState, isReady: true });
  },
}));

export function selectHasRole(role) {
  return (state) => {
    const roles = state.user?.roles || [];
    return roles.map((item) => String(item).toUpperCase()).includes(String(role).toUpperCase());
  };
}
