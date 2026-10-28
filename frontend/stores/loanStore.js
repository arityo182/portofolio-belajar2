"use client";

import { create } from "zustand";

const initialState = {
  step: 1,
  product: null,
  amount: "",
  tenorMonths: "",
  purpose: "",
  documents: {
    ktp: null,
    selfie: null,
  },
  idempotencyKey: null,
};

function generateIdempotencyKey() {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }
  return `loan-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}

export const useLoanStore = create((set, get) => ({
  ...initialState,

  setStep: (step) =>
    set({
      step: Math.min(4, Math.max(1, Number(step) || 1)),
    }),

  selectProduct: (product) =>
    set({
      product,
      amount: "",
      tenorMonths: product?.minTenorMonths ?? "",
      idempotencyKey: null,
    }),

  updateDraft: (partial) =>
    set({
      ...partial,
      idempotencyKey: null,
    }),

  setDocument: (kind, metadata) =>
    set((state) => ({
      documents: {
        ...state.documents,
        [kind]: metadata,
      },
    })),

  clearDocument: (kind) =>
    set((state) => ({
      documents: {
        ...state.documents,
        [kind]: null,
      },
    })),

  /**
   * The key must stay stable while the user retries the same submission, but must change
   * whenever the request payload changes. Callers clear it on every draft mutation.
   */
  ensureIdempotencyKey: () => {
    const existing = get().idempotencyKey;
    if (existing) {
      return existing;
    }
    const key = generateIdempotencyKey();
    set({ idempotencyKey: key });
    return key;
  },

  invalidateIdempotencyKey: () => set({ idempotencyKey: null }),

  resetDraft: () => set({ ...initialState }),
}));
