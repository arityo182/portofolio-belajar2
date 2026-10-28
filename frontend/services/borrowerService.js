import { api, unwrap } from "@/services/api";

export const borrowerService = {
  async getProfile() {
    const response = await api.get("/borrowers/profile");
    return unwrap(response);
  },

  async upsertProfile(payload) {
    const response = await api.post("/borrowers/profile", payload);
    return unwrap(response);
  },

  async uploadDocuments({ ktpPhoto, selfiePhoto }) {
    const formData = new FormData();
    formData.append("ktpPhoto", ktpPhoto);
    formData.append("selfiePhoto", selfiePhoto);
    const response = await api.post("/borrowers/profile/verify-ktp", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    return unwrap(response);
  },

  async getProfileByUser(userId) {
    const response = await api.get(`/borrowers/${userId}/profile`);
    return unwrap(response);
  },

  async verifyProfile(userId) {
    const response = await api.post(`/borrowers/${userId}/verify`);
    return unwrap(response);
  },

  async getDocument(userId, kind) {
    const response = await api.get(`/borrowers/${userId}/documents/${kind}`, {
      responseType: "blob",
    });
    return response.data;
  },
};
