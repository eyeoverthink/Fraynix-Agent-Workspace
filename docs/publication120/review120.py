"""Serial software, desktop, packaging and actual visible review; stop on any failure."""
from pathlib import Path
import os,subprocess,time,json
r=Path.cwd();env=os.environ.copy();env['JAVA_HOME']='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot'
env.pop('GALLIUM_DRIVER',None);env.pop('JAVA_TOOL_OPTIONS',None)
def run(name,args,cwd=r):
 start=time.time()
 with (r/'build'/f'{name}.log').open('wb') as f:p=subprocess.run(args,cwd=cwd,env=env,stdout=f,stderr=subprocess.STDOUT)
 print(json.dumps({'check':name,'exit':p.returncode,'seconds':round(time.time()-start,3)}),flush=True)
 if p.returncode:raise SystemExit(p.returncode)
run('software120-run',['python','docs/publication120/software120.py'])
run('desktop120',['cmd','/c','npm.cmd','test'],r/'workspace-desktop')
run('desktop120-extra',['node','--test','test/inputs100.test.cjs','test/data-creation101.test.cjs','test/workflow-cost102.test.cjs'],r/'workspace-desktop')
run('package120',['cmd','/c','gradlew.bat','prepareGameRuntime115',':workspace-engine:installDist','--console=plain','--max-workers=2'])
run('stamp120',['node','scripts/Study-Build095.cjs','--stamp'])
env['GAME120_OUTPUT']=str(r/'build/game120-ui-final')
run('game120-ui-final',['node','workspace-desktop/test/game120-integration.cjs'])
run('receipt120',['node','scripts/Study-Build095.cjs'])
