from pathlib import Path
import json,hashlib,subprocess,shutil,gzip,xml.etree.ElementTree as ET,re,datetime
r=Path.cwd();out=r/'docs/readmes/144';out.mkdir(parents=True,exist_ok=True);parent='5f36761f09b79ff170635be3e68fa511f70e2ab6'
read=lambda p:json.loads((r/p).read_text(encoding='utf-8-sig'));sha=lambda b:hashlib.sha256(b).hexdigest()
def text(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(s.encode('utf8'))
def write(p,v):text(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-144']).returncode==1
study=read('build/ambiguity144-app/result.json');assert study['passed'];write(out/'app-study.json',study)
lock=read('docs/OBSERVATION_144_LOCK.json');sources=read('docs/publication144/source144.json')
for n,h in lock['sourceHashes'].items():assert sha((r/n).read_bytes().replace(b'\r\n',b'\n'))==h,n
checks=set()
for p in (r/'build/validation144/final').glob('*.xml'):
 e=ET.parse(p).getroot();assert int(e.get('failures','0'))+int(e.get('errors','0'))==0,p
 for c in e.findall('testcase'):
  if c.find('skipped') is None:checks.add(c.get('classname')+'.'+c.get('name'))
assert checks
desktop=(r/'build/desktop144-final.log').read_text(encoding='utf8');assert 'fail 0' in desktop;desktop_count=int(re.search(r'(?m)^. tests (\d+)',desktop)[1])
for n in ['kernel144-final','java144-final','desktop144-final','agency144-regression','launcher144','package144','receipt144']:assert read('build/'+n+'.json')['exit']==0,n
for name in study['captures']:shutil.copy2(r/'build/ambiguity144-app'/name,out/name)
review=read('build/ambiguity144-review/result.json');assert review['passed'];write(out/'screenshot-review.json',review)
restart=read('build/ambiguity144-restart/result.json');assert restart['passed'];write(out/'restart-check.json',restart)
assert restart['harnessSha256']==sha((r/'docs/publication144/restart144.cjs').read_bytes())
diagnostics=read('build/diagnostics144.json');assert diagnostics['passed'];write(out/'diagnostics.json',diagnostics)
for item in review['captures']:shutil.copy2(r/'build/ambiguity144-review'/item['name'],out/item['name'])
rows=[{'case':x['caseName'],'domain':x['domain'],'policy':x['policy'],'summary':x['audit']['summary'],'evidence':x['audit'].get('ambiguity144') or x['audit'].get('references143'),'trackingMs':x['trackingMs'],'auditMs':x['auditMs']} for x in study['runs'] if x.get('role')=='evaluation']
def entry(name,policy='ambiguity-guard-144'):
 return next(x for x in rows if x['case']==name and x['policy']==policy)
candidate=entry('copy-only');previous=entry('copy-only','reference-confirmation-143')
clean=[entry(n) for n in ['clean-partial','clean-full']];oldclean=[entry(n,'reference-confirmation-143') for n in ['clean-partial','clean-full']]
cost=entry('legitimate-after-rival')['evidence']['blocked'];limit=entry('unseen-copy')['summary'];camera=entry('faceocc1-25')['summary']
finding=f"Known-copy control: previous143 accepted {previous['summary']['unsupported']} measurements while the target was hidden; guard144 accepted {candidate['summary']['unsupported']}. The guard blocked {candidate['evidence']['blocked']['incorrect']} independently contradicted parent proposals and {candidate['evidence']['blocked']['correct']} correct proposals on that case."
finding+=f"\n\nClean partial/full returns: guard144 {sum(x['summary']['correct'] for x in clean)} correct versus143 {sum(x['summary']['correct'] for x in oldclean)}. Legitimate return after earlier competition: {cost['correct']} correct parent proposals withheld, {cost['incorrect']} incorrect proposals withheld. This conservative cost is part of the result."
reset=entry('baseline-reset');resetTrace=next(x for x in diagnostics['rows'] if x['case']=='baseline-reset')
finding+=f"\n\nEstablished baseline return cleared ambiguity at samples {resetTrace['clearedByBaselineOrdinals']}; later accepted supplemental returns at {resetTrace['acceptedSupplementalOrdinals']}. This sequence retained {reset['summary']['correct']}/{reset['summary']['visible']} correct observations, with {reset['summary']['overlapFailures']} wrong overlaps and {reset['summary']['unsupported']} hidden outputs."
finding+=f"\n\nIdentical-pixel limit: {limit['unsupported']} measurements while the target was hidden in unseen-copy, with {limit['confirmedDistractorMatches']} labelled-copy matches. These frames are exactly the clean-full pixels with a different declared identity; this is an observability limit, not an independent statistical trial. No claim of universal copy rejection."
finding+=f"\n\nReused FaceOcc1 regression: {camera['correct']}/{camera['visible']} correct, {camera['missing']} missing, {camera['overlapFailures']} wrong overlaps, {camera['unsupported']} hidden outputs. This source was exposed in143; it is not fresh camera qualification. Defaults remain unchanged and no method is promoted."
comparison={'rows':rows,'lock':lock,'sources':sources,'defaultsUnchanged':True,'promoted':False,'refusals':study.get('refusals',[]),'finding':finding,'identityPairIsIndependent':False,'diagnostics':diagnostics}
write(out/'comparison.json',comparison)
validation=f"{len(checks)} distinct Java checks, {desktop_count} desktop checks, kernel integration, agency regression, offline preparation and package/build verification passed. Actual app: {len(study['runs'])} comparison/donor runs plus one separately reported active-ambiguity interruption check; original source/outcome verification, replay, monitor equality, baseline preservation, stopped-prefix resume, abrupt restart, complete export and tamper refusal."
v={'passed':True,'javaChecks':sorted(checks),'desktopChecks':desktop_count,'actualAppChecks':list(dict.fromkeys(study['checks']+restart['checks'])),'production':study['source'],'buildReceipt':study['buildReceipt'],'candidateLock':lock,'defaultsUnchanged':True,'scope':validation+' Generated geometry and exposed camera regression remain separate. Hosted CI is a separate result.'};write(out/'verification.json',v)
originals=[];seen=set()
def preserve(p,logical):
 if not p.is_file() or logical in seen:return
 seen.add(logical);raw=p.read_bytes();key=sha(raw);dest=out/'originals'/(key+'.gz');packed=gzip.compress(raw,mtime=0);dest.parent.mkdir(exist_ok=True)
 if not dest.exists():dest.write_bytes(packed)
 originals.append({'logicalPath':logical,'path':dest.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(raw),'rawSha256':key})
for name in ['ambiguity144-app','ambiguity144-review','ambiguity144-restart','agency144-evidence','validation144','sources144']:
 for p in sorted((r/'build'/name).rglob('*')):
  if any(x in p.parts for x in ['desktop-profile','node_modules','exports']) or p.name.endswith('.fobs.json'):continue
  if p.is_file() and p.suffix in ['.json','.jsonl','.xml','.png','.txt','.log','.html','.css','.js','.cjs','.fworld','.previous','.csv','.evidence','.pending','.before','.java','.class']:preserve(p,p.relative_to(r).as_posix())
for p in sorted((r/'build').glob('*144*')):
 if p.is_file() and p.suffix not in ['.zip','.jfr']:preserve(p,p.relative_to(r).as_posix())
for name in ['docs/OBSERVATION_144_LOCK.json','docs/publication144/source144.json']:preserve(r/name,name)
write(out/'original-evidence.json',{'files':originals,'scope':'Original run/audit records, development and fresh comparisons, tests, attempts, public app screenshots and startup/restart checks. Raw footage/images and private profiles excluded; exact source hashes/annotations/provenance retained.'})
paths={x['path'] for x in read('docs/readmes/143/file-inventory.json')}
for n in subprocess.check_output(['git','diff','--name-only',parent,'HEAD'],text=True).splitlines():
 if n.startswith(('workspace-engine/','workspace-desktop/','src/','assets/','.github/')) or n in ['build.gradle','.gitattributes']:paths.add(n)
paths.update(['docs/EDITION_144_CONTRACT.md','docs/EDITION_144_RESTART_CHECK.md','docs/OBSERVATION_144_LOCK.json','docs/publication144/source144.json','docs/publication144/restart144.cjs','docs/publication144/diagnostics144.py']);inventory=[]
for n in sorted(paths):
 raw=(r/n).read_bytes()
 try:raw=raw.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':n,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
write(out/'file-inventory.json',inventory)

table='| Source / method | Correct / visible | Missing | Wrong overlaps | Hidden outputs | Other-object matches |\n| --- | ---: | ---: | ---: | ---: | ---: |\n'
recovery='| Source / method | Additional correct / incorrect / unknown | Withheld correct / incorrect / unknown | Filter accepted correct / incorrect |\n| --- | ---: | ---: | ---: |\n'
timings='| Source / method | Tracking ms | Audit/replay ms |\n| --- | ---: | ---: |\n'
for x in rows:
 s=x['summary'];d=x['evidence'];a=d['additional'];f=d['filtered'];b=d.get('blocked',dict(correct=0,incorrect=0,unknown=0))
 table+=f"| {x['domain']} / {x['case']} / {x['policy']} | {s['correct']}/{s['visible']} | {s['missing']} | {s['overlapFailures']} | {s['unsupported']}/{s['hidden']} | {s['confirmedDistractorMatches']} |\n"
 recovery+=f"| {x['case']} / {x['policy']} | {a['correct']} / {a['incorrect']} / {a['unknown']} | {b['correct']} / {b['incorrect']} / {b['unknown']} | {f['correct']} / {f['incorrect']} |\n"
 timings+=f"| {x['case']} / {x['policy']} | {x['trackingMs']:.1f} | {x['auditMs']:.1f} |\n"
elapsed=(datetime.datetime.fromisoformat(study['endedAt'].replace('Z','+00:00'))-datetime.datetime.fromisoformat(study['startedAt'].replace('Z','+00:00'))).total_seconds()
timings+=f"\nComplete app interval: {elapsed:.1f} seconds, including imports, source verification, UI reads, exports, screenshots, stop/resume, restart and tamper checks. Includes any retained failed-attempt/resume gaps. Phase timings are subsets, not additive costs. Fixed-order measurements are not an isolated speed comparison.\n"
restartSeconds=(datetime.datetime.fromisoformat(restart['endedAt'].replace('Z','+00:00'))-datetime.datetime.fromisoformat(restart['startedAt'].replace('Z','+00:00'))).total_seconds()
timings+=f"\nSeparate active-ambiguity interruption check: {restartSeconds:.1f} seconds, outside the comparison interval above.\n"
sourceTable='| Source | Role | Domain | Frames | Visible/partial | Hidden | Uncertain |\n| --- | --- | --- | ---: | ---: | ---: | ---: |\n'
for x in sources['records']:
 c=x['visibilityCounts'];sourceTable+=f"| {x['case']} | {x['role']} | {x['domain']} | {x['frames']} | {c['visible']+c['partial']} | {c['occluded']+c['out-of-view']} | {c['uncertain']} |\n"
next_steps=(r/'build/next144.md').read_text(encoding='utf8')
md=(r/'docs/publication144/README-template.md').read_text(encoding='utf8')
for key,value in {'FINDING':finding,'RESULTS':table,'RECOVERIES':recovery,'COSTS':timings,'SOURCES':sourceTable,'VALIDATION':validation,'APP_CHECKS':'\n'.join('- '+x for x in v['actualAppChecks']),'ATTEMPTS':(r/'build/attempts144.md').read_text(encoding='utf8'),'NEXT':next_steps}.items():md=md.replace('{{'+key+'}}',value)
assert '{{' not in md;md=md.rstrip()+'\n';text(r/'README-144.md',md);text(r/'README.md',md)
note='docs/obsidian/152-ambiguity-continuity.md';modules=[{'id':i,'name':n,'source':s,'note':note} for i,n,s in [('guard','Remember identity ambiguity','workspace-engine/src/main/java/fraymus/workspace/AmbiguityGuard144.java'),('confirm','Unchanged per-appearance confirmation','workspace-engine/src/main/java/fraymus/workspace/AppearanceConfirmation143.java'),('audit','Replay decisions and score withheld proposals','workspace-engine/src/main/java/fraymus/workspace/AmbiguityAudit144.java'),('reference','Original verified reference provenance','workspace-engine/src/main/java/fraymus/workspace/ReferenceMemory137.java'),('world','Durable observations','workspace-engine/src/main/java/fraymus/workspace/WorldObservationStore.java'),('panel','Actual evidence view','workspace-desktop/renderer/observation-panel.js')]]
system={'modules':modules,'screenshots':[{'path':'docs/readmes/144/'+x['name'],'title':x['caption']} for x in review['captures']]+[{'path':'docs/readmes/144/'+name,'title':name.removesuffix('.png').replace('-',' ')} for name in study['captures']],'evidence':[{'path':'docs/readmes/144/'+n} for n in ['verification.json','app-study.json','original-evidence.json','comparison.json']],'comparison':comparison,'validation':validation,'finding':finding};write(out/'system-map.json',system)
text(r/note,'# Identity ambiguity through loss - edition144\n\n[[README-144.md]]\n\n'+validation+'\n\n'+finding+'\n\n'+next_steps+'\n\nPreserve defaults and source/outcome verification. Continue [[docs/INTEGRATED_AGENT_ROADMAP.md]].\n')
write(r/'docs/obsidian/152-ambiguity-continuity.canvas',{'nodes':[{'id':x['id'],'type':'file','file':x['source'],'x':i*430,'y':0,'width':390,'height':190} for i,x in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']} for i in range(len(modules)-1)]})
html=(r/'docs/publication144/guide-template.html').read_text(encoding='utf8')+'<script id="system-data" type="application/json">'+json.dumps(system).replace('</','<\\/')+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory).replace('</','<\\/')+'</script><script src="guide.js"></script></body></html>'
text(out/'index.html',html);shutil.copy2(r/'docs/publication144/guide.js',out/'guide.js')
for name,link in [('docs/readmes/INDEX.md','[Edition144 - identity ambiguity through loss](../../README-144.md) | [Offline guide](144/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/152-ambiguity-continuity.md]] | [[README-144.md]]')]:
 p=r/name;s=p.read_text(encoding='utf-8-sig');text(p,link+'\n\n'+s if not s.startswith(link) else s)
print(json.dumps({'passed':True,'javaChecks':len(checks),'sources':len(inventory),'originalEntries':len(originals),'finding':finding}))
