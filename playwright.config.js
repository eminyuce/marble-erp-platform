// @ts-check
const { defineConfig } = require('@playwright/test');
const fs = require('fs');

function getBrowserExecutable() {
  const chromePath = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
  const edgePath = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';
  if (fs.existsSync(chromePath)) return chromePath;
  if (fs.existsSync(edgePath)) return edgePath;
  return undefined;
}

const executablePath = getBrowserExecutable();

module.exports = defineConfig({
  testDir: './e2e',
  timeout: 35000,
  expect: {
    timeout: 7000,
  },
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [
    ['list'],
    ['html', { outputFolder: 'target/playwright-report', open: 'never' }]
  ],
  use: {
    baseURL: process.env.BASE_URL || 'http://localhost:8080',
    launchOptions: {
      executablePath: executablePath,
      args: ['--no-sandbox', '--disable-setuid-sandbox']
    },
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'off'
  },
  projects: [
    {
      name: 'desktop',
      use: {
        viewport: { width: 1440, height: 900 }
      }
    },
    {
      name: 'tablet',
      use: {
        viewport: { width: 768, height: 1024 }
      }
    },
    {
      name: 'mobile',
      use: {
        viewport: { width: 375, height: 812 },
        isMobile: true
      }
    }
  ]
});
