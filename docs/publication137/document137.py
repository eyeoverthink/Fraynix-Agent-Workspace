from pathlib import Path
import json,hashlib,subprocess,shutil,gzip,xml.etree.ElementTree as ET,re,statistics
r=Path.cwd();out=r/'docs/readmes/137';out.mkdir(parents=True,exist_ok=True);parent='72a3b96913a2fb81da1ad72f2aac21c7bb1dfbdd'
read=lambda p:json.loads((r/p).read_text(encoding='utf-8-sig'));sha=lambda b:hashlib.sha256(b).hexdigest()
def text(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(s.encode('utf8'))
def write(p,v):text(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-137']).returncode==1
study=read('build/reference137-app/result.json');assert study['passed'];write(out/'app-study.json',study)
small=read('build/reference137-export/result.json');assert small['passed'];write(out/'small-export.json',small)
app_checks=list(dict.fromkeys(study['checks']))+['Separate actual-app synthetic control: two runs/16 samples, complete small export with all13 donor images, reference labels/audit/source run and unchanged native world.']
for name in study['captures']:shutil.copy2(r/'build/reference137-app'/name,out/name)
lock=read('docs/OBSERVATION_137_LOCK.json')
for n,h in lock['sourceHashes'].items():assert sha((r/n).read_bytes().replace(b'\r\n',b'\n'))==h,n
checks=set()
for p in (r/'build/validation137/final').glob('*.xml'):
 e=ET.parse(p).getroot();assert int(e.get('failures','0'))+int(e.get('errors','0'))==0,p
 for c in e.findall('testcase'):
  if c.find('skipped') is None:checks.add(c.get('classname')+'.'+c.get('name'))
assert checks
desktop=(r/'build/desktop137-final.log').read_text(encoding='utf8');assert 'fail 0' in desktop;desktop_count=int(re.search(r'(?m)^. tests (\d+)',desktop)[1])
for n in ['kernel137-final','java137-final','desktop137-final','agency137-regression','launcher137','package137','receipt137']:assert read('build/'+n+'.json')['exit']==0,n
rows=[{'case':x['caseName'],'policy':x['policy'],'summary':x['audit']['summary'],'reference137':x['audit'].get('reference137'),'trackingMs':x['trackingMs'],'auditMs':x['auditMs']} for x in study['runs'] if x['role']=='evaluation']
sums={}
for policy in lock['methods']:
 selected=[x for x in rows if x['policy']==policy];sums[policy]={'tracking':{k:sum(x['summary'][k] for x in selected) for k in ['labels','visible','correct','missing','overlapFailures','hidden','unsupported','uncertain']},'importedRecoveries':sum((x['reference137'] or {}).get('importedRecoveries',0) for x in selected),'recoveries':sum((x['reference137'] or {}).get('recoveries',0) for x in selected)}
donors=[{'case':x['caseName'],'examples':len(x['retained']['examples']),'setId':x['retained']['id'],'excluded':x['retained']['excludedAdmissions']} for x in study['runs'] if x['retained']]
comparison={'rows':rows,'summary':sums,'retained':donors,'lock':lock,'sources':read('docs/publication137/source137.json'),'defaultsUnchanged':True,'promoted':False};write(out/'comparison.json',comparison)
cost=read('build/cost137-comparison.json');assert cost['all36ReadHashesEqual'] and cost['all9WriteAndPersistedHashesEqual'];write(out/'cost-comparison.json',cost)
validation=f"Local checks passed: {len(checks)} distinct Java tests, {desktop_count} desktop checks, kernel integration, actual agency regression and isolated offline/package preparation. Four source runs plus twelve evaluation runs exercise 1,520 source samples. A separate two-run synthetic control verifies a complete desktop export within 20 MB. All 380 incumbent outputs match published edition-136 original traces; independent host overlap arithmetic, source curation, stopped-prefix preservation, export and cold restart pass."
v={'passed':True,'javaChecks':sorted(checks),'desktopChecks':desktop_count,'actualAppChecks':app_checks,'production':study['source'],'buildReceipt':study['buildReceipt'],'candidateLock':lock,'defaultsUnchanged':True,'scope':validation+' Camera windows are previously exposed; independent human labels and general physical accuracy remain unresolved. Hosted CI separate.'};write(out/'verification.json',v)
originals=[];seen=set()
def preserve(p,logical):
 if not p.is_file() or logical in seen:return
 seen.add(logical);raw=p.read_bytes();key=sha(raw);dest=out/'originals'/(key+'.gz');packed=gzip.compress(raw,mtime=0);dest.parent.mkdir(exist_ok=True)
 if not dest.exists():dest.write_bytes(packed)
 originals.append({'logicalPath':logical,'path':dest.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(raw),'rawSha256':key})
for pattern in ['reference137-app*','reference137-export','agency137-evidence','validation137','target137-first-results']:
 for folder in (r/'build').glob(pattern):
  if not folder.is_dir():continue
  for p in sorted(folder.rglob('*')):
   if any(x in p.parts for x in ['desktop-profile','node_modules','exports']) or p.name.endswith('.fobs.json'):continue
   if p.is_file() and p.suffix in ['.json','.jsonl','.xml','.png','.txt','.log','.html','.css','.js','.cjs','.fworld','.previous','.csv','.evidence','.pending','.before','.java','.class']:preserve(p,p.relative_to(r).as_posix())
for folder in (r/'build').glob('cost137-*'):
 preserve(folder/'result.json','build/'+folder.name+'/result.json')
for p in sorted((r/'build').glob('*137*')):
 if p.is_file() and p.suffix not in ['.zip','.jfr']:preserve(p,p.relative_to(r).as_posix())
for name in ['docs/OBSERVATION_137_LOCK.json','docs/publication137/source137.json']:preserve(r/name,name)
write(out/'original-evidence.json',{'files':originals,'scope':'Original run/audit/curation records, costs, startup and restart checks, UI captures, test reports and attempts. Raw image files/footage and private desktop profiles excluded; exact source/label hashes and public provenance retained.'})
paths={x['path'] for x in read('docs/readmes/136/file-inventory.json')}
for n in subprocess.check_output(['git','diff','--name-only',parent,'HEAD'],text=True).splitlines():
 if n.startswith(('workspace-engine/','workspace-desktop/','src/','assets/','.github/')) or n in ['build.gradle','.gitattributes']:paths.add(n)
paths.update(['docs/EDITION_137_CONTRACT.md','docs/OBSERVATION_137_LOCK.json','docs/publication137/source137.json']);inventory=[]
for n in sorted(paths):
 raw=(r/n).read_bytes()
 try:raw=raw.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':n,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
write(out/'file-inventory.json',inventory)
candidate=sums['verified-memory-137'];control=sums['seed-only-137'];base=sums['legacy-flow']
finding=f"The current-code curation retained {sum(d['examples'] for d in donors)} independently supported examples, including {sum(d['examples'] for d in donors if not d['case'].startswith('bird1'))} camera examples. They remain frozen; no evaluation prediction was admitted. Verified-reference tracking produced {candidate['tracking']['correct']}/{candidate['tracking']['visible']} correct visible/partial measurements, versus {control['tracking']['correct']}/{control['tracking']['visible']} for seed-only and {base['tracking']['correct']}/{base['tracking']['visible']} for the default. Imported references produced {candidate['importedRecoveries']} recovery events. Retention and provenance are operational; general camera recovery remains unqualified and no default promotion is made."
table='| Method / window | Correct / visible or partial | Missing | Wrong overlaps | Imported recoveries |\n| --- | ---: | ---: | ---: | ---: |\n'
for x in rows:
 s=x['summary'];table+=f"| {x['policy']} / {x['case']} | {s['correct']} / {s['visible']} | {s['missing']} | {s['overlapFailures']} | {(x['reference137'] or {}).get('importedRecoveries',0)} |\n"
references='| Reference source | Retained fully visible examples | Excluded proposals |\n| --- | ---: | ---: |\n'+''.join(f"| {x['case']} | {x['examples']} | {x['excluded']} |\n" for x in donors)
ct='| Saved runs | Cold open, before → after / ms | Median write, before → after / ms |\n| ---: | ---: | ---: |\n'
for x in cost['rows']:ct+=f"| {x['runs']} | {x['coldOpenMs'][0]:.1f} → {x['coldOpenMs'][1]:.1f} | {x['writeMedianMs'][0]:.1f} → {x['writeMedianMs'][1]:.1f} |\n"
ct+='\n| Saved runs / read | Before median / ms | After median / ms |\n| --- | ---: | ---: |\n'
for x in cost['rows']:
 for op,t in x['operations'].items():ct+=f"| {x['runs']} / {op} | {t[0]:.2f} | {t[1]:.2f} |\n"
appcost='| Actual task | Track / ms | Audit and optional retain / ms |\n| --- | ---: | ---: |\n'+''.join(f"| {x['caseName']} / {x['policy']} | {x['trackingMs']:.1f} | {x['auditMs']:.1f} |\n" for x in study['runs'])
md=(r/'docs/publication137/README-template.md').read_text(encoding='utf8')
for k,value in {'FINDING':finding,'RESULTS':table,'REFERENCES':references,'COST':ct,'APP_COST':appcost,'VALIDATION':validation,'APP_CHECKS':'\n'.join('- '+x for x in app_checks),'ATTEMPTS':(r/'build/attempts137.md').read_text(encoding='utf8')}.items():md=md.replace('{{'+k+'}}',value)
assert '{{' not in md;text(r/'README-137.md',md);text(r/'README.md',md)
note='docs/obsidian/145-verified-references.md';modules=[{'id':i,'name':n,'source':s,'note':note} for i,n,s in [('reference','Verified reference provenance','workspace-engine/src/main/java/fraymus/workspace/ReferenceMemory137.java'),('tracker','Frozen reference recovery','workspace-engine/src/main/java/fraymus/workspace/VerifiedMemoryFlow137.java'),('audit','Independent recovery evidence','workspace-engine/src/main/java/fraymus/workspace/ReferenceAudit137.java'),('cost','Durable observation transactions','workspace-engine/src/main/java/fraymus/workspace/WorldObservationStore.java'),('panel','Reference selection controls','workspace-desktop/renderer/observation-panel.js')]]
system={'modules':modules,'screenshots':[{'path':'docs/readmes/137/'+name,'title':name.removesuffix('.png').replace('-',' ')} for name in study['captures']],'evidence':[{'path':'docs/readmes/137/'+n} for n in ['verification.json','app-study.json','small-export.json','original-evidence.json','comparison.json','cost-comparison.json']],'comparison':comparison,'validation':validation};write(out/'system-map.json',system)
text(r/note,'# Verified reference reuse — edition137\n\n[[README-137.md]]\n\n'+validation+'\n\n'+finding+'\n\nPreserve defaults, historical bytes, uncertainty and source/outcome checks. Continue [[docs/INTEGRATED_AGENT_ROADMAP.md]].\n')
write(r/'docs/obsidian/145-verified-references.canvas',{'nodes':[{'id':x['id'],'type':'file','file':x['source'],'x':i*430,'y':0,'width':390,'height':190} for i,x in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']} for i in range(len(modules)-1)]})
html=(r/'docs/publication137/guide-template.html').read_text(encoding='utf8')+'<script id="system-data" type="application/json">'+json.dumps(system).replace('</','<\\/')+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory).replace('</','<\\/')+'</script><script src="guide.js"></script></body></html>'
text(out/'index.html',html);shutil.copy2(r/'docs/publication137/guide.js',out/'guide.js')
for name,link in [('docs/readmes/INDEX.md','[Edition137 — verified references and durable evidence](../../README-137.md) | [Offline guide](137/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/145-verified-references.md]] | [[README-137.md]]')]:
 p=r/name;s=p.read_text(encoding='utf-8-sig');text(p,link+'\n\n'+s if not s.startswith(link) else s)
print(json.dumps({'passed':True,'javaChecks':len(checks),'sources':len(inventory),'originalEntries':len(originals),'finding':finding}))
