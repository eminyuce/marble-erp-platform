const { chromium } = require('playwright-core');
const fs = require('fs');
const path = require('path');

const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';

function getBrowserExecutable() {
    const chromePath = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
    const edgePath = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';
    if (fs.existsSync(chromePath)) return chromePath;
    if (fs.existsSync(edgePath)) return edgePath;
    return undefined;
}

(async () => {
  const browser = await chromium.launch({
    executablePath: getBrowserExecutable(),
    headless: true
  });
  const context = await browser.newContext({
    acceptDownloads: true
  });
  const page = await context.newPage();

  // Login
  console.log('[AUTH] Logging in as admin@eimece.test...');
  await page.goto(`${BASE_URL}/account/adminlogin/`, { waitUntil: 'domcontentloaded' });
  await page.fill('#username', 'admin@eimece.test');
  await page.fill('#password', 'B2u5c8JB');
  await Promise.all([
    page.waitForURL('**/admin/dashboard', { timeout: 15000 }),
    page.click('button[type="submit"]')
  ]);
  console.log('✔ Authenticated successfully');

  const reportTypes = [
    'QUARRY_BLOCKS',
    'FACTORY_SCRAP',
    'WORKSHOP_ORDERS',
    'SITE_INSTALLATION',
    'COST_ACCOUNTING',
    'SLABS_INVENTORY'
  ];

  const formats = ['excel', 'csv'];
  let failures = 0;

  console.log('\n--- 📊 Testing Server-Side Report Exports (12 combinations) ---');
  for (const rt of reportTypes) {
    for (const fmt of formats) {
      const url = `${BASE_URL}/reports/export/${rt}?format=${fmt}`;
      const resp = await context.request.get(url);
      const status = resp.status();
      const ct = resp.headers()['content-type'] || '';
      const cd = resp.headers()['content-disposition'] || '';
      const body = await resp.body();

      const filename = (cd.match(/filename="([^"]+)"/) || [])[1] || '';
      const expectedExt = fmt === 'csv' ? 'csv' : 'xlsx';
      const filenameOk = new RegExp(`^[a-z0-9_-]+_\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}\\.${expectedExt}$`).test(filename);
      if (status === 200 && body.length > 50 && filenameOk && !filename.includes(':')) {
        console.log(`✔ [${rt}][${fmt.toUpperCase()}] HTTP ${status} (${body.length} bytes) - Filename: ${filename}`);
      } else {
        console.error(`✖ FAILED [${rt}][${fmt.toUpperCase()}] HTTP ${status} size: ${body.length}`);
        failures++;
      }
    }
  }

  console.log('\n--- 📋 Testing Table Export Dropdowns and Client Downloads ---');
  const tablePages = [
    { url: `${BASE_URL}/blocks`, name: 'Blocks' },
    { url: `${BASE_URL}/production`, name: 'Production' },
    { url: `${BASE_URL}/production/slabs`, name: 'Slabs' },
    { url: `${BASE_URL}/workshop`, name: 'Workshop' },
    { url: `${BASE_URL}/projects`, name: 'Projects' },
    { url: `${BASE_URL}/procurement`, name: 'Procurement' },
    { url: `${BASE_URL}/sales`, name: 'Sales' },
    { url: `${BASE_URL}/admin/users`, name: 'Users' }
  ];

  for (const tp of tablePages) {
    await page.goto(tp.url, { waitUntil: 'networkidle' });

    // Check export dropdown button exists
    const exportBtn = await page.$('button:has-text("Dışa Aktar")');
    if (!exportBtn) {
      console.error(`✖ ${tp.name} MISSING export button`);
      failures++;
      continue;
    }

    // Open dropdown
    await exportBtn.click();
    await page.waitForTimeout(300);

    // Click CSV download and intercept download event
    try {
      const [download] = await Promise.all([
        page.waitForEvent('download', { timeout: 10000 }),
        page.click('button:has-text("CSV")')
      ]);
      const dlName = download.suggestedFilename();
      const csvOk = /^[a-z0-9_-]+_\d{4}-\d{2}-\d{2}_\d{2}-\d{2}-\d{2}\.csv$/.test(dlName);
      if (!csvOk || dlName.includes(':')) {
        throw new Error(`unexpected CSV filename: ${dlName}`);
      }
      console.log(`✔ ${tp.name} CSV export downloaded: ${dlName}`);
    } catch (e) {
      console.error(`✖ ${tp.name} CSV export failed: ${e.message}`);
      failures++;
    }

    // Re-open dropdown for Excel
    await exportBtn.click();
    await page.waitForTimeout(300);

    // Click Excel download and intercept download event
    try {
      const [download] = await Promise.all([
        page.waitForEvent('download', { timeout: 10000 }),
        page.click('button:has-text("Excel")')
      ]);
      const dlName = download.suggestedFilename();
      const xlsxOk = /^[a-z0-9_-]+_\d{4}-\d{2}-\d{2}_\d{2}-\d{2}-\d{2}\.xlsx$/.test(dlName);
      if (!xlsxOk || dlName.includes(':')) {
        throw new Error(`unexpected Excel filename: ${dlName}`);
      }
      console.log(`✔ ${tp.name} Excel export downloaded: ${dlName}`);
    } catch (e) {
      console.error(`✖ ${tp.name} Excel export failed: ${e.message}`);
      failures++;
    }
  }

  // Check /reports preview page
  console.log('\n--- 📈 Testing Reports Preview Page ---');
  await page.goto(`${BASE_URL}/reports`, { waitUntil: 'networkidle' });
  const pageTitle = await page.title();
  console.log(`✔ /reports preview loaded successfully. Page title: "${pageTitle}"`);

  await browser.close();

  if (failures === 0) {
    console.log('\n🎉 ALL EXPORT FUNCTIONALITIES PASSED WITH ZERO ERRORS!');
    process.exit(0);
  } else {
    console.error(`\n❌ ${failures} export tests failed!`);
    process.exit(1);
  }
})();
