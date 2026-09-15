// @ts-check
const { test, expect } = require('@playwright/test');
const { loginAsAdmin } = require('./helpers/auth.helper');
const { setupErrorTracking } = require('./helpers/audit.helper');
const { waitForTabulator } = require('./helpers/grid.helper');

test.describe('Admin Settings & Master Definitions (Tanımlar & Ayarlar)', () => {

  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test('User management list, create, and detail pages load cleanly', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);

    // List
    await page.goto('/admin/users', { waitUntil: 'domcontentloaded' });
    expect(page.url()).toContain('/admin/users');
    const rowCount = await waitForTabulator(page, '#users-table');
    expect(rowCount).toBeGreaterThan(0);

    // Create form
    await page.goto('/admin/users/create', { waitUntil: 'domcontentloaded' });
    expect(page.url()).toContain('/admin/users/create');
    const emailInput = page.locator('input[name="email"]');
    await expect(emailInput).toBeVisible();

    // User Detail & Edit (admin user id 1)
    await page.goto('/admin/users/1', { waitUntil: 'networkidle' });
    expect(page.url()).toContain('/admin/users/1');

    await page.goto('/admin/users/1/edit', { waitUntil: 'networkidle' });
    expect(page.url()).toContain('/admin/users/1/edit');

    // Reset password
    await page.goto('/admin/users/1/reset-password', { waitUntil: 'networkidle' });
    expect(page.url()).toContain('/admin/users/1/reset-password');

    await errorTracker.assertCleanState();
  });

  test('System Settings page loads and switches between tabs correctly', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);

    await page.goto('/admin/settings', { waitUntil: 'networkidle' });
    expect(page.url()).toContain('/admin/settings');

    // Tab buttons
    const rateLimitTab = page.locator('button:has-text("Hız Sınırları"), button:has-text("Rate Limiting")');
    if (await rateLimitTab.count() > 0) {
      await rateLimitTab.first().click();
      const content = await page.content();
      expect(content).toContain('rate-limiting');
    }

    await errorTracker.assertCleanState();
  });

  test('Definitions Hub page loads all definition categories', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);

    await page.goto('/admin/definitions', { waitUntil: 'domcontentloaded' });
    expect(page.url()).toContain('/admin/definitions');

    const content = await page.content();
    expect(content).toContain('Makineler');
    expect(content).toContain('Stok Sahaları');
    expect(content).toContain('Ocaklar');
    expect(content).toContain('Müşteriler');
    expect(content).toContain('Tedarikçiler');
    expect(content).toContain('Masraf Merkezleri');

    await errorTracker.assertCleanState();
  });

  test('All Master Definition grids and create forms load without errors', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);

    const definitions = [
      { name: 'machines', tableId: '#machines-table' },
      { name: 'stock-locations', tableId: '#stock-locations-table' },
      { name: 'quarries', tableId: '#quarries-table' },
      { name: 'customers', tableId: '#customers-table' },
      { name: 'suppliers', tableId: '#suppliers-table' },
      { name: 'cost-centers', tableId: '#cost-centers-table' }
    ];

    for (const def of definitions) {
      // Grid page
      await page.goto(`/admin/definitions/${def.name}`, { waitUntil: 'domcontentloaded' });
      expect(page.url()).toContain(`/admin/definitions/${def.name}`);
      const rowCount = await waitForTabulator(page, def.tableId);
      expect(rowCount).toBeGreaterThan(0);

      // Create page
      await page.goto(`/admin/definitions/${def.name}/create`, { waitUntil: 'domcontentloaded' });
      expect(page.url()).toContain(`/admin/definitions/${def.name}/create`);
      const form = page.locator('form:not(#logoutForm)');
      await expect(form.first()).toBeVisible();
    }

    await errorTracker.assertCleanState();
  });

});
