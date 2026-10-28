"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";

const STORAGE_KEY = "koperasi-theme";
const ThemeContext = createContext({
  theme: "light",
  mounted: false,
  setTheme: () => {},
  toggleTheme: () => {},
});

function applyThemeToDocument(theme) {
  if (typeof document === "undefined") {
    return;
  }
  const root = document.documentElement;
  const isDark = theme === "dark";
  root.classList.toggle("dark", isDark);
  root.dataset.theme = isDark ? "dark" : "light";
}

export function ThemeProvider({ children }) {
  const [theme, setThemeState] = useState("light");
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    const initial = document.documentElement.dataset.theme === "dark" ? "dark" : "light";
    setThemeState(initial);
    setMounted(true);
  }, []);

  const setTheme = useCallback((next) => {
    const normalized = next === "dark" ? "dark" : "light";
    applyThemeToDocument(normalized);
    try {
      window.localStorage.setItem(STORAGE_KEY, normalized);
    } catch {
      // Storage can be unavailable in private mode; the in-memory theme still applies.
    }
    setThemeState(normalized);
  }, []);

  const toggleTheme = useCallback(() => {
    setTheme(theme === "dark" ? "light" : "dark");
  }, [setTheme, theme]);

  const value = useMemo(
    () => ({
      theme: mounted ? theme : "light",
      resolvedTheme: mounted ? theme : "light",
      mounted,
      setTheme,
      toggleTheme,
    }),
    [mounted, setTheme, theme, toggleTheme],
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useTheme() {
  return useContext(ThemeContext);
}
