from pathlib import Path
import json,hashlib,gzip,shutil,xml.etree.ElementTree as E,subprocess,sys,re
r=Path.cwd();out=r/'docs/readmes/158';out.mkdir(parents=True,exist_ok=True);sha=lambda b:hashlib.sha256(b).hexdigest()
sys.path.insert(0,str(r/'docs/publication149'));from public_evidence149 import check_public
def read(p):return json.loads((r/p).read_bytes())
def write(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes((json.dumps(v,indent=2,ensure_ascii=False)+'\n').encode())
def text(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(s.encode())
paths=read('build/studies158.json')['paths'];studies=[read(p)for p in paths];assert len(studies)==2 and all(s['passed']for s in studies)
lock=read('docs/OBSERVATION_158_LOCK.json')
for n,h in lock['sourceHashes'].items():assert sha((r/n).read_bytes().replace(b'\r\n',b'\n'))==h,n
for s in studies:assert s['methodLockSha256']==sha((r/'docs/OBSERVATION_158_LOCK.json').read_bytes());check_public(s)
extra=read('build/review158/result.json');assert extra['passed'];review=read('build/app-visual-review158.json');assert review['passed'];assert {x['sha256']for x in review['images']}==({x['sha256']for s in studies for x in s['captures']}|{x['sha256']for x in extra['captures']})
count=0;xmls=list((r/'build/validation158/final').glob('*.xml'))
for p in xmls:
 e=E.parse(p).getroot();assert sum(int(e.get(k,'0'))for k in ['failures','errors','skipped'])==0;count+=int(e.get('tests'))
assert count==read('build/validation158/summary.json')['java']
raw=(r/'build/desktop158-background.log').read_bytes();desktop=raw.decode('utf16'if raw.startswith(b'\xff\xfe')else'utf8');assert 'fail 0' in desktop;desktopCount=int(re.search(r'tests (\d+)',desktop)[1])
finding='Edition 158 completes one source-bound Java workflow across recorded observation verification, world inspection, a witnessed browser reference and an independently checked interactive artifact. Recovery and saved-procedure reuse preserve the original uncertainty. Physical placement remains refused without calibrated mapping; defaults are unchanged.'
validation=f'{count} selected Java checks across eight classes and {desktopCount} desktop checks passed, with no failed or skipped cases. Three actual Java/Electron connected tasks passed, including forced Java interruption, same-task recovery, changed-page refusal, fresh-source procedure reuse, independent controls and geometry checks, lifecycle checks, cold verification and byte-corruption refusal. This is scoped local validation, not a rerun of every historical capability.'
rows=[];shots=[]
for name,s in zip(paths,studies):
 write(out/(s['group']+'-study.json'),s)
 for t in s['tasks']:
  w=t['workflow'];c=w['observation158']['context'];rows.append({'case':t['name'],'domain':s['group'],'accepted':c['accepted'],'withheld':c['withheld'],'segments':c['segments'],'physicalPlacement':w['world158']['physicalPlacement'],'elapsedMs':t['elapsedMs'],'taskId':t['id'],'workflowId':w['id'],'evidenceId':t['evidenceId'],'resumes':w['resumes'],'savedSkill':w['savedSkill'],'scope':'Descriptive task span including explicit recovery and repeated checks where exercised; not a throughput comparison.'})
 for c in s['captures']:
  p=(r/name).parent/c['name'];assert sha(p.read_bytes())==c['sha256'] and c['privatePixelsExcluded'];dest=s['group']+'-'+c['name'];shutil.copy2(p,out/dest);shots.append({'path':'docs/readmes/158/'+dest,'title':'2026-11-17 · '+s['group']+' / '+c['name']})
for c in extra['captures']:
 p=r/c['path'];assert sha(p.read_bytes())==c['sha256'];dest='review-'+c['name'];shutil.copy2(p,out/dest);shots.append({'path':'docs/readmes/158/'+dest,'title':'2026-11-17 · actual report review / '+c['name']})
write(out/'comparison.json',rows);write(out/'verification.json',{'passed':True,'java':count,'desktop':desktopCount,'applicationTasks':len(rows),'checks':[{'group':s['group'],'checks':s['checks']}for s in studies],'scope':validation,'physicalAccuracyPromoted':False,'defaultTracker':'legacy-flow','modelCalls':0})
table='| Source | Image estimates | Withheld frames | Segments | Physical placement | Task span s |\n| --- | ---: | ---: | ---: | --- | ---: |\n'
for v in rows:table+=f"| {v['case']} | {v['accepted']} | {v['withheld']} | {v['segments']} | {v['physicalPlacement']} | {v['elapsedMs']/1000:.2f} |\n"
attempts=read('build/attempts158.json')['description']
originals=[];logs=[p.relative_to(r).as_posix()for p in (r/'build').glob('*158*.log')];failed=[p.relative_to(r).as_posix()for p in (r/'build').glob('study158-*/result.json')if p.relative_to(r).as_posix()not in paths];archive=paths+failed+logs+['build/ci158-start.json','build/ci158-mid.json','build/app-visual-review158.json','build/review158/result.json','build/attempts158.json','build/validation158/summary.json','build/study095-build.json','build/study095-build-first158.json','build/study095-build-precommit158.json','build/method158-first.json','build/method158-second.json','build/method158-third.json','build/study095-build-second158.json','build/study095-build-third158.json','docs/OBSERVATION_158_LOCK.json']+[p.relative_to(r).as_posix()for p in xmls]+[p.relative_to(r).as_posix()for p in (r/'build/attempts158').rglob('*.xml')]
for name in dict.fromkeys(archive):
 raw=(r/name).read_bytes();check_public(json.loads(raw))if name.endswith('.json')else None;packed=gzip.compress(raw,mtime=0);p=out/'originals'/(sha(raw)+'.gz');p.parent.mkdir(exist_ok=True);p.write_bytes(packed);originals.append({'logicalPath':name,'path':p.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(raw),'rawSha256':sha(raw)})
write(out/'original-evidence.json',{'files':originals,'privatePixelsPublished':False})
note='docs/obsidian/166-observation-tasks.md';modules=[{'id':key,'name':name,'source':file,'note':note}for key,name,file in [('source','Original proof and fresh reconstruction','workspace-engine/src/main/java/fraymus/workspace/ObservationEvidence158.java'),('context','Uncertainty and world binding','workspace-engine/src/main/java/fraymus/workspace/ObservationTask158.java'),('workflow','Durable stages and reusable procedures','workspace-engine/src/main/java/fraymus/workspace/WorkflowRunner.java'),('report','Working artifact and independent comparator','workspace-engine/src/main/java/fraymus/workspace/ObservationReport158.java'),('host','Real browser and report observations','workspace-desktop/observation-task158.cjs')]]
system={'modules':modules,'screenshots':shots,'comparison':rows,'finding':finding,'validation':validation};write(out/'system-map.json',system)
text(r/note,'# Verified observation tasks — edition158\n\n[[README-158.md]]\n\n'+finding+'\n\n'+validation+'\n\nNext: independently calibrated coordinates and error bounds, then bounded world actions with original verification.\n')
write(r/'docs/obsidian/166-observation-tasks.canvas',{'nodes':[{'id':m['id'],'type':'file','file':m['source'],'x':i*430,'y':0,'width':390,'height':200}for i,m in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']}for i in range(len(modules)-1)]})
md=(r/'docs/publication158/README-template.md').read_text(encoding='utf8');hero=next(s for s in shots if 'generated-report-0' in s['path'])
for key,value in {'FINDING':finding,'HERO':'![Actual verified observation report]('+hero['path']+')','TABLE':table,'VALIDATION':validation,'ATTEMPTS':attempts}.items():md=md.replace('{{'+key+'}}',value)
assert '{{'not in md;text(r/'README-158.md',md);text(r/'README.md',md)
progress='## Edition 158 — locally verified; publication pending\n\n2026-11-17. Java target eyeoverthink/Fraynix-Agent-Workspace; isolated J:/Codex-Work/fraynix-workspace-edition158. No subagents. One heavy check at a time. Edition157 CI was still running at the initial check; edition156 passed. See publication receipt for latest CI status.\n\n'+finding+'\n\n'+validation+'\n\n'+attempts+'\n\nRead README-158.md and docs/publication158/plan158.md. Recorded sources only; no webcam active. Preserve original projects and every001–157 frozen record. World stage is inspection and explicit physical-action refusal, not native actuation. Whole-source changes require a newly linked full reconstruction, not a relabelled historical fingerprint. Next: calibrated mapping/error bounds and checked action use; measure complete connected costs separately.\n\n'
for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
 p=r/name;s=p.read_text(encoding='utf-8-sig');assert not s.startswith('## Edition 158');text(p,progress+s)
for name in ['docs/INTEGRATED_AGENT_ROADMAP.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md']:
 p=r/name;text(p,'## Edition158 continuity — 2026-11-17\n\n'+finding+'\n\n[Edition158](../README-158.md). Next: calibrated observation-to-world use, checked against independent physical measurements. Keep the unified Java world/browser/creation direction.\n\n'+p.read_text(encoding='utf-8-sig'))
for name,prefix in [('docs/readmes/INDEX.md','[Edition158 — verified observation tasks](../../README-158.md) | [Offline guide](158/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/166-observation-tasks.md]] | [[README-158.md]]')]:
 p=r/name;text(p,prefix+'\n\n'+p.read_text(encoding='utf-8-sig'))
names={x['path']for x in read('docs/readmes/157/file-inventory.json')};names.update(lock['sourceHashes']);names.add('docs/OBSERVATION_158_LOCK.json');names.update(p.relative_to(r).as_posix()for p in (r/'docs/publication158').iterdir()if p.is_file());inventory=[]
for name in sorted(names):
 raw=(r/name).read_bytes()
 try:raw=raw.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':name,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
write(out/'file-inventory.json',inventory);html=(r/'docs/publication158/guide-template.html').read_text(encoding='utf8')+'<script id="system-data" type="application/json">'+json.dumps(system).replace('</','<\\/')+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory).replace('</','<\\/')+'</script><script src="guide.js"></script></body></html>';text(out/'index.html',html);shutil.copy2(r/'docs/publication158/guide.js',out/'guide.js');print(json.dumps({'sources':len(inventory),'evidence':len(originals),'screenshots':len(shots),'java':count,'desktop':desktopCount}))
