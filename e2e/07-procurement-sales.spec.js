// @ts-check
const { test, expect } = require('@playwright/test');
const { loginAsAdmin } = require('./helpers/auth.helper');
const { setupErrorTracking } = require('./helpers/audit.helper');
const { waitForTabulator } = require('./helpers/grid.helper');

test.describe('Procurement & Sales (Satınalma & Satış)', () => {

  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test('Procurement list page loads with Tabulator grid', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);

    await page.goto('/procurement', { waitUntil: 'domcontentloaded' });
    expect(page.url()).toContain('/procurement');

    const rowCount = await waitForTabulator(page, '#procurement-table');
    expect(rowCount).toBeGreaterThan(0);

    const createBtn = page.locator('a[href*="/procurement/create"]');
    await expect(createBtn.first()).toBeVisible();

    await errorTracker.assertCleanState();
  });

  test('Procurement create, detail, and edit pages load successfully', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);

    // Create form
    await page.goto('/procurement/create', { waitUntil: 'domcontentloaded' });
    expect(page.url()).toContain('/procurement/create');
    const poInput = page.locator('input[name="poNumber"]');
    await expect(poInput).toBeVisible();

    // Get an existing ID
    const poId = await page.evaluate(async () => {
      const res = await fetch('/procurement/api/data?size=1');
      const data = await res.json();
      return (data.data && data.data[0]) ? data.data[0].id : 1;
    });

    // Detail
    await page.goto(`/procurement/${poId}`, { waitUntil: 'networkidle' });
    expect(page.url()).toContain(`/procurement/${poId}`);

    // Edit
    await page.goto(`/procurement/${poId}/edit`, { waitUntil: 'networkidle' });
    expect(page.url()).toContain(`/procurement/${poId}/edit`);

    await errorTracker.assertCleanState();
  });

  test('Sales list page loads with Tabulator grid', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);

    await page.goto('/sales', { waitUntil: 'domcontentloaded' });
    expect(page.url()).toContain('/sales');

    const rowCount = await waitForTabulator(page, '#sales-table');
    expect(rowCount).toBeGreaterThan(0);

    const createBtn = page.locator('a[href*="/sales/create"]');
    await expect(createBtn.first()).toBeVisible();

    await errorTracker.assertCleanState();
  });

  test('Sales create, detail, and edit pages load successfully', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);

    // Create form
    await page.goto('/sales/create', { waitUntil: 'domcontentloaded' });
    expect(page.url()).toContain('/sales/create');

    // Get an existing ID
    const salesId = await page.evaluate(async () => {
      const res = await fetch('/sales/api/data?size=1');
      const data = await res.json();
      return (data.data && data.data[0]) ? data.data[0].id : 1;
    });

    // Detail
    await page.goto(`/sales/${salesId}`, { waitUntil: 'networkidle' });
    expect(page.url()).toContain(`/sales/${salesId}`);

    // Edit
    await page.goto(`/sales/${salesId}/edit`, { waitUntil: 'networkidle' });
    expect(page.url()).toContain(`/sales/${salesId}/edit`);

    await errorTracker.assertCleanState();
  });

});
