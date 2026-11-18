'use strict';

const {chromium} = require('../../workspace-desktop/node_modules/playwright');
const assert = require('node:assert/strict');
const {writeFile} = require('node:fs/promises');
const {resolve} = require('node:path');
const {pathToFileURL} = require('node:url');

async function main() {
  const browser = await chromium.launch({channel: 'msedge', headless: true});
  const checks = [];
  try {
    for (const width of [1450, 390]) {
      const page = await browser.newPage({viewport: {width, height: 980}});
      const errors = [];
      const external = [];
      page.on('pageerror', error => errors.push(error.message));
      page.on('request', request => { if (!request.url().startsWith('file:')) external.push(request.url()); });
      await page.goto(pathToFileURL(resolve('docs/readmes/171/index.html')).href);
      await page.locator('#rows tr').first().waitFor();
      assert.equal(await page.locator('#rows tr').count(), 9);
      await page.locator('#pace').selectOption('steady');
      assert.equal(await page.locator('#rows tr').count(), 3);
      await page.locator('#course').selectOption('Relay Switchyard');
      assert.equal(await page.locator('#rows tr').count(), 2);
      await page.locator('#module').selectOption('runtime');
      assert.match(await page.locator('#module-detail').textContent(), /RelayPlaytest.java/);
      await page.locator('#search').fill('RelayPlaytest.java');
      assert.equal(await page.locator('#files li').count(), 1);
      await page.locator('#shot').selectOption('relay-mission-resume.png');
      await page.waitForFunction(() => document.querySelector('#screenshot').complete && document.querySelector('#screenshot').naturalWidth > 0);
      assert.match(await page.locator('#original').getAttribute('href'), /relay-mission-resume.png/);
      await page.locator('nav a[href="#measurements"]').click();
      assert.match(page.url(), /#measurements$/);
      assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
      assert.deepEqual(external, []);
      assert.deepEqual(errors, []);
      await page.screenshot({path: 'build/guide171-' + width + '.png', fullPage: true});
      checks.push({width, courseAndPace: true, sourceSearch: true, navigation: true, screenshot: true, external, errors});
      await page.close();
    }
  } finally {
    await browser.close();
  }
  await writeFile('build/visual171.json', JSON.stringify({passed: true, checks}, null, 2) + '\n');
}

main().catch(error => { console.error(error); process.exitCode = 1; });
