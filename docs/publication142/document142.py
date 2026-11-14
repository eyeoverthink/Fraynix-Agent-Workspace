from pathlib import Path
import json,hashlib,subprocess,shutil,gzip,xml.etree.ElementTree as ET,re,datetime
r=Path.cwd();out=r/'docs/readmes/142';out.mkdir(parents=True,exist_ok=True);parent='3aa81866d6195303a45ea040ac24f731f35fbc04'
read=lambda p:json.loads((r/p).read_text(encoding='utf-8-sig'));sha=lambda b:hashlib.sha256(b).hexdigest()
def text(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(s.encode('utf8'))
def write(p,v):text(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-142']).returncode==1
study=read('build/partial142-app/result.json');assert study['passed'];write(out/'app-study.json',study)
lock=read('docs/OBSERVATION_142_LOCK.json');sources=read('docs/publication142/source142.json')
for n,h in lock['sourceHashes'].items():assert sha((r/n).read_bytes().replace(b'\r\n',b'\n'))==h,n
checks=set()
for p in (r/'build/validation142/final').glob('*.xml'):
 e=ET.parse(p).getroot();assert int(e.get('failures','0'))+int(e.get('errors','0'))==0,p
 for c in e.findall('testcase'):
  if c.find('skipped') is None:checks.add(c.get('classname')+'.'+c.get('name'))
assert checks
desktop=(r/'build/desktop142-final.log').read_text(encoding='utf8');assert 'fail 0' in desktop;desktop_count=int(re.search(r'(?m)^. tests (\d+)',desktop)[1])
for n in ['kernel142-final','java142-final','desktop142-final','agency142-regression','launcher142','package142','receipt142']:assert read('build/'+n+'.json')['exit']==0,n
for name in study['captures']:shutil.copy2(r/'build/partial142-app'/name,out/name)
review=read('build/partial142-review/result.json');assert review['passed'];write(out/'screenshot-review.json',review)
for item in review['captures']:shutil.copy2(r/'build/partial142-review'/item['name'],out/item['name'])
rows=[{'case':x['caseName'],'domain':x['domain'],'policy':x['policy'],'summary':x['audit']['summary'],'partial142':x['audit'].get('partial142'),'trackingMs':x['trackingMs'],'auditMs':x['auditMs']} for x in study['runs'] if x.get('role')=='evaluation']
sums={}
for policy in lock['methods']:
 selected=[x for x in rows if x['policy']==policy]
 sums[policy]={'tracking':{k:sum(x['summary'][k] for x in selected) for k in ['labels','visible','correct','missing','overlapFailures','hidden','unsupported','uncertain','confirmedDistractorMatches']},'additional':{k:sum((x['partial142'] or {}).get('additional',{}).get(k,0) for x in selected) for k in ['returned','correct','incorrect','unknown']},'filtered':{k:sum((x['partial142'] or {}).get('filtered',{}).get(k,0) for x in selected) for k in ['accepted','correct','incorrect','unknown']}}
byDomain={}
for domain in ['real-camera','generated-control']:
 byDomain[domain]={}
 for policy in lock['methods']:
  selected=[x for x in rows if x['policy']==policy and x['domain']==domain]
  byDomain[domain][policy]={'tracking':{k:sum(x['summary'][k] for x in selected) for k in ['labels','visible','correct','missing','overlapFailures','hidden','unsupported','uncertain','confirmedDistractorMatches']},'additional':{k:sum((x['partial142'] or {}).get('additional',{}).get(k,0) for x in selected) for k in ['returned','correct','incorrect','unknown']},'filtered':{k:sum((x['partial142'] or {}).get('filtered',{}).get(k,0) for x in selected) for k in ['accepted','correct','incorrect','unknown']}}
candidate=byDomain['real-camera']['partial-reference-142']['tracking'];earlier=byDomain['real-camera']['foreground-guard-139']['tracking'];seed=byDomain['real-camera']['partial-seed-142']['tracking'];extra=byDomain['real-camera']['partial-reference-142']['additional'];filtered=byDomain['real-camera']['partial-reference-142']['filtered']
finding=f"**Real footage:** candidate {candidate['correct']}/{candidate['visible']} correct, {candidate['missing']} missing, {candidate['overlapFailures']} wrong overlaps and {candidate['unsupported']} hidden outputs; guard139 {earlier['correct']} correct, {earlier['missing']} missing, {earlier['overlapFailures']} wrong and {earlier['unsupported']} hidden outputs. Seed-only control: {seed['correct']} correct, {seed['overlapFailures']} wrong. Additional raw recoveries: {extra['correct']} correct, {extra['incorrect']} incorrect, {extra['unknown']} unscored. There are {candidate['hidden']} confirmed absent and {candidate['uncertain']} uncertain reference samples per method."
benefit=candidate['correct']>earlier['correct'] and candidate['overlapFailures']<=earlier['overlapFailures'] and candidate['unsupported']<=earlier['unsupported'] and candidate['confirmedDistractorMatches']<=earlier['confirmedDistractorMatches']
finding+=(" This bounded real cohort shows more correct observations without additional scored errors." if benefit else " This real cohort does not establish a recovery gain without added errors.")
g=byDomain['generated-control']['partial-reference-142'];gb=byDomain['generated-control']['foreground-guard-139']['tracking'];gs=byDomain['generated-control']['partial-seed-142']['tracking'];gt=g['tracking']
finding+=f"\n\n**Generated controls:** candidate {gt['correct']}/{gt['visible']} correct, {gt['overlapFailures']} wrong, {gt['unsupported']} hidden outputs and {gt['confirmedDistractorMatches']} labelled-other matches. Guard139: {gb['correct']} correct; seed-only: {gs['correct']} correct. Additional raw boxes: {g['additional']}. Generated controls do not establish camera reliability. **Defaults remain unchanged; no promotion.**"
diagnostics=read('build/partial142-diagnostics.json')
comparison={'rows':rows,'summary':sums,'measuredCohortBenefit':benefit,'diagnostics':diagnostics,'lock':lock,'sources':sources,'byDomain':byDomain,'defaultsUnchanged':True,'promoted':False};write(out/'comparison.json',comparison)
validation=f"Local checks passed: {len(checks)} distinct Java tests, {desktop_count} desktop checks, kernel integration, actual agency regression and isolated offline/package preparation. Three separate donor runs and {len(rows)} evaluation runs use {sum(x['run']['totalFrames'] for x in study['runs'] if x.get('role')=='evaluation')} evaluation-frame outcomes; donor outcomes are counted separately. Exact monitor equivalence, original-pixel audit replay, independent host arithmetic, export, stop/resume and abrupt-process cold restart are verified."
v={'passed':True,'javaChecks':sorted(checks),'desktopChecks':desktop_count,'actualAppChecks':list(dict.fromkeys(study['checks'])),'production':study['source'],'buildReceipt':study['buildReceipt'],'candidateLock':lock,'defaultsUnchanged':True,'scope':validation+' External OTB rectangles with separate source-only AI visibility/competitor review; generated cases have renderer-derived labels. No independent human adjudication of camera labels. Hosted CI separate.'};write(out/'verification.json',v)
originals=[];seen=set()
def preserve(p,logical):
 if not p.is_file() or logical in seen:return
 seen.add(logical);raw=p.read_bytes();key=sha(raw);dest=out/'originals'/(key+'.gz');packed=gzip.compress(raw,mtime=0);dest.parent.mkdir(exist_ok=True)
 if not dest.exists():dest.write_bytes(packed)
 originals.append({'logicalPath':logical,'path':dest.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(raw),'rawSha256':key})
for name in ['partial142-app','partial142-review','agency142-evidence','validation142','source-labels142-first','source-labels142-exact-before']:
 for p in sorted((r/'build'/name).rglob('*')):
  if any(x in p.parts for x in ['desktop-profile','node_modules','exports']) or p.name.endswith('.fobs.json'):continue
  if p.is_file() and p.suffix in ['.json','.jsonl','.xml','.png','.txt','.log','.html','.css','.js','.cjs','.fworld','.previous','.csv','.evidence','.pending','.before','.java','.class']:preserve(p,p.relative_to(r).as_posix())
for p in sorted((r/'build').glob('*142*')):
 if p.is_file() and p.suffix not in ['.zip','.jfr']:preserve(p,p.relative_to(r).as_posix())
for name in ['docs/OBSERVATION_142_LOCK.json','docs/publication142/source142.json','build/otb142/download.json','build/otb142/review/sheets.json']:preserve(r/name,name)
write(out/'original-evidence.json',{'files':originals,'scope':'Original run/audit records, development and fresh comparisons, tests, attempts, public app screenshots and startup/restart checks. Raw footage/images and private profiles excluded; exact source hashes/annotations/provenance retained.'})
paths={x['path'] for x in read('docs/readmes/141/file-inventory.json')}
for n in subprocess.check_output(['git','diff','--name-only',parent,'HEAD'],text=True).splitlines():
 if n.startswith(('workspace-engine/','workspace-desktop/','src/','assets/','.github/')) or n in ['build.gradle','.gitattributes']:paths.add(n)
paths.update(['docs/EDITION_142_CONTRACT.md','docs/OBSERVATION_142_LOCK.json','docs/publication142/source142.json']);inventory=[]
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
 s=x['summary'];table+=f"| {x['domain']} / {x['policy']} / {x['case']} | {s['correct']}/{s['visible']} | {s['missing']} | {s['overlapFailures']} | {s['confirmedDistractorMatches']} | {s['unsupported']}/{s['hidden']} |\n"
 cost+=f"| {x['domain']} / {x['policy']} / {x['case']} | {x['trackingMs']:.1f} | {x['auditMs']:.1f} |\n"
for donor in [x for x in study['runs'] if x.get('role')=='reference-source']:
 cost+=f"| verified donor / {donor['caseName']} | {donor['trackingMs']:.1f} | {donor['auditMs']:.1f} |\n"
st='| Recording / target | Original frames | Stride | Visible or partial | Hidden | Uncertain | Distractor-labelled samples |\n| --- | --- | ---: | ---: | ---: | ---: | ---: |\n'
for s in sources['records']:
 c=s['visibilityCounts'];st+=f"| {s['domain']} / {s['role']} / {s['case']} | {s['start']}–{s['end']} | {s['stride']} | {c['visible']+c['partial']} | {c['occluded']+c['out-of-view']} | {c['uncertain']} | {s['distractorLabelledSamples']} |\n"
elapsed=(datetime.datetime.fromisoformat(study['endedAt'].replace('Z','+00:00'))-datetime.datetime.fromisoformat(study['startedAt'].replace('Z','+00:00'))).total_seconds()
cost+=f'\nThe entire actual-app harness took {elapsed:.1f} seconds ({elapsed/60:.1f} minutes), including imports, UI inspection, repeated verified reads, export, captures, stop/resume, crash/reopen and tamper checks. The phase intervals above are subsets of this elapsed time, not additional costs to add to it. Regression/build/documentation work is separate.\n'
restorations='| Source / method | Additional boxes: correct / incorrect / unknown | Filter accepted: correct / incorrect / unknown |\n| --- | ---: | ---: |\n'
for x in rows:
 d=x['partial142']
 if d:
  a=d['additional'];f=d['filtered'];restorations+=f"| {x['case']} / {x['policy']} | {a['correct']} / {a['incorrect']} / {a['unknown']} | {f['correct']} / {f['incorrect']} / {f['unknown']} |\n"
next_steps=(r/'build/next142.md').read_text(encoding='utf8')
diag='| Source | Searches after loss | Qualified unique candidates | Highest recorded similarity | Max confirmations / adjacent reference switches | Position evaluations / descriptor-region comparisons |\n| --- | ---: | ---: | ---: | ---: | ---: |\n'
for x in diagnostics['rows']:
 peak=f"{x['maximumRecordedSimilarity']:.3f}" if x['attemptedSearches'] else '—'
 diag+=f"| {x['case']} | {x['attemptedSearches']} | {x['qualifiedUniqueSearches']} | {peak} | {x['maximumConfirmations']} / {x['adjacentUniqueReferenceChanges']} | {x['traceCandidateEvaluations']:,} / {x['descriptorRegionComparisons']:,} |\n"
diag+='\nThese are descriptive post-run diagnostics, not new qualification criteria. Similarity is not a probability. Recorded evaluations exclude repeated work from resume and audit replay.\n'
md=(r/'docs/publication142/README-template.md').read_text(encoding='utf8')
md=md.replace('{{DIAGNOSTICS}}',diag)
for key,value in {'FINDING':finding,'RESULTS':table,'WARNINGS':warn,'SOURCES':st,'COSTS':cost,'VALIDATION':validation,'APP_CHECKS':'\n'.join('- '+x for x in v['actualAppChecks']),'SOURCE_REVIEW':sources['sourceReview'],'RECOVERIES':restorations,'HERO':'![Actual app: real partial cover, retained baseline observation](docs/readmes/142/faceocc2-25-review-drift.png)','SOURCE_IMAGE':'![Real partial-cover source with independent outcome inspection](docs/readmes/142/faceocc2-25-review-source.png)\n\nIllustrative frame 85 from the completed FaceOcc2 evaluation: the underlying tracker still has the target. This is not an extra recovery.\n\n![Generated partial-view control](docs/readmes/142/generated-partial-review-drift.png)\n\n![Generated competing-copy control](docs/readmes/142/generated-twin-review-source.png)\n\nThese generated controls are separate from the real camera results. Selected frames illustrate completed evidence; the full tables include every scored frame.','ATTEMPTS':(r/'build/attempts142.md').read_text(encoding='utf8'),'NEXT':next_steps}.items():md=md.replace('{{'+key+'}}',value)
assert '{{' not in md;text(r/'README-142.md',md);text(r/'README.md',md)
note='docs/obsidian/150-partial-views.md';modules=[{'id':i,'name':n,'source':s,'note':note} for i,n,s in [('appearance','Verified reference provenance','workspace-engine/src/main/java/fraymus/workspace/ReferenceMemory137.java'),('guard','Partial recovery and monitor','workspace-engine/src/main/java/fraymus/workspace/PartialRecovery142.java'),('restore','Fixed-region partial search','workspace-engine/src/main/java/fraymus/workspace/PartialSearch142.java'),('audit','Original-pixel replay and outcome audit','workspace-engine/src/main/java/fraymus/workspace/PartialAudit142.java'),('world','Durable observed world','workspace-engine/src/main/java/fraymus/workspace/WorldObservationStore.java'),('panel','Live drift evidence','workspace-desktop/renderer/observation-panel.js')]]
system={'modules':modules,'screenshots':[{'path':'docs/readmes/142/'+name,'title':name.removesuffix('.png').replace('-',' ')} for name in study['captures']]+[{'path':'docs/readmes/142/'+x['name'],'title':x['caption']} for x in review['captures']],'evidence':[{'path':'docs/readmes/142/'+n} for n in ['verification.json','app-study.json','original-evidence.json','comparison.json']],'comparison':comparison,'validation':validation,'finding':finding.replace('**','')};write(out/'system-map.json',system)
text(r/note,'# Partial views — edition 142\n\n[[README-142.md]]\n\n'+validation+'\n\n'+finding+'\n\n'+next_steps+'\n\nPreserve defaults, historical bytes and source/outcome checks. Continue [[docs/INTEGRATED_AGENT_ROADMAP.md]].\n')
write(r/'docs/obsidian/150-partial-views.canvas',{'nodes':[{'id':x['id'],'type':'file','file':x['source'],'x':i*430,'y':0,'width':390,'height':190} for i,x in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']} for i in range(len(modules)-1)]})
html=(r/'docs/publication142/guide-template.html').read_text(encoding='utf8')+'<script id="system-data" type="application/json">'+json.dumps(system).replace('</','<\\/')+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory).replace('</','<\\/')+'</script><script src="guide.js"></script></body></html>'
text(out/'index.html',html);shutil.copy2(r/'docs/publication142/guide.js',out/'guide.js')
for name,link in [('docs/readmes/INDEX.md','[Edition142 — partial views from verified appearances](../../README-142.md) | [Offline guide](142/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/150-partial-views.md]] | [[README-142.md]]')]:
 p=r/name;s=p.read_text(encoding='utf-8-sig');text(p,link+'\n\n'+s if not s.startswith(link) else s)
print(json.dumps({'passed':True,'javaChecks':len(checks),'sources':len(inventory),'originalEntries':len(originals),'finding':finding}))
