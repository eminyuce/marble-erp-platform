// @ts-check
const { test, expect } = require('@playwright/test');
const { loginAsAdmin } = require('./helpers/auth.helper');
const fs = require('fs');
const os = require('os');
const path = require('path');

function writeTempUpload(name, contents) {
  const filePath = path.join(os.tmpdir(), name);
  fs.writeFileSync(filePath, contents);
  return filePath;
}

test.describe('File storage upload / download / delete', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test('authenticated user can upload, download, and delete a PNG on the block form', async ({ page }) => {
    await page.goto('/blocks/create', { waitUntil: 'domcontentloaded' });
    await expect(page.locator('#block-filepond')).toBeVisible({ timeout: 15000 });

    const png = Buffer.from(
      'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==',
      'base64'
    );
    const pngPath = writeTempUpload('e2e-block.png', png);

    const fileInput = page.locator('input.filepond--browser').first();
    await fileInput.setInputFiles(pngPath);

    await expect(page.locator('input[name="fileIds"]').first()).toHaveValue(/\d+/, { timeout: 20000 });
    const fileId = await page.locator('input[name="fileIds"]').first().inputValue();
    expect(Number(fileId)).toBeGreaterThan(0);

    const download = await page.evaluate(async (id) => {
      const res = await fetch('/api/upload/download/' + id, { credentials: 'same-origin' });
      return { status: res.status, contentType: res.headers.get('content-type') };
    }, fileId);
    expect(download.status).toBe(200);
    expect(download.contentType).toContain('image/png');

    const deleted = await page.evaluate(async (id) => {
      const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
      const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
      const headers = {};
      if (csrfHeader && csrfToken) {
        headers[csrfHeader] = csrfToken;
      }
      const res = await fetch('/api/upload/' + id, { method: 'DELETE', headers, credentials: 'same-origin' });
      return res.status;
    }, fileId);
    expect(deleted).toBe(200);

    const missing = await page.evaluate(async (id) => {
      const res = await fetch('/api/upload/download/' + id, { credentials: 'same-origin' });
      return res.status;
    }, fileId);
    expect(missing).toBeGreaterThanOrEqual(400);
  });
});
