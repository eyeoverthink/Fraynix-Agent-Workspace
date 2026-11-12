from pathlib import Path
import os,subprocess,time,json
r=Path.cwd();env=os.environ.copy();env['JAVA_HOME']='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot';env.pop('JAVA_TOOL_OPTIONS',None);env.pop('GALLIUM_DRIVER',None)
env['GAME125_OUTPUT']=str(r/'build/game125-ui-verified');env['GAME125_RESUME']=str(r/'build/game125-ui-final')
pipeline=json.loads((r/'build/pipeline125.json').read_text(encoding='utf8'));assert not pipeline['passed'] and pipeline['steps'][-1]['group'].startswith('review') and pipeline['steps'][-1]['exit']==1
assert all(x['exit']==0 for x in pipeline['steps'] if not x['group'].startswith('review'))
start=time.time()
with (r/'build/game125-ui-verified.log').open('wb') as f:p=subprocess.run(['node','workspace-desktop/test/game125-integration.cjs'],env=env,stdout=f,stderr=subprocess.STDOUT)
pipeline['steps'].append({'group':'review-after-refresh','exit':p.returncode,'seconds':time.time()-start,'scope':'Original seven completed requests and same project preserved. Test-driver property access and stale UI refresh corrected. Native/Java source unchanged. Earlier driver failures and blank-review diagnostics retained.'})
pipeline['passed']=False
(r/'build/pipeline125.json').write_text(json.dumps(pipeline,indent=2),encoding='utf8')
if p.returncode:raise SystemExit(p.returncode)
with (r/'build/receipt125.log').open('wb') as f:subprocess.run(['node','scripts/Study-Build095.cjs'],env=env,stdout=f,stderr=subprocess.STDOUT,check=True)
pipeline['passed']=True;(r/'build/pipeline125.json').write_text(json.dumps(pipeline,indent=2),encoding='utf8');print(json.dumps({'passed':True,'continued':True,'seconds':time.time()-start}),flush=True)
