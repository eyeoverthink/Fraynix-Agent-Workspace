from pathlib import Path
import json,hashlib,gzip,shutil,xml.etree.ElementTree as E,subprocess,re
r=Path.cwd();out=r/'docs/readmes/164';out.mkdir(parents=True,exist_ok=True);sha=lambda b:hashlib.sha256(b).hexdigest()
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
lock=read('docs/WORKFLOW_COST_164_LOCK.json');comp=read('build/comparison164.json');assert comp['passed'];names=comp['runs'];studies={n:read('build/study164-'+n+'/result.json')for n in names}
for n,h in lock['sourceHashes'].items():assert sha((r/n).read_bytes().replace(b'\r\n',b'\n'))==h,n
for n,s in studies.items():assert s['passed']and s['methodLockSha256']==sha((r/'docs/WORKFLOW_COST_164_LOCK.json').read_bytes());public(s);write(out/(n+'-study.json'),s)
study=studies['on-b'];review=read('build/app-visual-review164.json');assert review['passed'];count=0
for p in (r/'build/validation164/final').glob('*.xml'):
 e=E.parse(p).getroot();assert sum(int(e.get(k,'0'))for k in ['failures','errors','skipped'])==0;count+=int(e.get('tests'))
assert count==read('build/validation164/summary.json')['java'];desktop=(r/'build/desktop164-final.log').read_text(encoding='utf8');assert 'fail 0'in desktop;desktopCount=int(re.search(r'tests (\d+)',desktop)[1]);assert read('build/render163.json')['passed']
finding="Edition 164 measures complete mixed workflows across eight accepted simulated-world revisions and two fresh data reports per session. Forty durable requests complete across four sessions, including eight abrupt interruptions, sixteen saved motion-procedure versions and cold verification. Timing is diagnostic; original native/controller/report/verifier source and compiled bytes match edition 163."
validation=f'{count} selected Java checks and {desktopCount} desktop checks passed without failures or skips. Three halfway-time labels and exact poses passed actual Chromium checks. Four final Java/Electron sessions completed forty durable workflows: 32 motions and 8 fresh data reports. Each session preserved one world through 8 accepted revisions, 4 saved motion-procedure versions, 2 abrupt restarts, changed-report refusal and cold verification. One historical camera episode, two generated recordings and two original Delfys data files are distinct inputs. No new physical measurement or model inference is claimed; new physical validation remains pending.'
rows=comp['costRows'];pairTable='| Pair | Timing on, ten tasks s | Timing off, ten tasks s | Difference % |\n| --- | ---: | ---: | ---: |\n'
for x in comp['pairs']:pairTable+=f"| {x['pair']} | {x['instrumentedMs']/1000:.2f} | {x['disabledMs']/1000:.2f} | {x['overheadPercent']:+.2f} |\n"
table='| Run | Task | History | Preparation s | Execution s | Archive check s | Complete task s |\n| --- | --- | ---: | ---: | ---: | ---: | ---: |\n'
for x in rows:table+=f"| {x['run']} | {x['case']} | {x['historyDepth']} | {x['prepareMs']/1000:.2f} | {x['executeMs']/1000:.2f} | {x['archiveMs']/1000:.2f} | {x['totalMs']/1000:.2f} |\n"
probeTable='| Run | History | Original check s | Extra native replay s | Extra saved-report audit s | Whole probe s |\n| --- | ---: | ---: | ---: | ---: | ---: |\n'
for x in comp['probeRows']:probeTable+=f"| {x['run']} | {x['revision']} | {x['beforeOriginalCheckMs']/1000:.3f} | {x['nativeReplayMs']/1000:.3f} | {x['reportSourceAuditMs']/1000:.3f} | {x['wholeProbeMs']/1000:.3f} |\n"
shots=[]
for c in study['captures']:
 p=r/'build/study164-on-b'/c['name'];assert sha(p.read_bytes())==c['sha256']and c['privatePixelsExcluded'];shutil.copy2(p,out/c['name']);shots.append({'path':'docs/readmes/164/'+c['name'],'title':'2026-11-17 · actual app / '+c['name']})
assert {x['sha256']for x in review['images']}=={x['sha256']for x in study['captures']}
write(out/'native-identity.json',read('build/native-identity164.json'));write(out/'comparison.json',rows);write(out/'comparison-evidence.json',comp);write(out/'verification.json',{'passed':True,'java':count,'desktop':desktopCount,'completedChatWorkflows':40,'sessions':4,'historyDepth':8,'checks':study['checks'],'exactComparison':True,'scope':validation,'physicalAccuracyPromoted':False,'modelCalls':0,'defaultTracker':'legacy-flow','originalSourceFilesUnchanged':study['originalFilesUnchanged']})
attempts=read('build/attempts164.json')['description']
archive=[p.relative_to(r).as_posix()for p in (r/'build').glob('*164*.log')]+[p.relative_to(r).as_posix()for p in (r/'build').glob('study164*/result.json')]+['build/ci164-start.json','build/ci164-before-publication.json','build/attempts164.json','build/findings164.md','build/app-visual-review164.json','build/validation164/summary.json','build/study095-build.json','build/comparison164.json','build/native-identity164.json','build/render163.json','docs/WORKFLOW_COST_164_LOCK.json']+[p.relative_to(r).as_posix()for p in (r/'build/validation164').rglob('*.xml')]+[p.relative_to(r).as_posix()for p in (r/'build/attempts164').rglob('*')if p.is_file()];originals=[]
for n in dict.fromkeys(archive):
 raw=(r/n).read_bytes();public(json.loads(raw))if n.endswith('.json')else None;packed=gzip.compress(raw,mtime=0);p=out/'originals'/(sha(raw)+'.gz');p.parent.mkdir(exist_ok=True);p.write_bytes(packed);originals.append({'logicalPath':n,'path':p.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(raw),'rawSha256':sha(raw)})
