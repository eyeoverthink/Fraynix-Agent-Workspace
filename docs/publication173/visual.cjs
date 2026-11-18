'use strict';

const {chromium}=require('../../workspace-desktop/node_modules/playwright');
const assert=require('node:assert/strict');
const {writeFile}=require('node:fs/promises');
const {resolve}=require('node:path');
const {pathToFileURL}=require('node:url');

async function main(){
 const browser=await chromium.launch({channel:'msedge',headless:true}),checks=[];
 try{
  for(const width of [1450,390]){
   const page=await browser.newPage({viewport:{width,height:980}}),errors=[],external=[];
   page.on('pageerror',error=>errors.push(error.message));
   page.on('request',request=>{if(!request.url().startsWith('file:'))external.push(request.url());});
   await page.goto(pathToFileURL(resolve('docs/readmes/173/index.html')).href);
   await page.waitForFunction(()=>document.querySelector('#sample-result').textContent.includes('original'));
   await page.locator('#revision').selectOption('direct');
   const direct=await page.locator('#sample-result').textContent();
   assert.match(await page.locator('#trial-time').textContent(),/0 model calls/);
   await page.locator('#revision').selectOption('planned');
   assert.equal(await page.locator('#sample-result').textContent(),direct);
   assert.match(await page.locator('#trial-time').textContent(),/1 model call\b/);
   const chart=await page.locator('#plot').evaluate(c=>c.toDataURL());
   await page.locator('#sample').fill('0');
   assert.notEqual(await page.locator('#plot').evaluate(c=>c.toDataURL()),chart);
   assert.match(await page.locator('#sample-result').textContent(),/0\.000 s/);
   await page.locator('#module').selectOption('catalog');
   assert.match(await page.locator('#module-detail').textContent(),/RampProcedure\.java/);
   await page.locator('#search').fill('RampProcedure.java');
   assert.equal(await page.locator('#files li').count(),1);
   await page.locator('#shot').selectOption('ramp-csv-import.png');
   await page.waitForFunction(()=>document.querySelector('#screenshot').complete&&document.querySelector('#screenshot').naturalWidth>0);
   await page.locator('nav a[href="#measurements"]').click();
   assert.match(page.url(),/#measurements$/);
   assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
   assert.deepEqual(errors,[]);assert.deepEqual(external,[]);
   await page.screenshot({path:'build/guide173-'+width+'.png',fullPage:true});
   checks.push({width,comparisonAndPlot:true,sourceSearch:true,screenshot:true,offline:true});
   await page.close();
  }
 }finally{await browser.close();}
 await writeFile('build/visual173.json',JSON.stringify({passed:true,checks},null,2)+'\n');
}

main().catch(error=>{console.error(error);process.exitCode=1;});
