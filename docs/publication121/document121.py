from pathlib import Path
import json,hashlib,subprocess,shutil,gzip,statistics,xml.etree.ElementTree as ET
r=Path.cwd();out=r/'docs/readmes/121';out.mkdir(parents=True,exist_ok=True)
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-121']).returncode==1,'Never rewrite a published edition'
def read(p):return json.loads((r/p).read_text(encoding='utf8'))
def write(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes((json.dumps(v,indent=2,ensure_ascii=False)+'\n').encode())
def text(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(s.encode())
def sha(b):return hashlib.sha256(b).hexdigest()
uiroot=r/'build/game121-ui-reviewed';ui=read(uiroot/'result.json');assert ui['passed'] and all(ui[k] for k in ['returnFrame','collectionFrame','riskFrame'])
paths={'development':'controller121-development-final','reserved':'controller121-reserved','software-reserved':'controller121-mesa-reserved'}
for part in ['old119-development','old119-reserved','old118-development','old118-reserved','new','early-probes']:paths['regression-'+part]='controller121-old-'+part
paths['regression-old120-reserved']='controller121-old120-reserved'
for part in ['regression','wider','reserved']:paths['regression-old117-'+part]='controller121-old117-'+part
datasets={k:read('workspace-engine/build/'+v+'/result.json') for k,v in paths.items()};assert all(x['passed'] for x in datasets.values())
hardware=read('workspace-engine/build/reference121-final/result.json');software=read('workspace-engine/build/reference121-mesa/result.json');assert hardware['passed'] and software['passed']
shots=['old-repair-regression.png','challenge-controls.png','return-native.png','return-live.png','collected-native.png','collected-after-return.png','return-completed.png','reversal-completed.png','verified-return.png','declined-pickup.png','gap-refused.png','challenge-1100.png']
for n in shots:shutil.copy2(uiroot/n,out/n)
shutil.copy2(r/'build/controller121-motion.png',out/'controller-motion.png')
for n,d in datasets.items():write(out/('controller-'+n+'.json'),d)
write(out/'actual-ui.json',ui);write(out/'reference-hardware.json',hardware);write(out/'reference-software.json',software)
checks=set();suites=[]
for folder in sorted((r/'build/validation121').iterdir()):
 if not folder.is_dir():continue
 total=0
 for p in folder.glob('*.xml'):
  e=ET.parse(p).getroot();assert int(e.get('failures','0'))+int(e.get('errors','0'))==0,p
  for c in e.findall('testcase'):
   if c.find('skipped') is None:checks.add(c.get('classname')+'.'+c.get('name'));total+=1
 suites.append({'name':folder.name,'executions':total})
assert len(checks)>=174
assert read('build/launcher121-check/result.json')['passed']
assert 'BUILD SUCCESSFUL' in (r/'build/launcher121.log').read_text(encoding='utf-8')
for n,count in [('desktop121-final.log',158),('desktop121-extra.log',10)]:
 s=(r/'build'/n).read_text(encoding='utf8');assert f'tests {count}' in s and 'fail 0' in s
metrics=[]
for label,d in datasets.items():
 for policy in sorted({t['policy'] for c in d['cases'] for t in c['trials']}):
  ts=[t for c in d['cases'] for t in c['trials'] if t['policy']==policy];cs=[t['final']['controller'] for t in ts];ops=[c.get('opportunity120',{}) for c in cs]
  metrics.append({'partition':label,'policy':policy,'cases':len(ts),'wins':sum(t['won'] for t in ts),'pickups':None if policy=='simple' else sum(c['pickups'] for c in cs),'damage':None if policy=='simple' else sum(c['damage'] for c in cs),'deaths':sum(t['final']['player']['dead'] for t in ts),'waitTicks':sum(o.get('waitTicks',0) for o in ops),'revisitTicks':sum(o.get('revisitTicks',0) for o in ops),'revisitStarts':sum(o.get('revisitStarts',0) for o in ops),'reversals':sum(o.get('motion121',{}).get('reversals',0) for o in ops),'forecastRefusals':sum(o.get('motion121',{}).get('forecastRefusals',0) for o in ops),'opportunityChecks':sum(c.get('opportunityChecks',0) for c in cs),'opportunityMilliseconds':sum(c.get('opportunityNanos',0) for c in cs)/1e6,'trialSeconds':[t['elapsedNanos']/1e9 for t in ts],'medianSeconds':statistics.median(t['elapsedNanos']/1e9 for t in ts)})
old=[m for m in metrics if m['partition'].startswith('regression-')];assert sum(m['wins'] for m in old)==71 and sum(m['damage'] for m in old)==0
assert sum(m['pickups'] for m in old if 'old117' in m['partition'])==24
dev=next(m for m in metrics if m['partition']=='development' and m['policy']=='revisit121');assert dev['wins']==6 and dev['pickups']==4 and dev['damage']==0
case=next(c for c in datasets['development']['cases'] if c['name']=='late-room');benefit=[]
for t in case['trials']:
 if t['policy']=='simple':continue
 c=t['final']['controller'];assert t['won'] and c['damage']==0 and c['pickups']==(1 if t['policy']=='revisit121' else 0)
 benefit.append({'policy':t['policy'],'win':t['won'],'pickups':c['pickups'],'damage':c['damage'],'ticks':t['final']['tick'],'returnTicks':c['opportunity120']['revisitTicks'],'elapsedSeconds':t['elapsedNanos']/1e9})
refs={label:{'cases':len(d['cases']),'simpleWins':sum(c['task']['audit']['simpleControl']=='won' for c in d['cases']),'revisedWins':sum(c['task']['audit']['existingAI']=='won' for c in d['cases']),'refusals':len(d['refusals'])} for label,d in [('hardware',hardware),('software',software)]};assert all(v=={'cases':6,'simpleWins':6,'revisedWins':6,'refusals':4} for v in refs.values())
write(out/'measurements.json',{'controllers':metrics,'returnAblation':benefit,'scope':'Fresh isolated native trials; reserved evaluated after policy fixed. Software repeats those cases. Single-run wall costs include rendering, physics and bridge work.'})
v={'passed':True,'javaChecks':sorted(checks),'javaSuites':suites,'desktopChecks':168,'actualChatRequests':len(ui['requests']),'actualAbruptRecovery':True,'actualAiFailureRefused':True,'referenceControllerResults':refs,'referenceCases':{'freshNativeImagesPerRenderer':6,'freshRefusalsPerRenderer':4,'original115ReferenceRegression':True},'controllerCases':metrics,'returnAblation':benefit,'modelCalls':sum(x['task']['timing052']['modelCalls'] for x in ui['requests']),'sourceCommit':ui['source'],'scope':'Bounded observed enemy motion and input-only pickup return; actual collection and full costs. Two development enemy-contact deaths remain. No physics edits, general recognition, LLM training or new procedure admission.'};assert len(ui['requests'])==5 and v['modelCalls']==0;write(out/'verification.json',v)
originals=[]
def preserve(p,logical):
 if not p.is_file():return
 b=p.read_bytes();raw=sha(b);dest=out/'originals'/(raw+'.gz');packed=gzip.compress(b,mtime=0);dest.parent.mkdir(exist_ok=True)
 if not dest.exists():dest.write_bytes(packed)
 originals.append({'logicalPath':logical,'path':dest.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(b),'rawSha256':raw})
folders=[r/'build/validation121',r/'build/diagnostics121',r/'build/launcher121-check']+list((r/'workspace-engine/build').glob('controller121*'))+[r/'workspace-engine/build'/n for n in ['reference121-final','reference121-mesa','reference115','game115-repair','game115-restart','challenge118-task','challenge118-failed','challenge118-image','recovery119-task','opportunity120-task','opportunity121-task']]
projects={}
for folder in folders:
 for p in sorted(folder.rglob('*')):
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
for folder in sorted((r/'build').glob('game121-ui*')):
 if not folder.is_dir():continue
 for p in folder.iterdir():
  if p.is_file():preserve(p,p.relative_to(r).as_posix())
 for sub in ['game-tasks115','game-levels114','game-references115','agency-043','embedded-game']:
  for p in sorted((folder/'project'/sub).rglob('*')):
   if p.is_file() and p.suffix in ['.json','.jsonl','.png','.log']:preserve(p,p.relative_to(r).as_posix())
for pattern in ['*121*.log','ci121*','policy121*.json','study095-build*.json']:
 for p in sorted((r/'build').glob(pattern)):
  if p.is_file():preserve(p,p.relative_to(r).as_posix())
write(out/'original-evidence.json',{'files':originals,'scope':'Exact CI failure, baseline, unsuccessful return prototypes, development/ablation, reserved hardware/software, regressions and actual chat/recovery originals. Raw and compressed hashes; private user worlds excluded.'})
sourcePaths={x['path'] for x in read('docs/readmes/120/file-inventory.json')}
for n in subprocess.check_output(['git','diff','--name-only','6999d1a3','HEAD'],text=True).splitlines():
 if n.startswith(('workspace-engine/','workspace-desktop/','src/','assets/','.github/')) or n=='build.gradle':sourcePaths.add(n)
sourcePaths.update(['docs/EDITION_121_CONTRACT.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md','scripts/Start-Agent-Workspace.ps1']);inventory=[]
for n in sorted(sourcePaths):
 b=(r/n).read_bytes()
 try:b=b.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':n,'bytes':len(b),'sha256':sha(b),'hashKind':kind})
write(out/'file-inventory.json',inventory)
note='docs/obsidian/129-enemy-reversals-and-return-value.md'
modules=[{'id':i,'name':n,'source':s,'note':note} for i,n,s in [('motion','Observed enemy direction','src/main/java/agent/OpportunityMotion121.java'),('opportunity','Bounded return and waiting','src/main/java/agent/PickupOpportunity120.java'),('control','Ordinary player input','src/main/java/agent/GameInput.java'),('runtime','Original native simulation','src/main/java/runtime/EmbeddedGameRuntime.java'),('task','Durable checked task','workspace-engine/src/main/java/fraymus/workspace/GameTask115.java'),('verify','Motion receipt verifier','workspace-engine/src/main/java/fraymus/workspace/GameEvidence121.java'),('proof','Actual visible review','workspace-desktop/test/game121-integration.cjs')]]
system={'modules':modules,'screenshots':[{'path':'docs/readmes/121/'+n,'title':n} for n in shots],'evidence':[{'path':'docs/readmes/121/'+n} for n in ['actual-ui.json','reference-hardware.json','reference-software.json','measurements.json','verification.json','original-evidence.json']]};write(out/'system-map.json',system)
rr=next(x for x in metrics if x['partition']=='reserved' and x['policy']=='revisit121');pr=next(x for x in metrics if x['partition']=='reserved' and x['policy']=='opportunity120');sw=next(x for x in metrics if x['partition']=='software-reserved' and x['policy']=='revisit121')
assert rr['wins']>=pr['wins'] and rr['deaths']<=pr['deaths'],'No promotion with worse reserved survival'
reservedBenefits=[]
for c in datasets['reserved']['cases']:
 ts={t['policy']:t for t in c['trials']};new=ts['revisit121'];no=ts['observe121'];nc=new['final']['controller'];oc=no['final']['controller']
 if nc['opportunity120']['revisitStarts'] and new['won'] and no['won'] and nc['pickups']>oc['pickups'] and nc['damage']==0:
  reservedBenefits.append({'case':c['name'],'newPickups':nc['pickups'],'withoutReturns':oc['pickups'],'newTicks':new['final']['tick'],'withoutReturnTicks':no['final']['tick']})
write(out/'reserved-return-benefit.json',reservedBenefits);v['reservedReturnBenefits']=reservedBenefits;write(out/'verification.json',v)
reservedText=f"Six reserved courses: revisit121 finishes {rr['wins']}/6 with {rr['pickups']} pickups, {rr['damage']} damage and {rr['deaths']} deaths. Previous120 finishes {pr['wins']}/6 with {pr['pickups']} pickups, {pr['damage']} damage and {pr['deaths']} deaths. Software repeat: {sw['wins']}/6, {sw['pickups']} pickups, {sw['damage']} damage, {sw['deaths']} deaths. No tuning against these reserved outcomes."
rows=[]
for x in ui['requests']:
 a=x['child'].get('audit',{});rows.append(f"| {x['prompt']} | {x['elapsedMs']/1000:.3f} s | {x['task']['status']} | {a.get('aiPickups','not certified')} |")
controllers=[f"| {x['partition']} | {x['policy']} | {x['wins']}/{x['cases']} | {x['pickups'] if x['pickups'] is not None else 'n/a'} | {x['deaths']} | {x['revisitTicks']} | {x['medianSeconds']:.3f} s |" for x in metrics if not x['partition'].startswith('regression-')]
values={'JAVA_COUNT':str(len(checks)),'CHAT_ROWS':'\n'.join(rows),'CONTROLLER_ROWS':'\n'.join(controllers),'RESERVED_RESULT':reservedText,'RESERVED_BENEFIT_ROWS':'\n'.join(f"| {x['case']} | {x['newPickups']} | {x['withoutReturns']} | {x['newTicks']-x['withoutReturnTicks']} |" for x in reservedBenefits),'BENEFIT_ROWS':'\n'.join(f"| {x['policy']} | {x['pickups']} | {x['ticks']} | {x['returnTicks']} | {x['elapsedSeconds']:.3f} s |" for x in benefit),'COST_RESULT':f"Across eight development trials,121 used {dev['opportunityChecks']} opportunity checks and {dev['opportunityMilliseconds']:.3f} ms in that decision code. Complete trial median was {dev['medianSeconds']:.3f} s. Timing is from single runs, not a statistical speed claim."}
def fill(s):
 for k,v in values.items():s=s.replace('{{'+k+'}}',v)
 assert '{{' not in s;return s
md=fill((r/'docs/publication121/README-template.md').read_text(encoding='utf8'));text(r/'README-121.md',md);text(r/'README.md',md)
text(r/note,'# Enemy reversals and actual pickup return value â€” edition121\n\n[[README-121.md]] records one useful return on the development ablation,71 older safe finishes and native wall reversals. Two development enemy-contact deaths remain under both old and new control. '+reservedText+'\n\nKeep every source/outcome gate. No physics rewrite, new model training or procedure admission. Next: improve landing around late enemies, reserve new cases, and calibrate the cost of optional returns. [[docs/PLAYABLE_LEVEL_CREATION_PLAN.md]]\n')
write(r/'docs/obsidian/129-enemy-reversals-and-return-value.canvas',{'nodes':[{'id':m['id'],'type':'file','file':m['source'],'x':i%3*430,'y':i//3*250,'width':390,'height':190} for i,m in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']} for i in range(len(modules)-1)]})
html=fill((r/'docs/publication121/guide-template.html').read_text(encoding='utf8'))+'<script id="system-data" type="application/json">'+json.dumps(system)+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory)+'</script></html>';text(out/'index.html',html)
for name,link in [('docs/readmes/INDEX.md','[Edition121 â€” enemy reversals and useful returns](../../README-121.md) Â· [Offline guide](121/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/129-enemy-reversals-and-return-value.md]] Â· [[README-121.md]]')]:
 p=r/name;s=p.read_text(encoding='utf-8-sig');text(p,link+'\n\n'+s if not s.startswith(link) else s)
print(json.dumps({'passed':True,'javaChecks':len(checks),'sources':len(inventory),'originalEntries':len(originals),'reserved':reservedText}))
