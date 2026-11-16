"""Owned jobs run serially, with exact logs and no retries that hide failed attempts."""
from pathlib import Path
import os,sys,subprocess,time,json
r=Path.cwd();env=os.environ.copy();env['JAVA_HOME']='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot';env['PYTHONUTF8']='1';env['RETURN149_GROUP']='black-lighter-movement';env.pop('JAVA_TOOL_OPTIONS',None)
stages={
 'app':[
  ('focused149-revision2',['cmd','/c','gradlew.bat',':workspace-engine:test','--tests','fraymus.workspace.IdentityReturn149Test','--tests','fraymus.workspace.IdentityReserved149Test',':workspace-engine:installDist','--console=plain','--max-workers=2']),
  ('stamp149',['node','scripts/Study-Build095.cjs','--stamp']),
  ('app149',['node','workspace-desktop/test/identity149-integration.cjs'])],
 'remaining':[
  ('restart149',['node','docs/publication149/restart149.cjs']),
  ('capture149',['node','docs/publication149/capture149.cjs']),
  ('java149',['python','-X','utf8','docs/publication149/validate149.py','java']),
  ('desktop149',['python','-X','utf8','docs/publication149/validate149.py','desktop']),
  ('package149',['python','-X','utf8','docs/publication149/validate149.py','package'])]
}
for name,args in stages[sys.argv[1]]:
 p=r/'build'/f'{name}-serial.log';assert not p.exists(),f'Preserve prior attempt {p}'
 print(json.dumps({'started':name}),flush=True);start=time.monotonic()
 with p.open('wb') as out:result=subprocess.run(args,cwd=r,env=env,stdout=out,stderr=subprocess.STDOUT)
 receipt={'name':name,'exit':result.returncode,'seconds':time.monotonic()-start};(r/'build'/f'{name}-serial.json').write_bytes((json.dumps(receipt,indent=2)+'\n').encode());print(json.dumps(receipt),flush=True)
 if result.returncode:raise SystemExit(result.returncode)
