const { chromium } = require('../frontend/node_modules/playwright-core');
const fs = require('fs');
const path = require('path');

const ARTIFACT_DIR = 'C:\\Users\\eminy\\.gemini\\antigravity-cli\\brain\\abbfe9fa-85dd-47fb-8b8b-f9928365f308';

function getBrowserExecutable() {
    const chromePath = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
    const edgePath = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';
    if (fs.existsSync(chromePath)) return chromePath;
    if (fs.existsSync(edgePath)) return edgePath;
    return undefined;
}

async function run() {
    console.log('Launching browser...');
    const browser = await chromium.launch({
        executablePath: getBrowserExecutable(),
        headless: true,
        args: ['--no-sandbox', '--disable-setuid-sandbox']
    });

    const context = await browser.newContext({
        viewport: { width: 1440, height: 960 }
    });

    const page = await context.newPage();

    console.log('Navigating to login page...');
    await page.goto('http://localhost:8080/account/adminlogin/', { waitUntil: 'networkidle' });

    console.log('Attempting login with admin / changeit...');
    await page.fill('input[name="username"]', 'admin');
    await page.fill('input[name="password"]', 'changeit');
    await Promise.all([
        page.waitForNavigation({ waitUntil: 'networkidle' }),
        page.click('button[type="submit"]')
    ]);

    console.log('Current URL after login attempt:', page.url());

    console.log('Navigating to Öğrenme Merkezi: http://localhost:8080/ogrenme-merkezi ...');
    await page.goto('http://localhost:8080/ogrenme-merkezi', { waitUntil: 'networkidle' });
    await page.waitForTimeout(1000);

    // 1. Full Page Screenshot
    const shot1 = path.join(ARTIFACT_DIR, '01_learning_center_overview.png');
    await page.screenshot({ path: shot1, fullPage: true });
    console.log('Saved shot 1:', shot1);

    // 2. Top Header & Checklist Viewport Screenshot
    const shot2 = path.join(ARTIFACT_DIR, '02_checklist_and_header.png');
    await page.screenshot({ path: shot2 });
    console.log('Saved shot 2:', shot2);

    // 3. Toggle Demo Mode via clicking the label
    console.log('Toggling Demo Mode...');
    await page.evaluate(() => {
        const toggle = document.querySelector('input[type="checkbox"][x-model="demoMode"]');
        if (toggle) {
            toggle.click();
        }
    });
    await page.waitForTimeout(600);

    // 4. Click Checklist Item 1
    console.log('Clicking Checklist Item 1 button...');
    await page.evaluate(() => {
        // Find first checklist item toggle button
        const buttons = document.querySelectorAll('button');
        for (const b of buttons) {
            if (b.getAttribute('@click') && b.getAttribute('@click').includes('toggleChecklistItem')) {
                b.click();
                break;
            }
        }
    });
    await page.waitForTimeout(600);

    const shot3 = path.join(ARTIFACT_DIR, '03_progress_updated.png');
    await page.screenshot({ path: shot3 });
    console.log('Saved shot 3:', shot3);

    // 5. Scroll to Workflow Cards & expand card W1
    console.log('Scrolling to W1 card...');
    await page.evaluate(() => {
        const el = document.getElementById('card-W1');
        if (el) el.scrollIntoView({ behavior: 'instant', block: 'start' });
    });
    await page.waitForTimeout(600);

    const shot4 = path.join(ARTIFACT_DIR, '04_workflow_w1_expanded.png');
    await page.screenshot({ path: shot4 });
    console.log('Saved shot 4:', shot4);

    // 6. Scroll down to Empty State guide
    console.log('Scrolling to Empty State guide...');
    await page.evaluate(() => {
        window.scrollTo(0, document.body.scrollHeight);
    });
    await page.waitForTimeout(600);

    const shot5 = path.join(ARTIFACT_DIR, '05_empty_state_guide.png');
    await page.screenshot({ path: shot5 });
    console.log('Saved shot 5:', shot5);

    await browser.close();
    console.log('All 5 screenshots saved successfully to artifact directory!');
}

run().catch(err => {
    console.error('Walkthrough error:', err);
    process.exit(1);
});
