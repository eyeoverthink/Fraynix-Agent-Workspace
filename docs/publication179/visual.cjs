'use strict';

const assert = require('node:assert/strict');
const {writeFile} = require('node:fs/promises');
const {resolve} = require('node:path');
const {pathToFileURL} = require('node:url');
const {chromium} = require('../../workspace-desktop/node_modules/playwright');

async function main() {
  const browser = await chromium.launch({channel:'msedge', headless:true});
  const checks = [];
  try {
    for(const width of [1420, 390]){
      const page = await browser.newPage({viewport:{width,height:960}});
      const remote = [], errors = [];
      page.on('request', req=>{if(!req.url().startsWith('file:'))remote.push(req.url());});
      page.on('pageerror', error=>errors.push(error.message));
      await page.goto(pathToFileURL(resolve('docs/readmes/179/index.html')).href);
      assert.match(await page.locator('h1').textContent(), /Choose a workflow with a local model/);
      const images = await page.locator('figure img').evaluateAll(nodes=>nodes.map(img=>img.complete&&img.naturalWidth>0));
      assert(images.length >= 4 && images.every(Boolean));
      assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
      assert.deepEqual(remote,[]);
      assert.deepEqual(errors,[]);
      checks.push({width,captures:images.length,offline:true});
      await page.close();
    }
  }finally{await browser.close();}
  await writeFile('build/visual179.json',JSON.stringify({passed:true,checks},null,2)+'\n');
}
main().catch(error=>{console.error(error);process.exitCode=1;});
