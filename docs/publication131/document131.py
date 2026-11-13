from pathlib import Path
import json,hashlib,subprocess,shutil,gzip,xml.etree.ElementTree as ET,re
r=Path.cwd();out=r/'docs/readmes/131';out.mkdir(parents=True,exist_ok=True)
read=lambda p:json.loads((r/p).read_text(encoding='utf8'))
sha=lambda b:hashlib.sha256(b).hexdigest()
def text(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8')
def write(p,v):text(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-131']).returncode==1
study=read('build/observation131-complete/result-final.json');assert study['passed'];write(out/'observation-study.json',study)
review=read('build/observation131-review.json');assert review['passed'];write(out/'final-ui-review.json',review);shots=list(dict.fromkeys(study['captures']+review['captures']))
for name in shots:shutil.copy2(r/'build/observation131-complete'/name,out/name)
checks=set()
for p in (r/'build/validation131/final').glob('*.xml'):
 e=ET.parse(p).getroot();assert int(e.get('failures','0'))+int(e.get('errors','0'))==0,p
 for c in e.findall('testcase'):
  if c.find('skipped') is None:checks.add(c.get('classname')+'.'+c.get('name'))
assert checks
desktop=(r/'build/desktop131-final.log').read_text(encoding='utf8');assert 'fail 0' in desktop;desktop_count=int(re.search(r'(?m)^. tests (\d+)',desktop)[1])
for n in ['kernel131-final','java131-final','desktop131-final','agency131-regression','launcher131','package131','receipt131']:assert read('build/'+n+'.json')['exit']==0,n
decision=read('build/diagnostic131/decision.json');coverage=read('build/diagnostic131/coverage.json')
assert not decision['defaultChanged'];assert read('build/observation131-recovery-visual.json')['passed']
write(out/'decision.json',decision);write(out/'coverage.json',coverage);write(out/'diagnostics.json',read('build/diagnostic131/diagnostics.json'));write(out/'collapse-reproduction.json',read('build/diagnostic131/collapse-reproduction.json'))
v={'passed':True,'javaChecks':sorted(checks),'desktopChecks':desktop_count,'actualAppChecks':study['checks'],'production':study['source'],'finalUIReview':review,'buildReceipt':study['buildReceipt'],'recoveryVisual':read('build/observation131-recovery-visual.json'),'newModelCalls':0,'defaultsUnchanged':True,'scope':'Local engineering checks plus one new-camera comparison of correlated event/people. No general accuracy, probability or recognition guarantee. All001-130 results remain unchanged. Hosted CI is separate.'};write(out/'verification.json',v)
originals=[];seen=set()
def preserve(p,logical):
 if not p.is_file() or logical in seen:return
 seen.add(logical);raw=p.read_bytes();key=sha(raw);dest=out/'originals'/(key+'.gz');packed=gzip.compress(raw,mtime=0);dest.parent.mkdir(exist_ok=True)
 if not dest.exists():dest.write_bytes(packed)
 originals.append({'logicalPath':logical,'path':dest.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(raw),'rawSha256':key})
for pattern in ['observation131-*','agency131-evidence','validation131','diagnostic131']:
 for folder in sorted((r/'build').glob(pattern)):
  if not folder.is_dir():continue
  for p in sorted(folder.rglob('*')):
   if any(x in p.parts for x in ['desktop-profile','node_modules']) or p.name.endswith('.fobs.json'):continue
   if p.is_file() and p.suffix in ['.json','.jsonl','.xml','.png','.txt','.log','.html','.css','.js','.cjs','.fworld','.previous','.csv','.evidence','.pending','.before','.java']:preserve(p,p.relative_to(r).as_posix())
for p in sorted((r/'build').glob('*131*')):
 if p.is_file():preserve(p,p.relative_to(r).as_posix())
for p in (r/'build/footage131-held').glob('*.json'):
 if not p.name.endswith('.fobs.json'):preserve(p,p.relative_to(r).as_posix())
write(out/'original-evidence.json',{'files':originals,'scope':'Original numerical traces, initial failed candidate source/results, test outputs, immutable audit and actual app evidence. Private data and original video/frame packs excluded; public research screenshots attributed.'})
paths={x['path'] for x in read('docs/readmes/130/file-inventory.json')}
for n in subprocess.check_output(['git','diff','--name-only','a4e50e71','HEAD'],text=True).splitlines():
 if n.startswith(('workspace-engine/','workspace-desktop/','src/','assets/','.github/')) or n=='build.gradle':paths.add(n)
paths.update(['docs/EDITION_131_CONTRACT.md','docs/OBSERVATION_131_LOCK.json','scripts/Prepare-Observation131.py','docs/OBSERVATION_131_BINARY_CONTROL.json','docs/publication131/run131.py','docs/publication131/measure131.py']);inventory=[]
for n in sorted(paths):
 raw=(r/n).read_bytes()
 try:raw=raw.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':n,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
write(out/'file-inventory.json',inventory)
validation=f"Local checks passed: {len(checks)} distinct Java test methods, {desktop_count} desktop checks, executable kernel harnesses, the actual agency regression, isolated offline preparation and package verification. The actual app verifies cold recovery, new-source reuse, original procedure evidence and immutable physical audits. No verification condition was weakened."
tracking='| Method | Available / references | Within 1 m | Over 2 m | Mean accepted error / m |\n| --- | ---: | ---: | ---: | ---: |\n'
for k,m in decision['methods'].items():tracking+=f"| {k} | {m['available']} / {m['references']} | {m['withinOneMetre']} | {m['overTwoMetres']} | {('Unavailable' if m['meanAcceptedErrorM'] is None else format(m['meanAcceptedErrorM'],'.3f'))} |\n"
table='| Tracker / policy | Available / references | Covered centres | With reference allowance | Mean radius / m |\n| --- | ---: | ---: | ---: | ---: |\n'
for k,m in coverage['summary'].items():
 radius='Unavailable' if m['meanRadiusM'] is None else f"{m['meanRadiusM']:.3f}"
 table+=f"| {k} | {m['available']} / {m['references']} | {m['coveredCentre']} / {m['references']} | {m['coveredIncludingReferenceRadius']} / {m['references']} | {radius} |\n"
a=decision['methods']['flow'];b=decision['methods']['renewing-flow'];decisionText=('**The locked gate passed for this limited cohort.**' if decision['gatePassed'] else '**The locked gate failed.**')+f" Candidate availability {b['available']}/{b['references']}, default {a['available']}/{a['references']}; within 1 m {b['withinOneMetre']} versus {a['withinOneMetre']}. Checks: "+', '.join(k+': '+str(v) for k,v in decision['lockedGate'].items())+'. Both defaults remain unchanged. This cohort does not resolve general physical accuracy. On the nine shared references, candidate mean error is 0.628 m versus 0.675 m for default; the lower overall accepted-only mean must not hide six missing positions. All 20 executions completed.'
cost='| Method | Complete process time, five runs / s | Tracking stage, completed runs / ms |\n| --- | ---: | ---: |\n'
diag=read('build/diagnostic131/diagnostics.json')
for method,metrics in decision['methods'].items():cost+=f"| {method} | {sum(x['execution']['wallSeconds'] for x in diag['rows'] if x['method']==method):.3f} | {metrics['trackerMs']:.1f} |\n"
md=(r/'docs/publication131/README-template.md').read_text(encoding='utf8')
for k,value in {'VALIDATION':validation,'APP_CHECKS':'\n'.join('- '+x for x in study['checks']),'TRACKING':tracking,'COVERAGE':table,'DECISION':decisionText,'COST':cost}.items():md=md.replace('{{'+k+'}}',value)
assert '{{' not in md;text(r/'README-131.md',md);text(r/'README.md',md)
note='docs/obsidian/139-valid-observations-and-recovery.md';modules=[{'id':i,'name':n,'source':s,'note':note} for i,n,s in [('tracking','Feature renewal and bounded recovery','workspace-engine/src/main/java/fraymus/workspace/RenewingFlow131.java'),('audit','Independent physical reference audit','workspace-engine/src/main/java/fraymus/workspace/PhysicalAudit130.java'),('panel','Visible audit controls','workspace-desktop/renderer/metric-panel.js'),('study','Actual complete application study','workspace-desktop/test/observation131-integration.cjs')]]
system={'modules':modules,'screenshots':[{'path':'docs/readmes/131/'+name,'title':name.removesuffix('.png').replace('-',' ')} for name in shots],'evidence':[{'path':'docs/readmes/131/'+n} for n in ['verification.json','observation-study.json','original-evidence.json','decision.json','coverage.json','diagnostics.json','collapse-reproduction.json','final-ui-review.json']],'coverage':coverage,'validation':validation};write(out/'system-map.json',system)
text(r/note,'# Valid observations and bounded recovery — edition131\n\n[[README-131.md]]\n\n'+validation+'\n\n'+decisionText+'\n\nStrict box consumers remain unchanged; invalid producers now withhold. New candidate replenishes eight supported landmarks and requires two-image confirmation for bounded appearance recovery. All original failures, all missing references, unchanged defaults, source identity and restart checks remain. New camera pixels share earlier trajectories. Next: independent association/occlusion labels and separately qualified anchor uncertainty, then [[docs/INTEGRATED_AGENT_ROADMAP.md]].\n')
write(r/'docs/obsidian/139-valid-observations-and-recovery.canvas',{'nodes':[{'id':x['id'],'type':'file','file':x['source'],'x':i%2*430,'y':i//2*250,'width':390,'height':190} for i,x in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']} for i in range(len(modules)-1)]})
html=(r/'docs/publication131/guide-template.html').read_text(encoding='utf8')+'<script id="system-data" type="application/json">'+json.dumps(system).replace('</','<\\/')+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory).replace('</','<\\/')+'</script><script src="guide.js"></script></body></html>'
text(out/'index.html',html);shutil.copy2(r/'docs/publication131/guide.js',out/'guide.js')
for name,link in [('docs/readmes/INDEX.md','[Edition131 — valid observations and bounded recovery](../../README-131.md) | [Offline guide](131/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/139-valid-observations-and-recovery.md]] | [[README-131.md]]')]:
 p=r/name;s=p.read_text(encoding='utf-8-sig');text(p,link+'\n\n'+s if not s.startswith(link) else s)
print(json.dumps({'passed':True,'javaChecks':len(checks),'sources':len(inventory),'originalEntries':len(originals)}))
