from pathlib import Path
import json,hashlib,subprocess,shutil,gzip,xml.etree.ElementTree as ET,re
r=Path.cwd();out=r/'docs/readmes/130';out.mkdir(parents=True,exist_ok=True)
read=lambda p:json.loads((r/p).read_text(encoding='utf8'))
sha=lambda b:hashlib.sha256(b).hexdigest()
def text(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8')
def write(p,v):text(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-130']).returncode==1
study=read('build/observation130-complete/result.json');assert study['passed'];write(out/'observation-study.json',study)
shots=list(dict.fromkeys(study['captures']))
for name in shots:shutil.copy2(r/'build/observation130-complete'/name,out/name)
checks=set()
for p in (r/'build/validation130/final').glob('*.xml'):
 e=ET.parse(p).getroot();assert int(e.get('failures','0'))+int(e.get('errors','0'))==0,p
 for c in e.findall('testcase'):
  if c.find('skipped') is None:checks.add(c.get('classname')+'.'+c.get('name'))
assert checks
desktop=(r/'build/desktop130-final.log').read_text(encoding='utf8');assert 'fail 0' in desktop;desktop_count=int(re.search(r'(?m)^. tests (\d+)',desktop)[1])
for n in ['kernel130-final','java130-final','desktop130-final','agency130-regression','launcher130','package130','receipt130']:assert read('build/'+n+'.json')['exit']==0,n
decision=read('build/diagnostic130/decision.json');coverage=read('build/diagnostic130/coverage.json');assert decision['gatePassed'] and not decision['defaultChanged']
write(out/'decision.json',decision);write(out/'coverage.json',coverage)
v={'passed':True,'javaChecks':sorted(checks),'desktopChecks':desktop_count,'actualAppChecks':study['checks'],'production':study['source'],'buildReceipt':study['buildReceipt'],'newModelCalls':0,'defaultsUnchanged':True,'scope':'Local engineering checks plus one correlated fixed-camera temporal evaluation. No general accuracy, probability or recognition guarantee. All129 results remain unchanged. Hosted CI is separate.'};write(out/'verification.json',v)
originals=[];seen=set()
def preserve(p,logical):
 if not p.is_file() or logical in seen:return
 seen.add(logical);raw=p.read_bytes();key=sha(raw);dest=out/'originals'/(key+'.gz');packed=gzip.compress(raw,mtime=0);dest.parent.mkdir(exist_ok=True)
 if not dest.exists():dest.write_bytes(packed)
 originals.append({'logicalPath':logical,'path':dest.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(raw),'rawSha256':key})
for pattern in ['observation130-*','agency130-evidence','validation130','diagnostic130']:
 for folder in sorted((r/'build').glob(pattern)):
  if not folder.is_dir():continue
  for p in sorted(folder.rglob('*')):
   if any(x in p.parts for x in ['desktop-profile','node_modules']) or p.name.endswith('.fobs.json'):continue
   if p.is_file() and p.suffix in ['.json','.jsonl','.xml','.png','.txt','.log','.html','.css','.js','.cjs','.fworld','.previous','.csv','.evidence','.pending','.before','.java']:preserve(p,p.relative_to(r).as_posix())
for p in sorted((r/'build').glob('*130*')):
 if p.is_file():preserve(p,p.relative_to(r).as_posix())
for p in (r/'build/footage130-held').glob('*.json'):
 if not p.name.endswith('.fobs.json'):preserve(p,p.relative_to(r).as_posix())
write(out/'original-evidence.json',{'files':originals,'scope':'Original numerical traces, initial failed candidate source/results, test outputs, immutable audit and actual app evidence. Private data and original video/frame packs excluded; public research screenshots attributed.'})
paths={x['path'] for x in read('docs/readmes/129/file-inventory.json')}
for n in subprocess.check_output(['git','diff','--name-only','18259fc8','HEAD'],text=True).splitlines():
 if n.startswith(('workspace-engine/','workspace-desktop/','src/','assets/','.github/')) or n=='build.gradle':paths.add(n)
paths.update(['docs/EDITION_130_CONTRACT.md','docs/OBSERVATION_130_LOCK.json','scripts/Prepare-Observation130.py']);inventory=[]
for n in sorted(paths):
 raw=(r/n).read_bytes()
 try:raw=raw.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':n,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
write(out/'file-inventory.json',inventory)
validation=f"Local checks passed: {len(checks)} distinct Java test methods, {desktop_count} desktop checks, executable kernel harnesses, the actual agency regression, isolated offline preparation and package verification. The actual app verifies cold recovery, new-source reuse, original procedure evidence and immutable physical audits. No verification condition was weakened."
tracking='| Method | Available / references | Within 1 m | Over 2 m | Mean accepted error / m |\n| --- | ---: | ---: | ---: | ---: |\n'
for k,m in decision['methods'].items():tracking+=f"| {k} | {m['available']} / {m['references']} | {m['withinOneMetre']} | {m['overTwoMetres']} | {m['meanAcceptedErrorM']:.3f} |\n"
table='| Tracker / policy | Available / references | Covered centres | With reference allowance | Mean radius / m |\n| --- | ---: | ---: | ---: | ---: |\n'
for k,m in coverage['summary'].items():
 radius='Unavailable' if m['meanRadiusM'] is None else f"{m['meanRadiusM']:.3f}"
 table+=f"| {k} | {m['available']} / {m['references']} | {m['coveredCentre']} / {m['references']} | {m['coveredIncludingReferenceRadius']} / {m['references']} | {radius} |\n"
md=(r/'docs/publication130/README-template.md').read_text(encoding='utf8')
for k,value in {'VALIDATION':validation,'APP_CHECKS':'\n'.join('- '+x for x in study['checks']),'TRACKING':tracking,'COVERAGE':table}.items():md=md.replace('{{'+k+'}}',value)
assert '{{' not in md;text(r/'README-130.md',md);text(r/'README.md',md)
note='docs/obsidian/138-physical-reference-audits.md';modules=[{'id':i,'name':n,'source':s,'note':note} for i,n,s in [('tracking','Object-supported feature tracker','workspace-engine/src/main/java/fraymus/workspace/ObjectFlow130.java'),('audit','Independent physical reference audit','workspace-engine/src/main/java/fraymus/workspace/PhysicalAudit130.java'),('panel','Visible audit controls','workspace-desktop/renderer/metric-panel.js'),('study','Actual complete application study','workspace-desktop/test/observation130-integration.cjs')]]
system={'modules':modules,'screenshots':[{'path':'docs/readmes/130/'+name,'title':name.removesuffix('.png').replace('-',' ')} for name in shots],'evidence':[{'path':'docs/readmes/130/'+n} for n in ['verification.json','observation-study.json','original-evidence.json','decision.json','coverage.json']],'coverage':coverage,'validation':validation};write(out/'system-map.json',system)
text(r/note,'# Physical references and object-supported flow — edition130\n\n[[README-130.md]]\n\n'+validation+'\n\nThe candidate passes one locked temporal gate but loses2/15 references, versus1/15 for the incumbent. Both defaults stay intact. Original129 failures and coverage loss remain valid. Audit original sources and independent positions without fitting or treating predictions as measured points. Restart recomputes the original audit. Next: association/anchor uncertainty and a different camera, then [[docs/INTEGRATED_AGENT_ROADMAP.md]].\n')
write(r/'docs/obsidian/138-physical-reference-audits.canvas',{'nodes':[{'id':x['id'],'type':'file','file':x['source'],'x':i%2*430,'y':i//2*250,'width':390,'height':190} for i,x in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']} for i in range(len(modules)-1)]})
html=(r/'docs/publication130/guide-template.html').read_text(encoding='utf8')+'<script id="system-data" type="application/json">'+json.dumps(system).replace('</','<\\/')+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory).replace('</','<\\/')+'</script><script src="guide.js"></script></body></html>'
text(out/'index.html',html);shutil.copy2(r/'docs/publication130/guide.js',out/'guide.js')
for name,link in [('docs/readmes/INDEX.md','[Edition130 — physical reference audits](../../README-130.md) | [Offline guide](130/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/138-physical-reference-audits.md]] | [[README-130.md]]')]:
 p=r/name;s=p.read_text(encoding='utf-8-sig');text(p,link+'\n\n'+s if not s.startswith(link) else s)
print(json.dumps({'passed':True,'javaChecks':len(checks),'sources':len(inventory),'originalEntries':len(originals)}))
