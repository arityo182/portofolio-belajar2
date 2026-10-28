export const loanKeys = {
  all: ["loans"],
  products: () => [...loanKeys.all, "products"],
  lists: () => [...loanKeys.all, "list"],
  list: (filters = {}) => [...loanKeys.lists(), filters],
  detail: (loanId) => [...loanKeys.all, "detail", loanId],
  installments: (loanId) => [...loanKeys.all, "installments", loanId],
  score: (loanId) => [...loanKeys.all, "score", loanId],
  dashboard: (userId) => [...loanKeys.all, "dashboard", userId || "anonymous"],
};

export const paymentKeys = {
  all: ["payments"],
  status: (installmentId) => [...paymentKeys.all, "status", installmentId],
};

export const reviewerKeys = {
  all: ["reviewer"],
  queue: () => [...reviewerKeys.all, "queue"],
  history: (range = {}) => [...reviewerKeys.all, "history", range],
  borrower: (borrowerId) => [...reviewerKeys.all, "borrower", borrowerId],
};

export const adminKeys = {
  all: ["admin"],
  users: (filters = {}) => [...adminKeys.all, "users", filters],
  usersAll: () => [...adminKeys.all, "users"],
  products: () => [...adminKeys.all, "products"],
  loans: (filters = {}) => [...adminKeys.all, "loans", filters],
};

export const reportKeys = {
  all: ["reports"],
  dashboard: (range = {}) => [...reportKeys.all, "dashboard", range],
  loans: (filters = {}) => [...reportKeys.all, "loans", filters],
};

export const notificationKeys = {
  all: ["notifications"],
  list: (filters = {}) => [...notificationKeys.all, "list", filters],
  unreadCount: () => [...notificationKeys.all, "unread-count"],
};

export const borrowerKeys = {
  all: ["borrower"],
  profile: () => [...borrowerKeys.all, "profile"],
  profileByUser: (userId) => [...borrowerKeys.all, "profile", userId],
  document: (userId, kind) => [...borrowerKeys.all, "document", userId, kind],
};
