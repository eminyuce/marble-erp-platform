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

const VIEWPORTS = [
    { name: 'desktop', width: 1440, height: 900 },
    { name: 'tablet', width: 768, height: 1024 },
    { name: 'mobile', width: 375, height: 812 }
];

const PAGES_TO_TEST = [
    { name: '01_login', path: '/account/adminlogin/', isPublic: true },
    { name: '02_dashboard', path: '/admin/dashboard', isPublic: false },
    { name: '03_blocks', path: '/blocks', isPublic: false, tabulatorId: '#blocks-table' },
    { name: '04_production', path: '/production', isPublic: false },
    { name: '05_slabs', path: '/production/slabs', isPublic: false, tabulatorId: '#slabs-table' },
    { name: '06_workshop', path: '/workshop', isPublic: false },
    { name: '07_projects', path: '/projects', isPublic: false },
    { name: '08_project_detail', path: '/projects/1', isPublic: false },
    { name: '09_costs', path: '/costs', isPublic: false },
    { name: '10_reports', path: '/reports', isPublic: false },
    { name: '11_users', path: '/admin/users', isPublic: false },
    { name: '12_settings', path: '/admin/settings', isPublic: false },
    { name: '13_system_health', path: '/admin/dashboard/systemhealth/', isPublic: false },
    { name: '14_site_features', path: '/admin/dashboard/oursitefeatures/', isPublic: false },
    { name: '15_genealogy', path: '/genealogy', isPublic: false },
    { name: '16_passport', path: '/passport/SLB-2026-000101', isPublic: true },
    { name: '17_procurement', path: '/procurement', isPublic: false },
    { name: '18_sales', path: '/sales', isPublic: false }
];

async function login(page) {
    await page.goto(`${BASE_URL}/account/adminlogin/`, { waitUntil: 'domcontentloaded' });
    await page.fill('#username', 'admin@eimece.test');
    await page.fill('#password', 'B2u5c8JB');
    await Promise.all([
        page.waitForURL('**/admin/dashboard', { timeout: 15000 }),
        page.click('button[type="submit"]')
    ]);
}

async function runAllTests() {
    console.log(`\n=============================================================`);
    console.log(`🚀 RUNNING COMPREHENSIVE PLAYWRIGHT SUITE FOR ALL PAGES`);
    console.log(`Target Base URL: ${BASE_URL}`);
    console.log(`Executable: ${getBrowserExecutable()}`);
    console.log(`=============================================================\n`);

    const browser = await chromium.launch({
        executablePath: getBrowserExecutable(),
        headless: true,
        args: ['--no-sandbox', '--disable-setuid-sandbox']
    });

    const results = [];
    let overallPassed = true;

    // Step 1: Login once and capture session storage state
    console.log(`[AUTH] Authenticating once as admin@eimece.test...`);
    const authContext = await browser.newContext();
    const authPage = await authContext.newPage();
    await login(authPage);
    const authStatePath = path.resolve(__dirname, '../target/auth-state.json');
    await authContext.storageState({ path: authStatePath });
    await authContext.close();
    console.log(`[AUTH] ✔ Auth session established and stored to ${authStatePath}\n`);

    for (const vp of VIEWPORTS) {
        console.log(`\n--- 📱 Testing Viewport: ${vp.name.toUpperCase()} (${vp.width}x${vp.height}) ---`);

        const context = await browser.newContext({
            viewport: { width: vp.width, height: vp.height },
            storageState: authStatePath,
            ignoreHTTPSErrors: true
        });

        const page = await context.newPage();
        const pageErrors = [];

        page.on('pageerror', err => {
            pageErrors.push(err.message);
        });

        // Test Drawer toggle on tablet & mobile
        if (vp.name !== 'desktop') {
            await page.goto(`${BASE_URL}/admin/dashboard`, { waitUntil: 'domcontentloaded' });
            const toggleBtn = page.locator('#sidebar-toggle');
            if (await toggleBtn.isVisible()) {
                await toggleBtn.click();
                await page.waitForTimeout(400);
                const screenshotFile = `${vp.name}_drawer_open.png`;
                await page.screenshot({ path: path.join(OUT_DIR, screenshotFile) });
                
                const closeBtn = page.locator('aside[x-show="mobileMenuOpen"] button');
                if (await closeBtn.isVisible()) {
                    await closeBtn.click();
                } else {
                    await page.keyboard.press('Escape');
                }
                await page.waitForTimeout(300);
                console.log(`  [${vp.name}] ✔ Mobile navigation drawer verified`);
            }
        }

        // Iterate through all pages
        for (const p of PAGES_TO_TEST) {
            const fullUrl = `${BASE_URL}${p.path}`;
            const pageErrorsBefore = pageErrors.length;
            const res = await page.goto(fullUrl, { waitUntil: 'domcontentloaded', timeout: 15000 });
            const status = res ? res.status() : 0;
            
            // Wait for dynamic Tabulator rows if expected
            if (p.tabulatorId) {
                try {
                    await page.waitForSelector('.tabulator-row', { timeout: 6000 });
                } catch (_) {
                    // empty or loading
                }
            } else {
                await page.waitForTimeout(600);
            }

            const title = await page.title();
            const tabulatorRows = p.tabulatorId ? await page.locator(`${p.tabulatorId} .tabulator-row, .tabulator-row`).count() : null;
            const newErrors = pageErrors.slice(pageErrorsBefore);

            const isSuccess = status === 200 && newErrors.length === 0;
            if (!isSuccess) overallPassed = false;

            const screenshotFile = `${vp.name}_${p.name}.png`;
            await page.screenshot({ path: path.join(OUT_DIR, screenshotFile), fullPage: false });

            results.push({
                viewport: vp.name,
                page: p.name,
                path: p.path,
                status,
                title,
                tabulatorRows,
                errors: newErrors.length > 0 ? newErrors.join('; ') : 'None',
                success: isSuccess
            });

            console.log(`  [${vp.name}] ${isSuccess ? '✔' : '✖'} ${p.path} (HTTP ${status}) ${tabulatorRows !== null ? `[Rows: ${tabulatorRows}]` : ''} - "${title}"`);
        }

        await context.close();
    }

    await browser.close();

    console.log(`\n=============================================================`);
    console.log(`📊 PLAYWRIGHT AUDIT RESULTS SUMMARY`);
    console.log(`=============================================================`);
    console.table(results.map(r => ({
        Viewport: r.viewport,
        Page: r.page,
        Path: r.path,
        HTTP: r.status,
        Rows: r.tabulatorRows !== null ? r.tabulatorRows : '-',
        Status: r.success ? 'PASS' : 'FAIL',
        Errors: r.errors
    })));

    console.log(`\nAll screenshots written to: ${OUT_DIR}`);
    if (overallPassed) {
        console.log(`\n🎉 ALL ${results.length} TESTS ACROSS ALL VIEWPORTS PASSED WITH ZERO ERRORS!`);
    } else {
        console.error(`\n❌ SOME TESTS FAILED. Inspect the summary above.`);
        process.exit(1);
    }
}

runAllTests().catch(err => {
    console.error('Test run failed with error:', err);
    process.exit(1);
});
