from pathlib import Path
import json,hashlib,gzip,shutil,xml.etree.ElementTree as ET,subprocess,sys,statistics
r=Path.cwd();out=r/'docs/readmes/155';out.mkdir(parents=True,exist_ok=True);sha=lambda b:hashlib.sha256(b).hexdigest()
sys.path.insert(0,str(r/'docs/publication149'));from public_evidence149 import check_public
def read(p):return json.loads((r/p).read_bytes())
def write(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes((json.dumps(v,indent=2,ensure_ascii=False)+'\n').encode())
def text(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(s.encode())
assert subprocess.run(['git','show-ref','--quiet','--verify','refs/tags/readme-155']).returncode==1
paths=['build/study155-generated-final/result.json','build/study155-exposed150-final/result.json'];studies=[read(p) for p in paths];assert all(s['passed'] for s in studies)
lock=read('docs/OBSERVATION_155_LOCK.json')
for name,h in lock['sourceHashes'].items():assert sha((r/name).read_bytes().replace(b'\r\n',b'\n'))==h,name
for s in studies:assert s['methodLockSha256']==sha((r/'docs/OBSERVATION_155_LOCK.json').read_bytes());check_public(s)
review=read('build/app-visual-review155.json');assert review['passed'];assert {x['sha256'] for x in review['images']}=={x['sha256'] for s in studies for x in s['captures']}
checks=set();xmls=list((r/'build/validation155/final').glob('*.xml'))
for p in xmls:
 e=ET.parse(p).getroot();assert sum(int(e.get(k,'0')) for k in ['failures','errors','skipped'])==0
 for c in e.findall('testcase'):checks.add(c.get('classname')+'.'+c.get('name'))
assert len(checks)==44
desktopRaw=(r/'build/desktop155.log').read_bytes();desktop=desktopRaw.decode('utf16' if desktopRaw.startswith(b'\xff\xfe') else 'utf8');assert 'tests 164' in desktop and 'fail 0' in desktop
source=read('build/source-check155.json');qualification=read('build/qualification155.json');assert source['passed'] and qualification['passed']
measures={key:read(f'build/replay155-{key}.json') for key in ['generated','camera']};assert all(m['passed'] for m in measures.values())
validation='44 selected Java checks and 164 desktop checks passed, with no failures or skipped cases. Seven new generated app comparisons and one exposed-camera comparison passed visible-control, forced-replay, cold-restart and source-integrity checks. Complete reference/grid-search evidence matches exactly. Independent geometry and exact-evidence gates passed; harder behavioral targets and measured cost benefit are reported separately. These are scoped checks, not a rerun of every historical capability. Recorded tests only.'
def iou(a,b):
 x=max(0,min(a[2],b[2])-max(a[0],b[0]));y=max(0,min(a[3],b[3])-max(a[1],b[1]));v=x*y
 return v/((a[2]-a[0])*(a[3]-a[1])+(b[2]-b[0])*(b[3]-b[1])-v)
rows=[];shots=[]
for path,s in zip(paths,studies):
 write(out/(s['group']+'-study.json'),s)
 for c in s['cases']:
  e=c['result']['experiment'];q=next(x for x in qualification['cases'] if x['case']==c['name'])
  audit=read(f"build/study155-{s['group']}-final/project/association-labels134/audits/{e['auditId']}.json")
  correct153=0;accepted153=0
  for label in audit['scores']:
   box=e['trace'][label['ordinal']-1]['rotation']['box']
   if label['visibility'] in ['visible','partial'] and box is not None:
    accepted153+=1;correct153+=iou(box,label['targetBox'])>=.5
  rows.append({'case':c['name'],'domain':'new rendered control' if s['group']=='generated' else 'exposed edition150 camera','baseline':c['baseline'],'previous153':{'correct':correct153,'visible':e['summary']['visible'],'missing':e['summary']['visible']-accepted153},'summary':e['summary'],'preservedOriginalBoxes':q['preservedOriginalBoxes'],'newlyPreservedComparedWith153':q['newlyPreservedComparedWith153'],'totalMs':c['result']['totalMs'],'cost':c['result']['cost155'],'warmMedianMs':q['warmMedianMs'],'experimentId':e['id']})
 for c in s['captures']:
  p=(r/path).parent/c['name'];assert sha(p.read_bytes())==c['sha256'] and c['privatePixelsExcluded'];name=s['group']+'-'+c['name'];shutil.copy2(p,out/name);shots.append({'path':'docs/readmes/155/'+name,'title':'2026-11-17 · '+s['group']+' / '+c['name']})
camera=rows[-1];cs=camera['summary'];old=read('docs/readmes/154/exposed150-study.json')['cases'][0]['result']['experiment'];last=studies[-1]
behaviorCount=sum(x['behavioralTargetPassed'] for x in qualification['cases'] if x['case']!='exposed150-camera')
finding=f"Edition 155 reconstructs exactly the same evidence while reusing coordinate calculations. The exposed camera trace remains {cs['correct']}/{cs['visible']} correct observations, {cs['missing']} withheld, {cs['overlapFailures']} wrong overlaps and {cs['unsupported']}/{cs['hidden']} hidden outputs, identical to 154. All eight app comparisons reproduce exact reference evidence. {behaviorCount}/7 harder generated cases meet their separate behavioral targets. Complete paired costs include the original verification. Defaults remain unchanged."

table='| Source | Original correct | Nested 153 correct | Edition 155 correct | Withheld | Wrong / hidden / other | Full request s | Warm median s |\n| --- | ---: | ---: | ---: | ---: | --- | ---: | ---: |\n'
for x in rows:
 b=x['baseline'];p=x['previous153'];s=x['summary'];table+=f"| {x['case']} | {b['correct']}/{b['visible']} | {p['correct']}/{p['visible']} | {s['correct']}/{s['visible']} | {s['missing']} | {s['overlapFailures']} / {s['unsupported']} of {s['hidden']} / {s['confirmedDistractorMatches']} | {x['totalMs']/1000:.2f} | {x['warmMedianMs']/1000:.3f} |\n"
cameraText=f"The complete camera trace, scores, source identities and candidate counts equal published154. Accuracy remains34/42, including the same23/30 confirmed returned frames. Eight observations remain withheld: seven returned frames and one earlier partial view. Coordinate optimization adds no new acceptance or physical-accuracy claim. {camera['preservedOriginalBoxes']} original camera boxes remain numerically unchanged."
cost=f"On the exposed camera, the first complete request took **{camera['totalMs']/1000:.3f} seconds**; explicit full replay **{last['forced']['totalMs']/1000:.3f} seconds**; cold restarted verification **{last['restartMs']/1000:.3f} seconds**. Three unchanged warm readings had a median of **{camera['warmMedianMs']/1000:.3f} seconds**.\n\n| Complete forced comparison | Original coordinates median s | Grid coordinates median s | Pairs | Ratio |\n| --- | ---: | ---: | ---: | ---: |\n"
for key,m in measures.items():
 ref=[v for v in m['measurements'] if not v['gridSearch']];fast=[v for v in m['measurements'] if v['gridSearch']];a=statistics.median(v['totalMs'] for v in ref);b=statistics.median(v['totalMs'] for v in fast);cost+=f"| {key} | {a/1000:.3f} | {b/1000:.3f} | {len(ref)} | {a/b:.3f} |\n"
cost+='\nThree alternating pairs measure the generated direction-reversal case; two pairs measure the exposed camera. Each of the six other generated cases also has one complete reference/grid pair. All produce identical complete evidence and unchanged project inventories. Java21, two active processors and768MiB heap. These repeated local measurements do not guarantee a universal speed advantage. [Every timing](docs/readmes/155/qualification.json), including phase costs, remains available.'
cost=cost.replace('Java21','Java 21').replace('and768MiB','and a 768 MiB')

attempts='The first nine Java checks passed, followed by44selected regression checks. An exposed six-frame camera search probe produced exact traces in9.517seconds through the reference path and5.425seconds through the grid path; this development probe excludes full reconstruction. A trailing-whitespace commit check was corrected before freezing. Method and qualification gates were committed at5ce752d8 before rendering the new sources. '.replace('by44selected','by 44 selected').replace('in9.517seconds','in 9.517 seconds').replace('and5.425seconds','and 5.425 seconds').replace('at5ce752d8','at 5ce752d8')

attempts+='The first study launcher stopped on a normal Java stderr startup notice before any source generation or app study; native exit-code handling was corrected and the passed tests were retained. '
attempts+='The next launcher correctly refused an installed JAR older than a whitespace-only source edit; an explicit rebuild refreshed the installed artifacts before source generation. '
attempts+='The documentation reader was corrected to read the original UTF-16 desktop log; the original log bytes remain unchanged. '
failed=[]
for p in sorted((r/'build').glob('study155-*/result.json')):
 v=json.loads(p.read_bytes())
 if not v.get('passed'):failed.append(p.relative_to(r).as_posix())
attempts+=('Failed or incomplete app attempts are preserved: '+', '.join(failed)+'.') if failed else 'The first generated and camera application attempts passed their execution checks; no failed reserved app attempt was omitted.'
verification={'passed':True,'edition':155,'javaChecks':44,'desktopChecks':164,'appComparisons':len(rows),'sourceGeometryChecks':source,'qualification':qualification,'defaultsChanged':False,'promoted':False,'freshCameraTest':'not performed; user selected recorded tests only','scope':validation}
write(out/'verification.json',verification);write(out/'comparison.json',rows);write(out/'qualification.json',qualification)
originals=[];archive=paths+failed+[p.relative_to(r).as_posix() for p in sorted((r/'build').glob('replay155-*.json'))]+['build/probe155.json','build/probe155.log','build/studies155.log','build/studies155-resumed.log']+['build/ci155-start.json','build/ci155-during-measurement.json','build/app-visual-review155.json','build/source-check155.json','build/qualification155.json','build/replay155-generated.json','build/replay155-camera.json','build/preflight155.log','build/regression155.log','build/desktop155.log','build/study095-build.json','docs/OBSERVATION_155_LOCK.json']+[p.relative_to(r).as_posix() for p in xmls]+[p.relative_to(r).as_posix() for p in (r/'build/validation155/preflight').glob('*.xml')]
for name in dict.fromkeys(archive):
 raw=(r/name).read_bytes();check_public(json.loads(raw)) if name.endswith('.json') else None;packed=gzip.compress(raw,mtime=0);p=out/'originals'/(sha(raw)+'.gz');p.parent.mkdir(exist_ok=True);p.write_bytes(packed);originals.append({'logicalPath':name,'path':p.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(raw),'rawSha256':sha(raw)})
write(out/'original-evidence.json',{'files':originals,'privatePixelsPublished':False})
note='docs/obsidian/163-exact-reconstruction.md';modules=[{'id':key,'name':name,'source':file,'note':note} for key,name,file in [('search','Exact coordinate grid','workspace-engine/src/main/java/fraymus/workspace/RotationSearch155.java'),('confirm','Unchanged confirmation rules','workspace-engine/src/main/java/fraymus/workspace/RotationReturn155.java'),('proof','Exact evidence and replay costs','workspace-engine/src/main/java/fraymus/workspace/ContinuityExperiment155.java'),('view','Actual controls and progress','workspace-desktop/renderer/association134-panel.js'),('measure','Complete coordinate-path comparison','docs/publication155/ReplayMeasure155.java')]]
system={'modules':modules,'screenshots':shots,'evidence':[{'path':'docs/readmes/155/'+n} for n in ['verification.json','comparison.json','qualification.json','original-evidence.json']],'comparison':rows,'finding':finding,'validation':validation};write(out/'system-map.json',system)
text(r/note,'# Exact reconstruction and harder returns — edition155\n\n[[README-155.md]]\n\n'+finding+'\n\n'+validation+'\n\nNext: remaining original-verification cost, unresolved harder behaviors, and source-linked durable tasks across the unified application.\n')
write(r/'docs/obsidian/163-exact-reconstruction.canvas',{'nodes':[{'id':m['id'],'type':'file','file':m['source'],'x':i*430,'y':0,'width':390,'height':200} for i,m in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']} for i in range(len(modules)-1)]})
md=(r/'docs/publication155/README-template.md').read_text(encoding='utf8')
for key,value in {'FINDING':finding,'HERO':'![Actual continuous tracking and replay controls]('+shots[0]['path']+')','TABLE':table,'CAMERA':cameraText,'COST':cost,'VALIDATION':validation,'ATTEMPTS':attempts}.items():md=md.replace('{{'+key+'}}',value)
assert '{{' not in md;text(r/'README-155.md',md);text(r/'README.md',md)
progress='## Edition 155 — locally verified; publication pending\n\n2026-11-17. Java target eyeoverthink/Fraynix-Agent-Workspace; isolated J:/Codex-Work/fraynix-workspace-edition155. No subagents. One heavy check at a time. Edition154 CI was still running at the initial check; preceding153 runs passed. See the publication receipt for the latest check.\n\n'+finding+'\n\n'+validation+'\n\n'+attempts+'\n\nRead README-155.md and docs/publication155/plan155.md. User selected recorded tests only; no camera active. Preserve the unified Java world/browser/creation direction, historical tracker identities, original private projects and defaults. No automatic appearance admission or physical action authority. Cold/forced reconstruction and byte-bound warm reuse are distinct costs. Hosted CI separate from local validation.\n\n'
for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
 p=r/name;old=p.read_text(encoding='utf-8-sig');assert not old.startswith('## Edition 155');text(p,progress+old)
for name in ['docs/INTEGRATED_AGENT_ROADMAP.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md']:
 p=r/name;text(p,'## Edition 155 continuity — 2026-11-17\n\n'+finding+'\n\nRead [edition155](../README-155.md). Keep the unified Java agent/world/browser/creation direction. Next: varied continuity, remaining cold replay cost, evidence-linked durable tasks.\n\n'+p.read_text(encoding='utf-8-sig'))
for name,prefix in [('docs/readmes/INDEX.md','[Edition 155 — exact reconstruction and harder returns](../../README-155.md) | [Offline guide](155/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/163-exact-reconstruction.md]] | [[README-155.md]]')]:
 p=r/name;text(p,prefix+'\n\n'+p.read_text(encoding='utf-8-sig'))
inventory=[];names={x['path'] for x in read('docs/readmes/154/file-inventory.json')};names.update(lock['sourceHashes']);names.add('docs/OBSERVATION_155_LOCK.json');names.update(p.relative_to(r).as_posix() for p in (r/'docs/publication155').iterdir() if p.is_file())
for name in sorted(names):
 raw=(r/name).read_bytes()
 try:raw=raw.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':name,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
write(out/'file-inventory.json',inventory)
html=(r/'docs/publication155/guide-template.html').read_text(encoding='utf8')+'<script id="system-data" type="application/json">'+json.dumps(system).replace('</','<\\/')+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory).replace('</','<\\/')+'</script><script src="guide.js"></script></body></html>';text(out/'index.html',html);shutil.copy2(r/'docs/publication155/guide.js',out/'guide.js')
print(json.dumps({'passed':True,'sources':len(inventory),'evidence':len(originals),'screenshots':len(shots)}))
