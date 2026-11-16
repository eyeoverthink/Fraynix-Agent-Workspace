from pathlib import Path
import json,hashlib,gzip,shutil,xml.etree.ElementTree as ET,subprocess,re,statistics,sys
r=Path.cwd();out=r/'docs/readmes/151';out.mkdir(parents=True,exist_ok=True)
sys.path.insert(0,str(r/'docs/publication149'));from public_evidence149 import check_public
sha=lambda b:hashlib.sha256(b).hexdigest()
def read(p):return json.loads((r/p).read_bytes())
def write(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes((json.dumps(v,indent=2,ensure_ascii=False)+'\n').encode())
def text(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(s.encode())
assert subprocess.run(['git','show-ref','--quiet','--verify','refs/tags/readme-151']).returncode==1
paths=['build/study151-webcam150-displaced-return-final/result.json','build/study151-generated150/result.json']
studies=[read(p) for p in paths];assert all(s['passed'] for s in studies)
lock=read('docs/OBSERVATION_151_LOCK.json')
for p,h in lock['sourceHashes'].items():assert sha((r/p).read_bytes().replace(b'\r\n',b'\n'))==h,p
for s in studies:assert s['methodLockSha256']==sha((r/'docs/OBSERVATION_151_LOCK.json').read_bytes());check_public(s)
camera=studies[0]['cases'][0];d=camera['diagnosis'];rows=[x for x in d['rows'] if x['afterOcclusion']]
warm=[x['totalMs'] for x in camera['timings'] if x['kind']=='warm'];cold=next(x['totalMs'] for x in camera['timings'] if x['kind']=='cold')
findings=f"The shifted/rotated camera return remains unresolved: **{camera['summary']['correct']}/{camera['summary']['visible']} correct observations, {camera['summary']['missing']} missing, {camera['summary']['unsupported']}/{camera['summary']['hidden']} hidden outputs and {camera['summary']['confirmedDistractorMatches']} matches to the labelled red lighter**, exactly as in edition150. Read-only probes explain why; they do not add recovered observations. Reopening the unchanged verified comparison took **{statistics.median(warm)/1000:.3f} seconds median**, versus **{cold/1000:.3f} seconds** for its cold full replay. Defaults and tracking thresholds remain unchanged."
def span(key,metric):return f"{min(x[key][metric] for x in rows):.3f}–{max(x[key][metric] for x in rows):.3f}"
diag=f"Across all **{len(rows)} fully visible returned samples**, the original search overlapped the target in {d['summary']['searchOverlapsTarget']} and retained a competing-object veto in {d['summary']['returnsWithAmbiguityVeto']}. At the labelled axis-aligned crop, correlation was **{span('axisAlignedTarget','score')}** and gradient agreement **{span('axisAlignedTarget','gradient')}**. The best label-guided probes used {sorted({x['bestLabelGuidedProbe']['degrees'] for x in rows})} degrees, raised correlation to **{span('bestLabelGuidedProbe','score')}**, and raised gradient agreement to **{span('bestLabelGuidedProbe','gradient')}**. Color and quadrant errors improved, but the correlation and gradient thresholds still failed. **{d['summary']['strongWithLabelGuidedSweep']}/{len(rows)}** best probes passed all unchanged appearance checks.\n\nThis separates two problems: an axis-aligned search failed to propose the rotated target, and simply supplying a rotated crop still did not meet the original full-object evidence requirements. Lowering thresholds on this known sequence would not be evidence of general recovery. The first original hypothesis was near the red object but was rejected; an unaccepted hypothesis is not a measured identity switch."
table='| Source / read kind | Java opening seconds | Bytes rehashed | Reused? |\n| --- | ---: | ---: | --- |\n'
for s in studies:
 for case in s['cases']:
  for t in case['timings']:table+=f"| {case['case']} / {t['kind']} | {t['totalMs']/1000:.3f} | {t['bytesHashed']:,} | {t['reusedReplay']} |\n"
table+='\nThree warm repetitions per case are reported individually. Explicit replay and restart timings include full verification. These timings are local measurements, not a hardware-independent guarantee.\n'
checks=set();xmls=list((r/'build/validation151/regression').glob('*.xml'))+list((r/'build/validation151/final-focus').glob('*.xml'))
for p in xmls:
 e=ET.parse(p).getroot();assert sum(int(e.get(x,'0')) for x in ['failures','errors','skipped'])==0
 for c in e.findall('testcase'):checks.add(c.get('classname')+'.'+c.get('name'))
desktop=(r/'build/desktop151.log').read_text(encoding='utf8');assert 'fail 0' in desktop and 'tests 164' in desktop
assert len(checks)==29,len(checks)
validation=f"**{len(checks)} distinct selected Java checks and 164 desktop checks passed**, with no failures or skipped cases. The final nine focused Java checks include the missing-history refusal and unchanged diagnostic scoring. The broader Java regression passed before that narrow read-only guard was added. Two actual app studies cover one exposed camera case and two exposed generated controls. The installed build receipt matches current compiled inputs. This is a scoped regression, not a rerun of every historical system capability."
attempts="The first camera study completed its cold/warm verification and diagnostic calculation, then its screenshot helper was blocked by the existing content-security policy. The failed receipt remains in the evidence index. The helper was changed to hide existing image elements through their DOM styles, as prior captures do; the content policy was not relaxed. An additional review found that a missing observation state should be refused before calling the older store, which otherwise initializes absent history. A focused test now proves this path creates no replacement history. A second attempt completed forced replay, corruption refusal and restart, but its final filesystem comparison met an owned lock while the app was running. The helper now closes its owned app before comparing every file, including locks. That attempt remains a failure receipt; final camera and generated studies use the corrected, frozen method; no tracker threshold was adjusted after inspecting the diagnostic result."
shots=[]
for p,s in zip(paths,studies):
 write(out/(s['group']+'-study.json'),s)
 for c in s['captures']:
  source=(r/p).parent/c['name'];assert sha(source.read_bytes())==c['sha256'];assert c['privatePixelsExcluded'];dest=s['group']+'-'+c['name'];shutil.copy2(source,out/dest);shots.append({'path':'docs/readmes/151/'+dest,'title':s['domain']+' / '+('Return diagnosis' if 'diagnosis' in c['name'] else 'Verified replay cost')})
visual=read('build/app-visual-review151.json');assert visual['passed']
assert {x['sha256'] for x in visual['images']}=={x['sha256'] for s in studies for x in s['captures']}
write(out/'app-visual-review.json',visual)
comparison={'rows':[{'case':c['case'],'domain':s['domain'],'summary':c['summary'],'diagnosis':c['diagnosis']['summary'],'timings':c['timings']} for s in studies for c in s['cases']],'promoted':False,'trackerDefaultsChanged':False,'scope':'Unchanged historical outcomes; exposed retrospective probes and repeat-opening costs only'};write(out/'comparison.json',comparison)
verification={'passed':True,'javaChecks':sorted(checks),'desktopChecks':164,'scope':validation,'methodLock':lock,'studies':paths,'checks':[x for s in studies for x in s['checks']],'attempts':attempts};write(out/'verification.json',verification)
originals=[]
evidence=paths+['build/study151-webcam150-displaced-return/result.json','build/study151-webcam150-displaced-return-retry/result.json','build/preflight151.log','build/regression151.log','build/final-focus151.log','build/desktop151.log','build/study151-camera.log','build/study151-camera-retry.log','build/study151-camera-final.log','build/study151-generated.log','build/identity151-source-check.json','build/study095-build.json','build/ci151-start.json','docs/publication151/study151.cjs']+[p.relative_to(r).as_posix() for p in xmls]
for name in evidence:
 p=r/name;raw=p.read_bytes()
 if p.suffix=='.json':check_public(json.loads(raw))
 packed=gzip.compress(raw,mtime=0);file=out/'originals'/(sha(raw)+'.gz');file.parent.mkdir(exist_ok=True);file.write_bytes(packed)
 originals.append({'logicalPath':name,'path':file.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(raw),'rawSha256':sha(raw)})
write(out/'original-evidence.json',{'files':originals,'scope':'Original151 receipts, including the failed capture attempt and exact scoped tests. No source image bytes or reconstructible RGB descriptors. Historical150 private projects remain local and unchanged.'})
note='docs/obsidian/159-return-diagnosis.md'
modules=[{'id':i,'name':n,'source':s,'note':note} for i,n,s in [('memo','Verified replay reuse','workspace-engine/src/main/java/fraymus/workspace/ReplayEvidence151.java'),('diagnosis','Labelled return diagnosis','workspace-engine/src/main/java/fraymus/workspace/ReturnDiagnosis151.java'),('original','Unchanged original verifier','workspace-engine/src/main/java/fraymus/workspace/AssociationAudit134.java'),('view','Visible cost and diagnostic controls','workspace-desktop/renderer/association134-panel.js')]]
system={'modules':modules,'screenshots':shots,'evidence':[{'path':'docs/readmes/151/'+n} for n in ['verification.json','comparison.json','original-evidence.json']],'comparison':comparison,'validation':validation,'finding':findings};write(out/'system-map.json',system)
text(r/note,'# Return diagnosis and verified replay — edition151\n\n[[README-151.md]]\n\n'+findings+'\n\n'+diag+'\n\n'+validation+'\n\nNext: label-free orientation-aware proposals, unchanged identity safeguards and fresh evaluation; see [[docs/INTEGRATED_AGENT_ROADMAP.md]].\n')
write(r/'docs/obsidian/159-return-diagnosis.canvas',{'nodes':[{'id':m['id'],'type':'file','file':m['source'],'x':i*430,'y':0,'width':390,'height':190} for i,m in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']} for i in range(len(modules)-1)]})
md=(r/'docs/publication151/README-template.md').read_text(encoding='utf8')
hero=next(s['path'] for s in shots if 'webcam' in s['path'] and 'diagnosis' in s['path'])
for k,v in {'FINDINGS':findings,'DIAGNOSIS':diag,'COSTS':table,'VALIDATION':validation,'ATTEMPTS':attempts,'HERO':'![Actual return diagnosis; original camera pixels remain private]('+hero+')'}.items():md=md.replace('{{'+k+'}}',v)
assert '{{' not in md;text(r/'README-151.md',md);text(r/'README.md',md)
progress='## Edition 151 — locally verified; publication pending\n\n2026-11-16, America/Los_Angeles. Isolated J:/Codex-Work/fraynix-workspace-edition151 from affcf65a. Active publication target remains eyeoverthink/Fraynix-Agent-Workspace, separate from Chandra. No subagents; one heavy check at a time.\n\n'+findings+'\n\n'+validation+'\n\n'+attempts+'\n\nRead README-151.md and docs/publication151/plan151.md before changing trajectory. Private original150 projects preserved, camera off. No tracker promotion, relaxed identity thresholds, automatic appearance admission or physical action authority. Next: orientation-aware proposals without labels, independent new return/lookalike evaluation, then growing-bank and live-cost tests.\n\n'
for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
 p=r/name;old=p.read_text(encoding='utf-8-sig');assert not old.startswith('## Edition 151');text(p,progress+old)
for name in ['docs/INTEGRATED_AGENT_ROADMAP.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md']:
 p=r/name;text(p,'## Edition151 continuity — 2026-11-16\n\n'+findings+'\n\nThe integrated Java world/agent/creation direction is unchanged. Read [edition151](../README-151.md) for diagnostics and bounded replay reuse; no tracking promotion. Next proposals must work without labels and pass fresh identity tests.\n\n'+p.read_text(encoding='utf-8-sig'))
for name,prefix in [('docs/readmes/INDEX.md','[Edition151 — return diagnosis and verified replay](../../README-151.md) | [Offline guide](151/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/159-return-diagnosis.md]] | [[README-151.md]]')]:
 p=r/name;text(p,prefix+'\n\n'+p.read_text(encoding='utf-8-sig'))
inventory=[];paths={x['path'] for x in read('docs/readmes/150/file-inventory.json')};paths.update(lock['sourceHashes']);paths.add('docs/OBSERVATION_151_LOCK.json');paths.update(p.relative_to(r).as_posix() for p in (r/'docs/publication151').iterdir() if p.is_file())
for name in sorted(paths):
 raw=(r/name).read_bytes()
 try:raw=raw.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':name,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
write(out/'file-inventory.json',inventory)
html=(r/'docs/publication151/guide-template.html').read_text(encoding='utf8')+'<script id="system-data" type="application/json">'+json.dumps(system).replace('</','<\\/')+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory).replace('</','<\\/')+'</script><script src="guide.js"></script></body></html>'
text(out/'index.html',html);shutil.copy2(r/'docs/publication151/guide.js',out/'guide.js')
print(json.dumps({'passed':True,'javaChecks':len(checks),'sources':len(inventory),'evidenceFiles':len(originals),'screenshots':len(shots)}))
