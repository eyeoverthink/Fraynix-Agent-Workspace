from pathlib import Path
import subprocess,os,json,time
r=Path.cwd();env=os.environ.copy();env['JAVA_HOME']='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot';env['PYTHONUTF8']='1';env.pop('JAVA_TOOL_OPTIONS',None)
for name,args in [('public-evidence149-test-serial',['python','-X','utf8','docs/publication149/public_evidence149.py']),('preflight149-serial',['python','-X','utf8','docs/publication149/test-preflight149.py']),('recorder149-selftest-serial',['node','workspace-desktop/test/webcam149-recorder.cjs','--self-test'])]:
 p=r/'build'/f'{name}.log';assert not p.exists(),'Preserve previous attempts'
 print(json.dumps({'started':name}),flush=True);start=time.monotonic()
 with p.open('wb') as out:result=subprocess.run(args,cwd=r,env=env,stdout=out,stderr=subprocess.STDOUT)
 record={'name':name,'exit':result.returncode,'seconds':time.monotonic()-start};(r/'build'/f'{name}.json').write_bytes((json.dumps(record,indent=2)+'\n').encode());print(json.dumps(record),flush=True)
 if result.returncode:raise SystemExit(result.returncode)
subprocess.run(['python','-X','utf8','docs/publication149/freeze-camera149.py'],check=True,env=env)
