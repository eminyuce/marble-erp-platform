const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();
  
  console.log('Navigating to login...');
  await page.goto('http://localhost:8080/account/adminlogin/');
  await page.fill('input[name="username"]', 'admin@eimece.test');
  await page.fill('input[name="password"]', 'B2u5c8JB');
  await page.click('button[type="submit"]');
  await page.waitForURL('**/admin/dashboard');
  console.log('Successfully logged in! Landed on:', page.url());

  // Test Slabs API
  const apiRes = await page.evaluate(async () => {
    const res = await fetch('/production/api/slabs?page=1&size=10');
    return { status: res.status, data: await res.json() };
  });
  console.log('Slabs API status:', apiRes.status);
  console.log('Slabs total elements:', apiRes.data.last_page);
  console.log('Slabs returned count:', apiRes.data.data ? apiRes.data.data.length : 0);
  if (apiRes.data.data && apiRes.data.data.length > 0) {
    const s = apiRes.data.data[0];
    console.log('Sample Slab DTO -> Code:', s.slabCode, 'Area:', s.surfaceAreaM2, 'Cost:', s.costPerM2, 'Status:', s.status);
  }

  // Test /costs page
  const costsResponse = await page.goto('http://localhost:8080/costs');
  console.log('Costs page HTTP status:', costsResponse.status());

  // Test /production/slabs page
  const slabsPageResponse = await page.goto('http://localhost:8080/production/slabs');
  console.log('Production Slabs page HTTP status:', slabsPageResponse.status());
  await page.waitForTimeout(2000);
  const rowCount = await page.evaluate(() => document.querySelectorAll('#slabs-table .tabulator-row').length);
  console.log('Rendered Tabulator rows on /production/slabs:', rowCount);

  await browser.close();
  console.log('Verification finished successfully!');
})();
