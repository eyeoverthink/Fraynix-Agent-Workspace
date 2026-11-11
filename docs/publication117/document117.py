from pathlib import Path
import json,hashlib,subprocess,shutil,gzip,statistics,xml.etree.ElementTree as ET
r=Path.cwd();out=r/'docs/readmes/117';out.mkdir(parents=True,exist_ok=True)
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-117']).returncode==1,'Never rewrite a published edition'
def write(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes((json.dumps(v,indent=2,ensure_ascii=False)+'\n').encode())
def text(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(v.encode())
def read(p):return json.loads((r/p).read_text(encoding='utf8'))
def sha(b):return hashlib.sha256(b).hexdigest()
uiroot=r/'build/game117-ui';ui=read(uiroot/'result.json');assert ui['passed'] and ui['pickupFrame']['state']['controller']['pickups']>0
hardware=read('workspace-engine/build/reference117-final/result.json');software=read('workspace-engine/build/reference117-mesa/result.json');assert hardware['passed'] and software['passed']
datasets={p:read('workspace-engine/build/controller117-'+f+'/result.json') for p,f in [('regression','final-regression'),('wider','final-wider'),('reserved','final-reserved'),('software-reserved','mesa-reserved')]}
assert all(d['passed'] for d in datasets.values())
shots=['chat-level-completed.png','collect-powerup.png','retained-repairs.png','recovered-level.png','reference-analysis.png','reference-playtested.png','level-1100.png','actual-reference.png','pickup-native.png']
for name in shots:shutil.copy2(uiroot/name,out/name)
shutil.copy2(r/'build/controller117-motion.png',out/'controller-motion.png')
for name,source in [('fresh-reference.png','workspace-engine/build/reference117-final/seed15-reference.png'),('missing-finish.png','workspace-engine/build/reference117-final/missing-finish.png'),('original-stall.png','workspace-engine/build/controller117-baseline/seed15-landing116.png')]:shutil.copy2(r/source,out/name)
write(out/'actual-ui.json',ui);write(out/'reference-hardware.json',hardware);write(out/'reference-software.json',software)
for label,data in datasets.items():write(out/f'controller-{label}.json',data)
checks=set();suites=[]
for folder in sorted((r/'build/validation117').iterdir()):
 if not folder.is_dir() or folder.name in ['hardware117-wider-root','hardware117-reserved-root']:continue
 total=0
 for p in sorted(folder.glob('*.xml')):
  e=ET.parse(p).getroot();assert int(e.get('failures','0'))+int(e.get('errors','0'))==0,p
  for c in e.findall('testcase'):
   if c.find('skipped') is None:checks.add(c.get('classname')+'.'+c.get('name'));total+=1
 if total:suites.append({'folder':folder.relative_to(r).as_posix(),'testMethods':total})
assert len(checks)>100 and len(suites)>=6
for name,count in [('desktop117.log',155),('desktop117-extra.log',10)]:
 s=(r/'build'/name).read_text(encoding='utf8');assert f'tests {count}' in s and 'fail 0' in s
metrics=[]
for label,data in datasets.items():
 for policy in ['simple','legacy115','landing116','safe117']:
  ts=[next(t for t in c['trials'] if t['policy']==policy) for c in data['cases']]
  metrics.append({'partition':label,'policy':policy,'collisionCountersAvailable':policy!='simple','cases':len(ts),'wins':sum(t['won'] for t in ts),'pickups':sum(t['final']['controller']['pickups'] for t in ts),'damage':sum(t['final']['controller']['damage'] for t in ts),'deaths':sum(t['final']['player']['dead'] for t in ts),'belowCourse':sum(t['final']['player']['y']<-.5 for t in ts),'trialSeconds':[t['elapsedNanos']/1e9 for t in ts],'medianSeconds':statistics.median(t['elapsedNanos']/1e9 for t in ts),'landingChecks':sum(t['final']['controller']['landingChecks'] for t in ts),'landingOverrides':sum(t['final']['controller']['landingOverrides'] for t in ts),'landingForecastMilliseconds':sum(t['final']['controller']['landingNanos'] for t in ts)/1e6})
totals={}
for policy in ['simple','legacy115','landing116','safe117']:
 ms=[m for m in metrics if m['partition']!='software-reserved' and m['policy']==policy];totals[policy]={k:sum(m[k] for m in ms) for k in ['cases','wins','pickups','damage','deaths','belowCourse']}
totals['simple']['pickups']=None;totals['simple']['damage']=None
for m in metrics:
 if not m['collisionCountersAvailable']:m['pickups']=None;m['damage']=None
write(out/'measurements.json',{'controllers':metrics,'counterScope':'Pickup/damage callbacks are counted during automatic AI control. Simple control records default zeros but has no corresponding event instrumentation; the table reports n/a. All simple final players remain small in this cohort.', 'timingScope':'One fresh trial per case/policy. Launch/reset through final native observation, excluding subsequent archive writing. Software repeats reserved geometry, not new unfamiliar cases. Complete chat costs are separate. Not a repeated benchmark.'})
assert totals['safe117']=={'cases':24,'wins':24,'pickups':24,'damage':0,'deaths':0,'belowCourse':0},totals
referenceResults={label:{'cases':len(d['cases']),'simpleWins':sum(c['task']['audit']['simpleControl']=='won' for c in d['cases']),'revisedWins':sum(c['task']['audit']['existingAI']=='won' for c in d['cases']),'pickups':sum(c['task']['audit']['aiPickups'] for c in d['cases']),'damage':sum(c['task']['audit']['aiDamage'] for c in d['cases']),'refusals':len(d['refusals'])} for label,d in [('hardware',hardware),('software',software)]}
assert all(d=={'cases':6,'simpleWins':6,'revisedWins':6,'pickups':6,'damage':0,'refusals':4} for d in referenceResults.values())
verification={'passed':True,'javaChecks':sorted(checks),'javaSuites':suites,'desktopChecks':165,'actualChatRequests':4,'actualAbruptRecovery':True,'noRepairRefusal':True,'modelCalls':sum(x['task']['timing052']['modelCalls'] for x in ui['requests']),'referenceCases':{'freshNativeImagesPerRenderer':6,'freshRefusalsPerRenderer':4,'original115ReferenceRegression':True},'controllerCases':totals,'referenceControllerResults':referenceResults,'softwareRepeatedReservedLayouts':6,'sourceCommit':ui['source'],'scope':'Input-only approach correction and measured collection in declared original-sprite two-gap courses. No added enemies; no general jam-escape, image recognition, model-training or universal playability claim.'}
assert verification['modelCalls']==0 and len(ui['requests'])==4;write(out/'verification.json',verification)
originals=[]
def preserve(p,logical):
 if not p.is_file():return
 b=p.read_bytes();raw=sha(b);dest=out/'originals'/(raw+'.gz');packed=gzip.compress(b,mtime=0);dest.parent.mkdir(exist_ok=True)
 if not dest.exists():dest.write_bytes(packed)
 originals.append({'logicalPath':logical,'path':dest.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(b),'rawSha256':raw})
folders=['build/validation117','workspace-engine/build/controller117-baseline','workspace-engine/build/controller117-probe-a','workspace-engine/build/controller117-regression-a','workspace-engine/build/controller117-wider-a','workspace-engine/build/controller117-final-regression','workspace-engine/build/controller117-final-wider','workspace-engine/build/controller117-final-reserved','workspace-engine/build/controller117-mesa-reserved','workspace-engine/build/reference117-final','workspace-engine/build/reference117-mesa','workspace-engine/build/reference115','workspace-engine/build/game115-repair','workspace-engine/build/game115-restart']
for folder in folders:
 for p in sorted((r/folder).rglob('*')):
  if p.is_file() and p.suffix in ['.json','.jsonl','.xml','.png','.txt','.log']:preserve(p,p.relative_to(r).as_posix())
# Preserve test-owned source, candidate, reference and trial bytes behind the reports.
for folder in folders:
 resultPath=r/folder/'result.json'
 if not resultPath.is_file():continue
 report=json.loads(resultPath.read_text(encoding='utf8'));projectPath=report.get('project')
 if not projectPath:continue
 project=Path(projectPath)
 assert project.is_dir(),project
 for sub in ['game-levels114','game-tasks115','game-references115']:
  for p in sorted((project/sub).rglob('*')):
   if p.is_file() and p.suffix in ['.json','.jsonl','.png']:
    preserve(p,folder+'/original-project/'+p.relative_to(project).as_posix())
for p in sorted(uiroot.iterdir()):
 if p.is_file():preserve(p,p.relative_to(r).as_posix())
for sub in ['game-tasks115','game-levels114','game-references115','agency-043','embedded-game']:
 for p in sorted((uiroot/'project'/sub).rglob('*')):
  if p.suffix in ['.json','.jsonl','.png','.log']:preserve(p,p.relative_to(r).as_posix())
for p in sorted((r/'build').glob('*117*.log')):preserve(p,p.relative_to(r).as_posix())
for p in sorted((r/'build').glob('ci117*.json')):preserve(p,p.relative_to(r).as_posix())
for name in ['build/study095-build.json','build/validate117.py','build/software117.py']:preserve(r/name,name)
write(out/'original-evidence.json',{'files':originals,'scope':'Exact isolated test evidence, original failed controller baseline, skipped native-test invocation and actual rerun, final cohorts and UI/recovery. Raw/compressed SHA-256 with deterministic gzip. No private projects, credentials, model weights or runtime binaries.'})
paths={x['path'] for x in read('docs/readmes/116/file-inventory.json')}
for name in subprocess.check_output(['git','diff','--name-only','e015b692','HEAD'],text=True).splitlines():
 if name.startswith(('workspace-engine/','workspace-desktop/','src/','assets/','.github/')) or name=='build.gradle':paths.add(name)
paths.update(['docs/EDITION_117_CONTRACT.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md'])
inventory=[]
for name in sorted(paths):
 b=(r/name).read_bytes()
 try:b=b.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':name,'bytes':len(b),'sha256':sha(b),'hashKind':kind})
write(out/'file-inventory.json',inventory)
note='docs/obsidian/125-safe-game-pursuit.md'
modules=[{'id':i,'name':n,'source':s,'note':note} for i,n,s in [('pursuit','Bounded falling-item pursuit','src/main/java/agent/Pursuit117.java'),('contacts','Read-only native contacts','src/main/java/agent/GameContact117.java'),('control','Observed control with retained baselines','src/main/java/agent/GameInput.java'),('task','Verified durable outcomes','workspace-engine/src/main/java/fraymus/workspace/GameTask115.java'),('chat','Verified chat summary','workspace-engine/src/main/java/fraymus/workspace/Domain044.java'),('runtime','Actual native observations and inputs','src/main/java/runtime/EmbeddedGameRuntime.java'),('proof','Actual app and restart tests','workspace-desktop/test/game117-integration.cjs')]]
system={'modules':modules,'screenshots':[{'path':'docs/readmes/117/'+n,'title':n} for n in shots],'evidence':[{'path':'docs/readmes/117/'+n} for n in ['actual-ui.json','reference-hardware.json','reference-software.json','measurements.json','verification.json','original-evidence.json']]};write(out/'system-map.json',system)
rows=[]
for label,x in zip(['Two observed gap repairs','Wider gap with abrupt restart','Repairs disabled','Image reconstruction'],ui['requests']):
 a=x['child'].get('audit',{});rows.append(f"| {label} | {x['elapsedMs']/1000:.3f} s | {a.get('simpleControl','failed')} | {a.get('existingAI','not run')} | {a.get('repairCount',0)} |")
controllerRows=[f"| {x['partition']} | {x['policy']} | {x['wins']}/{x['cases']} | {x['pickups'] if x['collisionCountersAvailable'] else 'n/a'} | {x['damage'] if x['collisionCountersAvailable'] else 'n/a'} | {x['medianSeconds']:.3f} s |" for x in metrics]
values={'JAVA_COUNT':str(len(checks)),'CHAT_ROWS':'\n'.join(rows),'CONTROLLER_ROWS':'\n'.join(controllerRows),'FINISHES':str(totals['safe117']['wins']),'LAYOUTS':'24','PICKUPS':str(totals['safe117']['pickups']),'DAMAGE':str(totals['safe117']['damage']),'PREVIOUS_FINISHES':str(totals['landing116']['wins']),'PREVIOUS_PICKUPS':str(totals['landing116']['pickups'])}
def fill(s):
 for k,v in values.items():s=s.replace('{{'+k+'}}',v)
 assert '{{' not in s;return s
readme=fill((r/'docs/publication117/README-template.md').read_text(encoding='utf8'));text(r/'README-117.md',readme);text(r/'README.md',readme)
text(r/note,'# Safe game pursuit — edition117\n\n[[README-117.md]] preserves the original wider-gap stall and corrects its approach through bounded falling-mushroom interception and normal inputs. All24 declared hardware courses finish with24 pickups and no observed damage. Reserved six geometries repeat on software rendering. Six transformed references per renderer now require both controllers to finish; four new refusals remain.\n\nOriginal contacts distinguish requested velocity from displacement. This prevents the recorded jam; generic escape from an already-wedged state remains unproven. Native test cache inputs now include actual game code/assets and study options. Actual chat reports checked collision counters. Zero model calls, no new training claim, unchanged procedure limits.\n\nNext: obstacle/enemy variation and risky/unreachable pickup decisions, followed by broader explicit image support and procedure lifecycle. [[docs/PLAYABLE_LEVEL_CREATION_PLAN.md]]\n')
write(r/'docs/obsidian/125-safe-game-pursuit.canvas',{'nodes':[{'id':m['id'],'type':'file','file':m['source'],'x':i%3*430,'y':i//3*250,'width':390,'height':190} for i,m in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']} for i in range(len(modules)-1)]})
html=fill((r/'docs/publication117/guide-template.html').read_text(encoding='utf8'))+'<script id="system-data" type="application/json">'+json.dumps(system)+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory)+'</script></html>';text(out/'index.html',html)
for name,link in [('docs/readmes/INDEX.md','[Edition117 — wider-gap control and safe collection](../../README-117.md) · [Offline guide](117/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/125-safe-game-pursuit.md]] · [[README-117.md]]')]:
 p=r/name;s=p.read_text(encoding='utf-8-sig');text(p,link+'\n\n'+s if not s.startswith(link) else s)
print(json.dumps({'passed':True,'javaChecks':len(checks),'sources':len(inventory),'originalEntries':len(originals),'totals':totals}))
