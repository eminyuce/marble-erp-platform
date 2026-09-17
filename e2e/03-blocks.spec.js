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
    expect(api.body.meta, 'Grid API must return footer totals').toBeTruthy();
    expect(api.body.meta.totalTonnage).not.toBeUndefined();
    expect(api.body.meta.totalSurfaceM2).not.toBeUndefined();
    expect(api.body.meta.totalExtractionCost).not.toBeUndefined();
    expect(api.body.meta.totalCost).not.toBeUndefined();

    const rowCount = await waitForTabulator(page, '#blocks-table', 1);
    expect(rowCount).toBeGreaterThan(0);
    await expect(page.locator('#blocks-grid-totals')).toHaveCount(0);

    const searchInput = page.locator('#search-input');
    await expect(searchInput).toBeVisible();

    const productionChip = page.getByRole('button', { name: /Üretim Sahası/ }).first();
    await expect(productionChip).toBeVisible();
    const yardFilterResponse = page.waitForResponse(res => res.url().includes('/blocks/api/data') && res.status() === 200);
    await productionChip.click();
    await yardFilterResponse;

    const dispatchChip = page.getByRole('button', { name: /Stok Sahası/ }).first();
    await expect(dispatchChip).toBeVisible();

    // Verify FACTORY_BLOCK_YARD chip is removed
    const factoryChip = page.locator('button.erp-yard-chip:has-text("Fabrika Blok Sahası")');
    await expect(factoryChip).toHaveCount(0);

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
    await blockCodeInput.fill('blk-e2e-lower');
    await expect(blockCodeInput).toHaveValue('BLK-E2E-LOWER');
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

  test('Full block lifecycle: create with storage area, edit area, verify detail, and delete with modal', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);
    const testCode = 'BLK-E2E-' + Date.now().toString().slice(-6);

    // 1. Create Block with DISPATCH_YARD (Stok Sahası)
    await page.goto('/blocks/create', { waitUntil: 'networkidle' });

    await page.locator('select[name="quarryId"]').selectOption({ index: 1 });
    await page.locator('input[name="blockCode"]').fill(testCode);

    const locationSelect = page.locator('select[name="locationType"]');
    await expect(locationSelect).toBeVisible();
    await locationSelect.selectOption('DISPATCH_YARD');

    await page.locator('input[name="widthCm"]').fill('180');
    await page.locator('input[name="lengthCm"]').fill('280');
    await page.locator('input[name="heightCm"]').fill('140');
    await page.locator('input[name="stoneType"]').fill('Test Mermer');
    await page.locator('select[name="qualityGrade"]').selectOption('A');

    await Promise.all([
      page.waitForNavigation({ waitUntil: 'networkidle' }),
      page.locator('#block-form button[type="submit"]').click()
    ]);

    expect(page.url()).toContain('/blocks');

    // 2. Find the created block ID via API
    const blockData = await page.evaluate(async (code) => {
      const res = await fetch(`/blocks/api/data?search=${encodeURIComponent(code)}`);
      const json = await res.json();
      return json.data && json.data.length > 0 ? json.data[0] : null;
    }, testCode);

    expect(blockData, 'Created block should exist in API').toBeTruthy();
    expect(blockData.location_type || blockData.locationType).toBe('DISPATCH_YARD');
    const createdId = blockData.id;

    // 3. Edit block to move storage area to PRODUCTION_YARD (Üretim Sahası)
    await page.goto(`/blocks/${createdId}/edit`, { waitUntil: 'networkidle' });
    const editLocSelect = page.locator('select[name="locationType"]');
    await expect(editLocSelect).toHaveValue('DISPATCH_YARD');
    await editLocSelect.selectOption('PRODUCTION_YARD');

    await Promise.all([
      page.waitForNavigation({ waitUntil: 'networkidle' }),
      page.locator('#block-form button[type="submit"]').click()
    ]);

    // 4. Verify detail page reflects new location and movement history
    await page.goto(`/blocks/${createdId}`, { waitUntil: 'networkidle' });
    const detailContent = await page.content();
    expect(detailContent).toContain('Üretim Sahası');
    expect(detailContent).toContain('Saha hareket geçmişi');
    expect(detailContent).toContain('Düzenleme formundan saha taşıma');

    // 5. Delete block via modal
    const deleteBtn = page.locator('.erp-ops-card button:has-text("Bloğu Sil")');
    await expect(deleteBtn).toBeVisible();
    await deleteBtn.click();

    // Confirmation modal should appear
    const confirmDeleteBtn = page.locator('button:has-text("Evet, Bloğu Sil")');
    await expect(confirmDeleteBtn).toBeVisible();

    await Promise.all([
      page.waitForNavigation({ waitUntil: 'networkidle' }),
      confirmDeleteBtn.click()
    ]);

    expect(page.url()).toContain('/blocks');

    // Verify block is deleted
    const deletedCheck = await page.evaluate(async (code) => {
      const res = await fetch(`/blocks/api/data?search=${encodeURIComponent(code)}`);
      const json = await res.json();
      return json.data ? json.data.length : 0;
    }, testCode);
    expect(deletedCheck).toBe(0);

    await errorTracker.assertCleanState();
  });

  test('Dedicated block sell page: navigate from datagrid, verify help banner and tooltips, complete sale', async ({ page }) => {
    const errorTracker = setupErrorTracking(page);
    const testCode = 'BLK-SELL-' + Date.now().toString().slice(-6);

    // 1. Create a fresh test block to sell
    await page.goto('/blocks/create', { waitUntil: 'networkidle' });
    await page.locator('select[name="quarryId"]').selectOption({ index: 1 });
    await page.locator('input[name="blockCode"]').fill(testCode);
    await page.locator('select[name="locationType"]').selectOption('DISPATCH_YARD');
    await page.locator('input[name="widthCm"]').fill('150');
    await page.locator('input[name="lengthCm"]').fill('250');
    await page.locator('input[name="heightCm"]').fill('130');
    await page.locator('input[name="stoneType"]').fill('Burdur Bej');
    await page.locator('select[name="qualityGrade"]').selectOption('A');

    await Promise.all([
      page.waitForNavigation({ waitUntil: 'networkidle' }),
      page.locator('#block-form button[type="submit"]').click()
    ]);

    // 2. Locate created block in datagrid
    await page.goto('/blocks', { waitUntil: 'networkidle' });
    const searchResponse = page.waitForResponse(res => res.url().includes('/blocks/api/data') && res.status() === 200);
    const searchInput = page.locator('#search-input');
    await searchInput.fill(testCode);
    await searchResponse;

    const rowCount = await waitForTabulator(page, '#blocks-table', 1);
    expect(rowCount).toBeGreaterThan(0);

    // 3. Open row actions dropdown and verify 'Sat' action is a direct link to dedicated /sell page
    const actionsBtn = page.locator('#blocks-table .grid-actions-btn').first();
    await actionsBtn.scrollIntoViewIfNeeded();
    await expect(actionsBtn).toBeVisible();
    await actionsBtn.evaluate(btn => /** @type {HTMLElement} */ (btn).click());

    const sellLink = page.locator('#grid-actions-portal a:has-text("Sat")');
    await expect(sellLink).toBeVisible();
    const sellHref = await sellLink.getAttribute('href');
    expect(sellHref).toMatch(/\/blocks\/\d+\/sell$/);

    // Ensure no sell dialog exists on index page
    await expect(page.locator('#sell-block-dialog')).toHaveCount(0);

    // 4. Click "Sat" link and navigate to dedicated sell page
    await Promise.all([
      page.waitForNavigation({ waitUntil: 'networkidle' }),
      sellLink.evaluate(a => /** @type {HTMLElement} */ (a).click())
    ]);

    expect(page.url()).toMatch(/\/blocks\/\d+\/sell$/);

    // 5. Verify page title, breadcrumb and block code
    await expect(page.locator('.admin-page-title')).toContainText('Blok Dış Satış İşlemi');
    await expect(page.locator('.erp-form-entity')).toContainText(testCode);
    await expect(page.locator(`text=${testCode}`).first()).toBeVisible();

    // 6. Verify informative guidance banner / help text
    const infoCallout = page.locator('.erp-ops-info-callout');
    await expect(infoCallout).toBeVisible();
    await expect(infoCallout).toContainText('Stok Çıkışı');
    await expect(infoCallout).toContainText('Müşteri Tahsisi');
    await expect(infoCallout).toContainText('Kısıtlamalar');

    // 7. Verify tooltips are rendered on the page with title attributes
    const tips = page.locator('.erp-tip');
    expect(await tips.count()).toBeGreaterThanOrEqual(3);
    const firstTipTitle = await tips.first().getAttribute('title');
    expect(firstTipTitle).toBeTruthy();

    // 8. Verify block summary specs
    await expect(page.getByText('150 × 250 × 130 cm')).toBeVisible();
    await expect(page.getByText('Burdur Bej')).toBeVisible();

    // 9. Select a customer, provide sales notes, and complete the sale
    const customerSelect = page.locator('select[name="customerId"]');
    await expect(customerSelect).toBeVisible();
    await customerSelect.selectOption({ index: 1 });

    await page.locator('input[name="salePrice"]').fill('650000');

    const notesEditor = page.locator('#sale-notes-editor [contenteditable="true"], [data-html-notes] [contenteditable="true"]').first();
    await expect(notesEditor).toBeVisible();
    await notesEditor.fill('E2E Test satışı başarıyla tamamlandı.');

    const submitBtn = page.locator('#submit-sell-btn');
    await expect(submitBtn).toBeVisible();

    await Promise.all([
      page.waitForNavigation({ waitUntil: 'networkidle' }),
      submitBtn.click()
    ]);

    // 10. Verify redirected to detail page and status is SOLD
    expect(page.url()).toMatch(/\/blocks\/\d+$/);
    const detailContent = await page.content();
    expect(detailContent).toContain('Satıldı');
    expect(detailContent).toContain('Satın alan müşteri');

    // 11. Verify that trying to access /sell for an already sold block redirects
    const soldBlockId = page.url().split('/').pop();
    await page.goto(`/blocks/${soldBlockId}/sell`, { waitUntil: 'networkidle' });
    expect(page.url()).toContain(`/blocks/${soldBlockId}`);

    await errorTracker.assertCleanState();
  });

});

