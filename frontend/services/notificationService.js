import { api, unwrap } from "@/services/api";

export const notificationService = {
  async list({ isRead, page = 0, size = 20 } = {}) {
    const response = await api.get("/notifications", {
      params: {
        isRead: typeof isRead === "boolean" ? isRead : undefined,
        page,
        size,
      },
    });
    return unwrap(response);
  },

  async markRead(id) {
    const response = await api.patch(`/notifications/${id}/read`);
    return unwrap(response);
  },

  async markAllRead() {
    const response = await api.patch("/notifications/read-all");
    return unwrap(response);
  },

  async unreadCount() {
    const response = await api.get("/notifications/unread-count");
    return unwrap(response);
  },
};
