from pathlib import Path
import json,hashlib,subprocess,shutil,gzip,xml.etree.ElementTree as ET,re,datetime
r=Path.cwd();out=r/'docs/readmes/145';out.mkdir(parents=True,exist_ok=True);parent='c6445f6e7f1e176df6acaccfa7ac89a80f4d26a9'
read=lambda p:json.loads((r/p).read_text(encoding='utf-8-sig'));sha=lambda b:hashlib.sha256(b).hexdigest()
def text(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(s.encode('utf8'))
def write(p,v):text(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-145']).returncode==1
study=read('build/trajectory145-app/result.json');assert study['passed'];write(out/'app-study.json',study)
tail=read('build/trajectory145-tail-app/result.json');assert tail['passed'];write(out/'tail-app-study.json',tail);mainCaptures=list(study['captures']);mainRunCount=len(study['runs']);study['runs']+=tail['runs'];study['checks']+=tail['checks'];study['refusals']=study.get('refusals',[])+tail.get('refusals',[])
exposed=read('build/exposed145-app/result.json');assert exposed['passed'];write(out/'exposed-app-study.json',exposed);positive=next(x for x in exposed['runs'] if x['caseName']=='exposed-return' and x['policy']=='trajectory-guard-145');assert positive['audit']['trajectory145']['restored']['correct']>0 and positive['audit']['trajectory145']['restored']['incorrect']==0;assert exposed.get('export');oldPositive=next(x for x in exposed['runs'] if x['caseName']=='exposed-return' and x['policy']=='ambiguity-guard-144');assert positive['audit']['summary']['correct']>oldPositive['audit']['summary']['correct'];study['checks']+=exposed['checks']
lock=read('docs/OBSERVATION_145_LOCK.json');sources=read('docs/publication145/source145.json')
sources['records']+=[x for x in read('docs/publication145/source145-tail.json')['records'] if x['role']=='evaluation']
for n,h in lock['sourceHashes'].items():assert sha((r/n).read_bytes().replace(b'\r\n',b'\n'))==h,n
checks=set()
for p in (r/'build/validation145/final').glob('*.xml'):
 e=ET.parse(p).getroot();assert int(e.get('failures','0'))+int(e.get('errors','0'))==0,p
 for c in e.findall('testcase'):
  if c.find('skipped') is None:checks.add(c.get('classname')+'.'+c.get('name'))
assert checks
desktop=(r/'build/desktop145-final.log').read_text(encoding='utf8');assert 'fail 0' in desktop;desktop_count=int(re.search(r'(?m)^. tests (\d+)',desktop)[1])
for n in ['kernel145-final','java145-final','desktop145-final','agency145-regression','launcher145','package145','receipt145']:assert read('build/'+n+'.json')['exit']==0,n
for name in study['captures']:shutil.copy2(r/'build/trajectory145-app'/name,out/name)
for name in tail['captures']:shutil.copy2(r/'build/trajectory145-tail-app'/name,out/('tail-'+name));study['captures'].append('tail-'+name)
for name in exposed['captures']:shutil.copy2(r/'build/exposed145-app'/name,out/('exposed-'+name));study['captures'].append('exposed-'+name)
review=read('build/trajectory145-review/result.json');assert review['passed'];write(out/'screenshot-review.json',review)
for item in review['captures']:shutil.copy2(r/'build/trajectory145-review'/item['name'],out/item['name'])
restart=read('build/exposed145-restart/result.json');assert restart['passed'];write(out/'restart-check.json',restart)
assert restart['harnessSha256']==sha((r/'docs/publication145/restart-exposed145.cjs').read_bytes())
exposedReview=read('build/exposed145-review/result.json');assert exposedReview['passed'];write(out/'exposed-screenshot-review.json',exposedReview)
for item in exposedReview['captures']:shutil.copy2(r/'build/exposed145-review'/item['name'],out/item['name']);review['captures'].append(item)
diagnostics=read('build/diagnostics145.json');assert diagnostics['passed'];write(out/'diagnostics.json',diagnostics)
rows=[{'case':x['caseName'],'domain':x['domain'],'policy':x['policy'],'summary':x['audit']['summary'],'evidence':x['audit'].get('trajectory145') or x['audit'].get('ambiguity144') or x['audit'].get('references143'),'trackingMs':x['trackingMs'],'auditMs':x['auditMs']} for x in study['runs'] if x.get('role')=='evaluation']
def totals(domain,policy):
 selected=[x for x in rows if x['domain']==domain and x['policy']==policy]
 return {key:sum(x['summary'][key] for x in selected) for key in ['correct','visible','missing','overlapFailures','unsupported','confirmedDistractorMatches']},len(selected)
def restored(domain):
 selected=[x for x in rows if x['domain']==domain and x['policy']=='trajectory-guard-145']
 return {key:sum(x['evidence'].get('restored',{}).get(key,0) for x in selected) for key in ['returned','correct','incorrect','unknown']}
camera,nCamera=totals('real-camera','trajectory-guard-145');cameraOld,nOld=totals('real-camera','ambiguity-guard-144');assert nCamera==nOld
fresh,nGenerated=totals('generated-control','trajectory-guard-145');previous,nPrevious=totals('generated-control','ambiguity-guard-144');assert nGenerated==nPrevious
finding=f"Fresh eligible camera comparisons ({nCamera} bounded chunks from two fixed source windows): guard145 {camera['correct']}/{camera['visible']} correct versus144 {cameraOld['correct']}/{cameraOld['visible']}; wrong overlaps {camera['overlapFailures']} versus {cameraOld['overlapFailures']}, hidden outputs {camera['unsupported']} versus {cameraOld['unsupported']}. Raw restorations of144 refusals: {restored('real-camera')}."
finding+=f"\n\nFresh generated controls ({nGenerated} cases): guard145 {fresh['correct']}/{fresh['visible']} correct versus144 {previous['correct']}/{previous['visible']}; wrong overlaps {fresh['overlapFailures']} versus {previous['overlapFailures']}, hidden outputs {fresh['unsupported']} versus {previous['unsupported']}, labelled-other matches {fresh['confirmedDistractorMatches']} versus {previous['confirmedDistractorMatches']}. Raw restorations: {restored('generated-control')}. Generated geometry does not establish camera reliability."
for x in rows:
 if x['policy']=='trajectory-guard-145':
  d=x['evidence'];b=d['blocked'];a=d['restored'];f=next(z['filterAcceptedRestorations'] for z in diagnostics['rows'] if z['case']==x['case']);finding+=f"\n\n{x['case']}: restored {a['correct']} correct / {a['incorrect']} incorrect / {a['unknown']} unknown raw proposals. Filter-accepted restorations: {f['correct']} correct / {f['incorrect']} incorrect / {f['unknown']} unknown; still withheld {b['correct']} correct / {b['incorrect']} incorrect / {b['unknown']} unknown parent proposals."
for d in diagnostics['rows']:
 if d['domain']!='exposed-development-control':finding+=f"\n\n{d['case']} activation: {d['activation']['latchedFrames']} frames with retained ambiguity; {d['activation']['eligibleMotionFrames']} also had a valid pre-loss target forecast; {d['activation']['qualifiedFrames']} qualified overrides."
if study.get('refusals'):finding+='\n\nUnavailable reference-dependent comparisons: '+str(len(study['refusals']))+'. Donor refusals are retained; existing139 baseline runs are shown separately and never presented as145 results.'
finding+='\n\nDefaults remain unchanged; no promotion. Motion bounds are uncalibrated image-plane assumptions. Unseen identical copies, camera movement, intersecting paths and long gaps remain limits; extra raw proposals can still be refused by the shared filter.'
exposedRows=[{'case':x['caseName'],'policy':x['policy'],'summary':x['audit']['summary'],'evidence':x['audit'].get('trajectory145') or x['audit'].get('ambiguity144'),'trackingMs':x['trackingMs'],'auditMs':x['auditMs']} for x in exposed['runs'] if x['role']=='evaluation'];development='Already-exposed development replay through the actual app (not fresh qualification):\n\n'+'\n'.join(f"- {x['case']} / {x['policy']}: {x['summary']['correct']}/{x['summary']['visible']} correct, {x['summary']['unsupported']} hidden outputs, restored {x['evidence'].get('restored',{})}." for x in exposedRows)
comparison={'rows':rows,'exposedDevelopment':exposedRows,'lock':lock,'sources':sources,'defaultsUnchanged':True,'promoted':False,'refusals':study.get('refusals',[]),'finding':finding,'developmentFinding':development,'diagnostics':diagnostics}
write(out/'comparison.json',comparison)
validation=f"{len(checks)} distinct Java checks, {desktop_count} desktop checks, kernel integration, agency regression, offline preparation and package/build verification passed. Actual app: {len(study['runs'])} fresh-comparison/donor runs plus {len(exposed['runs'])} separately reported exposed-development runs and one active-ambiguity interruption check. Original source/outcome verification, replay, monitor equality, baseline preservation, stopped-prefix resume, abrupt restart, complete smaller-project export, large-project capacity refusal and tamper refusal."
v={'passed':True,'javaChecks':sorted(checks),'desktopChecks':desktop_count,'actualAppChecks':list(dict.fromkeys(study['checks']+restart['checks'])),'production':study['source'],'buildReceipt':study['buildReceipt'],'candidateLock':lock,'defaultsUnchanged':True,'scope':validation+' Fresh real-camera and generated geometry results remain separate. Hosted CI is a separate result.'};v['capacityPartitionStudy']={'production':tail['source'],'buildReceipt':tail['buildReceipt'],'path':'docs/readmes/145/tail-app-study.json'};write(out/'verification.json',v)
originals=[];seen=set()
def preserve(p,logical):
 if not p.is_file() or logical in seen:return
 seen.add(logical);raw=p.read_bytes();key=sha(raw);dest=out/'originals'/(key+'.gz');packed=gzip.compress(raw,mtime=0);dest.parent.mkdir(exist_ok=True)
 if not dest.exists():dest.write_bytes(packed)
 originals.append({'logicalPath':logical,'path':dest.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(raw),'rawSha256':key})
for name in ['trajectory145-app','trajectory145-tail-app','trajectory145-review','exposed145-app','exposed145-review','exposed145-restart','agency145-evidence','validation145','otb145']:
 for p in sorted((r/'build'/name).rglob('*')):
  if any(x in p.parts for x in ['desktop-profile','node_modules','exports']) or p.name.endswith('.fobs.json'):continue
  if p.is_file() and p.suffix in ['.json','.jsonl','.xml','.png','.txt','.log','.html','.css','.js','.cjs','.fworld','.previous','.csv','.evidence','.pending','.before','.java','.class']:preserve(p,p.relative_to(r).as_posix())
for p in sorted((r/'build').glob('*145*')):
 if p.is_file() and p.suffix not in ['.zip','.jfr']:preserve(p,p.relative_to(r).as_posix())
for name in ['docs/OBSERVATION_145_LOCK.json','docs/publication145/source145.json','docs/publication145/initial-source145.json','docs/publication145/source145-tail.json','docs/publication145/capacity-amendment145.json']:preserve(r/name,name)
for name in ['docs/publication145/restart-exposed145.cjs','docs/publication145/source145-exposed.json','docs/publication145/export-amendment145.json','workspace-desktop/test/trajectory145-integration.cjs','workspace-desktop/test/trajectory145-resume-integration.cjs','workspace-desktop/test/trajectory145-tail-integration.cjs','workspace-desktop/test/exposed145-integration.cjs']:preserve(r/name,name)
write(out/'original-evidence.json',{'files':originals,'scope':'Original run/audit records, development and fresh comparisons, tests, attempts, public app screenshots and startup/restart checks. Raw footage/images and private profiles excluded; exact source hashes/annotations/provenance retained.'})
paths={x['path'] for x in read('docs/readmes/144/file-inventory.json')}
for n in subprocess.check_output(['git','diff','--name-only',parent,'HEAD'],text=True).splitlines():
 if n.startswith(('workspace-engine/','workspace-desktop/','src/','assets/','.github/')) or n in ['build.gradle','.gitattributes']:paths.add(n)
paths.update(['docs/EDITION_145_CONTRACT.md','docs/OBSERVATION_145_LOCK.json','docs/publication145/source145.json','docs/publication145/restart145.cjs','docs/publication145/diagnostics145.py','docs/publication145/visibility145.json','docs/publication145/source-selection145.json']);inventory=[]
paths.update(['docs/publication145/source145-tail.json','docs/publication145/capacity-amendment145.json','docs/publication145/partition145.py'])
paths.update(['docs/publication145/source145-exposed.json','docs/publication145/export-amendment145.json','docs/publication145/Exposed145.java','docs/publication145/restart-exposed145.cjs'])
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
 s=x['summary'];d=x['evidence'] or {};a=d.get('additional',dict(correct=0,incorrect=0,unknown=0));f=d.get('filtered',dict(correct=0,incorrect=0));b=d.get('blocked',dict(correct=0,incorrect=0,unknown=0))
 table+=f"| {x['domain']} / {x['case']} / {x['policy']} | {s['correct']}/{s['visible']} | {s['missing']} | {s['overlapFailures']} | {s['unsupported']}/{s['hidden']} | {s['confirmedDistractorMatches']} |\n"
 recovery+=f"| {x['case']} / {x['policy']} | {a['correct']} / {a['incorrect']} / {a['unknown']} | {b['correct']} / {b['incorrect']} / {b['unknown']} | {f['correct']} / {f['incorrect']} |\n"
 timings+=f"| {x['case']} / {x['policy']} | {x['trackingMs']:.1f} | {x['auditMs']:.1f} |\n"
elapsed=(datetime.datetime.fromisoformat(study['endedAt'].replace('Z','+00:00'))-datetime.datetime.fromisoformat(study['startedAt'].replace('Z','+00:00'))).total_seconds()
timings+=f"\nComplete app interval: {elapsed:.1f} seconds, including imports, source verification, UI reads, exports, screenshots, stop/resume, restart and tamper checks. Includes any retained failed-attempt/resume gaps. Phase timings are subsets, not additive costs. Fixed-order measurements are not an isolated speed comparison.\n"
tailSeconds=(datetime.datetime.fromisoformat(tail['endedAt'].replace('Z','+00:00'))-datetime.datetime.fromisoformat(tail['startedAt'].replace('Z','+00:00'))).total_seconds();timings+=f"\nSeparate capacity-partition app interval: {tailSeconds:.1f} seconds, including repeated donor verification and all remaining camera transitions in an isolated project. Main and tail studies use {mainRunCount} and {len(tail['runs'])} runs respectively; neither changes the24-run limit. Camera chunks restart from source geometry at their boundary; no claim of uninterrupted120-frame tracking.\n"
restartSeconds=(datetime.datetime.fromisoformat(restart['endedAt'].replace('Z','+00:00'))-datetime.datetime.fromisoformat(restart['startedAt'].replace('Z','+00:00'))).total_seconds()
timings+=f"\nSeparate active-ambiguity interruption check: {restartSeconds:.1f} seconds, outside the comparison interval above.\n"
exposedSeconds=(datetime.datetime.fromisoformat(exposed['endedAt'].replace('Z','+00:00'))-datetime.datetime.fromisoformat(exposed['startedAt'].replace('Z','+00:00'))).total_seconds();timings+=f"\nAlready-exposed development app interval: {exposedSeconds:.1f} seconds, including genuine donor verification, full export and all provenance checks. This is regression cost, excluded from fresh outcome totals.\n"
for x in exposedRows:timings+=f"\nExposed {x['case']} / {x['policy']}: tracking {x['trackingMs']:.1f} ms; audit {x['auditMs']:.1f} ms.\n"
reviewSeconds=sum((datetime.datetime.fromisoformat(x['endedAt'].replace('Z','+00:00'))-datetime.datetime.fromisoformat(x['startedAt'].replace('Z','+00:00'))).total_seconds() for x in [review,exposedReview]);timings+=f"\nSeparate read-only documentation review: {reviewSeconds:.1f} seconds to reopen and reverify selected completed evidence, capture source/trajectory views, and confirm zero saved-state writes. Excluded from fresh comparison timing.\n"
sourceTable='| Source | Role | Domain | Frames | Visible/partial | Hidden | Uncertain |\n| --- | --- | --- | ---: | ---: | ---: | ---: |\n'
for x in sources['records']:
 c=x['visibilityCounts'];sourceTable+=f"| {x['case']} | {x['role']} | {x['domain']} | {x['frames']} | {c['visible']+c['partial']} | {c['occluded']+c['out-of-view']} | {c['uncertain']} |\n"
next_steps=(r/'build/next145.md').read_text(encoding='utf8')
md=(r/'docs/publication145/README-template.md').read_text(encoding='utf8')
for key,value in {'FINDING':finding,'DEVELOPMENT':development,'RESULTS':table,'RECOVERIES':recovery,'COSTS':timings,'SOURCES':sourceTable,'VALIDATION':validation,'APP_CHECKS':'\n'.join('- '+x for x in v['actualAppChecks']),'ATTEMPTS':(r/'build/attempts145.md').read_text(encoding='utf8'),'NEXT':next_steps}.items():md=md.replace('{{'+key+'}}',value)
assert '{{' not in md;md=md.rstrip()+'\n';text(r/'README-145.md',md);text(r/'README.md',md)
note='docs/obsidian/153-trajectory-continuity.md';modules=[{'id':i,'name':n,'source':s,'note':note} for i,n,s in [('guard','Bounded target and competing trajectories','workspace-engine/src/main/java/fraymus/workspace/TrajectoryRecovery145.java'),('confirm','Unchanged per-appearance confirmation','workspace-engine/src/main/java/fraymus/workspace/AppearanceConfirmation143.java'),('audit','Replay and score trajectory restorations','workspace-engine/src/main/java/fraymus/workspace/TrajectoryAudit145.java'),('reference','Original verified reference provenance','workspace-engine/src/main/java/fraymus/workspace/ReferenceMemory137.java'),('world','Durable observations','workspace-engine/src/main/java/fraymus/workspace/WorldObservationStore.java'),('panel','Actual evidence view','workspace-desktop/renderer/observation-panel.js')]]
system={'modules':modules,'screenshots':[{'path':'docs/readmes/145/'+x['name'],'title':x['caption']} for x in review['captures']]+[{'path':'docs/readmes/145/'+name,'title':name.removesuffix('.png').replace('-',' ')} for name in study['captures']],'evidence':[{'path':'docs/readmes/145/'+n} for n in ['verification.json','app-study.json','original-evidence.json','comparison.json']],'comparison':comparison,'validation':validation,'finding':finding};write(out/'system-map.json',system)
text(r/note,'# Trajectory evidence through loss - edition145\n\n[[README-145.md]]\n\n'+validation+'\n\n'+finding+'\n\n'+next_steps+'\n\nPreserve defaults and source/outcome verification. Continue [[docs/INTEGRATED_AGENT_ROADMAP.md]].\n')
write(r/'docs/obsidian/153-trajectory-continuity.canvas',{'nodes':[{'id':x['id'],'type':'file','file':x['source'],'x':i*430,'y':0,'width':390,'height':190} for i,x in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']} for i in range(len(modules)-1)]})
html=(r/'docs/publication145/guide-template.html').read_text(encoding='utf8')+'<script id="system-data" type="application/json">'+json.dumps(system).replace('</','<\\/')+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory).replace('</','<\\/')+'</script><script src="guide.js"></script></body></html>'
text(out/'index.html',html);shutil.copy2(r/'docs/publication145/guide.js',out/'guide.js')
for name,link in [('docs/readmes/INDEX.md','[Edition145 - measured trajectory recovery](../../README-145.md) | [Offline guide](145/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/153-trajectory-continuity.md]] | [[README-145.md]]')]:
 p=r/name;s=p.read_text(encoding='utf-8-sig');text(p,link+'\n\n'+s if not s.startswith(link) else s)
print(json.dumps({'passed':True,'javaChecks':len(checks),'sources':len(inventory),'originalEntries':len(originals),'finding':finding}))
