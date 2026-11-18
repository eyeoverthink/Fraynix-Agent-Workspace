from pathlib import Path
import json,hashlib,gzip,shutil,xml.etree.ElementTree as E,subprocess,re
r=Path.cwd();out=r/'docs/readmes/163';out.mkdir(parents=True,exist_ok=True);sha=lambda b:hashlib.sha256(b).hexdigest()
def read(p):return json.loads((r/p).read_bytes())
def write(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,indent=2,ensure_ascii=False)+'\n',encoding='utf8')
def text(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8')
def public(v):
 if isinstance(v,dict):
  for k,x in v.items():
   if k=='image':assert isinstance(x,list)and len(x)==2 and all(isinstance(n,(int,float))for n in x),'Only numeric plane annotations may be published'
   assert k not in ['base64','imageData','dataUrl'];public(x)
 elif isinstance(v,list):
  for x in v:public(x)
 elif isinstance(v,str):assert 'data:image/'not in v
lock=read('docs/RECORDED_REPLAY_163_LOCK.json');comp=read('build/comparison163.json');assert comp['passed'];names=comp['runs'];studies={n:read('build/study163-'+n+'/result.json')for n in names}
for n,h in lock['sourceHashes'].items():assert sha((r/n).read_bytes().replace(b'\r\n',b'\n'))==h,n
for n,s in studies.items():assert s['passed']and s['methodLockSha256']==sha((r/'docs/RECORDED_REPLAY_163_LOCK.json').read_bytes());public(s);write(out/(n+'-study.json'),s)
study=studies['memo-b'];review=read('build/app-visual-review163.json');assert review['passed'];count=0
for p in (r/'build/validation163/final').glob('*.xml'):
 e=E.parse(p).getroot();assert sum(int(e.get(k,'0'))for k in ['failures','errors','skipped'])==0;count+=int(e.get('tests'))
assert count==read('build/validation163/summary.json')['java'];desktop=(r/'build/desktop163-final.log').read_text(encoding='utf8');assert 'fail 0'in desktop;desktopCount=int(re.search(r'tests (\d+)',desktop)[1]);assert read('build/render163.json')['passed']
benefit=all(x['savedMs']>0 for x in comp['pairs'])
finding=f"Edition163 measures complete recorded-object workflows over three existing recordings and four accepted world revisions. Exact, process-local recording reuse {'reduced'if benefit else 'did not consistently reduce'} measured task cost across the two comparison pairs; source, numerical native trace and actual report evidence match exactly. Mean paired change: {comp['meanSavedPercent']:.1f}% less task time. This is a small exposed local study, not a general speed guarantee."
validation=f'{count} selected Java checks and {desktopCount} desktop checks passed without failures or skips. Three halfway-time labels and exact poses passed actual Chromium checks. Four final Java/Electron sessions completed sixteen durable chat workflows, four accepted revisions per session, retained skills, forced original reconstruction, abrupt restart, source-corruption refusal and cold verification. One camera episode and two generated recordings remain three distinct source sequences; repetition does not add physical evidence. New physical validation remains pending.'
rows=comp['costRows'];pairTable='| Pair | Full replay, four tasks s | Verified reuse, four tasks s | Saved % |\n| --- | ---: | ---: | ---: |\n'
for x in comp['pairs']:pairTable+=f"| {x['pair']} | {x['fullMs']/1000:.2f} | {x['memoMs']/1000:.2f} | {x['savedPercent']:.1f} |\n"
table='| Run | Source/task | Revision | Preparation s | Execution s | Archive check s | Complete task s |\n| --- | --- | ---: | ---: | ---: | ---: | ---: |\n'
for x in rows:table+=f"| {x['run']} | {x['case']} | {x['revision']} | {x['prepareMs']/1000:.2f} | {x['executeMs']/1000:.2f} | {x['archiveMs']/1000:.2f} | {x['totalMs']/1000:.2f} |\n"
shots=[]
for c in study['captures']:
 p=r/'build/study163-memo-b'/c['name'];assert sha(p.read_bytes())==c['sha256']and c['privatePixelsExcluded'];shutil.copy2(p,out/c['name']);shots.append({'path':'docs/readmes/163/'+c['name'],'title':'2026-11-17 · actual app / '+c['name']})
assert {x['sha256']for x in review['images']}=={x['sha256']for x in study['captures']}
write(out/'comparison.json',rows);write(out/'comparison-evidence.json',comp);write(out/'verification.json',{'passed':True,'java':count,'desktop':desktopCount,'completedChatWorkflows':16,'sessions':4,'historyDepth':4,'checks':study['checks'],'exactComparison':True,'scope':validation,'physicalAccuracyPromoted':False,'modelCalls':0,'defaultTracker':'legacy-flow','originalSourceFilesUnchanged':study['originalFilesUnchanged']})
attempts=read('build/attempts163.json')['description']
archive=[p.relative_to(r).as_posix()for p in (r/'build').glob('*163*.log')]+[p.relative_to(r).as_posix()for p in (r/'build').glob('study163*/result.json')]+['build/ci163-start.json','build/ci163-before-publication.json','build/attempts163.json','build/app-visual-review163.json','build/validation163/summary.json','build/study095-build.json','build/comparison163.json','build/rounding-diagnosis163.json','build/render163.json','docs/RECORDED_REPLAY_163_LOCK.json']+[p.relative_to(r).as_posix()for p in (r/'build/validation163').rglob('*.xml')]+[p.relative_to(r).as_posix()for p in (r/'build/attempts163').rglob('*')if p.is_file()];originals=[]
for n in dict.fromkeys(archive):
 raw=(r/n).read_bytes();public(json.loads(raw))if n.endswith('.json')else None;packed=gzip.compress(raw,mtime=0);p=out/'originals'/(sha(raw)+'.gz');p.parent.mkdir(exist_ok=True);p.write_bytes(packed);originals.append({'logicalPath':n,'path':p.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(raw),'rawSha256':sha(raw)})
write(out/'original-evidence.json',{'files':originals,'privatePixelsPublished':False})
modules=[{'id':k,'name':n,'source':f}for k,n,f in [('context','Fresh source and bounded reuse','workspace-engine/src/main/java/fraymus/workspace/RecordedReplay163.java'),('workflow','Durable Java stages','workspace-engine/src/main/java/fraymus/workspace/WorkflowRunner.java'),('physics','Original numerical controller','workspace-engine/src/main/java/fraymus/workspace/SmallPhysics161.java'),('report','Canonical visible labels and exact checks','workspace-engine/src/main/java/fraymus/workspace/SmallReport161.java'),('measurement','Complete actual workflow study','docs/publication163/study163.cjs'),('comparison','Exact results and paired costs','docs/publication163/compare163.py')]]
system={'modules':modules,'screenshots':shots,'comparison':rows,'finding':finding,'validation':validation};write(out/'system-map.json',system)
text(r/'docs/obsidian/171-recorded-workflow-costs.md','# Verified workflow costs — edition163\n\n[[README-163.md]]\n\n'+finding+'\n\n'+validation+'\n\n[[docs/publication163/plan163.md]]\n\nNext: complete native-history and report costs across longer mixed sequences; new physical motion separately pending.\n')
write(r/'docs/obsidian/171-recorded-workflow-costs.canvas',{'nodes':[{'id':m['id'],'type':'file','file':m['source'],'x':i*430,'y':0,'width':390,'height':200}for i,m in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']}for i in range(len(modules)-1)]})
md=(r/'docs/publication163/README-template.md').read_text(encoding='utf8')
for k,v in {'FINDING':finding,'HERO':'![Actual recording verification costs](docs/readmes/163/verification-costs.png)','PAIR_TABLE':pairTable,'RESULT_TABLE':table,'VALIDATION':validation,'ATTEMPTS':attempts}.items():md=md.replace('{{'+k+'}}',v)
assert '{{'not in md;text(r/'README-163.md',md);text(r/'README.md',md)
progress='## Edition 163 — locally verified; publication pending\n\n2026-11-17. Java target: eyeoverthink/Fraynix-Agent-Workspace, J:/Codex-Work/fraynix-workspace-edition163. No subagents; one heavy check at a time. Existing recordings only; new physical validation remains pending. CI checked first and separately.\n\n'+finding+'\n\n'+validation+'\n\n'+attempts+'\n\nRead README-163.md and docs/publication163/plan163.md. Preserve original private projects and001–162 frozen records. Original tracker and numerical controller/defaults unchanged. SmallReport161 display labels changed, so native/report identity changes; never relabel earlier proofs. Reuse is bounded and process-local with fresh evidence before/after. Native history and actual reports still reverify. Next: remaining native-history/report cost, longer mixed sequences, tighter resource limits; physical accuracy separately pending.\n\n'
for n in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
 p=r/n;s=p.read_text(encoding='utf-8-sig');assert not s.startswith('## Edition 163');text(p,progress+s)
for n in ['docs/INTEGRATED_AGENT_ROADMAP.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md']:
 p=r/n;text(p,'## Edition163 continuity — 2026-11-17\n\n'+finding+'\n\n[Edition163](../README-163.md). Keep one integrated Java agent and original source/outcome checks. Next: measure remaining complete history and report costs; new physical accuracy separately pending.\n\n'+p.read_text(encoding='utf-8-sig'))
for n,prefix in [('docs/readmes/INDEX.md','[Edition163 — verified workflow costs](../../README-163.md) | [Offline guide](163/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/171-recorded-workflow-costs.md]] | [[README-163.md]]')]:
 p=r/n;text(p,prefix+'\n\n'+p.read_text(encoding='utf-8-sig'))
names={x['path']for x in read('docs/readmes/162/file-inventory.json')};names.update(lock['sourceHashes']);names.add('docs/RECORDED_REPLAY_163_LOCK.json');names.update(p.relative_to(r).as_posix()for p in (r/'docs/publication163').iterdir()if p.is_file());inventory=[]
for n in sorted(names):
 raw=(r/n).read_bytes()
 try:raw=raw.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':n,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
write(out/'file-inventory.json',inventory);html=(r/'docs/publication163/guide-template.html').read_text(encoding='utf8')+'<script id="system-data" type="application/json">'+json.dumps(system).replace('</','<\\/')+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory).replace('</','<\\/')+'</script><script src="guide.js"></script></body></html>';text(out/'index.html',html);shutil.copy2(r/'docs/publication163/guide.js',out/'guide.js');print(json.dumps({'sources':len(inventory),'evidence':len(originals),'screenshots':len(shots),'java':count,'desktop':desktopCount}))
