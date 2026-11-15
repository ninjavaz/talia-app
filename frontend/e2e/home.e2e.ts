import { expect, test } from './fixtures';

test('REQ-052 the app opens on the Week screen with today\'s habits', async ({ loggedIn: page }) => {
  await expect(page).toHaveURL(/\/week$/);
  await expect(page.getByText('Nawyki dziś')).toBeVisible();
  // the first bottom-nav item is the start screen
  await expect(page.getByRole('navigation').getByRole('link').first()).toHaveAttribute('href', '/week');
});

test('REQ-052 a habit is entered with one tap', async ({ loggedIn: page }) => {
  await page.locator('button[tlCheck]').first().click();

  await expect(page.getByTestId('today-progress')).toContainText('1/3');
});

test('REQ-052 a workout is entered with two taps', async ({ loggedIn: page }) => {
  await page.getByRole('button', { name: 'Dodaj trening' }).click();
  await page.getByRole('button', { name: 'Dodaj', exact: true }).click();

  await expect(page.locator('tl-toaster').getByText('Dodano')).toBeVisible();
  await expect(page.locator('tl-ring').first()).toContainText('1');
  await expect(page.locator('tl-list-item').filter({ hasText: 'Siłownia' })).toHaveCount(1);
});
