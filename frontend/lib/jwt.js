function decodeBase64Url(value) {
  const normalized = value.replace(/-/g, "+").replace(/_/g, "/");
  const padding = normalized.length % 4 === 0 ? "" : "=".repeat(4 - (normalized.length % 4));
  return atob(`${normalized}${padding}`);
}

export function getJwtPayload(token) {
  if (!token || typeof atob === "undefined") {
    return null;
  }
  const segments = token.split(".");
  if (segments.length < 2) {
    return null;
  }
  try {
    return JSON.parse(decodeBase64Url(segments[1]));
  } catch {
    return null;
  }
}

export function getJwtExpirationMs(token) {
  const payload = getJwtPayload(token);
  return payload?.exp ? payload.exp * 1000 : null;
}

export function isTokenExpiring(token, thresholdSeconds = 60) {
  const expiresAt = getJwtExpirationMs(token);
  if (!expiresAt) {
    return true;
  }
  return expiresAt - Date.now() <= thresholdSeconds * 1000;
}