write(out/'original-evidence.json',{'files':originals,'privatePixelsPublished':False})
modules=[{'id':k,'name':n,'source':f}for k,n,f in [('context','Bounded Java operation timings','workspace-engine/src/main/java/fraymus/workspace/WorkflowCost164.java'),('history','Read-only native and report breakdown','workspace-engine/src/main/java/fraymus/workspace/HistoryCosts164.java'),('workflow','Durable Java stages and original checks','workspace-engine/src/main/java/fraymus/workspace/WorkflowRunner.java'),('report','Original native report and exact audit','workspace-engine/src/main/java/fraymus/workspace/SmallReport161.java'),('browser','Actual canvas operation timings','workspace-desktop/report-cost164.cjs'),('measurement','Complete mixed-workflow study','docs/publication164/study164.cjs'),('comparison','Exact outcomes and paired timing costs','docs/publication164/compare164.py')]]
system={'modules':modules,'screenshots':shots,'comparison':rows,'probes':comp['probeRows'],'finding':finding,'validation':validation};write(out/'system-map.json',system)
text(r/'docs/obsidian/172-history-report-costs.md','# Physics-history and report costs — edition164\n\n[[README-164.md]]\n\n'+finding+'\n\n'+validation+'\n\n[[docs/publication164/plan164.md]]\n\nNext: reduce the measured repeated source/history work without changing checks; new physical motion separately pending.\n')
write(r/'docs/obsidian/172-history-report-costs.canvas',{'nodes':[{'id':m['id'],'type':'file','file':m['source'],'x':i*430,'y':0,'width':390,'height':200}for i,m in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']}for i in range(len(modules)-1)]})
md=(r/'docs/publication164/README-template.md').read_text(encoding='utf8')
for k,v in {'FINDING':finding,'HERO':'![Actual workflow cost breakdown](docs/readmes/164/workflow-cost-breakdown.png)','PAIR_TABLE':pairTable,'RESULT_TABLE':table,'PROBE_TABLE':probeTable,'MEASURED':(r/'build/findings164.md').read_text(encoding='utf8'),'VALIDATION':validation,'ATTEMPTS':attempts}.items():md=md.replace('{{'+k+'}}',v)
assert '{{'not in md;text(r/'README-164.md',md);text(r/'README.md',md)
progress='## Edition 164 — locally verified; publication pending\n\n2026-11-17. Java target: eyeoverthink/Fraynix-Agent-Workspace, J:/Codex-Work/fraynix-workspace-edition164. No subagents; one heavy check at a time. Existing recordings only; new physical validation remains pending. CI checked first and separately.\n\n'+finding+'\n\n'+(r/'build/findings164.md').read_text(encoding='utf8').split('\n\n',1)[0]+'\n\n'+validation+'\n\n'+attempts+'\n\nRead README-164.md and docs/publication164/plan164.md. Preserve original private projects and001–163 frozen records. Original tracker and numerical controller/defaults unchanged. Five native/controller/report/verifier classes have unchanged compiled identities. New recorded-parent implementation identity is explicit; never relabel earlier proofs. Timings are bounded process-local diagnostics. Probes repeat original verification and saved-observation audit without world mutation or new observations. Next: reduce measured repeated source/history work without weakening any source or outcome check; physical accuracy separately pending.\n\n'
for n in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
 p=r/n;s=p.read_text(encoding='utf-8-sig');assert not s.startswith('## Edition 164');text(p,progress+s)
for n in ['docs/INTEGRATED_AGENT_ROADMAP.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md']:
 p=r/n;text(p,'## Edition 164 continuity — 2026-11-17\n\n'+finding+'\n\n[Edition 164](../README-164.md). Keep one integrated Java agent and original source/outcome checks. Next: reduce measured repeated source/history work and compare complete costs; new physical accuracy separately pending.\n\n'+p.read_text(encoding='utf-8-sig'))
for n,prefix in [('docs/readmes/INDEX.md','[Edition 164 — physics history and report costs](../../README-164.md) | [Offline guide](164/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/172-history-report-costs.md]] | [[README-164.md]]')]:
 p=r/n;text(p,prefix+'\n\n'+p.read_text(encoding='utf-8-sig'))
names={x['path']for x in read('docs/readmes/163/file-inventory.json')};names.update(lock['sourceHashes']);names.add('docs/WORKFLOW_COST_164_LOCK.json');names.update(p.relative_to(r).as_posix()for p in (r/'docs/publication164').iterdir()if p.is_file());inventory=[]
for n in sorted(names):
 raw=(r/n).read_bytes()
 try:raw=raw.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':n,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
write(out/'file-inventory.json',inventory);html=(r/'docs/publication164/guide-template.html').read_text(encoding='utf8')+'<script id="system-data" type="application/json">'+json.dumps(system).replace('</','<\\/')+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory).replace('</','<\\/')+'</script><script src="guide.js"></script></body></html>';text(out/'index.html',html);shutil.copy2(r/'docs/publication164/guide.js',out/'guide.js');print(json.dumps({'sources':len(inventory),'evidence':len(originals),'screenshots':len(shots),'java':count,'desktop':desktopCount}))
