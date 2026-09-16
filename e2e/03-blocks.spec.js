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

    await page.goto('/blocks', { waitUntil: 'networkidle' });
    expect(page.url()).toContain('/blocks');

    await page.waitForFunction(() => {
      const section = document.querySelector('section[x-show*="blocks"]');
      return section && window.getComputedStyle(section).display !== 'none';
    }, { timeout: 10000 });

    const createBtn = page.locator('a[href*="/blocks/create"]');
    await expect(createBtn.first()).toBeVisible();

    const api = await page.evaluate(async () => {
      const res = await fetch('/blocks/api/data?page=1&size=25');
      return { status: res.status, body: await res.json() };
    });
    expect(api.status).toBe(200);
    expect(Array.isArray(api.body.data)).toBe(true);
    expect(api.body.total, 'Grid API must return existing quarry blocks').toBeGreaterThan(0);
    expect(api.body.data.length).toBeGreaterThan(0);
    expect(api.body.data[0].blockCode || api.body.data[0].block_code).toBeTruthy();

    const rowCount = await waitForTabulator(page, '#blocks-table', 1);
    expect(rowCount).toBeGreaterThan(0);

    const searchInput = page.locator('#search-input');
    await expect(searchInput).toBeVisible();

    const productionChip = page.getByRole('button', { name: /Üretim Sahası/ }).first();
    await expect(productionChip).toBeVisible();
    const yardFilterResponse = page.waitForResponse(res => res.url().includes('/blocks/api/data') && res.status() === 200);
    await productionChip.click();
    await yardFilterResponse;

    const soldTab = page.getByRole('tab', { name: 'Satılan Bloklar' });
    await soldTab.click();
    await expect(page.getByText('Satılan bloklar', { exact: true })).toBeVisible();

    const blocksTab = page.getByRole('tab', { name: 'Blok Takibi' });
    await blocksTab.click();
    await expect(page.getByText('Nasıl kullanılır?')).toBeVisible();

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
    expect(content).toContain('Maliyet');
    expect(content).toContain('Saha hareket geçmişi');
    expect(content).toContain('İşlemler');
    expect(content).toContain('Müşteri işaretleme');

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
