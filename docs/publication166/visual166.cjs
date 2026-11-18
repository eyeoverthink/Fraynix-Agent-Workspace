'use strict';
const { chromium } = require('../../workspace-desktop/node_modules/playwright');
const fs = require('node:fs/promises');
const path = require('node:path');
const { pathToFileURL } = require('node:url');
const assert = require('node:assert/strict');
(async () => {
  const browser = await chromium.launch({ channel: 'msedge', headless: true });
  const checks = [];
  try {
    for (const width of [1450, 390]) {
      const page = await browser.newPage({ viewport: { width, height: 1000 } });
      const errors = [];
      page.on('pageerror', error => errors.push(error.message));
      await page.goto(pathToFileURL(path.resolve('docs/readmes/166/index.html')).href);
      assert.equal(await page.locator('#rows tr').count(), 10);
      await page.locator('#kind').selectOption('summary');
      assert.equal(await page.locator('#rows tr').count(), 2);
      await page.locator('#kind').selectOption('motion');
      assert.equal(await page.locator('#rows tr').count(), 8);
      await page.locator('#module').selectOption('archive');
      assert.match(await page.locator('#module-detail').textContent(), /history_archive.py/);
      await page.locator('#search').fill('SmallObjectRoutes.java');
      assert.equal(await page.locator('#files li').count(), 1);
      await page.locator('#shot').selectOption('docs/readmes/166/fresh-data-summary.png');
      await page.waitForFunction(() => document.querySelector('#screenshot').complete && document.querySelector('#screenshot').naturalWidth > 0);
      assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
      assert.deepEqual(errors, []);
      await page.screenshot({ path: `build/guide166-${width}.png`, fullPage: true });
      await page.evaluate(() => scrollTo(0, 0));
      await page.screenshot({ path: `build/guide166-${width}-viewport.png` });
      checks.push({ width, taskFiltering: true, moduleSelection: true, inventorySearch: true,
        actualScreenshotLoaded: true, noPageOverflow: true, errors });
      await page.close();
    }
  } finally { await browser.close(); }
  await fs.writeFile('build/visual166.json', JSON.stringify({ passed: true, checks }, null, 2));
  console.log(JSON.stringify({ passed: true, checks }));
})().catch(error => { console.error(error); process.exitCode = 1; });
