from pathlib import Path
import json,hashlib,subprocess,shutil,gzip,statistics,xml.etree.ElementTree as ET
r=Path.cwd();out=r/'docs/readmes/118';out.mkdir(parents=True,exist_ok=True)
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-118']).returncode==1,'Never rewrite a published edition'
def write(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes((json.dumps(v,indent=2,ensure_ascii=False)+'\n').encode())
def text(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(v.encode())
def read(p):return json.loads((r/p).read_text(encoding='utf8'))
def sha(b):return hashlib.sha256(b).hexdigest()
uiroot=r/'build/game118-ui';ui=read(uiroot/'result.json');assert ui['passed'] and ui['enemyFrame'] and ui['riskFrame']
paths={'development':'controller118-development','reserved':'controller118-reserved','software-reserved':'controller118-mesa-reserved','old-regression':'controller117-118-regression','old-wider':'controller117-118-wider','old-reserved':'controller117-118-reserved'}
datasets={k:read('workspace-engine/build/'+v+'/result.json') for k,v in paths.items()};assert all(d['passed'] for d in datasets.values())
hardware=read('workspace-engine/build/reference118-final/result.json');software=read('workspace-engine/build/reference118-mesa/result.json');assert hardware['passed'] and software['passed']
shots=['enemy-completed.png','challenge-controls.png','enemy-play.png','enemy-native.png','declined-pickup.png','declined-native.png','recovered-challenge.png','verified-decisions.png','old-repair-regression.png','challenge-1100.png']
for n in shots:shutil.copy2(uiroot/n,out/n)
shutil.copy2(r/'build/controller118-motion.png',out/'controller-motion.png')
shutil.copy2(r/'build/game118-ui-first/declined-pickup.png',out/'first-recovery-display-gap.png')
write(out/'actual-ui.json',ui);write(out/'reference-hardware.json',hardware);write(out/'reference-software.json',software)
for name,d in datasets.items():write(out/('controller-'+name+'.json'),d)
gate=read('workspace-engine/build/controller118-final-gate/result.json');assert gate['passed'] and len(gate['cases'])==12 and all(t['won'] for c in gate['cases'] for t in c['trials'])
checks=set();suites=[]
for folder in sorted((r/'build/validation118').iterdir()):
 if not folder.is_dir():continue
 total=0
 for p in folder.glob('*.xml'):
  e=ET.parse(p).getroot();assert int(e.get('failures','0'))+int(e.get('errors','0'))==0,p
  for c in e.findall('testcase'):
   if c.find('skipped') is None:checks.add(c.get('classname')+'.'+c.get('name'));total+=1
 if total:suites.append({'folder':folder.relative_to(r).as_posix(),'testMethods':total})
assert len(checks)>140
for n,count in [('desktop118.log',155),('desktop118-extra.log',10)]:
 s=(r/'build'/n).read_text(encoding='utf8');assert f'tests {count}' in s and 'fail 0' in s
metrics=[];movementChecks=0;contactChecks=0;stationaryEnemies=[]
for label,d in datasets.items():
 for policy in sorted({t['policy'] for c in d['cases'] for t in c['trials']}):
  ts=[t for c in d['cases'] for t in c['trials'] if t['policy']==policy]
  m={'partition':label,'policy':policy,'cases':len(ts),'wins':sum(t['won'] for t in ts),'pickups':None if policy=='simple' else sum(t['final']['controller']['pickups'] for t in ts),'damage':None if policy=='simple' else sum(t['final']['controller']['damage'] for t in ts),'declined':None if policy=='simple' else sum(t['final']['controller'].get('pickupSkips',0) for t in ts),'deaths':sum(t['final']['player']['dead'] for t in ts),'belowCourse':sum(t['final']['player']['y']<-.5 for t in ts),'trialSeconds':[t['elapsedNanos']/1e9 for t in ts],'medianSeconds':statistics.median(t['elapsedNanos']/1e9 for t in ts),'riskChecks':sum(t['final']['controller'].get('pickupRiskChecks',0) for t in ts),'riskMilliseconds':sum(t['final']['controller'].get('pickupRiskNanos',0) for t in ts)/1e6};metrics.append(m)
  for t in ts:
   assert any(s['after']['contact117']['contacts'] for s in t['steps']);contactChecks+=1
   moved=0
   for e in t['initial']['threats118']['enemies']:
    if any(abs(a['x']-e['x'])>.005 for s in t['steps'] for a in s['after']['threats118']['enemies'] if a['id']==e['id']):movementChecks+=1;moved+=1
    else:stationaryEnemies.append({'partition':label,'policy':policy,'initial':e,'ticks':t['final']['tick'],'playerDead':t['final']['player']['dead'],'scope':'No observed horizontal movement before this trial ended; presence alone is not counted as motion. Original components retain camera-dependent activation.'})
   if t['initial']['threats118']['enemies']:assert moved>0,'At least one actual moving enemy required in each enemy trial'

reg=[m for m in metrics if m['partition'].startswith('old-') and m['policy']=='risk118'];assert sum(m['cases'] for m in reg)==24 and sum(m['wins'] for m in reg)==24 and sum(m['pickups'] for m in reg)==24 and sum(m['damage'] for m in reg)==0
dev=next(m for m in metrics if m['partition']=='development' and m['policy']=='risk118');assert (dev['wins'],dev['pickups'],dev['damage'],dev['declined'])==(12,5,0,7)
reserved=[m for m in metrics if m['partition']=='reserved'];sw=[m for m in metrics if m['partition']=='software-reserved']
referenceResults={label:{'cases':len(d['cases']),'simpleWins':sum(c['task']['audit']['simpleControl']=='won' for c in d['cases']),'revisedWins':sum(c['task']['audit']['existingAI']=='won' for c in d['cases']),'refusals':len(d['refusals'])} for label,d in [('hardware',hardware),('software',software)]};assert all(v=={'cases':6,'simpleWins':6,'revisedWins':6,'refusals':4} for v in referenceResults.values())
write(out/'measurements.json',{'controllers':metrics,'nativeMovementChecks':movementChecks,'nativeContactChecks':contactChecks,'stationaryEnemyRecords':stationaryEnemies,'scope':'One fresh native trial per layout/policy. Simple collision counters unavailable. Risk timing measures only added checks; trial time includes native startup, simulation, rendering and bridge. Software repeats six reserved layouts, not additional unfamiliar cases.'})
v={'passed':True,'javaChecks':sorted(checks),'javaSuites':suites,'desktopChecks':165,'actualChatRequests':len(ui['requests']),'actualAbruptRecovery':True,'actualAiFailureRefused':True,'referenceControllerResults':referenceResults,'referenceCases':{'freshNativeImagesPerRenderer':6,'freshRefusalsPerRenderer':4,'original115ReferenceRegression':True},'controllerCases':metrics,'policyScope':'All cohort comparisons request explicit policies. Those policies remain unchanged after reserved evaluation. General default was retained as safe117; final-default suites and actual UI check the final explicit challenge selection and stronger gates. Older runtime proofs retain their original identities.','modelCalls':sum(x['task']['timing052']['modelCalls'] for x in ui['requests']),'sourceCommit':ui['source'],'scope':'Bounded source-component challenges, ordinary input-only risk decisions and measured outcomes. No arbitrary image recognition, model training, general safety or procedure-bank admission.'};assert v['modelCalls']==0 and len(ui['requests'])==4;write(out/'verification.json',v)
originals=[]
def preserve(p,logical):
 if not p.is_file():return
 b=p.read_bytes();raw=sha(b);dest=out/'originals'/(raw+'.gz');packed=gzip.compress(b,mtime=0);dest.parent.mkdir(exist_ok=True)
 if not dest.exists():dest.write_bytes(packed)
 originals.append({'logicalPath':logical,'path':dest.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(b),'rawSha256':raw})
folders=['build/validation118']+['workspace-engine/build/'+n for n in [*paths.values(),'controller118-final-gate','controller118-baseline','controller118-prototype-a','controller118-prototype-b','reference118-final','reference118-mesa','reference115','game115-repair','game115-restart','challenge118-task','challenge118-failed','challenge118-image']]
projects={}
for folder in folders:
 for p in sorted((r/folder).rglob('*')):
  if p.is_file() and p.suffix in ['.json','.jsonl','.xml','.png','.txt','.log']:
   preserve(p,p.relative_to(r).as_posix())
   if p.name=='result.json':
    report=json.loads(p.read_text(encoding='utf8'));project=report.get('project')
    if not project and p.with_name('project.txt').exists():project=p.with_name('project.txt').read_text(encoding='utf8').strip()
    if project:projects[p.relative_to(r).as_posix()]=Path(project)
for logical,project in projects.items():
 assert project.is_dir(),project
 for sub in ['game-levels114','game-tasks115','game-references115']:
  for p in sorted((project/sub).rglob('*')):
   if p.is_file() and p.suffix in ['.json','.jsonl','.png']:preserve(p,logical+'/original-project/'+p.relative_to(project).as_posix())
for uiFolder in [uiroot,r/'build/game118-ui-first']:
 for p in sorted(uiFolder.iterdir()):
  if p.is_file():preserve(p,p.relative_to(r).as_posix())
 for sub in ['game-tasks115','game-levels114','game-references115','agency-043','embedded-game']:
  for p in sorted((uiFolder/'project'/sub).rglob('*')):
   if p.is_file() and p.suffix in ['.json','.jsonl','.png','.log']:preserve(p,p.relative_to(r).as_posix())
preserve(r/'build/ui118-first-visual-review.json','build/ui118-first-visual-review.json')
for p in sorted((r/'build').glob('*118*.log')):preserve(p,p.relative_to(r).as_posix())
for p in sorted((r/'build').glob('ci118*.json')):preserve(p,p.relative_to(r).as_posix())
for n in ['build/study095-build.json']:preserve(r/n,n)
write(out/'original-evidence.json',{'files':originals,'scope':'Exact isolated test evidence including original baselines, both unsuccessful prototypes, compiler failure, final cohorts and actual UI/recovery. Raw and compressed SHA-256. No private projects, credentials, weights or runtime binaries.'})
sourcePaths={x['path'] for x in read('docs/readmes/117/file-inventory.json')}
for n in subprocess.check_output(['git','diff','--name-only','757f8d9d','HEAD'],text=True).splitlines():
 if n.startswith(('workspace-engine/','workspace-desktop/','src/','assets/','.github/')) or n=='build.gradle':sourcePaths.add(n)
sourcePaths.update(['docs/EDITION_118_CONTRACT.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md'])
inventory=[]
for n in sorted(sourcePaths):
 b=(r/n).read_bytes()
 try:b=b.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':n,'bytes':len(b),'sha256':sha(b),'hashKind':kind})
write(out/'file-inventory.json',inventory)
note='docs/obsidian/126-risk-aware-game-control.md'
modules=[{'id':i,'name':n,'source':s,'note':note} for i,n,s in [('construct','Verified enemy and obstacle construction','workspace-engine/src/main/java/fraymus/workspace/GameChallenge118.java'),('observe','Native support and threat observations','src/main/java/agent/PickupObserver118.java'),('decide','Bounded optional pickup decisions','src/main/java/agent/PickupSafety118.java'),('control','Ordinary game input','src/main/java/agent/GameInput.java'),('runtime','Original native simulation','src/main/java/runtime/EmbeddedGameRuntime.java'),('task','Durable verified challenge task','workspace-engine/src/main/java/fraymus/workspace/GameTask115.java'),('chat','Verified chat summary','workspace-engine/src/main/java/fraymus/workspace/GameSummary118.java'),('proof','Actual app and restart','workspace-desktop/test/game118-integration.cjs')]]
system={'modules':modules,'screenshots':[{'path':'docs/readmes/118/'+n,'title':n} for n in shots],'evidence':[{'path':'docs/readmes/118/'+n} for n in ['actual-ui.json','reference-hardware.json','reference-software.json','measurements.json','verification.json','original-evidence.json']]};write(out/'system-map.json',system)
rows=[]
for label,x in zip(['Existing two-gap repair','Enemy and steps','Cramped pickup with abrupt restart','Unfinishable challenge'],ui['requests']):
 a=x['child'].get('audit',{});rows.append(f"| {label} | {x['elapsedMs']/1000:.3f} s | {a.get('simpleControl','failed')} | {a.get('existingAI','failed')} | {a.get('repairCount',0)} |")
controllerRows=[f"| {x['partition']} | {x['policy']} | {x['wins']}/{x['cases']} | {x['pickups'] if x['pickups'] is not None else 'n/a'} | {x['damage'] if x['damage'] is not None else 'n/a'} | {x['deaths']} | {x['belowCourse']} | {x['declined'] if x['declined'] is not None else 'n/a'} | {x['medianSeconds']:.3f} s |" for x in metrics]
risk=next(m for m in reserved if m['policy']=='risk118');old=next(m for m in reserved if m['policy']=='safe117');softwareRisk=next(m for m in sw if m['policy']=='risk118')
failed=[c['name'] for c in datasets['reserved']['cases'] if any(t['policy']=='risk118' and not t['won'] for t in c['trials'])]
reservedText=f"Reserved hardware: risk118 finishes {risk['wins']}/6 with {risk['damage']} damage events and {risk['deaths']} death; safe117 finishes {old['wins']}/6. Software repeat: risk118 {softwareRisk['wins']}/6 with {softwareRisk['damage']} damage events. "+('Retained new-controller failures: '+', '.join(failed)+'.' if failed else 'No failed risk118 trial in this reserved cohort; broader behavior remains unproven.')
values={'JAVA_COUNT':str(len(checks)),'CHAT_ROWS':'\n'.join(rows),'CONTROLLER_ROWS':'\n'.join(controllerRows),'RESERVED_RESULT':reservedText,'COST_RESULT':f"Across twelve development trials, the added risk checks ran {dev['riskChecks']} times and used {dev['riskMilliseconds']:.3f} ms total; complete risk118 trials had median {dev['medianSeconds']:.3f} s. These are separate scopes and must not be treated as whole-app speedups."}
def fill(s):
 for k,v in values.items():s=s.replace('{{'+k+'}}',v)
 assert '{{' not in s;return s
readme=fill((r/'docs/publication118/README-template.md').read_text(encoding='utf8'));text(r/'README-118.md',readme);text(r/'README.md',readme)
text(r/note,'# Risk-aware game control — edition118\n\n[[README-118.md]] connects declared original enemies and obstacles to the durable Java chat task. Development12/12 finishes versus9/12 safe117; five pickups and seven declined opportunities, zero damage. Original24 regressions preserve finishes and pickups. '+reservedText+'\n\nNative physics, source/candidate checks, ordinary inputs and original trial evidence remain authoritative. A declined opportunity is not a failed learning episode. Actual desktop recovery and failed-AI refusal are retained. No image-recognition expansion, model training or procedure admission.\n\nNext: contact/displacement recovery for the retained gap-to-step stall, bounded opportunity reconsideration, broader challenge data and explicit image components. [[docs/PLAYABLE_LEVEL_CREATION_PLAN.md]]\n')
write(r/'docs/obsidian/126-risk-aware-game-control.canvas',{'nodes':[{'id':m['id'],'type':'file','file':m['source'],'x':i%3*430,'y':i//3*250,'width':390,'height':190} for i,m in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']} for i in range(len(modules)-1)]})
html=fill((r/'docs/publication118/guide-template.html').read_text(encoding='utf8'))+'<script id="system-data" type="application/json">'+json.dumps(system)+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory)+'</script></html>';text(out/'index.html',html)
for name,link in [('docs/readmes/INDEX.md','[Edition118 — enemies, obstacles and pickup decisions](../../README-118.md) · [Offline guide](118/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/126-risk-aware-game-control.md]] · [[README-118.md]]')]:
 p=r/name;s=p.read_text(encoding='utf-8-sig');text(p,link+'\n\n'+s if not s.startswith(link) else s)
print(json.dumps({'passed':True,'javaChecks':len(checks),'sources':len(inventory),'originalEntries':len(originals),'reserved':reservedText}))
