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
      await page.goto(pathToFileURL(resolve('docs/readmes/177/index.html')).href);
      await page.waitForFunction(() => document.querySelector('#trace-detail').textContent.includes('second setup'));
      assert.match(await page.locator('#sample-result').textContent(), /0\.800 s.*1\.629 m/);
      const initial = await page.locator('#plot').evaluate(element => element.toDataURL());
      await page.locator('#sample').fill('0');
      assert.notEqual(await page.locator('#plot').evaluate(element => element.toDataURL()), initial);
      assert.match(await page.locator('#sample-result').textContent(), /0\.000 s/);
      await page.locator('#trial').selectOption('automatic');
      assert.match(await page.locator('#trace-detail').textContent(), /first setup.*17 original samples/);
      await page.locator('#probe').selectOption('tie');
      assert.match(await page.locator('#probe-result').textContent(), /tie.*No summary started/);
      await page.locator('#module').selectOption('handoff');
      assert.match(await page.locator('#module-detail').textContent(), /RampTraceChain\.java/);
      await page.locator('#search').fill('RampTraceChain.java');
      assert.equal(await page.locator('#files li').count(), 1);
      await page.locator('#shot').selectOption('chain-first-verified.png');
      await page.waitForFunction(() => document.querySelector('#screenshot').complete && document.querySelector('#screenshot').naturalWidth > 0);
      assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
      assert.deepEqual(errors, []);
      assert.deepEqual(requests, []);
      checks.push({ width, trace: true, refusals: true, sourceSearch: true, screenshot: true, offline: true });
      await page.screenshot({ path: 'build/guide177-' + width + '.png', fullPage: true });
      await page.close();
    }
  } finally {
    await browser.close();
  }
  await writeFile('build/visual177.json', JSON.stringify({ passed: true, checks }, null, 2) + '\n');
}

main().catch(error => { console.error(error); process.exitCode = 1; });
