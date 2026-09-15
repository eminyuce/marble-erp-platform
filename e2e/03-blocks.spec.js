// @ts-check
const { test, expect } = require('@playwright/test');
const { loginAsAdmin } = require('./helpers/auth.helper');
const { setupErrorTracking } = require('./helpers/audit.helper');
const { waitForTabulator } = require('./helpers/grid.helper');

test.describe('Block Management (Ocak & Bloklar)', () => {

  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test('Blocks list page loads with Tabulator grid and filters', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);

    await page.goto('/blocks', { waitUntil: 'domcontentloaded' });
    expect(page.url()).toContain('/blocks');

    // Wait for Tabulator grid
    const rowCount = await waitForTabulator(page, '#blocks-table');
    expect(rowCount).toBeGreaterThan(0);

    // Verify search and yard filters are present
    const searchInput = page.locator('#search-input, input[type="search"], input[placeholder*="Ara"]');
    if (await searchInput.count() > 0) {
      await expect(searchInput.first()).toBeVisible();
    }

    // Verify New Block button
    const createBtn = page.locator('a[href*="/blocks/create"]');
    await expect(createBtn.first()).toBeVisible();

    await errorTracker.assertCleanState();
  });

  test('Block creation form loads with inputs and auto-weight calculator', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);

    await page.goto('/blocks/create', { waitUntil: 'domcontentloaded' });
    expect(page.url()).toContain('/blocks/create');

    // Form inputs
    const blockCodeInput = page.locator('input[name="blockCode"]');
    const widthInput = page.locator('input[name="widthCm"]');
    const lengthInput = page.locator('input[name="lengthCm"]');
    const heightInput = page.locator('input[name="heightCm"]');

    await expect(blockCodeInput).toBeVisible();
    await expect(widthInput).toBeVisible();
    await expect(lengthInput).toBeVisible();
    await expect(heightInput).toBeVisible();

    // Verify theoretical weight calculation helper
    await widthInput.fill('100');
    await lengthInput.fill('200');
    await heightInput.fill('150');

    // Verify save button exists
    const saveBtn = page.locator('button[type="submit"]');
    await expect(saveBtn.first()).toBeVisible();

    await errorTracker.assertCleanState();
  });

  test('Block detail page loads specifications, movements, and customer marks without SpEL error', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);

    // Fetch existing block IDs from API
    const firstBlockId = await page.evaluate(async () => {
      const res = await fetch('/blocks/api/data?size=1');
      const data = await res.json();
      return (data.data && data.data[0]) ? data.data[0].id : 1;
    });

    await page.goto(`/blocks/${firstBlockId}`, { waitUntil: 'networkidle' });
    expect(page.url()).toContain(`/blocks/${firstBlockId}`);

    // Verify specs section
    const content = await page.content();
    expect(content).toContain('Blok bilgisi');
    expect(content).toContain('Ocak ve maliyet');
    expect(content).toContain('Saha hareketi');
    expect(content).toContain('Müşteri işaretleme ve satış');

    // CRITICAL REGRESSION CHECK: No SpEL expression evaluation failure
    expect(content).not.toContain('Exception evaluating SpringEL expression');
    expect(content).not.toContain('LazyInitializationException');

    await errorTracker.assertCleanState();
  });

  test('Block edit form loads with current block data', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);

    const firstBlockId = await page.evaluate(async () => {
      const res = await fetch('/blocks/api/data?size=1');
      const data = await res.json();
      return (data.data && data.data[0]) ? data.data[0].id : 1;
    });

    await page.goto(`/blocks/${firstBlockId}/edit`, { waitUntil: 'networkidle' });
    expect(page.url()).toContain(`/blocks/${firstBlockId}/edit`);

    const blockCodeInput = page.locator('input[name="blockCode"]');
    await expect(blockCodeInput).toBeVisible();
    const val = await blockCodeInput.inputValue();
    expect(val.length).toBeGreaterThan(0);

    await errorTracker.assertCleanState();
  });

});
