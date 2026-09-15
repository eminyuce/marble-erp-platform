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
    // Auth & Info
    { name: '01_login', path: '/account/adminlogin/', isPublic: true },
    { name: '02_dashboard', path: '/admin/dashboard', isPublic: false },
    { name: '03_system_health', path: '/admin/dashboard/systemhealth/', isPublic: false },
    { name: '04_site_features', path: '/admin/dashboard/oursitefeatures/', isPublic: false },
    { name: '06_health', path: '/health/', isPublic: true },

    // Blocks
    { name: '07_blocks', path: '/blocks', isPublic: false, tabulatorId: '#blocks-table' },
    { name: '08_block_create', path: '/blocks/create', isPublic: false },
    { name: '09_block_detail', path: '/blocks/1', isPublic: false },
    { name: '09b_block_edit', path: '/blocks/1/edit', isPublic: false },

    // Production & Slabs
    { name: '10_production', path: '/production', isPublic: false },
    { name: '11_production_create', path: '/production/create', isPublic: false },
    { name: '12_production_order_detail', path: '/production/orders/1', isPublic: false },
    { name: '12b_production_order_edit', path: '/production/orders/1/edit', isPublic: false },
    { name: '13_slabs', path: '/production/slabs', isPublic: false, tabulatorId: '#slabs-table' },
    { name: '13b_slab_detail', path: '/production/slabs/1', isPublic: false },
    { name: '13c_slab_edit', path: '/production/slabs/1/edit', isPublic: false },
    { name: '14_slab_label', path: '/production/slabs/1/label', isPublic: false },

    // Workshop
    { name: '15_workshop', path: '/workshop', isPublic: false },
    { name: '16_workshop_create', path: '/workshop/create', isPublic: false },
    { name: '17_workshop_detail', path: '/workshop/1', isPublic: false },
    { name: '17b_workshop_edit', path: '/workshop/1/edit', isPublic: false },

    // Projects (including project 2 which failed with MECHANICAL_ANCHOR)
    { name: '18_projects', path: '/projects', isPublic: false },
    { name: '19_project_create', path: '/projects/create', isPublic: false },
    { name: '20_project_detail_1', path: '/projects/1', isPublic: false },
    { name: '21_project_detail_2_error_route', path: '/projects/2', isPublic: false },
    { name: '22_project_detail_3', path: '/projects/3', isPublic: false },
    { name: '23_project_edit_1', path: '/projects/1/edit', isPublic: false },

    // Procurement
    { name: '24_procurement', path: '/procurement', isPublic: false },
    { name: '25_procurement_create', path: '/procurement/create', isPublic: false },
    { name: '26_procurement_detail_1', path: '/procurement/1', isPublic: false },
    { name: '27_procurement_edit_1', path: '/procurement/1/edit', isPublic: false },

    // Sales
    { name: '28_sales', path: '/sales', isPublic: false },
    { name: '29_sales_create', path: '/sales/create', isPublic: false },
    { name: '30_sales_detail_1', path: '/sales/1', isPublic: false },
    { name: '31_sales_edit_1', path: '/sales/1/edit', isPublic: false },

    // Costs & Reports & Genealogy
    { name: '32_costs', path: '/costs', isPublic: false },
    { name: '33_reports', path: '/reports', isPublic: false },
    { name: '34_genealogy', path: '/genealogy', isPublic: false },
    { name: '35_passport', path: '/passport/SLB-2026-000101', isPublic: true },

    // Admin & Users & Settings
    { name: '36_users', path: '/admin/users', isPublic: false },
    { name: '37_user_create', path: '/admin/users/create', isPublic: false },
    { name: '38_user_detail_1', path: '/admin/users/1', isPublic: false },
    { name: '38b_user_edit_1', path: '/admin/users/1/edit', isPublic: false },
    { name: '39_user_reset_password_1', path: '/admin/users/1/reset-password', isPublic: false },
    { name: '40_settings', path: '/admin/settings', isPublic: false },
    { name: '42_definitions_suppliers', path: '/admin/definitions/suppliers', isPublic: false, tabulatorId: '#suppliers-table' },
    { name: '43_definitions_customers', path: '/admin/definitions/customers', isPublic: false, tabulatorId: '#customers-table' },
    { name: '44_definitions_machines', path: '/admin/definitions/machines', isPublic: false, tabulatorId: '#machines-table' },
    { name: '45_definitions_stock_locations', path: '/admin/definitions/stock-locations', isPublic: false, tabulatorId: '#stock-locations-table' },
    { name: '46_definitions_quarries', path: '/admin/definitions/quarries', isPublic: false, tabulatorId: '#quarries-table' },
    { name: '47_definitions_cost_centers', path: '/admin/definitions/cost-centers', isPublic: false, tabulatorId: '#cost-centers-table' },
    { name: '48_production_polish', path: '/production/polish', isPublic: false },
    { name: '49_production_pallets', path: '/production/pallets', isPublic: false },
    { name: '50_production_tablet', path: '/production/tablet', isPublic: false }
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
    console.log(`Total Pages to audit: ${PAGES_TO_TEST.length}`);
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
                await page.waitForTimeout(300);
                const screenshotFile = `${vp.name}_drawer_open.png`;
                await page.screenshot({ path: path.join(OUT_DIR, screenshotFile) });

                const closeBtn = page.locator('aside[x-show="mobileMenuOpen"] button');
                if (await closeBtn.isVisible()) {
                    await closeBtn.click();
                } else {
                    await page.keyboard.press('Escape');
                }
                await page.waitForTimeout(200);
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
                await page.waitForTimeout(300);
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
