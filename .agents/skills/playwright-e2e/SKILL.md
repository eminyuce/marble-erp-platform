---
name: playwright-e2e
description: >-
  Guide and best practices for writing, running, and maintaining reliable End-to-End (E2E)
  and visual regression tests using Playwright. Use when designing new E2E test suites,
  testing authentication flows, validating Tabulator tables, Alpine.js UI states, HTMX modals,
  multi-viewport responsive designs (Desktop, Tablet, Mobile), or diagnosing test flakiness.
---

# Playwright End-to-End (E2E) Testing Guide

Comprehensive guide for designing, implementing, and running resilient End-to-End (E2E) test suites, automated UI audits, and visual regression testing across all device viewports.

---

## 1. Quick Setup & Environment in Windows

When running in corporate Windows environments where downloading Playwright browser binaries via `npx playwright install` might be restricted or slow, utilize existing system-installed browsers (Google Chrome or Microsoft Edge) via `playwright-core` or standard `playwright`:

```javascript
const { chromium } = require('playwright-core');
const fs = require('fs');

function getBrowserExecutable() {
    const chromePath = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
    const edgePath = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';
    if (fs.existsSync(chromePath)) return chromePath;
    if (fs.existsSync(edgePath)) return edgePath;
    return undefined; // Falls back to Playwright bundled binary if installed
}

const browser = await chromium.launch({
    executablePath: getBrowserExecutable(),
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox']
});
```

---

## 2. Standard Viewport Matrix

Always execute audits and critical flows across all three standard responsive tiers:

| Tier | Width | Height | Key Verification Focus |
| :--- | :--- | :--- | :--- |
| **Desktop** | `1440` | `900` | Full sidebar, expanded data grids, desktop modals |
| **Tablet** | `768` | `1024` | Sidebar collapse, grid responsive sub-rows, tablet layout |
| **Mobile** | `375` | `812` | Hamburger button, off-canvas slide drawer, stacked cards, no horizontal overflow |

```javascript
const VIEWPORTS = [
    { name: 'desktop', width: 1440, height: 900 },
    { name: 'tablet', width: 768, height: 1024 },
    { name: 'mobile', width: 375, height: 812 }
];
```

---

## 3. Spring Security & Authentication Pattern

### A. Form Login with CSRF Handling
Spring Security forms require CSRF tokens. Always allow the login page to fully load so that the hidden `_csrf` input is populated:

```javascript
async function loginAsAdmin(page, baseUrl = 'http://localhost:8080') {
    await page.goto(`${baseUrl}/account/adminlogin/`, { waitUntil: 'domcontentloaded' });
    
    // Fill credentials
    await page.fill('input[name="username"]', 'admin@eimece.test');
    await page.fill('input[name="password"]', 'B2u5c8JB');
    
    // Submit and wait for redirect to dashboard
    await Promise.all([
        page.waitForURL('**/admin/dashboard', { timeout: 10000 }),
        page.click('button[type="submit"]')
    ]);
}
```

### B. Session Reuse via Storage State
To avoid re-logging in before every individual test, save and reuse the authenticated browser context state:

```javascript
// 1. In global-setup.js or beforeAll:
const context = await browser.newContext();
const page = await context.newPage();
await loginAsAdmin(page);
await context.storageState({ path: 'target/auth-state.json' });
await context.close();

// 2. In individual tests:
const authenticatedContext = await browser.newContext({
    storageState: 'target/auth-state.json',
    viewport: { width: 1440, height: 900 }
});
```

---

## 4. Testing Tabulator 6 Data Grids

Tabulator data grids render asynchronously with remote AJAX pagination and virtual DOM row recycling.

### Rules for Testing Tabulator:
1. **Never use static sleeps** (`waitForTimeout`) when waiting for data. Wait for the Tabulator rows or holder to populate:
```javascript
// Wait for at least one data row to render
await page.waitForSelector('#blocks-table .tabulator-row', { timeout: 10000 });
```

2. **Verify Row Counts & Content**:
```javascript
const rowCount = await page.locator('#blocks-table .tabulator-row').count();
expect(rowCount).toBeGreaterThan(0);

// Validate first row content
const firstRowCode = await page.locator('#blocks-table .tabulator-row').first().locator('.font-mono').textContent();
expect(firstRowCode).toContain('BLK-');
```

3. **Test Search / Filtering with Debounce**:
```javascript
const searchInput = page.locator('#search-input');
await searchInput.fill('Muğla');

// Wait for Tabulator remote reload to complete
const responsePromise = page.waitForResponse(res => 
    res.url().includes('/blocks/api/data') && res.status() === 200
);
await responsePromise;

// Assert filtered rows
const filteredCount = await page.locator('#blocks-table .tabulator-row').count();
expect(filteredCount).toBeGreaterThan(0);
```

---

