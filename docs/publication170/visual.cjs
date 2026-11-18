'use strict';
const {chromium}=require('../../workspace-desktop/node_modules/playwright');
const fs=require('node:fs/promises'),path=require('node:path'),assert=require('node:assert/strict');
const {pathToFileURL}=require('node:url');
(async()=>{
 const browser=await chromium.launch({channel:'msedge',headless:true}),checks=[];
 try{
  for(const width of [1450,390]){
   const page=await browser.newPage({viewport:{width,height:1000}}),errors=[];
   page.on('pageerror',e=>errors.push(e.message));
   await page.goto(pathToFileURL(path.resolve('docs/readmes/170/index.html')).href);
   assert.equal(await page.locator('#rows tr').count(),6);
   await page.locator('#kind').selectOption('checks');assert.equal(await page.locator('#rows tr').count(),2);
   await page.locator('#kind').selectOption('game');assert.equal(await page.locator('#rows tr').count(),4);
   await page.locator('#module').selectOption('rail');assert.match(await page.locator('#module-detail').textContent(),/RailNavigation.java/);
   await page.locator('#search').fill('RailNavigation.java');assert.equal(await page.locator('#files li').count(),1);
   await page.locator('#course').selectOption('relay-ridge-v1');
   const normal = await page.locator('#clearance').textContent();
   await page.locator('#pace').selectOption('careful');
   assert.notEqual(await page.locator('#clearance').textContent(), normal);
   const before=await page.locator('#position').textContent();await page.locator('#tick').fill(await page.locator('#tick').getAttribute('max'));
   assert.notEqual(await page.locator('#position').textContent(),before);assert.match(await page.locator('#position').textContent(),/finish contact/);
   assert.match(await page.locator('#clearance').textContent(),/Second ridge rail.*entry.*measured clearance/);
   assert.equal(await page.locator('#plot rect').count(),3);
   await page.locator('#shot').selectOption('docs/readmes/170/relay-ridge-careful-finish.png');
   await page.waitForFunction(()=>document.querySelector('#screenshot').complete&&document.querySelector('#screenshot').naturalWidth>0);
   assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));assert.deepEqual(errors,[]);
   await page.screenshot({path:`build/guide170-${width}.png`,fullPage:true});await page.evaluate(()=>scrollTo(0,0));
   await page.screenshot({path:`build/guide170-${width}-viewport.png`});
   checks.push({width,filters:true,sourceSearch:true,courseSelection:true,paceSelection:true,actualMotion:true,screenshotLoaded:true,noPageOverflow:true,errors});await page.close();
  }
 }finally{await browser.close();}
 await fs.writeFile('build/visual170.json',JSON.stringify({passed:true,checks},null,2)+'\n');console.log(JSON.stringify({passed:true,checks}));
})().catch(e=>{console.error(e);process.exitCode=1;});
