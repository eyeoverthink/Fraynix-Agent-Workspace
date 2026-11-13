from pathlib import Path
import json,hashlib,subprocess,shutil,gzip,xml.etree.ElementTree as ET,re
r=Path.cwd();out=r/'docs/readmes/129';out.mkdir(parents=True,exist_ok=True)
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-129']).returncode==1
read=lambda p:json.loads((r/p).read_text(encoding='utf8'))
def text(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8')
def write(p,v):text(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
sha=lambda b:hashlib.sha256(b).hexdigest()
study=read('build/observation129-release/result-after-first.json');review=read('build/observation129-review.json');assert study['passed'] and review['passed']
checksApp=list(dict.fromkeys(study.get('originalAttempt',{}).get('checks',[])+study['checks']))
write(out/'observation-study.json',study);write(out/'final-ui-review.json',review)
shots=[]
for name in study['captures']:
 shutil.copy2(r/'build/observation129-release'/name,out/name);shots.append(name)
for name in review['captures']:
 shutil.copy2(r/'build/observation129-release'/name,out/name)
 if name not in shots:shots.append(name)
checks=set();suites=[]
for folder in sorted((r/'build/validation129').iterdir()):
 if not folder.is_dir() or folder.name in ['first','prior-successful-packages']:continue
 count=0
 for p in folder.glob('*.xml'):
  e=ET.parse(p).getroot();assert int(e.get('failures','0'))+int(e.get('errors','0'))==0,p
  for c in e.findall('testcase'):
   if c.find('skipped') is None:checks.add(c.get('classname')+'.'+c.get('name'));count+=1
 suites.append({'name':folder.name,'executions':count})
assert any(x['name']=='final' for x in suites)
desktop=(r/'build/desktop129-final.log').read_text(encoding='utf8');assert 'fail 0' in desktop;desktop_count=int(re.search(r'(?m)^. tests (\d+)',desktop)[1])
for n in ['kernel129-final','java129-final','routing129-final','optin129-final','svg129-final','wording129-final','desktop129-final','agency129-regression','launcher129','package129','receipt129']:assert read('build/'+n+'.json')['exit']==0,n
measured=[]
for t in study['tasks']:
 scores=[x['errorM'] for x in t['referenceScores'] if x['errorM'] is not None]
 measured.append({'name':t['name'],'preparationMs':t['preparationMs'],'requestMs':t['elapsedMs'],'metrics':t['workflow']['metrics'],'independentReferenceSamples':len(scores),'meanTrackingReferenceErrorM':sum(scores)/len(scores) if scores else None,'resumes':t['task']['resumes'],'future':t['workflow']['forecast128']['future']})
measurements={'rows':measured,'trackingDecisions':read('build/diagnostic129/cohort-decisions.json'),'scope':'Single measured task spans on this machine. First includes explicit review, crash, cold restart and UI checks; both include source/outcome rechecks. Preparation is tracked separately. No speedup estimate, repeated timing confidence or new unseen-footage claim.'};write(out/'measurements.json',measurements)
v={'passed':True,'javaChecks':sorted(checks),'javaSuites':suites,'desktopChecks':desktop_count,'actualAppChecks':checksApp,'finalUI':review,'actualAbruptJavaInterruption':True,'freshSourceReuse':True,'newModelCalls':0,'sourceScopes':{'broadTests':'7d2172f6','routingFollowup':'e7f8b878','explicitOptinFollowup':'422e6fb5','svgRendererFollowup':'4e9cbb97','finalWording':'0a514d57','production':study['source'],'buildReceipt':study['buildReceipt'],'scope':'Complete final study executes final production. Earlier source-bound failed and completed development attempts remain separately retained.'},'scope':'Calibrated planar observed-motion explanation, causal bounded forecasts and independently verified working artifacts. Not new native actuation, semantic person recognition, model-weight learning or calibrated probabilistic forecasting.'};write(out/'verification.json',v)
originals=[];seen=set()
def preserve(p,logical):
 if not p.is_file() or logical in seen:return
 seen.add(logical);raw=p.read_bytes();key=sha(raw);dest=out/'originals'/(key+'.gz');packed=gzip.compress(raw,mtime=0);dest.parent.mkdir(exist_ok=True)
 if not dest.exists():dest.write_bytes(packed)
 originals.append({'logicalPath':logical,'path':dest.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(raw),'rawSha256':key})
for pattern in ['observation129-*','agency129-evidence','validation129','diagnostic129']:
 for folder in sorted((r/'build').glob(pattern)):
  if not folder.is_dir():continue
  for p in sorted(folder.rglob('*')):
   if any(x in p.parts for x in ['desktop-profile','node_modules']):continue
   if p.is_file() and p.suffix in ['.json','.jsonl','.xml','.png','.txt','.log','.html','.css','.js','.cjs','.fworld','.previous','.csv','.evidence','.pending','.before','.java']:preserve(p,p.relative_to(r).as_posix())
for p in sorted((r/'build').glob('*129*')):
 if p.is_file():preserve(p,p.relative_to(r).as_posix())
for folder in ['footage129-held','footage129-confirm']:
 for p in (r/'build'/folder).glob('*.json'):
  if not p.name.endswith('.fobs.json'):preserve(p,p.relative_to(r).as_posix())
write(out/'original-evidence.json',{'files':originals,'scope':'Exact new-project numerical source/tracker/metric/world records, workflow checkpoints, report originals, real DOM receipts, test failures and final reviews. Private user projects, browser profiles and original public video/frame bytes excluded. Source pixels remain local with hashes; limited screenshots attributed to EPFL.'})
paths={x['path'] for x in read('docs/readmes/128/file-inventory.json')}
for n in subprocess.check_output(['git','diff','--name-only','4b941ac6','HEAD'],text=True).splitlines():
 if n.startswith(('workspace-engine/','workspace-desktop/','src/','assets/','.github/')) or n=='build.gradle':paths.add(n)
paths.update(['docs/EDITION_129_CONTRACT.md','docs/INTEGRATED_AGENT_ROADMAP.md','docs/OBSERVATION_129_CANDIDATE_LOCK.json','docs/OBSERVATION_129_CONFIRMATION_LOCK.json','scripts/Prepare-Observation129.py','scripts/Prepare-Observation129-confirm.py']);inventory=[]
for n in sorted(paths):
 raw=(r/n).read_bytes()
 try:raw=raw.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':n,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
write(out/'file-inventory.json',inventory)
validation=f"Local validation passed: {len(checks)} distinct Java test methods, {desktop_count} desktop checks, executable factory-kernel harnesses, the actual agency regression and isolated offline preparation/package checks. The actual study includes an abrupt owned-Java interruption, cold restart of the same request, changed-input reuse, retained procedure lifecycle and independent numeric/rendered outcomes. Hosted CI was checked first and is reported separately."
results='\n'.join('- '+x for x in checksApp)
timing='| Recorded input | Accepted / rows | Forecast comparisons | Mean residual vs tracker / m | Request / seconds | Tracking preparation / seconds |\n| --- | ---: | ---: | ---: | ---: | ---: |\n'
for row in measured:
 m=row['metrics'];timing+=f"| {row['name']} | {m['measured']} / {m['rows']} | {m['compared']} | {m['meanErrorM']:.6f} | {row['requestMs']/1000:.2f} | {row['preparationMs']/1000:.2f} |\n"
timing+='\n| Independent reference check | Scored positions | Mean tracking error / m | Best future radius, WITHHELD / m |\n| --- | ---: | ---: | ---: |\n'
for row in measured:
 timing+=f"| {row['name']} | {row['independentReferenceSamples']} | {row['meanTrackingReferenceErrorM']:.3f} | {row['future'].get('radiusM',row['future'].get('bestRadiusM',0)):.3f} |\n"
timing+='\nThe first request includes the explicit review and crash/restart interval. These are single observed costs, not typical latency claims.\n'
failures='Both predeclared tracking promotion gates failed; the original protocols and all 30 held method executions are retained. The first candidate lost nearly every observation in the foreground-occluded development case. The first focused test run had one failing test assertion because the public workflow view intentionally omits its definition; the corrected test reads the retained artifact definition, preserving the original failure. A source review also caught a chat helper that would discard initial precision settings; the corrected routing and new/reused-request checks are verified on final source. Independent forecast coverage also fell from28/30 to16/30 under narrower envelopes. This is not improved accuracy: ordinary requests retain the original policy, while the precision-limited mode requires an explicit request and remains experimental. The first actual app run recovered the same request but refused its chart: one SVG coordinate was parsed one float step away from the mathematically expected value. The renderer now writes explicit IEEE single-precision coordinates, preserving the existing strict verifier. A fresh source-bound complete study follows; the earlier unfinished task remains retained. A later driver tried selecting the experimental tracker while the artifact tab was visible; the corrected navigation continues the same project, rechecks the already completed first task and preserves its original timing. Visual review then clarified the chart and chat wording: residuals are against tracked estimates, not ground truth. A complete final-source run repeats both tasks after that presentation change; earlier successful and unsuccessful runs remain distinct. The final driver also waits for the actual host task lease to become idle before manual import, after an early import was correctly refused during cleanup. Continuation rechecks the completed first task without replacing its original timing. The final tests and actual study are reported separately. No failed attempt counts as success or is relabelled under a newer source.'
decisions=read('build/diagnostic129/cohort-decisions.json');write(out/'cohort-decisions.json',decisions)
comparison=read('build/diagnostic129/forecast-comparison.json');write(out/'forecast-comparison.json',comparison)
tracking='| Cohort / method | Accepted references /15 | Within 1 m | Over 2 m | Mean accepted error /m | Tracking /s |\n| --- | ---: | ---: | ---: | ---: | ---: |\n'
for c in decisions['cohorts']:
 for method,m in c['methods'].items():tracking+=f"| {c['cohort']} / {method} | {m['acceptedReferences']} | {m['withinOneMetre']} | {m['overTwoMetres']} | {m['meanAcceptedErrorM']:.3f} | {m['trackerMs']/1000:.3f} |\n"
forecast='| Policy across 10 incumbent windows | Available forecasts | Mean available radius /m | Independent covered / available | Mean independent error /m |\n| --- | ---: | ---: | ---: | ---: |\n'
for policy,m in comparison['summary'].items():forecast+=f"| {policy} | {m['available']} / {m['totalRows']} | {m['meanRadiusM']:.3f} | {m['referenceCovered']} / {m['referenceAvailable']} | {m['meanReferenceErrorM']:.3f} |\n"
md=(r/'docs/publication129/README-template.md').read_text(encoding='utf8')
for k,value in {'VALIDATION':validation,'RESULTS':results,'TIMINGS':timing,'FAILURES':failures,'TRACKING':tracking,'FORECAST':forecast}.items():md=md.replace('{{'+k+'}}',value)
assert '{{' not in md;text(r/'README-129.md',md);text(r/'README.md',md)
note='docs/obsidian/137-tracking-precision.md';modules=[{'id':i,'name':n,'source':s,'note':note} for i,n,s in [('observation','Precision-limited causal forecast','workspace-engine/src/main/java/fraymus/workspace/ObservationForecast129.java'),('workflow','Durable workflow owner','workspace-engine/src/main/java/fraymus/workspace/WorkflowRunner.java'),('artifact','Working artifact and numeric verifier','workspace-engine/src/main/java/fraymus/workspace/ObservationArtifactReport128.java'),('host','Independent rendered observations','workspace-desktop/observation-artifact128.cjs'),('study','Actual app study','workspace-desktop/test/observation129-integration.cjs')]]
system={'modules':modules,'screenshots':[{'path':'docs/readmes/129/'+name,'title':name.removesuffix('.png').replace('-',' ')} for name in shots],'evidence':[{'path':'docs/readmes/129/'+n} for n in ['verification.json','observation-study.json','final-ui-review.json','measurements.json','original-evidence.json','cohort-decisions.json','forecast-comparison.json']],'measurements':measurements,'validation':validation};write(out/'system-map.json',system)
text(r/note,'# Tracking evidence and forecast precision — edition129\n\n[[README-129.md]]\n\n'+validation+'\n\nOne Java-owned mission pins recorded observations to a stable body, computes bounded causal forecasts, creates a working explorer and checks actual source cells and rendered points. Recovery keeps source identity. Reuse needs fresh inputs and fresh checks. Tracking quality and software completion are distinct.\n\nThe unchanged tracking candidate failed both locked gates; keep the incumbent default. Forecast precision is conditional, not probability calibration. Next: [[docs/INTEGRATED_AGENT_ROADMAP.md]] — coherent daily use.\n')
write(r/'docs/obsidian/137-tracking-precision.canvas',{'nodes':[{'id':x['id'],'type':'file','file':x['source'],'x':i%3*430,'y':i//3*250,'width':390,'height':190} for i,x in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']} for i in range(len(modules)-1)]})
html=(r/'docs/publication129/guide-template.html').read_text(encoding='utf8')+'<script id="system-data" type="application/json">'+json.dumps(system).replace('</','<\\/')+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory).replace('</','<\\/')+'</script><script src="guide.js"></script></body></html>'
text(out/'index.html',html);shutil.copy2(r/'docs/publication129/guide.js',out/'guide.js')
for name,link in [('docs/readmes/INDEX.md','[Edition129 — measured tracking and precision](../../README-129.md) | [Offline guide](129/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/137-tracking-precision.md]] | [[README-129.md]]')]:
 p=r/name;s=p.read_text(encoding='utf-8-sig');text(p,link+'\n\n'+s if not s.startswith(link) else s)
print(json.dumps({'passed':True,'javaChecks':len(checks),'sources':len(inventory),'originalEntries':len(originals)}))
