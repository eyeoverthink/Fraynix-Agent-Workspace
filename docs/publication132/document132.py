from pathlib import Path
import json,hashlib,subprocess,shutil,gzip,xml.etree.ElementTree as ET,re,statistics
r=Path.cwd();out=r/'docs/readmes/132';out.mkdir(parents=True,exist_ok=True)
read=lambda p:json.loads((r/p).read_text(encoding='utf8'))
sha=lambda b:hashlib.sha256(b).hexdigest()
def text(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8')
def write(p,v):text(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-132']).returncode==1
study=read('build/labels132-app/result.json');assert study['passed'];write(out/'app-study.json',study)
for name in study['captures']:shutil.copy2(r/'build/labels132-app'/name,out/name)
checks=set()
for p in list((r/'build/validation132/final').glob('*.xml'))+list((r/'build/validation132/verifier-final').glob('*.xml')):
 e=ET.parse(p).getroot();assert int(e.get('failures','0'))+int(e.get('errors','0'))==0,p
 for c in e.findall('testcase'):
  if c.find('skipped') is None:checks.add(c.get('classname')+'.'+c.get('name'))
assert checks
desktop=(r/'build/desktop132-final.log').read_text(encoding='utf8');assert 'fail 0' in desktop;desktop_count=int(re.search(r'(?m)^. tests (\d+)',desktop)[1])
for n in ['kernel132-final','java132-final','desktop132-final','agency132-regression','launcher132','package132','receipt132']:assert read('build/'+n+'.json')['exit']==0,n
comparison={'rows':[],'summary':{},'defaultsUnchanged':True,'promotionAttempted':False,'scope':study['scope'],'realFullyOccludedOrOutOfViewTargetLabels':0,'observableContactsPerMethod':22,'fixedLabelsPerMethod':25}
for c in study['comparisons']:
 comparison['rows'].append({k:c[k] for k in ['window','policy','trackingMs','auditMs']}|{'summary':c['audit']['summary'],'visibility':c['audit']['visibility'],'labelId':c['audit']['input']['labelId'],'auditId':c['audit']['id']})
for policy in ['legacy-flow','renewing-flow-131']:
 runs=[c for c in study['comparisons'] if c['policy']==policy];scores=[s for c in runs for s in c['audit']['scores']]
 values=[s for s in scores if 'filteredAnchorErrorPx' in s];raw=[s for s in scores if 'rawAnchorErrorPx' in s];projected=[s for s in scores if 'projectedContactErrorM' in s]
 total={'labels':len(scores),'observableContacts':sum(s['contact']=='observed' for s in scores),'contactAvailable':len(values),'withinLabelRadius':sum(s.get('withinLabelRadius',False) for s in scores),'rawAnchorMeanErrorPx':statistics.mean(s['rawAnchorErrorPx'] for s in raw) if raw else None,'filteredAnchorMeanErrorPx':statistics.mean(s['filteredAnchorErrorPx'] for s in values) if values else None,'projectedContactMeanErrorM':statistics.mean(s['projectedContactErrorM'] for s in projected) if projected else None,'unsupportedHiddenMeasurements':sum(s['unsupportedHiddenMeasurement'] for s in scores),'trackingMs':sum(c['trackingMs'] for c in runs if c['trackingMs'] is not None),'timedTrackingRuns':sum(c['trackingMs'] is not None for c in runs),'auditMs':sum(c['auditMs'] for c in runs)}
 total['contactMissing']=total['observableContacts']-total['contactAvailable'];comparison['summary'][policy]=total
# Paired scoring is descriptive; it cannot erase missing measurements.
a,b=[[c for c in study['comparisons'] if c['policy']==p] for p in ['legacy-flow','renewing-flow-131']]
paired=[]
for x,y in zip(a,b):
 assert x['window']==y['window']
 for xs,ys in zip(x['audit']['scores'],y['audit']['scores']):
  assert xs['ordinal']==ys['ordinal']
  if 'filteredAnchorErrorPx' in xs and 'filteredAnchorErrorPx' in ys:paired.append({'window':x['window'],'ordinal':xs['ordinal'],'defaultPx':xs['filteredAnchorErrorPx'],'candidatePx':ys['filteredAnchorErrorPx']})
comparison['paired']={'count':len(paired),'defaultMeanPx':statistics.mean(x['defaultPx'] for x in paired) if paired else None,'candidateMeanPx':statistics.mean(x['candidatePx'] for x in paired) if paired else None,'rows':paired}
write(out/'comparison.json',comparison)
v={'passed':True,'javaChecks':sorted(checks),'desktopChecks':desktop_count,'actualAppChecks':study['checks'],'production':study['source'],'buildReceipt':study['buildReceipt'],'finalVerifier':read('build/verifier132-final.json'),'newModelCalls':0,'defaultsUnchanged':True,'scope':'Local regression and actual app/source-label diagnostics. Retrospective AI annotations are not blinded human truth or new trajectories. No tracker promotion or calibration. Hosted CI remains separate.'};write(out/'verification.json',v)
originals=[];seen=set()
def preserve(p,logical):
 if not p.is_file() or logical in seen:return
 seen.add(logical);raw=p.read_bytes();key=sha(raw);dest=out/'originals'/(key+'.gz');packed=gzip.compress(raw,mtime=0);dest.parent.mkdir(exist_ok=True)
 if not dest.exists():dest.write_bytes(packed)
 originals.append({'logicalPath':logical,'path':dest.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(raw),'rawSha256':key})
for pattern in ['labels132-app*','agency132-evidence','validation132']:
 for folder in (r/'build').glob(pattern):
  if not folder.is_dir():continue
  for p in sorted(folder.rglob('*')):
   if any(x in p.parts for x in ['desktop-profile','node_modules']) or p.name.endswith('.fobs.json'):continue
   if p.is_file() and p.suffix in ['.json','.jsonl','.xml','.png','.txt','.log','.html','.css','.js','.cjs','.fworld','.previous','.csv','.evidence','.pending','.before','.java','.class']:preserve(p,p.relative_to(r).as_posix())
for p in sorted((r/'build').glob('*132*')):
 if p.is_file():preserve(p,p.relative_to(r).as_posix())
for name in ['docs/OBSERVATION_132_PROTOCOL.json','docs/publication132/source-labels.json']:preserve(r/name,name)
write(out/'original-evidence.json',{'files':originals,'scope':'Original labels, complete run traces, immutable audit results, real app sources and test output. Raw video, source-only inspection sheets, private data and browser profiles excluded; limited app screenshots attributed.'})
paths={x['path'] for x in read('docs/readmes/131/file-inventory.json')}
for n in subprocess.check_output(['git','diff','--name-only','434616ec','HEAD'],text=True).splitlines():
 if n.startswith(('workspace-engine/','workspace-desktop/','src/','assets/','.github/')) or n=='build.gradle':paths.add(n)
paths.update(['docs/EDITION_132_CONTRACT.md','docs/OBSERVATION_132_PROTOCOL.json','docs/publication132/source-labels.json']);inventory=[]
for n in sorted(paths):
 raw=(r/n).read_bytes()
 try:raw=raw.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':n,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
write(out/'file-inventory.json',inventory)
validation=f"Local checks passed: {len(checks)} distinct Java methods across the broad baseline and affected final-verifier suites, {desktop_count} desktop checks, executable kernel harnesses, actual agency regression and isolated offline/package preparation. The actual app independently checks label arithmetic, source identities, export/import and cold restart. No existing verification condition was weakened."
table='| Method | Available / visible contacts | Missing | Within 3 px / visible contacts | Raw mean error / px | Filtered mean error / px | Projected mean / m |\n| --- | ---: | ---: | ---: | ---: | ---: | ---: |\n'
for p,s in comparison['summary'].items():table+=f"| {p} | {s['contactAvailable']} / {s['observableContacts']} | {s['contactMissing']} | {s['withinLabelRadius']} / {s['observableContacts']} | {s['rawAnchorMeanErrorPx']:.3f} | {s['filteredAnchorMeanErrorPx']:.3f} | {s['projectedContactMeanErrorM']:.3f} |\n"
pair=comparison['paired'];finding=f"On the {pair['count']} contact samples measured by both methods, mean filtered contact error is {pair['defaultMeanPx']:.3f} px for default and {pair['candidateMeanPx']:.3f} px for the candidate. This paired subset is descriptive, not qualification. The labels expose contact-anchor disagreement separately from missing tracks; they do not fix either error. Physical accuracy remains unresolved."
cost='| Method | Tracking timed runs / ms | Verified UI comparisons / ms |\n| --- | ---: | ---: |\n'
for p,s in comparison['summary'].items():cost+=f"| {p} | {s['trackingMs']:.1f} ({s['timedTrackingRuns']}/5 timed) | {s['auditMs']:.1f} |\n"
attempts=(r/'build/attempts132.md').read_text(encoding='utf8') if (r/'build/attempts132.md').exists() else 'The initial focused Java checks and final regression pass are retained alongside the completed application study. No failed study is discarded or relabelled as a success.'
md=(r/'docs/publication132/README-template.md').read_text(encoding='utf8')
for k,value in {'RESULTS':table,'FINDING':finding,'COST':cost,'VALIDATION':validation,'APP_CHECKS':'\n'.join('- '+x for x in study['checks']),'ATTEMPTS':attempts}.items():md=md.replace('{{'+k+'}}',value)
assert '{{' not in md;text(r/'README-132.md',md);text(r/'README.md',md)
note='docs/obsidian/140-independent-contact-labels.md'
modules=[{'id':i,'name':n,'source':s,'note':note} for i,n,s in [('labels','Java source labels and immutable comparison','workspace-engine/src/main/java/fraymus/workspace/ObservationLabels132.java'),('panel','Source-only annotation controls','workspace-desktop/renderer/labels132-panel.js'),('tests','Contradictions, restart and source refusal','workspace-engine/src/test/java/fraymus/workspace/ObservationLabels132Test.java'),('study','Actual app and independent arithmetic','workspace-desktop/test/labels132-integration.cjs')]]
system={'modules':modules,'screenshots':[{'path':'docs/readmes/132/'+name,'title':name.removesuffix('.png').replace('-',' ')} for name in study['captures']],'evidence':[{'path':'docs/readmes/132/'+n} for n in ['verification.json','app-study.json','original-evidence.json','comparison.json']],'comparison':comparison,'validation':validation};write(out/'system-map.json',system)
text(r/note,'# Independent contact labels — edition132\n\n[[README-132.md]]\n\n'+validation+'\n\n'+finding+'\n\n25 fixed retrospective AI-reviewed source labels,22 visible contacts,3 unobservable. Fully hidden/out-of-view target rates remain unevaluated in real footage. Frozen source/image/clock/code/outcome checks and unchanged defaults. Next: human adjudication, independent trajectories, contact estimator and fixed-horizon uncertainty; preserve [[docs/INTEGRATED_AGENT_ROADMAP.md]].\n')
write(r/'docs/obsidian/140-independent-contact-labels.canvas',{'nodes':[{'id':x['id'],'type':'file','file':x['source'],'x':i%2*430,'y':i//2*250,'width':390,'height':190} for i,x in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']} for i in range(len(modules)-1)]})
html=(r/'docs/publication132/guide-template.html').read_text(encoding='utf8')+'<script id="system-data" type="application/json">'+json.dumps(system).replace('</','<\\/')+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory).replace('</','<\\/')+'</script><script src="guide.js"></script></body></html>'
text(out/'index.html',html);shutil.copy2(r/'docs/publication132/guide.js',out/'guide.js')
for name,link in [('docs/readmes/INDEX.md','[Edition132 — independent visibility and contact labels](../../README-132.md) | [Offline guide](132/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/140-independent-contact-labels.md]] | [[README-132.md]]')]:
 p=r/name;s=p.read_text(encoding='utf-8-sig');text(p,link+'\n\n'+s if not s.startswith(link) else s)
print(json.dumps({'passed':True,'javaChecks':len(checks),'sources':len(inventory),'originalEntries':len(originals),'comparison':comparison['summary']}))
