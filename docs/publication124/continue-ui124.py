from pathlib import Path
import os,json,subprocess,time,shutil
r=Path.cwd();old=r/'build/game124-ui';prior=json.loads((old/'result.json').read_text(encoding='utf8'));pipeline=json.loads((r/'build/pipeline124.json').read_text(encoding='utf8'))
assert not prior['passed'] and len(prior['requests'])==9 and not prior['errors']
last=prior['requests'][-1];decision=last['checked']['pickupChoice124']['decision']['result'];assert last['task']['status']=='completed' and decision['allowance']==30 and decision['selected']=='direct'
assert next(o for o in decision['alternatives'] if o['strategy']=='full')['requiredSecondsPerPickup']>30
assert all(x['exit']==0 for x in pipeline['steps'] if x['group']!='review')
shutil.copy2(r/'build/pipeline124.json',r/'build/pipeline124-before-ui-continuation.json')
env=os.environ.copy();env['JAVA_HOME']='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot';env.pop('JAVA_TOOL_OPTIONS',None);env.pop('GALLIUM_DRIVER',None);env['GAME124_OUTPUT']=str(r/'build/game124-ui-final');env['GAME124_RESUME']=str(old)
start=time.time()
with (r/'build/game124-ui-final.log').open('wb') as log:p=subprocess.run(['node','workspace-desktop/test/game124-integration.cjs'],env=env,stdout=log,stderr=subprocess.STDOUT)
pipeline['steps'].append({'group':'review-continuation','exit':p.returncode,'seconds':time.time()-start,'scope':'Same calibration project; preserve30-second abstention and test explicit120 allowance.'});pipeline['passed']=p.returncode==0
(r/'build/pipeline124.json').write_text(json.dumps(pipeline,indent=2),encoding='utf8');print(json.dumps(pipeline['steps'][-1]),flush=True)
if p.returncode:raise SystemExit(p.returncode)
subprocess.run(['node','scripts/Study-Build095.cjs'],check=True,env=env)
