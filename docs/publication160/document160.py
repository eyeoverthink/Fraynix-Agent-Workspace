from pathlib import Path
import json,hashlib,gzip,shutil,xml.etree.ElementTree as E,subprocess,re
r=Path.cwd();out=r/'docs/readmes/160';out.mkdir(parents=True,exist_ok=True);sha=lambda b:hashlib.sha256(b).hexdigest()
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
path=read('build/studies160.json')['path'];study=read(path);assert study['passed'];public(study)
lock=read('docs/PLANE_ACCURACY_160_LOCK.json')
for n,h in lock['sourceHashes'].items():assert sha((r/n).read_bytes().replace(b'\r\n',b'\n'))==h,n
assert study['methodLockSha256']==sha((r/'docs/PLANE_ACCURACY_160_LOCK.json').read_bytes())
review=read('build/app-visual-review160.json');assert review['passed']
count=0;xmls=list((r/'build/validation160/final-b').glob('*.xml'))
for p in xmls:
 e=E.parse(p).getroot();assert sum(int(e.get(k,'0'))for k in ['failures','errors','skipped'])==0;count+=int(e.get('tests'))
assert count==read('build/validation160/summary.json')['java'];desktop=(r/'build/desktop160-c.log').read_text(encoding='utf8');assert 'fail 0' in desktop;desktopCount=int(re.search(r'tests (\d+)',desktop)[1])
finding='Edition160 adds independent recorded-motion evaluation to the Java workspace. It freshly reconstructs every original frame, compares measured displacement with separately labelled tape endpoints, carries conditional error bounds, and independently checks a working rendered report. It preserves tracker and action defaults.'
validation=f'{count} selected Java checks and {desktopCount} desktop checks passed, with no failures or skips. Three actual Java/Electron evaluation jobs passed source, recovery, rendered-outcome and cold-verification checks. Measurement qualification is reported separately. This is scoped validation, not a rerun of every historical capability.'
rows=[];shots=[]
for t in study['jobs']:
 j=t['job'];a=j['results'][0];rows.append({'case':t['name'],'independentEpisodes':1,'usablePairs':a['usablePairs'],'referenceRegionCovered':a['referenceRegionCovered'],'pairedSamples':8,'maxErrorMm':None if a['maxErrorM'] is None else a['maxErrorM']*1000,'maxRadiusMm':None if a['maxRadiusM'] is None else a['maxRadiusM']*1000,'measurementGatesPassed':a['measurementGatesPassed'],'elapsedMs':t['elapsedMs'],'reconstructionMs':j['elapsedMs'],'jobId':t['id'],'trialId':t['trialId'],'resumes':j['resumes'],'scope':'Exposed historical camera episode or labelled generated episode. Conditional displacement bounds; not coverage probabilities or physical-action authority.'})
for c in study['captures']:
 p=(r/path).parent/c['name'];assert sha(p.read_bytes())==c['sha256'] and c['privatePixelsExcluded'];shutil.copy2(p,out/c['name']);shots.append({'path':'docs/readmes/160/'+c['name'],'title':'2026-11-17 · actual app / '+c['name']})
assert {x['sha256']for x in review['images']}=={x['sha256']for x in study['captures']}
write(out/'application-study.json',study);write(out/'comparison.json',rows);write(out/'verification.json',{'passed':True,'java':count,'desktop':desktopCount,'applicationJobs':len(rows),'checks':study['checks'],'scope':validation,'physicalAccuracyPromoted':False,'defaultTracker':'legacy-flow','modelCalls':0})
table='| Case | Usable / fixed pairs | Reference regions contained | Max error mm | Max radius mm | Measurement gates | Job span s |\n| --- | ---: | ---: | ---: | ---: | --- | ---: |\n'
mm=lambda x:'unknown' if x is None else f'{x:.3f}'
for v in rows:table+=f"| {v['case']} | {v['usablePairs']}/8 | {v['referenceRegionCovered']}/8 | {mm(v['maxErrorMm'])} | {mm(v['maxRadiusMm'])} | {'passed' if v['measurementGatesPassed'] else 'failed'} | {v['elapsedMs']/1000:.2f} |\n"
attempts=read('build/attempts160.json')['description'];archive=[p.relative_to(r).as_posix()for p in (r/'build').glob('*160*.log')]+[p.relative_to(r).as_posix()for p in (r/'build').glob('study160*/result.json')]+['build/ci160-start.json','build/ci160-before-publication.json','build/attempts160.json','build/app-visual-review160.json','build/validation160/summary.json','build/study095-build.json','docs/PLANE_ACCURACY_160_LOCK.json']+[p.relative_to(r).as_posix()for p in (r/'build/validation160').rglob('*.xml')]+[p.relative_to(r).as_posix()for p in (r/'build/attempts160').rglob('*')if p.is_file()];originals=[]
for n in dict.fromkeys(archive):
 raw=(r/n).read_bytes();public(json.loads(raw))if n.endswith('.json')else None;packed=gzip.compress(raw,mtime=0);p=out/'originals'/(sha(raw)+'.gz');p.parent.mkdir(exist_ok=True);p.write_bytes(packed);originals.append({'logicalPath':n,'path':p.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(raw),'rawSha256':sha(raw)})
