from pathlib import Path
import os,subprocess,time,sys,shutil,json
r=Path.cwd();env=os.environ.copy();env['JAVA_HOME']='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot';env.pop('GALLIUM_DRIVER',None)
name=sys.argv[1];env['JAVA_TOOL_OPTIONS']=' '.join(sys.argv[2:]);started=time.time()
with (r/'build'/f'{name}.log').open('wb') as out:p=subprocess.run(['cmd','/c','gradlew.bat',':workspace-engine:test','--tests','fraymus.workspace.GameController122Test','--console=plain','--max-workers=2'],env=env,stdout=out,stderr=subprocess.STDOUT)
folder=r/'workspace-engine/build/test-results/test'
if folder.exists():shutil.copytree(folder,r/'build/validation122'/name,dirs_exist_ok=True)
print(json.dumps({'check':name,'exit':p.returncode,'seconds':time.time()-started}));sys.exit(p.returncode)
