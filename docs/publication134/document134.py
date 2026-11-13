from pathlib import Path
import json,hashlib,subprocess,shutil,gzip,xml.etree.ElementTree as ET,re
r=Path.cwd();out=r/'docs/readmes/134';out.mkdir(parents=True,exist_ok=True)
read=lambda p:json.loads((r/p).read_text(encoding='utf8'));sha=lambda b:hashlib.sha256(b).hexdigest()
def text(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8')
def write(p,v):text(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-134']).returncode==1
study=read('build/association134-app/result.json');assert study['passed'];write(out/'app-study.json',study)
for name in study['captures']:shutil.copy2(r/'build/association134-app'/name,out/name)
lock=read('docs/OBSERVATION_134_LOCK.json')
for n,h in lock['sourceHashes'].items():assert sha((r/n).read_bytes().replace(b'\r\n',b'\n'))==h,n
checks=set()
for p in (r/'build/validation134/final').glob('*.xml'):
 e=ET.parse(p).getroot();assert int(e.get('failures','0'))+int(e.get('errors','0'))==0,p
 for c in e.findall('testcase'):
  if c.find('skipped') is None:checks.add(c.get('classname')+'.'+c.get('name'))
assert checks
desktop=(r/'build/desktop134-final.log').read_text(encoding='utf8');assert 'fail 0' in desktop;desktop_count=int(re.search(r'(?m)^. tests (\d+)',desktop)[1])
for n in ['kernel134-final','java134-final','desktop134-final','agency134-regression','launcher134','package134','receipt134']:assert read('build/'+n+'.json')['exit']==0,n
sources=read('docs/publication134/source134.json');comparison={'rows':[],'summary':{},'lock':lock,'sources':sources,'defaultsUnchanged':True,'promoted':False}
for source in sources['records']:
 for policy in lock['methods']:
  p='build/association134-comparison/'+source['case']+'-'+policy+'.json';d=read(p);assert d['sourceSha256']==source['packSha256'] and d['labelsSha256']==source['labelsSha256'];comparison['rows'].append({'case':source['case'],'policy':policy,'summary':d['audit']['summary'],'episodes':d['audit']['episodes'],'processingMs':d['processingMs'],'evidence':p})
for policy in lock['methods']:
 rows=[x for x in comparison['rows'] if x['policy']==policy];total={k:sum(x['summary'][k] for x in rows) for k in ['labels','visible','available','missing','correct','overlapFailures','hidden','unsupported','confirmedDistractorMatches','reacquired','correctReacquired','uncertain']};total['processingMs']=sum(x['processingMs'] for x in rows);comparison['summary'][policy]=total
write(out/'comparison.json',comparison)
v={'passed':True,'javaChecks':sorted(checks),'desktopChecks':desktop_count,'actualAppChecks':study['checks'],'production':study['source'],'buildReceipt':study['buildReceipt'],'candidateLock':lock,'defaultsUnchanged':True,'scope':'Software tests, nine frozen comparisons on independently supplied OTB boxes and separately declared AI visibility, actual UI/source arithmetic/export/cold restart. Candidate failed, no promotion or general identity/physical certification. Hosted CI separate.'};write(out/'verification.json',v)
originals=[];seen=set()
def preserve(p,logical):
 if not p.is_file() or logical in seen:return
 seen.add(logical);raw=p.read_bytes();key=sha(raw);dest=out/'originals'/(key+'.gz');packed=gzip.compress(raw,mtime=0);dest.parent.mkdir(exist_ok=True)
 if not dest.exists():dest.write_bytes(packed)
 originals.append({'logicalPath':logical,'path':dest.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(raw),'rawSha256':key})
for pattern in ['association134-app*','association134-comparison','agency134-evidence','validation134']:
 for folder in (r/'build').glob(pattern):
  if not folder.is_dir():continue
  for p in sorted(folder.rglob('*')):
   if any(x in p.parts for x in ['desktop-profile','node_modules']) or p.name.endswith('.fobs.json'):continue
   if p.is_file() and p.suffix in ['.json','.jsonl','.xml','.png','.txt','.log','.html','.css','.js','.cjs','.fworld','.previous','.csv','.evidence','.pending','.before','.java','.class']:preserve(p,p.relative_to(r).as_posix())
for p in sorted((r/'build').glob('*134*')):
 if p.is_file():preserve(p,p.relative_to(r).as_posix())
for name in ['docs/OBSERVATION_134_LOCK.json','build/otb134/download.json','build/otb134/source-index.json']:preserve(r/name,name)
for p in sorted((r/'docs/publication134').glob('*')):
 if p.suffix in ['.json','.txt']:preserve(p,p.relative_to(r).as_posix())
write(out/'original-evidence.json',{'files':originals,'scope':'Original source/annotation hashes and exact external annotations, all comparison traces, actual app records, test results and failure attempts. Raw ZIP/JPEG/frame packs, source-review sheets and private/browser data excluded. Limited actual app screenshots attributed.'})
paths={x['path'] for x in read('docs/readmes/133/file-inventory.json')}
for n in subprocess.check_output(['git','diff','--name-only','dcfa037f','HEAD'],text=True).splitlines():
 if n.startswith(('workspace-engine/','workspace-desktop/','src/','assets/','.github/')) or n in ['build.gradle','.gitattributes']:paths.add(n)
paths.update(['docs/EDITION_134_CONTRACT.md','docs/OBSERVATION_134_LOCK.json','docs/publication134/source134.json']);inventory=[]
for n in sorted(paths):
 raw=(r/n).read_bytes()
 try:raw=raw.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':n,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
write(out/'file-inventory.json',inventory)
validation=f"Local checks passed: {len(checks)} distinct Java tests, {desktop_count} desktop checks, executable kernel integration, actual agency regression and isolated offline/package preparation. Four actual app runs match all 323 frozen tracker/filter samples, verify overlap independently, export original evidence and cold-recheck after owned-Java termination. Every existing source/outcome condition remains intact."
table='| Method | Correct / visible | Missing | Confirmed other-object matches | Unsupported while hidden | Correct reacquisitions |\n| --- | ---: | ---: | ---: | ---: | ---: |\n'
for policy,m in comparison['summary'].items():table+=f"| {policy} | {m['correct']} / {m['visible']} | {m['missing']} | {m['confirmedDistractorMatches']} | {m['unsupported']} / {m['hidden']} | {m['correctReacquired']} / {m['reacquired']} |\n"
table+='\n| Source / target | Existing flow correct | Edition 133 correct | Edition 134 correct | Visible labels |\n| --- | ---: | ---: | ---: | ---: |\n'
for source in sources['records']:
 rows=[x for x in comparison['rows'] if x['case']==source['case']];table+='| '+source['case']+' | '+' | '.join(str(x['summary']['correct']) for x in rows)+' | '+str(source['visible'])+' |\n'
finding='The existing tracker recovers Jogging target 1 at sample 23 (original frame 93), four sampled intervals after first visibility at sample 19 (frame 77). Neither experimental candidate recovers after that labelled occlusion. Edition 134 has three correct reacquisition events elsewhere, but only 7/246 visible samples measured overall versus 83/246 for the existing tracker and 23/246 for edition 133. No thresholds changed after these results. **This is a failed candidate comparison, not a tracking improvement or proof of switch-free recovery.**'
cost='| Method | Standalone processing total / ms |\n| --- | ---: |\n'
for policy,m in comparison['summary'].items():cost+=f"| {policy} | {m['processingMs']:.1f} |\n"
cost+='\n| Actual app task | Tracking / ms | Verified audit / ms |\n| --- | ---: | ---: |\n'
for x in study['runs']:cost+=f"| {x['caseName']} / {x['policy']} | {x['trackingMs']:.1f} | {x['auditMs']:.1f} |\n"
attempts=(r/'build/attempts134.md').read_text(encoding='utf8');md=(r/'docs/publication134/README-template.md').read_text(encoding='utf8')
for k,value in {'RESULTS':table,'FINDING':finding,'COST':cost,'VALIDATION':validation,'APP_CHECKS':'\n'.join('- '+x for x in study['checks']),'ATTEMPTS':attempts}.items():md=md.replace('{{'+k+'}}',value)
assert '{{' not in md;text(r/'README-134.md',md);text(r/'README.md',md)
note='docs/obsidian/142-identity-recovery.md';modules=[{'id':i,'name':n,'source':s,'note':note} for i,n,s in [('recovery','Frozen part recovery','workspace-engine/src/main/java/fraymus/workspace/IdentityRecovery134.java'),('audit','Independent association audit','workspace-engine/src/main/java/fraymus/workspace/AssociationAudit134.java'),('panel','Actual source and recovery controls','workspace-desktop/renderer/association134-panel.js')]]
system={'modules':modules,'screenshots':[{'path':'docs/readmes/134/'+name,'title':name.removesuffix('.png').replace('-',' ')} for name in study['captures']],'evidence':[{'path':'docs/readmes/134/'+n} for n in ['verification.json','app-study.json','original-evidence.json','comparison.json']],'comparison':comparison,'validation':validation};write(out/'system-map.json',system)
text(r/note,'# Identity recovery — edition 134\n\n[[README-134.md]]\n\n'+validation+'\n\n'+finding+'\n\nIndependent OTB boxes and separate AI source visibility, full missing denominators, no promotion. Continue [[docs/INTEGRATED_AGENT_ROADMAP.md]].\n')
write(r/'docs/obsidian/142-identity-recovery.canvas',{'nodes':[{'id':x['id'],'type':'file','file':x['source'],'x':i*430,'y':0,'width':390,'height':190} for i,x in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']} for i in range(len(modules)-1)]})
html=(r/'docs/publication134/guide-template.html').read_text(encoding='utf8')+'<script id="system-data" type="application/json">'+json.dumps(system).replace('</','<\\/')+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory).replace('</','<\\/')+'</script><script src="guide.js"></script></body></html>'
text(out/'index.html',html);shutil.copy2(r/'docs/publication134/guide.js',out/'guide.js')
for name,link in [('docs/readmes/INDEX.md','[Edition 134 — identity recovery](../../README-134.md) | [Offline guide](134/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/142-identity-recovery.md]] | [[README-134.md]]')]:
 p=r/name;s=p.read_text(encoding='utf-8-sig');text(p,link+'\n\n'+s if not s.startswith(link) else s)
print(json.dumps({'passed':True,'javaChecks':len(checks),'sources':len(inventory),'originalEntries':len(originals),'summary':comparison['summary']}))
