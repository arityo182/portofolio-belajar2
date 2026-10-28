import "@testing-library/jest-dom";

process.env.NEXT_PUBLIC_API_BASE_URL = "http://localhost/api/v1";

if (typeof window !== "undefined") {
  window.matchMedia =
    window.matchMedia ||
    function matchMedia(query) {
      return {
        matches: false,
        media: query,
        addEventListener: () => {},
        removeEventListener: () => {},
        addListener: () => {},
        removeListener: () => {},
        dispatchEvent: () => false,
      };
    };
  window.scrollTo = () => {};
  window.ResizeObserver =
    window.ResizeObserver ||
    class ResizeObserver {
      observe() {}
      unobserve() {}
      disconnect() {}
    };
  if (!window.URL.createObjectURL) {
    window.URL.createObjectURL = () => "blob:mock";
    window.URL.revokeObjectURL = () => {};
  }
}
