'use strict';
const assert = require('node:assert/strict');
const path = require('node:path');
const {pathToFileURL} = require('node:url');
const {chromium} = require('../../workspace-desktop/node_modules/playwright');

async function main() {
    const browser = await chromium.launch({channel:'msedge'});
    try {
        const page = await browser.newPage({viewport:{width:390,height:840},offline:true});
        const failures = [];
        page.on('pageerror', error => failures.push(error.message));
        const url = pathToFileURL(path.resolve('docs/readmes/187/index.html')).href;
        await page.goto(url);
        assert.equal(await page.locator('img').count(),3);
        assert(await page.locator('img').evaluateAll(images => images.every(image => image.naturalWidth > 0)));
        assert(await page.locator('body').evaluate(() => document.documentElement.scrollWidth <= window.innerWidth));
        await page.locator('#modules').selectOption({label:'Failed save recovery'});
        assert.match(await page.locator('#module-link').getAttribute('href'),/RequestWorkbench\.java$/);
        await page.locator('#filter').fill('request-workbench.js');
        assert.equal(await page.locator('#files li').count(),1);
        await page.locator('a[href="#system"]').click();
        assert(page.url().endsWith('#system'));
        await page.locator('a[href="../186/index.html"]').click();
        assert(page.url().endsWith('/186/index.html'));
        assert.deepEqual(failures,[]);
        console.log(JSON.stringify({passed:true,viewportWidth:390,screenshots:3,localLinks:true,offline:true}));
    } finally { await browser.close(); }
}

main().catch(error=>{console.error(error);process.exitCode=1;});
