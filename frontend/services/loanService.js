import { api, unwrap } from "@/services/api";

function idempotencyConfig(idempotencyKey) {
  return {
    headers: {
      "Idempotency-Key": idempotencyKey,
    },
  };
}

export const loanService = {
  async getProducts() {
    const response = await api.get("/loans/products");
    return unwrap(response) || [];
  },

  async getApplications({ status, page = 0, size = 20 } = {}) {
    const response = await api.get("/loans/applications", {
      params: {
        status: status || undefined,
        page,
        size,
      },
    });
    return unwrap(response);
  },

  async getApplicationById(loanId) {
    const response = await api.get(`/loans/applications/${loanId}`);
    return unwrap(response);
  },

  async createApplication(payload, idempotencyKey) {
    const response = await api.post(
      "/loans/applications",
      payload,
      idempotencyConfig(idempotencyKey),
    );
    return unwrap(response);
  },

  async getInstallments(loanId) {
    const response = await api.get(`/loans/applications/${loanId}/installments`);
    return unwrap(response) || [];
  },

  async getScore(loanId) {
    const response = await api.get(`/loans/applications/${loanId}/score`);
    return unwrap(response);
  },

  async initiatePayment(installmentId, { paymentGateway, amount }, idempotencyKey) {
    const response = await api.post(
      `/payments/installments/${installmentId}/pay`,
      { paymentGateway, amount },
      idempotencyConfig(idempotencyKey),
    );
    return unwrap(response);
  },

  async getPaymentStatus(installmentId) {
    const response = await api.get(`/payments/installments/${installmentId}/status`);
    return unwrap(response);
  },

  async initiateDisbursement({ loanApplicationId, amount, paymentGateway }, idempotencyKey) {
    const response = await api.post(
      "/payments/disbursements",
      { loanApplicationId, amount, paymentGateway },
      idempotencyConfig(idempotencyKey),
    );
    return unwrap(response);
  },
};
