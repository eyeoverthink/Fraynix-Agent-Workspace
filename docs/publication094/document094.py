from pathlib import Path
import json,hashlib,gzip,base64,subprocess,shutil,xml.etree.ElementTree as ET,os
r=Path.cwd();d=r/'docs/readmes/094';d.mkdir(parents=True,exist_ok=True)
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def write(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8',newline='\n')
def js(p,v):write(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
def sha(b):return hashlib.sha256(b).hexdigest()
def sec(x):return f'{x/1000:.2f} s'
a=load(r/'build/analysis094.json');rec=load(r/'build/ui094-study/result.json');assert a['passed'] and rec['passed']
xml=list((r/'build/regression094').glob('TEST-*.xml'));assert xml
tests={'cases':0,'failures':0,'errors':0,'skipped':0,'classes':len(xml),'skips':[]}
for f in xml:
 t=ET.parse(f).getroot();tests['cases']+=int(t.attrib['tests'])
 for k in ['failures','errors','skipped']:tests[k]+=int(t.attrib.get(k,0))
 for c in t.findall('testcase'):
  if c.find('skipped') is not None:tests['skips'].append(c.attrib)
assert tests['failures']==tests['errors']==0
assert 'pass 135' in (r/'build/desktop094.log').read_text(encoding='utf-8-sig')
identity=load(r/'build/source-comparison094.json')
finding='Seven fresh three-stage goals completed and verified, including five recovered matrix tasks. Five abrupt Java terminations preserved committed progress; fresh downstream forecasts and actual report checks preceded each single world commit.'
checkpoint=load(r/'build/ui094-checkpoint/result.json');assert checkpoint['passed'];assert load(r/'build/checkpoint094-analysis.json')['passed'];assert a['completed']==7 and a['abruptTerminations']==5
assert tests['cases']-tests['skipped']>0
test_summary=f"{tests['cases']-tests['skipped']} Java checks passed across {tests['classes']} focused classes; {tests['skipped']} optional host-dependent case skipped. 135 desktop checks passed. Seven fresh native goals and the separate full-backup restoration completed. Three initial lock tests failed before the fix; all five new locking/compatibility checks passed afterward."

replays=[]
for row in rec['rows']:
 for packed in load(r/'build/ui094-study'/row['originalFile'])['originals']:
  run=json.loads(gzip.decompress(base64.b64decode(packed['gzipBase64'])));attempt=next(x for x in row['job']['arm']['attempts'] if x['id']==run['id'])
  if not run['samples']:continue
  replays.append({'label':f"{row['kind']} / {attempt['family']} / {attempt['stage']}",'id':run['id'],'status':run['status'],'objects':run['definition']['objects'],'samples':[{'tick':s['tick'],'timeS':s['timeS'],'bodies':s['bodies']} for s in run['samples']]})
archive=[];local={}
def pack(p,logical):
 raw=(Path('\\\\?\\'+str(p)) if os.name=='nt' else p).read_bytes();h=sha(raw)
 if h not in local:
  dest=d/'original'/(h+'.gz');dest.parent.mkdir(parents=True,exist_ok=True);z=gzip.compress(raw,mtime=0);dest.write_bytes(z);local[h]={'path':dest.relative_to(r).as_posix(),'bytes':len(z),'sha256':sha(z),'rawBytes':len(raw),'rawSha256':h}
 archive.append(dict(local[h],logicalPath=logical))
for folder in sorted((r/'build').glob('ui094-*')):
 for p in folder.rglob('*'):
  if (Path('\\\\?\\'+str(p)) if os.name=='nt' else p).is_file() and 'desktop-profile' not in p.parts and p.name!='host.json' and p.suffix in ['.json','.jsonl','.log','.png','.html','.css','.js','.txt','.csv','.previous','.pending','.fworld']:pack(p,folder.name+'/'+p.relative_to(folder).as_posix())
for f in xml:pack(f,'regression094/'+f.name)
for folder in ['checkpoint094-before','checkpoint094-after']:
 for f in (r/'build'/folder).glob('*.xml'):pack(f,folder+'/'+f.name)
for name in ['README-CONTRIBUTORS.md','docs/TRAJECTORY_REVIEW_094.md']:pack(r/name,'review/'+name)
for p in (r/'build').glob('*094*'):
 if p.is_file() and not p.name.startswith('document094.log') and p.suffix in ['.json','.log','.ps1','.py','.cjs','.html','.md','.txt']:pack(p,'validation/'+p.name)
for p in (r/'build/ui094-study').glob('*.png'):shutil.copyfile(p,d/p.name)
js(d/'original-evidence.json',{'files':archive,'distinctNewPayloads':len(local),'note':'Exact synthetic originals, checkpoints and logs. Raw and compressed hashes. Host credentials and desktop profiles excluded. Failed attempts remain distinct; no generated replacement observations.'})
old=load(r/'docs/readmes/093/file-inventory.json');paths={x['path'] for x in old};paths.update(x for x in subprocess.check_output(['git','diff','--name-only','--diff-filter=A','24052edef70eba152858dd15f93425c106db39df','HEAD'],text=True).splitlines() if x.startswith(('workspace-engine/','workspace-desktop/','scripts/','fraynix-modules/')))
kinds={x['path']:x['hashKind'] for x in old};inventory=[]
for name in sorted(paths):
 raw=(r/name).read_bytes();kind=kinds.get(name,'utf8-lf');raw=raw.decode('utf8').replace('\r\n','\n').encode() if kind=='utf8-lf' else raw
 inventory.append({'path':name,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
js(d/'file-inventory.json',inventory);write(d/'FILE_TREE.txt','Edition094 canonical source inventory\n'+'\n'.join(x['path'] for x in inventory)+'\n')
note='docs/obsidian/102-three-stage-recovery.md'
modules=[{'id':'runner','name':'Durable goal and actual stages','source':'workspace-engine/src/main/java/fraymus/workspace/WorldProcedure079.java'},{'id':'recovery','name':'Exact recovered forecast binding','source':'workspace-engine/src/main/java/fraymus/workspace/RecoveryForecast093.java'},{'id':'native','name':'Forced atomic checkpoint and bounded retry','source':'workspace-engine/src/main/java/fraymus/workspace/WorldNativeCheckpoint.java'},{'id':'inspection','name':'Independent source and outcome arithmetic','source':'scripts/Inspect-Recovery094.cjs'}]
for module in modules:module['note']=note
system={'modules':modules,'screenshots':[{'path':'docs/readmes/094/'+x['file'],'caption':'Actual094 report capture · 2026-11-08 America/Los_Angeles'} for x in rec['captures']],'evidence':[{'path':'docs/readmes/094/verification.json','caption':'Results and limits'},{'path':'docs/readmes/094/original-evidence.json','caption':'Original bytes'}]};js(d/'system-map.json',system)
verification={'passed':True,'analysis':a,'java':tests,'desktopPassed':135,'classes':identity,'checkpointRestoration':{'passed':checkpoint['passed'],'goal':checkpoint['goal']['id'],'phases':[x['phase'] for x in checkpoint['checks']]},'review':load(r/'build/repository094-review.json')['summary'] if 'summary' in load(r/'build/repository094-review.json') else {},'screenshots':rec['captures'],'finding':finding,'source':rec['source'],'limits':[a['scope'],'Three-stage recovery and abrupt isolated Java termination tested. No power-loss, storage-device failure, general recovery or physical-accuracy claim. Checkpoint-only restoration retains quarantine; complete stopped-state restoration is required in the tested corruption workflow.','Resumed totalMillis=0 means unavailable; it is excluded from calibration. Caller times include controlled restarts and test observations, so are not pure engine throughput.']};js(d/'verification.json',verification);js(d/'comparison-data.json',a)
data={'analysis':a,'finding':finding,'tests':test_summary,'replays':replays};html=(r/'build/guide094.html').read_text(encoding='utf8').replace('__DATA__',json.dumps(data,separators=(',',':')).replace('</','<\\/'))
extra=''.join('<script id="'+name+'" type="application/json">'+json.dumps(value,separators=(',',':')).replace('</','<\\/')+'</script>' for name,value in [('system-data',system),('inventory-data',inventory)])
write(d/'index.html',html.replace('</body>',extra+'</body>'))
table='\n'.join(f"| {x['kind']} | {'Completed' if x['success'] else 'Stopped'} | {x['resumes']} | {x['ticks']} | {x['forecastChargedTicks']} | {sec(x['forecastWallMillis'])} | {sec(x['callerMillis'])} |" for x in a['checks'])
readme=(r/'build/README094-template.md').read_text(encoding='utf8').replace('{finding}',finding).replace('{table}',table).replace('{test_summary}',test_summary)
write(r/'README-094.md',readme)
write(r/note,f'# Edition094 — three-stage crash recovery\n\n[[README-094.md|Full edition]] · [[docs/readmes/094/index.html|Offline evidence]] · [[docs/obsidian/101-recovered-state-forecast.md|Prior edition]] · [[README-CONTRIBUTORS.md|Contributor direction]]\n\n{finding}\n\n{test_summary}\n\nNative atomic retry correction; explicit closed093 frame upgrade, pending owners refused.332/334 prior classes identical. Five exact-owned-PID terminations, one graceful interruption, committed prefixes and both downstream forecast bindings. Checkpoint-only restoration stays quarantined; full stopped backup resumes explicitly. No power-loss, physical-accuracy or LLM inference claim.\n')
nodes=[{'id':m['id'],'type':'file','file':m['source'],'x':i*390,'y':0,'width':350,'height':260} for i,m in enumerate(modules)];nodes.append({'id':'evidence','type':'file','file':'README-094.md','x':390,'y':330,'width':740,'height':340});edges=[{'id':'e'+str(i),'fromNode':modules[i]['id'],'fromSide':'right','toNode':modules[i+1]['id'],'toSide':'left'} for i in range(3)];js(r/'docs/obsidian/102-three-stage-recovery.canvas',{'nodes':nodes,'edges':edges})
continuity=f'## Edition094 — locally verified; publication pending\n\n{finding}\n\n{test_summary} Original committed bytes, prefixes, chronology, provenance, charges and single commits independently inspected. Source{rec["source"]};332/334 engine classes identical to093. Initial harness race and actual Windows native-checkpoint replacement failure retained. New bounded atomic writer and explicit closed093 world upgrade; no old-proof relabelling. Corrupt checkpoint copy refuses motion; checkpoint-only restoration keeps quarantine, full stopped backup explicitly resumes.\n\nIsolated D:/Codex-Work/fraynix-workspace-edition094; public eyeoverthink/Fraynix-Agent-Workspace as verified2026-11-08. Read README-CONTRIBUTORS.md and dated TRAJECTORY_REVIEW_094.md. Copilot CI/path repair already merged, no production redesign. Preserve live045/qa046/private data and frozen001–093. No subagents. Next: complete mixed-task recovery/saving cost comparisons, then broader end-to-end tasks; no power-loss, physical-accuracy, new LLM inference or general intelligence claim.\n\n'
for name in ['AGENTS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md']:write(r/name,continuity+(r/name).read_text(encoding='utf8'))
root=(r/'README.md').read_text(encoding='utf8').replace('**Latest: [edition093','**Earlier edition: [edition093',1);root=root.replace('# Fraynix Agent Workspace\n','# Fraynix Agent Workspace\n\n**Latest: [edition094 — three-stage crash recovery](README-094.md).**\n\n'+finding+' [Offline evidence](docs/readmes/094/index.html).\n',1);write(r/'README.md',root)
for name,prefix in [('docs/ARCHITECTURE.md','Edition094 verifies three-stage recovery under abrupt owned-process termination, fixes native atomic replacement retries and adds an explicit closed093 frame handoff. All outcome checks remain. [Results](../README-094.md) · [Current contributor contract](../README-CONTRIBUTORS.md).\n\n'),('docs/obsidian/00-home.md','[[README-094.md|Edition094 — three-stage crash recovery]] · [[docs/obsidian/102-three-stage-recovery.md]]\n\n'),('docs/readmes/INDEX.md','[Edition094 — three-stage crash recovery](../../README-094.md) · [Offline evidence](094/index.html)\n\n')]:write(r/name,prefix+(r/name).read_text(encoding='utf8'))
print(json.dumps({'finding':finding,'tests':tests,'logicalOriginals':len(archive),'uniquePayloads':len(local),'sources':len(inventory),'replays':len(replays)},indent=2))
