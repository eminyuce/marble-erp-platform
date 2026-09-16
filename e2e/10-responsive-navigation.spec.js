// @ts-check
const { test, expect } = require('@playwright/test');
const { loginAsAdmin } = require('./helpers/auth.helper');
const { setupErrorTracking } = require('./helpers/audit.helper');

test.describe('Responsive Layout & Navigation Systems', () => {

  test('Desktop: Navigation bar and Mega Menu navigate to appropriate sections', async ({ page }) => {
    await loginAsAdmin(page);
    const errorTracker = setupErrorTracking(page);

    await page.setViewportSize({ width: 1440, height: 900 });
    await page.goto('/admin/dashboard', { waitUntil: 'domcontentloaded' });

    // Verify topbar header exists
    const topbar = page.locator('header.admin-topbar');
    await expect(topbar).toBeVisible();

    const menuTrigger = page.locator('#adminMegaMenuOpen');
    await expect(menuTrigger).toBeVisible();
    await menuTrigger.click();

    // Verify mega menu opens
    const megaMenu = page.locator('#adminMegaMenu');
    await expect(megaMenu).toBeVisible();
    await expect(megaMenu.locator('a[href*="/expenses"]').first()).toBeVisible();

    // Click blocks link inside mega menu
    const blockLink = megaMenu.locator('a[href*="/blocks"]');
    if (await blockLink.count() > 0) {
      await blockLink.first().click();
      await page.waitForURL('**/blocks**');
      expect(page.url()).toContain('/blocks');
    }

    await errorTracker.assertCleanState();
  });

  test('Desktop: Mega Menu expands and closes with Escape key', async ({ page }) => {
    await loginAsAdmin(page);
    const errorTracker = setupErrorTracking(page);

    await page.setViewportSize({ width: 1440, height: 900 });
    await page.goto('/admin/dashboard', { waitUntil: 'networkidle' });

    const megaBtn = page.locator('button:has-text("Mega Menü"), button[aria-label*="Mega"]');
    if (await megaBtn.count() > 0 && await megaBtn.first().isVisible()) {
      await megaBtn.first().click();

      const megaPanel = page.locator('div[x-show="megaMenuOpen"], .admin-mega-panel');
      if (await megaPanel.count() > 0) {
        await expect(megaPanel.first()).toBeVisible();

        // Close on Escape key
        await page.keyboard.press('Escape');
        await expect(megaPanel.first()).toBeHidden();
      }
    }

    await errorTracker.assertCleanState();
  });

  test('Mobile: Navigation drawer opens and closes without layout break', async ({ page }) => {
    await loginAsAdmin(page);
    const errorTracker = setupErrorTracking(page);

    await page.setViewportSize({ width: 375, height: 812 });
    await page.goto('/admin/dashboard', { waitUntil: 'domcontentloaded' });

    const toggleBtn = page.locator('#sidebar-toggle, button[aria-label*="Menü"], button[aria-label*="menu"]');
    if (await toggleBtn.count() > 0 && await toggleBtn.first().isVisible()) {
      await toggleBtn.first().click();

      const drawer = page.locator('aside[x-show*="mobileMenuOpen"], [x-show*="mobileMenuOpen"]');
      if (await drawer.count() > 0) {
        await expect(drawer.first()).toBeVisible();

        // Close via Escape or backdrop
        await page.keyboard.press('Escape');
        await expect(drawer.first()).toBeHidden();
      }
    }

    // Verify no horizontal overflow on mobile
    const hasHorizontalOverflow = await page.evaluate(() => {
      return document.documentElement.scrollWidth > document.documentElement.clientWidth;
    });
    expect(hasHorizontalOverflow).toBeFalsy();

    await errorTracker.assertCleanState();
  });

  test('Tablet: Layout renders cleanly without visual overlapping', async ({ page }) => {
    await loginAsAdmin(page);
    const errorTracker = setupErrorTracking(page);

    await page.setViewportSize({ width: 768, height: 1024 });
    await page.goto('/blocks', { waitUntil: 'domcontentloaded' });

    const heading = page.locator('h1, h2').first();
    await expect(heading).toBeVisible();

    await errorTracker.assertCleanState();
  });

});
