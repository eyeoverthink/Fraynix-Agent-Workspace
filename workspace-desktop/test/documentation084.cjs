'use strict';
const {chromium}=require('playwright'),fs=require('node:fs/promises'),path=require('node:path'),{pathToFileURL}=require('node:url'),assert=require('node:assert/strict');
(async()=>{
 const repo=path.resolve(__dirname,'../..'),output=path.join(repo,'build/docs084-review');await fs.mkdir(output,{recursive:true});const browser=await chromium.launch({channel:'msedge',headless:true});const checks=[],errors=[];
 try{for(const width of [1440,390]){
  const page=await browser.newPage({viewport:{width,height:1000}});page.on('pageerror',e=>errors.push(e.message));await page.goto(pathToFileURL(path.join(repo,'docs/readmes/084/index.html')).href);assert.equal(await page.title(),'Edition 084 · Repeated task costs');
  assert((await page.locator('#cards').innerText()).includes('24/24'));assert((await page.locator('#cards').innerText()).includes('8/8'));
  for(let group=0;group<2;group++){
   await page.selectOption('#group',String(group));assert(await page.locator('#summary').innerText());await page.locator('#results').evaluate(e=>e.scrollIntoView({block:'start'}));await page.screenshot({path:path.join(output,'results-'+width+'-'+group+'.png')});
   for(const metric of ['completeMillis','callerMillis','checkMillis','ticks']){
    await page.selectOption('#metric',metric);assert.equal(await page.locator('#bars rect').count(),3);
    const expected=await page.evaluate(k=>Object.values(study084.active().summary.totals).map(x=>(x[k]/(k==='ticks'?1:1000)).toFixed(k==='ticks'?0:2)),metric);const visible=await page.locator('#bars').textContent();for(const x of expected)assert(visible.includes(x));
   }
   await page.selectOption('#metric','completeMillis');const predictionText=await page.locator('#predictions').innerText();assert(!predictionText.includes('NaN'));const cases=await page.evaluate(()=>study084.active().summary.cases);for(const c of cases){assert(predictionText.includes(c.id));assert(predictionText.includes((c.observedSavingMillis/1000).toFixed(2)));if(!c.estimate.available){assert(predictionText.includes('Unavailable'));assert(predictionText.includes('changed receipt'));}}
   await page.locator('#cost').evaluate(e=>e.scrollIntoView({block:'start'}));await page.screenshot({path:path.join(output,'cost-'+width+'-'+group+'.png')});
   for(const method of ['simple','memory','calibrated']){await page.selectOption('#method',method);const goals=await page.locator('#case option').count();assert.equal(goals,4);for(let goal=0;goal<goals;goal++){await page.selectOption('#case',String(goal));const stages=await page.locator('#stage option').count();assert(stages>0);for(let stage=0;stage<stages;stage++){await page.selectOption('#stage',String(stage));const n=Number(await page.locator('#frame').getAttribute('max'));for(const frame of [0,Math.floor(n/2),n]){await page.locator('#frame').evaluate((e,v)=>{e.value=String(v);e.dispatchEvent(new Event('input',{bubbles:true}));},frame);const actual=await page.evaluate(()=>({text:document.querySelector('#pose').textContent,s:study084.currentStage().samples[+document.querySelector('#frame').value]}));assert(actual.text.includes('Native tick '+actual.s.tick));for(const b of actual.s.bodies)assert(actual.text.includes(b.position.map(x=>x.toFixed(3)).join(', ')));}}}}
   await page.locator('#replay').evaluate(e=>e.scrollIntoView({block:'start'}));await page.screenshot({path:path.join(output,'replay-'+width+'-'+group+'.png')});checks.push(`${width}px condition ${group}: actual costs and all controls/cases/stages, initial/middle/final native coordinates verified`);
  }
  await page.locator('#play').click();const value=await page.locator('#frame').inputValue();await page.waitForTimeout(300);assert.notEqual(await page.locator('#frame').inputValue(),value);await page.locator('#play').click();assert.equal(await page.locator('#play').textContent(),'Play');
  for(const button of await page.locator('#map button').all()){await button.click();assert.equal(await button.getAttribute('aria-pressed'),'true');assert(await page.locator('#module a').getAttribute('href'));}
  await page.fill('#filter','TaskCost084');assert((await page.locator('#files').innerText()).includes('TaskCost084.java'));await page.fill('#filter','no-such-file-084');assert.equal(await page.locator('#files a').count(),0);await page.fill('#filter','');
  assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));assert(await page.locator('#bars text').evaluateAll(xs=>xs.every(x=>x.getBoundingClientRect().height>=12)));for(const img of await page.locator('img').all()){await img.scrollIntoViewIfNeeded();await img.evaluate(i=>i.decode());assert(await img.evaluate(i=>i.naturalWidth>0));}
  checks.push(`${width}px: playback, selectors, charts, modules, file search, actual-image decode, readable chart labels and no document overflow`);await page.close();
 }
 assert.deepEqual(errors,[]);await fs.writeFile(path.join(output,'result.json'),JSON.stringify({passed:true,checks,errors,scope:'Actual offline HTML in Edge at two widths; costs and recorded motion checked through visible controls.'},null,2));console.log(JSON.stringify({passed:true,checks}));
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
