from pathlib import Path
import json,hashlib,subprocess,shutil,gzip,xml.etree.ElementTree as ET,re
r=Path.cwd();out=r/'docs/readmes/139';out.mkdir(parents=True,exist_ok=True);parent='7e66b0c86d3a665b5793ba795298a4eeb2c81cfa'
read=lambda p:json.loads((r/p).read_text(encoding='utf-8-sig'));sha=lambda b:hashlib.sha256(b).hexdigest()
def text(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(s.encode('utf8'))
def write(p,v):text(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-139']).returncode==1
study=read('build/drift139-app/result.json');assert study['passed'];write(out/'app-study.json',study)
lock=read('docs/OBSERVATION_139_LOCK.json');sources=read('docs/publication139/source139.json')
for n,h in lock['sourceHashes'].items():assert sha((r/n).read_bytes().replace(b'\r\n',b'\n'))==h,n
checks=set()
for p in (r/'build/validation139/final').glob('*.xml'):
 e=ET.parse(p).getroot();assert int(e.get('failures','0'))+int(e.get('errors','0'))==0,p
 for c in e.findall('testcase'):
  if c.find('skipped') is None:checks.add(c.get('classname')+'.'+c.get('name'))
assert checks
desktop=(r/'build/desktop139-final.log').read_text(encoding='utf8');assert 'fail 0' in desktop;desktop_count=int(re.search(r'(?m)^. tests (\d+)',desktop)[1])
for n in ['kernel139-final','java139-final','desktop139-final','agency139-regression','launcher139','package139','receipt139']:assert read('build/'+n+'.json')['exit']==0,n
for name in study['captures']:shutil.copy2(r/'build/drift139-app'/name,out/name)
rows=[{'case':x['caseName'],'policy':x['policy'],'summary':x['audit']['summary'],'drift139':(x['audit'].get('drift139') or x['audit'].get('drift138')),'trackingMs':x['trackingMs'],'auditMs':x['auditMs']} for x in study['runs']]
sums={}
for policy in lock['methods']:
 selected=[x for x in rows if x['policy']==policy]
 sums[policy]={'tracking':{k:sum(x['summary'][k] for x in selected) for k in ['labels','visible','correct','missing','overlapFailures','hidden','unsupported','uncertain','confirmedDistractorMatches']},'warnings':{k:sum((x['drift139'] or {}).get('summary',{}).get(k,0) for x in selected) for k in ['warnings','usefulWarnings','falseWarnings','unknownWarnings','missedWrongProposals','hiddenProposals','confirmedRecoveries']}}
candidate=sums['foreground-guard-139']['tracking'];base=sums['legacy-flow']['tracking']
finding=f"Fresh guard: **{candidate['correct']}/{candidate['visible']} correct visible/partial observations**, versus **{base['correct']}/{base['visible']}** for the default. Accepted wrong overlaps: **{candidate['overlapFailures']} versus {base['overlapFailures']}**. Missing observations: **{candidate['missing']} versus {base['missing']}**. Confirmed labelled-other matches: **{candidate['confirmedDistractorMatches']} versus {base['confirmedDistractorMatches']}**. Hidden outputs: **{candidate['unsupported']}/{candidate['hidden']} versus {base['unsupported']}/{base['hidden']}**. These measured tradeoffs do not qualify a default promotion; the candidate remains explicit."
timing=[]
for x in study['runs']:
 if x['policy']!='monitor-only-139':continue
 scored=x['audit']['drift139']['scores']
 wrong=[a['ordinal'] for a in scored if a['visibility'] in ['visible','partial'] and a['proposalAccepted'] and a['proposalOverlap']<.5]
 warnings=[a['ordinal'] for a in scored if a['warning']]
 timing.append({'case':x['caseName'],'firstWrongVisibleProposal':wrong[0] if wrong else None,'firstWarning':warnings[0] if warnings else None,'scope':'Descriptive post-run timing, not an additional qualification score; original frame ordinal excludes initial seed.'})
comparison={'warningTiming':timing,'rows':rows,'summary':sums,'lock':lock,'sources':sources,'development':read('build/drift139-development/results.json'),'defaultsUnchanged':True,'promoted':False};write(out/'comparison.json',comparison)
validation=f"Local checks passed: {len(checks)} distinct Java tests, {desktop_count} desktop checks, kernel integration, actual agency regression and isolated offline/package preparation. Sixteen actual-app fresh comparisons use {sum(x['run']['totalFrames'] for x in study['runs'])} later-frame outcomes. Exact monitor equivalence, original-pixel audit replay, independent host arithmetic, export, stop/resume and abrupt-process cold restart are verified."
v={'passed':True,'javaChecks':sorted(checks),'desktopChecks':desktop_count,'actualAppChecks':list(dict.fromkeys(study['checks'])),'production':study['source'],'buildReceipt':study['buildReceipt'],'candidateLock':lock,'defaultsUnchanged':True,'scope':validation+' External VOT rectangles/absence with separate source-only AI visibility/cause review; no independent human adjudication of added labels. Hosted CI separate.'};write(out/'verification.json',v)
originals=[];seen=set()
def preserve(p,logical):
 if not p.is_file() or logical in seen:return
 seen.add(logical);raw=p.read_bytes();key=sha(raw);dest=out/'originals'/(key+'.gz');packed=gzip.compress(raw,mtime=0);dest.parent.mkdir(exist_ok=True)
 if not dest.exists():dest.write_bytes(packed)
 originals.append({'logicalPath':logical,'path':dest.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(raw),'rawSha256':key})
for name in ['drift139-app','drift139-development','agency139-evidence','validation139']:
 for p in sorted((r/'build'/name).rglob('*')):
  if any(x in p.parts for x in ['desktop-profile','node_modules','exports']) or p.name.endswith('.fobs.json'):continue
  if p.is_file() and p.suffix in ['.json','.jsonl','.xml','.png','.txt','.log','.html','.css','.js','.cjs','.fworld','.previous','.csv','.evidence','.pending','.before','.java','.class']:preserve(p,p.relative_to(r).as_posix())
for p in sorted((r/'build').glob('*139*')):
 if p.is_file() and p.suffix not in ['.zip','.jfr']:preserve(p,p.relative_to(r).as_posix())
for name in ['docs/OBSERVATION_139_LOCK.json','docs/publication139/source139.json','build/vot139/source-index.json','build/vot139/download.json']:preserve(r/name,name)
write(out/'original-evidence.json',{'files':originals,'scope':'Original run/audit records, development and fresh comparisons, tests, attempts, public app screenshots and startup/restart checks. Raw footage/images and private profiles excluded; exact source hashes/annotations/provenance retained.'})
paths={x['path'] for x in read('docs/readmes/138/file-inventory.json')}
for n in subprocess.check_output(['git','diff','--name-only',parent,'HEAD'],text=True).splitlines():
 if n.startswith(('workspace-engine/','workspace-desktop/','src/','assets/','.github/')) or n in ['build.gradle','.gitattributes']:paths.add(n)
paths.update(['docs/EDITION_139_CONTRACT.md','docs/EDITION_139_SOURCE_NOTE.md','docs/OBSERVATION_139_LOCK.json','docs/publication139/source139.json']);inventory=[]
for n in sorted(paths):
 raw=(r/n).read_bytes()
 try:raw=raw.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':n,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
write(out/'file-inventory.json',inventory)
table='| Method / source | Correct / visible | Missing | Wrong overlaps | Other-object matches | Hidden outputs |\n| --- | ---: | ---: | ---: | ---: | ---: |\n'
warn='| Method / source | Useful warnings | False warnings | Missed wrong proposals |\n| --- | ---: | ---: | ---: |\n'
cost='| Actual task | Tracking / ms | Audit with replay / ms |\n| --- | ---: | ---: |\n'
for x in rows:
 s=x['summary'];table+=f"| {x['policy']} / {x['case']} | {s['correct']}/{s['visible']} | {s['missing']} | {s['overlapFailures']} | {s['confirmedDistractorMatches']} | {s['unsupported']}/{s['hidden']} |\n"
 if x['drift139']:
  d=x['drift139']['summary'];warn+=f"| {x['policy']} / {x['case']} | {d['usefulWarnings']} | {d['falseWarnings']} | {d['missedWrongProposals']} |\n"
 cost+=f"| {x['policy']} / {x['case']} | {x['trackingMs']:.1f} | {x['auditMs']:.1f} |\n"
st='| Recording / target | Original frames | Stride | Visible or partial | Hidden | Uncertain | Distractor-labelled samples |\n| --- | --- | ---: | ---: | ---: | ---: | ---: |\n'
for s in sources['records']:
 c=s['visibilityCounts'];st+=f"| {s['name']} / {s['target']} | {s['start']}–{s['end']} | {s['stride']} | {c['visible']+c['partial']} | {c['occluded']+c['out-of-view']} | {c['uncertain']} | {s['distractorLabelledSamples']} |\n"
warn+='\nFirst wrong visible proposal / first warning (later-frame ordinals, monitor control): '+ '; '.join(f"{x['case']} {x['firstWrongVisibleProposal']} / {x['firstWarning']}" for x in timing)+'. This descriptive timing is not advance prediction of drift; warnings can arrive after an error already exists.\n'
next_steps=(r/'build/next139.md').read_text(encoding='utf8')
md=(r/'docs/publication139/README-template.md').read_text(encoding='utf8')
for key,value in {'FINDING':finding,'RESULTS':table,'WARNINGS':warn,'SOURCES':st,'COSTS':cost,'VALIDATION':validation,'APP_CHECKS':'\n'.join('- '+x for x in v['actualAppChecks']),'SOURCE_REVIEW':sources['sourceReview'],'ATTEMPTS':(r/'build/attempts139.md').read_text(encoding='utf8'),'NEXT':next_steps}.items():md=md.replace('{{'+key+'}}',value)
assert '{{' not in md;text(r/'README-139.md',md);text(r/'README.md',md)
note='docs/obsidian/147-foreground-structure.md';modules=[{'id':i,'name':n,'source':s,'note':note} for i,n,s in [('appearance','Immutable spatial parts','workspace-engine/src/main/java/fraymus/workspace/SpatialEvidence139.java'),('guard','Foreground guard and monitor','workspace-engine/src/main/java/fraymus/workspace/ForegroundGuard139.java'),('audit','Original-pixel replay and outcome audit','workspace-engine/src/main/java/fraymus/workspace/ForegroundAudit139.java'),('world','Durable observed world','workspace-engine/src/main/java/fraymus/workspace/WorldObservationStore.java'),('panel','Live drift evidence','workspace-desktop/renderer/observation-panel.js')]]
system={'modules':modules,'screenshots':[{'path':'docs/readmes/139/'+name,'title':name.removesuffix('.png').replace('-',' ')} for name in study['captures']],'evidence':[{'path':'docs/readmes/139/'+n} for n in ['verification.json','app-study.json','original-evidence.json','comparison.json']],'comparison':comparison,'validation':validation};write(out/'system-map.json',system)
text(r/note,'# Foreground motion and spatial structure — edition139\n\n[[README-139.md]]\n\n'+validation+'\n\n'+finding+'\n\n'+next_steps+'\n\nPreserve defaults, historical bytes and source/outcome checks. Continue [[docs/INTEGRATED_AGENT_ROADMAP.md]].\n')
write(r/'docs/obsidian/147-foreground-structure.canvas',{'nodes':[{'id':x['id'],'type':'file','file':x['source'],'x':i*430,'y':0,'width':390,'height':190} for i,x in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']} for i in range(len(modules)-1)]})
html=(r/'docs/publication139/guide-template.html').read_text(encoding='utf8')+'<script id="system-data" type="application/json">'+json.dumps(system).replace('</','<\\/')+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory).replace('</','<\\/')+'</script><script src="guide.js"></script></body></html>'
text(out/'index.html',html);shutil.copy2(r/'docs/publication137/guide.js',out/'guide.js')
for name,link in [('docs/readmes/INDEX.md','[Edition139 — foreground motion and spatial structure](../../README-139.md) | [Offline guide](139/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/147-foreground-structure.md]] | [[README-139.md]]')]:
 p=r/name;s=p.read_text(encoding='utf-8-sig');text(p,link+'\n\n'+s if not s.startswith(link) else s)
print(json.dumps({'passed':True,'javaChecks':len(checks),'sources':len(inventory),'originalEntries':len(originals),'finding':finding}))
