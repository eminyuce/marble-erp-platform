// @ts-check
const { test, expect } = require('@playwright/test');
const { ADMIN_CREDENTIALS, loginAsAdmin } = require('./helpers/auth.helper');
const { setupErrorTracking } = require('./helpers/audit.helper');

test.describe('Authentication & Public Routes', () => {

  test('Public health endpoints return 200 OK', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);

    const resHealth = await page.goto('/health');
    expect(resHealth?.status()).toBe(200);

    const resHealthSlash = await page.goto('/health/');
    expect(resHealthSlash?.status()).toBe(200);

    await errorTracker.assertCleanState();
  });

  test('Public login page loads with CSRF and correct elements', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);

    await page.goto('/account/adminlogin/', { waitUntil: 'domcontentloaded' });
    expect(await page.title()).toContain('Özerler Mermer');

    const usernameInput = page.locator('#username');
    const passwordInput = page.locator('#password');
    const submitButton = page.locator('button[type="submit"]');

    await expect(usernameInput).toBeVisible();
    await expect(passwordInput).toBeVisible();
    await expect(submitButton).toBeVisible();

    // Verify CSRF input exists
    const csrfInput = page.locator('input[name="_csrf"]');
    await expect(csrfInput).toHaveCount(1);

    await errorTracker.assertCleanState();
  });

  test('Login with invalid credentials displays error message', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);

    await page.goto('/account/adminlogin/', { waitUntil: 'domcontentloaded' });
    await page.fill('#username', 'wronguser@example.com');
    await page.fill('#password', 'WrongPassword123');
    await page.click('button[type="submit"]');

    await page.waitForURL('**/account/adminlogin/**', { timeout: 8000 });
    const errorAlert = page.locator('.admin-login__alert--error');
    await expect(errorAlert).toBeVisible();

    await errorTracker.assertCleanState();
  });

  test('Login with valid admin credentials successfully redirects to dashboard', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);

    await loginAsAdmin(page);
    expect(page.url()).toContain('/admin/dashboard');

    // Verify dashboard user info is present
    const userBadge = page.locator('text=admin@eimece.test');
    await expect(userBadge.first()).toBeVisible({ timeout: 5000 });

    await errorTracker.assertCleanState();
  });

  test('Unauthenticated access to protected route redirects to login', async ({ page }) => {
    // Clear cookies/context to simulate guest
    await page.context().clearCookies();
    await page.goto('/admin/dashboard');

    // Should be redirected to /account/adminlogin
    expect(page.url()).toContain('/account/adminlogin');
  });

  test('Access denied page loads cleanly', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);

    await page.goto('/access-denied');
    const heading = page.locator('h1, h2');
    await expect(heading.first()).toBeVisible();

    await errorTracker.assertCleanState();
  });

  test('Change password page loads with form fields', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);
    await loginAsAdmin(page);

    await page.goto('/account/change-password');
    const oldPasswordInput = page.locator('input[name="currentPassword"]');
    const newPasswordInput = page.locator('input[name="newPassword"]');
    const confirmPasswordInput = page.locator('input[name="confirmPassword"]');

    await expect(oldPasswordInput).toBeVisible();
    await expect(newPasswordInput).toBeVisible();
    await expect(confirmPasswordInput).toBeVisible();

    await errorTracker.assertCleanState();
  });

});
