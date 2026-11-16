"""Publish measured150 results and source inventories, never invented outcomes or private pixels."""
from pathlib import Path
import json,hashlib,subprocess,shutil,gzip,xml.etree.ElementTree as ET,re,datetime,sys
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'publication149'))
from public_evidence149 import public_view,public_bytes,check_public
r=Path.cwd();out=r/'docs/readmes/150';parent='e5479b7193df57eb0fdfbc930bd87fe9a0e2cc70'
read=lambda p:json.loads((r/p).read_bytes());sha=lambda b:hashlib.sha256(b).hexdigest()
def write(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes((json.dumps(v,indent=2,ensure_ascii=False)+'\n').encode())
def text(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(s.encode())
def seconds(v):return (datetime.datetime.fromisoformat(v['endedAt'].replace('Z','+00:00'))-datetime.datetime.fromisoformat(v['startedAt'].replace('Z','+00:00'))).total_seconds()
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-150']).returncode==1
sources=read('docs/publication150/source150.json');check_public(sources);lock=read('docs/OBSERVATION_150_LOCK.json')
assert sha((r/'docs/OBSERVATION_150_LOCK.json').read_bytes())==sources['algorithmLockSha256']
for name,h in lock['sourceHashes'].items():assert sha((r/name).read_bytes().replace(b'\r\n',b'\n'))==h,name
studies={};reviews={};shots=[]
group_labels={'generated150':'Generated controls','black-lighter-long-cover-exploratory':'Exposed edition 149 camera recording','webcam150-displaced-return':'Fresh edition 150 camera recording'}
for group in sorted({x['group'] for x in sources['records']}):
 study=read(f'build/reference150-{group}-app/result.json');review=read(f'build/reference150-{group}-review/result.json')
 assert study['passed'] and study['export'] and len(study['runs'])<=24
 assert review['passed'] and review['privatePixelsExcluded'] and review['completedStateUnchanged'] and review['zeroObservationWrites']
 if review.get('captureHarnessSha256'):assert review['captureHarnessSha256']==sha((r/'docs/publication150/focused-capture150.cjs').read_bytes())
 public,_=public_view((r/f'build/reference150-{group}-app/result.json').read_bytes());check_public(public);studies[group]=study;reviews[group]=review;write(out/(group+'-app-study.json'),public);write(out/(group+'-screenshot-review.json'),review)
 for item in review['captures']:
  dest=group+'-'+item['name'];shutil.copy2(r/f'build/reference150-{group}-review'/item['name'],out/dest);shots.append({'path':'docs/readmes/150/'+dest,'title':group_labels[group]+' / '+item['case']+': '+item['caption']})
assert studies and shots
controls=read('build/reference150-controls/result.json');assert controls['passed'] and controls['privatePixelsExcluded'] and controls['completedStateUnchanged'] and controls['zeroObservationWrites']
write(out/'starting-controls-review.json',controls)
for item in controls['captures']:
 shutil.copy2(r/'build/reference150-controls'/item['name'],out/item['name']);shots.append({'path':'docs/readmes/150/'+item['name'],'title':item['caption']})
visual=read('build/app-screenshots-review150.json');assert visual['passed'] and visual['reviewedBy']=='assistant-visual-inspection'
required={f"build/reference150-{group}-review/{item['name']}" for group,review in reviews.items() for item in review['captures']}
required.update('build/reference150-controls/'+item['name'] for item in controls['captures'])
assert {item['path'] for item in visual['images']}==required
for item in visual['images']:assert sha((r/item['path']).read_bytes())==item['sha256'],item['path']
write(out/'app-screenshots-visual-review.json',visual)
control=read('build/camera-controls-visual-review150.json');assert control['passed'] and sha((r/control['path']).read_bytes())==control['sha256']
for name in ['webcam150-selftest','webcam150-timeout-selftest','preflight150-tests']:assert read('build/'+name+'.json')['passed']
assert read('build/webcam150-selftest.json')['recorderSha256']==sha((r/'workspace-desktop/test/webcam150-recorder.cjs').read_bytes().replace(b'\r\n',b'\n'))
shutil.copy2(r/control['path'],out/'recorder-controls-camera-off.png');shots.append({'path':'docs/readmes/150/recorder-controls-camera-off.png','title':'Actual150 recorder before preview; camera off. Real countdown/lifecycle checked with explicitly synthetic camera input.'})
shots.sort(key=lambda item:(0 if 'webcam150-displaced-return' in item['path'] and 'identity-return-149' in item['path'] else 1,item['path']))
restart=read('build/reference150-restart/result.json');assert restart['passed'];assert restart['harnessSha256']==sha((r/'docs/publication150/restart150.cjs').read_bytes())
assert any(f['objects'][0]['tracker']['identity149']['restored'] and f['objects'][0]['estimate']['accepted'] for f in restart['recovered']['trace'])
public,_=public_view((r/'build/reference150-restart/result.json').read_bytes());check_public(public);write(out/'restart-check.json',public)
checks=set()
for p in (r/'build/validation150/final').glob('*.xml'):
 e=ET.parse(p).getroot();assert int(e.get('failures','0'))+int(e.get('errors','0'))+int(e.get('skipped','0'))==0
 for c in e.findall('testcase'):checks.add(c.get('classname')+'.'+c.get('name'))
prior_checks=set(read('docs/readmes/149/verification.json')['javaChecks'])
new_checks={x for x in checks if x.startswith(('fraymus.workspace.StartingReference150Test.','fraymus.workspace.StartingReference150FixturesTest.'))}
assert len(new_checks)==6 and checks==prior_checks|new_checks,(sorted(prior_checks-checks),len(new_checks))
for name in ['kernel150-final','java150-final','desktop150-final','agency150-regression','launcher150','package150','receipt150']:assert read('build/'+name+'.json')['exit']==0,name
desktop=(r/'build/desktop150-final.log').read_text(encoding='utf8');assert 'fail 0' in desktop;desktop_count=int(re.search(r'(?m)^. tests (\d+)',desktop)[1])
allruns=[x for s in studies.values() for x in s['runs']]
rows=[{'case':x['caseName'],'group':group,'domain':x['domain'],'policy':x['policy'],'summary':x['audit']['summary'],'evidence':x['audit'].get('identity149') or x['audit'].get('confirmation148'),'trackingMs':x['trackingMs'],'auditMs':x['auditMs']} for group,s in studies.items() for x in s['runs'] if x['role']=='evaluation']
finding=(r/'build/findings150.md').read_text(encoding='utf8').strip();next_steps=(r/'build/next150.md').read_text(encoding='utf8').strip();attempts=(r/'build/attempts150.md').read_text(encoding='utf8').strip()
comparison={'rows':rows,'sources':sources,'lock':lock,'finding':finding,'defaultsUnchanged':True,'promoted':False,'refusals':[x for s in studies.values() for x in s.get('refusals',[])]};write(out/'comparison.json',comparison)
validation=f'{len(checks)} distinct Java checks and {desktop_count} desktop checks passed, plus kernel, agency, offline preparation and packaging. {len(allruns)} actual app runs across {len(studies)} isolated study projects, plus a separate positive-segment process interruption. Source eligibility, reference availability and useful recovery are separate findings. Hosted CI is reported separately.'
appchecks=list(dict.fromkeys([c for s in studies.values() for c in s['checks']]+restart['checks']))
verification={'passed':True,'javaChecks':sorted(checks),'desktopChecks':desktop_count,'actualAppChecks':appchecks,'candidateLock':lock,'defaultsUnchanged':True,'scope':validation,'positiveSegmentRestart':True,'studies':{g:{'source':s['source'],'sourceHeaderScope':'Method/source manifest committed before study. Domain: '+', '.join(sorted({x['domain'] for x in sources['records'] if x['group']==g})),'buildReceipt':s['buildReceipt'],'path':f'docs/readmes/150/{g}-app-study.json'} for g,s in studies.items()}}
write(out/'verification.json',verification)
originals=[]
def preserve(p):
 original=p.read_bytes()
 if p.suffix=='.log':
  for marker in [b'"descriptor"',b'"rgb"',b'"image"',b'"coarse"',b'data:image/']:
   assert marker not in original,('Review potential private image features in log before publishing',str(p),marker)
 raw,omitted=public_bytes(original) if p.suffix=='.json' else (original,[]);digest=sha(raw);packed=gzip.compress(raw,mtime=0);dest=out/'originals'/(digest+'.gz');dest.parent.mkdir(exist_ok=True)
 if not dest.exists():dest.write_bytes(packed)
 originals.append({'logicalPath':p.relative_to(r).as_posix(),'path':dest.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(raw),'rawSha256':digest,**({'transformation':'public-view-149','privateOriginalBytes':len(original),'privateOriginalSha256':sha(original),'omittedFeatureArrays':len(omitted)} if omitted else {})})
allowed=[r/f'build/reference150-{g}-app/result.json' for g in studies]+[r/f'build/reference150-{g}-review/result.json' for g in studies]
allowed += [p for g in studies for suffix in ['app','review'] for p in (r/f'build/reference150-{g}-{suffix}').glob('result-attempt-*.json')]
allowed += [r/'build/reference150-restart/result.json',r/'build/reference150-controls/result.json',r/'docs/OBSERVATION_150_LOCK.json',r/'docs/publication150/restart150.cjs',r/'workspace-desktop/test/starting150-integration.cjs']
allowed += list((r/'build/validation150').rglob('*.xml'))
for pattern in ['*150*.json','*150*.log','*150*.md']:allowed += [p for p in (r/'build').glob(pattern) if p.is_file() and p.name not in ['generated150-virtual-threads.json','checkpoint150.md']]
for p in sorted(set(allowed)):
 if p.suffix=='.json':check_public(public_view(p.read_bytes())[0])
 preserve(p)
write(out/'original-evidence.json',{'files':originals,'scope':'Allowlisted original receipts and derived public camera views. public-view-149 names the unchanged privacy transformation format, not the study edition. Private originals and omitted arrays remain hash-bound; private pixels are excluded and derived views are not original verifier inputs.'})
paths={x['path'] for x in read('docs/readmes/149/file-inventory.json')};paths.update(f.relative_to(r).as_posix() for f in (r/'docs/publication150').iterdir() if f.is_file() and f.suffix!='.log')
paths.update(lock['sourceHashes']);paths.add('docs/OBSERVATION_150_LOCK.json');inventory=[]
for name in sorted(paths):
 raw=(r/name).read_bytes()
 try:raw=raw.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':name,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
write(out/'file-inventory.json',inventory)
table='| Source / method | Correct / visible | Missing | Wrong overlap | Hidden outputs | Other-object matches |\n| --- | ---: | ---: | ---: | ---: | ---: |\n';recovery='| Source / method | Additional correct / incorrect | Filter-accepted additional correct / incorrect |\n| --- | ---: | ---: |\n';timings='| Source / method | Tracking ms | Replay/audit and retention ms |\n| --- | ---: | ---: |\n'
for row in rows:
 s=row['summary'];d=row['evidence'] or {};new=d.get('additional',dict(correct=0,incorrect=0));f=d.get('filtered',dict(correct=0,incorrect=0))
 table+=f"| {row['case']} / {row['policy']} | {s['correct']}/{s['visible']} | {s['missing']} | {s['overlapFailures']} | {s['unsupported']}/{s['hidden']} | {s['confirmedDistractorMatches']} |\n"
 recovery+=f"| {row['case']} / {row['policy']} | {new['correct']} / {new['incorrect']} | {f['correct']} / {f['incorrect']} |\n";timings+=f"| {row['case']} / {row['policy']} | {row['trackingMs']:.1f} | {row['auditMs']:.1f} |\n"
references='| Donor | Verified starting references | Newly learned examples available | Learned-only refusal |\n| --- | ---: | ---: | --- |\n'
for s in studies.values():
 for d in [x for x in s['runs'] if x['role']=='reference-source']:
  references+=f"| {d['caseName']} | {len(d['retained']['examples']) if d.get('retained') else 0} | {len(d['learnedRetained']['examples']) if d.get('learnedRetained') else 0} | {d.get('learnedRetentionRefusal') or 'none'} |\n"
for g,s in studies.items():timings+=f"\nComplete {g} workflow: {seconds(s):.1f}s, {len(s['runs'])} runs, {s['export']['bytes']} exported bytes, including checks, UI, source replay and retained retry intervals.\n"
timings+=f'\nPositive-segment interrupted-process check: {seconds(restart):.1f}s. Camera-free screenshot checks: {sum(seconds(s) for s in reviews.values()):.1f}s.\n'
source_table='| Source / role | Evidence domain | Original samples | Visible / partial | Fully covered |\n| --- | --- | --- | ---: | ---: |\n'
for row in sources['records']:
 c=row['visibilityCounts'];source_table+=f"| {row['case']} / {row['role']} | {row['domain']} | {row['start']}–{row['end']} | {c['visible']} / {c['partial']} | {c['occluded']} |\n"
md=(r/'docs/publication150/README-template.md').read_text(encoding='utf8')
replacements={'FINDING':finding,'FRESH_CAMERA':(r/'build/fresh-camera150.md').read_text(encoding='utf8').strip(),'HERO':'![Actual fresh-camera comparison; source imagery hidden]('+next(x['path'] for x in shots if 'webcam150-displaced-return' in x['path'] and 'identity-return-149' in x['path'])+')','SOURCE_REVIEW':source_table,'RESULTS':table,'RECOVERIES':recovery,'REFERENCES':references,'VALIDATION':validation,'APP_CHECKS':'\n'.join('- '+x for x in appchecks),'RESTART':'The actual Java process was interrupted after a new measured motion segment on the generated control. Cold reopen retained that segment and the unfinished prefix. Resumption reproduced the complete uninterrupted trace and independent outcomes. This is a genuine process-recovery check on generated pixels, not a new physical-camera validation.','COSTS':timings,'ATTEMPTS':attempts,'NEXT':next_steps}
replacements['CAPTURE_DATES']='Actual app screenshots captured '+', '.join(sorted({s['startedAt'][:10] for s in reviews.values()}|{controls['startedAt'][:10]}))+' (UTC). Each linked screenshot has its original capture receipt and hash-bound visual review in this edition’s evidence. Private source imagery is hidden.'
for k,v in replacements.items():md=md.replace('{{'+k+'}}',v)
assert '{{' not in md;text(r/'README-150.md',md);text(r/'README.md',md)
note='docs/obsidian/158-starting-references.md';modules=[{'id':i,'name':n,'source':s,'note':note} for i,n,s in [('guard','Verified starting reference','workspace-engine/src/main/java/fraymus/workspace/StartingReference150.java'),('reference','Atomic retention and rechecking','workspace-engine/src/main/java/fraymus/workspace/VerifiedAppearances135.java'),('audit','Existing149 original-pixel recovery audit','workspace-engine/src/main/java/fraymus/workspace/IdentityAudit149.java'),('recorder','Measured capture timing','workspace-desktop/renderer/recorder150-timing.js')]]
system={'modules':modules,'screenshots':shots,'evidence':[{'path':'docs/readmes/150/'+n} for n in ['verification.json','comparison.json','original-evidence.json','restart-check.json']],'comparison':comparison,'validation':validation,'finding':finding};write(out/'system-map.json',system)
text(r/note,'# Verified starting references — edition150\n\n[[README-150.md]]\n\n'+finding+'\n\n'+validation+'\n\n'+next_steps+'\n\nContinue [[docs/INTEGRATED_AGENT_ROADMAP.md]].\n')
write(r/'docs/obsidian/158-starting-references.canvas',{'nodes':[{'id':x['id'],'type':'file','file':x['source'],'x':i*430,'y':0,'width':390,'height':190} for i,x in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']} for i in range(len(modules)-1)]})
html=(r/'docs/publication150/guide-template.html').read_text(encoding='utf8')+'<script id="system-data" type="application/json">'+json.dumps(system).replace('</','<\\/')+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory).replace('</','<\\/')+'</script><script src="guide.js"></script></body></html>';text(out/'index.html',html);shutil.copy2(r/'docs/publication149/guide.js',out/'guide.js')
for name,link in [('docs/readmes/INDEX.md','[Edition150 — verified starting references](../../README-150.md) | [Offline guide](150/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/158-starting-references.md]] | [[README-150.md]]')]:
 p=r/name;s=p.read_text(encoding='utf-8-sig');text(p,link+'\n\n'+s if not s.startswith(link) else s)
print(json.dumps({'passed':True,'javaChecks':len(checks),'sourceFiles':len(inventory),'originalEntries':len(originals),'privateImagesExcluded':True}))
