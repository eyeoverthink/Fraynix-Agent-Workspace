from pathlib import Path
import json,hashlib,subprocess,shutil,gzip,statistics,xml.etree.ElementTree as ET
r=Path.cwd();out=r/'docs/readmes/122';out.mkdir(parents=True,exist_ok=True)
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-122']).returncode==1,'Never rewrite a published edition'
def read(p):return json.loads((r/p).read_text(encoding='utf-8'))
def write(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
def text(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf-8')
def sha(b):return hashlib.sha256(b).hexdigest()
uiroot=r/'build/game122-ui';ui=read(uiroot/'result.json');assert ui['passed'] and all(ui[k] for k in ['returnFrame','collectionFrame','riskFrame','contactFrame','edgeFrame'])
paths={'development':'controller122-development','reserved':'controller122-reserved','software-reserved':'controller122-mesa-reserved','baseline':'controller122-baseline'}
for p in sorted((r/'workspace-engine/build').glob('controller122-old*/result.json')):paths['regression-'+p.parent.name.removeprefix('controller122-')]=p.parent.name
datasets={k:read('workspace-engine/build/'+v+'/result.json') for k,v in paths.items()};assert all(d['passed'] for d in datasets.values())
refs={label:read('workspace-engine/build/reference122-'+suffix+'/result.json') for label,suffix in [('hardware','final'),('software','mesa')]};assert all(d['passed'] for d in refs.values())
shots=['old-repair-regression.png','challenge-controls.png','return-native.png','return-live.png','collected-native.png','collected-after-return.png','return-completed.png','near-completed.png','turtle-completed.png','edge-completed.png','contact-live.png','edge-live.png','verified-contact.png','declined-pickup.png','gap-refused.png','challenge-1100.png']
for n in shots:shutil.copy2(uiroot/n,out/n)
shutil.copy2(r/'build/controller122-motion.png',out/'controller-motion.png')
for label,d in datasets.items():write(out/('controller-'+label+'.json'),d)
for label,d in refs.items():write(out/('reference-'+label+'.json'),d)
write(out/'actual-ui.json',ui)
checks=set();suites=[]
for folder in sorted((r/'build/validation122').iterdir()):
 if not folder.is_dir() or (folder.name.startswith('prototype') or ('input-first' in folder.name or 'support-first' in folder.name)):continue
 total=0
 for p in folder.glob('*.xml'):
  e=ET.parse(p).getroot();assert int(e.get('failures','0'))+int(e.get('errors','0'))==0,p
  for c in e.findall('testcase'):
   if c.find('skipped') is None:checks.add(c.get('classname')+'.'+c.get('name'));total+=1
 suites.append({'name':folder.name,'executions':total})
assert len(checks)>=186
for name,count in [('desktop122-final.log',158),('desktop122-extra.log',10)]:
 s=(r/'build'/name).read_text(encoding='utf-8');assert f'tests {count}' in s and 'fail 0' in s
assert 'BUILD SUCCESSFUL' in (r/'build/launcher122.log').read_text(encoding='utf-8')
metrics=[]
for label,d in datasets.items():
 for policy in sorted({t['policy'] for c in d['cases'] for t in c['trials']}):
  ts=[t for c in d['cases'] for t in c['trials'] if t['policy']==policy];cs=[t['final']['controller'] for t in ts]
  metrics.append({'partition':label,'policy':policy,'cases':len(ts),'wins':sum(t['won'] for t in ts),'pickups':None if policy=='simple' else sum(c['pickups'] for c in cs),'damage':None if policy=='simple' else sum(c['damage'] for c in cs),'deaths':sum(t['final']['player']['dead'] for t in ts),'contactCorrections':sum(c.get('enemyLanding122',{}).get('overrides',0) for c in cs),'edgeEscapes':sum(c.get('enemyLanding122',{}).get('edgeStarts',0) for c in cs),'forecastChecks':sum(c.get('enemyLanding122',{}).get('checks',0) for c in cs),'forecastMillis':sum(c.get('enemyLanding122',{}).get('nanos',0) for c in cs)/1e6,'returnTicks':sum(c.get('opportunity120',{}).get('revisitTicks',0) for c in cs),'trialSeconds':[t['elapsedNanos']/1e9 for t in ts],'medianSeconds':statistics.median(t['elapsedNanos']/1e9 for t in ts)})
old=[m for m in metrics if m['partition'].startswith('regression-')];assert sum(m['cases'] for m in old)==85 and sum(m['wins'] for m in old)==85 and sum(m['damage'] for m in old)==0
assert sum(m['pickups'] for m in old if 'old117' in m['partition'])==24
for c in datasets['regression-old121-true']['cases']:
 if c['name'] in ['reserved-late-goomba','reserved-late-turtle','reserved-return-room']:
  t=c['trials'][0];assert t['won'] and t['final']['controller']['pickups']==1 and t['final']['controller']['damage']==0
fixes=[]
for c in datasets['development']['cases']:
 if not c['name'].startswith('retained-'):continue
 t=next(t for t in c['trials'] if t['policy']=='contact122');b=next(t for oldcase in datasets['baseline']['cases'] if oldcase['name']==c['name'] for t in oldcase['trials'] if t['policy']=='revisit121');co=t['final']['controller'];assert t['won'] and not b['won'] and co['damage']==0
 fixes.append({'case':c['name'],'oldFinish':b['won'],'newFinish':t['won'],'corrections':co['enemyLanding122']['overrides'],'edgeEscapes':co['enemyLanding122']['edgeStarts'],'oldTicks':b['final']['tick'],'newTicks':t['final']['tick']})
assert len(fixes)==3
dev=next(m for m in metrics if m['partition']=='development' and m['policy']=='contact122');rr=next(m for m in metrics if m['partition']=='reserved' and m['policy']=='contact122');pr=next(m for m in metrics if m['partition']=='reserved' and m['policy']=='revisit121');sw=next(m for m in metrics if m['partition']=='software-reserved' and m['policy']=='contact122')
assert dev['wins']==8 and dev['damage']==0
assert rr['wins']>=pr['wins'] and rr['deaths']<=pr['deaths'] and rr['damage']<=pr['damage'],'No promotion with worse reserved outcomes'
for label in ['reserved','software-reserved']:
 for case in datasets[label]['cases']:
  ts={t['policy']:t for t in case['trials']};a=ts['contact122'];b=ts['revisit121']
  assert not b['won'] or a['won'],(label,case['name'])
  assert a['final']['controller']['damage']<=b['final']['controller']['damage'],(label,case['name'])
  assert not a['final']['player']['dead'] or b['final']['player']['dead'],(label,case['name'])
referenceResults={label:{'cases':len(d['cases']),'simpleWins':sum(c['task']['audit']['simpleControl']=='won' for c in d['cases']),'revisedWins':sum(c['task']['audit']['existingAI']=='won' for c in d['cases']),'refusals':len(d['refusals'])} for label,d in refs.items()};assert all(v=={'cases':6,'simpleWins':6,'revisedWins':6,'refusals':4} for v in referenceResults.values())
write(out/'measurements.json',{'controllers':metrics,'retainedFixes':fixes,'scope':'Fresh isolated native trials. Old121 reserved scenes are regressions. Eight fresh reserved scenes declared before tuning; two input-admission corrections and repeated first cases are documented in the contract. The controller was unchanged. Software repeats the same cases. Complete single-run costs are not statistical speed claims.'})
recheck=read('build/receipt-recheck122.json');assert recheck['passed'] and recheck['trials']>=170
v={'recordedReceiptRecheck':recheck,'passed':True,'javaChecks':sorted(checks),'javaSuites':suites,'desktopChecks':168,'launcher':{'passed':True,'scope':'Normal isolated PrepareOnly; current build receipt checked before and after actual UI. Prior121 stale-installation/live-owner fixture retained in its own edition.'},'actualChatRequests':len(ui['requests']),'actualAbruptRecovery':True,'actualAiFailureRefused':True,'referenceControllerResults':referenceResults,'referenceCases':{'freshNativeImagesPerRenderer':6,'freshRefusalsPerRenderer':4,'original115ReferenceRegression':True},'controllerCases':metrics,'retainedFixes':fixes,'modelCalls':sum(x['task']['timing052']['modelCalls'] for x in ui['requests']),'sourceCommit':ui['source'],'scope':'Ordinary input only. Short descending-contact forecasts and bounded supported-edge recovery. No changed physics, neural training, arbitrary visual recognition or new skill admission.'};assert len(ui['requests'])==7 and v['modelCalls']==0;write(out/'verification.json',v)
originals=[]
def preserve(p,logical):
 if not p.is_file():return
 b=p.read_bytes();raw=sha(b);dest=out/'originals'/(raw+'.gz');packed=gzip.compress(b,mtime=0);dest.parent.mkdir(exist_ok=True)
 if not dest.exists():dest.write_bytes(packed)
 originals.append({'logicalPath':logical,'path':dest.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(b),'rawSha256':raw})
folders=[r/'build/validation122',r/'build/diagnostics122']+list((r/'workspace-engine/build').glob('controller122*'))+[r/'workspace-engine/build'/n for n in ['reference122-final','reference122-mesa','reference115','game115-repair','game115-restart','challenge118-task','challenge118-failed','challenge118-image','recovery119-task','opportunity120-task','opportunity121-task','contact122-task']]
projects={}
for folder in folders:
 for p in sorted(folder.rglob('*')):
  if p.is_file() and p.suffix in ['.json','.jsonl','.xml','.png','.txt','.log']:
   preserve(p,p.relative_to(r).as_posix())
   if p.name=='result.json':
    report=json.loads(p.read_text(encoding='utf-8'));project=report.get('project')
    if project:projects[p.relative_to(r).as_posix()]=Path(project)
for logical,project in projects.items():
 assert project.is_dir(),project
 for sub in ['game-levels114','game-tasks115','game-references115']:
  for p in sorted((project/sub).rglob('*')):
   if p.is_file() and p.suffix in ['.json','.jsonl','.png']:preserve(p,logical+'/original-project/'+p.relative_to(project).as_posix())
for folder in sorted((r/'build').glob('game122-ui*')):
 if not folder.is_dir():continue
 for p in folder.iterdir():
  if p.is_file():preserve(p,p.relative_to(r).as_posix())
 for sub in ['game-tasks115','game-levels114','game-references115','agency-043','embedded-game']:
  for p in sorted((folder/'project'/sub).rglob('*')):
   if p.is_file() and p.suffix in ['.json','.jsonl','.png','.log']:preserve(p,p.relative_to(r).as_posix())
for pattern in ['*122*.log','receipt-recheck122.json','regression122-gates.json','ci122*.json','study095-build*.json']:
 for p in sorted((r/'build').glob(pattern)):
  if p.is_file():preserve(p,p.relative_to(r).as_posix())
write(out/'original-evidence.json',{'files':originals,'scope':'Exact baseline, original unsuccessful invocation, development, reserved, regressions and actual visible chat/recovery originals. Private user worlds excluded.'})
sourcePaths={x['path'] for x in read('docs/readmes/121/file-inventory.json')}
for n in subprocess.check_output(['git','diff','--name-only','cdc5bdcb','HEAD'],text=True).splitlines():
 if n.startswith(('workspace-engine/','workspace-desktop/','src/','assets/','.github/')) or n=='build.gradle':sourcePaths.add(n)
sourcePaths.update(['docs/EDITION_122_CONTRACT.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md']);inventory=[]
for n in sorted(sourcePaths):
 b=(r/n).read_bytes()
 try:b=b.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':n,'bytes':len(b),'sha256':sha(b),'hashKind':kind})
write(out/'file-inventory.json',inventory)
note='docs/obsidian/130-enemy-contact-and-supported-edge-recovery.md'
modules=[{'id':i,'name':n,'source':s,'note':note} for i,n,s in [('forecast','Descending contact forecast','src/main/java/agent/EnemyLanding122.java'),('recovery','Supported-edge recovery','src/main/java/agent/ContactRecovery119.java'),('control','Ordinary player input','src/main/java/agent/GameInput.java'),('runtime','Original native simulation','src/main/java/runtime/EmbeddedGameRuntime.java'),('task','Durable checked task','workspace-engine/src/main/java/fraymus/workspace/GameTask115.java'),('verify','Added receipt checks','workspace-engine/src/main/java/fraymus/workspace/GameEvidence122.java'),('proof','Actual visible review','workspace-desktop/test/game122-integration.cjs')]]
system={'modules':modules,'screenshots':[{'path':'docs/readmes/122/'+n,'title':n} for n in shots],'evidence':[{'path':'docs/readmes/122/'+n} for n in ['actual-ui.json','reference-hardware.json','reference-software.json','measurements.json','verification.json','original-evidence.json']]};write(out/'system-map.json',system)
summary=f"Development: {dev['wins']}/8 finishes, {dev['pickups']} pickups, {dev['damage']} damage and {dev['deaths']} deaths. Fresh reserved: revised {rr['wins']}/8 finishes, {rr['pickups']} pickups, {rr['damage']} damage, {rr['deaths']} deaths; previous 121 {pr['wins']}/8, {pr['pickups']} pickups, {pr['damage']} damage, {pr['deaths']} {'death' if pr['deaths']==1 else 'deaths'}. Software repeat: {sw['wins']}/8, {sw['pickups']} pickups, {sw['damage']} damage, {sw['deaths']} deaths. No tuning against reserved results."
failed=[c['name'] for c in datasets['reserved']['cases'] for t in c['trials'] if t['policy']=='contact122' and not t['won']]
limits=('Remaining reserved failures: '+', '.join(failed)+'. Their full traces are retained.') if failed else 'All eight fresh reserved courses finish in this declared set. This does not establish safety for arbitrary levels, moving shells or untested combinations.'
regression='All 85 regression course executions finish without damage: the previous 71 courses plus the 14 edition 121 development/reserved courses. The original 24 pickup successes remain, and all three prior reserved return benefits still collect their item. Overlapping layouts are not additional unseen evidence.'
rows=[f"| {x['prompt']} | {x['elapsedMs']/1000:.3f} s | {x['task']['status']} | {x['child'].get('audit',{}).get('aiPickups','not certified')} |" for x in ui['requests']]
values={'JAVA_COUNT':str(len(checks)),'RESULT_SUMMARY':summary,'REGRESSION_SUMMARY':regression,'RESERVED_LIMITS':limits,'CHAT_ROWS':'\n'.join(rows),'CONTROLLER_ROWS':'\n'.join(f"| {x['partition']} | {x['policy']} | {x['wins']}/{x['cases']} | {x['pickups'] if x['pickups'] is not None else 'n/a'} | {x['damage'] if x['damage'] is not None else 'n/a'} | {x['deaths']} | {x['medianSeconds']:.3f} s |" for x in metrics if not x['partition'].startswith('regression-')),'FIX_ROWS':'\n'.join(f"| {x['case']} | failed | finished | {x['corrections']} | {x['edgeEscapes']} |" for x in fixes)}
def fill(s):
 for k,v in values.items():s=s.replace('{{'+k+'}}',v)
 assert '{{' not in s;return s
md=fill((r/'docs/publication122/README-template.md').read_text(encoding='utf-8'));text(r/'README-122.md',md);text(r/'README.md',md)
text(r/note,'# Enemy contact and supported-edge recovery — edition122\n\n[[README-122.md]] records the three retained corrections, original comparisons and fresh reserved results. '+summary+'\n\nKeep source/outcome verification and ordinary controls. Next: repeated complete costs and optional-goal value, then broader contact uncertainty. [[docs/PLAYABLE_LEVEL_CREATION_PLAN.md]]\n')
write(r/'docs/obsidian/130-enemy-contact-and-supported-edge-recovery.canvas',{'nodes':[{'id':m['id'],'type':'file','file':m['source'],'x':i%3*430,'y':i//3*250,'width':390,'height':190} for i,m in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']} for i in range(len(modules)-1)]})
html=fill((r/'docs/publication122/guide-template.html').read_text(encoding='utf-8'))+'<script id="system-data" type="application/json">'+json.dumps(system)+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory)+'</script></html>';text(out/'index.html',html)
for name,link in [('docs/readmes/INDEX.md','[Edition122 — enemy contact and edge recovery](../../README-122.md) · [Offline guide](122/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/130-enemy-contact-and-supported-edge-recovery.md]] · [[README-122.md]]')]:
 p=r/name;s=p.read_text(encoding='utf-8-sig');text(p,link+'\n\n'+s if not s.startswith(link) else s)
print(json.dumps({'passed':True,'javaChecks':len(checks),'sources':len(inventory),'originalEntries':len(originals),'summary':summary}))
