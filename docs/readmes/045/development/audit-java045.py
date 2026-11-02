import json,xml.etree.ElementTree as E
from pathlib import Path
import subprocess,hashlib,shutil
r=Path.cwd();rows=[];dest=r/'build/full-java045-junit';dest.mkdir(parents=True,exist_ok=True)
for p in r.glob('**/build/test-results/test/TEST-*.xml'):
 if 'node_modules' in p.parts:continue
 x=E.parse(p).getroot();module=p.relative_to(r).as_posix().split('/build/')[0];rows.append({'module':module,'file':p.name,'checks':int(x.attrib['tests']),'failures':int(x.attrib.get('failures',0)),'errors':int(x.attrib.get('errors',0)),'skipped':int(x.attrib.get('skipped',0))});shutil.copyfile(p,dest/(module.replace('/','-')+'-'+p.name))
v={'source':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),'checks':sum(x['checks']for x in rows),'classes':len(rows),'failures':sum(x['failures']for x in rows),'errors':sum(x['errors']for x in rows),'skipped':sum(x['skipped']for x in rows),'rows':rows};v['passed']=len(rows)>80 and v['failures']==v['errors']==v['skipped']==0
(r/'build/java045-full-audit.json').write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8');print(json.dumps({k:x for k,x in v.items()if k!='rows'}));assert v['passed']
