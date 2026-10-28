/** @jest-environment node */
import { http, HttpResponse } from "msw";
import { server } from "@/test/msw/server";
import { authService } from "@/services/authService";

beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe("authService", () => {
  it("logs in through the API gateway and unwraps the response envelope", async () => {
    server.use(
      http.post("http://localhost/api/v1/auth/login", async ({ request }) => {
        const body = await request.json();
        expect(body).toEqual({ email: "budi@example.id", password: "Koperasi#2026Aman" });
        return HttpResponse.json({
          success: true,
          data: { accessToken: "access", refreshToken: "refresh", tokenType: "Bearer", expiresIn: 900 },
          message: "Login berhasil",
        });
      }),
    );

    await expect(
      authService.login({ email: "budi@example.id", password: "Koperasi#2026Aman" }),
    ).resolves.toMatchObject({ accessToken: "access", refreshToken: "refresh" });
  });
});
