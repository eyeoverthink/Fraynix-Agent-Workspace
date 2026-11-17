from pathlib import Path
import json,hashlib,gzip,shutil,xml.etree.ElementTree as ET,subprocess,sys,statistics
r=Path.cwd();out=r/'docs/readmes/154';out.mkdir(parents=True,exist_ok=True);sha=lambda b:hashlib.sha256(b).hexdigest()
sys.path.insert(0,str(r/'docs/publication149'));from public_evidence149 import check_public
def read(p):return json.loads((r/p).read_bytes())
def write(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes((json.dumps(v,indent=2,ensure_ascii=False)+'\n').encode())
def text(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(s.encode())
assert subprocess.run(['git','show-ref','--quiet','--verify','refs/tags/readme-154']).returncode==1
paths=['build/study154-generated-final/result.json','build/study154-exposed150-final/result.json'];studies=[read(p) for p in paths];assert all(s['passed'] for s in studies)
lock=read('docs/OBSERVATION_154_LOCK.json')
for name,h in lock['sourceHashes'].items():assert sha((r/name).read_bytes().replace(b'\r\n',b'\n'))==h,name
for s in studies:assert s['methodLockSha256']==sha((r/'docs/OBSERVATION_154_LOCK.json').read_bytes());check_public(s)
review=read('build/app-visual-review154.json');assert review['passed'];assert {x['sha256'] for x in review['images']}=={x['sha256'] for s in studies for x in s['captures']}
checks=set();xmls=list((r/'build/validation154/final').glob('*.xml'))
for p in xmls:
 e=ET.parse(p).getroot();assert sum(int(e.get(k,'0')) for k in ['failures','errors','skipped'])==0
 for c in e.findall('testcase'):checks.add(c.get('classname')+'.'+c.get('name'))
assert len(checks)==35
desktop=(r/'build/desktop154.log').read_text(encoding='utf8');assert 'tests 164' in desktop and 'fail 0' in desktop
source=read('build/source-check154.json');qualification=read('build/qualification154.json');assert source['passed'] and qualification['passed']
measures={key:read(f'build/replay154-{key}.json') for key in ['generated','camera']};assert all(m['passed'] for m in measures.values())
validation='35 selected Java checks and 164 desktop checks passed, with no failures or skipped cases. Six new generated app comparisons and one exposed-camera comparison passed visible-control, forced-replay, cold-restart and source-integrity checks. Complete reference/snapshot-access evidence matches exactly. Independent geometry and separate outcome/cost gates passed. These are scoped checks, not a rerun of every historical capability. Recorded tests only.'
def iou(a,b):
 x=max(0,min(a[2],b[2])-max(a[0],b[0]));y=max(0,min(a[3],b[3])-max(a[1],b[1]));v=x*y
 return v/((a[2]-a[0])*(a[3]-a[1])+(b[2]-b[0])*(b[3]-b[1])-v)
rows=[];shots=[]
for path,s in zip(paths,studies):
 write(out/(s['group']+'-study.json'),s)
 for c in s['cases']:
  e=c['result']['experiment'];q=next(x for x in qualification['cases'] if x['case']==c['name'])
  audit=read(f"build/study154-{s['group']}-final/project/association-labels134/audits/{e['auditId']}.json")
  correct153=0;accepted153=0
  for label in audit['scores']:
   box=e['trace'][label['ordinal']-1]['rotation']['box']
   if label['visibility'] in ['visible','partial'] and box is not None:
    accepted153+=1;correct153+=iou(box,label['targetBox'])>=.5
  rows.append({'case':c['name'],'domain':'new rendered control' if s['group']=='generated' else 'exposed edition150 camera','baseline':c['baseline'],'previous153':{'correct':correct153,'visible':e['summary']['visible'],'missing':e['summary']['visible']-accepted153},'summary':e['summary'],'preservedOriginalBoxes':q['preservedOriginalBoxes'],'newlyPreservedComparedWith153':q['newlyPreservedComparedWith153'],'totalMs':c['result']['totalMs'],'cost':c['result']['cost154'],'warmMedianMs':q['warmMedianMs'],'experimentId':e['id']})
 for c in s['captures']:
  p=(r/path).parent/c['name'];assert sha(p.read_bytes())==c['sha256'] and c['privatePixelsExcluded'];name=s['group']+'-'+c['name'];shutil.copy2(p,out/name);shots.append({'path':'docs/readmes/154/'+name,'title':'2026-11-17 · '+s['group']+' / '+c['name']})
camera=rows[-1];cs=camera['summary'];old=read('docs/readmes/153/exposed150-study.json')['cases'][0]['result']['experiment'];last=studies[-1]
finding=f"The explicit Java comparison preserves supported original tracking and retains every confirmed 153 return. On the exposed camera recording it scores {cs['correct']}/{cs['visible']} correct visible/partial observations, versus {old['summary']['correct']}/{old['summary']['visible']} with 153; {cs['missing']} remain withheld. There are {cs['overlapFailures']} wrong overlaps, {cs['unsupported']}/{cs['hidden']} hidden outputs and {cs['confirmedDistractorMatches']} matches to the labelled red object. All six new generated cases pass their declared outcome gates. Warm reopening takes {camera['warmMedianMs']/1000:.3f} seconds median, with full byte checks; cold or forced verification still reconstructs. Defaults remain unchanged."
table='| Source | Original correct | Nested 153 correct | Edition 154 correct | Withheld | Wrong / hidden / other | Full request s | Warm median s |\n| --- | ---: | ---: | ---: | ---: | --- | ---: | ---: |\n'
for x in rows:
 b=x['baseline'];p=x['previous153'];s=x['summary'];table+=f"| {x['case']} | {b['correct']}/{b['visible']} | {p['correct']}/{p['visible']} | {s['correct']}/{s['visible']} | {s['missing']} | {s['overlapFailures']} / {s['unsupported']} of {s['hidden']} / {s['confirmedDistractorMatches']} | {x['totalMs']/1000:.2f} | {x['warmMedianMs']/1000:.3f} |\n"
cameraText=f"The camera improvement is {camera['newlyPreservedComparedWith153']} preserved initial observations; it is not additional recovery after occlusion. The same 23/30 returned frames remain confirmed, and the same 7 remain withheld. The nested 153 trace matches the published trace exactly. {camera['preservedOriginalBoxes']} original camera boxes are retained numerically unchanged where continuity qualifies. The other accepted observations come from unchanged return confirmation."
cost=f"On the exposed camera, the first complete request took **{camera['totalMs']/1000:.3f}seconds**; explicit full replay took **{last['forced']['totalMs']/1000:.3f}seconds**; cold restarted verification took **{last['restartMs']/1000:.3f}seconds**. Three unchanged warm reads had a median of **{camera['warmMedianMs']/1000:.3f}seconds**. These are different workloads.\n\n| Complete forced comparison | Repeated history access median s | Verified manifest access median s | History reads per pair |\n| --- | ---: | ---: | --- |\n"
for key,m in measures.items():
 ref=[v for v in m['measurements'] if not v['snapshotImages']];fast=[v for v in m['measurements'] if v['snapshotImages']];cost+=f"| {key} | {statistics.median(v['totalMs'] for v in ref)/1000:.3f} | {statistics.median(v['totalMs'] for v in fast)/1000:.3f} | {ref[0]['cost']['historyReads']} → {fast[0]['cost']['historyReads']} |\n"
cost+='\nThe generated source uses three alternating pairs; the camera uses one pair. Both paths reconstruct identical complete results and leave the project unchanged. Java21, two active processors and a768MiB heap were used. A single camera pair cannot establish a stable speed advantage; the byte/read reduction and measured timing are reported separately. No outcome or source check is removed. [Exact timings](qualification.json) are retained in the qualification receipt.'
cost=cost.replace('(qualification.json)','(docs/readmes/154/qualification.json)').replace('seconds',' seconds').replace('Java21','Java 21').replace('a768MiB','a 768 MiB')
attempts='The first nine-check development suite passed, followed by 35 selected regression checks. An editing-time character-encoding issue in the desktop source was detected in the diff and corrected before freezing the method; historical text and tracker code were preserved. The method and qualification gates were committed at 11417dcb before generating reserved 154 sources. '
failed=[]
for p in sorted((r/'build').glob('study154-*/result.json')):
 v=json.loads(p.read_bytes())
 if not v.get('passed'):failed.append(p.relative_to(r).as_posix())
attempts+=('Failed or incomplete app attempts are preserved: '+', '.join(failed)+'.') if failed else 'The first generated and camera application attempts passed their execution checks; no failed reserved app attempt was omitted.'
verification={'passed':True,'edition':154,'javaChecks':35,'desktopChecks':164,'appComparisons':len(rows),'sourceGeometryChecks':source,'qualification':qualification,'defaultsChanged':False,'promoted':False,'freshCameraTest':'not performed; user selected recorded tests only','scope':validation}
write(out/'verification.json',verification);write(out/'comparison.json',rows);write(out/'qualification.json',qualification)
originals=[];archive=paths+failed+['build/ci154-start.json','build/app-visual-review154.json','build/source-check154.json','build/qualification154.json','build/replay154-generated.json','build/replay154-camera.json','build/preflight154.log','build/regression154.log','build/desktop154.log','build/study095-build.json','docs/OBSERVATION_154_LOCK.json']+[p.relative_to(r).as_posix() for p in xmls]+[p.relative_to(r).as_posix() for p in (r/'build/validation154/preflight').glob('*.xml')]
for name in dict.fromkeys(archive):
 raw=(r/name).read_bytes();check_public(json.loads(raw)) if name.endswith('.json') else None;packed=gzip.compress(raw,mtime=0);p=out/'originals'/(sha(raw)+'.gz');p.parent.mkdir(exist_ok=True);p.write_bytes(packed);originals.append({'logicalPath':name,'path':p.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(raw),'rawSha256':sha(raw)})
write(out/'original-evidence.json',{'files':originals,'privatePixelsPublished':False})
note='docs/obsidian/162-continuity-replay.md';modules=[{'id':key,'name':name,'source':file,'note':note} for key,name,file in [('search','Supported original continuity','workspace-engine/src/main/java/fraymus/workspace/ContinuityReturn154.java'),('confirm','Unchanged return confirmation','workspace-engine/src/main/java/fraymus/workspace/RotationReturn153.java'),('proof','Exact evidence and replay costs','workspace-engine/src/main/java/fraymus/workspace/ContinuityExperiment154.java'),('view','Actual controls and progress','workspace-desktop/renderer/association134-panel.js'),('measure','Complete access-path comparison','docs/publication154/ReplayMeasure154.java')]]
system={'modules':modules,'screenshots':shots,'evidence':[{'path':'docs/readmes/154/'+n} for n in ['verification.json','comparison.json','qualification.json','original-evidence.json']],'comparison':rows,'finding':finding,'validation':validation};write(out/'system-map.json',system)
text(r/note,'# Preserved continuity and verified replay — edition154\n\n[[README-154.md]]\n\n'+finding+'\n\n'+validation+'\n\nNext: varied recorded continuity, remaining cold cost, and source-linked durable tasks across the unified application.\n')
write(r/'docs/obsidian/162-continuity-replay.canvas',{'nodes':[{'id':m['id'],'type':'file','file':m['source'],'x':i*430,'y':0,'width':390,'height':200} for i,m in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']} for i in range(len(modules)-1)]})
md=(r/'docs/publication154/README-template.md').read_text(encoding='utf8')
for key,value in {'FINDING':finding,'HERO':'![Actual continuous tracking and replay controls]('+shots[0]['path']+')','TABLE':table,'CAMERA':cameraText,'COST':cost,'VALIDATION':validation,'ATTEMPTS':attempts}.items():md=md.replace('{{'+key+'}}',value)
assert '{{' not in md;text(r/'README-154.md',md);text(r/'README.md',md)
progress='## Edition 154 — locally verified; publication pending\n\n2026-11-17. Java target eyeoverthink/Fraynix-Agent-Workspace; isolated J:/Codex-Work/fraynix-workspace-edition154. No subagents. One heavy check at a time. Edition153 CI passed before implementation.\n\n'+finding+'\n\n'+validation+'\n\n'+attempts+'\n\nRead README-154.md and docs/publication154/plan154.md. User selected recorded tests only; no camera active. Preserve the unified Java world/browser/creation direction, historical tracker identities, original private projects and defaults. No automatic appearance admission or physical action authority. Cold/forced reconstruction and byte-bound warm reuse are distinct costs. Hosted CI separate from local validation.\n\n'
for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
 p=r/name;old=p.read_text(encoding='utf-8-sig');assert not old.startswith('## Edition 154');text(p,progress+old)
for name in ['docs/INTEGRATED_AGENT_ROADMAP.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md']:
 p=r/name;text(p,'## Edition 154 continuity — 2026-11-17\n\n'+finding+'\n\nRead [edition154](../README-154.md). Keep the unified Java agent/world/browser/creation direction. Next: varied continuity, remaining cold replay cost, evidence-linked durable tasks.\n\n'+p.read_text(encoding='utf-8-sig'))
for name,prefix in [('docs/readmes/INDEX.md','[Edition 154 — preserved tracking and verified replay](../../README-154.md) | [Offline guide](154/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/162-continuity-replay.md]] | [[README-154.md]]')]:
 p=r/name;text(p,prefix+'\n\n'+p.read_text(encoding='utf-8-sig'))
inventory=[];names={x['path'] for x in read('docs/readmes/153/file-inventory.json')};names.update(lock['sourceHashes']);names.add('docs/OBSERVATION_154_LOCK.json');names.update(p.relative_to(r).as_posix() for p in (r/'docs/publication154').iterdir() if p.is_file())
for name in sorted(names):
 raw=(r/name).read_bytes()
 try:raw=raw.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':name,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
write(out/'file-inventory.json',inventory)
html=(r/'docs/publication154/guide-template.html').read_text(encoding='utf8')+'<script id="system-data" type="application/json">'+json.dumps(system).replace('</','<\\/')+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory).replace('</','<\\/')+'</script><script src="guide.js"></script></body></html>';text(out/'index.html',html);shutil.copy2(r/'docs/publication154/guide.js',out/'guide.js')
print(json.dumps({'passed':True,'sources':len(inventory),'evidence':len(originals),'screenshots':len(shots)}))
