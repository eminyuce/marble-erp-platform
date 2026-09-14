const { chromium } = require('playwright-core');
const path = require('path');
const fs = require('fs');

const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';
const OUT_DIR = path.resolve(__dirname, '../target/audit_screenshots');

if (!fs.existsSync(OUT_DIR)) {
  fs.mkdirSync(OUT_DIR, { recursive: true });
}

function getBrowserExecutable() {
  const chromePath = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
  const edgePath = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';
  if (fs.existsSync(chromePath)) return chromePath;
  if (fs.existsSync(edgePath)) return edgePath;
  return undefined;
}

const EDIT_PAGES_TO_TEST = [
  { name: 'block_1_detail', path: '/blocks/1', label: 'Blok Detay' },
  { name: 'block_10_edit', path: '/blocks/10/edit', label: 'Blok Düzenle' },
  { name: 'user_1_detail', path: '/admin/users/1', label: 'Kullanıcı Detay' },
  { name: 'user_1_edit', path: '/admin/users/1/edit', label: 'Kullanıcı Düzenle' },
  { name: 'template_1_edit', path: '/admin/settings/templates/1/edit', label: 'E-posta Şablonu Düzenle' },
  { name: 'project_1_edit', path: '/projects/1/edit', label: 'Proje Düzenle' },
  { name: 'project_1_detail', path: '/projects/1', label: 'Proje Detay' },
  { name: 'procurement_1_edit', path: '/procurement/1/edit', label: 'Satınalma Düzenle' },
  { name: 'procurement_1_detail', path: '/procurement/1', label: 'Satınalma Detay' },
  { name: 'sales_1_edit', path: '/sales/1/edit', label: 'Satış Düzenle' },
  { name: 'sales_1_detail', path: '/sales/1', label: 'Satış Detay' },
  { name: 'production_order_1_detail', path: '/production/orders/1', label: 'Üretim Emri Detay' },
  { name: 'production_order_1_edit', path: '/production/orders/1/edit', label: 'Üretim Emri Düzenle' },
  { name: 'workshop_1_detail', path: '/workshop/1', label: 'Atölye Kesim Detay' },
  { name: 'workshop_1_edit', path: '/workshop/1/edit', label: 'Atölye Kesim Düzenle' },
  { name: 'slab_1_detail', path: '/production/slabs/1', label: 'Plaka Detay' },
  { name: 'slab_1_edit', path: '/production/slabs/1/edit', label: 'Plaka Düzenle' }
];

async function run() {
  console.log(`\n=============================================================`);
  console.log(`🚀 COMPREHENSIVE PLAYWRIGHT AUDIT TEST FOR ALL EDIT & AUDIT PAGES`);
  console.log(`Target Base URL: ${BASE_URL}`);
  console.log(`Executable: ${getBrowserExecutable()}`);
  console.log(`=============================================================\n`);

  const browser = await chromium.launch({
    executablePath: getBrowserExecutable(),
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });

  const page = await browser.newPage();
  const pageErrors = [];
  page.on('pageerror', err => pageErrors.push(err.message));

  console.log('[AUTH] Logging in as admin@eimece.test...');
  await page.goto(`${BASE_URL}/account/adminlogin/`, { waitUntil: 'domcontentloaded' });
  await page.fill('#username', 'admin@eimece.test');
  await page.fill('#password', 'B2u5c8JB');
  await Promise.all([
    page.waitForURL('**/admin/dashboard', { timeout: 15000 }),
    page.click('button[type="submit"]')
  ]);
  console.log('[AUTH] ✔ Successfully logged in!\n');

  const summary = [];
  let allPassed = true;

  for (const item of EDIT_PAGES_TO_TEST) {
    const fullUrl = `${BASE_URL}${item.path}`;
    const errsBefore = pageErrors.length;
    console.log(`Testing [${item.label}] -> ${item.path}...`);

    let status = 0;
    let title = '';
    let hasAuditHeader = false;
    let auditCardsFound = 0;
    let cardDetails = [];

    try {
      const res = await page.goto(fullUrl, { waitUntil: 'domcontentloaded', timeout: 15000 });
      status = res ? res.status() : 0;
      await page.waitForTimeout(400);
      title = await page.title();

      // Check for audit information block
      const bodyText = await page.innerText('body');
      hasAuditHeader = bodyText.includes('Denetim Bilgileri');

      // Extract audit cards
      const cards = await page.locator('.erp-audit-card').all();
      auditCardsFound = cards.length;

      for (const card of cards) {
        const label = (await card.locator('.erp-audit-label').innerText().catch(() => '')).trim();
        const value = (await card.locator('.erp-audit-value').innerText().catch(() => '')).trim();
        if (label) cardDetails.push(`${label}: ${value}`);
      }

      // Take screenshot
      const shotFile = `${item.name}.png`;
      await page.screenshot({ path: path.join(OUT_DIR, shotFile), fullPage: true });

    } catch (e) {
      console.error(`  ✖ Error navigating to ${item.path}:`, e.message);
    }

    const newErrors = pageErrors.slice(errsBefore);
    const passed = status === 200 && hasAuditHeader && auditCardsFound === 4 && newErrors.length === 0;
    if (!passed) allPassed = false;

    summary.push({
      Label: item.label,
      Path: item.path,
      HTTP: status,
      AuditHeader: hasAuditHeader ? 'YES' : 'NO',
      AuditCards: auditCardsFound,
      Status: passed ? 'PASS' : 'FAIL',
      CardSample: cardDetails.slice(0, 2).join(' | '),
      Errors: newErrors.length > 0 ? newErrors.join('; ') : 'None'
    });

    console.log(`  ${passed ? '✔ PASS' : '✖ FAIL'} (HTTP ${status}) [Audit Cards: ${auditCardsFound}] - ${cardDetails.slice(0, 2).join(' | ')}`);
  }

  await browser.close();

  console.log(`\n=============================================================`);
  console.log(`📊 EDIT & AUDIT PAGES TEST SUMMARY`);
  console.log(`=============================================================`);
  console.table(summary);

  console.log(`\nScreenshots saved to: ${OUT_DIR}`);

  if (allPassed) {
    console.log(`\n🎉 ALL ${summary.length} EDIT & DETAIL PAGES DISPLAY PROPER TURKISH AUDIT INFORMATION!`);
  } else {
    console.error(`\n❌ SOME PAGES FAILED AUDIT CHECK. Review the table above.`);
    process.exit(1);
  }
}

run().catch(err => {
  console.error('Fatal error:', err);
  process.exit(1);
});
