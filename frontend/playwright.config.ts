import { defineConfig, devices } from '@playwright/test';

/**
 * E2e tests (e2e/*.e2e.ts) against the real backend on a throwaway Postgres (Testcontainers, needs Docker).
 * Ports 8081/4300, so the dev servers (8080/4200) can keep running. Every test resets the data (see e2e/fixtures.ts),
 * hence a single worker.
 */
const isWindows = process.platform === 'win32';
const mvnw = isWindows ? '.\\mvnw.cmd' : './mvnw';

export default defineConfig({
  testDir: './e2e',
  testMatch: '**/*.e2e.ts',
  fullyParallel: false,
  workers: 1,
  forbidOnly: !!process.env['CI'],
  retries: process.env['CI'] ? 1 : 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: 'http://localhost:4300',
    locale: 'pl-PL',
    timezoneId: 'Europe/Warsaw',
    trace: 'retain-on-failure',
  },
  projects: [
    {
      // the app is used on an iPhone: phone-sized viewport with touch
      name: 'mobile-chromium',
      use: { ...devices['Desktop Chrome'], viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true },
    },
  ],
  webServer: [
    {
      command: `${mvnw} -q spring-boot:test-run -Dspring-boot.run.main-class=app.talia.e2e.E2eTaliaApplication`,
      cwd: '../backend',
      url: 'http://localhost:8081/actuator/health',
      timeout: 240_000,
      reuseExistingServer: !process.env['CI'],
      stdout: 'ignore',
      stderr: 'pipe',
    },
    {
      command: 'npx ng serve --port 4300 --proxy-config proxy.e2e.json',
      url: 'http://localhost:4300',
      timeout: 180_000,
      reuseExistingServer: !process.env['CI'],
    },
  ],
});
