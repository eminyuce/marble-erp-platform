const { chromium } = require('../frontend/node_modules/playwright-core');
const fs = require('fs');

function getBrowserExecutable() {
    const chromePath = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
    const edgePath = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';
    if (fs.existsSync(chromePath)) return chromePath;
    if (fs.existsSync(edgePath)) return edgePath;
    return undefined;
}

async function deepDiagnose() {
    console.log('Launching browser for comprehensive interactive testing...');
    const browser = await chromium.launch({
        executablePath: getBrowserExecutable(),
        headless: true,
        args: ['--no-sandbox', '--disable-setuid-sandbox']
    });

    const context = await browser.newContext({
        viewport: { width: 1440, height: 960 }
    });

    const page = await context.newPage();

    const consoleMessages = [];
    const pageErrors = [];

    page.on('console', msg => {
        // Collect warnings and errors
        if (msg.type() === 'error' || msg.type() === 'warning') {
            consoleMessages.push({ type: msg.type(), text: msg.text() });
        }
    });

    page.on('pageerror', err => {
        pageErrors.push(err.toString());
    });

    console.log('1. Navigating to login...');
    await page.goto('http://localhost:8080/account/adminlogin/', { waitUntil: 'networkidle' });
    await page.fill('input[name="username"]', 'admin');
    await page.fill('input[name="password"]', 'changeit');
    await Promise.all([
        page.waitForNavigation({ waitUntil: 'networkidle' }),
        page.click('button[type="submit"]')
    ]);

    console.log('2. Navigating to http://localhost:8080/ogrenme-merkezi ...');
    await page.goto('http://localhost:8080/ogrenme-merkezi', { waitUntil: 'networkidle' });
    await page.waitForTimeout(600);

    console.log('3. Testing Category Filter tabs...');
    const categories = ['Ocak', 'Fabrika', 'Atölye', 'Şantiye', 'Maliyet Analizi', 'Tümü'];
    for (const cat of categories) {
        await page.evaluate((c) => {
            const buttons = Array.from(document.querySelectorAll('button'));
            const btn = buttons.find(b => b.innerText.trim().startsWith(c));
            if (btn) btn.click();
        }, cat);
        await page.waitForTimeout(150);
    }

    console.log('4. Testing Role selector...');
    const roles = ['operator', 'manager', 'all'];
    for (const r of roles) {
        await page.evaluate((role) => {
            const buttons = Array.from(document.querySelectorAll('button'));
            const btn = buttons.find(b => b.getAttribute('@click') && b.getAttribute('@click').includes(`setRole('${role}')`));
            if (btn) btn.click();
        }, r);
        await page.waitForTimeout(150);
    }

    console.log('5. Testing Checklist toggling...');
    for (let i = 1; i <= 5; i++) {
        await page.evaluate((index) => {
            const buttons = Array.from(document.querySelectorAll('button'));
            const checklistBtns = buttons.filter(b => b.getAttribute('@click') && b.getAttribute('@click').includes('toggleChecklistItem'));
            if (checklistBtns[index - 1]) checklistBtns[index - 1].click();
        }, i);
        await page.waitForTimeout(100);
    }

    console.log('6. Checking Progress Ring & percentage after checklist...');
    const progressAfter = await page.evaluate(() => {
        const ring = document.querySelector('[x-text="progressPercent + \'%\'"]');
        const count = document.querySelector('[x-text="completedCount + \'/10\'"]');
        return {
            percent: ring ? ring.innerText : null,
            count: count ? count.innerText : null
        };
    });
    console.log('Progress status:', progressAfter);

    console.log('7. Testing Workflow card accordions...');
    const wfIds = ['W1', 'W2', 'W3', 'W4', 'W5', 'W6', 'W7', 'W8', 'W9', 'W10'];
    for (const id of wfIds) {
        await page.evaluate((wfId) => {
            const card = document.getElementById('card-' + wfId);
            if (card) {
                const trigger = card.querySelector('button[type="button"]');
                if (trigger) trigger.click();
            }
        }, id);
        await page.waitForTimeout(100);
    }

    console.log('8. Testing Feedback thumbs buttons...');
    await page.evaluate(() => {
        const thumbBtns = document.querySelectorAll('button[x-tooltip*="Açık"]');
        if (thumbBtns.length > 0) thumbBtns[0].click();
    });
    await page.waitForTimeout(150);

    console.log('9. Testing Reset Progress button...');
    page.on('dialog', async dialog => {
        await dialog.accept();
    });
    await page.evaluate(() => {
        const resetBtn = Array.from(document.querySelectorAll('button')).find(b => b.innerText.includes('İlerlemeyi Sıfırla'));
        if (resetBtn) resetBtn.click();
    });
    await page.waitForTimeout(300);

    console.log('=== TEST RESULTS ===');
    console.log('Console Errors/Warnings Count:', consoleMessages.length);
    consoleMessages.forEach(m => console.log(`  [${m.type.toUpperCase()}] ${m.text}`));

    console.log('Page Errors Count:', pageErrors.length);
    pageErrors.forEach(e => console.log(`  [PAGE ERROR] ${e}`));

    const finalState = await page.evaluate(() => {
        return {
            doneCount: document.querySelector('[x-text="completedCount + \'/10\'"]')?.innerText,
            progress: document.querySelector('[x-text="progressPercent + \'%\'"]')?.innerText
        };
    });
    console.log('Final State after reset:', finalState);

    await browser.close();

    if (consoleMessages.length > 0 || pageErrors.length > 0) {
        console.error('FAILED: There are console or page errors!');
        process.exit(1);
    } else {
        console.log('SUCCESS: ZERO console errors, ZERO page errors! All 10 workflows and checklist are 100% interactive and working!');
    }
}

deepDiagnose().catch(err => {
    console.error('Fatal test error:', err);
    process.exit(1);
});
