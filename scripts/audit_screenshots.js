const { chromium } = require('playwright-core');
const path = require('path');
const fs = require('fs');

const VIEWPORTS = [
    { name: 'desktop', width: 1440, height: 900 },
    { name: 'tablet', width: 768, height: 1024 },
    { name: 'mobile', width: 375, height: 812 }
];

const BASE_URL = 'http://localhost:8080';
const OUT_DIR = path.resolve(__dirname, '../target/audit_screenshots');

if (!fs.existsSync(OUT_DIR)) {
    fs.mkdirSync(OUT_DIR, { recursive: true });
}

async function runAudit() {
    const chromePath = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
    const edgePath = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';
    const executablePath = fs.existsSync(chromePath) ? chromePath : edgePath;

    const browser = await chromium.launch({
        executablePath,
        headless: true,
        args: ['--no-sandbox', '--disable-setuid-sandbox']
    });

    for (const vp of VIEWPORTS) {
        console.log(`\n📸 Starting capture for Viewport: ${vp.name.toUpperCase()} (${vp.width}x${vp.height})`);
        const context = await browser.newContext({ viewport: { width: vp.width, height: vp.height } });
        const page = await context.newPage();

        // 1. Login Page
        await page.goto(`${BASE_URL}/account/adminlogin/`, { waitUntil: 'domcontentloaded' });
        await page.screenshot({ path: path.join(OUT_DIR, `${vp.name}_01_login.png`) });

        // 1b. Login Error State (on desktop only)
        if (vp.name === 'desktop') {
            await page.fill('#username', 'wrong@admin.test');
            await page.fill('#password', 'wrongpassword');
            await Promise.all([
                page.waitForNavigation({ waitUntil: 'domcontentloaded' }),
                page.click('button[type="submit"]')
            ]);
            await page.screenshot({ path: path.join(OUT_DIR, `desktop_01b_login_error.png`) });
        }

        // 2. Perform Real Login
        await page.goto(`${BASE_URL}/account/adminlogin/`, { waitUntil: 'domcontentloaded' });
        await page.fill('#username', 'admin@eimece.test');
        await page.fill('#password', 'B2u5c8JB');
        await Promise.all([
            page.waitForNavigation({ waitUntil: 'domcontentloaded' }),
            page.click('button[type="submit"]')
        ]);

        // 3. Admin Dashboard
        await page.goto(`${BASE_URL}/admin/dashboard`, { waitUntil: 'domcontentloaded' });
        await page.waitForTimeout(1000);
        await page.screenshot({ path: path.join(OUT_DIR, `${vp.name}_02_dashboard.png`), fullPage: true });

        // 3b. Mobile Drawer Open (on mobile & tablet)
        if (vp.name !== 'desktop') {
            const toggleBtn = page.locator('#sidebar-toggle');
            if (await toggleBtn.isVisible()) {
                await toggleBtn.click();
                await page.waitForTimeout(500);
                await page.screenshot({ path: path.join(OUT_DIR, `${vp.name}_02b_drawer_open.png`) });
                // Close it back
                const closeBtn = page.locator('aside[x-show="mobileMenuOpen"] button');
                if (await closeBtn.isVisible()) {
                    await closeBtn.click();
                    await page.waitForTimeout(300);
                } else {
                    await page.keyboard.press('Escape');
                    await page.waitForTimeout(300);
                }
            }
        }

        // 4. Blocks Grid
        await page.goto(`${BASE_URL}/blocks`, { waitUntil: 'domcontentloaded' });
        await page.waitForTimeout(2000);
        await page.screenshot({ path: path.join(OUT_DIR, `${vp.name}_03_blocks.png`), fullPage: true });

        // 4b. Blocks Modal (desktop)
        if (vp.name === 'desktop') {
            const newBlockBtn = page.locator('button:has-text("Yeni Blok Kaydı")');
            if (await newBlockBtn.isVisible()) {
                await newBlockBtn.click();
                await page.waitForTimeout(800);
                await page.screenshot({ path: path.join(OUT_DIR, `desktop_03b_blocks_modal.png`) });
                await page.keyboard.press('Escape');
                await page.waitForTimeout(300);
            }
        }

        // 5. Production Orders
        await page.goto(`${BASE_URL}/production`, { waitUntil: 'domcontentloaded' });
        await page.waitForTimeout(2000);
        await page.screenshot({ path: path.join(OUT_DIR, `${vp.name}_04_production.png`), fullPage: true });

        // 6. Slabs Inventory
        await page.goto(`${BASE_URL}/production/slabs`, { waitUntil: 'domcontentloaded' });
        await page.waitForTimeout(2000);
        await page.screenshot({ path: path.join(OUT_DIR, `${vp.name}_05_slabs.png`), fullPage: true });

        // 7. Workshop
        await page.goto(`${BASE_URL}/workshop`, { waitUntil: 'domcontentloaded' });
        await page.waitForTimeout(2000);
        await page.screenshot({ path: path.join(OUT_DIR, `${vp.name}_06_workshop.png`), fullPage: true });

        // 8. Projects
        await page.goto(`${BASE_URL}/projects`, { waitUntil: 'domcontentloaded' });
        await page.waitForTimeout(2000);
        await page.screenshot({ path: path.join(OUT_DIR, `${vp.name}_07_projects.png`), fullPage: true });

        // 9. Project Detail (Project 1)
        await page.goto(`${BASE_URL}/projects/1`, { waitUntil: 'domcontentloaded' });
        await page.waitForTimeout(1000);
        await page.screenshot({ path: path.join(OUT_DIR, `${vp.name}_08_project_detail.png`), fullPage: true });

        // 10. Cost & Pricing Engine
        await page.goto(`${BASE_URL}/costs`, { waitUntil: 'domcontentloaded' });
        await page.waitForTimeout(1000);
        await page.screenshot({ path: path.join(OUT_DIR, `${vp.name}_09_costs.png`), fullPage: true });

        // 11. Reports Center
        await page.goto(`${BASE_URL}/reports`, { waitUntil: 'domcontentloaded' });
        await page.waitForTimeout(1000);
        await page.screenshot({ path: path.join(OUT_DIR, `${vp.name}_10_reports.png`), fullPage: true });

        // 12. User Management
        await page.goto(`${BASE_URL}/admin/users`, { waitUntil: 'domcontentloaded' });
        await page.waitForTimeout(2000);
        await page.screenshot({ path: path.join(OUT_DIR, `${vp.name}_11_users.png`), fullPage: true });

        // 13. System Settings
        await page.goto(`${BASE_URL}/admin/settings`, { waitUntil: 'domcontentloaded' });
        await page.waitForTimeout(1000);
        await page.screenshot({ path: path.join(OUT_DIR, `${vp.name}_12_settings.png`), fullPage: true });

        // 14. System Health Cockpit
        await page.goto(`${BASE_URL}/admin/dashboard/systemhealth/`, { waitUntil: 'domcontentloaded' });
        await page.waitForTimeout(1000);
        await page.screenshot({ path: path.join(OUT_DIR, `${vp.name}_13_system_health.png`), fullPage: true });

        // 15. Site Features & Guide
        await page.goto(`${BASE_URL}/admin/dashboard/oursitefeatures/`, { waitUntil: 'domcontentloaded' });
        await page.waitForTimeout(1000);
        await page.screenshot({ path: path.join(OUT_DIR, `${vp.name}_14_site_features.png`), fullPage: true });

        // 16. Digital Genealogy Trace
        await page.goto(`${BASE_URL}/genealogy`, { waitUntil: 'domcontentloaded' });
        await page.waitForTimeout(1000);
        await page.screenshot({ path: path.join(OUT_DIR, `${vp.name}_15_genealogy.png`), fullPage: true });

        // 17. Public Passport
        await page.goto(`${BASE_URL}/passport/SLB-2026-000101`, { waitUntil: 'domcontentloaded' });
        await page.waitForTimeout(1000);
        await page.screenshot({ path: path.join(OUT_DIR, `${vp.name}_16_passport.png`), fullPage: true });

        await context.close();
        console.log(`✔ Finished ${vp.name.toUpperCase()} screenshots!`);
    }

    await browser.close();
    console.log('\n🎉 ALL VIEWPORT & STATE SCREENSHOTS CAPTURED SUCCESSFULLY!');
}

runAudit().catch(err => {
    console.error('Audit failed:', err);
    process.exit(1);
});
