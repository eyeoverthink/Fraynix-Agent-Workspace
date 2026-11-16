from pathlib import Path
import json,hashlib,gzip,shutil,xml.etree.ElementTree as ET,subprocess,sys
r=Path.cwd();out=r/'docs/readmes/152';out.mkdir(parents=True,exist_ok=True);sha=lambda b:hashlib.sha256(b).hexdigest()
sys.path.insert(0,str(r/'docs/publication149'));from public_evidence149 import check_public
def read(name):return json.loads((r/name).read_bytes())
def write(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes((json.dumps(v,indent=2,ensure_ascii=False)+'\n').encode())
def text(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(s.encode())
assert subprocess.run(['git','show-ref','--quiet','--verify','refs/tags/readme-152']).returncode==1
paths=['build/study152-generated-final/result.json','build/study152-exposed150-final/result.json'];studies=[read(p) for p in paths];assert all(s['passed'] for s in studies)
lock=read('docs/OBSERVATION_152_LOCK.json')
for p,h in lock['sourceHashes'].items():assert sha((r/p).read_bytes().replace(b'\r\n',b'\n'))==h,p
for s in studies:assert s['methodLockSha256']==sha((r/'docs/OBSERVATION_152_LOCK.json').read_bytes());check_public(s)
review=read('build/app-visual-review152.json');assert review['passed'];assert {x['sha256'] for x in review['images']}=={x['sha256'] for s in studies for x in s['captures']}
checks=set();xmls=list((r/'build/validation152/final').glob('*.xml'))
for p in xmls:
 e=ET.parse(p).getroot();assert sum(int(e.get(x,'0')) for x in ['failures','errors','skipped'])==0
 for c in e.findall('testcase'):checks.add(c.get('classname')+'.'+c.get('name'))
desktop=(r/'build/desktop152.log').read_text(encoding='utf8');assert 'fail 0' in desktop and 'tests 164' in desktop
assert len(checks)==18,len(checks)
source=read('build/source-check152.json');assert source['passed']
validation=f'{len(checks)} selected Java checks and 164 desktop checks passed, with no failures or skipped cases. Four generated app comparisons and one exposed-camera comparison passed execution, restart and source-integrity checks. Independent source geometry checks passed. These are scoped checks, not a rerun of every historical capability. Fresh webcam capture remains pending.'
rows=[];shots=[]
for name,s in zip(paths,studies):
 write(out/(s['group']+'-study.json'),s)
 for c in s['cases']:rows.append({'case':c['name'],'domain':'new generated software control' if s['group']=='generated' else 'exposed edition150 camera','baseline':c['baseline'],'summary':c['result']['experiment']['summary'],'totalMs':c['result']['totalMs'],'experimentId':c['result']['experiment']['id']})
 for c in s['captures']:
  p=(r/name).parent/c['name'];assert sha(p.read_bytes())==c['sha256'] and c['privatePixelsExcluded'];dest=s['group']+'-'+c['name'];shutil.copy2(p,out/dest);shots.append({'path':'docs/readmes/152/'+dest,'title':'2026-11-16 · '+s['group']+' / '+c['name']})
camera=rows[-1];s=camera['summary'];coverage=read('build/camera-search152.json');finding=f"The explicit Java search handles rotated proposals without supplied return locations. On the two new generated returns it accepts 7/13 visible samples versus 4/13 for the simple default, with no wrong or hidden outputs. It refuses the identical-copy return. On the exposed camera recording it accepts {s['correct']}/{s['visible']} visible/partial samples, with {s['missing']} withheld, {s['unsupported']}/{s['hidden']} hidden outputs and {s['confirmedDistractorMatches']} matches to the labelled red object. Search proposals overlap all 30 returned camera frames, but none earns full confirmation. Defaults remain unchanged; fresh physical validation is pending."
table='| Source | Baseline correct | Rotation correct | Missing | Wrong overlap | Hidden outputs | Other-object matches | Complete request seconds |\n| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |\n'
for row in rows:
 a=row['baseline'];b=row['summary'];table+=f"| {row['case']} | {a['correct']}/{a['visible']} | {b['correct']}/{b['visible']} | {b['missing']} | {b['overlapFailures']} | {b['unsupported']}/{b['hidden']} | {b['confirmedDistractorMatches']} | {row['totalMs']/1000:.2f} |\n"
cameraText=f"The existing edition150 recording is **exposed diagnostic evidence**. Its original 149 comparison remains {camera['baseline']['correct']}/{camera['baseline']['visible']}. The new independent experiment reaches {s['correct']}/{s['visible']}; it does not replace the original trace. The complete request took {camera['totalMs']/1000:.2f} seconds, and cold restarted verification took {studies[-1]['restartMs']/1000:.2f} seconds. A separate retrospective overlap check finds proposals at the target in all 30 returned frames (the original149 search overlapped none). Eleven returned frames contain a strong full-object match. The longest same-reference confirmation is three, short of the required four; no ambiguity veto was retained on this interval. Thus the location-proposal gap improves on this exposed sequence, while useful physical recovery remains unresolved. No calibrated physical pose or general identity guarantee follows from this one recording."
fresh='A new webcam test was offered for the fixed mat, black target and red or second black lighter. Setup availability was not confirmed during this implementation, and no new capture was started. No old recording is relabelled as fresh.'
attempts='The first development grid found rotated targets but missed a competing identical copy. Two safety tests failed; their original XML and debug receipts are retained. A denser proposal grid and joint translation refinement corrected that failure before the first new generated evaluation. No acceptance threshold was relaxed. The first app harness failed because a collapsed panel was not opened. A camera-study harness then tried to read an owned lock while the app was open; it now releases the app before comparing every file. The next camera attempt exceeded the browser request timeout before completion. A single background worker and visible progress polling now handle that operation; incomplete work is not reported as verified. The search mathematics stayed fixed through these UI/job changes. The final generated replay repeats exposed controls and is identified as regression; the separately retained initial prospective result has the same measured outcomes.'
initial=read('build/study152-generated-ui-corrected/result.json');assert initial['passed']
for before,after in zip(initial['cases'],studies[0]['cases']):assert before['result']['experiment']['summary']==after['result']['experiment']['summary'];assert before['result']['experiment']['trace']==after['result']['experiment']['trace']
verification={'passed':True,'edition':152,'javaChecks':len(checks),'desktopChecks':164,'appComparisons':len(rows),'sourceGeometryChecks':source,'defaultsChanged':False,'promoted':False,'freshCameraTest':'pending setup confirmation','initialAndFinalGeneratedTracesEqual':True,'scope':validation}
write(out/'verification.json',verification);write(out/'comparison.json',rows);write(out/'camera-search.json',coverage)
originals=[];pathsToArchive=paths+['build/study152-generated/result.json','build/study152-generated-ui-corrected/result.json','build/study152-exposed150/result.json','build/study152-exposed150-locks-corrected/result.json','build/ci152-start.json','build/generated-equivalence152.json','build/generated-visual-review152.json','build/app-visual-review152.json','build/initial-method152-lock.json','build/ci151-replay.xml','build/source-check152.json','build/camera-search152.json','build/preflight152.log','build/copy-failure152.xml','build/rotation152-debug.xml','build/regression152-final.log','build/desktop152.log','build/study095-build.json']+[p.relative_to(r).as_posix() for p in xmls]+[p.relative_to(r).as_posix() for p in (r/'build/validation152/preflight').glob('*.xml')]
for name in pathsToArchive:
 raw=(r/name).read_bytes();check_public(json.loads(raw)) if name.endswith('.json') else None
 packed=gzip.compress(raw,mtime=0);p=out/'originals'/(sha(raw)+'.gz');p.parent.mkdir(exist_ok=True);p.write_bytes(packed)
 originals.append({'logicalPath':name,'path':p.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(raw),'rawSha256':sha(raw)})
write(out/'original-evidence.json',{'files':originals,'privatePixelsPublished':False})
note='docs/obsidian/160-rotation-search.md';modules=[{'id':key,'name':title,'source':file,'note':note} for key,title,file in [('search','Label-free pose proposals','workspace-engine/src/main/java/fraymus/workspace/RotationSearch152.java'),('confirm','Same-reference confirmation','workspace-engine/src/main/java/fraymus/workspace/RotationReturn152.java'),('proof','Durable experimental evidence','workspace-engine/src/main/java/fraymus/workspace/RotationExperiment152.java'),('view','Visible search and progress','workspace-desktop/renderer/association134-panel.js')]]
system={'modules':modules,'screenshots':shots,'evidence':[{'path':'docs/readmes/152/'+n} for n in ['verification.json','comparison.json','original-evidence.json']],'comparison':rows,'finding':finding,'validation':validation};write(out/'system-map.json',system)
text(r/note,'# Rotation search — edition152\n\n[[README-152.md]]\n\n'+finding+'\n\n'+validation+'\n\nOriginal runtime direction preserved. Next: fresh physical identity evidence, measured coverage/cost, then a verified continuity combination.\n')
write(r/'docs/obsidian/160-rotation-search.canvas',{'nodes':[{'id':m['id'],'type':'file','file':m['source'],'x':i*430,'y':0,'width':390,'height':200} for i,m in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']} for i in range(len(modules)-1)]})
md=(r/'docs/publication152/README-template.md').read_text(encoding='utf8')
for k,v in {'FINDING':finding,'HERO':'![Actual generated return comparison in the application]('+shots[0]['path']+')','TABLE':table,'CAMERA':cameraText,'FRESH':fresh,'VALIDATION':validation,'ATTEMPTS':attempts}.items():md=md.replace('{{'+k+'}}',v)
assert '{{' not in md;text(r/'README-152.md',md);text(r/'README.md',md)
progress='## Edition 152 — locally verified; publication pending\n\n2026-11-16. Java target eyeoverthink/Fraynix-Agent-Workspace; isolated J:/Codex-Work/fraynix-workspace-edition152. No subagents. One heavy check at a time.\n\n'+finding+'\n\n'+validation+'\n\n'+attempts+'\n\nRead README-152.md and docs/publication152/plan152.md. Private worlds and all camera sources preserved. No active camera. Fresh camera validation pending current setup confirmation. No altered historical tracker identities, threshold relaxation, automatic appearance admission or physical action authority. Original151 CI failures corrected locally; hosted status remains separate.\n\n'
for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
 p=r/name;old=p.read_text(encoding='utf-8-sig');assert not old.startswith('## Edition 152');text(p,progress+old)
for name in ['docs/INTEGRATED_AGENT_ROADMAP.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md']:
 p=r/name;text(p,'## Edition152 continuity — 2026-11-16\n\n'+finding+'\n\nRead [edition152](../README-152.md). Keep the unified Java agent/world/browser/creation direction. Next: new physical returns and stronger lookalikes, measured search cost, then verified incumbent continuity.\n\n'+p.read_text(encoding='utf-8-sig'))
for name,prefix in [('docs/readmes/INDEX.md','[Edition152 — rotation search without return labels](../../README-152.md) | [Offline guide](152/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/160-rotation-search.md]] | [[README-152.md]]')]:
 p=r/name;text(p,prefix+'\n\n'+p.read_text(encoding='utf-8-sig'))
inventory=[];names={x['path'] for x in read('docs/readmes/151/file-inventory.json')};names.update(lock['sourceHashes']);names.add('docs/OBSERVATION_152_LOCK.json');names.update(p.relative_to(r).as_posix() for p in (r/'docs/publication152').iterdir() if p.is_file())
for name in sorted(names):
 raw=(r/name).read_bytes()
 try:raw=raw.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':name,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
write(out/'file-inventory.json',inventory)
html=(r/'docs/publication152/guide-template.html').read_text(encoding='utf8')+'<script id="system-data" type="application/json">'+json.dumps(system).replace('</','<\\/')+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory).replace('</','<\\/')+'</script><script src="guide.js"></script></body></html>'
text(out/'index.html',html);shutil.copy2(r/'docs/publication152/guide.js',out/'guide.js')
print(json.dumps({'passed':True,'javaChecks':len(checks),'sources':len(inventory),'evidenceFiles':len(originals),'screenshots':len(shots)}))
