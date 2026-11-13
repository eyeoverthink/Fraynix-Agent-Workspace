from pathlib import Path
import json,subprocess,hashlib,time,sys
r=Path.cwd();folder=r/'build/diagnostic131';media=r/'build/footage131-held'
lock=json.loads((r/'docs/OBSERVATION_131_LOCK.json').read_text())
for n,h in lock['sources'].items():assert hashlib.sha256((r/n).read_bytes().replace(b'\r\n',b'\n')).hexdigest()==h,n
java='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot/bin/java.exe'
old='S:/Codex-Work/fraynix-workspace-edition130'
cp='workspace-engine/build/classes/java/test;workspace-engine/build/classes/java/main;'+old+'/workspace-engine/build/install/workspace-engine/lib/*'
original=old+'/workspace-engine/build/classes/java/test;'+old+'/workspace-engine/build/install/workspace-engine/lib/*'
for n in lock['heldStarts']:
 for method in ['original130-flow']+lock['methods']:
  out=folder/f'held-{n}-{method}.json';receipt=folder/f'held-{n}-{method}.execution.json'
  assert not out.exists() and not receipt.exists(),'Preserve the original run'
  start=time.monotonic();args=[java,'-Xmx512m','-XX:ActiveProcessorCount=2','-cp',original if method=='original130-flow' else cp,'fraymus.workspace.ObservationStudy130' if method=='original130-flow' else 'fraymus.workspace.ObservationStudy131',str(media/f'held-{n}.fobs.json'),str(media/'geometry.json'),str(media/f'held-{n}.references.json'),str(out),'flow' if method=='original130-flow' else method]
  with (folder/f'held-{n}-{method}.log').open('wb') as log:p=subprocess.run(args,stdout=log,stderr=subprocess.STDOUT)
  v={'start':n,'method':method,'exit':p.returncode,'wallSeconds':time.monotonic()-start,'partialTraceInvented':False};receipt.write_text(json.dumps(v,indent=2));print(json.dumps(v),flush=True)
  if p.returncode:continue
  audit=folder/f'audit-held-{n}-{method}.json';subprocess.run([java,'-Xmx512m','-XX:ActiveProcessorCount=2','-cp',cp,'fraymus.workspace.AuditStudy130',str(out),str(audit)],check=True,stdout=subprocess.DEVNULL)
