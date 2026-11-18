'use strict';

const { chromium } = require('../../workspace-desktop/node_modules/playwright');
const assert = require('node:assert/strict');
const { writeFile } = require('node:fs/promises');
const { resolve } = require('node:path');
const { pathToFileURL } = require('node:url');

async function main() {
  const browser = await chromium.launch({ channel: 'msedge', headless: true });
  const checks = [];
  try {
    for (const width of [1450, 390]) {
      const page = await browser.newPage({ viewport: { width, height: 1020 } });
      const errors = [], requests = [];
      page.on('pageerror', error => errors.push(error.message));
      page.on('request', request => { if (!request.url().startsWith('file:')) requests.push(request.url()); });
      await page.goto(pathToFileURL(resolve('docs/readmes/175/index.html')).href);
      await page.waitForFunction(() => document.querySelector('#pair-result').textContent.includes('retained sample'));
      assert.match(await page.locator('#direct-label').textContent(), /Direct.+ s/);
      const before = await page.locator('#model-bar').getAttribute('style');
      await page.locator('#pair').selectOption('creation');
      assert.match(await page.locator('#pair-result').textContent(), /separately generated apps/);
      assert.notEqual(await page.locator('#model-bar').getAttribute('style'), before);
      await page.locator('#probe').selectOption('ambiguous');
      assert.match(await page.locator('#probe-result').textContent(), /Which result.+no child started/);
      await page.locator('#module').selectOption('selector');
      assert.match(await page.locator('#module-detail').textContent(), /LocalWorkflowSelection\.java/);
      await page.locator('#search').fill('LocalWorkflowSelection.java');
      assert.equal(await page.locator('#files li').count(), 1);
      await page.locator('#shot').selectOption('routing-creation.png');
      await page.waitForFunction(() => document.querySelector('#screenshot').complete && document.querySelector('#screenshot').naturalWidth > 0);
      assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
      assert.deepEqual(errors, []);
      assert.deepEqual(requests, []);
      checks.push({ width, timings: true, probes: true, sourceSearch: true, screenshot: true, offline: true });
      await page.screenshot({ path: 'build/guide175-' + width + '.png', fullPage: true });
      await page.close();
    }
  } finally {
    await browser.close();
  }
  await writeFile('build/visual175.json', JSON.stringify({ passed: true, checks }, null, 2) + '\n');
}

main().catch(error => { console.error(error); process.exitCode = 1; });
