import type { Page } from '@playwright/test';
import { expect, test } from './fixtures';

/** Today in Europe/Warsaw (the backend's time zone), as yyyy-mm-dd. */
function today(): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'Europe/Warsaw' }).format(new Date());
}

async function addWorkout(page: Page, type: string, minutes: string): Promise<void> {
  await page.getByRole('button', { name: 'Dodaj trening' }).click();
  await page.getByRole('radio', { name: type }).click();
  await page.locator('input[name="duration"]').fill(minutes);
  await page.getByRole('button', { name: 'Dodaj', exact: true }).click();
  await expect(page.locator('tl-toaster').getByText('Dodano')).toBeVisible();
}

function workoutRow(page: Page, type: string) {
  return page.locator('tl-list-item').filter({ hasText: type }).filter({ hasText: 'min' });
}

test('REQ-020 a workout is added with type, date and duration', async ({ loggedIn: page }) => {
  await page.getByRole('button', { name: 'Dodaj trening' }).click();
  await expect(page.locator('input[name="date"]')).toHaveValue(today());
  await page.getByRole('button', { name: 'Zamknij' }).click();

  await addWorkout(page, 'Pilates', '30');

  await expect(workoutRow(page, 'Pilates')).toContainText('30 min');
});

test('REQ-021 the weekly goal is configurable and shows the progress', async ({ loggedIn: page }) => {
  await page.goto('/settings');
  const goals = page.locator('form').filter({ has: page.locator('input[name="workouts"]') });
  await goals.locator('input[name="workouts"]').fill('2');
  await goals.getByRole('button').click();
  await expect(page.locator('tl-toaster').getByText('Zapisano')).toBeVisible();

  await page.goto('/week');
  const ring = page.locator('tl-panel tl-ring');
  await expect(ring).toContainText('z 2');
  await addWorkout(page, 'Spacer', '40');
  await expect(ring).toContainText('1');
});

test('REQ-022 the list shows estimated kcal and the week shows the sum', async ({ loggedIn: page }) => {
  const response = await page.request.put('/api/checkins', { data: { date: today(), weightKg: 64 } });
  expect(response.ok()).toBeTruthy();
  await page.reload();

  await addWorkout(page, 'Siłownia', '60');
  await addWorkout(page, 'Pilates', '50');

  // 5.0 x 64 x 1 h = 320, 3.0 x 64 x 50/60 h = 160
  await expect(workoutRow(page, 'Siłownia')).toContainText('~320 kcal');
  await expect(workoutRow(page, 'Pilates')).toContainText('~160 kcal');
  await expect(page.getByTestId('kcal-total')).toContainText('480 kcal');
});
