from pathlib import Path
import json,hashlib,subprocess,shutil,gzip,xml.etree.ElementTree as ET,re
r=Path.cwd();out=r/'docs/readmes/136';out.mkdir(parents=True,exist_ok=True);parent='2b68dd343bfbc179b71ca4c8ece14bb0f42bb356'
read=lambda p:json.loads((r/p).read_text(encoding='utf-8-sig'));sha=lambda b:hashlib.sha256(b).hexdigest()
def text(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(s.encode('utf8'))
def write(p,v):text(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-136']).returncode==1
study=read('build/appearance136-app/result.json');assert study['passed'];write(out/'app-study.json',study)
for name in study['captures']:shutil.copy2(r/'build/appearance136-app'/name,out/name)
lock=read('docs/OBSERVATION_136_LOCK.json')
for n,h in lock['sourceHashes'].items():assert sha((r/n).read_bytes().replace(b'\r\n',b'\n'))==h,n
checks=set()
for p in (r/'build/validation136/final').glob('*.xml'):
 e=ET.parse(p).getroot();assert int(e.get('failures','0'))+int(e.get('errors','0'))==0,p
 for c in e.findall('testcase'):
  if c.find('skipped') is None:checks.add(c.get('classname')+'.'+c.get('name'))
assert checks
desktop=(r/'build/desktop136-final.log').read_text(encoding='utf8');assert 'fail 0' in desktop;desktop_count=int(re.search(r'(?m)^. tests (\d+)',desktop)[1])
for n in ['kernel136-final','java136-combined','desktop136-final','agency136-regression','launcher136','package136','receipt136']:assert read('build/'+n+'.json')['exit']==0,n
sources=read('docs/publication136/source136.json');full=read('build/comparison136-summary.json');comparison=full['later'];comparison.update({'development':full['development'],'lock':lock,'sources':sources,'defaultsUnchanged':True,'promoted':False});write(out/'comparison.json',comparison)
cost=read('build/cost136-comparison.json');assert cost['all36ResultHashesEqual'] and cost['allSeedHashesEqual'];write(out/'cost-comparison.json',cost)
v={'passed':True,'javaChecks':sorted(checks),'desktopChecks':desktop_count,'actualAppChecks':study['checks'],'production':study['source'],'buildReceipt':study['buildReceipt'],'candidateLock':lock,'defaultsUnchanged':True,'scope':'Software tests,24 comparisons on eight temporal windows with external boxes and source-only AI visibility; actual UI, exports, stop/resume and cold restart. Guard reduces contamination but rejects useful camera examples; unpromoted. Exact-content memo keeps source/outcome checks. Hosted CI separate.'};write(out/'verification.json',v)
originals=[];seen=set()
def preserve(p,logical):
 if not p.is_file() or logical in seen:return
 seen.add(logical);raw=p.read_bytes();key=sha(raw);dest=out/'originals'/(key+'.gz');packed=gzip.compress(raw,mtime=0);dest.parent.mkdir(exist_ok=True)
 if not dest.exists():dest.write_bytes(packed)
 originals.append({'logicalPath':logical,'path':dest.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(raw),'rawSha256':key})
for pattern in ['appearance136-app*','association136-comparison','association136-development','agency136-evidence','validation136']:
 for folder in (r/'build').glob(pattern):
  if not folder.is_dir():continue
  for p in sorted(folder.rglob('*')):
   if any(x in p.parts for x in ['desktop-profile','node_modules']) or p.name.endswith('.fobs.json'):continue
   if p.is_file() and p.suffix in ['.json','.jsonl','.xml','.png','.txt','.log','.html','.css','.js','.cjs','.fworld','.previous','.csv','.evidence','.pending','.before','.java','.class']:preserve(p,p.relative_to(r).as_posix())
for folder in ['cost136-baseline','cost136-candidate']:
 preserve(r/'build'/folder/'result.json','build/'+folder+'/result.json')
for p in sorted((r/'build').glob('*136*')):
 if p.is_file() and p.suffix not in ['.zip','.jfr']:preserve(p,p.relative_to(r).as_posix())
for name in ['docs/OBSERVATION_136_LOCK.json','build/otb136/window-selection.json']:preserve(r/name,name)
for p in sorted((r/'docs/publication136').glob('*')):
 if p.suffix in ['.json','.txt']:preserve(p,p.relative_to(r).as_posix())
write(out/'original-evidence.json',{'files':originals,'scope':'Original comparison traces, raw label/lock hashes, CI failure XML and status, controlled cost results/profile summary, actual app records and test attempts. Raw footage, packs, source-review contact sheets, JFR binaries and private profiles excluded.'})
paths={x['path'] for x in read('docs/readmes/135/file-inventory.json')}
for n in subprocess.check_output(['git','diff','--name-only',parent,'HEAD'],text=True).splitlines():
 if n.startswith(('workspace-engine/','workspace-desktop/','src/','assets/','.github/')) or n in ['build.gradle','.gitattributes']:paths.add(n)
paths.update(['docs/EDITION_136_CONTRACT.md','docs/EDITION_136_LABEL_NOTE.md','docs/OBSERVATION_136_LOCK.json','docs/publication136/source136.json']);inventory=[]
for n in sorted(paths):
 raw=(r/n).read_bytes()
 try:raw=raw.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':n,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
write(out/'file-inventory.json',inventory)
validation=f"Local checks passed: {len(checks)} distinct Java tests, {desktop_count} desktop checks, kernel integration, actual agency regression and isolated offline/package preparation. Six actual app runs reproduce all570 frozen tracker/filter samples. Independent host arithmetic, supported-example export or explicit refusal, stopped-prefix preservation and cold restart pass."
finding='On the four known development windows, the previous bank admitted46 examples:18 correct and28 wrong. The anchored bank admitted none, rejecting both groups. On the later four windows, the prior bank admitted58:37 correct and21 wrong. The anchored bank admitted3, all correct and all rendered; it rejected the remaining34 correct and21 wrong examples. Across both cohorts it prevents49 observed wrong admissions but also loses52 of55 correct proposals. That is a useful contamination boundary, not successful general appearance learning.'
table='| Cohort / method | Correct measurements / visible | Missing | Wrong overlaps | Admitted | Correct / wrong admissions |\n| --- | ---: | ---: | ---: | ---: | ---: |\n'
for cohort,d in full.items():
 for policy,m in d['summary'].items():
  a=m['tracking'];b=m['memory'];table+=f"| {cohort} / {policy} | {a['correct']} / {a['visible']} | {a['missing']} | {a['overlapFailures']} | {b['admitted']} | {b['independentlyCorrect']} / {b['incorrect']} |\n"
ct='| Saved runs | Operation | Baseline median / ms | New median / ms |\n| ---: | --- | ---: | ---: |\n'
for row in cost['rows']:
 for op,t in row['operations'].items():ct+=f"| {row['runs']} | {op} | {t['baselineMedianMs']:.2f} | {t['candidateMedianMs']:.2f} |\n"
ct+='\n| Saved runs | Baseline cold open / ms | New cold open / ms |\n| ---: | ---: | ---: |\n'
for row in cost['rows']:ct+=f"| {row['runs']} | {row['baselineColdMs']:.2f} | {row['candidateColdMs']:.2f} |\n"
appcost='| Actual task | Tracking / ms | Audit plus optional export / ms |\n| --- | ---: | ---: |\n'
for x in study['runs']:appcost+=f"| {x['caseName']} / {x['policy']} | {x['trackingMs']:.1f} | {x['auditMs']:.1f} |\n"
md=(r/'docs/publication136/README-template.md').read_text(encoding='utf-8-sig')
for k,value in {'RESULTS':table,'FINDING':finding,'COST':ct,'APP_COST':appcost,'VALIDATION':validation,'APP_CHECKS':'\n'.join('- '+x for x in study['checks']),'ATTEMPTS':(r/'build/attempts136.md').read_text(encoding='utf-8-sig')}.items():md=md.replace('{{'+k+'}}',value)
assert '{{' not in md;text(r/'README-136.md',md);text(r/'README.md',md)
note='docs/obsidian/144-anchored-evidence.md';modules=[{'id':i,'name':n,'source':s,'note':note} for i,n,s in [('anchor','Original-image admission boundary','workspace-engine/src/main/java/fraymus/workspace/AppearanceFlow136.java'),('audit','Independent anchor/outcome audit','workspace-engine/src/main/java/fraymus/workspace/AppearanceAudit136.java'),('cost','Byte-verified observation memo','workspace-engine/src/main/java/fraymus/workspace/WorldObservationStore.java'),('panel','Actual evidence-cost control','workspace-desktop/renderer/observation-panel.js')]]
system={'modules':modules,'screenshots':[{'path':'docs/readmes/136/'+name,'title':name.removesuffix('.png').replace('-',' ')} for name in study['captures']],'evidence':[{'path':'docs/readmes/136/'+n} for n in ['verification.json','app-study.json','original-evidence.json','comparison.json','cost-comparison.json']],'comparison':comparison,'validation':validation};write(out/'system-map.json',system)
text(r/note,'# Anchored memory and verified evidence cost — edition136\n\n[[README-136.md]]\n\n'+validation+'\n\n'+finding+'\n\nAll36 controlled read result hashes agree; cold-open cost increases. Keep defaults and independent source/outcome checks. Continue [[docs/INTEGRATED_AGENT_ROADMAP.md]].\n')
write(r/'docs/obsidian/144-anchored-evidence.canvas',{'nodes':[{'id':x['id'],'type':'file','file':x['source'],'x':i*430,'y':0,'width':390,'height':190} for i,x in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']} for i in range(len(modules)-1)]})
html=(r/'docs/publication136/guide-template.html').read_text(encoding='utf8')+'<script id="system-data" type="application/json">'+json.dumps(system).replace('</','<\\/')+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory).replace('</','<\\/')+'</script><script src="guide.js"></script></body></html>'
text(out/'index.html',html);shutil.copy2(r/'docs/publication136/guide.js',out/'guide.js')
for name,link in [('docs/readmes/INDEX.md','[Edition136 — anchored memory and evidence costs](../../README-136.md) | [Offline guide](136/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/144-anchored-evidence.md]] | [[README-136.md]]')]:
 p=r/name;s=p.read_text(encoding='utf-8-sig');text(p,link+'\n\n'+s if not s.startswith(link) else s)
print(json.dumps({'passed':True,'javaChecks':len(checks),'sources':len(inventory),'originalEntries':len(originals)}))
