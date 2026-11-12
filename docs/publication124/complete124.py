from pathlib import Path
import json,subprocess,shutil,time
r=Path.cwd().resolve();assert json.loads((r/'build/pipeline124.json').read_text(encoding='utf8'))['passed']
for rel in ['workspace-engine/build/budget124-reserved','build/validation124/budget124-reserved-engine']:
 p=(r/rel).resolve();target=p.with_name(p.name+'-pre-admission');assert p.is_relative_to(r) and target.is_relative_to(r) and not target.exists();p.rename(target)
shutil.copy2(r/'build/budget124-reserved.log',r/'build/budget124-reserved-pre-admission.log')
result={'passed':False,'scope':'Fresh final wrapper tests and development/reserved calibration after the admission interruption guard. Earlier passes retained.','steps':[]}
for group in ['final','development','reserved']:
 start=time.time();p=subprocess.run(['python','docs/publication124/validate124.py',group]);result['steps'].append({'group':group,'exit':p.returncode,'seconds':time.time()-start});(r/'build/final-recheck124.json').write_text(json.dumps(result,indent=2),encoding='utf8')
 if p.returncode:raise SystemExit(p.returncode)
result['passed']=True;(r/'build/final-recheck124.json').write_text(json.dumps(result,indent=2),encoding='utf8')
