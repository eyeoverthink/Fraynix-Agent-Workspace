from pathlib import Path
import json,hashlib,subprocess,shutil,gzip,xml.etree.ElementTree as ET,re,statistics
r=Path.cwd();out=r/'docs/readmes/133';out.mkdir(parents=True,exist_ok=True)
read=lambda p:json.loads((r/p).read_text(encoding='utf8'))
sha=lambda b:hashlib.sha256(b).hexdigest()
def text(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8')
def write(p,v):text(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-133']).returncode==1
study=read('build/contact133-app/result.json');assert study['passed'];write(out/'app-study.json',study)
for name in study['captures']:shutil.copy2(r/'build/contact133-app'/name,out/name)
lock=read('docs/OBSERVATION_133_LOCK.json')
for n,h in lock['sourceHashes'].items():assert sha((r/n).read_bytes().replace(b'\r\n',b'\n'))==h,n
checks=set()
for p in (r/'build/validation133/final').glob('*.xml'):
 e=ET.parse(p).getroot();assert int(e.get('failures','0'))+int(e.get('errors','0'))==0,p
 for c in e.findall('testcase'):
  if c.find('skipped') is None:checks.add(c.get('classname')+'.'+c.get('name'))
assert checks
desktop=(r/'build/desktop133-final.log').read_text(encoding='utf8');assert 'fail 0' in desktop;desktop_count=int(re.search(r'(?m)^. tests (\d+)',desktop)[1])
for n in ['kernel133-final','java133-final','desktop133-final','agency133-regression','launcher133','package133','receipt133']:assert read('build/'+n+'.json')['exit']==0,n
prior=read('docs/readmes/132/app-study.json');comparison={'rows':[],'summary':{},'prospectiveSummary':{},'paired':{},'lock':lock,'defaultsUnchanged':True,'promotionAttempted':False}
for window in [600,1600,2600,3600,4600]:
 for policy in ['candidate','default-support']:
  p=f'build/association133-development/{"final" if policy=="candidate" else "default-support"}-{window}.json' if window in [600,2600] else f'build/association133-comparison/{policy}-{window}.json'
  result=read(p);m=result['metrics'];old=next(c for c in prior['comparisons'] if c['window']==window and c['policy']=='legacy-flow')
  pairs=[]
  for score in result['scores']:
   original=next(s for s in old['audit']['scores'] if s['ordinal']==score['ordinal']);assert original['imageSha256']==score['imageSha256']
   if 'errorPx' in score and 'rawAnchorErrorPx' in original:pairs.append({'window':window,'ordinal':score['ordinal'],'defaultRawPx':original['rawAnchorErrorPx'],'defaultFilteredPx':original['filteredAnchorErrorPx'],'supportPx':score['errorPx']})
  comparison['rows'].append({'window':window,'policy':policy,'development':window in [600,2600],'evidence':p,'summary':m,'pairs':pairs})
for policy in ['candidate','default-support']:
 for field,subset in [('summary',comparison['rows']),('prospectiveSummary',[x for x in comparison['rows'] if not x['development']])]:
  rows=[x for x in subset if x['policy']==policy];available=sum(x['summary']['available'] for x in rows);observable=sum(x['summary']['observable'] for x in rows)
  comparison[field][policy]={'available':available,'observable':observable,'missing':observable-available,'within':sum(x['summary']['within'] for x in rows),'unsupported':sum(x['summary']['unsupported'] for x in rows),'meanPx':sum((x['summary']['meanPx'] or 0)*x['summary']['available'] for x in rows)/available if available else None,'processingMs':sum(x['summary']['processingMs'] for x in rows)}
 pairs=[p for x in comparison['rows'] if x['policy']==policy for p in x['pairs']]
 comparison['paired'][policy]={'count':len(pairs),'defaultRawMeanPx':statistics.mean(p['defaultRawPx'] for p in pairs) if pairs else None,'supportMeanPx':statistics.mean(p['supportPx'] for p in pairs) if pairs else None,'rows':pairs}
comparison['published132Default']=read('docs/readmes/132/comparison.json')['summary']['legacy-flow'];write(out/'comparison.json',comparison)
v={'passed':True,'javaChecks':sorted(checks),'desktopChecks':desktop_count,'actualAppChecks':study['checks'],'production':study['source'],'buildReceipt':study['buildReceipt'],'candidateLock':lock,'newModelCalls':0,'defaultsUnchanged':True,'scope':'Software regression, fixed prospective implementation comparison on already exposed footage, actual UI/source arithmetic/export/cold restart. No tracker promotion or physical accuracy certification. Hosted CI separate.'};write(out/'verification.json',v)
originals=[];seen=set()
def preserve(p,logical):
 if not p.is_file() or logical in seen:return
 seen.add(logical);raw=p.read_bytes();key=sha(raw);dest=out/'originals'/(key+'.gz');packed=gzip.compress(raw,mtime=0);dest.parent.mkdir(exist_ok=True)
 if not dest.exists():dest.write_bytes(packed)
 originals.append({'logicalPath':logical,'path':dest.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(raw),'rawSha256':key})
for pattern in ['contact133-app*','association133-development','association133-comparison','agency133-evidence','validation133']:
 for folder in (r/'build').glob(pattern):
  if not folder.is_dir():continue
  for p in sorted(folder.rglob('*')):
   if any(x in p.parts for x in ['desktop-profile','node_modules']) or p.name.endswith('.fobs.json'):continue
   if p.is_file() and p.suffix in ['.json','.jsonl','.xml','.png','.txt','.log','.html','.css','.js','.cjs','.fworld','.previous','.csv','.evidence','.pending','.before','.java','.class']:preserve(p,p.relative_to(r).as_posix())
for p in sorted((r/'build').glob('*133*')):
 if p.is_file():preserve(p,p.relative_to(r).as_posix())
for name in ['docs/OBSERVATION_133_LOCK.json','docs/publication132/source-labels.json']:preserve(r/name,name)
write(out/'original-evidence.json',{'files':originals,'scope':'Original development attempts, frozen candidate comparisons, source labels, complete run traces, contact audit results, actual app records and test output. Raw video, source-only inspection sheets, private data and browser profiles excluded; limited app screenshots attributed.'})
paths={x['path'] for x in read('docs/readmes/132/file-inventory.json')}
for n in subprocess.check_output(['git','diff','--name-only','62c62fda','HEAD'],text=True).splitlines():
 if n.startswith(('workspace-engine/','workspace-desktop/','src/','assets/','.github/')) or n=='build.gradle':paths.add(n)
paths.update(['docs/EDITION_133_CONTRACT.md','docs/OBSERVATION_133_LOCK.json','docs/publication132/source-labels.json']);inventory=[]
for n in sorted(paths):
 raw=(r/n).read_bytes()
 try:raw=raw.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':n,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
write(out/'file-inventory.json',inventory)
validation=f"Local checks passed: {len(checks)} distinct Java tests, {desktop_count} desktop checks, executable kernel integration, actual agency regression and isolated offline/package preparation. Actual app checks independently recompute contact errors, compare 190 original tracker/filter frames, export exact evidence and cold-recheck after owned-Java termination. Existing verification conditions remain intact."
table='| Method | Available / observable | Missing | Within 3 px / observable | Accepted-only mean / px | Unsupported on unobservable references |\n| --- | ---: | ---: | ---: | ---: | ---: |\n'
old=comparison['published132Default'];table+=f"| Published 132 default filtered box anchor | {old['contactAvailable']} /22 | {old['contactMissing']} | {old['withinLabelRadius']} /22 | {old['filteredAnchorMeanErrorPx']:.3f} | Not a support-point estimate |\n"
for policy,m in comparison['summary'].items():table+=f"| {policy} visual support | {m['available']} /{m['observable']} | {m['missing']} | {m['within']} /{m['observable']} | {m['meanPx']:.3f} | {m['unsupported']} |\n"
finding='The frozen comparison subset 1600/3600/4600 has 14 observable contacts. '
for policy,m in comparison['prospectiveSummary'].items():finding+=f"{policy}: {m['available']}/14 returned, {m['within']}/14 within 3 px, mean accepted error {m['meanPx']:.3f}px. "
p=comparison['paired']['default-support'];finding+=f"On all {p['count']} samples where default-box visual support is available, the original raw box anchor averages {p['defaultRawMeanPx']:.3f}px error and new support {p['supportMeanPx']:.3f}px. This paired subset is descriptive; it does not erase the missing contacts. The conservative association candidate misses too many targets. Contact precision improves on some visible samples, but availability is substantially lower."
cost='| Processing | Measured total / ms | Scope |\n| --- | ---: | --- |\n'
for policy,m in comparison['summary'].items():cost+=f"| {policy} | {m['processingMs']:.1f} | Five fixed 96-image windows; {'new tracking/filter/support' if policy=='candidate' else 'new support on published default boxes; historical tracking excluded'} |\n"
cost+='\n| Actual app run | Tracking / ms | Verified UI audit / ms |\n| --- | ---: | ---: |\n'
for x in study['runs']:cost+=f"| {x['policy']} | {x['trackingMs']:.1f} | {x['auditMs']:.1f} |\n"
attempts=(r/'build/attempts133.md').read_text(encoding='utf8');md=(r/'docs/publication133/README-template.md').read_text(encoding='utf8')
for k,value in {'RESULTS':table,'FINDING':finding,'COST':cost,'VALIDATION':validation,'APP_CHECKS':'\n'.join('- '+x for x in study['checks']),'ATTEMPTS':attempts}.items():md=md.replace('{{'+k+'}}',value)
assert '{{' not in md;text(r/'README-133.md',md);text(r/'README.md',md)
note='docs/obsidian/141-association-visual-support.md'
modules=[{'id':i,'name':n,'source':s,'note':note} for i,n,s in [('association','Frozen appearance association','workspace-engine/src/main/java/fraymus/workspace/AssociationContact133.java'),('support','Pixel-derived visual support','workspace-engine/src/main/java/fraymus/workspace/VisualSupport133.java'),('audit','Immutable source and contact audit','workspace-engine/src/main/java/fraymus/workspace/ContactAudit133.java'),('panel','Actual contact comparison controls','workspace-desktop/renderer/contact133-panel.js')]]
system={'modules':modules,'screenshots':[{'path':'docs/readmes/133/'+name,'title':name.removesuffix('.png').replace('-',' ')} for name in study['captures']],'evidence':[{'path':'docs/readmes/133/'+n} for n in ['verification.json','app-study.json','original-evidence.json','comparison.json']],'comparison':comparison,'validation':validation};write(out/'system-map.json',system)
text(r/note,'# Association and visual support — edition133\n\n[[README-133.md]]\n\n'+validation+'\n\n'+finding+'\n\nFrozen original sources, declared retrospective AI labels, missing/unsupported denominators and unchanged defaults. Prior proofs never relabelled; contact evidence on original boxes is new scoring, not replayed tracking. Next: independently adjudicated association/contact and availability improvement without reassignment; preserve [[docs/INTEGRATED_AGENT_ROADMAP.md]].\n')
write(r/'docs/obsidian/141-association-visual-support.canvas',{'nodes':[{'id':x['id'],'type':'file','file':x['source'],'x':i%2*430,'y':i//2*250,'width':390,'height':190} for i,x in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']} for i in range(len(modules)-1)]})
html=(r/'docs/publication133/guide-template.html').read_text(encoding='utf8')+'<script id="system-data" type="application/json">'+json.dumps(system).replace('</','<\\/')+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory).replace('</','<\\/')+'</script><script src="guide.js"></script></body></html>'
text(out/'index.html',html);shutil.copy2(r/'docs/publication133/guide.js',out/'guide.js')
for name,link in [('docs/readmes/INDEX.md','[Edition133 — association and visual support](../../README-133.md) | [Offline guide](133/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/141-association-visual-support.md]] | [[README-133.md]]')]:
 p=r/name;s=p.read_text(encoding='utf-8-sig');text(p,link+'\n\n'+s if not s.startswith(link) else s)
print(json.dumps({'passed':True,'javaChecks':len(checks),'sources':len(inventory),'originalEntries':len(originals),'summary':comparison['summary']}))
