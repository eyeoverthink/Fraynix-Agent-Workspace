'use strict';
const {launch}=require('./calibration-integration.cjs'),fs=require('node:fs/promises'),path=require('node:path'),os=require('node:os'),assert=require('node:assert/strict'),{createHash}=require('node:crypto');
const hash=b=>createHash('sha256').update(b).digest('hex'),sleep=ms=>new Promise(r=>setTimeout(r,ms));
(async()=>{
 process.env.FRAYNIX_WORKSPACE_PERSISTENT='0';process.env.FRAYNIX_WORKSPACE_PLAN_CPU_ONLY='1';
 const out=path.resolve(process.argv[2]),primary=JSON.parse(await fs.readFile(path.join(out,'result.json'),'utf8'));assert(primary.passed);
 const project=await fs.mkdtemp(path.join(os.tmpdir(),'fraynix-audit075-stop-')),source=path.join(primary.project,'memory-075',primary.studyId),beforeBytes=await fs.readFile(path.join(source,'study.json'));
 await fs.cp(primary.project,project,{recursive:true,filter:p=>{const rel=path.relative(primary.project,p).split(path.sep);return rel[0]!=='exports'&&!(rel[0]==='memory-075'&&rel.length>1&&rel[1]!==primary.studyId);}});
 const originals=JSON.parse(await fs.readFile(path.resolve(__dirname,'../../build/app075-audit-failure/original-files.json'),'utf8'));
 const {app,page}=await launch(project),api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args});
 const result={project,sourceProject:primary.project,studyId:primary.studyId,source:require('node:child_process').execFileSync('git',['rev-parse','HEAD'],{encoding:'utf8'}).trim(),scope:'Actual Stop during proof-only audit on a closed copied project; no new native motion is claimed.'};
 try{
  await page.waitForFunction(()=>window.agency043View);await page.locator('#spatial-tab').click();await page.locator('#persistent-world-panel').evaluate(e=>e.open=true);await page.locator('#memory075').evaluate(e=>e.open=true);await page.locator('#m075-refresh').click();await page.locator('#m075-studies option[value="'+primary.studyId+'"]').waitFor({state:'attached'});await page.locator('#m075-studies').selectOption(primary.studyId);
  const before=await api('/api/memory075/read',{id:primary.studyId});assert.equal(before.status,'completed');await fs.writeFile(path.join(out,'audit-stop-before.json'),JSON.stringify(before));
  await page.locator('#m075-recheck').click();let working;for(let i=0;i<1200;i++){const j=await api('/api/memory075/read',{id:primary.studyId});if(j.status==='interrupted')throw Error(j.reason);if(j.status==='running'&&j.auditProgress>=1){working=j;break;}await sleep(250);}assert(working);result.checkedBeforeStop=working.auditProgress;
  await page.locator('#m075-stop').click();let stopped;for(let i=0;i<240;i++){const j=await api('/api/memory075/read',{id:primary.studyId});if(j.status==='cancelled'){stopped=j;break;}await sleep(250);}assert(stopped);assert(!stopped.evaluation);assert.deepEqual(stopped.rows,before.rows);
  for(const f of originals){assert.equal(hash(await fs.readFile(path.join(project,'memory-075',primary.studyId,f.path))),f.sha256);assert.equal(hash(await fs.readFile(path.join(source,f.path))),f.sha256);}assert.equal(hash(await fs.readFile(path.join(source,'study.json'))),hash(beforeBytes));
  await fs.writeFile(path.join(out,'audit-stop-after.json'),JSON.stringify(stopped));await page.locator('#m075-status').scrollIntoViewIfNeeded();await fs.writeFile(path.join(out,'audit-stopped.png'),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
  result.passed=true;result.originalFilesUnchanged=originals.length;result.nativeRowsUnchanged=true;result.qualificationRemoved=true;console.log(JSON.stringify(result));await fs.writeFile(path.resolve(__dirname,'../../build/audit-stop075.json'),JSON.stringify(result,null,2));
 }finally{await app.close();}
})().catch(e=>{console.error(e);process.exitCode=1});
