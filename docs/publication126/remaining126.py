from pathlib import Path
import json,subprocess,os,time
r=Path.cwd();assert json.loads((r/'build/browser126-passed/result.json').read_text(encoding='utf8'))['passed']
record={'startedAt':time.time(),'steps':[],'passed':False};env=os.environ.copy();env['MODEL126_OUTPUT']='build/model126-first'
try:
 for name,args in [('model126-first',['node','workspace-desktop/test/model126-integration.cjs']),('validation126-java',['python','docs/publication126/validate126.py','java']),('validation126-desktop',['python','docs/publication126/validate126.py','desktop']),('validation126-package',['python','docs/publication126/validate126.py','package'])]:
  start=time.monotonic();print('Starting '+name,flush=True)
  with (r/'build'/f'{name}.log').open('wb') as out:p=subprocess.run(args,cwd=r,env=env,stdout=out,stderr=subprocess.STDOUT)
  record['steps'].append({'name':name,'exit':p.returncode,'seconds':time.monotonic()-start});(r/'build/pipeline126.json').write_text(json.dumps(record,indent=2),encoding='utf8');print(json.dumps(record['steps'][-1]),flush=True)
  if p.returncode:raise SystemExit(p.returncode)
 record['passed']=True
finally:
 record['finishedAt']=time.time();(r/'build/pipeline126.json').write_text(json.dumps(record,indent=2),encoding='utf8')
