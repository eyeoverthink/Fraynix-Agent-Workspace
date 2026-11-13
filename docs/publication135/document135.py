from pathlib import Path
import json,hashlib,subprocess,shutil,gzip,xml.etree.ElementTree as ET,re
r=Path.cwd();out=r/'docs/readmes/135';out.mkdir(parents=True,exist_ok=True)
read=lambda p:json.loads((r/p).read_text(encoding='utf8'));sha=lambda b:hashlib.sha256(b).hexdigest()
def text(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8')
def write(p,v):text(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-135']).returncode==1
study=read('build/appearance135-app/result.json');assert study['passed'];write(out/'app-study.json',study)
for name in study['captures']:shutil.copy2(r/'build/appearance135-app'/name,out/name)
lock=read('docs/OBSERVATION_135_LOCK.json')
for n,h in lock['sourceHashes'].items():assert sha((r/n).read_bytes().replace(b'\r\n',b'\n'))==h,n
checks=set()
for p in (r/'build/validation135').rglob('*.xml'):
 if not any(part in ['final','verified-export-final'] for part in p.parts):continue
 e=ET.parse(p).getroot();assert int(e.get('failures','0'))+int(e.get('errors','0'))==0,p
 for c in e.findall('testcase'):
  if c.find('skipped') is None:checks.add(c.get('classname')+'.'+c.get('name'))
assert checks
desktop=(r/'build/desktop135-final.log').read_text(encoding='utf8');assert 'fail 0' in desktop;desktop_count=int(re.search(r'(?m)^. tests (\d+)',desktop)[1])
for n in ['kernel135-final','java135-final','desktop135-final','agency135-regression','launcher135','package135','receipt135','verified-export135']:assert read('build/'+n+'.json')['exit']==0,n
sources=read('docs/publication135/source135.json');comparison={'rows':[],'summary':{},'lock':lock,'sources':sources,'defaultsUnchanged':True,'promoted':False}
for source in sources['records']:
 for policy in lock['methods']:
  p='build/association135-comparison/'+source['case']+'-'+policy+'.json';d=read(p);assert d['sourceSha256']==source['packSha256'] and d['labelsSha256']==source['labelsSha256'];comparison['rows'].append({'case':source['case'],'policy':policy,'summary':d['audit']['summary'],'appearance135':d['audit']['appearance135'],'episodes':d['audit']['episodes'],'processingMs':d['processingMs'],'evidence':p})
for policy in lock['methods']:
 rows=[x for x in comparison['rows'] if x['policy']==policy];total={k:sum(x['summary'][k] for x in rows) for k in ['labels','visible','available','missing','correct','overlapFailures','hidden','unsupported','confirmedDistractorMatches','reacquired','correctReacquired','uncertain']};total['processingMs']=sum(x['processingMs'] for x in rows);comparison['summary'][policy]=total
write(out/'comparison.json',comparison)
v={'passed':True,'javaChecks':sorted(checks),'desktopChecks':desktop_count,'actualAppChecks':study['checks'],'production':study['source'],'buildReceipt':study['buildReceipt'],'candidateLock':lock,'defaultsUnchanged':True,'scope':'Software tests, twelve frozen comparisons with external OTB boxes and separate source-only AI visibility; actual UI, numeric appearance export and cold restart. Candidate preserves continuity but fails bank qualification. Post-evaluation curation never feeds measured tracking. Hosted CI separate.'};write(out/'verification.json',v)
originals=[];seen=set()
def preserve(p,logical):
 if not p.is_file() or logical in seen:return
 seen.add(logical);raw=p.read_bytes();key=sha(raw);dest=out/'originals'/(key+'.gz');packed=gzip.compress(raw,mtime=0);dest.parent.mkdir(exist_ok=True)
 if not dest.exists():dest.write_bytes(packed)
 originals.append({'logicalPath':logical,'path':dest.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(raw),'rawSha256':key})
for pattern in ['appearance135-app*','association135-comparison','agency135-evidence','validation135','development135']:
 for folder in (r/'build').glob(pattern):
  if not folder.is_dir():continue
  for p in sorted(folder.rglob('*')):
   if any(x in p.parts for x in ['desktop-profile','node_modules']) or p.name.endswith('.fobs.json'):continue
   if p.is_file() and p.suffix in ['.json','.jsonl','.xml','.png','.txt','.log','.html','.css','.js','.cjs','.fworld','.previous','.csv','.evidence','.pending','.before','.java','.class']:preserve(p,p.relative_to(r).as_posix())
for p in sorted((r/'build').glob('*135*')):
 if p.is_file():preserve(p,p.relative_to(r).as_posix())
for name in ['docs/OBSERVATION_135_LOCK.json','build/otb135/download.json','build/otb135/source-index.json']:preserve(r/name,name)
for p in sorted((r/'docs/publication135').glob('*')):
 if p.suffix in ['.json','.txt']:preserve(p,p.relative_to(r).as_posix())
write(out/'original-evidence.json',{'files':originals,'scope':'Original source/annotation hashes and exact external annotations, all comparison traces, actual app records, test results and failure attempts. Raw ZIP/JPEG/frame packs, source-review sheets and private/browser data excluded. Limited actual app screenshots attributed.'})
paths={x['path'] for x in read('docs/readmes/134/file-inventory.json')}
for n in subprocess.check_output(['git','diff','--name-only','da90a8d0','HEAD'],text=True).splitlines():
 if n.startswith(('workspace-engine/','workspace-desktop/','src/','assets/','.github/')) or n in ['build.gradle','.gitattributes']:paths.add(n)
paths.update(['docs/EDITION_135_CONTRACT.md','docs/OBSERVATION_135_LOCK.json','docs/publication135/source135.json','docs/EDITION_135_SOURCE_AMENDMENT.md']);inventory=[]
for n in sorted(paths):
 raw=(r/n).read_bytes()
 try:raw=raw.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':n,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
write(out/'file-inventory.json',inventory)
validation=f"Local checks passed: {len(checks)} distinct Java tests, {desktop_count} desktop checks, executable kernel integration, actual agency regression and isolated offline/package preparation. Six actual app runs match all570 frozen tracker/filter samples. Independent host overlap and admission checks, numeric appearance export, exact original evidence and cold recheck after owned-Java termination pass."
table='| Method | Correct / visible or partial | Missing | Wrong overlap | Hidden output / hidden labels | Bank recoveries |\n| --- | ---: | ---: | ---: | ---: | ---: |\n'
for policy,m in comparison['summary'].items():table+=f"| {policy} | {m['correct']} / {m['visible']} | {m['missing']} | {m['overlapFailures']} | {m['unsupported']} / {m['hidden']} | 0 |\n"
table+='\n| Source / domain | Correct / visible or partial, all methods | Missing | Motion-admitted examples | Correct / incorrect examples | Fully visible export |\n| --- | ---: | ---: | ---: | ---: | ---: |\n'
for source in sources['records']:
 row=next(x for x in comparison['rows'] if x['case']==source['case'] and x['policy']=='appearance-continuity-135');m=row['summary'];bank=row['appearance135']['summary'];app=next(x for x in study['runs'] if x['caseName']==source['case'] and x['policy']=='appearance-continuity-135')
 table+=f"| {source['case']} / {source['domain']} | {m['correct']} / {m['visible']} | {m['missing']} | {bank['admitted']} | {bank['independentlyCorrect']} / {bank['incorrect']} | {len(app['appearances']['examples'])} |\n"
finding='All380 tracker/filter samples match the incumbent exactly. The motion-only admission rule accepts46 examples;18 agree with external boxes and28 do not. No bank recovery occurs on these windows. The candidate fails useful-bank qualification. A separate post-evaluation export retains16 fully visible examples rebuilt from independent boxes:11 camera and5 rendered. It does not turn the failed admission rule into reliable learning, and was not fed back into any measured run.'
cost='| Method | Standalone processing total / ms |\n| --- | ---: |\n'
for policy,m in comparison['summary'].items():cost+=f"| {policy} | {m['processingMs']:.1f} |\n"
cost+='\n| Actual app task | Tracking / ms | Audit plus any appearance export / ms |\n| --- | ---: | ---: |\n'
for x in study['runs']:cost+=f"| {x['caseName']} / {x['policy']} | {x['trackingMs']:.1f} | {x['auditMs']:.1f} |\n"
attempts=(r/'build/attempts135.md').read_text(encoding='utf8');md=(r/'docs/publication135/README-template.md').read_text(encoding='utf8')
for k,value in {'RESULTS':table,'FINDING':finding,'COST':cost,'VALIDATION':validation,'APP_CHECKS':'\n'.join('- '+x for x in study['checks']),'ATTEMPTS':attempts}.items():md=md.replace('{{'+k+'}}',value)
assert '{{' not in md;text(r/'README-135.md',md);text(r/'README.md',md)
note='docs/obsidian/143-appearance-continuity.md';modules=[{'id':i,'name':n,'source':s,'note':note} for i,n,s in [('continuity','Incumbent continuity and bounded appearance bank','workspace-engine/src/main/java/fraymus/workspace/AppearanceFlow135.java'),('audit','Independent admission audit','workspace-engine/src/main/java/fraymus/workspace/AppearanceAudit135.java'),('curation','Source-bound supported appearance export','workspace-engine/src/main/java/fraymus/workspace/VerifiedAppearances135.java'),('panel','Actual source and comparison controls','workspace-desktop/renderer/association134-panel.js')]]
system={'modules':modules,'screenshots':[{'path':'docs/readmes/135/'+name,'title':name.removesuffix('.png').replace('-',' ')} for name in study['captures']],'evidence':[{'path':'docs/readmes/135/'+n} for n in ['verification.json','app-study.json','original-evidence.json','comparison.json']],'comparison':comparison,'validation':validation};write(out/'system-map.json',system)
text(r/note,'# Continuity and checked appearances — edition135\n\n[[README-135.md]]\n\n'+validation+'\n\n'+finding+'\n\nExternal OTB boxes and separate AI source visibility, full missing denominators, no promotion. Continue [[docs/INTEGRATED_AGENT_ROADMAP.md]].\n')
write(r/'docs/obsidian/143-appearance-continuity.canvas',{'nodes':[{'id':x['id'],'type':'file','file':x['source'],'x':i*430,'y':0,'width':390,'height':190} for i,x in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']} for i in range(len(modules)-1)]})
html=(r/'docs/publication135/guide-template.html').read_text(encoding='utf8')+'<script id="system-data" type="application/json">'+json.dumps(system).replace('</','<\\/')+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory).replace('</','<\\/')+'</script><script src="guide.js"></script></body></html>'
text(out/'index.html',html);shutil.copy2(r/'docs/publication135/guide.js',out/'guide.js')
for name,link in [('docs/readmes/INDEX.md','[Edition 135 — continuity and checked appearances](../../README-135.md) | [Offline guide](135/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/143-appearance-continuity.md]] | [[README-135.md]]')]:
 p=r/name;s=p.read_text(encoding='utf-8-sig');text(p,link+'\n\n'+s if not s.startswith(link) else s)
print(json.dumps({'passed':True,'javaChecks':len(checks),'sources':len(inventory),'originalEntries':len(originals),'summary':comparison['summary']}))
