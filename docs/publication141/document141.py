from pathlib import Path
import json,hashlib,subprocess,shutil,gzip,xml.etree.ElementTree as ET,re,datetime
r=Path.cwd();out=r/'docs/readmes/141';out.mkdir(parents=True,exist_ok=True);parent='d27846b206c73b8dc3326b398f746d0212a01fd9'
read=lambda p:json.loads((r/p).read_text(encoding='utf-8-sig'));sha=lambda b:hashlib.sha256(b).hexdigest()
def text(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(s.encode('utf8'))
def write(p,v):text(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-141']).returncode==1
study=read('build/recovery141-app/result.json');assert study['passed'];write(out/'app-study.json',study)
lock=read('docs/OBSERVATION_141_LOCK.json');sources=read('docs/publication141/source141.json')
for n,h in lock['sourceHashes'].items():assert sha((r/n).read_bytes().replace(b'\r\n',b'\n'))==h,n
checks=set()
for p in (r/'build/validation141/final').glob('*.xml'):
 e=ET.parse(p).getroot();assert int(e.get('failures','0'))+int(e.get('errors','0'))==0,p
 for c in e.findall('testcase'):
  if c.find('skipped') is None:checks.add(c.get('classname')+'.'+c.get('name'))
assert checks
desktop=(r/'build/desktop141-final.log').read_text(encoding='utf8');assert 'fail 0' in desktop;desktop_count=int(re.search(r'(?m)^. tests (\d+)',desktop)[1])
for n in ['kernel141-final','java141-final','desktop141-final','agency141-regression','launcher141','package141','receipt141']:assert read('build/'+n+'.json')['exit']==0,n
for name in study['captures']:shutil.copy2(r/'build/recovery141-app'/name,out/name)
rows=[{'case':x['caseName'],'policy':x['policy'],'summary':x['audit']['summary'],'recovery141':x['audit'].get('recovery141'),'trackingMs':x['trackingMs'],'auditMs':x['auditMs']} for x in study['runs']]
sums={}
for policy in lock['methods']:
 selected=[x for x in rows if x['policy']==policy]
 sums[policy]={'tracking':{k:sum(x['summary'][k] for x in selected) for k in ['labels','visible','correct','missing','overlapFailures','hidden','unsupported','uncertain','confirmedDistractorMatches']},'additional':{k:sum((x['recovery141'] or {}).get('additional',{}).get(k,0) for x in selected) for k in ['returned','correct','incorrect','unknown']},'filtered':{k:sum((x['recovery141'] or {}).get('filtered',{}).get(k,0) for x in selected) for k in ['accepted','correct','incorrect','unknown']}}
candidate=sums['recovery-guard-141']['tracking'];base=sums['legacy-flow']['tracking'];earlier=sums['foreground-guard-139']['tracking'];extra=sums['recovery-guard-141']['additional'];filtered=sums['recovery-guard-141']['filtered']
finding=f"Fresh candidate versus unchanged139: **{candidate['correct']} vs {earlier['correct']} correct**, **{candidate['overlapFailures']} vs {earlier['overlapFailures']} wrong**, **{candidate['missing']} vs {earlier['missing']} missing**, and **{candidate['unsupported']} vs {earlier['unsupported']} hidden outputs**. There are {candidate['visible']} visible/partial references, {candidate['hidden']} confirmed absent and {candidate['uncertain']} uncertain per method. Additional source recoveries: **{extra['correct']} correct, {extra['incorrect']} incorrect, {extra['unknown']} unknown**. The filter accepted **{filtered['correct']} correct, {filtered['incorrect']} incorrect, {filtered['unknown']} unknown** of these. Default flow: {base['correct']} correct, {base['overlapFailures']} wrong, {base['missing']} missing and {base['unsupported']} hidden outputs."
benefit=candidate['correct']>earlier['correct'] and candidate['overlapFailures']<=earlier['overlapFailures'] and candidate['unsupported']<=earlier['unsupported']
finding+=(" This bounded cohort shows more correct observations without an additional scored wrong or hidden output." if benefit else " This fresh cohort does not demonstrate more correct observations without additional errors; recovery improvement remains unresolved.")
finding+=" The candidate stays an explicit experiment. The controlled return test proves the mechanism, not general camera reliability. This cohort has no confirmed fully hidden references and no independently labelled distractors. No universal switch-free claim follows."
diagnostics=read('build/recovery141-diagnostics.json')
comparison={'rows':rows,'summary':sums,'measuredCohortBenefit':benefit,'diagnostics':diagnostics,'lock':lock,'sources':sources,'development':read('build/recovery141-development/results.json'),'defaultsUnchanged':True,'promoted':False};write(out/'comparison.json',comparison)
validation=f"Local checks passed: {len(checks)} distinct Java tests, {desktop_count} desktop checks, kernel integration, actual agency regression and isolated offline/package preparation. {len(study['runs'])} actual-app fresh comparisons use {sum(x['run']['totalFrames'] for x in study['runs'])} later-frame outcomes. Exact monitor equivalence, original-pixel audit replay, independent host arithmetic, export, stop/resume and abrupt-process cold restart are verified."
v={'passed':True,'javaChecks':sorted(checks),'desktopChecks':desktop_count,'actualAppChecks':list(dict.fromkeys(study['checks'])),'production':study['source'],'buildReceipt':study['buildReceipt'],'candidateLock':lock,'defaultsUnchanged':True,'scope':validation+' External VOT rectangles/absence with separate source-only AI visibility/cause review; no independent human adjudication of added labels. Hosted CI separate.'};write(out/'verification.json',v)
originals=[];seen=set()
def preserve(p,logical):
 if not p.is_file() or logical in seen:return
 seen.add(logical);raw=p.read_bytes();key=sha(raw);dest=out/'originals'/(key+'.gz');packed=gzip.compress(raw,mtime=0);dest.parent.mkdir(exist_ok=True)
 if not dest.exists():dest.write_bytes(packed)
 originals.append({'logicalPath':logical,'path':dest.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(raw),'rawSha256':key})
for name in ['recovery141-app','recovery141-development','agency141-evidence','validation141','source-labels141-first','source-labels141-exact-before']:
 for p in sorted((r/'build'/name).rglob('*')):
  if any(x in p.parts for x in ['desktop-profile','node_modules','exports']) or p.name.endswith('.fobs.json'):continue
  if p.is_file() and p.suffix in ['.json','.jsonl','.xml','.png','.txt','.log','.html','.css','.js','.cjs','.fworld','.previous','.csv','.evidence','.pending','.before','.java','.class']:preserve(p,p.relative_to(r).as_posix())
for p in sorted((r/'build').glob('*141*')):
 if p.is_file() and p.suffix not in ['.zip','.jfr']:preserve(p,p.relative_to(r).as_posix())
for name in ['docs/OBSERVATION_141_LOCK.json','docs/publication141/source141.json','build/vot141/source-index.json','build/vot141/download.json','build/vot141/occlusion-search.json','build/vot141/bird-occlusion-search.json','build/vot141/extra-source.json','build/vot141/bird-extra-source.json']:preserve(r/name,name)
write(out/'original-evidence.json',{'files':originals,'scope':'Original run/audit records, development and fresh comparisons, tests, attempts, public app screenshots and startup/restart checks. Raw footage/images and private profiles excluded; exact source hashes/annotations/provenance retained.'})
paths={x['path'] for x in read('docs/readmes/140/file-inventory.json')}
for n in subprocess.check_output(['git','diff','--name-only',parent,'HEAD'],text=True).splitlines():
 if n.startswith(('workspace-engine/','workspace-desktop/','src/','assets/','.github/')) or n in ['build.gradle','.gitattributes']:paths.add(n)
paths.update(['docs/EDITION_141_CONTRACT.md','docs/OBSERVATION_141_LOCK.json','docs/publication141/source141.json']);inventory=[]
for n in sorted(paths):
 raw=(r/n).read_bytes()
 try:raw=raw.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':n,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
write(out/'file-inventory.json',inventory)
table='| Method / source | Correct / visible | Missing | Wrong overlaps | Other-object matches | Hidden outputs |\n| --- | ---: | ---: | ---: | ---: | ---: |\n'
warn='Baseline139 accepted tracker proposals are preserved exactly. Supplemental measurements may change later filter estimates; compare complete final outcomes as well as additional raw boxes. The monitor preserves139 tracker and filter outputs.\n'
cost='| Measured run phase | Tracking / ms | Audit with replay / ms |\n| --- | ---: | ---: |\n'
for x in rows:
 s=x['summary'];table+=f"| {x['policy']} / {x['case']} | {s['correct']}/{s['visible']} | {s['missing']} | {s['overlapFailures']} | {s['confirmedDistractorMatches']} | {s['unsupported']}/{s['hidden']} |\n"
 cost+=f"| {x['policy']} / {x['case']} | {x['trackingMs']:.1f} | {x['auditMs']:.1f} |\n"
st='| Recording / target | Original frames | Stride | Visible or partial | Hidden | Uncertain | Distractor-labelled samples |\n| --- | --- | ---: | ---: | ---: | ---: | ---: |\n'
for s in sources['records']:
 c=s['visibilityCounts'];st+=f"| {s['name']} / {s['target']} | {s['start']}–{s['end']} | {s['stride']} | {c['visible']+c['partial']} | {c['occluded']+c['out-of-view']} | {c['uncertain']} | {s['distractorLabelledSamples']} |\n"
elapsed=(datetime.datetime.fromisoformat(study['endedAt'].replace('Z','+00:00'))-datetime.datetime.fromisoformat(study['startedAt'].replace('Z','+00:00'))).total_seconds()
cost+=f'\nThe entire actual-app harness took {elapsed:.1f} seconds ({elapsed/60:.1f} minutes), including imports, UI inspection, repeated verified reads, export, captures, stop/resume, crash/reopen and tamper checks. The phase intervals above are subsets of this elapsed time, not additional costs to add to it. Regression/build/documentation work is separate.\n'
restorations='| Source / method | Additional boxes: correct / incorrect / unknown | Filter accepted: correct / incorrect / unknown |\n| --- | ---: | ---: |\n'
for x in rows:
 d=x['recovery141']
 if d:
  a=d['additional'];f=d['filtered'];restorations+=f"| {x['case']} / {x['policy']} | {a['correct']} / {a['incorrect']} / {a['unknown']} | {f['correct']} / {f['incorrect']} / {f['unknown']} |\n"
next_steps=(r/'build/next141.md').read_text(encoding='utf8')
diag='| Source | Searches after loss | Qualified unique candidates | Highest recorded similarity | Recorded candidate evaluations |\n| --- | ---: | ---: | ---: | ---: |\n'
for x in diagnostics['rows']:
 peak=f"{x['maximumRecordedSimilarity']:.3f}" if x['attemptedSearches'] else '—'
 diag+=f"| {x['case']} | {x['attemptedSearches']} | {x['qualifiedUniqueSearches']} | {peak} | {x['traceCandidateEvaluations']:,} |\n"
diag+='\nThese are descriptive post-run diagnostics, not new qualification criteria. Similarity is not a probability. Recorded evaluations exclude repeated work from resume and audit replay.\n'
md=(r/'docs/publication141/README-template.md').read_text(encoding='utf8')
md=md.replace('{{DIAGNOSTICS}}',diag)
for key,value in {'FINDING':finding,'RESULTS':table,'WARNINGS':warn,'SOURCES':st,'COSTS':cost,'VALIDATION':validation,'APP_CHECKS':'\n'.join('- '+x for x in v['actualAppChecks']),'SOURCE_REVIEW':sources['sourceReview'],'RECOVERIES':restorations,'HERO':f"![Actual app: experimental evidence view](docs/readmes/141/{sources['records'][0]['case']}-drift.png)",'SOURCE_IMAGE':f"![Actual source and outcome inspection](docs/readmes/141/{sources['records'][0]['case']}-source.png)",'ATTEMPTS':(r/'build/attempts141.md').read_text(encoding='utf8'),'NEXT':next_steps}.items():md=md.replace('{{'+key+'}}',value)
assert '{{' not in md;text(r/'README-141.md',md);text(r/'README.md',md)
note='docs/obsidian/149-loss-recovery.md';modules=[{'id':i,'name':n,'source':s,'note':note} for i,n,s in [('appearance','Immutable spatial parts','workspace-engine/src/main/java/fraymus/workspace/SpatialEvidence139.java'),('guard','Loss recovery and monitor','workspace-engine/src/main/java/fraymus/workspace/LossRecovery141.java'),('restore','Multi-location source search','workspace-engine/src/main/java/fraymus/workspace/RecoverySearch141.java'),('audit','Original-pixel replay and outcome audit','workspace-engine/src/main/java/fraymus/workspace/LossRecoveryAudit141.java'),('world','Durable observed world','workspace-engine/src/main/java/fraymus/workspace/WorldObservationStore.java'),('panel','Live drift evidence','workspace-desktop/renderer/observation-panel.js')]]
system={'modules':modules,'screenshots':[{'path':'docs/readmes/141/'+name,'title':name.removesuffix('.png').replace('-',' ')} for name in study['captures']],'evidence':[{'path':'docs/readmes/141/'+n} for n in ['verification.json','app-study.json','original-evidence.json','comparison.json']],'comparison':comparison,'validation':validation,'finding':finding.replace('**','')};write(out/'system-map.json',system)
text(r/note,'# Recovery after loss — edition141\n\n[[README-141.md]]\n\n'+validation+'\n\n'+finding+'\n\n'+next_steps+'\n\nPreserve defaults, historical bytes and source/outcome checks. Continue [[docs/INTEGRATED_AGENT_ROADMAP.md]].\n')
write(r/'docs/obsidian/149-loss-recovery.canvas',{'nodes':[{'id':x['id'],'type':'file','file':x['source'],'x':i*430,'y':0,'width':390,'height':190} for i,x in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']} for i in range(len(modules)-1)]})
html=(r/'docs/publication141/guide-template.html').read_text(encoding='utf8')+'<script id="system-data" type="application/json">'+json.dumps(system).replace('</','<\\/')+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory).replace('</','<\\/')+'</script><script src="guide.js"></script></body></html>'
text(out/'index.html',html);shutil.copy2(r/'docs/publication141/guide.js',out/'guide.js')
for name,link in [('docs/readmes/INDEX.md','[Edition141 — recovery after loss](../../README-141.md) | [Offline guide](141/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/149-loss-recovery.md]] | [[README-141.md]]')]:
 p=r/name;s=p.read_text(encoding='utf-8-sig');text(p,link+'\n\n'+s if not s.startswith(link) else s)
print(json.dumps({'passed':True,'javaChecks':len(checks),'sources':len(inventory),'originalEntries':len(originals),'finding':finding}))
