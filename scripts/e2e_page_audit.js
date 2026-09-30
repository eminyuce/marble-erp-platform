// @ts-check
const { chromium } = require('playwright-core');
const fs = require('fs');
const path = require('path');

const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';

// List of all critical routes across Özerler Mermer ERP
const ROUTES = [
    { name: 'Yönetici Kokpiti', path: '/admin/dashboard' },
    { name: '10 Adımda Öğrenme Merkezi', path: '/ogrenme-merkezi' },
    { name: 'Sitemizin Özellikleri', path: '/admin/dashboard/oursitefeatures/' },
    { name: 'Ocak — Blok Takibi', path: '/blocks' },
    { name: 'Makine Mazot Takibi', path: '/machines/fuel' },
    { name: 'Fabrika / Kesim İş Emirleri', path: '/production' },
    { name: 'Plaka Stok Sahası', path: '/production/slabs' },
    { name: 'Atölye', path: '/workshop' },
    { name: 'Şantiyeler', path: '/projects' },
    { name: 'Satışlar', path: '/sales' },
    { name: 'Satın Alma', path: '/procurement' },
    { name: 'Giderler ve Satın Alma', path: '/expenses' },
    { name: 'Maliyet Analizi', path: '/costs' },
    { name: 'Rapor Merkezi', path: '/reports' },
    { name: 'Tanımlar - Makineler', path: '/admin/definitions/machines' },
    { name: 'Tanımlar - Stok Sahaları', path: '/admin/definitions/stock-locations' },
    { name: 'Tanımlar - Ocaklar', path: '/admin/definitions/quarries' },
    { name: 'Tanımlar - Müşteriler', path: '/admin/definitions/customers' },
    { name: 'Tanımlar - Tedarikçiler', path: '/admin/definitions/suppliers' },
    { name: 'Tanımlar - Masraf Merkezleri', path: '/admin/definitions/cost-centers' },
    { name: 'Sistem - Kullanıcılar', path: '/admin/users' },
    { name: 'Sistem - Ayarlar', path: '/admin/settings' },
    { name: 'Sistem Sağlığı', path: '/admin/dashboard/systemhealth/' }
];

function getBrowserExecutable() {
    const chromePath = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
    const edgePath = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';
    if (fs.existsSync(chromePath)) return chromePath;
    if (fs.existsSync(edgePath)) return edgePath;
    return undefined;
}

