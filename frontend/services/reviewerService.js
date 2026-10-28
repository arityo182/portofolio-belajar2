import { api, unwrap } from "@/services/api";

export const reviewerService = {
  async getQueue() {
    const response = await api.get("/loans/applications", {
      params: { status: "UNDER_REVIEW", page: 0, size: 100 },
    });
    return unwrap(response)?.content || [];
  },

  async getApplication(loanId) {
    const response = await api.get(`/loans/applications/${loanId}`);
    return unwrap(response);
  },

  async getScore(loanId) {
    const response = await api.get(`/loans/applications/${loanId}/score`);
    return unwrap(response);
  },

  async getBorrower(borrowerId) {
    const response = await api.get(`/admin/users/${borrowerId}`);
    return unwrap(response);
  },

  async decide(loanId, payload) {
    const response = await api.patch(`/loans/applications/${loanId}/status`, payload);
    return unwrap(response);
  },

  async getHistory() {
    const response = await api.get("/loans/applications", {
      params: { page: 0, size: 100 },
    });
    const content = unwrap(response)?.content || [];
    return content.filter(
      (application) =>
        Boolean(application.reviewedAt) &&
        ["APPROVED", "REJECTED"].includes(application.status),
    );
  },
};
