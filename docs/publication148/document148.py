"""Build edition148 documentation only from completed checks and reviewed evidence."""
from pathlib import Path
import json,hashlib,subprocess,shutil,gzip,xml.etree.ElementTree as ET,re,datetime
r=Path.cwd();out=r/'docs/readmes/148';parent='c8a8c7efabd9a3a6ee2bb4e01180ffc736944edd'
read=lambda p:json.loads((r/p).read_bytes());sha=lambda b:hashlib.sha256(b).hexdigest()
def write(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes((json.dumps(v,indent=2,ensure_ascii=False)+'\n').encode())
def text(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(s.encode())
def seconds(v):return (datetime.datetime.fromisoformat(v['endedAt'].replace('Z','+00:00'))-datetime.datetime.fromisoformat(v['startedAt'].replace('Z','+00:00'))).total_seconds()
def no_pixels(v):
 if isinstance(v,dict):
  assert not ({'image','base64','imageData','dataUrl'}&set(v)), 'Do not publish private pixel payloads'
  for x in v.values():no_pixels(x)
 elif isinstance(v,list):
  for x in v:no_pixels(x)
 elif isinstance(v,str):assert 'data:image/' not in v
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-148']).returncode==1
sources=read('docs/publication148/source148.json');no_pixels(sources);lock=read('docs/OBSERVATION_148_LOCK.json');webcam=read('docs/publication148/webcam-lock148.json')
assert sha((r/'docs/OBSERVATION_148_LOCK.json').read_bytes())==sources['algorithmLockSha256']
assert sha((r/'docs/publication148/webcam-lock148.json').read_bytes())==sources['webcamLockSha256']
for frozen in [lock,webcam]:
 for name,h in frozen['sourceHashes'].items():assert sha((r/name).read_bytes().replace(b'\r\n',b'\n'))==h,name
studies={};reviews={};shots=[]
for group in sorted({x['group'] for x in sources['records']}):
 study=read(f'build/occlusion148-{group}-app/result.json');review=read(f'build/occlusion148-{group}-review/result.json')
 assert study['passed'] and study['export'] and len(study['runs'])<=24
 assert review['passed'] and review['privatePixelsExcluded'] and review['completedStateUnchanged'] and review['zeroObservationWrites']
 no_pixels(study);studies[group]=study;reviews[group]=review;write(out/(group+'-app-study.json'),study);write(out/(group+'-screenshot-review.json'),review)
 for item in review['captures']:
  dest=group+'-'+item['name'];shutil.copy2(r/f'build/occlusion148-{group}-review'/item['name'],out/dest);shots.append({'path':'docs/readmes/148/'+dest,'title':item['caption']})
assert studies and shots,'Actual completed studies and camera-free screenshots required'
provenance=read('docs/publication148/study-provenance148.json');assert provenance['passed'];no_pixels(provenance)
assert provenance['originalStudySha256']==sha((r/'build/occlusion148-black-lighter-movement-app/result.json').read_bytes())
write(out/'input-provenance.json',provenance)
shutil.copy2(r/'build/webcam148-controls.png',out/'recorder-controls.png');shots.append({'path':'docs/readmes/148/recorder-controls.png','title':'Actual edition 148 recording controls with camera off, captured during the synthetic-device lifecycle check. UI evidence only; no physical camera pixels or tracking result.'})
restart=read('build/occlusion148-restart/result.json');assert restart['passed'];assert restart['harnessSha256']==sha((r/'docs/publication148/restart148.cjs').read_bytes());no_pixels(restart);write(out/'restart-check.json',restart)
checks=set()
for p in (r/'build/validation148/final').glob('*.xml'):
 e=ET.parse(p).getroot();assert int(e.get('failures','0'))+int(e.get('errors','0'))==0
 for c in e.findall('testcase'):
  if c.find('skipped') is None:checks.add(c.get('classname')+'.'+c.get('name'))
assert checks and any('ReturnConfirmation148Test' in x for x in checks) and any('CalibrationStoreTest.validationCannotRetuneWinnerAndFailedGateCannotApply' in x for x in checks)
prior_checks=set(read('docs/readmes/147/verification.json')['javaChecks'])
new_checks={x for x in checks if x.startswith('fraymus.workspace.ReturnConfirmation148Test.')}
assert prior_checks<=checks,sorted(prior_checks-checks)
assert len(new_checks)==8 and checks==prior_checks|new_checks,'Require every predecessor regression and all eight new checks'
for name in ['kernel148-final','java148-final','desktop148-final','agency148-regression','launcher148','package148','receipt148']:assert read('build/'+name+'.json')['exit']==0,name
desktop=(r/'build/desktop148-final.log').read_text(encoding='utf8');assert 'fail 0' in desktop;desktop_count=int(re.search(r'(?m)^. tests (\d+)',desktop)[1])
allruns=[x for s in studies.values() for x in s['runs']];rows=[{'case':x['caseName'],'group':group,'domain':x['domain'],'policy':x['policy'],'summary':x['audit']['summary'],'evidence':x['audit'].get('confirmation148') or x['audit'].get('continuity146') or x['audit'].get('trajectory145'),'trackingMs':x['trackingMs'],'auditMs':x['auditMs']} for group,s in studies.items() for x in s['runs'] if x['role']=='evaluation']
refusals=[x for s in studies.values() for x in s.get('refusals',[])]
finding=(r/'build/findings148.md').read_text(encoding='utf8').strip();next_steps=(r/'build/next148.md').read_text(encoding='utf8').strip();attempts=(r/'build/attempts148.md').read_text(encoding='utf8').strip()
comparison={'rows':rows,'sources':sources,'lock':lock,'webcamLock':webcam,'finding':finding,'defaultsUnchanged':True,'promoted':False,'refusals':refusals};write(out/'comparison.json',comparison)
validation=f'{len(checks)} distinct Java checks and {desktop_count} desktop checks passed, plus kernel, agency, offline preparation and package verification. {len(allruns)} actual app runs across {len(studies)} private study project(s), with separate unfinished-process recovery. Source eligibility and useful recovery remain distinct from execution checks. Hosted CI is reported separately.'
appchecks=list(dict.fromkeys([c for s in studies.values() for c in s['checks']]+restart['checks']))
verification={'passed':True,'javaChecks':sorted(checks),'desktopChecks':desktop_count,'actualAppChecks':appchecks,'candidateLock':lock,'webcamLock':webcam,'defaultsUnchanged':True,'scope':validation,'inputProvenance':provenance,'studies':{g:{'source':s['source'],'sourceHeaderScope':'Initial failed attempt; effective committed inputs are bound separately in inputProvenance','effectiveInputRevision':provenance['effectiveInputRevision'],'buildReceipt':s['buildReceipt'],'path':f'docs/readmes/148/{g}-app-study.json'} for g,s in studies.items()}}
write(out/'verification.json',verification)
originals=[]
def preserve(p,logical):
 raw=p.read_bytes();digest=sha(raw);packed=gzip.compress(raw,mtime=0);dest=out/'originals'/(digest+'.gz');dest.parent.mkdir(exist_ok=True)
 if not dest.exists():dest.write_bytes(packed)
 originals.append({'logicalPath':logical,'path':dest.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(raw),'rawSha256':digest})
# Explicit allowlist: never recurse into source images, private project stores or app screenshot folders.
allowed=[r/f'build/occlusion148-{g}-app/result.json' for g in studies]+[r/f'build/occlusion148-{g}-review/result.json' for g in studies]
allowed += [p for g in studies for p in (r/f'build/occlusion148-{g}-review').glob('result-attempt-*.json')]
allowed += [p for g in studies for p in (r/f'build/occlusion148-{g}-app').glob('result-attempt-*.json')]
allowed += [r/'build/occlusion148-restart/result.json',r/'docs/OBSERVATION_148_LOCK.json',r/'docs/publication148/webcam-lock148.json',r/'docs/publication148/restart148.cjs',r/'workspace-desktop/test/occlusion148-integration.cjs',r/'workspace-desktop/test/webcam148-recorder.cjs']
allowed += list((r/'build/validation148').rglob('*.xml'))
for pattern in ['*148*.json','*148*.log','*148*.md']:
 allowed += [p for p in (r/'build').glob(pattern) if p.is_file()]
for p in sorted(set(allowed)):
 if p.suffix=='.json':no_pixels(json.loads(p.read_bytes()))
 preserve(p,p.relative_to(r).as_posix())
write(out/'original-evidence.json',{'files':originals,'scope':'Allowlisted original test and app receipts. Private source images, project stores, device details and pixel-containing screenshots excluded. Exact source hashes/labels remain available.'})
paths={x['path'] for x in read('docs/readmes/147/file-inventory.json')};paths.update(f.relative_to(r).as_posix() for f in (r/'docs/publication148').iterdir() if f.is_file() and f.suffix not in ['.log'])
paths.update(['workspace-desktop/test/webcam148-recorder.cjs','workspace-desktop/test/occlusion148-integration.cjs','docs/publication148/PROTOCOL_148.md','docs/OBSERVATION_148_LOCK.json']);inventory=[]
paths.update(p.relative_to(r).as_posix() for p in (r/'workspace-engine/src/main/java/fraymus/workspace').glob('*148.java'))
paths.update(p.relative_to(r).as_posix() for p in (r/'workspace-engine/src/test/java/fraymus/workspace').glob('*148*.java'))
for name in sorted(paths):
 raw=(r/name).read_bytes()
 try:raw=raw.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':name,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
write(out/'file-inventory.json',inventory)
table='| Source / method | Correct / visible | Missing | Wrong overlap | Hidden outputs | Other-object matches |\n| --- | ---: | ---: | ---: | ---: | ---: |\n';recovery='| Source / method | Additional restored correct / incorrect | Accepted supplemental correct / incorrect |\n| --- | ---: | ---: |\n';timings='| Source / method | Tracking ms | Replay/audit ms |\n| --- | ---: | ---: |\n'
for row in rows:
 s=row['summary'];d=row['evidence'] or {};new=d.get('additional',dict(correct=0,incorrect=0));f=d.get('filtered',dict(correct=0,incorrect=0))
 table+=f"| {row['case']} / {row['policy']} | {s['correct']}/{s['visible']} | {s['missing']} | {s['overlapFailures']} | {s['unsupported']}/{s['hidden']} | {s['confirmedDistractorMatches']} |\n"
 recovery+=f"| {row['case']} / {row['policy']} | {new['correct']} / {new['incorrect']} | {f['correct']} / {f['incorrect']} |\n";timings+=f"| {row['case']} / {row['policy']} | {row['trackingMs']:.1f} | {row['auditMs']:.1f} |\n"
for g,s in studies.items():timings+=f"\nComplete {g} comparison workflow: {seconds(s):.1f}s, {len(s['runs'])} runs, {s['export']['bytes']} exported bytes. Includes checks, UI, source replay and retained retry intervals.\n"
timings+=f'\nSeparate interrupted-process check: {seconds(restart):.1f}s. Camera-free screenshot review: {sum(seconds(s) for s in reviews.values()):.1f}s.\n'
source_table='| Source / role | Original samples | Visible / partial | Fully covered |\n| --- | --- | ---: | ---: |\n'
for row in sources['records']:
 c=row['visibilityCounts'];source_table+=f"| {row['case']} / {row['role']} | {row['start']}–{row['end']} | {c['visible']} / {c['partial']} | {c['occluded']} |\n"
md=(r/'docs/publication148/README-template.md').read_text(encoding='utf8')
for key,value in {'FINDING':finding,'HERO':'![Actual saved comparison; private camera pixels excluded]('+shots[0]['path']+')','RESULTS':table,'RECOVERIES':recovery,'SOURCE_REVIEW':source_table,'VALIDATION':validation,'APP_CHECKS':'\n'.join('- '+x for x in appchecks),'COSTS':timings,'ATTEMPTS':attempts,'NEXT':next_steps}.items():md=md.replace('{{'+key+'}}',value)
assert '{{' not in md;text(r/'README-148.md',md);text(r/'README.md',md)
note='docs/obsidian/156-return-confirmation.md';modules=[{'id':i,'name':n,'source':s,'note':note} for i,n,s in [('capture','Private source recording','workspace-desktop/test/webcam148-recorder.cjs'),('preflight','Source-only qualification','docs/publication148/preflight148.py'),('guard','Motion-supported confirmation','workspace-engine/src/main/java/fraymus/workspace/ReturnConfirmation148.java'),('reference','Verified source appearances','workspace-engine/src/main/java/fraymus/workspace/ReferenceMemory137.java'),('audit','Independent original-pixel replay','workspace-engine/src/main/java/fraymus/workspace/ReturnAudit148.java'),('world','Durable observations','workspace-engine/src/main/java/fraymus/workspace/WorldObservationStore.java')]]
system={'modules':modules,'screenshots':shots,'evidence':[{'path':'docs/readmes/148/'+n} for n in ['verification.json','comparison.json','original-evidence.json','restart-check.json']],'comparison':comparison,'validation':validation,'finding':finding};write(out/'system-map.json',system)
text(r/note,'# Motion-supported return confirmation — edition148\n\n[[README-148.md]]\n\n'+finding+'\n\n'+validation+'\n\n'+next_steps+'\n\nContinue [[docs/INTEGRATED_AGENT_ROADMAP.md]].\n')
write(r/'docs/obsidian/156-return-confirmation.canvas',{'nodes':[{'id':x['id'],'type':'file','file':x['source'],'x':i*430,'y':0,'width':390,'height':190} for i,x in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']} for i in range(len(modules)-1)]})
html=(r/'docs/publication148/guide-template.html').read_text(encoding='utf8')+'<script id="system-data" type="application/json">'+json.dumps(system).replace('</','<\\/')+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory).replace('</','<\\/')+'</script><script src="guide.js"></script></body></html>';text(out/'index.html',html);shutil.copy2(r/'docs/publication148/guide.js',out/'guide.js')
for name,link in [('docs/readmes/INDEX.md','[Edition148 — motion-supported return confirmation](../../README-148.md) | [Offline guide](148/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/156-return-confirmation.md]] | [[README-148.md]]')]:
 p=r/name;s=p.read_text(encoding='utf-8-sig');text(p,link+'\n\n'+s if not s.startswith(link) else s)
print(json.dumps({'passed':True,'javaChecks':len(checks),'sourceFiles':len(inventory),'originalEntries':len(originals),'privateImagesExcluded':True}))
