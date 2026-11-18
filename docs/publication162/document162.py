from pathlib import Path
import json,hashlib,gzip,shutil,xml.etree.ElementTree as E,subprocess,re
r=Path.cwd();out=r/'docs/readmes/162';out.mkdir(parents=True,exist_ok=True);sha=lambda b:hashlib.sha256(b).hexdigest()
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
paths=read('build/studies162.json');study=read(paths['study']);lock=read('docs/RECORDED_SMALL_162_LOCK.json')
for n,h in lock['sourceHashes'].items():assert sha((r/n).read_bytes().replace(b'\r\n',b'\n'))==h,n
assert study['passed']and study['methodLockSha256']==sha((r/'docs/RECORDED_SMALL_162_LOCK.json').read_bytes());public(study)
review=read('build/app-visual-review162.json');assert review['passed'];count=0
for p in (r/'build/validation162/final').glob('*.xml'):
 e=E.parse(p).getroot();assert sum(int(e.get(k,'0'))for k in ['failures','errors','skipped'])==0;count+=int(e.get('tests'))
assert count==read('build/validation162/summary.json')['java'];desktop=(r/'build/desktop162-final.log').read_text(encoding='utf8');assert 'fail 0'in desktop;desktopCount=int(re.search(r'tests (\d+)',desktop)[1])
finding='Edition162 connects reconstructed recorded-object coordinates and conditional uncertainty to the unchanged small-object controller, through durable Java chat requests and evidence-backed reusable procedures. Defaults remain unchanged.'
validation=f'{count} selected Java checks and {desktopCount} desktop checks passed, with no failures or skips. Two actual Java/Electron chat workflows passed source, native action, rendered-report, recovery and cold-verification checks. Both use one exposed historical camera episode at different independently labelled endpoints; they are not two independent physical trials. Fresh physical validation remains pending.'
rows=[];table='| Complete workflow | Endpoint error mm | Coordinate radius mm | Combined bound mm | Task span s |\n| --- | ---: | ---: | ---: | ---: |\n'
for t in study['tasks']:
 w=t['workflow'];m=w['outcome162'];row={'case':t['name'],'kind':'recorded-plane simulation proxy','errorMm':m['endpointErrorM']*1000,'radiusMm':m['coordinateRadiusM']*1000,'combinedMm':m['combinedBoundM']*1000,'status':w['status'],'elapsedMs':t['elapsedMs'],'workflowId':w['id'],'childId':t['child']['id']};rows.append(row);table+=f"| {row['case']} | {row['errorMm']:.3f} | {row['radiusMm']:.3f} | {row['combinedMm']:.3f} | {row['elapsedMs']/1000:.2f} |\n"
shots=[]
for c in study['captures']:
 p=(r/paths['study']).parent/c['name'];assert sha(p.read_bytes())==c['sha256']and c['privatePixelsExcluded'];shutil.copy2(p,out/c['name']);shots.append({'path':'docs/readmes/162/'+c['name'],'title':'2026-11-17 · actual app / '+c['name']})
assert {x['sha256']for x in review['images']}=={x['sha256']for x in study['captures']}
write(out/'application-study.json',study);write(out/'comparison.json',rows);write(out/'verification.json',{'passed':True,'java':count,'desktop':desktopCount,'completedChatWorkflows':2,'acceptedNativeChildren':2,'checks':study['checks'],'scope':validation,'physicalAccuracyPromoted':False,'freshPhysicalValidation':'pending by user choice','defaultTracker':'legacy-flow','modelCalls':0,'privateOriginalFilesUnchanged':study['originalFilesUnchanged']})
attempts=read('build/attempts162.json')['description'];archive=[p.relative_to(r).as_posix()for p in (r/'build').glob('*162*.log')]+[p.relative_to(r).as_posix()for pattern in ['study162*/result.json']for p in (r/'build').glob(pattern)]+['build/ci162-start.json','build/ci162-before-publication.json','build/attempts162.json','build/app-visual-review162.json','build/validation162/summary.json','build/study095-build.json','docs/RECORDED_SMALL_162_LOCK.json']+[p.relative_to(r).as_posix()for p in (r/'build/validation162').rglob('*.xml')]+[p.relative_to(r).as_posix()for p in (r/'build/attempts162').rglob('*')if p.is_file()];originals=[]
for n in dict.fromkeys(archive):
 raw=(r/n).read_bytes();public(json.loads(raw))if n.endswith('.json')else None;packed=gzip.compress(raw,mtime=0);p=out/'originals'/(sha(raw)+'.gz');p.parent.mkdir(exist_ok=True);p.write_bytes(packed);originals.append({'logicalPath':n,'path':p.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(raw),'rawSha256':sha(raw)})
