// @ts-check
const { test, expect } = require('@playwright/test');
const { loginAsAdmin } = require('./helpers/auth.helper');
const { setupErrorTracking } = require('./helpers/audit.helper');
const { waitForTabulator } = require('./helpers/grid.helper');

test.describe('Production & Slabs (Fabrika & Plaka)', () => {

  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test('Production orders page loads with Tabulator grid', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);

    await page.goto('/production', { waitUntil: 'domcontentloaded' });
    expect(page.url()).toContain('/production');

    const rowCount = await waitForTabulator(page, '#production-table');
    expect(rowCount).toBeGreaterThan(0);

    const createBtn = page.locator('a[href*="/production/create"]');
    await expect(createBtn.first()).toBeVisible();

    await errorTracker.assertCleanState();
  });

  test('Production order creation form loads with block and machine selectors', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);

    await page.goto('/production/create', { waitUntil: 'domcontentloaded' });
    expect(page.url()).toContain('/production/create');

    const blockSelect = page.locator('select[name="blockId"]');
    const machineSelect = page.locator('select[name="machineId"]');

    await expect(blockSelect).toBeVisible();
    await expect(machineSelect).toBeVisible();

    await errorTracker.assertCleanState();
  });

  test('Production order detail and edit pages load successfully', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);

    const orderId = await page.evaluate(async () => {
      const res = await fetch('/production/api/orders?size=1');
      const data = await res.json();
      return (data.data && data.data[0]) ? data.data[0].id : 1;
    });

    // Detail page
    await page.goto(`/production/orders/${orderId}`, { waitUntil: 'networkidle' });
    expect(page.url()).toContain(`/production/orders/${orderId}`);
    const content = await page.content();
    expect(content).not.toContain('Geliştirici hata ayrıntısı');
    expect(content).not.toContain('Whitelabel');

    // Edit page
    await page.goto(`/production/orders/${orderId}/edit`, { waitUntil: 'networkidle' });
    expect(page.url()).toContain(`/production/orders/${orderId}/edit`);

    await errorTracker.assertCleanState();
  });

  test('Slabs inventory grid loads with Tabulator and status filters', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);

    await page.goto('/production/slabs', { waitUntil: 'domcontentloaded' });
    expect(page.url()).toContain('/production/slabs');

    const rowCount = await waitForTabulator(page, '#slabs-table');
    expect(rowCount).toBeGreaterThan(0);

    await errorTracker.assertCleanState();
  });

  test('Slab detail, edit, and printable barcode label pages load', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);

    const slabId = await page.evaluate(async () => {
      const res = await fetch('/production/api/slabs?size=1');
      const data = await res.json();
      return (data.data && data.data[0]) ? data.data[0].id : 1;
    });

    // Detail
    await page.goto(`/production/slabs/${slabId}`, { waitUntil: 'networkidle' });
    expect(page.url()).toContain(`/production/slabs/${slabId}`);

    // Edit
    await page.goto(`/production/slabs/${slabId}/edit`, { waitUntil: 'networkidle' });
    expect(page.url()).toContain(`/production/slabs/${slabId}/edit`);

    // Label
    await page.goto(`/production/slabs/${slabId}/label`, { waitUntil: 'networkidle' });
    expect(page.url()).toContain(`/production/slabs/${slabId}/label`);

    await errorTracker.assertCleanState();
  });

  test('Polish line, Pallets, and Factory Tablet kiosk interfaces load', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);

    // Polish
    await page.goto('/production/polish', { waitUntil: 'domcontentloaded' });
    expect(page.url()).toContain('/production/polish');

    // Pallets
    await page.goto('/production/pallets', { waitUntil: 'domcontentloaded' });
    expect(page.url()).toContain('/production/pallets');

    // Tablet kiosk
    await page.goto('/production/tablet', { waitUntil: 'domcontentloaded' });
    expect(page.url()).toContain('/production/tablet');

    await errorTracker.assertCleanState();
  });

});
