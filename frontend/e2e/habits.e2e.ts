import type { Locator, Page } from '@playwright/test';
import { expect, test } from './fixtures';

/** A date `days` before today in Europe/Warsaw (the backend's time zone), as yyyy-mm-dd. */
function daysAgo(days: number): string {
  const date = new Date(Date.now() - days * 24 * 60 * 60 * 1000);
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'Europe/Warsaw' }).format(date);
}

/** Habit rows of the daily list on the Week screen, in display order. */
function habitRows(page: Page): Locator {
  return page.locator('tl-list-item').filter({ has: page.locator('button[tlCheck]') });
}

function habitCheck(page: Page, name: string | RegExp): Locator {
  return habitRows(page).filter({ hasText: name }).locator('button[tlCheck]');
}

/** Drags with small mouse steps, as the CDK drag & drop needs movement past its threshold. */
async function drag(page: Page, from: Locator, to: Locator): Promise<void> {
  const a = (await from.boundingBox())!;
  const b = (await to.boundingBox())!;
  await page.mouse.move(a.x + a.width / 2, a.y + a.height / 2);
  await page.mouse.down();
  await page.mouse.move(a.x + a.width / 2, a.y + a.height / 2 - 10, { steps: 5 });
  await page.mouse.move(b.x + b.width / 2, b.y + 2, { steps: 20 });
  await page.mouse.up();
}

const STEPS = /Kroki:\s8\s000/;

test('REQ-010 a new account has default habits and they can be reordered by dragging', async ({ loggedIn: page }) => {
  await expect(habitRows(page)).toHaveText([/Suplementy/, /Picie wody/, STEPS]);

  await page.goto('/settings');
  const settingsRows = page.locator('tl-list-item').filter({ has: page.locator('[cdkDragHandle]') });
  await expect(settingsRows).toHaveCount(3);
  const handle = settingsRows.filter({ hasText: STEPS }).locator('[cdkDragHandle]');
  await drag(page, handle, settingsRows.first());
  await expect(page.locator('tl-toaster').getByText('Zapisano')).toBeVisible();
  await expect(settingsRows.first()).toContainText(STEPS);

  await page.goto('/week');
  await expect(habitRows(page)).toHaveText([STEPS, /Suplementy/, /Picie wody/]);
});

test('REQ-011 a habit can have a time that is shown on the daily list', async ({ loggedIn: page }) => {
  await page.goto('/settings');
  await page.getByRole('button', { name: 'Dodaj nawyk', exact: true }).click();
  await page.locator('input[name="name"]').fill('Witamina D');
  await page.locator('input[name="time"]').fill('08:00');
  await page.getByRole('button', { name: 'Zapisz' }).last().click();
  await expect(page.locator('tl-toaster').getByText('Dodano')).toBeVisible();

  await page.goto('/week');
  const row = habitRows(page).filter({ hasText: 'Witamina D' });
  await expect(row).toContainText('8:00');
});

test('REQ-012 a habit is checked and unchecked with one tap', async ({ loggedIn: page }) => {
  const check = habitCheck(page, 'Suplementy');
  await check.click();
  await expect(check).toHaveAttribute('aria-checked', 'true');

  await page.reload();
  await expect(habitCheck(page, 'Suplementy')).toHaveAttribute('aria-checked', 'true');
  await habitCheck(page, 'Suplementy').click();
  await expect(habitCheck(page, 'Suplementy')).toHaveAttribute('aria-checked', 'false');
  await page.reload();
  await expect(habitCheck(page, 'Suplementy')).toHaveAttribute('aria-checked', 'false');
});

test('REQ-013 the steps habit shows the daily steps goal', async ({ loggedIn: page }) => {
  await expect(habitRows(page).filter({ hasText: STEPS })).toHaveCount(1);

  await page.goto('/settings');
  const goals = page.locator('form').filter({ has: page.locator('input[name="steps"]') });
  await goals.locator('input[name="steps"]').fill('10000');
  await goals.getByRole('button').click();
  await expect(page.locator('tl-toaster').getByText('Zapisano')).toBeVisible();

  await page.goto('/week');
  await expect(habitRows(page).filter({ hasText: /Kroki:\s10\s000/ })).toHaveCount(1);
  await habitCheck(page, /Kroki/).click();
  await expect(habitCheck(page, /Kroki/)).toHaveAttribute('aria-checked', 'true');
});

test('REQ-014 the daily progress shows checked habits out of all', async ({ loggedIn: page }) => {
  const progress = page.getByTestId('today-progress');
  await expect(progress).toContainText('0/3');

  await habitCheck(page, 'Suplementy').click();
  await habitCheck(page, 'Picie wody').click();
  await expect(progress).toContainText('2/3');
  await expect(progress.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '2');

  await habitCheck(page, STEPS).click();
  await expect(progress).toContainText('Wszystko na dziś zrobione');
});

test('REQ-015 the streak counts completed days and grows when today is completed', async ({ loggedIn: page }) => {
  const habits: { id: number }[] = await (await page.request.get('/api/habits')).json();
  for (const days of [1, 2, 3]) {
    for (const h of habits) {
      const response = await page.request.put(`/api/habits/${h.id}/checks/${daysAgo(days)}`);
      expect(response.ok()).toBeTruthy();
    }
  }
  await page.reload();

  const streak = page.getByTestId('streak');
  await expect(streak).toContainText('3');

  for (const name of ['Suplementy', 'Picie wody', STEPS]) {
    await habitCheck(page, name).click();
  }
  await expect(streak).toContainText('4');
});
