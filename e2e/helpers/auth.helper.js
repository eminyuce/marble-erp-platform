// @ts-check

const ADMIN_CREDENTIALS = {
  username: process.env.ERP_ADMIN_USER || 'admin@eimece.test',
  password: process.env.ERP_ADMIN_PASSWORD || 'B2u5c8JB',
};

/**
 * Perform login as admin user and wait for dashboard redirect
 * @param {import('@playwright/test').Page} page
 * @param {string} [baseUrl]
 */
async function loginAsAdmin(page, baseUrl) {
  const url = baseUrl ? `${baseUrl}/account/adminlogin/` : '/account/adminlogin/';
  await page.goto(url, { waitUntil: 'domcontentloaded' });

  await page.fill('#username', ADMIN_CREDENTIALS.username);
  await page.fill('#password', ADMIN_CREDENTIALS.password);

  await Promise.all([
    page.waitForURL('**/admin/dashboard', { timeout: 15000 }),
    page.click('button[type="submit"]')
  ]);
}

/**
 * Ensure the page is logged in; if at login page, logs in
 * @param {import('@playwright/test').Page} page
 */
async function ensureLoggedIn(page) {
  const currentUrl = page.url();
  if (currentUrl.includes('/adminlogin') || currentUrl.includes('/login') || currentUrl === 'about:blank') {
    await loginAsAdmin(page);
  }
}

module.exports = {
  ADMIN_CREDENTIALS,
  loginAsAdmin,
  ensureLoggedIn,
};
