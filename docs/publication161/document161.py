from pathlib import Path
import json,hashlib,gzip,shutil,xml.etree.ElementTree as E,subprocess,re
r=Path.cwd();out=r/'docs/readmes/161';out.mkdir(parents=True,exist_ok=True);sha=lambda b:hashlib.sha256(b).hexdigest()
def read(p):return json.loads((r/p).read_bytes())
def write(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes((json.dumps(v,indent=2,ensure_ascii=False)+'\n').encode())
def text(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(s.encode())
def public(v):
 if isinstance(v,dict):
  for k,x in v.items():
   if k=='image':assert isinstance(x,list)and len(x)==2 and all(isinstance(n,(int,float))for n in x),'Only planar numeric annotations are public'
   assert k not in ['base64','imageData','dataUrl'];public(x)
 elif isinstance(v,list):
  for x in v:public(x)
 elif isinstance(v,str):assert 'data:image/' not in v
paths=read('build/studies161.json');small=read(paths['small']);motion=read(paths['motion']);lock=read('docs/SMALL_BODY_161_LOCK.json')
for n,h in lock['sourceHashes'].items():assert sha((r/n).read_bytes().replace(b'\r\n',b'\n'))==h,n
for study in [small,motion]:assert study['passed']and study['methodLockSha256']==sha((r/'docs/SMALL_BODY_161_LOCK.json').read_bytes());public(study)
review=read('build/app-visual-review161.json');assert review['passed'];count=0
for p in (r/'build/validation161/final').glob('*.xml'):
 e=E.parse(p).getroot();assert sum(int(e.get(k,'0'))for k in ['failures','errors','skipped'])==0;count+=int(e.get('tests'))
assert count==read('build/validation161/summary.json')['java'];desktop=(r/'build/desktop161-final.log').read_text(encoding='utf8');assert 'fail 0'in desktop;desktopCount=int(re.search(r'tests (\d+)',desktop)[1])
finding='Edition161 adds a real single-object Java control path with bounded native forces, persistent accepted revisions and independent replay/report checks. Existing recorded-motion evidence is re-evaluated against separate endpoint labels. Tracker and older controller defaults remain unchanged.'
validation=f'{count} selected Java checks and {desktopCount} desktop checks passed, with no failures or skips. Actual Java/Electron studies cover three accepted native moves, a deliberately refused altered report, and three completed motion evaluations. Abrupt interruption, cold reconstruction, source corruption and rendered controls are checked. This is scoped validation, not a rerun of every historical feature. Fresh physical validation remains pending.'
rows=[];nativeTable='| Native case | Accepted outcome | Error mm | Native ticks | Job span s |\n| --- | --- | ---: | ---: | ---: |\n';motionTable='| Motion source | Usable pairs | Reference regions contained | Max error mm | Max radius mm | Measurement gates |\n| --- | ---: | ---: | ---: | ---: | --- |\n'
for t in small['jobs']:
 j=t['job'];m=j['run']['verification'];status='expected refusal; world unchanged'if t.get('expectedRefusal')else j['status'];row={'case':t['name'],'kind':'declared native simulation','errorMm':m['maxPositionErrorM']*1000,'status':status,'nativeTicks':m['nativeTicks'],'elapsedMs':t['elapsedMs'],'worldRevision':j.get('committedRevision'),'jobId':j['id']};rows.append(row);nativeTable+=f"| {row['case']} | {status} | {row['errorMm']:.3f} | {row['nativeTicks']} | {row['elapsedMs']/1000:.2f} |\n"
for t in motion['jobs']:
 a=t['job']['results'][0];row={'case':t['name'],'kind':'exposed physical recording'if t['name'].startswith('exposed')else'labelled generated footage','errorMm':a['maxErrorM']*1000 if a['maxErrorM']is not None else None,'status':'measurement gates passed'if a['measurementGatesPassed']else'measurement gates failed','radiusMm':a['maxRadiusM']*1000 if a['maxRadiusM']is not None else None,'usablePairs':a['usablePairs'],'referenceRegionCovered':a['referenceRegionCovered'],'elapsedMs':t['elapsedMs'],'jobId':t['id'],'independentEpisodes':1};rows.append(row);motionTable+=f"| {row['case']} | {row['usablePairs']}/8 | {row['referenceRegionCovered']}/8 | {row['errorMm']:.3f} | {row['radiusMm']:.3f} | {row['status']} |\n"
shots=[]
for key,study in [('small',small),('motion',motion)]:
 for c in study['captures']:
  p=(r/paths[key]).parent/c['name'];assert sha(p.read_bytes())==c['sha256']and c['privatePixelsExcluded'];name=key+'-'+c['name'];shutil.copy2(p,out/name);shots.append({'path':'docs/readmes/161/'+name,'title':'2026-11-17 · actual app / '+key+' / '+c['name']})
assert {x['sha256']for x in review['images']}=={x['sha256']for s in [small,motion]for x in s['captures']}
write(out/'application-study.json',small);write(out/'motion-study.json',motion);write(out/'comparison.json',rows);write(out/'verification.json',{'passed':True,'java':count,'desktop':desktopCount,'nativeAccepted':3,'nativeExpectedRefusals':1,'motionEvaluations':3,'checks':small['checks']+motion['checks'],'scope':validation,'physicalAccuracyPromoted':False,'freshPhysicalValidation':'pending by user choice','defaultTracker':'legacy-flow','modelCalls':0,'privateOriginalFilesUnchanged':motion['originalFilesUnchanged']})
attempts=read('build/attempts161.json')['description'];archive=[p.relative_to(r).as_posix()for p in (r/'build').glob('*161*.log')]+[p.relative_to(r).as_posix()for pattern in ['small-study161*/result.json','motion-study161*/result.json']for p in (r/'build').glob(pattern)]+['build/ci161-start.json','build/ci161-before-publication.json','build/attempts161.json','build/app-visual-review161.json','build/validation161/summary.json','build/study095-build.json','docs/SMALL_BODY_161_LOCK.json']+[p.relative_to(r).as_posix()for p in (r/'build/validation161').rglob('*.xml')]+[p.relative_to(r).as_posix()for p in (r/'build/attempts161').rglob('*')if p.is_file()];originals=[]
for n in dict.fromkeys(archive):
 raw=(r/n).read_bytes();public(json.loads(raw))if n.endswith('.json')else None;packed=gzip.compress(raw,mtime=0);p=out/'originals'/(sha(raw)+'.gz');p.parent.mkdir(exist_ok=True);p.write_bytes(packed);originals.append({'logicalPath':n,'path':p.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(raw),'rawSha256':sha(raw)})
write(out/'original-evidence.json',{'files':originals,'privatePixelsPublished':False})
modules=[{'id':k,'name':n,'source':f}for k,n,f in [('context','Small-body domain and route','workspace-engine/src/main/java/fraymus/workspace/SmallBody161.java'),('physics','Bounded native forces','workspace-engine/src/main/java/fraymus/workspace/SmallPhysics161.java'),('verify','Full tick and contact verification','workspace-engine/src/main/java/fraymus/workspace/SmallVerify161.java'),('state','Durable world and attempt state','workspace-engine/src/main/java/fraymus/workspace/SmallStore161.java'),('report','Actual report comparator','workspace-engine/src/main/java/fraymus/workspace/SmallReport161.java'),('host','Independent rendered observations','workspace-desktop/small161.cjs'),('measurement','Separately labelled recorded motion','workspace-engine/src/main/java/fraymus/workspace/PlaneAccuracy160.java')]]
system={'modules':modules,'screenshots':shots,'comparison':rows,'finding':finding,'validation':validation};write(out/'system-map.json',system)
text(r/'docs/obsidian/169-small-object-motion.md','# Small-object motion — edition161\n\n[[README-161.md]]\n\n'+finding+'\n\n'+validation+'\n\n[[docs/publication161/blender161.md]]\n\nNext: independent physical episodes; qualified small-object observation binding; then durable chat/skill integration. Blender remains optional authoring/rendering.\n')
write(r/'docs/obsidian/169-small-object-motion.canvas',{'nodes':[{'id':m['id'],'type':'file','file':m['source'],'x':i*430,'y':0,'width':390,'height':200}for i,m in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']}for i in range(len(modules)-1)]})
md=(r/'docs/publication161/README-template.md').read_text(encoding='utf8')
for k,v in {'FINDING':finding,'HERO':'![Actual small-object replay inside the workspace](docs/readmes/161/small-small-replay.png)','NATIVE_TABLE':nativeTable,'MOTION_TABLE':motionTable,'VALIDATION':validation,'ATTEMPTS':attempts}.items():md=md.replace('{{'+k+'}}',v)
assert '{{'not in md;text(r/'README-161.md',md);text(r/'README.md',md)
progress='## Edition 161 — locally verified; publication pending\n\n2026-11-17. Java target: eyeoverthink/Fraynix-Agent-Workspace, J:/Codex-Work/fraynix-workspace-edition161. No subagents; one heavy check at a time. User chose existing recordings and left new physical validation pending. CI checked first and separately.\n\n'+finding+'\n\n'+validation+'\n\n'+attempts+'\n\nRead README-161.md and docs/publication161/plan161.md. Preserve private originals, all001–160 frozen records and existing tracker/controller defaults. Small-body motion is an explicit 5–15cm/20–200g domain, not lighter geometry or physical action. Recovery repeats provisional computation from the unchanged accepted head; it does not resume hidden native solver state. Blender review is targeted; no Blender code copied or dependency added. Next: fresh labelled physical episodes, qualified small-object source/uncertainty binding, then durable chat/skills.\n\n'
for n in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
 p=r/n;s=p.read_text(encoding='utf-8-sig');assert not s.startswith('## Edition 161');text(p,progress+s)
for n in ['docs/INTEGRATED_AGENT_ROADMAP.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md']:
 p=r/n;text(p,'## Edition161 continuity — 2026-11-17\n\n'+finding+'\n\n[Edition161](../README-161.md). Keep unified Java agency and source/outcome checks. Next: physical labels, small-object observation binding and chat/skills; optional Blender exchange remains planned.\n\n'+p.read_text(encoding='utf-8-sig'))
for n,prefix in [('docs/readmes/INDEX.md','[Edition161 — verified small-object motion](../../README-161.md) | [Offline guide](161/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/169-small-object-motion.md]] | [[README-161.md]]')]:
 p=r/n;text(p,prefix+'\n\n'+p.read_text(encoding='utf-8-sig'))
names={x['path']for x in read('docs/readmes/160/file-inventory.json')};names.update(lock['sourceHashes']);names.add('docs/SMALL_BODY_161_LOCK.json');names.update(p.relative_to(r).as_posix()for p in (r/'docs/publication161').iterdir()if p.is_file());inventory=[]
for n in sorted(names):
 raw=(r/n).read_bytes()
 try:raw=raw.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':n,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
write(out/'file-inventory.json',inventory);html=(r/'docs/publication161/guide-template.html').read_text(encoding='utf8')+'<script id="system-data" type="application/json">'+json.dumps(system).replace('</','<\\/')+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory).replace('</','<\\/')+'</script><script src="guide.js"></script></body></html>';text(out/'index.html',html);shutil.copy2(r/'docs/publication161/guide.js',out/'guide.js');print(json.dumps({'sources':len(inventory),'evidence':len(originals),'screenshots':len(shots),'java':count,'desktop':desktopCount}))
