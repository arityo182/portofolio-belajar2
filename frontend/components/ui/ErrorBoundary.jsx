"use client";

import { Component } from "react";
import { AlertTriangle, RotateCcw } from "lucide-react";
import { Button } from "@/components/ui/Button";

export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
    this.handleReset = this.handleReset.bind(this);
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    if (typeof console !== "undefined") {
      console.error("Unhandled UI error", error, errorInfo);
    }
    this.props.onError?.(error, errorInfo);
  }

  handleReset() {
    this.setState({ hasError: false, error: null });
    this.props.onReset?.();
  }

  render() {
    if (!this.state.hasError) {
      return this.props.children;
    }

    const { fallback, title, description } = this.props;

    if (typeof fallback === "function") {
      return fallback({ error: this.state.error, reset: this.handleReset });
    }

    return (
      <div
        role="alert"
        className="mx-auto flex max-w-lg flex-col items-center rounded-2xl border border-line bg-surface-raised px-6 py-10 text-center shadow-card"
      >
        <span className="flex h-12 w-12 items-center justify-center rounded-2xl bg-danger-50 text-danger-600 dark:bg-danger-700/15 dark:text-danger-200">
          <AlertTriangle className="h-6 w-6" aria-hidden="true" />
        </span>
        <h2 className="mt-4 text-lg font-semibold text-content">
          {title || "Terjadi kesalahan pada halaman ini"}
        </h2>
        <p className="mt-2 max-w-sm text-sm leading-6 text-content-muted">
          {description ||
            "Silakan muat ulang halaman. Jika masalah berlanjut, hubungi tim dukungan koperasi."}
        </p>
        <Button
          className="mt-6"
          variant="secondary"
          onClick={this.handleReset}
          leftIcon={<RotateCcw className="h-4 w-4" />}
        >
          Coba lagi
        </Button>
      </div>
    );
  }
}
