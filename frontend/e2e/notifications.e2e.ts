import { expect, test } from './fixtures';

test('REQ-033 reminder switches, day and times are saved', async ({ loggedIn: page }) => {
  await page.goto('/settings');
  const reminders = page.getByTestId('push-reminders');
  const habits = reminders.getByRole('checkbox', { name: 'Nawyki o ich godzinie' });
  await expect(habits).toHaveAttribute('aria-checked', 'true');

  await habits.click();
  await expect(habits).toHaveAttribute('aria-checked', 'false');
  await page.getByRole('radio', { name: 'So', exact: true }).click();
  await page.locator('input[name="checkinTime"]').fill('09:30');
  await page.locator('input[name="checkinTime"]').blur();
  await page.locator('input[name="streakTime"]').fill('21:15');
  await page.locator('input[name="streakTime"]').blur();
  await expect(page.locator('tl-toaster').getByText('Zapisano').first()).toBeVisible();

  await page.reload();
  await expect(page.getByTestId('push-reminders').getByRole('checkbox', { name: 'Nawyki o ich godzinie' }))
    .toHaveAttribute('aria-checked', 'false');
  await expect(page.getByRole('radio', { name: 'So', exact: true })).toHaveAttribute('aria-checked', 'true');
  await expect(page.locator('input[name="checkinTime"]')).toHaveValue('09:30');
  await expect(page.locator('input[name="streakTime"]')).toHaveValue('21:15');

  const settings = await (await page.request.get('/api/settings')).json();
  expect(settings).toMatchObject({
    pushHabits: false,
    pushCheckin: true,
    checkinReminderDay: 6,
    checkinReminderTime: '09:30',
    streakReminderTime: '21:15',
  });
});

test('REQ-033 push on a device needs the installed app (no service worker on the dev server)', async ({
  loggedIn: page,
}) => {
  await page.goto('/settings');
  await expect(page.getByTestId('push-device')).toContainText('ekranu początkowego');
});
