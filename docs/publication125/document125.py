from pathlib import Path
import json,hashlib,subprocess,shutil,gzip,xml.etree.ElementTree as ET
r=Path.cwd();out=r/'docs/readmes/125';out.mkdir(parents=True,exist_ok=True)
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-125']).returncode==1

def read(p):return json.loads((r/p).read_text(encoding='utf8'))
def text(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8')
def write(p,v):text(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
def sha(b):return hashlib.sha256(b).hexdigest()
datasets={k:read(f'workspace-engine/build/cost125-{k}/result.json') for k in ['development','reserved','software']}
assert [len(datasets[k]['rows']) for k in datasets]==[26,14,13]
assert all(d['passed'] for d in datasets.values())
assert read('build/pipeline125.json')['passed']
uiroot=r/'build/game125-ui-final';ui=read(uiroot/'result.json');assert ui['passed'] and len(ui['requests'])==15
shots=['decision-review.png','insufficient-comparisons.png','recovered-cost.png','saved-review-reopened.png','failed-cost-review.png','review-1100.png']
for name in shots:shutil.copy2(uiroot/name,out/name)
shutil.copy2(r/'build/cost-review125.png',out/'cost-review.png')
for key,data in datasets.items():write(out/('study-'+key+'.json'),data)
write(out/'actual-ui.json',ui)
checks=set();suites=[]
for folder in sorted((r/'build/validation125').iterdir()):
 if not folder.is_dir():continue
 count=0
 for p in folder.glob('*.xml'):
  e=ET.parse(p).getroot();assert int(e.get('failures','0'))+int(e.get('errors','0'))==0,p
  for case in e.findall('testcase'):
   if case.find('skipped') is None:checks.add(case.get('classname')+'.'+case.get('name'));count+=1
 suites.append({'name':folder.name,'executions':count})
for name,count in [('desktop125-final.log',158),('desktop125-extra.log',10)]:
 s=(r/'build'/name).read_text(encoding='utf8');assert f'tests {count}' in s and 'fail 0' in s
assert 'BUILD SUCCESSFUL' in (r/'build/launcher125.log').read_text(encoding='utf8')
regressions={p.parent.name:read(p) for p in (r/'workspace-engine/build').glob('controller125-regression-*/result.json')}
trials=[t for d in regressions.values() for c in d['cases'] for t in c['trials']]
assert len(trials)==18 and all(t['won'] and t['final']['controller']['damage']==0 for t in trials)
refs=read('workspace-engine/build/reference125-final/result.json');assert refs['passed'];write(out/'reference-hardware.json',refs)
rows=[]
for partition,data in datasets.items():
 for entry in data['reviews']:
  if entry['role']!='final':continue
  report=entry['report'];rows.append({'partition':partition,'case':entry['case'],'decision':report['decision'],'metrics':report['metrics'],'reviewSeconds':report['reviewSeconds'],'report':report})
measurements={'rows':rows,'workflows':53,'development':26,'reserved':14,'software':13,'scope':'Original decisions compared against later explicit trials. Three per strategy. Non-randomized complete first-outcome ranges; recovered clocks and later checks stay distinct.'}
write(out/'measurements.json',measurements)
v={'passed':True,'javaChecks':sorted(checks),'javaSuites':suites,'desktopChecks':168,'actualChatRequests':15,'actualAbruptRecovery':True,'actualAiFailureRefused':True,'regressionExecutions':18,'studyRows':53,'measurements':measurements,'referenceControllerResults':{'hardware':{'cases':len(refs['cases']),'refusals':len(refs['refusals'])}},'launcher':{'passed':True,'scope':'Isolated PrepareOnly Offline and final package receipt verified.'},'sourceCommit':ui['source'],'modelCalls':sum(x['task']['timing052']['modelCalls'] for x in ui['requests']),'scope':'Java measured decision review. Original GameChoices124/GameTask115/native contact122 unchanged; no new policy promotion, general transfer or model training.'}
assert len(checks)>=200 and v['modelCalls']==0;write(out/'verification.json',v)
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
folders=[r/'build/validation125']+list((r/'workspace-engine/build').glob('cost125*'))+list((r/'workspace-engine/build').glob('controller125-regression-*'))+[r/'workspace-engine/build'/n for n in ['reference125-final','reference115','game115-repair','game115-restart','challenge118-task','challenge118-failed','challenge118-image','recovery119-task','opportunity120-task','opportunity121-task','contact122-task','pickup123-task']]
for folder in folders:
 for p in sorted(folder.rglob('*')):
  if p.is_file() and p.suffix in ['.json','.jsonl','.xml','.png','.txt','.log']:
   preserve(p,p.relative_to(r).as_posix())
   if p.name in ['result.json','legacy-failure.json']:findprojects(read(p))
for project in projects:
 assert project.is_dir(),project
 for sub in ['game-levels114','game-tasks115','game-choices124','game-costs125','game-reviews125','game-references115']:
  for p in sorted((project/sub).rglob('*')):
   if p.is_file() and p.suffix in ['.json','.jsonl','.png']:preserve(p,'original-projects/'+project.name+'/'+p.relative_to(project).as_posix())
for folder in sorted((r/'build').glob('game125-ui*')):
 if not folder.is_dir():continue
 for p in folder.iterdir():
  if p.is_file():preserve(p,p.relative_to(r).as_posix())
 for sub in ['game-tasks115','game-choices124','game-costs125','game-reviews125','game-levels114','game-references115','agency-043','embedded-game']:
  for p in sorted((folder/'project'/sub).rglob('*')):
   if p.is_file() and p.suffix in ['.json','.jsonl','.png','.log']:preserve(p,p.relative_to(r).as_posix())
for pattern in ['*125*.log','ci125*.json','pipeline125*.json','final-recheck125.json','validation-source125.json','study095-build*.json']:
 for p in sorted((r/'build').glob(pattern)):preserve(p,p.relative_to(r).as_posix())
write(out/'original-evidence.json',{'files':originals,'scope':'Exact initial development pilot, final repeated workflows, regressions, failed-task refusal, restart and actual chat originals. Private worlds excluded.'})
sourcePaths={x['path'] for x in read('docs/readmes/124/file-inventory.json')}
for n in subprocess.check_output(['git','diff','--name-only','63db5798','HEAD'],text=True).splitlines():
 if n.startswith(('workspace-engine/','workspace-desktop/','src/','assets/','.github/')) or n=='build.gradle':sourcePaths.add(n)
sourcePaths.update(['docs/EDITION_125_CONTRACT.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md','docs/INTEGRATED_AGENT_ROADMAP.md']);inventory=[]
for n in sorted(sourcePaths):
 b=(r/n).read_bytes()
 try:b=b.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':n,'bytes':len(b),'sha256':sha(b),'hashKind':kind})
write(out/'file-inventory.json',inventory)

note='docs/obsidian/133-measured-decision-review.md'
modules=[{'id':i,'name':n,'source':s,'note':note} for i,n,s in [('cost','Durable operation costs','workspace-engine/src/main/java/fraymus/workspace/TaskCost125.java'),('review','Independent decision review','workspace-engine/src/main/java/fraymus/workspace/GameCosts125.java'),('math','Measured comparison','workspace-engine/src/main/java/fraymus/workspace/GameCostReview125.java'),('runtime','Original native simulation','src/main/java/runtime/EmbeddedGameRuntime.java'),('proof','Actual visible review','workspace-desktop/test/game125-integration.cjs')]]
system={'modules':modules,'screenshots':[{'path':'docs/readmes/125/'+name,'title':name.removesuffix('.png').replace('-',' ')} for name in shots],'evidence':[{'path':'docs/readmes/125/'+name} for name in ['actual-ui.json','measurements.json','verification.json','original-evidence.json']]};write(out/'system-map.json',system)
def number(n):return 'unestablished' if n is None else f'{n:.2f}'
findings=' '.join(f"{x['partition']}/{x['case']}: {x['metrics']['missedOpportunity']}." for x in rows)
renderer=datasets['development']['rows'][0]['measurement']['final']['renderer']
values={'FINDINGS':findings,'ENVIRONMENT':f"Hardware renderer: {renderer['name']}. Java 21. Isolated study owner: four processors, 1024 MiB heap; native child: four processors, 512 MiB. Actual chat owner: two processors, 512 MiB. Software rendering has its own context. These are measurements of this installation, not a benchmark of the user's newer 5070 computer.",'RESULT_ROWS':'\n'.join(f"| {x['partition']} / {x['case']} | {x['decision']['selected']} | {x['metrics']['directSamples']} / {x['metrics']['fullSamples']} | {number(x['metrics'].get('benefitLower'))} | {number(x['metrics'].get('predictedSecondsPerPickup'))} | {number(x['metrics'].get('observedSecondsPerPickupUpper'))} | {x['metrics']['missedOpportunity']} |" for x in rows),'VALIDATION':f"Local validation includes {len(checks)} distinct Java methods across retained invocations, 168 desktop checks, 53 final native workflows, 18 prior courses finishing without damage, six transformed reference images/four refusals and the original reference regression. Fifteen actual chat requests include abrupt owned-process recovery, stored-review reopening and an honestly refused failed course. Original observations, negative cases and complete costs remain available.",'CHAT_ROWS':'\n'.join(f"| {x['prompt']} | {x['elapsedMs']/1000:.2f} | {x['task']['status']} |" for x in ui['requests'])}
md=(r/'docs/publication125/README-template.md').read_text(encoding='utf8')
for key,value in values.items():md=md.replace('{{'+key+'}}',value)
assert '{{' not in md;text(r/'README-125.md',md);text(r/'README.md',md)
text(r/note,'# Measured decision review — edition125\n\n[[README-125.md]] retains original predictions and independent later outcomes. '+findings+'\n\nNext: whole-app local-model selection, procedure lifecycle and observation-to-artifact integration. [[docs/INTEGRATED_AGENT_ROADMAP.md]]\n')
write(r/'docs/obsidian/133-measured-decision-review.canvas',{'nodes':[{'id':m['id'],'type':'file','file':m['source'],'x':i%3*430,'y':i//3*250,'width':390,'height':190} for i,m in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']} for i in range(len(modules)-1)]})
html=(r/'docs/publication125/guide-template.html').read_text(encoding='utf8')+'<script id="system-data" type="application/json">'+json.dumps(system)+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory)+'</script><script id="measurements-data" type="application/json">'+json.dumps(measurements)+'</script><script src="guide.js"></script></html>'
text(out/'index.html',html);shutil.copy2(r/'docs/publication125/guide.js',out/'guide.js')
for name,link in [('docs/readmes/INDEX.md','[Edition125 — measured decision review](../../README-125.md) · [Offline guide](125/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/133-measured-decision-review.md]] · [[README-125.md]]')]:
 p=r/name;s=p.read_text(encoding='utf-8-sig');text(p,link+'\n\n'+s if not s.startswith(link) else s)
print(json.dumps({'passed':True,'javaChecks':len(checks),'sources':len(inventory),'originalEntries':len(originals),'findings':findings}))
