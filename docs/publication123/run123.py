from pathlib import Path
import os,sys,subprocess,time,shutil,json
r=Path.cwd();env=os.environ.copy();env['JAVA_HOME']='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot';env.pop('GALLIUM_DRIVER',None)
name=sys.argv[1];env['JAVA_TOOL_OPTIONS']=' '.join(sys.argv[2:]);start=time.time()
with (r/'build'/f'{name}.log').open('wb') as f:p=subprocess.run(['cmd','/c','gradlew.bat',':workspace-engine:test','--tests','fraymus.workspace.GamePickupStudy123Test','--console=plain','--max-workers=2'],env=env,stdout=f,stderr=subprocess.STDOUT)
v=r/'build/validation123'/name;v.parent.mkdir(parents=True,exist_ok=True);shutil.copytree(r/'workspace-engine/build/test-results/test',v,dirs_exist_ok=True)
print(json.dumps({'check':name,'exit':p.returncode,'seconds':time.time()-start}));raise SystemExit(p.returncode)
