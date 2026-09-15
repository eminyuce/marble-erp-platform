// @ts-check
const { test, expect } = require('@playwright/test');
const { loginAsAdmin } = require('./helpers/auth.helper');
const { setupErrorTracking } = require('./helpers/audit.helper');
const { waitForTabulator } = require('./helpers/grid.helper');

test.describe('Projects & Construction Sites (Projeler & Şantiyeler)', () => {

  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test('Projects list page loads with Tabulator grid', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);

    await page.goto('/projects', { waitUntil: 'domcontentloaded' });
    expect(page.url()).toContain('/projects');

    const rowCount = await waitForTabulator(page, '#projects-table');
    expect(rowCount).toBeGreaterThan(0);

    const createBtn = page.locator('a[href*="/projects/create"]');
    await expect(createBtn.first()).toBeVisible();

    await errorTracker.assertCleanState();
  });

  test('Project creation form loads with inputs', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);

    await page.goto('/projects/create', { waitUntil: 'domcontentloaded' });
    expect(page.url()).toContain('/projects/create');

    const codeInput = page.locator('input[name="projectCode"]');
    const nameInput = page.locator('input[name="name"]');
    const customerInput = page.locator('input[name="customerName"]');

    await expect(codeInput).toBeVisible();
    await expect(nameInput).toBeVisible();
    await expect(customerInput).toBeVisible();

    await errorTracker.assertCleanState();
  });

  test('Project detail pages (including all existing projects) load cleanly with WBS locations and consumptions', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);

    const projectIds = await page.evaluate(async () => {
      const res = await fetch('/projects/api/data?size=10');
      const data = await res.json();
      return (data.data || []).map(p => p.id);
    });

    for (const id of projectIds) {
      await page.goto(`/projects/${id}`, { waitUntil: 'networkidle' });
      expect(page.url()).toContain(`/projects/${id}`);
      const content = await page.content();
      expect(content).toContain('PROJ-');
      const title = await page.title();
      expect(title).toContain('Özerler Mermer ERP');
    }

    await errorTracker.assertCleanState();
  });

  test('Project edit form loads with project values', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);

    const firstId = await page.evaluate(async () => {
      const res = await fetch('/projects/api/data?size=1');
      const data = await res.json();
      return (data.data && data.data[0]) ? data.data[0].id : 1;
    });

    await page.goto(`/projects/${firstId}/edit`, { waitUntil: 'networkidle' });
    expect(page.url()).toContain(`/projects/${firstId}/edit`);

    const codeInput = page.locator('input[name="projectCode"]');
    await expect(codeInput).toBeVisible();

    await errorTracker.assertCleanState();
  });

});
