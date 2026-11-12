"""Serial software, desktop, packaging and actual visible review; stop on any failure."""
from pathlib import Path
import os,subprocess,time,json,shutil,sys
r=Path.cwd();env=os.environ.copy();env['JAVA_HOME']='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot'
env.pop('GALLIUM_DRIVER',None);env.pop('JAVA_TOOL_OPTIONS',None)
def run(name,args,cwd=r):
 start=time.time()
 with (r/'build'/f'{name}.log').open('wb') as f:p=subprocess.run(args,cwd=cwd,env=env,stdout=f,stderr=subprocess.STDOUT)
 print(json.dumps({'check':name,'exit':p.returncode,'seconds':round(time.time()-start,3)}),flush=True)
 if p.returncode:raise SystemExit(p.returncode)
if len(sys.argv)==1:
 run('software121-run',['python','docs/publication121/software121.py'])
 run('desktop121-final',['cmd','/c','npm.cmd','test'],r/'workspace-desktop')
 run('desktop121-extra',['node','--test','test/inputs100.test.cjs','test/data-creation101.test.cjs','test/workflow-cost102.test.cjs'],r/'workspace-desktop')
else:
 assert sys.argv[1]=='finish'
 assert json.loads((r/'workspace-engine/build/controller121-mesa-reserved/result.json').read_text(encoding='utf-8'))['passed']
 assert 'BUILD SUCCESSFUL' in (r/'build/software121.log').read_text(encoding='utf-8')
 for name,count in [('desktop121-final.log',158),('desktop121-extra.log',10)]:
  report=(r/'build'/name).read_text(encoding='utf-8');assert f'tests {count}' in report and 'fail 0' in report
env['FRAYNIX_WORKSPACE_PROJECT']=str(r/'build/launcher121-isolated-project')
run('launcher121',['powershell.exe','-NoProfile','-ExecutionPolicy','Bypass','-File','scripts/Start-Agent-Workspace.ps1','-PrepareOnly','-Offline'])
env.pop('FRAYNIX_WORKSPACE_PROJECT',None)
run('launcher121-owner',['python','docs/publication121/launcher121.py'])
run('package121',['cmd','/c','gradlew.bat','prepareGameRuntime115',':workspace-engine:installDist','--console=plain','--max-workers=2'])
run('stamp121',['node','scripts/Study-Build095.cjs','--stamp'])
shutil.copy2(r/'build/study095-build.json',r/'build/study095-build-before-ui121.json')
env['GAME121_OUTPUT']=str(r/'build/game121-ui-reviewed')
run('game121-ui-reviewed',['node','workspace-desktop/test/game121-integration.cjs'])
run('receipt121',['node','scripts/Study-Build095.cjs'])
