from pathlib import Path
import json,hashlib,gzip,shutil,xml.etree.ElementTree as ET,subprocess,sys,statistics
r=Path.cwd();out=r/'docs/readmes/153';out.mkdir(parents=True,exist_ok=True);sha=lambda b:hashlib.sha256(b).hexdigest()
sys.path.insert(0,str(r/'docs/publication149'));from public_evidence149 import check_public
def read(p):return json.loads((r/p).read_bytes())
def write(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes((json.dumps(v,indent=2,ensure_ascii=False)+'\n').encode())
def text(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(s.encode())
assert subprocess.run(['git','show-ref','--quiet','--verify','refs/tags/readme-153']).returncode==1
paths=['build/study153-generated-final/result.json','build/study153-exposed150-final/result.json'];studies=[read(p) for p in paths];assert all(s['passed'] for s in studies)
lock=read('docs/OBSERVATION_153_LOCK.json')
for name,h in lock['sourceHashes'].items():assert sha((r/name).read_bytes().replace(b'\r\n',b'\n'))==h,name
for s in studies:assert s['methodLockSha256']==sha((r/'docs/OBSERVATION_153_LOCK.json').read_bytes());check_public(s)
review=read('build/app-visual-review153.json');assert review['passed'];assert {x['sha256'] for x in review['images']}=={x['sha256'] for s in studies for x in s['captures']}
checks=set();xmls=list((r/'build/validation153/final').glob('*.xml'))
for p in xmls:
 e=ET.parse(p).getroot();assert sum(int(e.get(k,'0')) for k in ['failures','errors','skipped'])==0
 for c in e.findall('testcase'):checks.add(c.get('classname')+'.'+c.get('name'))
assert len(checks)==26
desktop=(r/'build/desktop153.log').read_text(encoding='utf8');assert 'tests 164' in desktop and 'fail 0' in desktop
source=read('build/source-check153.json');qualification=read('build/qualification153.json');assert source['passed'] and qualification['passed']
measure=read('build/recorded153-measured.json');controls=read('build/generated153-arithmetic.json');assert controls['passed'] and measure['exactTraceComparison']
validation='26 selected Java checks and 164 desktop checks passed, with no failures or skipped cases. Five generated app comparisons and one exposed-camera comparison passed execution, restart and source-integrity checks. Scalar and optimized traces match exactly on all six sources. Independent source geometry and separate outcome/cost gates passed. These are scoped checks, not a rerun of every historical capability. The user selected recorded tests only.'
rows=[];shots=[];old=read('docs/readmes/152/exposed150-study.json')['cases'][0]['result']['experiment']
for path,s in zip(paths,studies):
 write(out/(s['group']+'-study.json'),s)
 for c in s['cases']:
  e=c['result']['experiment'];previous=c['rotation152']['experiment']['summary'] if s['group']=='generated' else old['summary'];start=10 if s['group']=='generated' else 43
  rows.append({'case':c['name'],'domain':'new rendered control' if s['group']=='generated' else 'exposed edition150 camera','baseline':c['baseline'],'previous152':previous,'summary':e['summary'],'acceptedReturns':sum(x['accepted'] and x['correct'] for x in e['scores'] if x['ordinal']>=start),'totalMs':c['result']['totalMs'],'experimentId':e['id']})
  if s['group']=='generated':
   b=next(x for x in controls['cases'] if x['case']==c['name']);v=read(b['result']);trace=[{k:v for k,v in t.items() if k!='imageSha256'} for t in e['trace']];assert trace==v['SCALAR']['trace']==v['PRECOMPUTED']['trace'];assert [x['imageSha256'] for x in e['trace']]==v['imageHashes'][1:]
 for c in s['captures']:
  p=(r/path).parent/c['name'];assert sha(p.read_bytes())==c['sha256'] and c['privatePixelsExcluded'];name=s['group']+'-'+c['name'];shutil.copy2(p,out/name);shots.append({'path':'docs/readmes/153/'+name,'title':'2026-11-16 · '+s['group']+' / '+c['name']})
camera=rows[-1];s=camera['summary'];speed=qualification['medianSearchSpeedup']
finding=f"The explicit Java experiment confirms {camera['acceptedReturns']}/30 returned frames on the exposed camera recording, versus 0/30 with edition 152. It has {s['correct']}/{s['visible']} correct visible/partial observations, {s['missing']} withheld, {s['overlapFailures']} wrong overlaps, {s['unsupported']}/{s['hidden']} hidden outputs and {s['confirmedDistractorMatches']} matches to the labelled red object. Both new rendered return cases recover correct observations; wrong-only, changed-quadrant and identical-copy returns are refused. The paired search measurement is {speed:.2f}× faster with exactly identical scalar/optimized traces. Defaults remain unchanged; this is recorded evidence, not fresh physical validation."
table='| Source | Original control correct | Edition 152 correct | Edition 153 correct | Correct returned frames | Missing | Wrong / hidden / other outputs | Complete request seconds |\n| --- | ---: | ---: | ---: | ---: | ---: | --- | ---: |\n'
for row in rows:
 a=row['baseline'];b=row['previous152'];c=row['summary'];table+=f"| {row['case']} | {a['correct']}/{a['visible']} | {b['correct']}/{b['visible']} | {c['correct']}/{c['visible']} | {row['acceptedReturns']} | {c['missing']} | {c['overlapFailures']} / {c['unsupported']} of {c['hidden']} / {c['confirmedDistractorMatches']} | {row['totalMs']/1000:.2f} |\n"
cost=f"The full recorded search took **{measure['SCALAR']['ms']/1000:.2f} seconds** in scalar mode and **{measure['PRECOMPUTED']['ms']/1000:.2f} seconds** with precomputation. All 72 output rows are exactly equal. Three alternating paired short-sequence timings have medians of **{statistics.median(qualification['pairedSearchMs']['SCALAR'])/1000:.2f} seconds** and **{statistics.median(qualification['pairedSearchMs']['PRECOMPUTED'])/1000:.2f} seconds**, a **{speed:.2f}×** ratio. Every original image is rehashed after measurement. The actual camera app request took **{camera['totalMs']/1000:.2f} seconds** and cold restarted verification took **{studies[-1]['restartMs']/1000:.2f} seconds**. [Exact measurements](docs/readmes/153/qualification.json) retain all paired values."
cameraText=f"On the exposed recording, {camera['acceptedReturns']} of 30 returned frames are accepted correctly. The remaining {30-camera['acceptedReturns']} are withheld. The total is {s['correct']}/{s['visible']} correct visible or partial observations, compared with {old['summary']['correct']}/{old['summary']['visible']} for the separate edition152 experiment and {camera['baseline']['correct']}/{camera['baseline']['visible']} for the original149 run. No hidden frame or labelled red object is accepted. These results use the same original source images, run and labels; the complete trace also matches the independently executed scalar measurement. They establish improvement on this exposed recording, not general physical accuracy or fresh-camera performance."
attempts='The first eight-check development suite passed. The exposed fast development replay produced 23 correct returned observations; it was not a held-out result. The implementation and qualification gates were committed before rendering the five reserved sources. The final regression, arithmetic comparisons and actual-app evidence are retained separately. '
failed=[]
for p in sorted((r/'build').glob('study153-*/result.json')):
 v=json.loads(p.read_bytes())
 if not v.get('passed'):failed.append(p.relative_to(r).as_posix())
attempts+=('Incomplete or failed app attempts remain in the evidence index: '+', '.join(failed)+'.') if failed else 'No failed reserved app attempt is omitted; the recorded final attempts passed their execution checks.'
verification={'passed':True,'edition':153,'javaChecks':26,'desktopChecks':164,'appComparisons':len(rows),'sourceGeometryChecks':source,'qualification':qualification,'defaultsChanged':False,'promoted':False,'freshCameraTest':'not performed; user selected recorded tests only','scope':validation}
write(out/'verification.json',verification);write(out/'comparison.json',rows);write(out/'qualification.json',qualification)
originals=[];archive=paths+failed+['build/ci153-start.json','build/recorded153-development.json','build/recorded153-development-label-check.json','build/recorded153-measured.json','build/generated153-arithmetic.json','build/app-visual-review153.json','build/source-check153.json','build/qualification153.json','build/preflight153.log','build/regression153.log','build/desktop153.log','build/study095-build.json']+[x['result'] for x in controls['cases']]+[p.relative_to(r).as_posix() for p in xmls]+[p.relative_to(r).as_posix() for p in (r/'build/validation153/preflight').glob('*.xml')]
for name in dict.fromkeys(archive):
 raw=(r/name).read_bytes();check_public(json.loads(raw)) if name.endswith('.json') else None;packed=gzip.compress(raw,mtime=0);p=out/'originals'/(sha(raw)+'.gz');p.parent.mkdir(exist_ok=True);p.write_bytes(packed);originals.append({'logicalPath':name,'path':p.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(raw),'rawSha256':sha(raw)})
write(out/'original-evidence.json',{'files':originals,'privatePixelsPublished':False})
note='docs/obsidian/161-rotated-confirmation.md';modules=[{'id':key,'name':name,'source':file,'note':note} for key,name,file in [('search','Fresh pose and balanced refinement','workspace-engine/src/main/java/fraymus/workspace/RotationSearch153.java'),('confirm','Four-frame identity confirmation','workspace-engine/src/main/java/fraymus/workspace/RotationReturn153.java'),('proof','Original evidence and replay','workspace-engine/src/main/java/fraymus/workspace/RotationExperiment153.java'),('view','Actual controls and progress','workspace-desktop/renderer/association134-panel.js'),('arithmetic','Exact reference comparison','docs/publication153/RotationMeasure153.java')]]
system={'modules':modules,'screenshots':shots,'evidence':[{'path':'docs/readmes/153/'+n} for n in ['verification.json','comparison.json','qualification.json','original-evidence.json']],'comparison':rows,'finding':finding,'validation':validation};write(out/'system-map.json',system)
text(r/note,'# Rotated confirmation — edition153\n\n[[README-153.md]]\n\n'+finding+'\n\n'+validation+'\n\nNext: new physical returns and lookalikes, measured incumbent continuity, complete verification cost, then evidence-linked durable tasks.\n')
write(r/'docs/obsidian/161-rotated-confirmation.canvas',{'nodes':[{'id':m['id'],'type':'file','file':m['source'],'x':i*430,'y':0,'width':390,'height':200} for i,m in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']} for i in range(len(modules)-1)]})
md=(r/'docs/publication153/README-template.md').read_text(encoding='utf8')
for key,value in {'FINDING':finding,'HERO':'![Actual application rotated-return comparison]('+shots[0]['path']+')','TABLE':table,'CAMERA':cameraText,'COST':cost,'VALIDATION':validation,'ATTEMPTS':attempts}.items():md=md.replace('{{'+key+'}}',value)
assert '{{' not in md;text(r/'README-153.md',md);text(r/'README.md',md)
progress='## Edition 153 — locally verified; publication pending\n\n2026-11-16. Java target eyeoverthink/Fraynix-Agent-Workspace; isolated J:/Codex-Work/fraynix-workspace-edition153. No subagents. One heavy check at a time.\n\n'+finding+'\n\n'+validation+'\n\n'+attempts+'\n\nRead README-153.md and docs/publication153/plan153.md. Keep original world/browser/creation direction, every historical tracker identity, defaults and source/outcome checks. Original private sources preserved. User selected recorded tests only; no camera active. No automatic appearance admission or physical action authority. Hosted CI status is separate from local verification.\n\n'
for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
 p=r/name;old=p.read_text(encoding='utf-8-sig');assert not old.startswith('## Edition 153');text(p,progress+old)
for name in ['docs/INTEGRATED_AGENT_ROADMAP.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md']:
 p=r/name;text(p,'## Edition153 continuity — 2026-11-16\n\n'+finding+'\n\nRead [edition153](../README-153.md). Keep the unified Java agent/world/browser/creation direction. Next: new physical evidence, verified incumbent continuity and complete cost reduction.\n\n'+p.read_text(encoding='utf-8-sig'))
for name,prefix in [('docs/readmes/INDEX.md','[Edition153 — rotated confirmation and measured cost](../../README-153.md) | [Offline guide](153/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/161-rotated-confirmation.md]] | [[README-153.md]]')]:
 p=r/name;text(p,prefix+'\n\n'+p.read_text(encoding='utf-8-sig'))
inventory=[];names={x['path'] for x in read('docs/readmes/152/file-inventory.json')};names.update(lock['sourceHashes']);names.add('docs/OBSERVATION_153_LOCK.json');names.update(p.relative_to(r).as_posix() for p in (r/'docs/publication153').iterdir() if p.is_file())
for name in sorted(names):
 raw=(r/name).read_bytes()
 try:raw=raw.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':name,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
write(out/'file-inventory.json',inventory)
html=(r/'docs/publication153/guide-template.html').read_text(encoding='utf8')+'<script id="system-data" type="application/json">'+json.dumps(system).replace('</','<\\/')+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory).replace('</','<\\/')+'</script><script src="guide.js"></script></body></html>';text(out/'index.html',html);shutil.copy2(r/'docs/publication153/guide.js',out/'guide.js')
print(json.dumps({'passed':True,'sources':len(inventory),'evidence':len(originals),'screenshots':len(shots)}))
