// @ts-check
const { expect } = require('@playwright/test');

/**
 * Wait for a Tabulator table to load and render at least minRows
 * @param {import('@playwright/test').Page} page
 * @param {string} tableSelector e.g. '#blocks-table'
 * @param {number} [minRows=1]
 * @param {number} [timeout=10000]
 */
async function waitForTabulator(page, tableSelector, minRows = 1, timeout = 10000) {
  const table = page.locator(tableSelector);
  await expect(table).toBeVisible({ timeout });

  // Tabulator creates `.tabulator-row` inside `.tabulator-table`
  const rowLocator = page.locator(`${tableSelector} .tabulator-row`);
  await rowLocator.first().waitFor({ state: 'visible', timeout }).catch(() => {
    // If no rows rendered, it might be an empty table
  });

  const count = await rowLocator.count();
  return count;
}

/**
 * Get Tabulator row count
 * @param {import('@playwright/test').Page} page
 * @param {string} tableSelector
 */
async function getTabulatorRowCount(page, tableSelector) {
  return await page.locator(`${tableSelector} .tabulator-row`).count();
}

module.exports = {
  waitForTabulator,
  getTabulatorRowCount,
};
