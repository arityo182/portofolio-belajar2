import { api, unwrap } from "@/services/api";

export const authService = {
  async login(credentials) {
    const response = await api.post("/auth/login", credentials);
    return unwrap(response);
  },

  async register(payload) {
    const response = await api.post("/auth/register", payload);
    return unwrap(response);
  },

  async refresh(refreshToken) {
    const response = await api.post("/auth/refresh", { refreshToken });
    return unwrap(response);
  },

  async logout(refreshToken) {
    await api.post("/auth/logout", refreshToken ? { refreshToken } : {});
  },

  async getMe() {
    const response = await api.get("/auth/me");
    return unwrap(response);
  },

  async forgotPassword(email) {
    const response = await api.post(
      "/auth/forgot-password",
      { email },
      { skipAuthRefresh: true },
    );
    return unwrap(response);
  },

  async resetPassword({ token, newPassword }) {
    const response = await api.post(
      "/auth/reset-password",
      { token, newPassword },
      { skipAuthRefresh: true },
    );
    return unwrap(response);
  },
};
