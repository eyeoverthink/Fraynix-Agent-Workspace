from pathlib import Path
import json,hashlib,subprocess,statistics,time
r=Path.cwd();read=lambda p:json.loads((r/p).read_text(encoding='utf8'));sha=lambda b:hashlib.sha256(b).hexdigest()
baseline=Path('S:/Codex-Work/fraynix-workspace-edition136')
old=read('docs/readmes/136/verification.json')['buildReceipt']
for artifact in old['files']['artifacts']:assert sha((baseline/artifact['path']).read_bytes())==artifact['sha256'],artifact['path']
subprocess.run(['node','scripts/Study-Build095.cjs'],check=True)
folder=r/'build/cost137-candidate-final';assert not folder.exists(),'Preserve earlier attempts'
start=time.monotonic()
subprocess.run(['C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot/bin/java.exe','-Xmx768m','-XX:ActiveProcessorCount=2','-cp','workspace-engine/build/classes/java/test;workspace-engine/build/install/workspace-engine/lib/*','fraymus.workspace.ObservationCost137Study',str(baseline.parent/'fraynix-workspace-edition135/build/appearance135-app/project'),str(folder)],check=True)
a=read('build/cost137-baseline/result.json');b=read('build/cost137-candidate-final/result.json');assert a['originalStateSha256']==b['originalStateSha256'];rows=[]
for x,y in zip(a['rows'],b['rows']):
 assert x['seedSha256']==y['seedSha256']
 assert [(v['operation'],v['repeat'],v['resultSha256']) for v in x['reads']]==[(v['operation'],v['repeat'],v['resultSha256']) for v in y['reads']]
 assert [(v['resultSha256'],v['persistedSha256']) for v in x['writes']]==[(v['resultSha256'],v['persistedSha256']) for v in y['writes']]
 rows.append({'runs':x['runs'],'bytes':x['seedBytes'],'coldOpenMs':[x['coldOpenMs'],y['coldOpenMs']],'writeMedianMs':[statistics.median(v['ms'] for v in z['writes']) for z in [x,y]],'operations':{op:[statistics.median(v['ms'] for v in z['reads'] if v['operation']==op) for z in [x,y]] for op in ['summary','run','dataset','image']},'candidateChecks':y['verificationCost']})
result={'all36ReadHashesEqual':True,'all9WriteAndPersistedHashesEqual':True,'rows':rows,'baselineBuild':old,'candidateBuild':read('build/study095-build.json'),'finalBuildProbeSeconds':time.monotonic()-start,'scope':'Same D drive, published136 baseline and final137 binaries. One cold open and three repeats each; transaction probes are not complete task timings. Initial pre-final-build comparison retained separately; final repeat justified by the added end-of-run reference recheck, not selecting the fastest result.'}
(r/'build/cost137-comparison-initial.json').write_bytes((r/'build/cost137-comparison.json').read_bytes())
(r/'build/cost137-comparison.json').write_text(json.dumps(result,indent=2),encoding='utf8');print(json.dumps({'passed':True,'rows':rows}))
