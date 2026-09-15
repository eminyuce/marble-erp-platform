// @ts-check
const { expect } = require('@playwright/test');

/**
 * Attaches console, network error, and page error listeners to a page.
 * Returns an object with accumulated errors and unhandled exceptions.
 * @param {import('@playwright/test').Page} page
 */
function setupErrorTracking(page) {
  const pageErrors = [];
  const httpFailures = [];

  page.on('pageerror', (err) => {
    // Ignore harmless 3rd-party extension errors if injected by Chrome
    const msg = err.message || '';
    if (!msg.includes('contentscript.js') && !msg.includes('polkadot') && !msg.includes('metamask')) {
      pageErrors.push(msg);
    }
  });

  page.on('response', (res) => {
    // Flag unexpected server errors (500, 502, 503) or missing assets (404 for application css/js)
    const status = res.status();
    const url = res.url();
    if (status >= 500) {
      httpFailures.push(`HTTP ${status} on ${url}`);
    } else if (status === 404 && (url.includes('/css/') || url.includes('/js/') || url.includes('/static/'))) {
      httpFailures.push(`Asset 404 on ${url}`);
    }
  });

  return {
    pageErrors,
    httpFailures,
    assertCleanState: async () => {
      // Also verify page content doesn't show Spring Boot white label error or Whitelabel / SpEL error
      const content = await page.content();
      const hasSpelError = content.includes('Exception evaluating SpringEL expression');
      const hasWhitelabel = content.includes('Whitelabel Error Page') || content.includes('Geliştirici hata ayrıntısı');
      
      expect(hasSpelError, `Page rendered a SpringEL expression error: ${page.url()}`).toBeFalsy();
      expect(hasWhitelabel, `Page rendered an unhandled server error / exception: ${page.url()}`).toBeFalsy();
      expect(pageErrors, `Page JavaScript errors found: ${pageErrors.join(' | ')}`).toHaveLength(0);
      expect(httpFailures, `Backend 500 or asset failures detected: ${httpFailures.join(' | ')}`).toHaveLength(0);
    }
  };
}

module.exports = {
  setupErrorTracking,
};
