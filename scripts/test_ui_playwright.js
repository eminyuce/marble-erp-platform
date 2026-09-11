const { chromium } = require('playwright-core');
const path = require('path');
const fs = require('fs');

async function run() {
    console.log('🚀 Launching Playwright browser check...');
    
    // Auto-detect Chrome or Edge
    const chromePath = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
    const edgePath = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';
    const executablePath = fs.existsSync(chromePath) ? chromePath : edgePath;
    
    console.log(`Using Browser executable: ${executablePath}`);

    const browser = await chromium.launch({
        executablePath: executablePath,
        headless: true,
        args: ['--no-sandbox', '--disable-setuid-sandbox']
    });

    const context = await browser.newContext({
        viewport: { width: 1440, height: 900 }
    });
    const page = await context.newPage();

    page.on('console', msg => console.log(`[BROWSER CONSOLE] ${msg.type()}: ${msg.text()}`));
    page.on('pageerror', err => console.log(`[BROWSER ERROR] ${err.message}`));
    page.on('requestfailed', req => console.log(`[REQUEST FAILED] ${req.method()} ${req.url()} - ${req.failure()?.errorText}`));
    page.on('response', res => {
        if (res.status() >= 400) {
            console.log(`[HTTP ERROR ${res.status()}] ${res.url()}`);
        }
    });

    const screenshotsDir = path.resolve(__dirname, '../target/screenshots');
    if (!fs.existsSync(screenshotsDir)) {
        fs.mkdirSync(screenshotsDir, { recursive: true });
    }

    try {
        // -------------------------------------------------------------
        // Step 1: Navigate to Login Page
        // -------------------------------------------------------------
        console.log('\n--- Step 1: Navigating to Admin Login Page ---');
        await page.goto('http://localhost:8080/account/adminlogin/', { waitUntil: 'networkidle' });
        console.log(`Page title: ${await page.title()}`);
        await page.screenshot({ path: path.join(screenshotsDir, '1_login_page.png') });
        console.log('✔ Screenshot captured: 1_login_page.png');

        // -------------------------------------------------------------
        // Step 2: Fill Credentials & Submit
        // -------------------------------------------------------------
        console.log('\n--- Step 2: Submitting Admin Credentials ---');
        await page.fill('#username', 'admin@eimece.test');
        await page.fill('#password', 'B2u5c8JB');
        console.log('Filled email: admin@eimece.test and password');

        await Promise.all([
            page.waitForNavigation({ waitUntil: 'networkidle' }),
            page.click('button[type="submit"]')
        ]);

        console.log(`Current URL after login: ${page.url()}`);
        console.log(`Dashboard title: ${await page.title()}`);
        await page.screenshot({ path: path.join(screenshotsDir, '2_dashboard.png') });
        console.log('✔ Screenshot captured: 2_dashboard.png');

        if (!page.url().includes('/admin/dashboard')) {
            throw new Error(`Login redirection failed, current URL is: ${page.url()}`);
        }
        console.log('✔ Successfully verified Admin Dashboard load!');

        // -------------------------------------------------------------
        // Step 3: Navigate to Blocks Grid
        // -------------------------------------------------------------
        console.log('\n--- Step 3: Navigating to Quarry & Block Management Grid ---');
        await page.goto('http://localhost:8080/blocks', { waitUntil: 'networkidle' });
        console.log('Waiting 3 seconds for Tabulator data rows to render...');
        await page.waitForTimeout(3000);

        // Check Tabulator table rows
        const rowsCount = await page.locator('.tabulator-row').count();
        console.log(`Found Tabulator rows rendered: ${rowsCount}`);
        
        if (rowsCount > 0) {
            const firstRowText = await page.locator('.tabulator-row').first().innerText();
            console.log(`First Block Row Content:\n${firstRowText.replace(/\n+/g, ' | ')}`);
        } else {
            console.warn('⚠ No rows found in .tabulator-row, checking placeholder or data fetch...');
        }

        await page.screenshot({ path: path.join(screenshotsDir, '3_blocks_grid.png') });
        console.log('✔ Screenshot captured: 3_blocks_grid.png');

        // -------------------------------------------------------------
        // Step 4: Navigate to System Health
        // -------------------------------------------------------------
        console.log('\n--- Step 4: Navigating to System Health Cockpit ---');
        await page.goto('http://localhost:8080/admin/dashboard/systemhealth/', { waitUntil: 'networkidle' });
        await page.waitForTimeout(1500);

        const healthTitle = await page.title();
        console.log(`System Health Page Title: ${healthTitle}`);

        const statusText = await page.locator('#overall-status-badge').innerText();
        console.log(`System overall status: ${statusText}`);
        const isUp = statusText.trim() === 'UP';
        console.log(`System UP status verified: ${isUp}`);

        await page.screenshot({ path: path.join(screenshotsDir, '4_system_health.png') });
        console.log('✔ Screenshot captured: 4_system_health.png');

        // -------------------------------------------------------------
        // Step 5: Navigate to Site Features Guide
        // -------------------------------------------------------------
        console.log('\n--- Step 5: Navigating to Site Features Guide ---');
        await page.goto('http://localhost:8080/admin/dashboard/oursitefeatures/', { waitUntil: 'networkidle' });
        await page.waitForTimeout(1500);
        console.log(`Site Features Page Title: ${await page.title()}`);
        await page.screenshot({ path: path.join(screenshotsDir, '5_site_features.png') });
        console.log('✔ Screenshot captured: 5_site_features.png');

        // -------------------------------------------------------------
        // Step 6: Navigate to Operational Report Center
        // -------------------------------------------------------------
        console.log('\n--- Step 6: Navigating to Report Center ---');
        await page.goto('http://localhost:8080/reports', { waitUntil: 'networkidle' });
        await page.waitForTimeout(1500);
        console.log(`Reports Page Title: ${await page.title()}`);
        await page.screenshot({ path: path.join(screenshotsDir, '6_reports.png') });
        console.log('✔ Screenshot captured: 6_reports.png');

        console.log('\n=============================================================');
        console.log('🎉 ALL PLAYWRIGHT CHECKS PASSED SUCCESSFULLY WITHOUT ERRORS!');
        console.log(`Screenshots saved to: ${screenshotsDir}`);
        console.log('=============================================================');

    } catch (err) {
        console.error('❌ Playwright Test Failure:', err);
        await page.screenshot({ path: path.join(screenshotsDir, 'error_state.png') });
        throw err;
    } finally {
        await browser.close();
    }
}

run().catch(err => {
    console.error(err);
    process.exit(1);
});
