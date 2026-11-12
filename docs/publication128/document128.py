from pathlib import Path
import json,hashlib,subprocess,shutil,gzip,xml.etree.ElementTree as ET,re
r=Path.cwd();out=r/'docs/readmes/128';out.mkdir(parents=True,exist_ok=True)
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-128']).returncode==1
read=lambda p:json.loads((r/p).read_text(encoding='utf8'))
def text(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8')
def write(p,v):text(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
sha=lambda b:hashlib.sha256(b).hexdigest()
study=read('build/observation128-complete/result.json');review=read('build/observation128-review.json');assert study['passed'] and review['passed']
write(out/'observation-study.json',study);write(out/'final-ui-review.json',review)
shots=[]
for name in study['captures']:
 shutil.copy2(r/'build/observation128-complete'/name,out/name);shots.append(name)
for name in review['captures']:
 shutil.copy2(r/'build/observation128-complete'/name,out/name)
 if name not in shots:shots.append(name)
checks=set();suites=[]
for folder in sorted((r/'build/validation128').iterdir()):
 if not folder.is_dir():continue
 count=0
 for p in folder.glob('*.xml'):
  e=ET.parse(p).getroot();assert int(e.get('failures','0'))+int(e.get('errors','0'))==0,p
  for c in e.findall('testcase'):
   if c.find('skipped') is None:checks.add(c.get('classname')+'.'+c.get('name'));count+=1
 suites.append({'name':folder.name,'executions':count})
assert any(x['name']=='final' for x in suites)
desktop=(r/'build/desktop128-final.log').read_text(encoding='utf8');assert 'fail 0' in desktop;desktop_count=int(re.search(r'(?m)^. tests (\d+)',desktop)[1])
for n in ['kernel128-final','java128-final','desktop128-final','agency128-regression','launcher128','package128','receipt128']:assert read('build/'+n+'.json')['exit']==0,n
measured=[]
for t in study['tasks']:
 scores=[x['errorM'] for x in t['referenceScores'] if x['errorM'] is not None]
 measured.append({'name':t['name'],'preparationMs':t['preparationMs'],'requestMs':t['elapsedMs'],'metrics':t['workflow']['metrics'],'independentReferenceSamples':len(scores),'meanTrackingReferenceErrorM':sum(scores)/len(scores) if scores else None,'resumes':t['task']['resumes'],'future':t['workflow']['forecast128']['future']})
measurements={'rows':measured,'rejectedSource':study['rejectedSource'],'scope':'Single measured task spans on this machine. First includes explicit review, crash, cold restart and UI checks; both include source/outcome rechecks. Preparation is tracked separately. No speedup estimate, repeated timing confidence or new unseen-footage claim.'};write(out/'measurements.json',measurements)
v={'passed':True,'javaChecks':sorted(checks),'javaSuites':suites,'desktopChecks':desktop_count,'actualAppChecks':study['checks'],'finalUI':review,'actualAbruptJavaInterruption':True,'freshSourceReuse':True,'newModelCalls':0,'sourceScopes':{'production':study['source'],'buildReceipt':study['buildReceipt'],'scope':'Complete final study executes final production. Earlier source-bound failed and completed development attempts remain separately retained.'},'scope':'Calibrated planar observed-motion explanation, causal bounded forecasts and independently verified working artifacts. Not new native actuation, semantic person recognition, model-weight learning or calibrated probabilistic forecasting.'};write(out/'verification.json',v)
originals=[];seen=set()
def preserve(p,logical):
 if not p.is_file() or logical in seen:return
 seen.add(logical);raw=p.read_bytes();key=sha(raw);dest=out/'originals'/(key+'.gz');packed=gzip.compress(raw,mtime=0);dest.parent.mkdir(exist_ok=True)
 if not dest.exists():dest.write_bytes(packed)
 originals.append({'logicalPath':logical,'path':dest.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(raw),'rawSha256':key})
for pattern in ['observation128-*','agency128-evidence','validation128']:
 for folder in sorted((r/'build').glob(pattern)):
  if not folder.is_dir():continue
  for p in sorted(folder.rglob('*')):
   if any(x in p.parts for x in ['desktop-profile','node_modules']):continue
   if p.is_file() and p.suffix in ['.json','.jsonl','.xml','.png','.txt','.log','.html','.css','.js','.cjs','.fworld','.previous','.csv','.evidence','.pending','.before']:preserve(p,p.relative_to(r).as_posix())
for p in sorted((r/'build').glob('*128*')):
 if p.is_file():preserve(p,p.relative_to(r).as_posix())
for folder in ['footage128-dense','footage128-96']:
 for p in (r/'build'/folder).glob('*.json'):
  if not p.name.endswith('.fobs.json'):preserve(p,p.relative_to(r).as_posix())
write(out/'original-evidence.json',{'files':originals,'scope':'Exact new-project numerical source/tracker/metric/world records, workflow checkpoints, report originals, real DOM receipts, test failures and final reviews. Private user projects, browser profiles and original public video/frame bytes excluded. Source pixels remain local with hashes; limited screenshots attributed to EPFL.'})
paths={x['path'] for x in read('docs/readmes/127/file-inventory.json')}
for n in subprocess.check_output(['git','diff','--name-only','86886289','HEAD'],text=True).splitlines():
 if n.startswith(('workspace-engine/','workspace-desktop/','src/','assets/','.github/')) or n=='build.gradle':paths.add(n)
paths.update(['docs/EDITION_128_CONTRACT.md','docs/INTEGRATED_AGENT_ROADMAP.md']);inventory=[]
for n in sorted(paths):
 raw=(r/n).read_bytes()
 try:raw=raw.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':n,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
write(out/'file-inventory.json',inventory)
validation=f"Local validation passed: {len(checks)} distinct Java test methods, {desktop_count} desktop checks, executable factory-kernel harnesses, the actual agency regression and isolated offline preparation/package checks. The actual study includes an abrupt owned-Java interruption, cold restart of the same request, changed-input reuse, retained procedure lifecycle and independent numeric/rendered outcomes. Hosted CI was checked first and is reported separately."
results='\n'.join('- '+x for x in study['checks'])
timing='| Recorded input | Accepted / rows | Forecast comparisons | Mean forecast error / m | Request / seconds | Tracking preparation / seconds |\n| --- | ---: | ---: | ---: | ---: | ---: |\n'
for row in measured:
 m=row['metrics'];timing+=f"| {row['name']} | {m['measured']} / {m['rows']} | {m['compared']} | {m['meanErrorM']:.6f} | {row['requestMs']/1000:.2f} | {row['preparationMs']/1000:.2f} |\n"
timing+='\n| Independent reference check | Scored positions | Mean tracking error / m | Future radius at 0.6 s / m |\n| --- | ---: | ---: | ---: |\n'
for row in measured:
 timing+=f"| {row['name']} | {row['independentReferenceSamples']} | {row['meanTrackingReferenceErrorM']:.3f} | {row['future']['radiusM']:.3f} |\n"
timing+='\nThe first request includes the explicit review and crash/restart interval. These are single observed costs, not typical latency claims.\n'
failures='The first sparse source yielded zero accepted measurements and was refused before creating a workflow or procedure. A 121-frame dense input was rejected by the unchanged 96-frame import limit; explicitly prepared 96-frame inputs respect that limit. The third original seed lies outside the image and remains refused, without clipping or inventing a target. A rendered audit exposed SVG 32-bit coordinate representation; the verifier now compares against that defined representation while retaining the strict geometry checks. A later chat completion refused a missing artifact-test receipt; the independent host now retains its actual checks in the existing receipt store. Both failures remain preserved. Test-driver timing, GET/POST and manual-selection-during-ownership mistakes are also retained. A thread diagnostic exposed repeated metadata loading per image; final verification reads one pinned metadata snapshot, hashes every original image and rechecks the snapshot afterward. The final complete study reruns on that final source; earlier attempts are not relabelled.'
md=(r/'docs/publication128/README-template.md').read_text(encoding='utf8')
for k,value in {'VALIDATION':validation,'RESULTS':results,'TIMINGS':timing,'FAILURES':failures}.items():md=md.replace('{{'+k+'}}',value)
assert '{{' not in md;text(r/'README-128.md',md);text(r/'README.md',md)
note='docs/obsidian/136-observation-artifact.md';modules=[{'id':i,'name':n,'source':s,'note':note} for i,n,s in [('observation','Original observations and causal forecast','workspace-engine/src/main/java/fraymus/workspace/ObservationArtifact128.java'),('workflow','Durable workflow owner','workspace-engine/src/main/java/fraymus/workspace/WorkflowRunner.java'),('artifact','Working artifact and numeric verifier','workspace-engine/src/main/java/fraymus/workspace/ObservationArtifactReport128.java'),('host','Independent rendered observations','workspace-desktop/observation-artifact128.cjs'),('study','Actual app study','workspace-desktop/test/observation128-integration.cjs')]]
system={'modules':modules,'screenshots':[{'path':'docs/readmes/128/'+name,'title':name.removesuffix('.png').replace('-',' ')} for name in shots],'evidence':[{'path':'docs/readmes/128/'+n} for n in ['verification.json','observation-study.json','final-ui-review.json','measurements.json','original-evidence.json']],'measurements':measurements,'validation':validation};write(out/'system-map.json',system)
text(r/note,'# Observation to a working artifact — edition128\n\n[[README-128.md]]\n\n'+validation+'\n\nOne Java-owned mission pins recorded observations to a stable body, computes bounded causal forecasts, creates a working explorer and checks actual source cells and rendered points. Recovery keeps source identity. Reuse needs fresh inputs and fresh checks. Tracking quality and software completion are distinct.\n\nNext: [[docs/INTEGRATED_AGENT_ROADMAP.md]] — evaluated adaptation and coherent daily use.\n')
write(r/'docs/obsidian/136-observation-artifact.canvas',{'nodes':[{'id':x['id'],'type':'file','file':x['source'],'x':i%3*430,'y':i//3*250,'width':390,'height':190} for i,x in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']} for i in range(len(modules)-1)]})
html=(r/'docs/publication128/guide-template.html').read_text(encoding='utf8')+'<script id="system-data" type="application/json">'+json.dumps(system).replace('</','<\\/')+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory).replace('</','<\\/')+'</script><script src="guide.js"></script></body></html>'
text(out/'index.html',html);shutil.copy2(r/'docs/publication128/guide.js',out/'guide.js')
for name,link in [('docs/readmes/INDEX.md','[Edition128 — observed motion to working artifact](../../README-128.md) | [Offline guide](128/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/136-observation-artifact.md]] | [[README-128.md]]')]:
 p=r/name;s=p.read_text(encoding='utf-8-sig');text(p,link+'\n\n'+s if not s.startswith(link) else s)
print(json.dumps({'passed':True,'javaChecks':len(checks),'sources':len(inventory),'originalEntries':len(originals)}))
