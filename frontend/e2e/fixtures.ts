import { test as base, expect, type Page } from '@playwright/test';

/** Account created by the backend's test-only reset endpoint (E2eSupport.java). */
export const USER = { username: 'e2e', password: 'Spokojny-Tydzien-2026' };

const BACKEND = 'http://localhost:8081';

/**
 * `test` with clean data before every test. `loggedIn` is a page with an active session (login over the API,
 * which shares cookies with the page), opened on the home screen.
 */
export const test = base.extend<{ cleanData: void; loggedIn: Page }>({
  cleanData: [
    async ({ request }, use) => {
      const response = await request.post(`${BACKEND}/api/e2e/reset`);
      expect(response.status(), 'e2e reset endpoint').toBe(204);
      await use();
    },
    { auto: true },
  ],
  loggedIn: async ({ page }, use) => {
    const response = await page.request.post('/api/auth/login', { data: USER });
    expect(response.ok(), 'login over the API').toBeTruthy();
    await page.goto('/');
    await use(page);
  },
});

export { expect };
