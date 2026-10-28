import { api, unwrap } from "@/services/api";

export const adminService = {
  async getUsers({ search, role, status, page = 0, size = 100 } = {}) {
    const response = await api.get("/admin/users", {
      params: {
        search: search || undefined,
        role: role || undefined,
        status: status || undefined,
        page,
        size,
      },
    });
    return unwrap(response);
  },

  async createUser(payload) {
    const response = await api.post("/admin/users", payload);
    return unwrap(response);
  },

  async updateUser(userId, payload) {
    const response = await api.put(`/admin/users/${userId}`, payload);
    return unwrap(response);
  },

  async deactivateUser(userId) {
    await api.delete(`/admin/users/${userId}`);
  },

  async getProducts() {
    const response = await api.get("/loans/admin/products");
    return unwrap(response) || [];
  },

  async createProduct(payload) {
    const response = await api.post("/loans/admin/products", payload);
    return unwrap(response);
  },

  async updateProduct(productId, payload) {
    const response = await api.put(`/loans/admin/products/${productId}`, payload);
    return unwrap(response);
  },

  async deactivateProduct(productId) {
    await api.delete(`/loans/admin/products/${productId}`);
  },

  async getLoans() {
    const response = await api.get("/loans/applications", {
      params: { page: 0, size: 100 },
    });
    return unwrap(response)?.content || [];
  },

  async getScore(loanId) {
    const response = await api.get(`/loans/applications/${loanId}/score`);
    return unwrap(response);
  },
};