## 5. Testing Alpine.js & Off-Canvas Mobile Drawer

### A. Testing Mobile Drawer Open / Close
```javascript
test('Mobile navigation drawer opens and closes properly', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 812 });
    await page.goto('/admin/dashboard');

    const toggleBtn = page.locator('#sidebar-toggle');
    await expect(toggleBtn).toBeVisible();
    await toggleBtn.click();

    // Verify drawer is visible
    const drawer = page.locator('aside[x-show="mobileMenuOpen"]');
    await expect(drawer).toBeVisible();

    // Verify backdrop is visible
    const backdrop = page.locator('div[x-show="mobileMenuOpen"]');
    await expect(backdrop).toBeVisible();

    // Close via close button or Escape key
    const closeBtn = drawer.locator('button[aria-label="Menüyü Kapat"]');
    if (await closeBtn.isVisible()) {
        await closeBtn.click();
    } else {
        await page.keyboard.press('Escape');
    }

    await expect(drawer).toBeHidden();
});
```

### B. Testing Mega Menu Dropdowns
```javascript
test('Mega menu expands and closes on Escape', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 });
    await page.goto('/admin/dashboard');

    const megaBtn = page.locator('button:has-text("Mega Menü")');
    await megaBtn.click();

    const megaPanel = page.locator('div[x-show="megaMenuOpen"]');
    await expect(megaPanel).toBeVisible();

    // Press Escape
    await page.keyboard.press('Escape');
    await expect(megaPanel).toBeHidden();
});
```

---

## 6. Testing HTMX Modals & Dynamic Swaps

HTMX replaces fragments inside `#modal-container` without full page reloads:

```javascript
test('HTMX Modal opens and closes', async ({ page }) => {
    await page.goto('/blocks');

    // Click button configured with hx-get="/blocks/modal/new"
    const openModalBtn = page.locator('button:has-text("Yeni Blok Kaydı")');
    if (await openModalBtn.isVisible()) {
        await openModalBtn.click();

        // Wait for modal dialog inside modal-container
        const dialog = page.locator('#modal-container dialog, #modal-container .modal-content');
        await expect(dialog).toBeVisible({ timeout: 5000 });

        // Close modal
        const closeBtn = dialog.locator('button:has-text("Kapat"), button[aria-label="Close"]');
        await closeBtn.click();
        await expect(dialog).toBeHidden();
    }
});
```

---

## 7. Testing In-Browser APIs directly

Use `page.evaluate()` inside an authenticated session to test REST endpoints and JSON payloads without extra HTTP client overhead:

```javascript
test('API /production/api/slabs returns valid JSON without circular nesting', async ({ page }) => {
    await page.goto('/admin/dashboard');

    const apiResponse = await page.evaluate(async () => {
        const res = await fetch('/production/api/slabs?page=1&size=10', {
            headers: { 'Accept': 'application/json' }
        });
        return {
            status: res.status,
            contentType: res.headers.get('content-type'),
            body: await res.json()
        };
    });

    expect(apiResponse.status).toBe(200);
    expect(apiResponse.contentType).toContain('application/json');
    expect(Array.isArray(apiResponse.body.data)).toBe(true);
    expect(apiResponse.body.data.length).toBeGreaterThan(0);
});
```

---

## 8. Visual Regression & Screenshot Auditing

For automated UI inspections across releases:

```javascript
// Capture clean full-page screenshot
await page.screenshot({
    path: `target/screenshots/${viewportName}_${pageName}.png`,
    fullPage: true
});

// Capture single component or modal
const card = page.locator('#kpi-summary-cards');
await card.screenshot({
    path: `target/screenshots/${viewportName}_kpi_cards.png`
});
```

### Best Practices for Visual Audits:
- **Disable animations** before capturing if testing for pixel diffs:
  `await page.addStyleTag({ content: '*, *::before, *::after { transition-duration: 0s !important; animation-duration: 0s !important; }' });`
- **Mask Dynamic Dates & Timestamps** so screenshots remain stable between runs:
  `await page.screenshot({ mask: [page.locator('.dynamic-timestamp, .current-time')] });`

---

## 9. Common Flakiness Pitfalls & Solutions

1. **Avoid `page.waitForTimeout(n)`**: Always prefer event-driven waits like `locator.waitFor()`, `page.waitForResponse()`, or `expect(locator).toBeVisible()`.
2. **Handle Double Click / Race Conditions**: Use `page.click(selector, { delay: 100 })` if JS event listeners take a frame to bind.
3. **Handle Font Rendering Differences**: Use `await document.fonts.ready` before taking critical UI screenshots:
   ```javascript
   await page.evaluate(() => document.fonts.ready);
   ```
4. **Clean up Database State**: Use test-specific IDs or transactions when inserting data during E2E tests, or seed deterministic fixture data via Flyway/SQL prior to runs.
