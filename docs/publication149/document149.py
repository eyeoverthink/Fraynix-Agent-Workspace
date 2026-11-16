"""Build edition149 documentation only from completed checks and reviewed evidence."""
from pathlib import Path
import json,hashlib,subprocess,shutil,gzip,xml.etree.ElementTree as ET,re,datetime
from public_evidence149 import public_view,public_bytes,check_public
r=Path.cwd();out=r/'docs/readmes/149';parent='d3012d4bf02ce3c2396439099e9011d391ff8780'
read=lambda p:json.loads((r/p).read_bytes());sha=lambda b:hashlib.sha256(b).hexdigest()
def write(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes((json.dumps(v,indent=2,ensure_ascii=False)+'\n').encode())
def text(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(s.encode())
def seconds(v):return (datetime.datetime.fromisoformat(v['endedAt'].replace('Z','+00:00'))-datetime.datetime.fromisoformat(v['startedAt'].replace('Z','+00:00'))).total_seconds()
def no_pixels(v):check_public(v)
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-149']).returncode==1
assert read('build/public-evidence149-test.json')['passed']
sources=read('docs/publication149/source149.json');no_pixels(sources);lock=read('docs/OBSERVATION_149_LOCK.json')
assert sha((r/'docs/OBSERVATION_149_LOCK.json').read_bytes())==sources['algorithmLockSha256']
for frozen in [lock]:
 for name,h in frozen['sourceHashes'].items():assert sha((r/name).read_bytes().replace(b'\r\n',b'\n'))==h,name
studies={};reviews={};shots=[]
for group in sorted({x['group'] for x in sources['records']}):
 study=read(f'build/occlusion149-{group}-app/result.json');review=read(f'build/occlusion149-{group}-review/result.json')
 assert study['passed'] and study['export'] and len(study['runs'])<=24
 assert review['passed'] and review['privatePixelsExcluded'] and review['completedStateUnchanged'] and review['zeroObservationWrites']
 public_study,_=public_view((r/f'build/occlusion149-{group}-app/result.json').read_bytes());no_pixels(public_study);studies[group]=study;reviews[group]=review;write(out/(group+'-app-study.json'),public_study);write(out/(group+'-screenshot-review.json'),review)
 for item in review['captures']:
  dest=group+'-'+item['name'];shutil.copy2(r/f'build/occlusion149-{group}-review'/item['name'],out/dest);shots.append({'path':'docs/readmes/149/'+dest,'title':item['caption']})
assert studies and shots,'Actual completed studies and camera-free screenshots required'
control=read('build/camera-controls-visual-review149.json');assert control['passed'] and sha((r/control['path']).read_bytes())==control['sha256'];assert read('build/webcam149-selftest.json')['passed']
shutil.copy2(r/control['path'],out/'recorder-controls-camera-off.png');shots.append({'path':'docs/readmes/149/recorder-controls-camera-off.png','title':'Actual private recorder controls before preview; camera off. Synthetic lifecycle test UI, not a tracking result.'})
restart=read('build/occlusion149-restart/result.json');assert restart['passed'];assert restart['harnessSha256']==sha((r/'docs/publication149/restart149.cjs').read_bytes());public_restart,_=public_view((r/'build/occlusion149-restart/result.json').read_bytes());no_pixels(public_restart);write(out/'restart-check.json',public_restart)
checks=set()
for p in (r/'build/validation149/final').glob('*.xml'):
 e=ET.parse(p).getroot();assert int(e.get('failures','0'))+int(e.get('errors','0'))==0
 for c in e.findall('testcase'):
  if c.find('skipped') is None:checks.add(c.get('classname')+'.'+c.get('name'))
assert checks and any('IdentityReturn149Test' in x for x in checks) and any('CalibrationStoreTest.validationCannotRetuneWinnerAndFailedGateCannotApply' in x for x in checks)
prior_checks=set(read('docs/readmes/148/verification.json')['javaChecks'])
new_checks={x for x in checks if x.startswith(('fraymus.workspace.IdentityReturn149Test.','fraymus.workspace.IdentityReserved149Test.','fraymus.workspace.IdentityMotion149Test.'))}
assert prior_checks<=checks,sorted(prior_checks-checks)
assert len(new_checks)==11 and checks==prior_checks|new_checks,'Require every predecessor regression and all eleven new checks'
for name in ['kernel149-final','java149-final','desktop149-final','agency149-regression','launcher149','package149','receipt149']:assert read('build/'+name+'.json')['exit']==0,name
desktop=(r/'build/desktop149-final.log').read_text(encoding='utf8');assert 'fail 0' in desktop;desktop_count=int(re.search(r'(?m)^. tests (\d+)',desktop)[1])
allruns=[x for s in studies.values() for x in s['runs']];rows=[{'case':x['caseName'],'group':group,'domain':x['domain'],'policy':x['policy'],'summary':x['audit']['summary'],'evidence':x['audit'].get('identity149') or x['audit'].get('confirmation148') or x['audit'].get('continuity146') or x['audit'].get('trajectory145'),'trackingMs':x['trackingMs'],'auditMs':x['auditMs']} for group,s in studies.items() for x in s['runs'] if x['role']=='evaluation']
refusals=[x for s in studies.values() for x in s.get('refusals',[])]
finding=(r/'build/findings149.md').read_text(encoding='utf8').strip();next_steps=(r/'build/next149.md').read_text(encoding='utf8').strip();attempts=(r/'build/attempts149.md').read_text(encoding='utf8').strip()
comparison={'rows':rows,'sources':sources,'lock':lock,'finding':finding,'defaultsUnchanged':True,'promoted':False,'refusals':refusals};write(out/'comparison.json',comparison)
validation=f'{len(checks)} distinct Java checks and {desktop_count} desktop checks passed, plus kernel, agency, offline preparation and package verification. {len(allruns)} actual app runs across {len(studies)} private study project(s), with separate unfinished-process recovery. Source eligibility and useful recovery remain distinct from execution checks. Hosted CI is reported separately.'
appchecks=list(dict.fromkeys([c for s in studies.values() for c in s['checks']]+restart['checks']))
verification={'passed':True,'javaChecks':sorted(checks),'desktopChecks':desktop_count,'actualAppChecks':appchecks,'candidateLock':lock,'defaultsUnchanged':True,'scope':validation,'reservedGenerated':{'path':'docs/readmes/149/reserved-generated.json'},'studies':{g:{'source':s['source'],'sourceHeaderScope':'Frozen149 production; '+('fresh149 source labels committed before predictions' if any(x['group']==g and x['domain']=='fresh-real-camera' for x in sources['records']) else 'unaltered exposed148 source packs'),'buildReceipt':s['buildReceipt'],'path':f'docs/readmes/149/{g}-app-study.json'} for g,s in studies.items()}}
positive=read('workspace-engine/build/identity149-filter/result.json');assert positive['passed'] and positive['acceptedNewMeasurements']>0
assert positive['audit']['filtered']['incorrect']==0 and positive['relabelledAudit']['filtered']['incorrect']==1
write(out/'filter-audit.json',positive);verification['positiveFilterAudit']={'path':'docs/readmes/149/filter-audit.json','scope':positive['scope']}
write(out/'verification.json',verification)
originals=[]
def preserve(p,logical):
 original=p.read_bytes();raw,omitted=public_bytes(original) if p.suffix=='.json' else (original,[]);digest=sha(raw);packed=gzip.compress(raw,mtime=0);dest=out/'originals'/(digest+'.gz');dest.parent.mkdir(exist_ok=True)
 if not dest.exists():dest.write_bytes(packed)
 originals.append({'logicalPath':logical,'path':dest.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(raw),'rawSha256':digest,**({'transformation':'public-view-149','privateOriginalBytes':len(original),'privateOriginalSha256':sha(original),'omittedFeatureArrays':len(omitted)} if omitted else {})})
# Explicit allowlist: never recurse into source images, private project stores or app screenshot folders.
allowed=[r/f'build/occlusion149-{g}-app/result.json' for g in studies]+[r/f'build/occlusion149-{g}-review/result.json' for g in studies]
allowed += [p for g in studies for p in (r/f'build/occlusion149-{g}-review').glob('result-attempt-*.json')]
allowed += [p for g in studies for p in (r/f'build/occlusion149-{g}-app').glob('result-attempt-*.json')]
allowed += [r/'build/occlusion149-restart/result.json',r/'docs/OBSERVATION_149_LOCK.json',r/'docs/publication149/restart149.cjs',r/'workspace-desktop/test/identity149-integration.cjs']
allowed += list((r/'build/validation149').rglob('*.xml'))
allowed += [r/'workspace-engine/build/identity149-reserved-v2/result.json',r/'workspace-engine/build/identity149-exposed-v2/result.json',r/'build/reserved149-v1-failed.json',r/'workspace-engine/build/identity149-filter/result.json']
for pattern in ['*149*.json','*149*.log','*149*.md']:
 allowed += [p for p in (r/'build').glob(pattern) if p.is_file()]
for p in sorted(set(allowed)):
 if p.suffix=='.json':no_pixels(public_view(p.read_bytes())[0])
 preserve(p,p.relative_to(r).as_posix())
write(out/'original-evidence.json',{'files':originals,'scope':'Allowlisted original test receipts and derived public camera views. Entries marked public-view-149 retain the hash and length of their unchanged private original; sampled RGB arrays are omitted. Private source images, project stores, camera identifiers and pixel-containing screenshots excluded. Original test paths and host names remain in exact test receipts. Exact source hashes/labels remain available.'})
paths={x['path'] for x in read('docs/readmes/148/file-inventory.json')};paths.update(f.relative_to(r).as_posix() for f in (r/'docs/publication149').iterdir() if f.is_file() and f.suffix not in ['.log'])
paths.update(['workspace-desktop/test/webcam149-recorder.cjs','workspace-desktop/test/identity149-integration.cjs','docs/publication149/PROTOCOL_149.md','docs/OBSERVATION_149_LOCK.json']);inventory=[]
paths.update(p.relative_to(r).as_posix() for p in (r/'workspace-engine/src/main/java/fraymus/workspace').glob('*149.java'))
paths.update(p.relative_to(r).as_posix() for p in (r/'workspace-engine/src/test/java/fraymus/workspace').glob('*149*.java'))
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
reserved=read('workspace-engine/build/identity149-reserved-v2/result.json');write(out/'reserved-generated.json',{**reserved,'cases':[{k:v for k,v in c.items() if k!='trace'} for c in reserved['cases']]})
reserved_table='| Reserved generated texture / case | Correct / visible | Hidden outputs | Additional149 |\n| --- | ---: | ---: | ---: |\n'
for row in reserved['cases']:reserved_table+=f"| {row['textureSeed']} / {row['case']} | {row['correct']}/{row['visible']} | {row['hiddenOutputs']}/{row['hidden']} | {row['additional']} |\n"
motion=read('build/motion149.json');assert motion['passed'];write(out/'motion.json',motion)
motion_table='**New-segment next-frame prediction, checked after evaluation:**\n\n| Source / case | New segments | Scored future centers | Mean / maximum error in pixels |\n| --- | ---: | ---: | ---: |\n'
for row in motion['rows']:
 if row['exposure']=='exposed generated regression':continue
 error='unavailable' if row['meanNextSamplePixelError'] is None else f"{row['meanNextSamplePixelError']:.3f} / {row['maximumNextSamplePixelError']:.3f}"
 name=row['source']+(' / '+str(row['textureSeed'])+' / '+row['case'] if 'textureSeed' in row else '')
 motion_table+=f"| {name} | {row['segments']} | {row['nextSampleComparisons']} | {error} |\n"
motion_table+='\nThe prediction is never used as an observation. Hidden or unavailable future labels are excluded and counted in [the full motion receipt](docs/readmes/149/motion.json). Generated geometry is exact at 320 × 240 pixels; camera boxes are manually labelled at 640 × 480 pixels. These pixel/sample results are not calibrated physical speed or probability bounds.\n'
md=(r/'docs/publication149/README-template.md').read_text(encoding='utf8')
for key,value in {'FRESH_CAMERA':(r/'build/fresh-camera149.md').read_text(encoding='utf8').strip(),'POSITIVE_CHAIN':f"The exposed generated tracker/filter/audit control accepted {positive['acceptedNewMeasurements']} new correct measurements through the unchanged filter, reproduced the trace and filter after source reconstruction, and refused a forged motion segment. Changing one independent outcome label correctly produced one failed outcome without changing the tracker. [Original chain result](docs/readmes/149/filter-audit.json).",'MOTION':motion_table,'RESERVED':reserved_table,'FINDING':finding,'HERO':'![Actual saved comparison; private camera pixels excluded]('+next(x['path'] for x in shots if 'identity-return-149' in x['path'])+')','RESULTS':table,'RECOVERIES':recovery,'SOURCE_REVIEW':source_table,'VALIDATION':validation,'APP_CHECKS':'\n'.join('- '+x for x in appchecks),'COSTS':timings,'ATTEMPTS':attempts,'NEXT':next_steps}.items():md=md.replace('{{'+key+'}}',value)
assert '{{' not in md;text(r/'README-149.md',md);text(r/'README.md',md)
note='docs/obsidian/157-fresh-identity.md';modules=[{'id':i,'name':n,'source':s,'note':note} for i,n,s in [('guard','Fresh full-object identity','workspace-engine/src/main/java/fraymus/workspace/IdentityReturn149.java'),('reference','Verified source appearances','workspace-engine/src/main/java/fraymus/workspace/ReferenceMemory137.java'),('audit','Independent original-pixel replay','workspace-engine/src/main/java/fraymus/workspace/IdentityAudit149.java'),('world','Durable observations','workspace-engine/src/main/java/fraymus/workspace/WorldObservationStore.java')]]
system={'modules':modules,'screenshots':shots,'evidence':[{'path':'docs/readmes/149/'+n} for n in ['verification.json','comparison.json','original-evidence.json','restart-check.json']],'comparison':comparison,'validation':validation,'finding':finding};write(out/'system-map.json',system)
text(r/note,'# Fresh identity and observed motion — edition149\n\n[[README-149.md]]\n\n'+finding+'\n\n'+validation+'\n\n'+next_steps+'\n\nContinue [[docs/INTEGRATED_AGENT_ROADMAP.md]].\n')
write(r/'docs/obsidian/157-fresh-identity.canvas',{'nodes':[{'id':x['id'],'type':'file','file':x['source'],'x':i*430,'y':0,'width':390,'height':190} for i,x in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']} for i in range(len(modules)-1)]})
html=(r/'docs/publication149/guide-template.html').read_text(encoding='utf8')+'<script id="system-data" type="application/json">'+json.dumps(system).replace('</','<\\/')+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory).replace('</','<\\/')+'</script><script src="guide.js"></script></body></html>';text(out/'index.html',html);shutil.copy2(r/'docs/publication149/guide.js',out/'guide.js')
for name,link in [('docs/readmes/INDEX.md','[Edition149 — fresh identity and observed motion](../../README-149.md) | [Offline guide](149/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/157-fresh-identity.md]] | [[README-149.md]]')]:
 p=r/name;s=p.read_text(encoding='utf-8-sig');text(p,link+'\n\n'+s if not s.startswith(link) else s)
print(json.dumps({'passed':True,'javaChecks':len(checks),'sourceFiles':len(inventory),'originalEntries':len(originals),'privateImagesExcluded':True}))
