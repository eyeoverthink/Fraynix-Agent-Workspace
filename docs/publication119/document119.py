from pathlib import Path
import json,hashlib,subprocess,shutil,gzip,statistics,xml.etree.ElementTree as ET
r=Path.cwd();out=r/'docs/readmes/119';out.mkdir(parents=True,exist_ok=True)
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-119']).returncode==1,'Never rewrite a published edition'
def read(p):return json.loads((r/p).read_text(encoding='utf8'))
def write(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes((json.dumps(v,indent=2,ensure_ascii=False)+'\n').encode())
def text(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(s.encode())
def sha(b):return hashlib.sha256(b).hexdigest()
uiroot=r/'build/game119-ui-final';ui=read(uiroot/'result.json');assert ui['passed'] and all(ui[k] for k in ['contactFrame','deferralFrame','collectionFrame','riskFrame'])
scope=read('build/edition119-scope.json')
paths={'development':'controller119-prototype-b','development-first':'controller119-development','development-final':'controller119-final-development','reserved-first':'controller119-reserved','reserved':'controller119-final-reserved','software-reserved':'controller119-mesa-reserved','old-challenge-development':'controller119-final-old-development','old-challenge-reserved':'controller119-final-old-reserved','old-regression':'controller117-119-final-regression','old-wider':'controller117-119-final-wider','old-reserved':'controller117-119-final-reserved'}
datasets={k:read('workspace-engine/build/'+v+'/result.json') for k,v in paths.items()};assert all(x['passed'] for x in datasets.values())
hardware=read('workspace-engine/build/reference119-final/result.json');software=read('workspace-engine/build/reference119-mesa/result.json');assert hardware['passed'] and software['passed']
shots=['old-repair-regression.png','challenge-controls.png','corner-completed.png','contact-recovery.png','contact-native.png','pickup-deferred.png','deferral-native.png','pickup-reconsidered.png','collected-native.png','recovered-challenge.png','verified-decisions.png','declined-pickup.png','clearance-completed.png','challenge-1100.png']
for n in shots:shutil.copy2(uiroot/n,out/n)
shutil.copy2(r/'build/controller119-motion.png',out/'controller-motion.png')
for n,d in datasets.items():write(out/('controller-'+n+'.json'),d)
write(out/'actual-ui.json',ui);write(out/'reference-hardware.json',hardware);write(out/'reference-software.json',software)
checks=set();suites=[]
for folder in sorted((r/'build/validation119').iterdir()):
 if not folder.is_dir():continue
 total=0
 for p in folder.glob('*.xml'):
  e=ET.parse(p).getroot();assert int(e.get('failures','0'))+int(e.get('errors','0'))==0,p
  for c in e.findall('testcase'):
   if c.find('skipped') is None:checks.add(c.get('classname')+'.'+c.get('name'));total+=1
 if total:suites.append({'folder':folder.relative_to(r).as_posix(),'testMethods':total})
assert len(checks)>154
for n,count in [('desktop119.log',155),('desktop119-extra.log',10)]:
 s=(r/'build'/n).read_text(encoding='utf8');assert f'tests {count}' in s and 'fail 0' in s
metrics=[]
for label,d in datasets.items():
 for policy in sorted({t['policy'] for c in d['cases'] for t in c['trials']}):
  ts=[t for c in d['cases'] for t in c['trials'] if t['policy']==policy];cs=[t['final']['controller'] for t in ts]
  m={'partition':label,'policy':policy,'cases':len(ts),'wins':sum(t['won'] for t in ts),'pickups':None if policy=='simple' else sum(c['pickups'] for c in cs),'damage':None if policy=='simple' else sum(c['damage'] for c in cs),'declined':None if policy=='simple' else sum(c.get('pickupSkips',0) for c in cs),'deferrals':sum(c.get('pickupDeferrals',0) for c in cs),'contactAttempts':sum(c.get('contactRecovery119',{}).get('attempts',0) for c in cs),'contactOverrides':sum(c.get('contactOverrides',0) for c in cs),'deaths':sum(t['final']['player']['dead'] for t in ts),'belowCourse':sum(t['final']['player']['y']<-.5 for t in ts),'trialSeconds':[t['elapsedNanos']/1e9 for t in ts],'medianSeconds':statistics.median(t['elapsedNanos']/1e9 for t in ts),'contactChecks':sum(c.get('contactChecks',0) for c in cs),'contactMilliseconds':sum(c.get('contactNanos',0) for c in cs)/1e6,'riskChecks':sum(c.get('pickupRiskChecks',0) for c in cs),'riskMilliseconds':sum(c.get('pickupRiskNanos',0) for c in cs)/1e6};metrics.append(m)
  for t in ts:
   assert any(s['after']['contact117']['contacts'] for s in t['steps'])
   if policy=='recover119':assert t['final']['controller']['contactRecovery119']['attempts']<=3
old=[m for m in metrics if m['partition'] in ['old-regression','old-wider','old-reserved'] and m['policy']=='recover119'];assert sum(m['cases'] for m in old)==24 and sum(m['wins'] for m in old)==24 and sum(m['pickups'] for m in old)==24 and sum(m['damage'] for m in old)==0
dev=next(m for m in metrics if m['partition']=='development-final' and m['policy']=='recover119');assert dev['wins']==6 and dev['damage']==0
prior=[m for m in metrics if m['partition'].startswith('old-challenge') and m['policy']=='recover119'];assert sum(m['wins'] for m in prior)==18 and sum(m['damage'] for m in prior)==0
refs={label:{'cases':len(d['cases']),'simpleWins':sum(c['task']['audit']['simpleControl']=='won' for c in d['cases']),'revisedWins':sum(c['task']['audit']['existingAI']=='won' for c in d['cases']),'refusals':len(d['refusals'])} for label,d in [('hardware',hardware),('software',software)]};assert all(v=={'cases':6,'simpleWins':6,'revisedWins':6,'refusals':4} for v in refs.values())
write(out/'measurements.json',{'controllers':metrics,'scope':'Fresh native sessions; old course groups are regressions; final hardware and software repeat the six originally reserved cases; reserved-first retains the original pre-correction evaluation. Added observation timing and complete trial timing are separate, single-run measurements.'})
v={'passed':True,'javaChecks':sorted(checks),'javaSuites':suites,'desktopChecks':165,'actualChatRequests':len(ui['requests']),'actualAbruptRecovery':True,'actualAiFailureRefused':True,'referenceControllerResults':refs,'referenceCases':{'freshNativeImagesPerRenderer':6,'freshRefusalsPerRenderer':4,'original115ReferenceRegression':True},'controllerCases':metrics,'policyScope':scope['defaultScope'],'modelCalls':sum(x['task']['timing052']['modelCalls'] for x in ui['requests']),'sourceCommit':ui['source'],'scope':'Owned input-only contact recovery and bounded opportunity reconsideration. No physics edits, arbitrary image recognition, LLM training or procedure admission.'};assert len(ui['requests'])==5 and v['modelCalls']==0;write(out/'verification.json',v)
originals=[]
def preserve(p,logical):
 if not p.is_file():return
 b=p.read_bytes();raw=sha(b);dest=out/'originals'/(raw+'.gz');packed=gzip.compress(b,mtime=0);dest.parent.mkdir(exist_ok=True)
 if not dest.exists():dest.write_bytes(packed)
 originals.append({'logicalPath':logical,'path':dest.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(b),'rawSha256':raw})
folders=['build/validation119']+['workspace-engine/build/'+n for n in [*paths.values(),'controller119-baseline','controller119-prototype-a','controller119-old-development','controller119-old-reserved','controller117-119-regression','controller117-119-wider','controller117-119-reserved','reference119-final','reference119-mesa','reference115','game115-repair','game115-restart','challenge118-task','challenge118-failed','challenge118-image','recovery119-task']]
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
for uiFolder in sorted((r/'build').glob('game119-ui*')):
 if not uiFolder.is_dir():continue
 for p in sorted(uiFolder.iterdir()):
  if p.is_file():preserve(p,p.relative_to(r).as_posix())
 for sub in ['game-tasks115','game-levels114','game-references115','agency-043','embedded-game']:
  for p in sorted((uiFolder/'project'/sub).rglob('*')):
   if p.is_file() and p.suffix in ['.json','.jsonl','.png','.log']:preserve(p,p.relative_to(r).as_posix())
for pattern in ['*119*.log','ci119*.json','policy119*.json','edition119-scope.json']:
 for p in sorted((r/'build').glob(pattern)):preserve(p,p.relative_to(r).as_posix())
preserve(r/'build/study095-build.json','build/study095-build.json')
write(out/'original-evidence.json',{'files':originals,'scope':'Exact isolated baseline, failed first prototype, successful development, reserved hardware/software, regressions, source/outcome checks and actual UI/recovery. Raw and compressed SHA-256; private projects and runtime binaries excluded.'})
sourcePaths={x['path'] for x in read('docs/readmes/118/file-inventory.json')}
for n in subprocess.check_output(['git','diff','--name-only','b154e9ea','HEAD'],text=True).splitlines():
 if n.startswith(('workspace-engine/','workspace-desktop/','src/','assets/','.github/')) or n=='build.gradle':sourcePaths.add(n)
sourcePaths.update(['docs/EDITION_119_CONTRACT.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md']);inventory=[]
for n in sorted(sourcePaths):
 b=(r/n).read_bytes()
 try:b=b.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':n,'bytes':len(b),'sha256':sha(b),'hashKind':kind})
write(out/'file-inventory.json',inventory)
note='docs/obsidian/127-contact-recovery-and-reconsideration.md'
modules=[{'id':i,'name':n,'source':s,'note':note} for i,n,s in [('observe','Native contact observation','src/main/java/agent/ContactObserver119.java'),('recover','Bounded input recovery','src/main/java/agent/ContactRecovery119.java'),('opportunity','Finite pickup reconsideration','src/main/java/agent/PowerupPlanner.java'),('control','Composed normal input','src/main/java/agent/GameInput.java'),('runtime','Original native simulation','src/main/java/runtime/EmbeddedGameRuntime.java'),('task','Durable checked task','workspace-engine/src/main/java/fraymus/workspace/GameTask115.java'),('verify','Recovery evidence bounds','workspace-engine/src/main/java/fraymus/workspace/GameEvidence119.java'),('proof','Actual visible recovery','workspace-desktop/test/game119-integration.cjs')]]
system={'modules':modules,'screenshots':[{'path':'docs/readmes/119/'+n,'title':n} for n in shots],'evidence':[{'path':'docs/readmes/119/'+n} for n in ['actual-ui.json','reference-hardware.json','reference-software.json','measurements.json','verification.json','original-evidence.json']]};write(out/'system-map.json',system)
labels=['Existing two-gap repair','Retained corner stall','Pickup reconsideration with abrupt restart','Cramped pickup refusal','Unfinishable challenge'];rows=[]
for label,x in zip(labels,ui['requests']):
 a=x['child'].get('audit',{});rows.append(f"| {label} | {x['elapsedMs']/1000:.3f} s | {a.get('simpleControl','failed')} | {a.get('existingAI','failed')} | {a.get('repairCount',0)} |")
controllers=[f"| {x['partition']} | {x['policy']} | {x['wins']}/{x['cases']} | {x['pickups'] if x['pickups'] is not None else 'n/a'} | {x['damage'] if x['damage'] is not None else 'n/a'} | {x['deaths']} | {x['declined'] if x['declined'] is not None else 'n/a'} | {x['deferrals']} | {x['contactAttempts']} | {x['medianSeconds']:.3f} s |" for x in metrics]
reserved=[m for m in metrics if m['partition']=='reserved'];revised=next(x for x in reserved if x['policy']=='recover119');previous=next(x for x in reserved if x['policy']=='risk118');safe=next(x for x in reserved if x['policy']=='safe117');sw=next(x for x in metrics if x['partition']=='software-reserved' and x['policy']=='recover119')
failed=[c['name'] for c in datasets['reserved']['cases'] if any(t['policy']=='recover119' and not t['won'] for t in c['trials'])]
reservedText=f"Final hardware repeat of the reserved cases: recover119 finishes {revised['wins']}/6, with {revised['pickups']} pickups, {revised['damage']} damage and {revised['deaths']} deaths; risk118 finishes {previous['wins']}/6 and safe117 {safe['wins']}/6. Software repeat: recover119 {sw['wins']}/6, {sw['damage']} damage. "+('Retained new failures: '+', '.join(failed)+'.' if failed else 'No failed revised-controller trial in these six cases; broader behavior remains unproven.')
values={'JAVA_COUNT':str(len(checks)),'CHAT_ROWS':'\n'.join(rows),'CONTROLLER_ROWS':'\n'.join(controllers),'RESERVED_RESULT':reservedText,'DEFAULT_SCOPE':scope['defaultScope'],'NEXT_STEPS':scope['nextSteps'],'COST_RESULT':f"Across the six final development trials, contact observation/recovery checks ran {dev['contactChecks']} times and used {dev['contactMilliseconds']:.3f} ms total; pickup risk checks used {dev['riskMilliseconds']:.3f} ms. Complete revised trials had median {dev['medianSeconds']:.3f} s. These scopes do not establish a whole-app speedup."}
def fill(s):
 for k,v in values.items():s=s.replace('{{'+k+'}}',v)
 assert '{{' not in s;return s
md=fill((r/'docs/publication119/README-template.md').read_text(encoding='utf8'));text(r/'README-119.md',md);text(r/'README.md',md)
text(r/note,'# Contact recovery and reconsideration — edition119\n\n[[README-119.md]] records both retained failures corrected through native input. Six development finishes/five pickups/zero damage. Old18 challenge finishes and old24 finishes/pickups preserved. '+reservedText+'\n\n'+scope['defaultScope']+'\n\nOriginal physics, source/ownership/session/action/image/outcome checks remain. Deferred opportunities are not failed motor lessons. Both actual corrected tasks and abrupt desktop recovery retained. No new training, recognition or procedure admission.\n\n'+scope['nextSteps']+'\n\n[[docs/PLAYABLE_LEVEL_CREATION_PLAN.md]]\n')
write(r/'docs/obsidian/127-contact-recovery-and-reconsideration.canvas',{'nodes':[{'id':m['id'],'type':'file','file':m['source'],'x':i%3*430,'y':i//3*250,'width':390,'height':190} for i,m in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']} for i in range(len(modules)-1)]})
html=fill((r/'docs/publication119/guide-template.html').read_text(encoding='utf8'))+'<script id="system-data" type="application/json">'+json.dumps(system)+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory)+'</script></html>';text(out/'index.html',html)
for name,link in [('docs/readmes/INDEX.md','[Edition119 — contact recovery and pickup reconsideration](../../README-119.md) · [Offline guide](119/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/127-contact-recovery-and-reconsideration.md]] · [[README-119.md]]')]:
 p=r/name;s=p.read_text(encoding='utf-8-sig');text(p,link+'\n\n'+s if not s.startswith(link) else s)
print(json.dumps({'passed':True,'javaChecks':len(checks),'sources':len(inventory),'originalEntries':len(originals),'reserved':reservedText}))
