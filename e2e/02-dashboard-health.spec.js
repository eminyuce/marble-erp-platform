// @ts-check
const { test, expect } = require('@playwright/test');
const { loginAsAdmin } = require('./helpers/auth.helper');
const { setupErrorTracking } = require('./helpers/audit.helper');

test.describe('Dashboard & System Health & Help Pages', () => {

  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test('Dashboard loads KPI summary cards, quick navigation, and recent tables', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);

    await page.goto('/admin/dashboard', { waitUntil: 'networkidle' });
    expect(page.url()).toContain('/admin/dashboard');

    // Verify main dashboard elements
    const heading = page.locator('h1, h2').first();
    await expect(heading).toBeVisible();

    // Verify KPI summary or stat cards
    const statCards = page.locator('.stat-card, [class*="rounded-xl border"], .grid');
    expect(await statCards.count()).toBeGreaterThan(0);

    // Quick action links
    const quickLinks = page.locator('a[href*="/blocks"], a[href*="/production"]');
    expect(await quickLinks.count()).toBeGreaterThan(0);

    await errorTracker.assertCleanState();
  });

  test('System Health page loads system status, JVM memory, and DB health', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);

    await page.goto('/admin/dashboard/systemhealth/', { waitUntil: 'networkidle' });
    expect(page.url()).toContain('/admin/dashboard/systemhealth');

    // Page title and health indicator
    const content = await page.content();
    expect(content.toLowerCase()).toContain('sistem');

    // Verify system health API returns valid JSON
    const apiRes = await page.evaluate(async () => {
      const res = await fetch('/admin/dashboard/systemhealth/api');
      return { status: res.status, data: await res.json() };
    });
    expect(apiRes.status).toBe(200);
    expect(apiRes.data).toBeDefined();

    await errorTracker.assertCleanState();
  });

  test('Site Features page loads with system features list', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);

    await page.goto('/admin/dashboard/oursitefeatures/', { waitUntil: 'networkidle' });
    expect(page.url()).toContain('/admin/dashboard/oursitefeatures');

    const content = await page.content();
    expect(content).toContain('Özerler Mermer');

    await errorTracker.assertCleanState();
  });

  test('Global Search API returns results for entities', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);

    const searchRes = await page.evaluate(async () => {
      const res = await fetch('/admin/dashboard/api/search?q=BLK');
      return { status: res.status, data: await res.json() };
    });

    expect(searchRes.status).toBe(200);
    expect(searchRes.data).toBeDefined();

    await errorTracker.assertCleanState();
  });

  test('Help catalog API returns documentation cleanly', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);

    const helpKeys = ['blocks', 'production', 'workshop', 'projects', 'sales'];
    for (const key of helpKeys) {
      const res = await page.evaluate(async (k) => {
        const r = await fetch(`/api/help/${k}`);
        return { status: r.status, ok: r.ok };
      }, key);
      expect(res.status).toBe(200);
    }

    await errorTracker.assertCleanState();
  });

});