write(out/'original-evidence.json',{'files':originals,'privatePixelsPublished':False})
modules=[{'id':k,'name':n,'source':f}for k,n,f in [('source','Original sequence reconstruction','workspace-engine/src/main/java/fraymus/workspace/PlaneReplay160.java'),('context','Independent displacement and conditional bounds','workspace-engine/src/main/java/fraymus/workspace/PlaneAccuracy160.java'),('workflow','Durable evaluation jobs','workspace-engine/src/main/java/fraymus/workspace/PlaneAccuracyStore160.java'),('report','Actual report and comparator','workspace-engine/src/main/java/fraymus/workspace/PlaneAccuracyReport160.java'),('host','Independent rendered observations','workspace-desktop/accuracy160.cjs')]]
system={'modules':modules,'screenshots':shots,'comparison':rows,'finding':finding,'validation':validation};write(out/'system-map.json',system)
note='docs/obsidian/168-independent-motion-checks.md';text(r/note,'# Independent motion checks — edition160\n\n[[README-160.md]]\n\n'+finding+'\n\n'+validation+'\n\nNext: separately labelled physical episodes, calibration-change recovery and smaller single-marker native control. Do not transfer historical measurements into live-pose authority.\n')
write(r/'docs/obsidian/168-independent-motion-checks.canvas',{'nodes':[{'id':m['id'],'type':'file','file':m['source'],'x':i*430,'y':0,'width':390,'height':200}for i,m in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']}for i in range(len(modules)-1)]})
md=(r/'docs/publication160/README-template.md').read_text(encoding='utf8')
for k,v in {'FINDING':finding,'HERO':'![Actual recorded displacement report](docs/readmes/160/camera-error-and-bounds.png)','TABLE':table,'VALIDATION':validation,'ATTEMPTS':attempts}.items():md=md.replace('{{'+k+'}}',v)
assert '{{'not in md;text(r/'README-160.md',md);text(r/'README.md',md)
progress='## Edition 160 — locally verified; publication pending\n\n2026-11-17. Correct Java target: eyeoverthink/Fraynix-Agent-Workspace, J:/Codex-Work/fraynix-workspace-edition160. No subagents. One heavy check at a time. Recorded sources only; no webcam active. CI checked first; see publication receipt.\n\n'+finding+'\n\n'+validation+'\n\n'+attempts+'\n\nRead README-160.md and docs/publication160/plan160.md. Preserve original private projects, all001–159 frozen documentation, legacy tracker and native-controller identities. Conditional recorded grid coordinates are not live object pose or calibrated coverage probabilities. The completed evaluator reports measurement failure honestly. Eight pairs remain one episode; all-missing metrics are unknown. Next: held-out physical displacement episodes and a separately tested smaller single-marker action domain. No new chat route or model training in160.\n\n'
for n in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
 p=r/n;s=p.read_text(encoding='utf-8-sig');assert not s.startswith('## Edition 160');text(p,progress+s)
for n in ['docs/INTEGRATED_AGENT_ROADMAP.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md']:
 p=r/n;text(p,'## Edition160 continuity — 2026-11-17\n\n'+finding+'\n\n[Edition160](../README-160.md). Preserve unified Java agency, existing physics and evidence. Next: held-out physical displacement/coverage and single-marker control.\n\n'+p.read_text(encoding='utf-8-sig'))
for n,prefix in [('docs/readmes/INDEX.md','[Edition160 — independent recorded motion checks](../../README-160.md) | [Offline guide](160/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/168-independent-motion-checks.md]] | [[README-160.md]]')]:
 p=r/n;text(p,prefix+'\n\n'+p.read_text(encoding='utf-8-sig'))
names={x['path']for x in read('docs/readmes/159/file-inventory.json')};names.update(lock['sourceHashes']);names.add('docs/PLANE_ACCURACY_160_LOCK.json');names.update(p.relative_to(r).as_posix()for p in (r/'docs/publication160').iterdir()if p.is_file());inventory=[]
for n in sorted(names):
 raw=(r/n).read_bytes()
 try:raw=raw.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':n,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
write(out/'file-inventory.json',inventory);html=(r/'docs/publication160/guide-template.html').read_text(encoding='utf8')+'<script id="system-data" type="application/json">'+json.dumps(system).replace('</','<\\/')+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory).replace('</','<\\/')+'</script><script src="guide.js"></script></body></html>';text(out/'index.html',html);shutil.copy2(r/'docs/publication160/guide.js',out/'guide.js');print(json.dumps({'sources':len(inventory),'evidence':len(originals),'screenshots':len(shots),'java':count,'desktop':desktopCount}))
