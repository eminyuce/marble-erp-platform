const { chromium } = require('playwright-core');
const fs = require('fs');
const path = require('path');

const BASE_URL = process.env.ERP_BASE_URL || 'http://localhost:8080';
const CHROME_PATH = process.env.CHROME_PATH || 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';

const OUT_DIR = path.join(__dirname, '..', 'target', 'responsive_screenshots');
if (!fs.existsSync(OUT_DIR)) {
    fs.mkdirSync(OUT_DIR, { recursive: true });
}

async function run() {
    console.log('Testing responsive block edit page: ' + BASE_URL + '/blocks/10/edit');
    const browser = await chromium.launch({
        executablePath: CHROME_PATH,
        headless: true,
        args: ['--no-sandbox', '--disable-setuid-sandbox']
    });

    const context = await browser.newContext();
    const page = await context.newPage();

    // 1. Login
    console.log('Logging in as admin@eimece.test...');
    await page.goto(`${BASE_URL}/account/adminlogin/`, { waitUntil: 'domcontentloaded' });
    await page.fill('#username', 'admin@eimece.test');
    await page.fill('#password', 'B2u5c8JB');
    await Promise.all([
        page.waitForURL('**/admin/dashboard', { timeout: 15000 }),
        page.click('button[type="submit"]')
    ]);
    console.log('Logged in successfully!');

    const viewports = [
        { name: 'desktop_1920', width: 1920, height: 1080 },
        { name: 'tablet_1024', width: 1024, height: 768 },
        { name: 'mobile_390', width: 390, height: 844 }
    ];

    for (const vp of viewports) {
        await page.setViewportSize({ width: vp.width, height: vp.height });
        await page.goto(`${BASE_URL}/blocks/10/edit`, { waitUntil: 'networkidle' });

        const formPageBox = await page.locator('.erp-form-page').boundingBox();
        const cardBox = await page.locator('.erp-form-card').boundingBox();
        const auditCardsCount = await page.locator('.erp-audit-card').count();

        console.log(`\n--- Viewport: ${vp.name} (${vp.width}x${vp.height}) ---`);
        console.log(`  .erp-form-page width: ${formPageBox ? Math.round(formPageBox.width) + 'px' : 'N/A'}`);
        console.log(`  .erp-form-card width: ${cardBox ? Math.round(cardBox.width) + 'px' : 'N/A'}`);
        console.log(`  Audit cards count: ${auditCardsCount}`);

        const screenshotPath = path.join(OUT_DIR, `block_edit_${vp.name}.png`);
        await page.screenshot({ path: screenshotPath, fullPage: true });
        console.log(`  Screenshot saved to: ${screenshotPath}`);
    }

    await browser.close();
    console.log('\nResponsive tests completed successfully!');
}

run().catch(err => {
    console.error('Fatal error:', err);
    process.exit(1);
});
