from pathlib import Path
import json,hashlib,subprocess,shutil,gzip,xml.etree.ElementTree as ET,re,datetime
r=Path.cwd();out=r/'docs/readmes/146';out.mkdir(parents=True,exist_ok=True);parent='672894eab948f372cfa368ce7168dc667951c4eb'
read=lambda p:json.loads((r/p).read_text(encoding='utf-8-sig'));sha=lambda b:hashlib.sha256(b).hexdigest()
def text(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8')
def write(p,v):text(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
def elapsed(s):return (datetime.datetime.fromisoformat(s['endedAt'].replace('Z','+00:00'))-datetime.datetime.fromisoformat(s['startedAt'].replace('Z','+00:00'))).total_seconds()
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-146']).returncode==1
lock=read('docs/OBSERVATION_146_LOCK.json');sources=read('docs/publication146/source146.json');groups=sorted({x['group'] for x in sources['records']});studies={};reviews={};shots=[]
for n,h in lock['sourceHashes'].items():assert sha((r/n).read_bytes().replace(b'\r\n',b'\n'))==h,n
for group in groups:
 folder='build/return146-'+group+'-app';study=read(folder+'/result.json');assert study['passed'] and study['export'];assert len(study['runs'])<=24;studies[group]=study;write(out/(group+'-app-study.json'),study)
 for name in study['captures']:
  destination=group+'-'+name;shutil.copy2(r/folder/name,out/destination);shots.append({'path':'docs/readmes/146/'+destination,'title':group+' / original study capture / '+name})
 folder='build/return146-'+group+'-review';review=read(folder+'/result.json');assert review['passed'] and review['completedStateUnchanged'] and review['zeroObservationWrites'];reviews[group]=review;write(out/(group+'-screenshot-review.json'),review)
 for item in review['captures']:
  destination=group+'-'+item['name'];shutil.copy2(r/folder/item['name'],out/destination);shots.insert(0,{'path':'docs/readmes/146/'+destination,'title':item['caption'],'case':item['case'],'group':group,'ordinal':item['ordinal']})
restart=read('build/return146-restart/result.json');assert restart['passed'];assert restart['harnessSha256']==sha((r/'docs/publication146/restart146.cjs').read_bytes());write(out/'restart-check.json',restart)
diagnostics=read('build/diagnostics146.json');assert diagnostics['passed'];write(out/'diagnostics.json',diagnostics)
allruns=[x for s in studies.values() for x in s['runs']];checks=set()
for p in (r/'build/validation146/final').glob('*.xml'):
 e=ET.parse(p).getroot();assert int(e.get('failures','0'))+int(e.get('errors','0'))==0,p
 for c in e.findall('testcase'):
  if c.find('skipped') is None:checks.add(c.get('classname')+'.'+c.get('name'))
assert checks and any('ReturnContinuity146Test' in x for x in checks) and any('CalibrationStoreTest.validationCannotRetuneWinnerAndFailedGateCannotApply' in x for x in checks)
desktop=(r/'build/desktop146-final.log').read_text(encoding='utf8');assert 'fail 0' in desktop;desktop_count=int(re.search(r'(?m)^. tests (\d+)',desktop)[1])
for n in ['kernel146-final','java146-final','desktop146-final','agency146-regression','launcher146','package146','receipt146']:assert read('build/'+n+'.json')['exit']==0,n
rows=[{'case':x['caseName'],'group':x['group'],'domain':x['domain'],'policy':x['policy'],'summary':x['audit']['summary'],'evidence':x['audit'].get('continuity146') or x['audit'].get('trajectory145'),'trackingMs':x['trackingMs'],'auditMs':x['auditMs']} for x in allruns if x['role']=='evaluation']
oldCopy=next(x for x in rows if x['group']=='regression' and x['case']=='copy-remains' and x['policy']=='trajectory-guard-145');copy=next(x for x in rows if x['group']=='regression' and x['case']=='copy-remains' and x['policy']=='return-guard-146')
assert oldCopy['summary']['unsupported']>0 and copy['summary']['unsupported']==0 and copy['evidence']['screened']['incorrect']>0,'Actual exposed copy failure must be reproduced and prevented'
legitimate=next(x for x in rows if x['group']=='regression' and x['case']=='trajectory-return' and x['policy']=='return-guard-146');assert legitimate['evidence']['filtered']['correct']>0,'Do not pass by refusing every return'
def totals(policy):
 selected=[x for x in rows if x['domain']=='real-camera' and x['policy']==policy]
 return {k:sum(x['summary'][k] for x in selected) for k in ['correct','visible','missing','overlapFailures','unsupported','hidden','confirmedDistractorMatches']},len(selected)
camera,n=totals('return-guard-146');oldCamera,oldn=totals('trajectory-guard-145');assert n==oldn
finding=f"**Exposed regression:** the app reduces accepted measurements on the hidden copy from {oldCopy['summary']['unsupported']} to {copy['summary']['unsupported']}. It newly withholds {copy['evidence']['screened']['incorrect']} incorrect raw proposals without a competitor warning. The separate legitimate regression retains {legitimate['evidence']['filtered']['correct']} correct supplemental measurements while newly withholding {legitimate['evidence']['screened']['correct']} correct raw proposals. This is development evidence, not fresh qualification."
if n:
 finding+=f"\n\n**Fresh camera events ({n} eligible paired comparisons):**146 retains {camera['correct']}/{camera['visible']} correct measurements versus145 {oldCamera['correct']}/{oldCamera['visible']}."
else:
 finding+="\n\n**Real-camera recovery remains unqualified.** The four predeclared sources yielded zero eligible event/reference pairs. Coke has a complete disappearance and return, but its all-partial donor supplies no admissible unobscured examples;145/146 comparisons are unavailable."
 for x in rows:
  if x['domain']=='real-camera' and x['policy']=='foreground-guard-139':
   q=x['summary'];finding+=f" Its existing139 baseline produced {q['correct']}/{q['visible']} correct visible measurements, {q['missing']} missing, {q['overlapFailures']} wrong overlaps and {q['unsupported']}/{q['hidden']} hidden-frame outputs. This is a baseline-only diagnostic, not evidence for146."
finding+="\n\n**Generated cases are diagnostics only:** three evaluation images duplicate donor images in each case. The app refused every reference-dependent start. The frozen generator, refusals and baseline-only outcomes are retained; these cases do not count as fresh qualification."

refusals=[x for s in studies.values() for x in s.get('refusals',[])];finding+=f"\n\nUnavailable reference-dependent comparisons: {len(refusals)}. Defaults remain unchanged and the candidate is not promoted. Generated overlapping-source diagnostics and the identical-pixel limit are listed separately below; neither is camera qualification."
comparison={'rows':rows,'sources':sources,'lock':lock,'finding':finding,'defaultsUnchanged':True,'promoted':False,'refusals':refusals,'diagnostics':diagnostics};write(out/'comparison.json',comparison)
validation=f"{len(checks)} distinct Java tests and {desktop_count} desktop checks passed, plus kernel integration, agency regression, offline preparation and package/build verification. {len(allruns)} actual app runs across {len(studies)} isolated groups, plus one separately reported process-interruption regression. All original source, code and outcome checks remain required. Hosted CI is separate."
appchecks=list(dict.fromkeys([c for s in studies.values() for c in s['checks']]+restart['checks']))
v={'passed':True,'javaChecks':sorted(checks),'desktopChecks':desktop_count,'actualAppChecks':appchecks,'candidateLock':lock,'defaultsUnchanged':True,'scope':validation,'studies':{g:{'source':s['source'],'buildReceipt':s['buildReceipt'],'path':'docs/readmes/146/'+g+'-app-study.json'} for g,s in studies.items()}};write(out/'verification.json',v)
originals=[];seen=set()
def preserve(p,logical):
 if not p.is_file() or logical in seen:return
 seen.add(logical);raw=p.read_bytes();digest=sha(raw);dest=out/'originals'/(digest+'.gz');packed=gzip.compress(raw,mtime=0);dest.parent.mkdir(exist_ok=True)
 if not dest.exists():dest.write_bytes(packed)
 originals.append({'logicalPath':logical,'path':dest.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(raw),'rawSha256':digest})
folders=['return146-'+g+'-'+kind for g in groups for kind in ['app','review']]+['return146-restart','agency146-evidence','validation146','otb146','screens146']
for name in folders:
 for p in sorted((r/'build'/name).rglob('*')):
  if any(x in p.parts for x in ['desktop-profile','node_modules','exports']) or p.name.endswith('.fobs.json'):continue
  if p.is_file() and p.suffix in ['.json','.jsonl','.xml','.png','.txt','.log','.html','.css','.js','.cjs','.fworld','.previous','.csv','.evidence','.pending','.before','.java','.class']:preserve(p,p.relative_to(r).as_posix())
for p in sorted((r/'build').glob('*146*')):
 if p.is_file() and p.suffix not in ['.zip','.jfr']:preserve(p,p.relative_to(r).as_posix())
for p in sorted((r/'workspace-engine/build/return146-development').glob('*.json')):preserve(p,p.relative_to(r).as_posix())
for name in ['docs/publication146/generated-unavailable146.cjs','docs/publication146/AMENDMENT-source-unavailable.md','docs/OBSERVATION_146_LOCK.json','docs/publication146/source146.json','docs/publication146/visibility146.json','docs/publication146/restart146.cjs','workspace-desktop/test/return146-integration.cjs']:preserve(r/name,name)
write(out/'original-evidence.json',{'files':originals,'scope':'Original runs, audits, tests, attempts, public source/app review, interrupted state and receipts. Raw source archives/images, private profiles and executable exports excluded. Their exact source hashes and provenance remain recorded.'})
paths={x['path'] for x in read('docs/readmes/145/file-inventory.json')}
for n in subprocess.check_output(['git','diff','--name-only',parent,'HEAD'],text=True).splitlines():
 if n.startswith(('workspace-engine/','workspace-desktop/','src/','assets/','.github/')) or n in ['build.gradle','.gitattributes']:paths.add(n)
paths.update(['docs/publication146/generated-unavailable146.cjs','docs/publication146/all-annotations146.json','docs/publication146/AMENDMENT-source-unavailable.md','docs/publication146/.gitattributes','docs/EDITION_146_CONTRACT.md','docs/OBSERVATION_146_LOCK.json','docs/publication146/source146.json','docs/publication146/visibility146.json','docs/publication146/source-selection146.json','docs/publication146/diagnostics146.py','docs/publication146/restart146.cjs']);inventory=[]
for n in sorted(paths):
 raw=(r/n).read_bytes()
 try:raw=raw.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':n,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
write(out/'file-inventory.json',inventory)
table='| Domain / source / method | Correct / visible | Missing | Wrong overlap | Hidden outputs | Other-object matches |\n| --- | ---: | ---: | ---: | ---: | ---: |\n';recovery='| Source / method | New146 refused correct / incorrect / unknown | All refused correct / incorrect | Filtered additional correct / incorrect |\n| --- | ---: | ---: | ---: |\n';timings='| Domain / source / method | Tracking ms | Replay/audit ms |\n| --- | ---: | ---: |\n'
for x in rows:
 s=x['summary'];d=x['evidence'] or {};new=d.get('screened',dict(correct=0,incorrect=0,unknown=0));blocked=d.get('blocked',dict(correct=0,incorrect=0));filtered=d.get('filtered',dict(correct=0,incorrect=0));table+=f"| {x['domain']} / {x['case']} / {x['policy']} | {s['correct']}/{s['visible']} | {s['missing']} | {s['overlapFailures']} | {s['unsupported']}/{s['hidden']} | {s['confirmedDistractorMatches']} |\n";recovery+=f"| {x['group']} / {x['case']} / {x['policy']} | {new['correct']} / {new['incorrect']} / {new['unknown']} | {blocked['correct']} / {blocked['incorrect']} | {filtered['correct']} / {filtered['incorrect']} |\n";timings+=f"| {x['domain']} / {x['case']} / {x['policy']} | {x['trackingMs']:.1f} | {x['auditMs']:.1f} |\n"
for g,s in studies.items():timings+=f"\nComplete {g} app interval: {elapsed(s):.1f}s, {len(s['runs'])} runs, {s['export']['bytes']} export bytes. Includes imports, reference checks, UI, source replay, stop/resume, completed-state restart, export and tamper checks, plus any retained attempt gaps. Phase durations above are subsets.\n"
timings+=f"\nAdditional unfinished-run interruption: {elapsed(restart):.1f}s. Separate read-only screenshot reviews: {sum(elapsed(x) for x in reviews.values()):.1f}s.\n"
sourceTable='| Source / role | Domain | Original frames | Visible / partial | Fully hidden / out-of-view | Uncertain |\n| --- | --- | --- | ---: | ---: | ---: |\n'
for x in sources['records']:
 c=x['visibilityCounts'];sourceTable+=f"| {x['case']} / {x['role']} | {x['domain']} | {x['start']}–{x['end']} | {c['visible']} / {c['partial']} | {c['occluded']} / {c['out-of-view']} | {c['uncertain']} |\n"
sourceReview='| Source | Reviewed frames | Primary eligibility | Source-only finding |\n| --- | ---: | --- | --- |\n'
for x in sources['sourceReview']['sources']:
 reason=x.get('notes','').replace('|','/')
 if x['name']=='Coke':reason='Complete cover255–263; return264. Holding hand prevents the required unobscured donor. Supplemental baseline-only diagnostic.'
 sourceReview+=f"| {x['name']} | {x['reviewedFrames'][0]}–{x['reviewedFrames'][1]} | {'Eligible' if x['eligible'] else 'Ineligible'} | {reason} |\n"
sourceReview+='\nEvery rejected event, original annotation selection and review detail is retained in the [source-review record](docs/publication146/visibility146.json).'

development='Already-exposed regression results are listed separately in the table and original group report. The production study uses genuine135 donor retention; no descriptor is injected into the app. The same pixels and original labels are checked against their published145 hashes.'
blockedOrdinal=next(x['newlyBlockedOrdinals'][0] for x in diagnostics['rows'] if x['group']=='regression' and x['case']=='copy-remains');shots.sort(key=lambda s:0 if s.get('group')=='regression' and s.get('case')=='copy-remains' and s.get('ordinal')==blockedOrdinal and s['path'].endswith('drift.png') else 1);reviewshots=[s for s in shots if 'case' in s];hero=next(s for s in reviewshots if s['group']=='regression' and s['case']=='copy-remains' and s['ordinal']==blockedOrdinal and s['path'].endswith('drift.png'));hero='![Actual app: exposed copy regression with return continuity]('+hero['path']+')\n\nAlready-exposed development illustration; not fresh qualification. See all outcomes below.'
cameraShots='\n\n'.join('!['+s['title']+']('+s['path']+')' for s in reviewshots if s['group'] not in ['regression','generated'] and s['path'].endswith('source.png'))
nextSteps=(r/'build/next146.md').read_text(encoding='utf8');md=(r/'docs/publication146/README-template.md').read_text(encoding='utf8')
for key,value in {'FINDING':finding,'HERO':hero,'RESULTS':table,'RECOVERIES':recovery,'DEVELOPMENT':development,'SOURCE_REVIEW':sourceReview,'SOURCES':sourceTable,'CAMERA_SHOTS':cameraShots,'VALIDATION':validation,'APP_CHECKS':'\n'.join('- '+x for x in appchecks),'ATTEMPTS':(r/'build/attempts146.md').read_text(encoding='utf8'),'COSTS':timings,'NEXT':nextSteps}.items():md=md.replace('{{'+key+'}}',value)
assert '{{' not in md;md=md.rstrip()+'\n';text(r/'README-146.md',md);text(r/'README.md',md)
note='docs/obsidian/154-return-continuity.md';modules=[{'id':i,'name':n,'source':s,'note':note} for i,n,s in [('guard','Return continuity without a warning','workspace-engine/src/main/java/fraymus/workspace/ReturnContinuity146.java'),('trajectory','Unchanged pre-loss motion bounds','workspace-engine/src/main/java/fraymus/workspace/TrajectoryGate145.java'),('audit','Replay and score added refusals','workspace-engine/src/main/java/fraymus/workspace/ReturnAudit146.java'),('reference','Verified donor provenance','workspace-engine/src/main/java/fraymus/workspace/ReferenceMemory137.java'),('world','Durable observations','workspace-engine/src/main/java/fraymus/workspace/WorldObservationStore.java'),('panel','Actual source and decision display','workspace-desktop/renderer/observation-panel.js')]]
system={'modules':modules,'screenshots':shots,'evidence':[{'path':'docs/readmes/146/'+n} for n in ['verification.json','comparison.json','original-evidence.json','restart-check.json']],'comparison':comparison,'validation':validation,'finding':finding};write(out/'system-map.json',system)
text(r/note,'# Return continuity - edition146\n\n[[README-146.md]]\n\n'+validation+'\n\n'+finding+'\n\n'+nextSteps+'\n\nContinue [[docs/INTEGRATED_AGENT_ROADMAP.md]].\n')
write(r/'docs/obsidian/154-return-continuity.canvas',{'nodes':[{'id':x['id'],'type':'file','file':x['source'],'x':i*430,'y':0,'width':390,'height':190} for i,x in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']} for i in range(len(modules)-1)]})
html=(r/'docs/publication146/guide-template.html').read_text(encoding='utf8')+'<script id="system-data" type="application/json">'+json.dumps(system).replace('</','<\\/')+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory).replace('</','<\\/')+'</script><script src="guide.js"></script></body></html>';text(out/'index.html',html);shutil.copy2(r/'docs/publication146/guide.js',out/'guide.js')
for name,link in [('docs/readmes/INDEX.md','[Edition146 - return continuity](../../README-146.md) | [Offline guide](146/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/154-return-continuity.md]] | [[README-146.md]]')]:
 p=r/name;s=p.read_text(encoding='utf-8-sig');text(p,link+'\n\n'+s if not s.startswith(link) else s)
print(json.dumps({'passed':True,'javaChecks':len(checks),'sources':len(inventory),'originalEntries':len(originals),'finding':finding}))
