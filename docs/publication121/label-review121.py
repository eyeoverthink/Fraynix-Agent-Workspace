"""Recheck the restored historical label and repeat visible review after packaging."""
from pathlib import Path
import os,subprocess,shutil,time,json
r=Path.cwd();env=os.environ.copy();env['JAVA_HOME']='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot'
env.pop('GALLIUM_DRIVER',None);env.pop('JAVA_TOOL_OPTIONS',None)
def run(name,args):
 start=time.time()
 with (r/'build'/f'{name}.log').open('wb') as f:p=subprocess.run(args,env=env,stdout=f,stderr=subprocess.STDOUT)
 print(json.dumps({'check':name,'exit':p.returncode,'seconds':round(time.time()-start,3)}),flush=True)
 if p.returncode:raise SystemExit(p.returncode)
run('load121-label-final',['cmd','/c','gradlew.bat',':workspace-engine:test','--tests','fraymus.workspace.Load055Test','--console=plain','--max-workers=2'])
shutil.copytree(r/'workspace-engine/build/test-results/test',r/'build/validation121/load121-label-final-engine',dirs_exist_ok=True)
run('package121-label-final',['cmd','/c','gradlew.bat','prepareGameRuntime115',':workspace-engine:installDist','--console=plain','--max-workers=2'])
run('stamp121-label-final',['node','scripts/Study-Build095.cjs','--stamp'])
shutil.copy2(r/'build/study095-build.json',r/'build/study095-build-before-ui121-final.json')
env['GAME121_OUTPUT']=str(r/'build/game121-ui-final')
run('game121-ui-final',['node','workspace-desktop/test/game121-integration.cjs'])
run('receipt121-label-final',['node','scripts/Study-Build095.cjs'])
