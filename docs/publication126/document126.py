"""Publish actual study originals and derive every result from retained records."""
from pathlib import Path
import json,hashlib,subprocess,shutil,gzip,xml.etree.ElementTree as ET
r=Path.cwd();out=r/'docs/readmes/126';out.mkdir(parents=True,exist_ok=True)
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-126']).returncode==1
def read(p):return json.loads((r/p).read_text(encoding='utf8'))
def text(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8')
def write(p,v):text(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
def sha(b):return hashlib.sha256(b).hexdigest()
broot=r/'build/browser126-passed';mroot=r/'build/model126-complete'
b=read(broot/'result.json');m=read(mroot/'result.json')
assert b['passed'] and m['passed']
assert len(b['rows'])==3 and len(m['requests'])==3
write(out/'browser-study.json',b);write(out/'model-study.json',m)
shots=[]
for folder,data in [(broot,b),(mroot,m)]:
 for name in dict.fromkeys(data['captures']):
  if name=='failure.png':continue
  shutil.copy2(folder/name,out/name);shots.append(name)
assert read('build/browser126-history-review.json')['passed']
shutil.copy2(broot/'browser-recovery-history.png',out/'browser-recovery-history.png');shots.append('browser-recovery-history.png')
assert read('build/witness126-proof.json')['passed']
assert read('build/independent126-world.json')['passed']
shutil.copy2(r/'build/independent126-world.json',out/'independent-world.json')
checks=set();suites=[]
for folder in sorted((r/'build/validation126').iterdir()):
 if not folder.is_dir() or folder.name=='first':continue
 count=0
 for p in folder.glob('*.xml'):
  e=ET.parse(p).getroot();assert int(e.get('failures','0'))+int(e.get('errors','0'))==0,p
  for case in e.findall('testcase'):
   if case.find('skipped') is None:checks.add(case.get('classname')+'.'+case.get('name'));count+=1
 suites.append({'name':folder.name,'executions':count})
assert any(s['name']=='final' for s in suites)
desktop=(r/'build/desktop126-final.log').read_text(encoding='utf8');assert 'tests 161' in desktop and 'fail 0' in desktop
for name in ['kernel126-final','java126-final','desktop126-final','agency126-regression','launcher126','package126','receipt126']:
 assert read('build/'+name+'.json')['exit']==0
kernel=(r/'build/kernel126-final.log').read_text(encoding='utf8')
for name in ['JournalContinuityTest','LocalSkillsTest','FactoryKernelIntegrationTest']:assert name+' passed' in kernel
rows=[]
for x in b['rows']:
 assert x['task']['outcome']=='verified' and x['historical']['passed']
 rows.append({'task':'Browser '+x['name'],'kind':'browser','seconds':x['elapsedMs']/1000,'modelCalls':x['task']['timing052']['modelCalls'],'recovered':x['recovery'],'outcome':'verified','scope':x.get('timingScope','Complete caller span; later historical checks excluded.'),'resumedCallerSeconds':x.get('resumedCallerMs',0)/1000})
for x in m['requests']:
 assert x['task']['outcome']=='verified'
 calls=x['task']['timing052']['modelCalls'] if x['kind']!='creation' else len([j for j in x['generation'] if j.get('model',{}).get('digest')])
 rows.append({'task':x['kind'],'kind':'creation' if x['kind']=='creation' else 'world','seconds':x['elapsedMs']/1000,'modelCalls':calls,'recovered':False,'outcome':'verified','scope':'Complete chat caller span; creation counts actual contract/code worker calls separately from Agency planning.'})
baselines=[{'task':'Direct browser '+x['kind'],'seconds':x['elapsedMs']/1000,'scope':x['scope']} for x in b['baselines']]+[{'task':x['kind'],'seconds':x['elapsedMs']/1000,'scope':'Direct equivalent world task; same native verifier; independent recheck excluded.'} for x in m['direct']]+[{'task':'Replay generated implementation','seconds':m['replay']['elapsedMs']/1000,'scope':m['replay']['scope']}]
measurements={'rows':rows,'baselines':baselines,'scope':'Individual serial measurements, not statistical intervals. Baselines have explicitly different admission/generation scope. Setup and independent rechecks are not attributed to model work.'};write(out/'measurements.json',measurements)
v={'passed':True,'javaChecks':sorted(checks),'javaSuites':suites,'desktopChecks':161,'kernelHarnesses':['JournalContinuityTest','LocalSkillsTest','FactoryKernelIntegrationTest'],'actualChatRequests':6,'actualAbruptRecovery':True,'worldIndependentChecks':True,'creationIndependentChecks':True,'browserIndependentChecks':True,'model':b['modelCatalog'],'measurements':measurements,'launcher':{'passed':True,'scope':'Isolated offline preparation and package/build receipt.'},'sourceScopes':{'production':'a381d5a6','browserOriginal':b['source'],'browserContinuation':b.get('continuation',{}),'worldCreation':m['source'],'worldCreationContinuation':m.get('continuation',{}),'scope':'A/B completed on46e21739. 14ef2cb0 restores unchanged-read limits and supplies current references. Its forced crash exposed a complete pending witness append. f4ebdbd5 adds exact verified append recovery, then resumes that SAME original task. That model repeated submission and was cancelled. ab01c83d checks original outcomes after every mutation and stops at the first passing check; final new recovery and world/creation run onab01c83d. Finala381d5a6 changes only two List indexing calls for the kernel module Java17 compatibility; final kernel/Java/package checks and historical UI use that build. No model outcome is relabelled as a new run. Current context is not claimed for older calls. Cancelled recovery and corrected drivers remain separate originals.'},'scope':'Supported labelled browser/world/creation tasks. No arbitrary browsing, new physical generalization, automatic neural training or automatic procedure adaptation claim.'};write(out/'verification.json',v)
originals=[];seen=set()
def preserve(p,logical):
 if not p.is_file() or logical in seen:return
 seen.add(logical);raw=p.read_bytes();key=sha(raw);dest=out/'originals'/(key+'.gz');packed=gzip.compress(raw,mtime=0);dest.parent.mkdir(exist_ok=True)
 if not dest.exists():dest.write_bytes(packed)
 originals.append({'logicalPath':logical,'path':dest.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(raw),'rawSha256':key})
for pattern in ['browser126-*','model126-*','agency126-*','validation126','witness126-crash-original']:
 for folder in sorted((r/'build').glob(pattern)):
  if not folder.is_dir():continue
  for p in sorted(folder.rglob('*')):
   if any(x in p.parts for x in ['desktop-profile','node_modules']):continue
   if p.is_file() and p.suffix in ['.json','.jsonl','.xml','.png','.txt','.log','.html','.css','.js','.cjs','.pending','.evidence','.before','.fworld','.previous','.csv','.tsv']:
    preserve(p,p.relative_to(r).as_posix())
for p in sorted((r/'build').glob('*126*')):
 if p.is_file():preserve(p,p.relative_to(r).as_posix())
write(out/'original-evidence.json',{'files':originals,'scope':'Exact synthetic test-project originals, actual native proofs, model receipts, screenshots, first failed unit run and all failed/continued drivers. Desktop profiles and private pre-existing projects excluded.'})
sourcePaths={x['path'] for x in read('docs/readmes/125/file-inventory.json')}
for n in subprocess.check_output(['git','diff','--name-only','246c70d2','HEAD'],text=True).splitlines():
 if n.startswith(('workspace-engine/','workspace-desktop/','src/','assets/','.github/')) or n=='build.gradle':sourcePaths.add(n)
sourcePaths.update(['docs/EDITION_126_CONTRACT.md','docs/INTEGRATED_AGENT_ROADMAP.md']);inventory=[]
for n in sorted(sourcePaths):
 raw=(r/n).read_bytes()
 try:raw=raw.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':n,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
write(out/'file-inventory.json',inventory)
failed=[]
for folder in sorted((r/'build').glob('browser126-*')):
 p=folder/'result.json'
 if p.is_file():
  d=read(p)
  if not d.get('passed'):failed.append({'attempt':folder.name,'error':d.get('error'),'scope':'Retained original failure, not counted as a successful final request.'})
write(out/'failures.json',failed)
resultText='Six actual chat requests completed with independently checked results: three browser requests (one interrupted and resumed), two persistent-world arrangements and one generated calculator. After restart, all browser proofs were reverified, both committed world frames were compared and the saved calculator procedure was retained and replayed. The two world baselines and the generated implementation replay also passed their stated independent checks.'
timingText='| Actual request | Full seconds | Real model calls | Outcome |\n| --- | ---: | ---: | --- |\n'+'\n'.join(f"| {x['task']} | {x['seconds']:.2f} | {x['modelCalls']} | {x['outcome']}{' after recovery' if x['recovered'] else ''} |" for x in rows)+'\n\n| Explicit baseline | Seconds | Scope |\n| --- | ---: | --- |\n'+'\n'.join(f"| {x['task']} | {x['seconds']:.2f} | {x['scope']} |" for x in baselines)
timingText+='\n\nThe final recovery span includes its actual restart. A separate earlier attempt required an engineering repair to the witness journal and is retained as an unsuccessful attempt; its investigation time is not relabelled as the final task duration.'
failureText='- The first unit pass exposed an objective-binding test mistake and the missing strict browser-episode completion path; both were corrected and covered by later checks.\n- The first browser driver had invalid fixture JavaScript and stopped before inference. The second exposed a browser admission mismatch with the existing creation/workflow route.\n- The third model attempt performed the correct submission in four real calls, but journal event naming rejected completion; it remains an interrupted result. A real continuing-runtime regression now covers that path.\n- The fourth attempt completed and verified the first request, then its driver tried to click a control inside collapsed settings. The continuation reverified that saved request in the same project and continued; its original clock and failure remain intact.\n- The first recovery attempt was cancelled after611.23 seconds: one stale-reference proposal was refused, then unchanged observations repeated. The new browser branch had bypassed the existing repeated-read restriction. The correction restores it and exposes only the current actionable page references, leaving all older evidence in the journal. The next recovery attempt used14ef2cb0 and exposed the pending append described below; original A/B results remain explicitly scoped to46e21739. Final successful recovery usedab01c83d.\n- The next forced crash landed during an atomic witness append. Startup correctly refused the pending file. A new recovery path verifies one complete append against the exact committed prefix and owner evidence, preserves both byte sequences and atomically completes it. Partial, ambiguous, changed-prefix and invalid-owner candidates still refuse. The SAME original browser task then resumed, but the model submitted the form three times and was cancelled. That failed attempt retains its wall span and separate resume segment. The final change checks the original outcome after every mutation and ends the task at the first passing check, preventing a later redundant model action. It does not substitute scripted actions for model choices. A test-source punctuation compile error was also retained and corrected before passing checks.\n- The initial independent calculator driver did not await completion of its Open request; its first integer assertion failed, consistent with a navigation race. The same generated files were byte-compared after restart and rechecked after awaiting the actual Open request. All11 independent steps then passed, including decimal/reset, and the unchanged implementation passed again on a new artifact. No generated code was edited or model request replayed to replace the failure.'
failureText+='\n- The broad kernel command originally selected a deliberately skipped task; its subsequent missing-report copy failed. The executable kernel verification harness was then run successfully. The older desktop regression driver also needed to expand the current settings panel before its legacy controls; all10 original behavioral checks then passed. Original logs and drivers are retained.'
validationText=f"Local checks passed: {len(checks)} distinct Java methods across retained successful invocations, 161 desktop checks, actual browser integration regression, isolated offline preparation and package receipt. The six final real-model requests include abrupt owned-process recovery, cold reopening, independently rendered calculator checks and original native world proofs. Initial failed runs are separately indexed. Hosted CI status is reported in the publication receipt, not inferred from these results."
md=(r/'docs/publication126/README-template.md').read_text(encoding='utf8')
for key,value in {'RESULTS':resultText,'TIMINGS':timingText,'FAILURES':failureText,'VALIDATION':validationText}.items():md=md.replace('{{'+key+'}}',value)
assert '{{' not in md;text(r/'README-126.md',md);text(r/'README.md',md)
note='docs/obsidian/134-real-model-workspace.md'
modules=[{'id':i,'name':n,'source':s,'note':note} for i,n,s in [('java','Java task owner','workspace-engine/src/main/java/fraymus/workspace/Agency043.java'),('proof','Frozen browser criteria','workspace-engine/src/main/java/fraymus/workspace/BrowserOutcome126.java'),('host','Independent DOM snapshot','workspace-desktop/browser-outcome126.cjs'),('world','Actual world and creation study','workspace-desktop/test/model126-integration.cjs'),('recovery','Browser recovery study','workspace-desktop/test/browser126-integration.cjs')]]
system={'modules':modules,'screenshots':[{'path':'docs/readmes/126/'+name,'title':name.removesuffix('.png').replace('-',' ')} for name in shots],'evidence':[{'path':'docs/readmes/126/'+name} for name in ['verification.json','browser-study.json','model-study.json','measurements.json','independent-world.json','original-evidence.json']],'measurements':measurements};write(out/'system-map.json',system)
text(r/note,'# Real local-model workspace — edition126\n\n[[README-126.md]]\n\n'+resultText+'\n\n'+validationText+'\n\nJava owns continuity and completion; the model chooses admitted actions. Browser criteria stay frozen, native motion remains independently checked, and created code must actually work. Historical evidence is dated. Next: [[docs/INTEGRATED_AGENT_ROADMAP.md]] — procedure lifecycle, observation-to-artifact work, measured adaptation and daily use.\n')
write(r/'docs/obsidian/134-real-model-workspace.canvas',{'nodes':[{'id':x['id'],'type':'file','file':x['source'],'x':i%3*430,'y':i//3*250,'width':390,'height':190} for i,x in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']} for i in range(len(modules)-1)]})
html=(r/'docs/publication126/guide-template.html').read_text(encoding='utf8')+'<script id="system-data" type="application/json">'+json.dumps(system).replace('</','<\\/')+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory).replace('</','<\\/')+'</script><script src="guide.js"></script></body></html>'
text(out/'index.html',html);shutil.copy2(r/'docs/publication126/guide.js',out/'guide.js')
for name,link in [('docs/readmes/INDEX.md','[Edition126 — real local-model workspace](../../README-126.md) · [Offline guide](126/index.html)'),('docs/obsidian/00-home.md','Current: [[docs/obsidian/134-real-model-workspace.md]] · [[README-126.md]]')]:
 p=r/name;s=p.read_text(encoding='utf-8-sig');text(p,link+'\n\n'+s if not s.startswith(link) else s)
print(json.dumps({'passed':True,'javaChecks':len(checks),'sources':len(inventory),'originalEntries':len(originals),'measurements':measurements}))
