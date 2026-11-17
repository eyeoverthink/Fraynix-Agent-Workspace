from pathlib import Path
import json,hashlib,gzip,shutil,xml.etree.ElementTree as E,subprocess,re
r=Path.cwd();out=r/'docs/readmes/159';out.mkdir(parents=True,exist_ok=True);sha=lambda b:hashlib.sha256(b).hexdigest()
def read(p):return json.loads((r/p).read_bytes())
def write(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes((json.dumps(v,indent=2,ensure_ascii=False)+'\n').encode())
def text(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(s.encode())
def public(v):
 if isinstance(v,dict):
  for k,x in v.items():
   if k=='image':assert isinstance(x,list) and len(x)==2 and all(isinstance(n,(int,float))for n in x),'Only planar numeric annotations are public'
   assert k not in ['base64','imageData','dataUrl'];public(x)
 elif isinstance(v,list):
  for x in v:public(x)
 elif isinstance(v,str):assert 'data:image/' not in v
path=read('build/studies159.json')['path'];study=read(path);assert study['passed'];public(study)
lock=read('docs/CALIBRATED_159_LOCK.json')
for n,h in lock['sourceHashes'].items():assert sha((r/n).read_bytes().replace(b'\r\n',b'\n'))==h,n
assert study['methodLockSha256']==sha((r/'docs/CALIBRATED_159_LOCK.json').read_bytes())
review=read('build/app-visual-review159.json');assert review['passed']
count=0;xmls=list((r/'build/validation159/final').glob('*.xml'))
for p in xmls:
 e=E.parse(p).getroot();assert sum(int(e.get(k,'0'))for k in ['failures','errors','skipped'])==0;count+=int(e.get('tests'))
assert count==read('build/validation159/summary.json')['java'];desktop=(r/'build/desktop159.log').read_text(encoding='utf8');assert 'fail 0' in desktop;desktopCount=int(re.search(r'tests (\d+)',desktop)[1])
finding='Edition159 connects an original measured plane to a durable Java request that checks coordinate allowances, executes actual bounded native motion and verifies the committed result and interactive report. Recorded sources, simulation states and unresolved physical uncertainty stay distinct. Existing defaults are unchanged.'
validation=f'{count} selected Java checks and {desktopCount} desktop checks passed, with no failures or skips. Three actual Java/Electron tasks passed source, native-action, rendered-outcome, recovery and cold-verification checks. This is scoped validation, not a rerun of every historical capability.'
rows=[];shots=[]
for t in study['tasks']:
 w=t['workflow'];a=w['outcome159']['rows'];rows.append({'case':t['name'],'markers':len(a),'maxCoordinateRadiusMm':max(x['coordinateRadiusM']for x in a)*1000,'maxActualErrorMm':max(x['planarErrorM']for x in a)*1000,'maxCombinedMm':max(x['combinedErrorBudgetM']for x in a)*1000,'elapsedMs':t['elapsedMs'],'taskId':t['id'],'workflowId':w['id'],'resumes':w['resumes'],'savedSkill':w['savedSkill'],'sourceFrameSha256':w['calibration159']['frameSha256'],'scope':'Conditional declared bounds and actual native simulation errors; not measured physical accuracy.'})
for c in study['captures']:
 p=(r/path).parent/c['name'];assert sha(p.read_bytes())==c['sha256'] and c['privatePixelsExcluded'];shutil.copy2(p,out/c['name']);shots.append({'path':'docs/readmes/159/'+c['name'],'title':'2026-11-17 · actual app / '+c['name']})
extra=read('build/review159/result.json');assert extra['passed']
for c in extra['captures']:
 p=r/c['path'];assert sha(p.read_bytes())==c['sha256'];dest='review-'+c['name'];shutil.copy2(p,out/dest);shots.append({'path':'docs/readmes/159/'+dest,'title':'Actual report inspection / '+c['name']})
assert {x['sha256']for x in review['images']}=={x['sha256']for x in study['captures']+extra['captures']}
write(out/'application-study.json',study);write(out/'comparison.json',rows);write(out/'verification.json',{'passed':True,'java':count,'desktop':desktopCount,'applicationTasks':len(rows),'checks':study['checks'],'scope':validation,'physicalAccuracyPromoted':False,'defaultTracker':'legacy-flow','modelCalls':0})
table='| Task | Measured markers | Coordinate radius mm | Actual simulation error mm | Combined mm | Task span s |\n| --- | ---: | ---: | ---: | ---: | ---: |\n'
for v in rows:table+=f"| {v['case']} | {v['markers']} | {v['maxCoordinateRadiusMm']:.3f} | {v['maxActualErrorMm']:.3f} | {v['maxCombinedMm']:.3f} | {v['elapsedMs']/1000:.2f} |\n"
attempts=read('build/attempts159.json')['description'];archive=[p.relative_to(r).as_posix()for p in (r/'build').glob('*159*.log')]+[p.relative_to(r).as_posix()for p in (r/'build').glob('study159*/result.json')]+['build/ci159-start.json','build/ci159-before-publication.json','build/attempts159.json','build/app-visual-review159.json','build/review159/result.json','build/validation159/summary.json','build/study095-build.json','docs/CALIBRATED_159_LOCK.json']+[p.relative_to(r).as_posix()for p in xmls]+[p.relative_to(r).as_posix()for p in (r/'build/attempts159').rglob('*')if p.is_file()];originals=[]
for n in dict.fromkeys(archive):
 raw=(r/n).read_bytes();public(json.loads(raw))if n.endswith('.json')else None;packed=gzip.compress(raw,mtime=0);p=out/'originals'/(sha(raw)+'.gz');p.parent.mkdir(exist_ok=True);p.write_bytes(packed);originals.append({'logicalPath':n,'path':p.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(raw),'rawSha256':sha(raw)})
write(out/'original-evidence.json',{'files':originals,'privatePixelsPublished':False})
modules=[{'id':k,'name':n,'source':f}for k,n,f in [('source','Original reference and conditional mapping','workspace-engine/src/main/java/fraymus/workspace/CalibratedPlane159.java'),('context','Bounded intention and endpoint checks','workspace-engine/src/main/java/fraymus/workspace/CalibratedAction159.java'),('workflow','Durable Java stages','workspace-engine/src/main/java/fraymus/workspace/WorkflowRunner.java'),('report','Actual report and comparator','workspace-engine/src/main/java/fraymus/workspace/CalibratedReport159.java'),('host','Independent rendered observations','workspace-desktop/calibrated159.cjs')]]
system={'modules':modules,'screenshots':shots,'comparison':rows,'finding':finding,'validation':validation};write(out/'system-map.json',system)
note='docs/obsidian/167-calibrated-actions.md';text(r/note,'# Calibrated recorded actions — edition159\n\n[[README-159.md]]\n\n'+finding+'\n\n'+validation+'\n\nNext: separate physical localization-error evaluation and smaller single-marker native control; do not silently apply historical calibration to current footage.\n')
write(r/'docs/obsidian/167-calibrated-actions.canvas',{'nodes':[{'id':m['id'],'type':'file','file':m['source'],'x':i*430,'y':0,'width':390,'height':200}for i,m in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']}for i in range(len(modules)-1)]})
md=(r/'docs/publication159/README-template.md').read_text(encoding='utf8')
for k,v in {'FINDING':finding,'HERO':'![Actual calibrated action report](docs/readmes/159/review-bounds-and-outcome.png)','TABLE':table,'VALIDATION':validation,'ATTEMPTS':attempts}.items():md=md.replace('{{'+k+'}}',v)
assert '{{'not in md;text(r/'README-159.md',md);text(r/'README.md',md)
progress='## Edition 159 — locally verified; publication pending\n\n2026-11-17. Correct Java target: eyeoverthink/Fraynix-Agent-Workspace, J:/Codex-Work/fraynix-workspace-edition159. No subagents. One heavy check at a time. Recorded sources only; no webcam active. CI checked first; see publication receipt.\n\n'+finding+'\n\n'+validation+'\n\n'+attempts+'\n\nRead README-159.md and docs/publication159/plan159.md. Preserve original private projects, all001–158 frozen documentation, legacy tracker and native-controller identities. Conditional recorded grid coordinates are not live object pose or calibrated coverage probabilities. The existing20cm/two-body pose domain requires an explicit return marker for the small mat. Next: independent localization/error-coverage evaluation and a separately tested smaller single-marker action domain.\n\n'
for n in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
 p=r/n;s=p.read_text(encoding='utf-8-sig');assert not s.startswith('## Edition 159');text(p,progress+s)
for n in ['docs/INTEGRATED_AGENT_ROADMAP.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md']:
 p=r/n;text(p,'## Edition159 continuity — 2026-11-17\n\n'+finding+'\n\n[Edition159](../README-159.md). Preserve unified Java agency, existing physics and evidence. Next: held-out calibration/coverage and single-marker control.\n\n'+p.read_text(encoding='utf-8-sig'))
for n,prefix in [('docs/readmes/INDEX.md','[Edition159 — calibrated recorded actions](../../README-159.md) | [Offline guide](159/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/167-calibrated-actions.md]] | [[README-159.md]]')]:
 p=r/n;text(p,prefix+'\n\n'+p.read_text(encoding='utf-8-sig'))
names={x['path']for x in read('docs/readmes/158/file-inventory.json')};names.update(lock['sourceHashes']);names.add('docs/CALIBRATED_159_LOCK.json');names.update(p.relative_to(r).as_posix()for p in (r/'docs/publication159').iterdir()if p.is_file());inventory=[]
for n in sorted(names):
 raw=(r/n).read_bytes()
 try:raw=raw.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':n,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
write(out/'file-inventory.json',inventory);html=(r/'docs/publication159/guide-template.html').read_text(encoding='utf8')+'<script id="system-data" type="application/json">'+json.dumps(system).replace('</','<\\/')+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory).replace('</','<\\/')+'</script><script src="guide.js"></script></body></html>';text(out/'index.html',html);shutil.copy2(r/'docs/publication159/guide.js',out/'guide.js');print(json.dumps({'sources':len(inventory),'evidence':len(originals),'screenshots':len(shots),'java':count,'desktop':desktopCount}))
