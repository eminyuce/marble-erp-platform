const { chromium } = require('playwright-core');
const fs = require('fs');
const path = require('path');

(async () => {
    const chromePath = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
    const edgePath = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';
    const executablePath = fs.existsSync(chromePath) ? chromePath : edgePath;

    const browser = await chromium.launch({ executablePath, headless: true });
    const context = await browser.newContext({ viewport: { width: 375, height: 812 } });
    const page = await context.newPage();

    console.log('Logging in...');
    await page.goto('http://localhost:8080/account/adminlogin/', { waitUntil: 'domcontentloaded' });
    await page.fill('#username', '');
    await page.fill('#password', '');
    await Promise.all([
        page.waitForNavigation({ waitUntil: 'domcontentloaded' }),
        page.click('button[type="submit"]')
    ]);

    const urls = [
        ['dashboard', 'http://localhost:8080/admin/dashboard'],
        ['blocks', 'http://localhost:8080/blocks'],
        ['production', 'http://localhost:8080/production'],
        ['slabs', 'http://localhost:8080/production/slabs'],
        ['projects', 'http://localhost:8080/projects'],
        ['sales', 'http://localhost:8080/sales'],
        ['settings', 'http://localhost:8080/admin/settings'],
        ['health', 'http://localhost:8080/admin/dashboard/systemhealth/']
    ];

    const outDir = path.resolve(__dirname, '../target/mobile_audit');
    if (!fs.existsSync(outDir)) fs.mkdirSync(outDir, { recursive: true });

    for (const [name, url] of urls) {
        await page.goto(url, { waitUntil: 'domcontentloaded' });
        await page.waitForTimeout(1000);
        const overflow = await page.evaluate(() => {
            const elementsCausingOverflow = [];
            document.querySelectorAll('*').forEach(el => {
                const rect = el.getBoundingClientRect();
                if (rect.right > window.innerWidth + 1) {
                    elementsCausingOverflow.push({
                        tag: el.tagName,
                        id: el.id,
                        className: (el.className || '').toString().slice(0, 50),
                        right: Math.round(rect.right),
                        width: Math.round(rect.width)
                    });
                }
            });
            return {
                windowWidth: window.innerWidth,
                docElemScrollWidth: document.documentElement.scrollWidth,
                bodyScrollWidth: document.body.scrollWidth,
                hasHorizontalScroll: document.documentElement.scrollWidth > window.innerWidth,
                overflowElementsCount: elementsCausingOverflow.length,
                sampleOverflows: elementsCausingOverflow.slice(0, 5)
            };
        });
        console.log(`[${name}] overflow:`, JSON.stringify(overflow));
        await page.screenshot({ path: path.join(outDir, `${name}_375.png`), fullPage: false });
    }

    // Also test mega menu open
    await page.goto('http://localhost:8080/admin/dashboard', { waitUntil: 'domcontentloaded' });
    await page.click('#adminMegaMenuOpen');
    await page.waitForTimeout(500);
    await page.screenshot({ path: path.join(outDir, 'megamenu_opened_375.png') });

    // Tablet 768x1024
    const tabletContext = await browser.newContext({ viewport: { width: 768, height: 1024 } });
    const tabletPage = await tabletContext.newPage();
    await tabletPage.goto('http://localhost:8080/account/adminlogin/', { waitUntil: 'domcontentloaded' });
    await tabletPage.fill('#username', 'admin@eimece.test');
    await tabletPage.fill('#password', 'B2u5c8JB');
    await Promise.all([
        tabletPage.waitForNavigation({ waitUntil: 'domcontentloaded' }),
        tabletPage.click('button[type="submit"]')
    ]);
    await tabletPage.goto('http://localhost:8080/admin/dashboard', { waitUntil: 'domcontentloaded' });
    await tabletPage.screenshot({ path: path.join(outDir, 'tablet_dashboard_768.png') });
    await tabletPage.goto('http://localhost:8080/blocks', { waitUntil: 'domcontentloaded' });
    await tabletPage.waitForTimeout(1000);
    await tabletPage.screenshot({ path: path.join(outDir, 'tablet_blocks_768.png') });

    // Landscape 812x375 (iPhone X/12 landscape)
    const lsContext = await browser.newContext({ viewport: { width: 812, height: 375 } });
    const lsPage = await lsContext.newPage();
    await lsPage.goto('http://localhost:8080/account/adminlogin/', { waitUntil: 'domcontentloaded' });
    await lsPage.fill('#username', 'admin@eimece.test');
    await lsPage.fill('#password', 'B2u5c8JB');
    await Promise.all([
        lsPage.waitForNavigation({ waitUntil: 'domcontentloaded' }),
        lsPage.click('button[type="submit"]')
    ]);
    await lsPage.goto('http://localhost:8080/admin/dashboard', { waitUntil: 'domcontentloaded' });
    await lsPage.screenshot({ path: path.join(outDir, 'landscape_dashboard_812.png') });
    await lsPage.goto('http://localhost:8080/blocks', { waitUntil: 'domcontentloaded' });
    await lsPage.waitForTimeout(1000);
    await lsPage.screenshot({ path: path.join(outDir, 'landscape_blocks_812.png') });

    await browser.close();
    console.log('Mobile audit completed!');
})();
