import type { APIRequestContext, Page } from '@playwright/test';
import { expect, test } from './fixtures';

/** A date `days` before today in Europe/Warsaw (the backend's time zone), as yyyy-mm-dd. */
function daysAgo(days: number): string {
  const date = new Date(Date.now() - days * 24 * 60 * 60 * 1000);
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'Europe/Warsaw' }).format(date);
}

/** `date` (yyyy-mm-dd) moved by `days`. */
function addDays(date: string, days: number): string {
  const d = new Date(`${date}T12:00:00Z`);
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString().slice(0, 10);
}

/** Monday of the ISO week of `date` (yyyy-mm-dd). */
function monday(date: string): string {
  const dow = new Date(`${date}T12:00:00Z`).getUTCDay() || 7;
  return addDays(date, 1 - dow);
}

/** A weight from Apple Health (the second source of weights) through the personal sync token. */
async function healthWeight(request: APIRequestContext, ts: string, value: number): Promise<void> {
  const { token } = await (await request.post('/api/auth/sync-token')).json();
  const response = await request.post('/api/health-sync', {
    headers: { Authorization: `Bearer ${token}` },
    data: { weight: [{ ts, value }] },
  });
  expect(response.ok(), 'health sync').toBeTruthy();
}

async function waistId(request: APIRequestContext): Promise<number> {
  const parts = await (await request.get('/api/body-parts')).json();
  return parts[0].id;
}

async function checkin(request: APIRequestContext, date: string, weightKg: number, waistCm?: number): Promise<void> {
  const measurements = waistCm === undefined ? [] : [{ partId: await waistId(request), valueCm: waistCm }];
  const response = await request.put('/api/checkins', { data: { date, weightKg, measurements } });
  expect(response.ok(), `check-in ${date}`).toBeTruthy();
}

/** The headline tiles on the Progress screen. */
function tile(page: Page, label: string) {
  return page.locator('tl-stat').filter({ hasText: label });
}

test('REQ-001 weekly check-in saves the weight with its date', async ({ loggedIn: page }) => {
  await page.goto('/progress');
  await page.getByRole('button', { name: 'Dodaj pomiar' }).click();
  await expect(page.locator('input[name="date"]')).toHaveValue(daysAgo(0));
  await page.locator('input[name="weight"]').fill('68,4');
  await page.getByRole('button', { name: 'Zapisz' }).click();

  await expect(page.getByText('Zapisano')).toBeVisible();
  await expect(page.locator('button[tlListItem]').filter({ hasText: '68,4 kg' })).toHaveCount(1);
  await expect(tile(page, 'Waga')).toContainText('68,4');
});

test('REQ-002 a new account measures the waist and it can be deleted', async ({ loggedIn: page }) => {
  await page.goto('/progress');
  await expect(tile(page, 'Talia')).toBeVisible();

  await page.goto('/settings');
  const waist = page.locator('tl-list-item').filter({ hasText: 'Talia' });
  await expect(waist).toHaveCount(1);
  page.once('dialog', (dialog) => dialog.accept());
  await waist.getByRole('button', { name: 'Usuń' }).click();
  await expect(waist).toHaveCount(0);

  await page.goto('/progress');
  await expect(tile(page, 'Obwody')).toContainText('Dodaj pomiary w Ustawieniach');
});

test('REQ-003 the chart shows one point per week', async ({ loggedIn: page }) => {
  await page.goto('/progress');
  // two weights on different days of the same week (two weeks ago) count as one point
  const week = monday(daysAgo(14));
  await healthWeight(page.request, `${week}T07:00:00`, 70.4);
  await checkin(page.request, addDays(week, 1), 70);
  await checkin(page.request, daysAgo(7), 69.2);
  await checkin(page.request, daysAgo(0), 68.6);
  await page.reload();

  // weight is the chart's default metric
  await expect(page.locator('tl-trend-chart circle')).toHaveCount(3);
});

test('REQ-004 progress shows the difference since the first measurement', async ({ loggedIn: page }) => {
  await page.goto('/progress');
  await expect(tile(page, 'Waga')).toContainText('brak danych');

  await checkin(page.request, daysAgo(14), 70, 82);
  await checkin(page.request, daysAgo(0), 68.5, 80.5);
  await page.reload();

  await expect(tile(page, 'Waga')).toContainText('68,5');
  await expect(tile(page, 'Waga')).toContainText('▼ 1,5 kg od startu');
  await expect(tile(page, 'Talia')).toContainText('80,5');
  await expect(tile(page, 'Talia')).toContainText('▼ 1,5 cm od startu');
});
