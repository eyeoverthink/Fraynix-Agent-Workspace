from pathlib import Path
import json,hashlib,subprocess,shutil,gzip,xml.etree.ElementTree as ET,re,datetime
r=Path.cwd();out=r/'docs/readmes/143';out.mkdir(parents=True,exist_ok=True);parent='fe1e5c3fbe0a4512a00d5488efe6a73f42191f06'
read=lambda p:json.loads((r/p).read_text(encoding='utf-8-sig'));sha=lambda b:hashlib.sha256(b).hexdigest()
def text(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(s.encode('utf8'))
def write(p,v):text(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-143']).returncode==1
study=read('build/confirmation143-app/result.json');assert study['passed'];write(out/'app-study.json',study)
lock=read('docs/OBSERVATION_143_LOCK.json');sources=read('docs/publication143/source143.json')
for n,h in lock['sourceHashes'].items():assert sha((r/n).read_bytes().replace(b'\r\n',b'\n'))==h,n
checks=set()
for p in (r/'build/validation143/final').glob('*.xml'):
 e=ET.parse(p).getroot();assert int(e.get('failures','0'))+int(e.get('errors','0'))==0,p
 for c in e.findall('testcase'):
  if c.find('skipped') is None:checks.add(c.get('classname')+'.'+c.get('name'))
assert checks
desktop=(r/'build/desktop143-final.log').read_text(encoding='utf8');assert 'fail 0' in desktop;desktop_count=int(re.search(r'(?m)^. tests (\d+)',desktop)[1])
for n in ['kernel143-final','java143-final','desktop143-final','agency143-regression','launcher143','package143','receipt143']:assert read('build/'+n+'.json')['exit']==0,n
for name in study['captures']:shutil.copy2(r/'build/confirmation143-app'/name,out/name)
review=read('build/confirmation143-review/result.json');assert review['passed'];write(out/'screenshot-review.json',review)
for item in review['captures']:shutil.copy2(r/'build/confirmation143-review'/item['name'],out/item['name'])
rows=[{'case':x['caseName'],'domain':x['domain'],'policy':x['policy'],'summary':x['audit']['summary'],'references143':x['audit'].get('references143') or x['audit'].get('partial142'),'trackingMs':x['trackingMs'],'auditMs':x['auditMs']} for x in study['runs'] if x.get('role')=='evaluation']
sums={}
for policy in lock['methods']:
 selected=[x for x in rows if x['policy']==policy]
 sums[policy]={'tracking':{k:sum(x['summary'][k] for x in selected) for k in ['labels','visible','correct','missing','overlapFailures','hidden','unsupported','uncertain','confirmedDistractorMatches']},'additional':{k:sum((x['references143'] or {}).get('additional',{}).get(k,0) for x in selected) for k in ['returned','correct','incorrect','unknown']},'filtered':{k:sum((x['references143'] or {}).get('filtered',{}).get(k,0) for x in selected) for k in ['accepted','correct','incorrect','unknown']}}
pairedCases={x['case'] for x in rows if x['policy']=='reference-confirmation-143'}
byDomain={}
for domain in ['real-camera','generated-control']:
 byDomain[domain]={}
 for policy in lock['methods']:
  selected=[x for x in rows if x['policy']==policy and x['domain']==domain and x['case'] in pairedCases]
  byDomain[domain][policy]={'tracking':{k:sum(x['summary'][k] for x in selected) for k in ['labels','visible','correct','missing','overlapFailures','hidden','unsupported','uncertain','confirmedDistractorMatches']},'additional':{k:sum((x['references143'] or {}).get('additional',{}).get(k,0) for x in selected) for k in ['returned','correct','incorrect','unknown']},'filtered':{k:sum((x['references143'] or {}).get('filtered',{}).get(k,0) for x in selected) for k in ['accepted','correct','incorrect','unknown']}}
previous=byDomain['real-camera']['partial-reference-142']['tracking']
candidate=byDomain['real-camera']['reference-confirmation-143']['tracking'];earlier=byDomain['real-camera']['foreground-guard-139']['tracking'];seed=byDomain['real-camera']['reference-seed-143']['tracking'];extra=byDomain['real-camera']['reference-confirmation-143']['additional'];filtered=byDomain['real-camera']['reference-confirmation-143']['filtered']
finding=f"**Paired eligible real footage:** candidate {candidate['correct']}/{candidate['visible']} correct, {candidate['missing']} missing, {candidate['overlapFailures']} wrong overlaps and {candidate['unsupported']} hidden outputs; guard139 {earlier['correct']} correct, {earlier['missing']} missing, {earlier['overlapFailures']} wrong and {earlier['unsupported']} hidden outputs. Previous142: {previous['correct']} correct, {previous['overlapFailures']} wrong, {previous['missing']} missing. Seed-only control: {seed['correct']} correct, {seed['overlapFailures']} wrong. Additional raw recoveries: {extra['correct']} correct, {extra['incorrect']} incorrect, {extra['unknown']} unscored. There are {candidate['hidden']} confirmed absent and {candidate['uncertain']} uncertain reference samples per method."
benefit=candidate['correct']>earlier['correct'] and candidate['overlapFailures']<=earlier['overlapFailures'] and candidate['unsupported']<=earlier['unsupported'] and candidate['confirmedDistractorMatches']<=earlier['confirmedDistractorMatches']
finding+=(" This bounded real cohort shows more correct observations without additional scored errors." if benefit else " This real cohort does not establish a recovery gain without added errors.")
g=byDomain['generated-control']['reference-confirmation-143'];gb=byDomain['generated-control']['foreground-guard-139']['tracking'];gs=byDomain['generated-control']['reference-seed-143']['tracking'];gt=g['tracking'];gp=byDomain['generated-control']['partial-reference-142']['tracking']
finding+=f"\n\n**Generated controls:** candidate {gt['correct']}/{gt['visible']} correct, {gt['overlapFailures']} low-overlap visible outputs, {gt['unsupported']} output while the target was hidden and {gt['confirmedDistractorMatches']} labelled-other matches. Guard139: {gb['correct']} correct; previous142: {gp['correct']} correct, {gp['overlapFailures']} wrong and {gp['unsupported']} hidden outputs; seed-only: {gs['correct']} correct. Additional raw boxes: {g['additional']['correct']} correct, {g['additional']['incorrect']} incorrect and {g['additional']['unknown']} unscored. Generated controls do not establish camera reliability. **Defaults remain unchanged; no promotion.**"
refusals=study.get('refusals',[])
if refusals:
 finding+='\n\n**Eligibility refusal:** Coupon donor tracking was correct on 23/23 references, but existing motion/appearance admission produced no verified examples. All four reference-dependent methods are unavailable on Coupon; its139 baseline remains in the full table. Paired totals compare only cases completed by every method. No replacement source or relaxed admission. '+str(len(refusals))+' unavailable method/source combinations are retained, not counted as successful trials.'
diagnostics=read('build/references143-diagnostics.json')
comparison={'rows':rows,'refusals':study.get('refusals',[]),'pairedCases':sorted(pairedCases),'summary':sums,'measuredCohortBenefit':benefit,'diagnostics':diagnostics,'lock':lock,'sources':sources,'byDomain':byDomain,'defaultsUnchanged':True,'promoted':False};write(out/'comparison.json',comparison)
validation=f"Local checks passed: {len(checks)} distinct Java tests, {desktop_count} desktop checks, kernel integration, actual agency regression and isolated offline/package preparation. Three donor-source comparisons, with earlier failed-attempt runs preserved ({sum(bool(x.get('retained')) for x in study['runs'] if x.get('role')=='reference-source')} retained sets) and {len(rows)} evaluation runs use {sum(x['run']['totalFrames'] for x in study['runs'] if x.get('role')=='evaluation')} evaluation-frame outcomes; donor outcomes are counted separately. Exact monitor equivalence, original-pixel audit replay, independent host arithmetic, export, stop/resume and abrupt-process cold restart are verified."
v={'passed':True,'javaChecks':sorted(checks),'desktopChecks':desktop_count,'actualAppChecks':list(dict.fromkeys(study['checks'])),'production':study['source'],'buildReceipt':study['buildReceipt'],'candidateLock':lock,'defaultsUnchanged':True,'scope':validation+' External OTB rectangles with separate source-only AI visibility/competitor review; generated cases have renderer-derived labels. No independent human adjudication of camera labels. Hosted CI separate.'};write(out/'verification.json',v)
originals=[];seen=set()
def preserve(p,logical):
 if not p.is_file() or logical in seen:return
 seen.add(logical);raw=p.read_bytes();key=sha(raw);dest=out/'originals'/(key+'.gz');packed=gzip.compress(raw,mtime=0);dest.parent.mkdir(exist_ok=True)
 if not dest.exists():dest.write_bytes(packed)
 originals.append({'logicalPath':logical,'path':dest.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(raw),'rawSha256':key})
for name in ['confirmation143-app','confirmation143-review','agency143-evidence','validation143','source-labels143-first','source-labels143-exact-before']:
 for p in sorted((r/'build'/name).rglob('*')):
  if any(x in p.parts for x in ['desktop-profile','node_modules','exports']) or p.name.endswith('.fobs.json'):continue
  if p.is_file() and p.suffix in ['.json','.jsonl','.xml','.png','.txt','.log','.html','.css','.js','.cjs','.fworld','.previous','.csv','.evidence','.pending','.before','.java','.class']:preserve(p,p.relative_to(r).as_posix())
for p in sorted((r/'build').glob('*143*')):
 if p.is_file() and p.suffix not in ['.zip','.jfr']:preserve(p,p.relative_to(r).as_posix())
for name in ['docs/OBSERVATION_143_LOCK.json','docs/publication143/source143.json','build/otb143/download.json','build/otb143/review/sheets.json']:preserve(r/name,name)
write(out/'original-evidence.json',{'files':originals,'scope':'Original run/audit records, development and fresh comparisons, tests, attempts, public app screenshots and startup/restart checks. Raw footage/images and private profiles excluded; exact source hashes/annotations/provenance retained.'})
paths={x['path'] for x in read('docs/readmes/142/file-inventory.json')}
for n in subprocess.check_output(['git','diff','--name-only',parent,'HEAD'],text=True).splitlines():
 if n.startswith(('workspace-engine/','workspace-desktop/','src/','assets/','.github/')) or n in ['build.gradle','.gitattributes']:paths.add(n)
paths.update(['docs/EDITION_143_CONTRACT.md','docs/OBSERVATION_143_LOCK.json','docs/publication143/source143.json']);inventory=[]
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
 cost+=f"| donor audit ({'retained' if donor.get('retained') else 'retention refused'}) / {donor['caseName']} | {donor['trackingMs']:.1f} | {donor['auditMs']:.1f} |\n"
st='| Recording / target | Original frames | Stride | Visible or partial | Hidden | Uncertain | Distractor-labelled samples |\n| --- | --- | ---: | ---: | ---: | ---: | ---: |\n'
for s in sources['records']:
 c=s['visibilityCounts'];st+=f"| {s['domain']} / {s['role']} / {s['case']} | {s['start']}–{s['end']} | {s['stride']} | {c['visible']+c['partial']} | {c['occluded']+c['out-of-view']} | {c['uncertain']} | {s['distractorLabelledSamples']} |\n"
elapsed=(datetime.datetime.fromisoformat(study['endedAt'].replace('Z','+00:00'))-datetime.datetime.fromisoformat(study['startedAt'].replace('Z','+00:00'))).total_seconds()
cost+=f'\nThe actual-app interval across retained attempts and resumptions was {elapsed:.1f} seconds ({elapsed/60:.1f} minutes), including imports, UI inspection, repeated verified reads, export, captures, stop/resume, crash/reopen and tamper checks. The phase intervals above are subsets of this elapsed time, not additional costs to add to it. This wall-clock interval includes time between failed-attempt resumptions; it is not an uninterrupted benchmark. Regression/build/documentation work is separate.\n'
restorations='| Source / method | Additional boxes: correct / incorrect / unknown | Filter accepted: correct / incorrect / unknown |\n| --- | ---: | ---: |\n'
for x in rows:
 d=x['references143']
 if d:
  a=d['additional'];f=d['filtered'];restorations+=f"| {x['case']} / {x['policy']} | {a['correct']} / {a['incorrect']} / {a['unknown']} | {f['correct']} / {f['incorrect']} / {f['unknown']} |\n"
table+='\nUnavailable reference trials: '+('; '.join(x['caseName']+' / '+x['policy']+' — '+x['reason'] for x in study.get('refusals',[])) or 'none')+'\n'
next_steps=(r/'build/next143.md').read_text(encoding='utf8')
diag='| Source | Searches after loss | Qualified unique candidates | Highest recorded similarity | Max confirmations / adjacent reference switches | Position evaluations / descriptor-region comparisons |\n| --- | ---: | ---: | ---: | ---: | ---: |\n'
for x in diagnostics['rows']:
 peak=f"{x['maximumRecordedSimilarity']:.3f}" if x['attemptedSearches'] else '—'
 diag+=f"| {x['case']} | {x['attemptedSearches']} | {x['qualifiedUniqueSearches']} | {peak} | {x['maximumConfirmations']} / {x['adjacentUniqueReferenceChanges']} | {x['traceCandidateEvaluations']:,} / {x['descriptorRegionComparisons']:,} |\n"
diag+='\nThese are descriptive post-run diagnostics, not new qualification criteria. Similarity is not a probability. Recorded evaluations exclude repeated work from resume and audit replay.\n'
md=(r/'docs/publication143/README-template.md').read_text(encoding='utf8')
md=md.replace('{{DIAGNOSTICS}}',diag)
for key,value in {'FINDING':finding,'RESULTS':table,'WARNINGS':warn,'SOURCES':st,'COSTS':cost,'VALIDATION':validation,'APP_CHECKS':'\n'.join('- '+x for x in v['actualAppChecks']),'SOURCE_REVIEW':sources['sourceReview'],'RECOVERIES':restorations,'HERO':'![Actual app: real partial cover, retained baseline observation](docs/readmes/143/faceocc1-25-review-drift.png)','SOURCE_IMAGE':'![Real partial-cover source with independent outcome inspection](docs/readmes/143/faceocc1-25-review-source.png)\n\nIllustrative independently labelled partial-cover frame from the completed FaceOcc1 evaluation. See the recorded frame and outcome; this screenshot alone does not establish a recovery gain.\n\n![Generated partial-view control](docs/readmes/143/generated-partial-review-drift.png)\n\n![Generated competing-copy control](docs/readmes/143/generated-twin-review-source.png)\n\nThese generated controls are separate from the real camera results. Selected frames illustrate completed evidence; the full tables include every scored frame.','ATTEMPTS':(r/'build/attempts143.md').read_text(encoding='utf8'),'NEXT':next_steps}.items():md=md.replace('{{'+key+'}}',value)
assert '{{' not in md;text(r/'README-143.md',md);text(r/'README.md',md)
note='docs/obsidian/151-reference-confirmation.md';modules=[{'id':i,'name':n,'source':s,'note':note} for i,n,s in [('appearance','Verified reference provenance','workspace-engine/src/main/java/fraymus/workspace/ReferenceMemory137.java'),('guard','Independent confirmation and monitor','workspace-engine/src/main/java/fraymus/workspace/AppearanceConfirmation143.java'),('restore','Fixed-region partial search','workspace-engine/src/main/java/fraymus/workspace/ReferenceSearch143.java'),('audit','Original-pixel replay and outcome audit','workspace-engine/src/main/java/fraymus/workspace/ConfirmationAudit143.java'),('world','Durable observed world','workspace-engine/src/main/java/fraymus/workspace/WorldObservationStore.java'),('panel','Live drift evidence','workspace-desktop/renderer/observation-panel.js')]]
system={'modules':modules,'screenshots':[{'path':'docs/readmes/143/'+name,'title':name.removesuffix('.png').replace('-',' ')} for name in study['captures']]+[{'path':'docs/readmes/143/'+x['name'],'title':x['caption']} for x in review['captures']],'evidence':[{'path':'docs/readmes/143/'+n} for n in ['verification.json','app-study.json','original-evidence.json','comparison.json']],'comparison':comparison,'validation':validation,'finding':finding.replace('**','')};write(out/'system-map.json',system)
text(r/note,'# Independent reference confirmation — edition 143\n\n[[README-143.md]]\n\n'+validation+'\n\n'+finding+'\n\n'+next_steps+'\n\nPreserve defaults, historical bytes and source/outcome checks. Continue [[docs/INTEGRATED_AGENT_ROADMAP.md]].\n')
write(r/'docs/obsidian/151-reference-confirmation.canvas',{'nodes':[{'id':x['id'],'type':'file','file':x['source'],'x':i*430,'y':0,'width':390,'height':190} for i,x in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']} for i in range(len(modules)-1)]})
html=(r/'docs/publication143/guide-template.html').read_text(encoding='utf8')+'<script id="system-data" type="application/json">'+json.dumps(system).replace('</','<\\/')+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory).replace('</','<\\/')+'</script><script src="guide.js"></script></body></html>'
text(out/'index.html',html);shutil.copy2(r/'docs/publication143/guide.js',out/'guide.js')
for name,link in [('docs/readmes/INDEX.md','[Edition143 — independent appearance confirmation](../../README-143.md) | [Offline guide](143/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/151-reference-confirmation.md]] | [[README-143.md]]')]:
 p=r/name;s=p.read_text(encoding='utf-8-sig');text(p,link+'\n\n'+s if not s.startswith(link) else s)
print(json.dumps({'passed':True,'javaChecks':len(checks),'sources':len(inventory),'originalEntries':len(originals),'finding':finding}))
