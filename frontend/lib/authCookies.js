export const ACCESS_TOKEN_COOKIE = "koperasi_access_token";
export const REFRESH_TOKEN_COOKIE = "koperasi_refresh_token";
export const ROLE_COOKIE = "koperasi_role";

const REFRESH_TOKEN_MAX_AGE = 60 * 60 * 24 * 7;
const ACCESS_TOKEN_FALLBACK_MAX_AGE = 60 * 15;

function isBrowser() {
  return typeof document !== "undefined";
}

function isSecureContext() {
  return typeof window !== "undefined" && window.location.protocol === "https:";
}

function writeCookie(name, value, maxAgeSeconds) {
  if (!isBrowser() || value === undefined || value === null) {
    return;
  }
  const parts = [
    `${name}=${encodeURIComponent(value)}`,
    "Path=/",
    `Max-Age=${Math.max(0, Math.floor(maxAgeSeconds))}`,
    "SameSite=Lax",
  ];
  if (isSecureContext()) {
    parts.push("Secure");
  }
  document.cookie = parts.join("; ");
}

function readCookie(name) {
  if (!isBrowser()) {
    return null;
  }
  const prefix = `${name}=`;
  const match = document.cookie
    .split(";")
    .map((part) => part.trim())
    .find((part) => part.startsWith(prefix));
  if (!match) {
    return null;
  }
  try {
    return decodeURIComponent(match.slice(prefix.length));
  } catch {
    return null;
  }
}

function deleteCookie(name) {
  if (!isBrowser()) {
    return;
  }
  const parts = [`${name}=`, "Path=/", "Max-Age=0", "SameSite=Lax"];
  if (isSecureContext()) {
    parts.push("Secure");
  }
  document.cookie = parts.join("; ");
}

export function writeAuthCookies({ accessToken, refreshToken, roles, expiresIn }) {
  if (!isBrowser()) {
    return;
  }
  if (accessToken) {
    writeCookie(
      ACCESS_TOKEN_COOKIE,
      accessToken,
      Number(expiresIn) > 0 ? Number(expiresIn) : ACCESS_TOKEN_FALLBACK_MAX_AGE,
    );
  }
  if (refreshToken) {
    writeCookie(REFRESH_TOKEN_COOKIE, refreshToken, REFRESH_TOKEN_MAX_AGE);
  }
  if (Array.isArray(roles) && roles.length > 0) {
    writeCookie(ROLE_COOKIE, roles.join(","), REFRESH_TOKEN_MAX_AGE);
  }
}

export function readAuthCookies() {
  return {
    accessToken: readCookie(ACCESS_TOKEN_COOKIE),
    refreshToken: readCookie(REFRESH_TOKEN_COOKIE),
    roles: readCookie(ROLE_COOKIE),
  };
}

export function clearAuthCookies() {
  deleteCookie(ACCESS_TOKEN_COOKIE);
  deleteCookie(REFRESH_TOKEN_COOKIE);
  deleteCookie(ROLE_COOKIE);
}
