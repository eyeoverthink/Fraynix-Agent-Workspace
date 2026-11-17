from pathlib import Path
import json,hashlib,gzip,shutil,xml.etree.ElementTree as ET,subprocess,sys,statistics
r=Path.cwd();out=r/'docs/readmes/157';out.mkdir(parents=True,exist_ok=True);sha=lambda b:hashlib.sha256(b).hexdigest()
sys.path.insert(0,str(r/'docs/publication149'));from public_evidence149 import check_public
def read(p):return json.loads((r/p).read_bytes())
def write(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes((json.dumps(v,indent=2,ensure_ascii=False)+'\n').encode())
def text(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(s.encode())
assert subprocess.run(['git','show-ref','--quiet','--verify','refs/tags/readme-157']).returncode==1
paths=['build/study157-generated-final/result.json','build/study157-exposed150-final/result.json'];studies=[read(p) for p in paths];assert all(s['passed'] for s in studies)
lock=read('docs/OBSERVATION_157_LOCK.json')
for name,h in lock['sourceHashes'].items():assert sha((r/name).read_bytes().replace(b'\r\n',b'\n'))==h,name
for s in studies:assert s['methodLockSha256']==sha((r/'docs/OBSERVATION_157_LOCK.json').read_bytes());check_public(s)
review=read('build/app-visual-review157.json');assert review['passed'];assert {x['sha256'] for x in review['images']}=={x['sha256'] for s in studies for x in s['captures']}
checks=set();xmls=list((r/'build/validation157/final').glob('*.xml'))
for p in xmls:
 e=ET.parse(p).getroot();assert sum(int(e.get(k,'0')) for k in ['failures','errors','skipped'])==0
 for c in e.findall('testcase'):checks.add(c.get('classname')+'.'+c.get('name'))
assert len(checks)==57
desktopRaw=(r/'build/desktop157.log').read_bytes();desktop=desktopRaw.decode('utf16' if desktopRaw.startswith(b'\xff\xfe') else 'utf8');assert 'tests 164' in desktop and 'fail 0' in desktop
source=read('build/source-check157.json');qualification=read('build/qualification157.json');assert source['passed'] and qualification['passed']
measures={key:read(f'build/replay157-{key}.json') for key in ['generated','camera']};assert all(m['passed'] for m in measures.values())
validation='57 selected Java checks and 164 desktop checks passed, with no failures or skipped cases. Two new generated app comparisons and one exposed-camera comparison passed visible-control, forced-replay, cold-restart and source-integrity checks. Complete reference/precomputed evidence matches exactly. Independent geometry and exact-evidence gates passed; harder behavioral targets and measured cost benefit are reported separately. These are scoped checks, not a rerun of every historical capability. Recorded tests only.'
def iou(a,b):
 x=max(0,min(a[2],b[2])-max(a[0],b[0]));y=max(0,min(a[3],b[3])-max(a[1],b[1]));v=x*y
 return v/((a[2]-a[0])*(a[3]-a[1])+(b[2]-b[0])*(b[3]-b[1])-v)
rows=[];shots=[]
for path,s in zip(paths,studies):
 write(out/(s['group']+'-study.json'),s)
 for c in s['cases']:
  e=c['result']['experiment'];q=next(x for x in qualification['cases'] if x['case']==c['name'])
  audit=read(f"build/study157-{s['group']}-final/project/association-labels134/audits/{e['auditId']}.json")
  correct153=0;accepted153=0
  for label in audit['scores']:
   box=e['trace'][label['ordinal']-1]['rotation']['box']
   if label['visibility'] in ['visible','partial'] and box is not None:
    accepted153+=1;correct153+=iou(box,label['targetBox'])>=.5
  rows.append({'case':c['name'],'domain':'new rendered control' if s['group']=='generated' else 'exposed edition150 camera','baseline':c['baseline'],'previous153':{'correct':correct153,'visible':e['summary']['visible'],'missing':e['summary']['visible']-accepted153},'summary':e['summary'],'preservedOriginalBoxes':q['preservedOriginalBoxes'],'newlyPreservedComparedWith153':q['newlyPreservedComparedWith153'],'totalMs':c['result']['totalMs'],'cost':c['result']['cost157'],'warmMedianMs':q['warmMedianMs'],'experimentId':e['id']})
 for c in s['captures']:
  p=(r/path).parent/c['name'];assert sha(p.read_bytes())==c['sha256'] and c['privatePixelsExcluded'];name=s['group']+'-'+c['name'];shutil.copy2(p,out/name);shots.append({'path':'docs/readmes/157/'+name,'title':'2026-11-17 · '+s['group']+' / '+c['name']})
camera=rows[-1];cs=camera['summary'];old=read('docs/readmes/156/exposed150-study.json')['cases'][0]['result']['experiment'];last=studies[-1]
behaviorCount=sum(x['behavioralTargetPassed'] for x in qualification['cases'] if x['case']!='exposed150-camera')
finding=f"Edition 157 reconstructs exactly the same evidence while reusing exact sampling coordinates and immutable reference calculations on fresh frame pixels. The exposed camera trace remains {cs['correct']}/{cs['visible']} correct observations, {cs['missing']} withheld, {cs['overlapFailures']} wrong overlaps and {cs['unsupported']}/{cs['hidden']} hidden outputs, identical to 156. All three app comparisons reproduce exact reference evidence. {behaviorCount}/2 new generated cases meet their separate behavioral targets. Complete paired costs include the original verification. Defaults remain unchanged."

cm=measures['camera']['measurements'];cr=statistics.median(v['totalMs'] for v in cm if not v['precomputedOriginalReplay']);cf=statistics.median(v['totalMs'] for v in cm if v['precomputedOriginalReplay'])
finding+=f" On two alternating exposed-camera pairs, complete forced verification measured {cr/1000:.2f} seconds through published 156 and {cf/1000:.2f} seconds with precomputed terms ({abs(1-cf/cr)*100:.1f}% {'less' if cf<cr else 'more'} time)."

table='| Source | Original correct | Nested 153 correct | Edition 157 correct | Withheld | Wrong / hidden / other | Full request s | Warm median s |\n| --- | ---: | ---: | ---: | ---: | --- | ---: | ---: |\n'
for x in rows:
 b=x['baseline'];p=x['previous153'];s=x['summary'];table+=f"| {x['case']} | {b['correct']}/{b['visible']} | {p['correct']}/{p['visible']} | {s['correct']}/{s['visible']} | {s['missing']} | {s['overlapFailures']} / {s['unsupported']} of {s['hidden']} / {s['confirmedDistractorMatches']} | {x['totalMs']/1000:.2f} | {x['warmMedianMs']/1000:.3f} |\n"
cameraText=f"The complete camera trace, scores, source identities and candidate counts equal published 156. Accuracy remains 34/42, including the same 23/30 confirmed returned frames. Eight observations remain withheld: seven returned frames and one earlier partial view. Original-audit optimization adds no new acceptance or physical-accuracy claim. {camera['preservedOriginalBoxes']} original camera boxes remain numerically unchanged."
cost=f"On the exposed camera, the first complete request took **{camera['totalMs']/1000:.3f} seconds**; explicit full replay **{last['forced']['totalMs']/1000:.3f} seconds**; cold restarted verification **{last['restartMs']/1000:.3f} seconds**. Three unchanged warm readings had a median of **{camera['warmMedianMs']/1000:.3f} seconds**.\n\n| Complete forced comparison | Published 156 median s | Precomputed terms median s | Pairs | Ratio | Original audit phase, 156 → 157 s |\n| --- | ---: | ---: | ---: | ---: | --- |\n"
for key,m in measures.items():
 ref=[v for v in m['measurements'] if not v['precomputedOriginalReplay']];fast=[v for v in m['measurements'] if v['precomputedOriginalReplay']];a=statistics.median(v['totalMs'] for v in ref);b=statistics.median(v['totalMs'] for v in fast);cost+=f"| {key} | {a/1000:.3f} | {b/1000:.3f} | {len(ref)} | {a/b:.3f} | {statistics.median(v['cost']['originalReplayMs'] for v in ref)/1000:.3f} → {statistics.median(v['cost']['originalReplayMs'] for v in fast)/1000:.3f} |\n"
cost+='\nThree alternating pairs measure the generated long-occlusion return case; two pairs measure the exposed camera. The other generated case has one complete 156/157 pair. All produce identical complete evidence and unchanged project inventories. Java21, two active processors and768MiB heap. These repeated local measurements do not guarantee a universal speed advantage. [Every timing](docs/readmes/157/qualification.json), including phase costs, remains available.'
cost=cost.replace('Java21','Java 21').replace('and768MiB','and a 768 MiB')

probe=read('build/probe157.json');assert probe['passed'];pm=probe['measurements']
attempts=f"An exposed original-audit development probe took {pm[0]['totalMs']/1000:.3f} seconds through the published 156 path and {pm[1]['totalMs']/1000:.3f} seconds with exact precomputed terms, with complete audit equality and unchanged source files. This probe excludes the full rotation experiment. The 13 new checks passed before 44 selected regression checks; their distinct results cover the same final Java implementation. The method and qualification gates were committed at {studies[0]['source'][:8]} before rendering the reserved sources. "
failed=[]
for p in sorted((r/'build').glob('study157-*/result.json')):
 v=json.loads(p.read_bytes())
 if not v.get('passed'):failed.append(p.relative_to(r).as_posix())
attempts+=('Failed or incomplete app attempts are preserved: '+', '.join(failed)+'.') if failed else 'The first generated and camera application attempts passed their execution checks; no failed reserved app attempt was omitted.'
verification={'passed':True,'edition':157,'javaChecks':57,'desktopChecks':164,'appComparisons':len(rows),'sourceGeometryChecks':source,'qualification':qualification,'defaultsChanged':False,'promoted':False,'freshCameraTest':'not performed; user selected recorded tests only','scope':validation}
write(out/'verification.json',verification);write(out/'comparison.json',rows);write(out/'qualification.json',qualification)
originals=[];archive=paths+failed+[p.relative_to(r).as_posix() for p in sorted((r/'build').glob('replay157-*.json'))]+['build/probe157.json','build/probe157.log','build/profile157/receipt.json','build/profile157/report.json','build/profile157/samples.json','build/profile157/run.log']+['build/ci157-start.json','build/app-visual-review157.json','build/source-check157.json','build/qualification157.json','build/replay157-generated.json','build/replay157-camera.json','build/preflight157.log','build/regression157.log','build/desktop157.log','build/study095-build.json','docs/OBSERVATION_157_LOCK.json']+[p.relative_to(r).as_posix() for p in xmls]+[p.relative_to(r).as_posix() for p in (r/'build/validation157/preflight').glob('*.xml')]
for name in dict.fromkeys(archive):
 raw=(r/name).read_bytes();check_public(json.loads(raw)) if name.endswith('.json') else None;packed=gzip.compress(raw,mtime=0);p=out/'originals'/(sha(raw)+'.gz');p.parent.mkdir(exist_ok=True);p.write_bytes(packed);originals.append({'logicalPath':name,'path':p.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(raw),'rawSha256':sha(raw)})
write(out/'original-evidence.json',{'files':originals,'privatePixelsPublished':False})
note='docs/obsidian/165-precomputed-verification.md';modules=[{'id':key,'name':name,'source':file,'note':note} for key,name,file in [('search','Exact sampled values and reference terms','workspace-engine/src/main/java/fraymus/workspace/StructureReplay157.java'),('confirm','Exact original identity replay','workspace-engine/src/main/java/fraymus/workspace/IdentityReplay157.java'),('proof','Exact evidence and replay costs','workspace-engine/src/main/java/fraymus/workspace/ContinuityExperiment157.java'),('view','Actual controls and progress','workspace-desktop/renderer/association134-panel.js'),('measure','Complete original-replay comparison','docs/publication157/ReplayMeasure157.java')]]
system={'modules':modules,'screenshots':shots,'evidence':[{'path':'docs/readmes/157/'+n} for n in ['verification.json','comparison.json','qualification.json','original-evidence.json']],'comparison':rows,'costComparison':qualification['costSummary'],'finding':finding,'validation':validation};write(out/'system-map.json',system)
text(r/note,'# Precomputed original verification — edition157\n\n[[README-157.md]]\n\n'+finding+'\n\n'+validation+'\n\nNext: measured remaining reconstruction work and source-linked durable tasks across the unified application.\n')
write(r/'docs/obsidian/165-precomputed-verification.canvas',{'nodes':[{'id':m['id'],'type':'file','file':m['source'],'x':i*430,'y':0,'width':390,'height':200} for i,m in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']} for i in range(len(modules)-1)]})
md=(r/'docs/publication157/README-template.md').read_text(encoding='utf8')
for key,value in {'FINDING':finding,'HERO':'![Actual continuous tracking and replay controls]('+shots[0]['path']+')','TABLE':table,'CAMERA':cameraText,'COST':cost,'VALIDATION':validation,'ATTEMPTS':attempts}.items():md=md.replace('{{'+key+'}}',value)
assert '{{' not in md;text(r/'README-157.md',md);text(r/'README.md',md)
progress='## Edition 157 — locally verified; publication pending\n\n2026-11-17. Java target eyeoverthink/Fraynix-Agent-Workspace; isolated J:/Codex-Work/fraynix-workspace-edition157. No subagents. One heavy check at a time. Edition 156 CI was still running at the initial check. See the publication receipt for the latest check.\n\n'+finding+'\n\n'+validation+'\n\n'+attempts+'\n\nRead README-157.md and docs/publication157/plan157.md. User selected recorded tests only; no camera active. Preserve the unified Java world/browser/creation direction, historical tracker identities, original private projects and defaults. No automatic appearance admission or physical action authority. Cold/forced reconstruction and byte-bound warm reuse are distinct costs. Hosted CI separate from local validation.\n\n'
for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
 p=r/name;old=p.read_text(encoding='utf-8-sig');assert not old.startswith('## Edition 157');text(p,progress+old)
for name in ['docs/INTEGRATED_AGENT_ROADMAP.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md']:
 p=r/name;text(p,'## Edition 157 continuity — 2026-11-17\n\n'+finding+'\n\nRead [edition157](../README-157.md). Keep the unified Java agent/world/browser/creation direction. Next: varied continuity, remaining cold replay cost, evidence-linked durable tasks.\n\n'+p.read_text(encoding='utf-8-sig'))
for name,prefix in [('docs/readmes/INDEX.md','[Edition 157 — precomputed original verification](../../README-157.md) | [Offline guide](157/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/165-precomputed-verification.md]] | [[README-157.md]]')]:
 p=r/name;text(p,prefix+'\n\n'+p.read_text(encoding='utf-8-sig'))
inventory=[];names={x['path'] for x in read('docs/readmes/156/file-inventory.json')};names.update(lock['sourceHashes']);names.add('docs/OBSERVATION_157_LOCK.json');names.update(p.relative_to(r).as_posix() for p in (r/'docs/publication157').iterdir() if p.is_file())
for name in sorted(names):
 raw=(r/name).read_bytes()
 try:raw=raw.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':name,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
write(out/'file-inventory.json',inventory)
html=(r/'docs/publication157/guide-template.html').read_text(encoding='utf8')+'<script id="system-data" type="application/json">'+json.dumps(system).replace('</','<\\/')+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory).replace('</','<\\/')+'</script><script src="guide.js"></script></body></html>';text(out/'index.html',html);shutil.copy2(r/'docs/publication157/guide.js',out/'guide.js')
print(json.dumps({'passed':True,'sources':len(inventory),'evidence':len(originals),'screenshots':len(shots)}))
