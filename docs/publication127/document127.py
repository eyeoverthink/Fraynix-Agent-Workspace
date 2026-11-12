from pathlib import Path
import json,hashlib,subprocess,shutil,gzip,xml.etree.ElementTree as ET
r=Path.cwd();out=r/'docs/readmes/127';out.mkdir(parents=True,exist_ok=True)
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-127']).returncode==1
read=lambda p:json.loads((r/p).read_text(encoding='utf8'))
def text(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8')
def write(p,v):text(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
sha=lambda b:hashlib.sha256(b).hexdigest()
study=read('build/lifecycle127-first/result.json');review=read('build/lifecycle127-review.json');assert study['passed'] and review['passed']
write(out/'lifecycle-study.json',study);write(out/'final-ui-review.json',review)
shots=[]
for name in review['captures']:
 shutil.copy2(r/'build/lifecycle127-first'/name,out/name);shots.append(name)
checks=set();suites=[]
for folder in sorted((r/'build/validation127').iterdir()):
 if not folder.is_dir() or folder.name in ['first','failed-broad']:continue
 count=0
 for p in folder.glob('*.xml'):
  e=ET.parse(p).getroot();assert int(e.get('failures','0'))+int(e.get('errors','0'))==0,p
  for c in e.findall('testcase'):
   if c.find('skipped') is None:checks.add(c.get('classname')+'.'+c.get('name'));count+=1
 suites.append({'name':folder.name,'executions':count})
assert any(x['name']=='final' for x in suites)
desktop=(r/'build/desktop127-final.log').read_text(encoding='utf8');assert 'tests 161' in desktop and 'fail 0' in desktop
for n in ['kernel127-final','java127-final','desktop127-final','agency127-regression','launcher127','package127','receipt127']:assert read('build/'+n+'.json')['exit']==0,n
measurements={'rows':study['timings'],'scope':'Individual explicit lifecycle caller spans, including deliberately refused reactivation attempts; no repeated statistical or universal latency claim.'};write(out/'measurements.json',measurements)
v={'passed':True,'javaChecks':sorted(checks),'javaSuites':suites,'desktopChecks':161,'actualAppChecks':study['checks'],'finalUI':review,'actualAbruptJavaInterruption':True,'full32ActiveCapacity':True,'distinctFamilies':['motion data','four-dimensional procedural geometry','qualified Java-authored creation/workflow','native typed graph with declared unit-host receipts'],'newModelCalls':0,'sourceScopes':{'originalAppStudy':study['source'],'finalUIReview':review['source'],'scope':'Original completed app study is retained unchanged. Later UTF-8 correction and state-boundary checks are separately recorded; final UI reopens the SAME study project and original proofs. No original result is relabelled as a new model or physics run.'},'scope':'Shared Java saved-procedure management, preserving all original source/outcome checks. Not automatic adaptation, model-weight learning, general intelligence or removal of established runtime dependencies.'};write(out/'verification.json',v)
originals=[];seen=set()
def preserve(p,logical):
 if not p.is_file() or logical in seen:return
 seen.add(logical);raw=p.read_bytes();key=sha(raw);dest=out/'originals'/(key+'.gz');packed=gzip.compress(raw,mtime=0);dest.parent.mkdir(exist_ok=True)
 if not dest.exists():dest.write_bytes(packed)
 originals.append({'logicalPath':logical,'path':dest.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(raw),'rawSha256':key})
for pattern in ['lifecycle127-*','validation127']:
 for folder in sorted((r/'build').glob(pattern)):
  if not folder.is_dir():continue
  for p in sorted(folder.rglob('*')):
   if any(x in p.parts for x in ['desktop-profile','node_modules']):continue
   if p.is_file() and p.suffix in ['.json','.jsonl','.xml','.png','.txt','.log','.html','.css','.js','.cjs','.fworld','.previous','.csv','.evidence','.pending','.before']:preserve(p,p.relative_to(r).as_posix())
for p in sorted((r/'build').glob('*127*')):
 if p.is_file():preserve(p,p.relative_to(r).as_posix())
write(out/'original-evidence.json',{'files':originals,'scope':'Exact synthetic project originals, actual DOM receipts, source snapshots, deliberate altered-report control, failed unit attempt and final reviews. Profiles/private projects excluded. Capacity aliases do not count as distinct learned methods.'})
paths={x['path'] for x in read('docs/readmes/126/file-inventory.json')}
for n in subprocess.check_output(['git','diff','--name-only','7369b946','HEAD'],text=True).splitlines():
 if n.startswith(('workspace-engine/','workspace-desktop/','src/','assets/','.github/')) or n=='build.gradle':paths.add(n)
paths.update(['docs/EDITION_127_CONTRACT.md','docs/INTEGRATED_AGENT_ROADMAP.md']);inventory=[]
for n in sorted(paths):
 raw=(r/n).read_bytes()
 try:raw=raw.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':n,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
write(out/'file-inventory.json',inventory)
validation=f"Local validation passed: {len(checks)} distinct Java test methods, 161 desktop checks, the executable factory-kernel harnesses, the actual agency regression and isolated offline preparation/package checks. The actual lifecycle study includes real rendered reports, an abrupt owned Java-process interruption, a cold restart and complete32-active capacity controls. Hosted CI was checked before implementation and remains a separately reported publication result."
results='\n'.join('- '+x for x in study['checks'])
timing='| Observed operation | Milliseconds |\n| --- | ---: |\n'
for name in ['inspect motion-report127','inspect branching4d127','inspect qualified-summary127','reactivate qualified-summary127','catalog at32']:
 row=next(x for x in study['timings'] if x['name']==name);timing+=f"| {name} | {row['ms']:.2f} |\n"
failures='The initial focused unit run passed9 of10 checks. One test wrongly expected an unchanged frozen dataset v1 to become invalid after importing v2. The corrected test preserves that legitimate behavior and deliberately alters original bound content to prove refusal. Later focused and regression runs retain the original assertions plus version, capacity, interruption and graph-dependency checks. The initial UI study passed functionally, but screenshot inspection found UTF-8 punctuation corrupted by a local file edit. The original captures remain archived; final reviewed captures use corrected text and recheck the same saved project. No failed outcome or report was silently replaced.'
md=(r/'docs/publication127/README-template.md').read_text(encoding='utf8')
for k,value in {'VALIDATION':validation,'RESULTS':results,'TIMINGS':timing,'FAILURES':failures}.items():md=md.replace('{{'+k+'}}',value)
assert '{{' not in md;text(r/'README-127.md',md);text(r/'README.md',md)
note='docs/obsidian/135-procedure-lifecycle.md'
modules=[{'id':i,'name':n,'source':s,'note':note} for i,n,s in [('lifecycle','Lifecycle owner','workspace-engine/src/main/java/fraymus/workspace/ProcedureLifecycle127.java'),('goal','Original goal contracts','workspace-engine/src/main/java/fraymus/workspace/GoalRunner.java'),('workflow','Verified workflow ancestry','workspace-engine/src/main/java/fraymus/workspace/WorkflowRunner.java'),('ui','Integrated inspector','workspace-desktop/renderer/procedures127.js'),('study','Actual app study','workspace-desktop/test/lifecycle127-integration.cjs')]]
system={'modules':modules,'screenshots':[{'path':'docs/readmes/127/'+name,'title':name.removesuffix('.png').replace('-',' ')} for name in shots],'evidence':[{'path':'docs/readmes/127/'+n} for n in ['verification.json','lifecycle-study.json','final-ui-review.json','measurements.json','original-evidence.json']],'measurements':measurements,'validation':validation};write(out/'system-map.json',system)
text(r/note,'# Saved Java procedure lifecycle - edition127\n\n[[README-127.md]]\n\n'+validation+'\n\nSeparate availability from evidence. Retirement changes one atomic lifecycle record, preserves original procedure/source bytes and leaves historical lookup available. Reactivation reconstructs original verified contracts, checks dependencies and capacity, and retains a dated receipt. Fresh execution still needs fresh verification.\n\nNext: [[docs/INTEGRATED_AGENT_ROADMAP.md]] - observation to a working artifact, evaluated adaptation and coherent daily use.\n')
write(r/'docs/obsidian/135-procedure-lifecycle.canvas',{'nodes':[{'id':x['id'],'type':'file','file':x['source'],'x':i%3*430,'y':i//3*250,'width':390,'height':190} for i,x in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']} for i in range(len(modules)-1)]})
html=(r/'docs/publication127/guide-template.html').read_text(encoding='utf8')+'<script id="system-data" type="application/json">'+json.dumps(system).replace('</','<\\/')+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory).replace('</','<\\/')+'</script><script src="guide.js"></script></body></html>'
text(out/'index.html',html);shutil.copy2(r/'docs/publication127/guide.js',out/'guide.js')
for name,link in [('docs/readmes/INDEX.md','[Edition127 - saved Java procedure lifecycle](../../README-127.md) | [Offline guide](127/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/135-procedure-lifecycle.md]] | [[README-127.md]]')]:
 p=r/name;s=p.read_text(encoding='utf-8-sig');text(p,link+'\n\n'+s if not s.startswith(link) else s)
print(json.dumps({'passed':True,'javaChecks':len(checks),'sources':len(inventory),'originalEntries':len(originals)}))
