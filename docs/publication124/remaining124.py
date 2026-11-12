from pathlib import Path
import subprocess,json,time
r=Path.cwd();steps=['development','reserved','regression','full','software'];result={'passed':False,'steps':[]}
for group in steps:
 start=time.time();p=subprocess.run(['python','docs/publication124/validate124.py',group]);result['steps'].append({'group':group,'exit':p.returncode,'seconds':time.time()-start});(r/'build/pipeline124.json').write_text(json.dumps(result,indent=2),encoding='utf8')
 if p.returncode:raise SystemExit(p.returncode)
start=time.time();p=subprocess.run(['python','docs/publication124/review124.py']);result['steps'].append({'group':'review','exit':p.returncode,'seconds':time.time()-start});result['passed']=p.returncode==0;(r/'build/pipeline124.json').write_text(json.dumps(result,indent=2),encoding='utf8');raise SystemExit(p.returncode)
