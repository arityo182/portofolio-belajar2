import { NextResponse } from "next/server";

const ACCESS_TOKEN_COOKIE = "koperasi_access_token";
const REFRESH_TOKEN_COOKIE = "koperasi_refresh_token";

const AUTH_PAGES = new Set(["/login", "/register", "/forgot-password", "/reset-password"]);

const PROTECTED_PREFIXES = [
  "/dashboard",
  "/loans",
  "/investments",
  "/payments",
  "/notifications",
  "/reports",
  "/users",
  "/profile",
  "/settings",
  "/admin",
  "/reviewer",
];

function isProtectedPath(pathname) {
  return PROTECTED_PREFIXES.some(
    (prefix) => pathname === prefix || pathname.startsWith(`${prefix}/`),
  );
}

export function middleware(request) {
  const { pathname, search } = request.nextUrl;
  const hasSession = Boolean(
    request.cookies.get(ACCESS_TOKEN_COOKIE)?.value ||
      request.cookies.get(REFRESH_TOKEN_COOKIE)?.value,
  );

  if (AUTH_PAGES.has(pathname) && hasSession) {
    return NextResponse.redirect(new URL("/dashboard", request.url));
  }

  if (isProtectedPath(pathname) && !hasSession) {
    const loginUrl = new URL("/login", request.url);
    loginUrl.searchParams.set("next", `${pathname}${search}`);
    return NextResponse.redirect(loginUrl);
  }

  const response = NextResponse.next();
  response.headers.set("x-pathname", pathname);
  return response;
}

export const config = {
  matcher: [
    "/((?!api|_next/static|_next/image|favicon.ico|.*\\.(?:svg|png|jpg|jpeg|gif|webp|ico|css|js|woff|woff2|ttf|otf)$).*)",
  ],
};
