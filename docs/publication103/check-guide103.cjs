'use strict';
const {chromium}=require('../workspace-desktop/node_modules/playwright');
const fs=require('node:fs/promises'),path=require('node:path'),assert=require('node:assert/strict');
(async()=>{
 const browser=await chromium.launch({channel:'msedge',headless:true}),page=await browser.newPage(),errors=[],checks=[];
 page.on('pageerror',e=>errors.push(e.message));
 try {
  for(const width of [1440,390]){
   await page.setViewportSize({width,height:1000});
   await page.goto('file:///'+path.resolve('docs/readmes/103/index.html').replaceAll('\\','/'));
   assert.equal(await page.locator('#case option').count(),6);
   for(let i=0;i<6;i++){
    await page.locator('#case').selectOption(String(i));
    await page.waitForFunction(()=>document.querySelector('#shot').complete&&document.querySelector('#shot').naturalWidth>0);
    const data=await page.locator('#record').textContent();const r=JSON.parse(data);
    assert((await page.locator('#verdict').innerText()).includes(i<3?'Verified':'Refused'));
    assert((await page.locator('#cost').innerText()).includes((r.totalMs/1000).toFixed(3)));
    assert.equal(r.browserMs+r.restartMs+r.otherMs,r.totalMs);
    assert.equal(await page.locator('#shot').getAttribute('src'),r.name+'-1600.png');
    const widths=await page.locator('.bar > div').evaluateAll(es=>es.map(e=>parseFloat(e.style.width)));
    assert(Math.abs(widths.reduce((a,b)=>a+b,0)-100)<0.001);
   }
   await page.locator('#case').selectOption('0');
   await page.waitForFunction(()=>document.querySelector('#shot').complete&&document.querySelector('#shot').naturalWidth>0);
   await page.locator('details').last().locator('summary').click();
   assert.equal(await page.locator('#modules tbody tr').count(),6);
   for(const href of await page.locator('a').evaluateAll(es=>es.map(e=>e.getAttribute('href')))){
    if(href&&!href.startsWith('http')&&!href.startsWith('#')) await fs.access(path.resolve('docs/readmes/103',href));
   }
   assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'horizontal overflow at '+width);
   await page.screenshot({path:`build/guide103-${width}.png`,fullPage:true});
   checks.push(width+'px: all six result selections, actual images, exact elapsed partitions, six module links, all local links and no horizontal overflow');
  }
  assert.deepEqual(errors,[]);
  await fs.writeFile('build/visual103.json',JSON.stringify({passed:true,checks,errors},null,2));
  console.log(JSON.stringify({passed:true,checks}));
 } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
