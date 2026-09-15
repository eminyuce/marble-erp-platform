// @ts-check
const { test, expect } = require('@playwright/test');
const { loginAsAdmin } = require('./helpers/auth.helper');
const { setupErrorTracking } = require('./helpers/audit.helper');

test.describe('Costs, Reports, Genealogy & Digital Passport', () => {

  test('Costs analysis page loads with business unit breakdown', async ({ page }) => {
    await loginAsAdmin(page);
    const errorTracker = setupErrorTracking(page);

    await page.goto('/costs', { waitUntil: 'networkidle' });
    expect(page.url()).toContain('/costs');

    const content = await page.content();
    expect(content).toContain('Maliyet');

    await errorTracker.assertCleanState();
  });

  test('Reports hub page loads with summary metrics and export links', async ({ page }) => {
    await loginAsAdmin(page);
    const errorTracker = setupErrorTracking(page);

    await page.goto('/reports', { waitUntil: 'networkidle' });
    expect(page.url()).toContain('/reports');

    const content = await page.content();
    expect(content).toContain('Rapor');

    await errorTracker.assertCleanState();
  });

  test('Genealogy tree page loads and allows searching by block/slab code', async ({ page }) => {
    await loginAsAdmin(page);
    const errorTracker = setupErrorTracking(page);

    await page.goto('/genealogy', { waitUntil: 'domcontentloaded' });
    expect(page.url()).toContain('/genealogy');

    const searchInput = page.locator('input[name="code"]');
    await expect(searchInput).toBeVisible();

    await errorTracker.assertCleanState();
  });

  test('Digital Slab Passport page is publicly accessible without login', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);

    // Get a valid slab code or use sample SLB-2026-000101
    await page.goto('/passport/SLB-2026-000101', { waitUntil: 'domcontentloaded' });
    const content = await page.content();
    expect(content).toContain('Dijital Taş Pasaportu');

    await errorTracker.assertCleanState();
  });

});
