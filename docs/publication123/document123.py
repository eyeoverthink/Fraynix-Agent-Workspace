from pathlib import Path
import json,hashlib,subprocess,shutil,gzip,xml.etree.ElementTree as ET
r=Path.cwd();out=r/'docs/readmes/123';out.mkdir(parents=True,exist_ok=True)
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-123']).returncode==1,'Never rewrite a published edition'
def read(p):return json.loads((r/p).read_text(encoding='utf8'))
def text(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8')
def write(p,v):text(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
def sha(b):return hashlib.sha256(b).hexdigest()
datasets={k:read(f'workspace-engine/build/pickup123-{k}/result.json') for k in ['development','reserved','software']}
assert [len(datasets[k]['rows']) for k in datasets]==[36,24,12]
assert all(d['passed'] for d in datasets.values())
uiroot=r/'build/game123-ui';ui=read(uiroot/'result.json');assert ui['passed'] and all(ui[k] for k in ['returnFrame','collectionFrame','waitFrame','riskFrame'])
shots=['pickup-controls.png','return-full.png','return-disabled.png','wait-full.png','wait-disabled.png','verified-strategy.png','unsupported-pickup.png','failed-course.png','pickup-1100.png','live-wait.png','return-native.png','return-live.png','collected-native.png','collected-after-return.png','declined-pickup.png']
for n in shots:shutil.copy2(uiroot/n,out/n)
shutil.copy2(r/'build/pickup123-costs.png',out/'pickup-costs.png')
for label,d in datasets.items():write(out/('study-'+label+'.json'),d)
write(out/'actual-ui.json',ui)
checks=set();suites=[]
for folder in sorted((r/'build/validation123').iterdir()):
 if not folder.is_dir():continue
 total=0
 for p in folder.glob('*.xml'):
  e=ET.parse(p).getroot();assert int(e.get('failures','0'))+int(e.get('errors','0'))==0,p
  for c in e.findall('testcase'):
   if c.find('skipped') is None:checks.add(c.get('classname')+'.'+c.get('name'));total+=1
 suites.append({'name':folder.name,'executions':total})
assert len(checks)>=180
for name,count in [('desktop123-final.log',158),('desktop123-extra.log',10)]:
 s=(r/'build'/name).read_text(encoding='utf8');assert f'tests {count}' in s and 'fail 0' in s
assert 'BUILD SUCCESSFUL' in (r/'build/launcher123.log').read_text(encoding='utf8')
regressions={p.parent.name:read(p) for p in (r/'workspace-engine/build').glob('controller123-regression-*/result.json')}
assert len(regressions)==3 and all(d['passed'] for d in regressions.values())
trials=[t for d in regressions.values() for c in d['cases'] for t in c['trials']]
assert len(trials)==18 and all(t['won'] and t['final']['controller']['damage']==0 for t in trials)
refs={k:read(f'workspace-engine/build/reference123-{suffix}/result.json') for k,suffix in [('hardware','final'),('software','mesa')]}
referenceResults={k:{'cases':len(d['cases']),'refusals':len(d['refusals']),'aiWins':sum(c['task']['audit']['existingAI']=='won' for c in d['cases'])} for k,d in refs.items()}
assert all(d['passed'] for d in refs.values()) and all(v=={'cases':6,'refusals':4,'aiWins':6} for v in referenceResults.values())
for k,d in refs.items():write(out/('reference-'+k+'.json'),d)
metrics=[];comparisons=[]
for partition,d in datasets.items():
 for c in d['analysis']['cases']:
  metrics += [dict(s,partition=partition,case=c['case']) for s in c['strategies']]
  comparisons += [dict(x,partition=partition,case=c['case']) for x in c['comparisons']]
measurements={'strategies':metrics,'comparisons':comparisons,'scope':datasets['development']['analysis']['scope'],'rows':72,'repetitionsPerApproach':3,'pilotExcluded':True,'controllerTunedAfterReserved':False}
write(out/'measurements.json',measurements)
v={'passed':True,'javaChecks':sorted(checks),'javaSuites':suites,'desktopChecks':168,'launcher':{'passed':True,'scope':'Isolated PrepareOnly Offline; package receipt verified before and after actual UI.'},'actualChatRequests':len(ui['requests']),'actualAbruptRecovery':True,'actualAiFailureRefused':True,'referenceControllerResults':referenceResults,'regressionExecutions':len(trials),'studyRows':72,'measurements':measurements,'modelCalls':sum(x['task']['timing052']['modelCalls'] for x in ui['requests']),'sourceCommit':ui['source'],'scope':'Same contact122 controller with explicit wait/return ablations. Full Java workflow timing and separate actual app timings. All source and outcome checks retained. No automatic cost policy, user value assumption, neural training or skill promotion.'}
assert len(ui['requests'])==6 and v['modelCalls']==0
write(out/'verification.json',v)
originals=[];seen=set();projects={}
def preserve(p,logical):
 if not p.is_file() or logical in seen:return
 seen.add(logical);b=p.read_bytes();raw=sha(b);dest=out/'originals'/(raw+'.gz');packed=gzip.compress(b,mtime=0);dest.parent.mkdir(exist_ok=True)
 if not dest.exists():dest.write_bytes(packed)
 originals.append({'logicalPath':logical,'path':dest.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(b),'rawSha256':raw})
def findprojects(x,logical):
 if isinstance(x,dict):
  if isinstance(x.get('project'),str):projects[logical+':'+x['project']]=Path(x['project'])
  for k,v in x.items():findprojects(v,logical+'/'+k)
 elif isinstance(x,list):
  for i,v in enumerate(x):findprojects(v,logical+'/'+str(i))
folders=[r/'build/validation123']+list((r/'workspace-engine/build').glob('pickup123*'))+list((r/'workspace-engine/build').glob('controller123-regression-*'))+[r/'workspace-engine/build'/n for n in ['reference123-final','reference123-mesa','reference115','game115-repair','game115-restart','challenge118-task','challenge118-failed','challenge118-image','recovery119-task','opportunity120-task','opportunity121-task','contact122-task']]
for folder in folders:
 for p in sorted(folder.rglob('*')):
  if p.is_file() and p.suffix in ['.json','.jsonl','.xml','.png','.txt','.log']:
   logical=p.relative_to(r).as_posix();preserve(p,logical)
   if p.name=='result.json':findprojects(read(p),logical)
for logical,project in projects.items():
 assert project.is_dir(),project
 for sub in ['game-levels114','game-tasks115','game-references115']:
  for p in sorted((project/sub).rglob('*')):
   if p.is_file() and p.suffix in ['.json','.jsonl','.png']:preserve(p,logical+'/original-project/'+p.relative_to(project).as_posix())
for folder in sorted((r/'build').glob('game123-ui*')):
 if not folder.is_dir():continue
 for p in folder.iterdir():
  if p.is_file():preserve(p,p.relative_to(r).as_posix())
 for sub in ['game-tasks115','game-levels114','game-references115','agency-043','embedded-game']:
  for p in sorted((folder/'project'/sub).rglob('*')):
   if p.is_file() and p.suffix in ['.json','.jsonl','.png','.log']:preserve(p,p.relative_to(r).as_posix())
for pattern in ['*123*.log','ci123*.json','study095-build*.json']:
 for p in sorted((r/'build').glob(pattern)):preserve(p,p.relative_to(r).as_posix())
write(out/'original-evidence.json',{'files':originals,'scope':'Exact pilot, all repeated workflows, regressions, failed-task refusal, restarted task and actual visible chat originals. Private user worlds excluded.'})
sourcePaths={x['path'] for x in read('docs/readmes/122/file-inventory.json')}
for n in subprocess.check_output(['git','diff','--name-only','98d0724b','HEAD'],text=True).splitlines():
 if n.startswith(('workspace-engine/','workspace-desktop/','src/','assets/','.github/')) or n=='build.gradle':sourcePaths.add(n)
sourcePaths.update(['docs/EDITION_123_CONTRACT.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md']);inventory=[]
for n in sorted(sourcePaths):
 b=(r/n).read_bytes()
 try:b=b.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':n,'bytes':len(b),'sha256':sha(b),'hashKind':kind})
write(out/'file-inventory.json',inventory)
note='docs/obsidian/131-full-cost-pickup-choices.md'
modules=[{'id':i,'name':n,'source':s,'note':note} for i,n,s in [('strategy','Explicit pickup choices','src/main/java/agent/PickupStrategy123.java'),('control','Bounded waiting and returning','src/main/java/agent/PickupOpportunity120.java'),('runtime','Original native simulation','src/main/java/runtime/EmbeddedGameRuntime.java'),('task','Durable checked task','workspace-engine/src/main/java/fraymus/workspace/GameTask115.java'),('verify','Action and outcome checks','workspace-engine/src/main/java/fraymus/workspace/GameEvidence123.java'),('cost','Measured complete cost','workspace-engine/src/main/java/fraymus/workspace/GameCost123.java'),('proof','Actual visible review','workspace-desktop/test/game123-integration.cjs')]]
system={'modules':modules,'screenshots':[{'path':'docs/readmes/123/'+n,'title':n} for n in shots],'evidence':[{'path':'docs/readmes/123/'+n} for n in ['actual-ui.json','measurements.json','verification.json','original-evidence.json']]};write(out/'system-map.json',system)
def num(n):return f'{n:.2f}'
useful=[x for x in comparisons if x['enabled']=='full' and ((x['case'].endswith('wait') and x['comparison']=='no-wait') or (x['case'].endswith('return') and x['comparison']=='no-return'))]
findings=' '.join(f"On {x['partition']} / {x['case']}, full versus {x['comparison']} added {num(x['extraPickups']['min'])}–{num(x['extraPickups']['max'])} pickups per run; paired wall-time change was {num(x['elapsedDeltaSeconds']['min'])} to {num(x['elapsedDeltaSeconds']['max'])} seconds." for x in useful)
findings+=' All negative and mixed comparisons remain below. Costs are measured on this host, not promised for another machine.'
values={'FINDINGS':findings,'RESULT_ROWS':'\n'.join(f"| {m['partition']} / {m['case']} | {m['strategy']} | {m['wins']}/{m['repetitions']} | {m['pickups']} | {m['damage']} / {m['deaths']} | {num(m['wallSeconds']['median'])} | {num(m['wallSeconds']['min'])}–{num(m['wallSeconds']['max'])} |" for m in metrics),'COMPARISON_ROWS':'\n'.join(f"| {x['partition']} / {x['case']} | {x['enabled']} vs {x['comparison']} | {num(x['extraPickups']['min'])}–{num(x['extraPickups']['max'])} | {num(x['elapsedDeltaSeconds']['min'])} to {num(x['elapsedDeltaSeconds']['max'])} | {num(x['observedConservativeSecondsPerExtraPickup']) if 'observedConservativeSecondsPerExtraPickup' in x else 'not established'} | {x['assessment']} |" for x in comparisons),'VALIDATION':f"Local checks passed: {len(checks)} distinct Java test methods, 168 desktop checks, 72 complete repeated workflows, 18 prior course executions with no damage, six transformed reference images and four refusal cases per renderer, plus the original reference regression. The actual app completed five requests, recovered its own interrupted Java process, and retained/refused an impossible sixth course. These edition123 regression counts are separate from the 85 executions documented in edition122.",'CHAT_ROWS':'\n'.join(f"| {x['prompt']} | {x['elapsedMs']/1000:.3f} | {x['task']['status']} | {x['child'].get('audit',{}).get('aiPickups','not certified')} |" for x in ui['requests'])}
def fill(s):
 for k,v in values.items():s=s.replace('{{'+k+'}}',v)
 assert '{{' not in s;return s
md=fill((r/'docs/publication123/README-template.md').read_text(encoding='utf8'));text(r/'README-123.md',md);text(r/'README.md',md)
text(r/note,'# Full-cost pickup choices — edition123\n\n[[README-123.md]] records 72 repeated workflows and four explicit pickup strategies. '+findings+'\n\nDefault remains contact122. No automatic value policy. Next: explicit user allowances and uncertainty on new courses, then broader contacts. [[docs/PLAYABLE_LEVEL_CREATION_PLAN.md]]\n')
write(r/'docs/obsidian/131-full-cost-pickup-choices.canvas',{'nodes':[{'id':m['id'],'type':'file','file':m['source'],'x':i%3*430,'y':i//3*250,'width':390,'height':190} for i,m in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']} for i in range(len(modules)-1)]})
html=fill((r/'docs/publication123/guide-template.html').read_text(encoding='utf8'))+'<script id="system-data" type="application/json">'+json.dumps(system)+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory)+'</script><script id="measurements-data" type="application/json">'+json.dumps(measurements)+'</script><script src="guide.js"></script></html>'
text(out/'index.html',html);shutil.copy2(r/'docs/publication123/guide.js',out/'guide.js')
for name,link in [('docs/readmes/INDEX.md','[Edition123 — full-cost pickup choices](../../README-123.md) · [Offline guide](123/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/131-full-cost-pickup-choices.md]] · [[README-123.md]]')]:
 p=r/name;s=p.read_text(encoding='utf-8-sig');text(p,link+'\n\n'+s if not s.startswith(link) else s)
print(json.dumps({'passed':True,'javaChecks':len(checks),'sources':len(inventory),'originalEntries':len(originals),'findings':findings}))
