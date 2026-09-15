// @ts-check
const { test, expect } = require('@playwright/test');
const { loginAsAdmin } = require('./helpers/auth.helper');
const { setupErrorTracking } = require('./helpers/audit.helper');
const { waitForTabulator } = require('./helpers/grid.helper');

test.describe('Workshop Management (Atölye)', () => {

  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test('Workshop list page loads with Tabulator grid', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);

    await page.goto('/workshop', { waitUntil: 'domcontentloaded' });
    expect(page.url()).toContain('/workshop');

    const rowCount = await waitForTabulator(page, '#workshop-table');
    expect(rowCount).toBeGreaterThan(0);

    const createBtn = page.locator('a[href*="/workshop/create"]');
    await expect(createBtn.first()).toBeVisible();

    await errorTracker.assertCleanState();
  });

  test('Workshop order creation form loads with inputs', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);

    await page.goto('/workshop/create', { waitUntil: 'domcontentloaded' });
    expect(page.url()).toContain('/workshop/create');

    const slabSelect = page.locator('select[name="slabId"]');
    const piecesInput = page.locator('input[name="piecesCount"]');
    await expect(slabSelect).toBeVisible();
    await expect(piecesInput).toBeVisible();

    const saveBtn = page.locator('button[type="submit"]');
    await expect(saveBtn.first()).toBeVisible();

    await errorTracker.assertCleanState();
  });

  test('Workshop order detail and edit pages load successfully', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);

    const orderId = await page.evaluate(async () => {
      const res = await fetch('/workshop/api/data?size=1');
      const data = await res.json();
      return (data.data && data.data[0]) ? data.data[0].id : 1;
    });

    // Detail
    await page.goto(`/workshop/${orderId}`, { waitUntil: 'networkidle' });
    expect(page.url()).toContain(`/workshop/${orderId}`);
    const content = await page.content();
    expect(content).toContain('Atölye');

    // Edit
    await page.goto(`/workshop/${orderId}/edit`, { waitUntil: 'networkidle' });
    expect(page.url()).toContain(`/workshop/${orderId}/edit`);

    await errorTracker.assertCleanState();
  });

});