async function runAudit() {
    console.log('====================================================');
    console.log('🔍 ÖZERLER ERP — UÇTAN UCA SAYFA DENETİMİ BAŞLATILIYOR');
    console.log(`🌐 Hedef URL: ${BASE_URL}`);
    console.log('====================================================\n');

    const executablePath = getBrowserExecutable();
    console.log(`Tarayıcı yürütülebilir dosyası: ${executablePath || 'Playwright varsayılanı'}`);

    const browser = await chromium.launch({
        executablePath: executablePath,
        headless: true,
        args: ['--no-sandbox', '--disable-setuid-sandbox']
    });

    const context = await browser.newContext({
        viewport: { width: 1440, height: 900 }
    });

    const page = await context.newPage();

    const auditResults = {
        timestamp: new Date().toISOString(),
        baseUrl: BASE_URL,
        totalPagesChecked: ROUTES.length,
        passedPages: 0,
        failedPages: 0,
        totalErrors: 0,
        details: []
    };

    // -------------------------------------------------------------
    // Adım 1: Kimlik Doğrulama (Admin Login)
    // -------------------------------------------------------------
    console.log('🔑 Giriş yapılıyor (admin@eimece.test)...');
    try {
        await page.goto(`${BASE_URL}/account/adminlogin/`, { waitUntil: 'domcontentloaded', timeout: 15000 });
        const adminUser = process.env.ERP_ADMIN_USER || 'admin@eimece.test';
        const adminPass = process.env.ERP_ADMIN_PASSWORD || 'B2u5c8JB';
        await page.fill('#username', adminUser);
        await page.fill('#password', adminPass);
        
        await Promise.all([
            page.waitForNavigation({ waitUntil: 'domcontentloaded', timeout: 15000 }),
            page.click('button[type="submit"]')
        ]);

        if (!page.url().includes('/admin/dashboard')) {
            console.warn(`⚠ Login yönlendirmesi dashboard'a düşmedi, mevcut URL: ${page.url()}`);
        } else {
            console.log('✔ Giriş başarılı, oturum doğrulandı.\n');
        }
    } catch (authErr) {
        console.error('❌ Kimlik doğrulama başarısız! Sunucunun ayakta olduğunu (port 8080) kontrol edin.', authErr.message);
        await browser.close();
        process.exit(1);
    }

    // -------------------------------------------------------------
    // Adım 2: Tüm Rotaları Tek Tek Denetleme
    // -------------------------------------------------------------
    for (const route of ROUTES) {
        const fullUrl = `${BASE_URL}${route.path}`;
        const pageErrors = [];
        const consoleErrors = [];
        const failedRequests = [];

        const consoleListener = (msg) => {
            if (msg.type() === 'error') {
                const text = msg.text();
                // Harici tarayıcı eklenti hatalarını filtrele
                if (!text.includes('chrome-extension://') && !text.includes('polkadot') && !text.includes('metamask')) {
                    consoleErrors.push(text);
                }
            }
        };

        const pageErrorListener = (err) => {
            const msg = err.message || String(err);
            if (!msg.includes('contentscript.js')) {
                pageErrors.push(msg);
            }
        };

        const responseListener = (res) => {
            const status = res.status();
            const url = res.url();
            if (status >= 500) {
                failedRequests.push(`HTTP ${status}: ${url}`);
            } else if (status === 404 && (url.includes('/css/') || url.includes('/js/') || url.includes('/static/'))) {
                failedRequests.push(`Asset 404: ${url}`);
            }
        };

        page.on('console', consoleListener);
        page.on('pageerror', pageErrorListener);
        page.on('response', responseListener);

        let loadSuccess = true;
        let spelErrorFound = false;
        let whitelabelFound = false;

        try {
            await page.goto(fullUrl, { waitUntil: 'domcontentloaded', timeout: 12000 });
            // Dinamik bileşenlerin (Alpine.js, Tabulator, Lucide) yüklenmesini bekle
            await page.waitForTimeout(1500);

            const content = await page.content();
            if (content.includes('Exception evaluating SpringEL expression')) {
                spelErrorFound = true;
                pageErrors.push('SpringEL Expression Evaluation Error');
            }
            if (content.includes('Whitelabel Error Page')) {
                whitelabelFound = true;
                pageErrors.push('Whitelabel 500 Error Page');
            }
        } catch (navErr) {
            loadSuccess = false;
            pageErrors.push(`Navigasyon hatası: ${navErr.message}`);
        } finally {
            page.off('console', consoleListener);
            page.off('pageerror', pageErrorListener);
            page.off('response', responseListener);
        }

        const isClean = loadSuccess && pageErrors.length === 0 && consoleErrors.length === 0 && failedRequests.length === 0 && !spelErrorFound && !whitelabelFound;

        const routeResult = {
            name: route.name,
            path: route.path,
            status: isClean ? 'PASS' : 'FAIL',
            pageErrors,
            consoleErrors,
            failedRequests
        };

        auditResults.details.push(routeResult);

        if (isClean) {
            auditResults.passedPages++;
            console.log(`[PASS] ✔ ${route.name.padEnd(32)} ${route.path}`);
        } else {
            auditResults.failedPages++;
            const pageTotalIssues = pageErrors.length + consoleErrors.length + failedRequests.length;
            auditResults.totalErrors += pageTotalIssues;
            console.log(`[FAIL] ❌ ${route.name.padEnd(32)} ${route.path}`);
            if (consoleErrors.length > 0) {
                consoleErrors.forEach(e => console.log(`       └─ [Console Error]: ${e}`));
            }
            if (pageErrors.length > 0) {
                pageErrors.forEach(e => console.log(`       └─ [Page Error]: ${e}`));
            }
            if (failedRequests.length > 0) {
                failedRequests.forEach(e => console.log(`       └─ [Network/Asset]: ${e}`));
            }
        }
    }

    await browser.close();

    // -------------------------------------------------------------
    // Adım 3: Raporu Kaydetme ve Çıktı Özeti
    // -------------------------------------------------------------
    const targetDir = path.resolve(__dirname, '../target');
    if (!fs.existsSync(targetDir)) {
        fs.mkdirSync(targetDir, { recursive: true });
    }
    const reportPath = path.join(targetDir, 'e2e-audit-report.json');
    fs.writeFileSync(reportPath, JSON.stringify(auditResults, null, 2), 'utf-8');

    console.log('\n====================================================');
    console.log('📊 DENETİM SONUÇ RAPORU');
    console.log(`Toplam Kontrol Edilen Sayfa : ${auditResults.totalPagesChecked}`);
    console.log(`Başarılı (PASS)             : ${auditResults.passedPages}`);
    console.log(`Hatalı (FAIL)               : ${auditResults.failedPages}`);
    console.log(`Toplam Tespit Edilen Hata   : ${auditResults.totalErrors}`);
    console.log(`Ayrıntılı Rapor             : ${reportPath}`);
    console.log('====================================================\n');

    if (auditResults.totalErrors > 0) {
        console.error('❌ Sistemde çözülmesi gereken sayfa/konsol hataları mevcut!');
        process.exit(1);
    } else {
        console.log('🎉 Tebrikler! Tüm sayfalar konsol, ağ ve render hatası olmadan tertemiz çalışıyor.');
        process.exit(0);
    }
}

runAudit().catch(err => {
    console.error('Beklenmeyen denetim hatası:', err);
    process.exit(1);
});
