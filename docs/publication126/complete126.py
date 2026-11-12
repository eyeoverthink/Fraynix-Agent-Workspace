from pathlib import Path
import json,subprocess,time
r=Path.cwd();assert json.loads((r/'build/model126-complete/result.json').read_text(encoding='utf8'))['passed'];result={'steps':[],'passed':False}
try:
 for name,args in [('validation126-java',['python','docs/publication126/validate126.py','java']),('validation126-desktop',['python','docs/publication126/validate126.py','desktop']),('validation126-package',['python','docs/publication126/validate126.py','package']),('history126',['node','docs/publication126/history126.cjs']),('postaudit126',['python','docs/publication126/postaudit126.py'])]:
  print('Starting '+name,flush=True);start=time.monotonic()
  with (r/'build'/f'{name}.log').open('wb') as out:p=subprocess.run(args,cwd=r,stdout=out,stderr=subprocess.STDOUT)
  result['steps'].append({'name':name,'exit':p.returncode,'seconds':time.monotonic()-start});print(json.dumps(result['steps'][-1]),flush=True)
  if p.returncode:raise SystemExit(p.returncode)
 result['passed']=True
finally:(r/'build/pipeline126-complete.json').write_text(json.dumps(result,indent=2),encoding='utf8')
