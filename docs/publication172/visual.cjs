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
   await page.goto(pathToFileURL(resolve('docs/readmes/172/index.html')).href);
   await page.waitForFunction(()=>document.querySelector('#sample-result').textContent.includes('1.7239'));
   await page.locator('#revision').selectOption('2');
   assert.match(await page.locator('#sample-result').textContent(),/4\.2392/);
   const chart=await page.locator('#plot').evaluate(c=>c.toDataURL());
   await page.locator('#sample').fill('0');
   assert.notEqual(await page.locator('#plot').evaluate(c=>c.toDataURL()),chart);
   assert.match(await page.locator('#sample-result').textContent(),/0\.000 s/);
   await page.locator('#module').selectOption('world');
   assert.match(await page.locator('#module-detail').textContent(),/RampMission\.java/);
   await page.locator('#search').fill('RampMission.java');
   assert.equal(await page.locator('#files li').count(),1);
   await page.locator('#shot').selectOption('ramp-checkpoint.png');
   await page.waitForFunction(()=>document.querySelector('#screenshot').complete&&document.querySelector('#screenshot').naturalWidth>0);
   await page.locator('nav a[href="#measurements"]').click();
   assert.match(page.url(),/#measurements$/);
   assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
   assert.deepEqual(errors,[]);assert.deepEqual(external,[]);
   await page.screenshot({path:'build/guide172-'+width+'.png',fullPage:true});
   checks.push({width,plotAndSamples:true,sourceSearch:true,screenshot:true,offline:true});
   await page.close();
  }
 }finally{await browser.close();}
 await writeFile('build/visual172.json',JSON.stringify({passed:true,checks},null,2)+'\n');
}

main().catch(error=>{console.error(error);process.exitCode=1;});
