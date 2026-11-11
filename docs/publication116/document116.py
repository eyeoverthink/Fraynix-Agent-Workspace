from pathlib import Path
import json,hashlib,subprocess,shutil,gzip,xml.etree.ElementTree as ET,statistics
r=Path.cwd();out=r/'docs/readmes/116';out.mkdir(parents=True,exist_ok=True)
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-116']).returncode==1,'Never rewrite a published edition'
def write(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes((json.dumps(v,indent=2,ensure_ascii=False)+'\n').encode('utf8'))
def text(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(v.encode('utf8'))
def read(p):return json.loads((r/p).read_text(encoding='utf8'))
def sha(b):return hashlib.sha256(b).hexdigest()
uiroot=r/'build/game116-ui';ui=read(uiroot/'result.json');assert ui['passed']
hardware=read('workspace-engine/build/reference116-final/result.json');software=read('workspace-engine/build/reference116-mesa-final/result.json');assert hardware['passed'] and software['passed']
development=read('workspace-engine/build/controller116-final-dev/result.json');reserved=read('workspace-engine/build/controller116-validation/result.json');mesa=read('workspace-engine/build/controller116-mesa-validation/result.json');assert all(x['passed'] for x in [development,reserved,mesa])
shots=['chat-level-completed.png','retained-repairs.png','recovered-level.png','reference-analysis.png','reference-playtested.png','level-1100.png','actual-reference.png']
for name in shots:shutil.copy2(uiroot/name,out/name)
for name,source in [('software-before.png','build/ci116/mesa-reference/actual-overview.png'),('software-after.png','build/ci116/mesa-fixed-reference/actual-overview.png'),('fresh-reference.png','workspace-engine/build/reference116-final/seed15-reference.png'),('missing-finish.png','workspace-engine/build/reference116-final/missing-finish.png')]:shutil.copy2(r/source,out/name)
write(out/'actual-ui.json',ui);write(out/'reference-hardware.json',hardware);write(out/'reference-software.json',software)
write(out/'controller-development.json',development);write(out/'controller-reserved.json',reserved);write(out/'controller-software.json',mesa)
checks=set();suites=[]
for folder in ['build/validation116/hardware-root','build/validation116/hardware-engine','build/validation116/software-root','build/validation116/software-engine']:
 total=0
 for p in sorted((r/folder).glob('*.xml')):
  e=ET.parse(p).getroot();assert int(e.get('failures','0'))+int(e.get('errors','0'))==0
  for c in e.findall('testcase'):checks.add(c.get('classname')+'.'+c.get('name'));total+=1
 suites.append({'folder':folder,'testMethods':total})
assert checks and all(x['testMethods']>0 for x in suites)
assert 'tests 155' in (r/'build/desktop116.log').read_text(encoding='utf8') and 'fail 0' in (r/'build/desktop116.log').read_text(encoding='utf8')
assert 'tests 10' in (r/'build/desktop116-extra.log').read_text(encoding='utf8') and 'fail 0' in (r/'build/desktop116-extra.log').read_text(encoding='utf8')
metrics=[]
for label,data in [('development',development),('reserved',reserved),('software-reserved',mesa)]:
 for policy in ['simple','legacy115','landing116']:
  trials=[next(t for t in c['trials'] if t['policy']==policy) for c in data['cases']]
  metrics.append({'partition':label,'policy':policy,'cases':len(trials),'wins':sum(t['won'] for t in trials),'pickups':sum(t['final']['controller']['pickups'] for t in trials),'trialSeconds':[t['elapsedNanos']/1e9 for t in trials],'medianSeconds':statistics.median(t['elapsedNanos']/1e9 for t in trials),'landingChecks':sum(t['final']['controller']['landingChecks'] for t in trials),'landingOverrides':sum(t['final']['controller']['landingOverrides'] for t in trials),'landingForecastMilliseconds':sum(t['final']['controller']['landingNanos'] for t in trials)/1e6})
write(out/'measurements.json',{'controllers':metrics,'timingScope':'One fresh trial per case/policy. Includes launch/reset, all input chunks and final observation; excludes writing retained PNG/JSON afterward. Not a repeated latency benchmark or whole-chat cost. Software cohort repeats the six reserved geometries on a different renderer; it is not six more unfamiliar layouts.'})
verification={'passed':True,'javaChecks':sorted(checks),'javaSuites':suites,'desktopChecks':165,'actualChatRequests':4,'actualAbruptRecovery':True,'noRepairRefusal':True,'modelCalls':sum(x['task']['timing052']['modelCalls'] for x in ui['requests']),'referenceCases':{'freshNativeImagesPerRenderer':6,'freshRefusalsPerRenderer':4,'original115ReferenceRegression':True,'shaderCompileAndFailure':True},'controllerCases':{'distinctLayouts':12,'simpleWins':12,'originalWins':5,'revisedWins':12,'originalPickups':7,'revisedPickups':2,'softwareRepeatedReservedLayouts':6},'sourceCommit':ui['source'],'scope':'Known original sprites and bounded two-gap layouts. Current collision geometry and input-only control; actual native physics and finish contact. Conservative controller reduces power-up collection. No general image recognition, model training or universal playability claim.'}
write(out/'verification.json',verification)
verification['referenceControllerResults']={label:{'simpleWins':sum(c['task']['audit']['simpleControl']=='won' for c in d['cases']),'revisedWins':sum(c['task']['audit']['existingAI']=='won' for c in d['cases']),'cases':len(d['cases']),'failureParameters':[c['sourceCandidate']['parameters'] for c in d['cases'] if c['task']['audit']['existingAI']!='won']} for label,d in [('hardware',hardware),('software',software)]}
for policy,wins,pickups in [('simple',12,0),('legacy115',5,7),('landing116',12,2)]:
 measured=[m for m in metrics if m['partition'] in ['development','reserved'] and m['policy']==policy]
 assert sum(m['wins'] for m in measured)==wins and sum(m['pickups'] for m in measured)==pickups,'Update the narrative to the measured cohort'
assert verification['modelCalls']==0 and len(ui['requests'])==4
assert all(x['simpleWins']==6 and x['revisedWins']==5 and x['cases']==6 for x in verification['referenceControllerResults'].values())
write(out/'verification.json',verification)
originals=[]
def preserve(p,logical):
 if not p.is_file():return
 b=p.read_bytes();raw=sha(b);dest=out/'originals'/(raw+'.gz');packed=gzip.compress(b,mtime=0);dest.parent.mkdir(exist_ok=True)
 if not dest.exists():dest.write_bytes(packed)
 originals.append({'logicalPath':logical,'path':dest.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(b),'rawSha256':raw})
folders=['build/display116','build/ci116','build/development116-a','build/validation116','workspace-engine/build/controller116-probe1','workspace-engine/build/controller116-b','workspace-engine/build/controller116-validation','workspace-engine/build/controller116-final-dev','workspace-engine/build/controller116-mesa-validation','workspace-engine/build/reference116-final','workspace-engine/build/reference116-mesa-final']
for folder in folders:
 for p in sorted((r/folder).rglob('*')):
  if p.is_file() and p.suffix in ['.json','.jsonl','.xml','.png','.txt','.java','.js','.log','.zip']:preserve(p,p.relative_to(r).as_posix())
for folder in ['build/game116-ui']:
 for p in sorted((r/folder).iterdir()):
  if p.is_file():preserve(p,p.relative_to(r).as_posix())
 for sub in ['game-tasks115','game-levels114','game-references115','agency-043','embedded-game']:
  for p in sorted((r/folder/'project'/sub).rglob('*')):
   if p.suffix in ['.json','.jsonl','.png','.log']:preserve(p,p.relative_to(r).as_posix())
for p in sorted((r/'build').glob('*116*.log')):preserve(p,p.relative_to(r).as_posix())
for name in ['build/study095-build.json','build/ci116-parent.json']:preserve(r/name,name)
write(out/'original-evidence.json',{'files':originals,'scope':'Exact isolated test bytes, original CI evidence, failed prototypes, software-renderer reproduction and final actual UI trials. Gzip deduplicated by raw SHA-256. No private projects, model weights, Java installation or Mesa binaries.'})
prior=read('docs/readmes/115/file-inventory.json');paths={x['path'] for x in prior}
for name in subprocess.check_output(['git','diff','--name-only','2eae6260','HEAD'],text=True).splitlines():
 if name.startswith(('workspace-engine/','workspace-desktop/','src/','assets/','.github/')) or name=='build.gradle':paths.add(name)
paths.update(['docs/EDITION_116_CONTRACT.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md'])
inventory=[]
for name in sorted(paths):
 b=(r/name).read_bytes()
 try:b=b.decode('utf-8-sig').replace('\r\n','\n').encode('utf8');kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':name,'bytes':len(b),'sha256':sha(b),'hashKind':kind})
write(out/'file-inventory.json',inventory)
note='docs/obsidian/124-measured-game-control.md'
modules=[{'id':i,'name':n,'source':s,'note':note} for i,n,s in [('forecast','Bounded landing forecasts','src/main/java/agent/LandingGuard116.java'),('control','Observed input selection','src/main/java/agent/GameInput.java'),('render','Portable original sprite rendering','assets/shaders/default.glsl'),('task','Durable Java task and identity','workspace-engine/src/main/java/fraymus/workspace/GameTask115.java'),('image','Preserved known-sprite reconstruction','workspace-engine/src/main/java/fraymus/workspace/GameReference115.java'),('runtime','Actual renderer and bounded AI input','src/main/java/runtime/EmbeddedGameRuntime.java'),('proof','Actual chat and restart trial','workspace-desktop/test/game116-integration.cjs')]]
system={'modules':modules,'screenshots':[{'path':'docs/readmes/116/'+n,'title':n} for n in shots],'evidence':[{'path':'docs/readmes/116/'+n} for n in ['actual-ui.json','reference-hardware.json','reference-software.json','measurements.json','verification.json','original-evidence.json']]};write(out/'system-map.json',system)
rows=[]
for label,x in zip(['Two observed gap repairs','Abrupt restart and resume','Repairs disabled','Image reconstruction'],ui['requests']):
 a=x['child'].get('audit',{});rows.append(f"| {label} | {x['elapsedMs']/1000:.3f} s | {a.get('simpleControl','failed')} | {a.get('existingAI','not run')} | {a.get('repairCount',0)} |")
controllerRows=[]
for x in metrics:controllerRows.append(f"| {x['partition']} | {x['policy']} | {x['wins']}/{x['cases']} | {x['pickups']} | {x['medianSeconds']:.3f} s |")
template=(r/'docs/publication116/README-template.md').read_text(encoding='utf8')
readme=template.replace('{{JAVA_COUNT}}',str(len(checks))).replace('{{CHAT_ROWS}}','\n'.join(rows)).replace('{{CONTROLLER_ROWS}}','\n'.join(controllerRows))
text(r/'README-116.md',readme);text(r/'README.md',readme)
text(r/note,'# Measured game control — edition116\n\n[[README-116.md]] preserves the original failure and original controller, adds bounded collision-ray landing prediction and committed edge crossing through normal input. Twelve hardware layouts: simple12/12, original5/12, revised12/12. Revised collects2 power-ups versus original7; completion is not richer play. Six reserved layouts also repeat on software rendering.\n\nThe115 CI failure was an invalid GLSL330 sampler index, not missing image tolerance. Literal sampler branches and explicit shader failures restore actual sprites. Shader bytes join engine identity; unchanged known-sprite matching now passes both renderers. Six fresh references per renderer reconstruct separate candidates and playtest; four unsupported images refuse. Actual chat/restart costs and all failed prototypes retained. Wider image seed15 with a five-tile gap remains an AI failure: simple6/6 versus revised5/6 reconstructed courses; do not conflate that with the narrower12-case cohort.\n\nNo new model training or general scene understanding. Next: wider-gap contact recovery, safe power-up pursuit and explicit procedure retirement/reactivation under32 active slots. [[docs/PLAYABLE_LEVEL_CREATION_PLAN.md]] preserves the larger phased direction.\n')
write(r/'docs/obsidian/124-measured-game-control.canvas',{'nodes':[{'id':m['id'],'type':'file','file':m['source'],'x':i%3*430,'y':i//3*250,'width':390,'height':190} for i,m in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']} for i in range(len(modules)-1)]})
html=(r/'docs/publication116/guide-template.html').read_text(encoding='utf8').replace('{{JAVA_COUNT}}',str(len(checks)))
html+='<script id="system-data" type="application/json">'+json.dumps(system)+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory)+'</script></html>';text(out/'index.html',html)
for name,link in [('docs/readmes/INDEX.md','[Edition116 — measured control and fresh reference images](../../README-116.md) · [Offline guide](116/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/124-measured-game-control.md]] · [[README-116.md]]')]:
 p=r/name;s=p.read_text(encoding='utf-8-sig');text(p,link+'\n\n'+s if not s.startswith(link) else s)
print(json.dumps({'passed':True,'javaChecks':len(checks),'sources':len(inventory),'originalEntries':len(originals),'requests':len(ui['requests'])}))
