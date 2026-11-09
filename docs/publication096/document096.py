from pathlib import Path
import json,hashlib,gzip,subprocess,shutil,xml.etree.ElementTree as ET,re,os
r=Path.cwd();d=r/'docs/readmes/096'
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def write(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8',newline='\n')
def js(p,v):write(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
def sha(b):return hashlib.sha256(b).hexdigest()
def tests(folder):
 rows=[]
 for p in sorted(folder.rglob('TEST-*.xml')):
  x=ET.parse(p).getroot();rows.append({'name':x.attrib['name'],'tests':int(x.attrib['tests']),'failed':int(x.attrib.get('failures',0))+int(x.attrib.get('errors',0)),'skipped':int(x.attrib.get('skipped',0)),'seconds':float(x.attrib['time']),'file':p.relative_to(r).as_posix(),'skips':[dict(c.attrib,reason=(c.find('skipped').get('message') or (c.find('skipped').text or '').strip() or ('Optional pre-existing Electron recovery project not supplied in this fresh default lifecycle; see the original test assumption.' if x.attrib['name'].endswith(('RecoveryAudit093Test','RecoveryAudit094Test')) else 'JUnit XML did not include a reason; inspect the original test source.'))) for c in x.findall('testcase') if c.find('skipped') is not None]})
 return {'classes':rows,'tests':sum(x['tests'] for x in rows),'failures':sum(x['failed'] for x in rows),'skipped':sum(x['skipped'] for x in rows),'passed':sum(x['tests']-x['failed']-x['skipped'] for x in rows)}
finish=load(r/'build/full096-finished.json');assert finish['exit']==1
coverage=load(r/'build/coverage096.json');assert coverage['passed'] and not coverage['singleFullInvocationPassed']
original=tests(r/'build/full096-results');assert original['failures']==4
full={k:coverage[k] for k in ['classes','tests','failures','skipped']};full['passed']=coverage['passedMethods'];assert full['tests']>735 and full['failures']==0
packaging=load(r/'build/packaging096-finished.json');assert packaging['exit']==0
zipcheck=load(r/'build/zip096.json');assert zipcheck['passed'];packaging['archive']=zipcheck;packaging['packageKind']='Original Java Studio ZIP with full documentation history; the integrated workspace runs from the checkout and installed Java engine, not a standalone desktop installer.'
focused=tests(r/'build/focused096-results');assert focused['tests']==25 and not focused['failures']
attempts=load(r/'build/ui096-attempts.json');appdir=r/attempts['accepted'];ui=load(appdir/'result.json');assert ui['passed'] and not ui['errors']
for attempt in attempts['attempts']:
 record=load(r/attempt['directory']/'result.json');attempt.update({'passed':bool(record.get('passed')),'source':record['source']})
assert all(not a['passed'] for a in attempts['attempts'][:-1]) and attempts['attempts'][-1]['passed']
uiaudit=load(r/'build/ui096-audit.json');assert uiaudit['passed'] and uiaudit['source']==ui['source']
desktop=(r/'build/desktop096.log').read_text(encoding='utf-8-sig');assert re.search(r'pass\s+135\b',desktop) and re.search(r'fail\s+0\b',desktop)
heap=[load(r/f'build/heap096-summary{x}.json') for x in [512,1024]]
run=load(r/'build/full096-run.json');assert run['freshNative'] and run['resumeVariables']==[]
result={'passed':True,'source':run['source'],'fullLifecycle':{'run':run,'finish':finish,'tests':original},'methodCoverage':full,'correctionEvidence':coverage,'packaging':packaging,'focused':focused,'desktopPassed':135,'heapDiagnostics':heap,'app':{'attemptHistory':attempts,'source':ui['source'],'checks':uiaudit['checks'],'nativeAudit':uiaudit,'captures':ui['captures'],'scope':ui['scope']},'limits':['The original complete invocation failed four test-boundary assertions. Source-qualified corrected checks complete method coverage; this is not one clean full invocation or a remote CI result.','Fresh software/native regressions, not new physical measurements or a general intelligence claim.','The512/1024MiB diagnosis reused complete historical071/072 records; fresh native studies are identified separately in the original complete invocation.','The1024MiB setting applies to isolated test JVMs only; the app walkthrough still uses512MiB/two processors. GC snapshots are not instantaneous peak RSS.','No language-model inference, new trained policy or warm growing-bank payoff is claimed.']}
identity=load(r/'build/source-comparison096.json');assert identity['identical']==333 and identity['changed']==['fraymus/workspace/Tools044.class'];result['productionClassComparison']=identity
js(d/'verification.json',result)
archive=[];unique={}
def pack(p,label):
 p=Path(p);raw=(Path('\\\\?\\'+str(p.resolve())) if os.name=='nt' else p).read_bytes();h=sha(raw)
 if h not in unique:
  z=gzip.compress(raw,mtime=0);dest=d/'original'/(h+'.gz');dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(z);unique[h]={'path':dest.relative_to(r).as_posix(),'bytes':len(z),'sha256':sha(z),'rawBytes':len(raw),'rawSha256':h}
 archive.append(dict(unique[h],logicalPath=label))
for folder in ['baseline096-assertions','focused096-results','heap096-results512','heap096-results1024','full096-results','corrected096-results']:
 for p in (r/'build'/folder).rglob('*'):
  if p.is_file() and p.suffix in ['.xml','.log']:pack(p,folder+'/'+p.relative_to(r/'build'/folder).as_posix())
for p in (r/'build').glob('*096*'):
 if p.is_file() and p.suffix in ['.json','.log','.ps1','.py','.md','.diff'] and p.name not in ['document096.log']:pack(p,'validation/'+p.name)
for p in (r/'build/heap096').rglob('*'):
 if p.is_file() and p.suffix in ['.json','.fworld','.jsonl']:pack(p,'diagnostic-copies/'+p.relative_to(r/'build/heap096').as_posix())
# Only the declared fresh native studies and historical world sequences are published as native originals.
# The remaining full-suite XML/logs are published; all temporary projects remain locally preserved.
roots=[]
for x in full['classes']:
 if x['name'].split('.')[-1] not in ['UncertainNative071Test','UsefulNative072Test','WorldReservedTest']:continue
 text=(r/x['file']).read_text(encoding='utf8')
 for root in re.findall(r'(?:EDITION07[12]_NATIVE_ROOT=|WORLD096_HISTORICAL035_REGRESSION )([^\r\n<]+)',text):
  p=Path(root.strip());assert p.is_relative_to(r/'build/full096-native');roots.append(p)
assert len(roots)==3
fresh=[]
for root in roots:
 if root.name.startswith(('fraynix-uncertain071-','fraynix-useful072-')):
  record=load(root/'original-experiment.json');expected=(546,504) if 'uncertain071' in root.name else (666,612)
  assert record['status']=='completed' and (record['progress'],record['nativeExecutions'])==expected
  fresh.append({'project':root.name,'kind':record['kind'],'progress':record['progress'],'nativeExecutions':record['nativeExecutions'],'status':record['status'],'source':run['source']})
result['freshStudies']=fresh;js(d/'verification.json',result)
for root in roots:
 for p in root.rglob('*'):
  if p.is_file() and p.suffix in ['.json','.fworld','.jsonl']:pack(p,'fresh-native/'+root.name+'/'+p.relative_to(root).as_posix())
for attempt in attempts['attempts']:
 base=r/attempt['directory']
 for p in base.rglob('*'):
  if p.is_file() and not {'desktop-profile'}.intersection(p.parts) and p.name!='host.json' and p.suffix in ['.json','.jsonl','.fworld','.pending','.html','.css','.js','.csv','.log','.png']:pack(p,'app/'+base.name+'/'+p.relative_to(base).as_posix())
for c in ui['captures']:shutil.copyfile(appdir/c['file'],d/c['file'])
js(d/'original-evidence.json',{'files':archive,'distinctNewPayloads':len(unique),'scope':'Exact failed baseline/heap diagnostics, copied-record provenance, full test XML/logs, fresh071/072 and historical035 native originals, and both original failed and corrected isolated app attempts. Other full-suite temporary projects are preserved locally; not every temporary file is archived. No private user project, webcam footage or desktop credential profile.'})
prior=load(r/'docs/readmes/095/file-inventory.json');kinds={x['path']:x['hashKind'] for x in prior};paths=set(kinds);paths.update(['docs/WORLD_035_CANDIDATE_LOCK.json','docs/WORLD_035_FROZEN_CONTRACT.json','docs/WORLD_056_REGRESSION_LOCK.json','docs/WORLD_096_REGRESSION_LOCK.json','docs/EDITION_096_CONTRACT.md','docs/EDITION_096_VALIDATION_UPDATE.md'])
paths.update(x for x in subprocess.check_output(['git','diff','--name-only','--diff-filter=A','55d95f472133ff412a1940613e5bc0f9c179ad59','HEAD'],text=True).splitlines() if x.startswith(('workspace-engine/','workspace-desktop/','scripts/','fraynix-modules/')))
inventory=[]
for p in sorted(paths):
 b=(r/p).read_bytes();kind=kinds.get(p,'utf8-lf');b=b.decode('utf8').replace('\r\n','\n').encode() if kind=='utf8-lf' else b;inventory.append({'path':p,'bytes':len(b),'sha256':sha(b),'hashKind':kind})
js(d/'file-inventory.json',inventory);write(d/'FILE_TREE.txt','Edition096 canonical sources\n'+'\n'.join(sorted(paths))+'\n')
note='docs/obsidian/104-full-verification.md'
modules=[{'id':'schema','name':'Java-owned construction tool choices','source':'workspace-engine/src/main/java/fraymus/workspace/Tools044.java','note':note},{'id':'lock','name':'Explicit historical source chain','source':'workspace-engine/src/test/java/fraymus/workspace/WorldRegression096.java','note':note},{'id':'resources','name':'Bounded test processes','source':'workspace-engine/build.gradle','note':note},{'id':'app','name':'Actual review, execution and restart','source':'workspace-desktop/test/verification096-integration.cjs','note':note}]
system={'modules':modules,'screenshots':[{'path':'docs/readmes/096/'+c['file'],'caption':'Actual isolated096 app: '+c['label']} for c in ui['captures']],'evidence':[{'path':'docs/readmes/096/verification.json','caption':'Results and limits'},{'path':'docs/readmes/096/original-evidence.json','caption':'Retained original bytes'}]};js(d/'system-map.json',system)
html=(r/'build/guide096.html').read_text(encoding='utf8').replace('__RESULT__',json.dumps(result,separators=(',',':')).replace('</','<\\/'))
extra=''.join('<script id="'+name+'" type="application/json">'+json.dumps(value,separators=(',',':')).replace('</','<\\/')+'</script>' for name,value in [('system-data',system),('inventory-data',inventory)])
write(d/'index.html',html.replace('</body>',extra+'</body>'))
readme=(r/'build/README096-template.md').read_text(encoding='utf8')
for k,v in {'passed':full['passed'],'skipped':full['skipped'],'total':full['tests'],'classes':len(full['classes']),'source':run['source'],'correction_source':coverage['correctionRun']['source'],'correction_count':sum(x['tests'] for x in coverage['followupClasses']),'ui_source':ui['source'],'archive_gib':round(zipcheck['bytes']/1024**3,2),'finished':packaging['finishedAt']}.items():readme=readme.replace('{'+k+'}',str(v))
write(r/'README-096.md',readme)
finding=f"Combined Java method coverage: {full['passed']} passed, {full['skipped']} skipped, no remaining failures across {len(full['classes'])} JUnit classes. The original complete invocation failed four test-boundary assertions; corrected checks passed separately with all500 compared engine/Studio classes unchanged. This is not a single clean full invocation. Factory custom verification and separate source-isolation/distribution checks passed. Fresh071/072 native studies are distinct from copied512/1024MiB diagnostics.25 initial focused methods and135 desktop tests pass. Actual app reviews three declared alternatives; one direct-first trial succeeds and two are not needed, then it explicitly executes one verified construction, retains a skill and survives two cold restarts."
nextstep='Next: declare a continuing warm-workspace mixed-task protocol with a growing verified bank, matched controls and complete costs. No warm payoff or new LLM result is claimed here.'
continuity='## Edition096 — locally verified; publication pending\n\n'+finding+'\n\nSource '+run['source']+'. Only Tools044 production Java source changed: Java-owned design/review fields are hidden from model arguments. The full distribution uses ZIP64 to retain its growing evidence history without exclusions. Explicit096 source extension preserves original035/056 contracts and outcome assertions. Test JVMs1024MiB, one class per fork and one fork at a time; production limits unchanged. Original failures retained. Frozen001–095, live045/qa046 and private data preserved; no subagents. '+nextstep+'\n\n'
for name in ['AGENTS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md']:write(r/name,continuity+(r/name).read_text(encoding='utf8'))
root=(r/'README.md').read_text(encoding='utf8').replace('**Latest: [edition095','**Earlier edition: [edition095',1);root=root.replace('# Fraynix Agent Workspace\n','# Fraynix Agent Workspace\n\n**Latest: [edition096 — restoring the full verification path](README-096.md).**\n\n'+finding+' [Offline verification explorer](docs/readmes/096/index.html).\n',1);write(r/'README.md',root)
write(r/note,'# Edition096 — complete verification\n\n[[README-096.md|Full edition]] · [[docs/readmes/096/index.html|Offline explorer]] · [[docs/obsidian/103-mixed-task-costs.md|Prior edition]]\n\n'+finding+'\n\n'+nextstep+'\n')
nodes=[{'id':m['id'],'type':'file','file':m['source'],'x':i*390,'y':0,'width':350,'height':260} for i,m in enumerate(modules)];nodes.append({'id':'evidence','type':'file','file':'README-096.md','x':390,'y':330,'width':740,'height':340});js(r/'docs/obsidian/104-full-verification.canvas',{'nodes':nodes,'edges':[{'id':'e'+str(i),'fromNode':modules[i]['id'],'fromSide':'right','toNode':modules[i+1]['id'],'toSide':'left'} for i in range(3)]})
for name,prefix in [('docs/ARCHITECTURE.md','Edition096 restores the full verification path; production motion and original outcomes are unchanged. [Results](../README-096.md).\n\n'),('docs/obsidian/00-home.md','[[README-096.md|Edition096 — full verification]] · [[docs/obsidian/104-full-verification.md]]\n\n'),('docs/readmes/INDEX.md','[Edition096 — full verification](../../README-096.md) · [Offline evidence](096/index.html)\n\n')]:write(r/name,prefix+(r/name).read_text(encoding='utf8'))
p=r/'README-CONTRIBUTORS.md';s=p.read_text(encoding='utf8').replace('For edition095, the isolated source checkout is `D:/Codex-Work/fraynix-workspace-edition095`, branch `edition095`, based on verified094 main `88b60a6dd6865c0e9d83115401f93a03d6c4ac14`.','For edition096, the isolated source checkout is `D:/Codex-Work/fraynix-workspace-edition096`, branch `edition096`, based on verified095 main `55d95f472133ff412a1940613e5bc0f9c179ad59`.').replace('(docs/readmes/095/file-inventory.json)','(docs/readmes/096/file-inventory.json)').replace('The next priorities are resolving the four inherited full-CI failures with evidence, then continuing warm-workspace sequences with changing memory contents and matched cost coverage.','Edition096 repairs four inherited CI failures. The original full invocation found four test-boundary assertions, corrected separately with all production classes unchanged; combined method coverage has no remaining failures. Next are continuing warm-workspace sequences with changing memory contents and matched cost coverage; a local pass does not predeclare a remote CI result.');s=s.replace('| Nov 8 | 095 |','| Nov 8 | 096 | Source-qualified complete method coverage, explicit historical source chain, bounded test resources and actual construction restart. |\n| Nov 8 | 095 |');write(p,s)
print(json.dumps({'full':{k:v for k,v in full.items() if k!='classes'},'archives':len(archive),'sources':len(inventory)},indent=2))