write(out/'original-evidence.json',{'files':originals,'privatePixelsPublished':False})
modules=[{'id':k,'name':n,'source':f}for k,n,f in [('context','Recorded source and bounds','workspace-engine/src/main/java/fraymus/workspace/RecordedSmall162.java'),('workflow','Java durable stages','workspace-engine/src/main/java/fraymus/workspace/WorkflowRunner.java'),('physics','Existing small-object controller','workspace-engine/src/main/java/fraymus/workspace/SmallPhysics161.java'),('state','Original atomic small-world store','workspace-engine/src/main/java/fraymus/workspace/SmallStore161.java'),('report','Source-bound report verification','workspace-engine/src/main/java/fraymus/workspace/RecordedReport162.java'),('host','Actual canvas observations','workspace-desktop/workflows.cjs'),('selection','Explicit app inputs','workspace-desktop/renderer/recorded162.js')]]
system={'modules':modules,'screenshots':shots,'comparison':rows,'finding':finding,'validation':validation};write(out/'system-map.json',system)
text(r/'docs/obsidian/170-recorded-object-workflows.md','# Recorded-object workflows — edition162\n\n[[README-162.md]]\n\n'+finding+'\n\n'+validation+'\n\n[[docs/publication162/plan162.md]]\n\nNext: complete workflow cost across more sources and histories, then separately labelled new physical motion.\n')
write(r/'docs/obsidian/170-recorded-object-workflows.canvas',{'nodes':[{'id':m['id'],'type':'file','file':m['source'],'x':i*430,'y':0,'width':390,'height':200}for i,m in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']}for i in range(len(modules)-1)]})
md=(r/'docs/publication162/README-template.md').read_text(encoding='utf8')
for k,v in {'FINDING':finding,'HERO':'![Actual recorded coordinates and conditional bounds](docs/readmes/162/source-and-bounds.png)','RESULT_TABLE':table,'VALIDATION':validation,'ATTEMPTS':attempts}.items():md=md.replace('{{'+k+'}}',v)
assert '{{'not in md;text(r/'README-162.md',md);text(r/'README.md',md)
progress='## Edition 162 — locally verified; publication pending\n\n2026-11-17. Java target: eyeoverthink/Fraynix-Agent-Workspace, J:/Codex-Work/fraynix-workspace-edition162. No subagents; one heavy check at a time. User chose existing recordings and left new physical validation pending. CI checked first and separately.\n\n'+finding+'\n\n'+validation+'\n\n'+attempts+'\n\nRead README-162.md and docs/publication162/plan162.md. Preserve private originals, all001–161 frozen records and existing tracker/controller defaults. Recorded plane coordinates guide an authored simulation proxy, not measured lighter geometry or hardware. Recovery finds the same child; retained procedures require fresh bindings. No model inference or weight training claim. Next: complete workflow costs across more recorded sources and histories, with new physical accuracy separately pending.\n\n'
for n in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
 p=r/n;s=p.read_text(encoding='utf-8-sig');assert not s.startswith('## Edition 162');text(p,progress+s)
for n in ['docs/INTEGRATED_AGENT_ROADMAP.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md']:
 p=r/n;text(p,'## Edition162 continuity — 2026-11-17\n\n'+finding+'\n\n[Edition162](../README-162.md). Keep unified Java agency and source/outcome checks. Next: complete source-to-action cost measurements and separately labelled new physical evidence.\n\n'+p.read_text(encoding='utf-8-sig'))
for n,prefix in [('docs/readmes/INDEX.md','[Edition162 — recorded coordinates to durable actions](../../README-162.md) | [Offline guide](162/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/170-recorded-object-workflows.md]] | [[README-162.md]]')]:
 p=r/n;text(p,prefix+'\n\n'+p.read_text(encoding='utf-8-sig'))
names={x['path']for x in read('docs/readmes/161/file-inventory.json')};names.update(lock['sourceHashes']);names.add('docs/RECORDED_SMALL_162_LOCK.json');names.update(p.relative_to(r).as_posix()for p in (r/'docs/publication162').iterdir()if p.is_file());inventory=[]
for n in sorted(names):
 raw=(r/n).read_bytes()
 try:raw=raw.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':n,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
write(out/'file-inventory.json',inventory);html=(r/'docs/publication162/guide-template.html').read_text(encoding='utf8')+'<script id="system-data" type="application/json">'+json.dumps(system).replace('</','<\\/')+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory).replace('</','<\\/')+'</script><script src="guide.js"></script></body></html>';text(out/'index.html',html);shutil.copy2(r/'docs/publication162/guide.js',out/'guide.js');print(json.dumps({'sources':len(inventory),'evidence':len(originals),'screenshots':len(shots),'java':count,'desktop':desktopCount}))
