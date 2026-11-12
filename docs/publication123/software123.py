from pathlib import Path
import os,subprocess,shutil,json,time
r=Path.cwd();env=os.environ.copy();env['JAVA_HOME']='S:/Codex-Work/fraynix-workspace-edition116/build/mesa116/jdk21';env['GALLIUM_DRIVER']='llvmpipe';env['JAVA_TOOL_OPTIONS']='-Dstudy123.partition=reserved -Dstudy123.only=fresh-return -Dstudy123.output=build/pickup123-software -Dreference116.output=build/reference123-mesa'
v=r/'build/validation123';v.mkdir(parents=True,exist_ok=True)
for name in ['contact122-task','pickup123-task','reference115','opportunity121-task','opportunity120-task','recovery119-task','game115-restart','game115-repair','challenge118-task','challenge118-failed','challenge118-image']:
 p=r/'workspace-engine/build'/name
 if p.exists():shutil.copytree(p,v/'hardware-artifacts'/name,dirs_exist_ok=True)
start=time.time()
with (r/'build/software123.log').open('wb') as f:p=subprocess.run(['cmd','/c','gradlew.bat',':test','--tests','agent.*','--tests','renderer.*',':workspace-engine:test','--tests','fraymus.workspace.GamePickupStudy123Test','--tests','fraymus.workspace.GamePickupTask123Test','--tests','fraymus.workspace.GameContact122Test','--tests','fraymus.workspace.GameReference115Test','--tests','fraymus.workspace.GameReference116Test','--console=plain','--max-workers=2'],env=env,stdout=f,stderr=subprocess.STDOUT)
for prefix,suffix in [('', 'root'),('workspace-engine/','engine')]:shutil.copytree(r/prefix/'build/test-results/test',v/('software123-'+suffix),dirs_exist_ok=True)
if p.returncode==0:
 d=json.loads((r/'workspace-engine/build/pickup123-software/result.json').read_text(encoding='utf-8'));assert d['passed'];assert all('llvmpipe' in json.dumps(x['task']['trials'][0]['initial']['renderer']).lower() for x in d['rows'])
print(json.dumps({'check':'software123','exit':p.returncode,'seconds':time.time()-start}));raise SystemExit(p.returncode)
