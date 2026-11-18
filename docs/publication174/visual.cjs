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
   await page.goto(pathToFileURL(resolve('docs/readmes/174/index.html')).href);
   await page.waitForFunction(()=>document.querySelector('#sample-result').textContent.includes('difference'));
   const a=await page.locator('#sample-result').textContent();
   assert.match(a,/difference -/);
   await page.locator('#revision').selectOption('reversed');
   const b=await page.locator('#sample-result').textContent();
   assert.notEqual(a,b);assert.match(b,/difference [0-9]/);
   const chart=await page.locator('#plot').evaluate(c=>c.toDataURL());
   await page.locator('#sample').fill('0');
   assert.notEqual(await page.locator('#plot').evaluate(c=>c.toDataURL()),chart);
   await page.locator('#route').selectOption('route-6');
   assert.match(await page.locator('#route-outcome').textContent(),/clarification.*no physics/i);
   await page.locator('nav a[href="#routing"]').click();assert.match(page.url(),/#routing$/);
   await page.locator('#module').selectOption('comparison');
   assert.match(await page.locator('#module-detail').textContent(),/RampComparison\.java/);
   await page.locator('#search').fill('RampComparison.java');
   assert.equal(await page.locator('#files li').count(),1);
   await page.locator('#shot').selectOption('paired-ramp-input.png');
   await page.waitForFunction(()=>document.querySelector('#screenshot').complete&&document.querySelector('#screenshot').naturalWidth>0);
   assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
   assert.deepEqual(errors,[]);assert.deepEqual(external,[]);
   await page.screenshot({path:'build/guide174-'+width+'.png',fullPage:true});
   checks.push({width,casesAndPlot:true,routing:true,sourceSearch:true,screenshot:true,offline:true});
   await page.close();
  }
 }finally{await browser.close();}
 await writeFile('build/visual174.json',JSON.stringify({passed:true,checks},null,2)+'\n');
}

main().catch(error=>{console.error(error);process.exitCode=1;});
