import { expect, test, USER } from './fixtures';

test('login form opens the app', async ({ page }) => {
  await page.goto('/');
  await expect(page).toHaveURL(/\/login/);

  await page.locator('input[name="username"]').first().fill(USER.username);
  await page.locator('input[name="password"]').first().fill(USER.password);
  await page.getByRole('button', { name: 'Zaloguj' }).click();

  await expect(page).toHaveURL(/\/week/);
  await expect(page.getByRole('navigation')).toBeVisible();
});

test('session from the API login is kept', async ({ loggedIn: page }) => {
  await expect(page).toHaveURL(/\/week/);
});
