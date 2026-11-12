from pathlib import Path
import json,hashlib,subprocess,shutil,gzip,xml.etree.ElementTree as ET
r=Path.cwd();out=r/'docs/readmes/124';out.mkdir(parents=True,exist_ok=True)
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-124']).returncode==1
def read(p):return json.loads((r/p).read_text(encoding='utf8'))
def text(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8')
def write(p,v):text(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
def sha(b):return hashlib.sha256(b).hexdigest()
datasets={k:read(f'workspace-engine/build/budget124-{k}/result.json') for k in ['development','reserved','software']}
assert [len(datasets[k]['rows']) for k in datasets]==[27,4,9]
assert all(d['passed'] for d in datasets.values())
uiroot=r/'build/game124-ui';ui=read(uiroot/'result.json');assert ui['passed'] and len(ui['requests'])==11
shots=['budget-controls.png','budget-0.png','budget-4.png','budget-30.png','unknown-scene.png','failed-course.png','budget-1100.png','return-live.png','collected-after-return.png']
for n in shots:shutil.copy2(uiroot/n,out/n)
shutil.copy2(r/'build/budget124-costs.png',out/'budget-costs.png')
for k,d in datasets.items():write(out/('study-'+k+'.json'),d)
write(out/'actual-ui.json',ui)
checks=set();suites=[]
for folder in sorted((r/'build/validation124').iterdir()):
 if not folder.is_dir():continue
 total=0
 for p in folder.glob('*.xml'):
  e=ET.parse(p).getroot();assert int(e.get('failures','0'))+int(e.get('errors','0'))==0,p
  for c in e.findall('testcase'):
   if c.find('skipped') is None:checks.add(c.get('classname')+'.'+c.get('name'));total+=1
 suites.append({'name':folder.name,'executions':total})
assert len(checks)>=200
for name,count in [('desktop124-final.log',158),('desktop124-extra.log',10)]:
 s=(r/'build'/name).read_text(encoding='utf8');assert f'tests {count}' in s and 'fail 0' in s
assert 'BUILD SUCCESSFUL' in (r/'build/launcher124.log').read_text(encoding='utf8')
regressions={p.parent.name:read(p) for p in (r/'workspace-engine/build').glob('controller124-regression-*/result.json')}
trials=[t for d in regressions.values() for c in d['cases'] for t in c['trials']]
assert len(trials)==18 and all(t['won'] and t['final']['controller']['damage']==0 for t in trials)
refs=read('workspace-engine/build/reference124-final/result.json');assert refs['passed'];write(out/'reference-hardware.json',refs)
rows=[]
for partition,d in datasets.items():
 for row in d['rows']:
  if not row['requested'].startswith('budget'):continue
  m=row['measurement'];decision=row['decision']['result'];full=next(o for o in decision['alternatives'] if o['strategy']=='full')
  rows.append({'partition':partition,'case':row['case'],'allowance':decision['allowance'],'selected':decision['selected'],'requiredSecondsPerPickup':full.get('requiredSecondsPerPickup'),'reason':full['reason'],'pickups':m['audit']['aiPickups'],'completionCertified':m['completionCertified'],'seconds':m['choice124']['seconds'],'externalSeconds':row['wallNanos']/1e9,'overheadSeconds':row['decision']['overheadSeconds'],'admissionSeconds':row['decision']['admissionSeconds'],'completionReserveSeconds':row['decision']['completionVerificationReserveSeconds'],'alternatives':decision['alternatives']})
measurements={'rows':rows,'workflows':40,'development':27,'reserved':4,'software':9,'scope':'Admission through first verified outcome; external rows additionally include measurement re-audit. Later app shutdown excluded. Initial development pilot retained separately and excluded from final calibration. No probability or deadline guarantee.'}
write(out/'measurements.json',measurements)
v={'passed':True,'javaChecks':sorted(checks),'javaSuites':suites,'desktopChecks':168,'actualChatRequests':11,'actualAbruptRecovery':True,'actualAiFailureRefused':True,'regressionExecutions':18,'studyRows':40,'measurements':measurements,'referenceControllerResults':{'hardware':{'cases':len(refs['cases']),'refusals':len(refs['refusals'])}},'launcher':{'passed':True,'scope':'Isolated PrepareOnly Offline; package receipt verified before and after UI.'},'sourceCommit':ui['source'],'modelCalls':sum(x['task']['timing052']['modelCalls'] for x in ui['requests']),'scope':'Opt-in exact-condition cost/benefit envelope around unchanged GameTask115 and contact122. Current source and outcome checks retained. No general transfer, hard deadline, neural training or skill-bank promotion.'}
assert v['modelCalls']==0;write(out/'verification.json',v)
originals=[];seen=set();projects=set()
def preserve(p,logical):
 if not p.is_file() or logical in seen:return
 seen.add(logical);b=p.read_bytes();raw=sha(b);dest=out/'originals'/(raw+'.gz');packed=gzip.compress(b,mtime=0);dest.parent.mkdir(exist_ok=True)
 if not dest.exists():dest.write_bytes(packed)
 originals.append({'logicalPath':logical,'path':dest.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(b),'rawSha256':raw})
def findprojects(x):
 if isinstance(x,dict):
  if isinstance(x.get('project'),str):projects.add(Path(x['project']))
  for value in x.values():findprojects(value)
 elif isinstance(x,list):
  for value in x:findprojects(value)
folders=[r/'build/validation124']+list((r/'workspace-engine/build').glob('budget124*'))+list((r/'workspace-engine/build').glob('controller124-regression-*'))+[r/'workspace-engine/build'/n for n in ['reference124-final','reference115','game115-repair','game115-restart','challenge118-task','challenge118-failed','challenge118-image','recovery119-task','opportunity120-task','opportunity121-task','contact122-task','pickup123-task']]
for folder in folders:
 for p in sorted(folder.rglob('*')):
  if p.is_file() and p.suffix in ['.json','.jsonl','.xml','.png','.txt','.log']:
   preserve(p,p.relative_to(r).as_posix())
   if p.name=='result.json':findprojects(read(p))
for project in projects:
 assert project.is_dir(),project
 for sub in ['game-levels114','game-tasks115','game-choices124','game-references115']:
  for p in sorted((project/sub).rglob('*')):
   if p.is_file() and p.suffix in ['.json','.jsonl','.png']:preserve(p,'original-projects/'+project.name+'/'+p.relative_to(project).as_posix())
for folder in sorted((r/'build').glob('game124-ui*')):
 if not folder.is_dir():continue
 for p in folder.iterdir():
  if p.is_file():preserve(p,p.relative_to(r).as_posix())
 for sub in ['game-tasks115','game-choices124','game-levels114','game-references115','agency-043','embedded-game']:
  for p in sorted((folder/'project'/sub).rglob('*')):
   if p.is_file() and p.suffix in ['.json','.jsonl','.png','.log']:preserve(p,p.relative_to(r).as_posix())
for pattern in ['*124*.log','ci124*.json','study095-build*.json']:
 for p in sorted((r/'build').glob(pattern)):preserve(p,p.relative_to(r).as_posix())
write(out/'original-evidence.json',{'files':originals,'scope':'Exact initial development pilot, final repeated workflows, regressions, failed-task refusal, restart and actual chat originals. Private worlds excluded.'})
sourcePaths={x['path'] for x in read('docs/readmes/123/file-inventory.json')}
for n in subprocess.check_output(['git','diff','--name-only','1d328f37','HEAD'],text=True).splitlines():
 if n.startswith(('workspace-engine/','workspace-desktop/','src/','assets/','.github/')) or n=='build.gradle':sourcePaths.add(n)
sourcePaths.update(['docs/EDITION_124_CONTRACT.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md']);inventory=[]
for n in sorted(sourcePaths):
 b=(r/n).read_bytes()
 try:b=b.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':n,'bytes':len(b),'sha256':sha(b),'hashKind':kind})
write(out/'file-inventory.json',inventory)
note='docs/obsidian/132-budgeted-optional-actions.md'
modules=[{'id':i,'name':n,'source':s,'note':note} for i,n,s in [('choice','Allowance and benefit bounds','workspace-engine/src/main/java/fraymus/workspace/GameBudgetPolicy124.java'),('memory','Durable measured choices','workspace-engine/src/main/java/fraymus/workspace/GameChoices124.java'),('runtime','Original native simulation','src/main/java/runtime/EmbeddedGameRuntime.java'),('task','Unchanged task verifier','workspace-engine/src/main/java/fraymus/workspace/GameTask115.java'),('proof','Actual visible review','workspace-desktop/test/game124-integration.cjs')]]
titles=['Explicit allowance in Game Lab','Zero allowance chooses direct','Tight allowance decision','Generous allowance and recovered task','New scene abstention','Impossible course retained','Compact integrated layout','Actual return in progress','Collection after return']
system={'modules':modules,'screenshots':[{'path':'docs/readmes/124/'+n,'title':title} for n,title in zip(shots,titles)],'evidence':[{'path':'docs/readmes/124/'+n} for n in ['actual-ui.json','measurements.json','verification.json','original-evidence.json']]};write(out/'system-map.json',system)
def num(n):return 'not established' if n is None else f'{n:.2f}'
findings=' '.join(f"{x['partition']}/{x['case']}: allowance {x['allowance']:g} selected {x['selected']}, collected {x['pickups']}, first verified outcome {x['seconds']:.2f}s." for x in rows if x['allowance']==30)
renderer=datasets['development']['rows'][0]['measurement']['final']['renderer']
values={'FINDINGS':findings,'ENVIRONMENT':f"Measured hardware renderer: {renderer['name']}. Software rendering is a separate same-case repeat, not additional unseen evidence. Java21; study owner limited to four processors and1024MiB heap, native child four processors/512MiB. Actual chat owner uses two processors/512MiB. Context fingerprints prevent pooling these clocks.",'RESULT_ROWS':'\n'.join(f"| {x['partition']} / {x['case']} | {x['allowance']:g} | {x['selected']} | {num(x['requiredSecondsPerPickup'])} | {x['pickups']} / {'verified' if x['completionCertified'] else 'refused'} | {x['seconds']:.2f} | {x['externalSeconds']:.2f} |" for x in rows),'VALIDATION':f"Local validation passed: {len(checks)} distinct Java methods,168 desktop checks,40 final native workflows,18 prior courses with no damage, six transformed reference images/four refusals, plus original reference regression. Native tests cover original checks, invalid allowances, uncertainty, failures, interrupted clocks, legacy restart, and altered choices/timing-source receipts. The actual app ran11 requests, recovered its deliberately interrupted Java owner, verified retained choice evidence and refused an impossible course. Zero LLM calls in these explicit Java routes; no model-training claim.",'CHAT_ROWS':'\n'.join(f"| {x['prompt']} | {x['elapsedMs']/1000:.2f} | {x['task']['status']} | {x['child'].get('audit',{}).get('aiPickups','not certified')} |" for x in ui['requests'])}
def fill(s):
 for k,v in values.items():s=s.replace('{{'+k+'}}',v)
 assert '{{' not in s;return s
md=fill((r/'docs/publication124/README-template.md').read_text(encoding='utf8'));text(r/'README-124.md',md);text(r/'README.md',md)
text(r/note,'# Budgeted optional actions — edition124\n\n[[README-124.md]] records opt-in time allowances, conservative benefit and cost bounds, abstention and40 final workflows. '+findings+'\n\nUnchanged native verification; original failures retained. Next: evaluate cost prediction and missed useful opportunities, then richer contacts. [[docs/PLAYABLE_LEVEL_CREATION_PLAN.md]]\n')
write(r/'docs/obsidian/132-budgeted-optional-actions.canvas',{'nodes':[{'id':m['id'],'type':'file','file':m['source'],'x':i%3*430,'y':i//3*250,'width':390,'height':190} for i,m in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']} for i in range(len(modules)-1)]})
html=(r/'docs/publication124/guide-template.html').read_text(encoding='utf8')+'<script id="system-data" type="application/json">'+json.dumps(system)+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory)+'</script><script id="measurements-data" type="application/json">'+json.dumps(measurements)+'</script><script src="guide.js"></script></html>'
text(out/'index.html',html);shutil.copy2(r/'docs/publication124/guide.js',out/'guide.js')
for name,link in [('docs/readmes/INDEX.md','[Edition124 — budgeted optional actions](../../README-124.md) · [Offline guide](124/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/132-budgeted-optional-actions.md]] · [[README-124.md]]')]:
 p=r/name;s=p.read_text(encoding='utf-8-sig');text(p,link+'\n\n'+s if not s.startswith(link) else s)
print(json.dumps({'passed':True,'javaChecks':len(checks),'sources':len(inventory),'originalEntries':len(originals),'findings':findings}))
