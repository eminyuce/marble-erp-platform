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

  test('Deployment page is reachable from Sistem Araçları and shows live console', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);

    await page.goto('/admin/settings?tab=tools', { waitUntil: 'networkidle' });
    const deployLink = page.locator('a[href="/admin/deployment"]');
    await expect(deployLink.first()).toBeVisible();

    await page.goto('/admin/deployment', { waitUntil: 'networkidle' });
    expect(page.url()).toContain('/admin/deployment');
    await expect(page.locator('h1')).toContainText('Üretim Yayını');
    await expect(page.locator('#deployment-start-btn')).toBeVisible();
    await expect(page.locator('#deployment-log')).toBeVisible();

    const statusRes = await page.evaluate(async () => {
      const res = await fetch('/admin/deployment/status');
      return { status: res.status, data: await res.json() };
    });
    expect(statusRes.status).toBe(200);
    expect(statusRes.data.state).toBeTruthy();
    expect(typeof statusRes.data.canStart).toBe('boolean');

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

  test('Email template preview is a dedicated page, not a modal', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);

    await page.goto('/admin/settings?tab=templates', { waitUntil: 'networkidle' });
    const previewLink = page.locator('a[href*="/admin/settings/templates/"][href$="/preview"]').first();
    await expect(previewLink).toBeVisible();
    await previewLink.click();
    await expect(page).toHaveURL(/\/admin\/settings\/templates\/\d+\/preview$/);
    await expect(page.locator('body')).toHaveAttribute('data-help-page-key', 'email-preview');
    await expect(page.locator('#email-preview-frame')).toBeVisible();
    await expect(page.locator('#email-preview-subject')).not.toHaveText('');
    await expect(page.locator('.admin-page-title')).toContainText('E-posta şablonu canlı önizleme');

    await errorTracker.assertCleanState();
  });

  test('User delete uses a confirmation dialog, not a native confirm popup', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);

    await page.goto('/admin/users', { waitUntil: 'domcontentloaded' });
    await waitForTabulator(page, '#users-table');
    const actionsBtn = page.locator('#users-table .grid-actions-btn').first();
    await actionsBtn.scrollIntoViewIfNeeded();
    await actionsBtn.evaluate(btn => /** @type {HTMLElement} */ (btn).click());
    const deleteBtn = page.locator('#grid-actions-portal button:has-text("Sil")').first();
    await expect(deleteBtn).toBeVisible();
    await deleteBtn.click();
    const dialog = page.locator('#delete-user-dialog');
    await expect(dialog).toBeVisible();
    await dialog.locator('button:has-text("Vazgeç")').click();
    await expect(dialog).toBeHidden();

    await errorTracker.assertCleanState();
  });

});
