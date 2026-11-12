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
 run('software122-run',['python','docs/publication122/software122.py'])
 run('desktop122-final',['cmd','/c','npm.cmd','test'],r/'workspace-desktop')
 run('desktop122-extra',['node','--test','test/inputs100.test.cjs','test/data-creation101.test.cjs','test/workflow-cost102.test.cjs'],r/'workspace-desktop')
else:
 assert sys.argv[1]=='finish'
 assert json.loads((r/'workspace-engine/build/controller122-mesa-reserved/result.json').read_text(encoding='utf-8'))['passed']
 assert 'BUILD SUCCESSFUL' in (r/'build/software122.log').read_text(encoding='utf-8')
 for name,count in [('desktop122-final.log',158),('desktop122-extra.log',10)]:
  report=(r/'build'/name).read_text(encoding='utf-8');assert f'tests {count}' in report and 'fail 0' in report
env['FRAYNIX_WORKSPACE_PROJECT']=str(r/'build/launcher122-isolated-project')
run('launcher122',['powershell.exe','-NoProfile','-ExecutionPolicy','Bypass','-File','scripts/Start-Agent-Workspace.ps1','-PrepareOnly','-Offline'])
env.pop('FRAYNIX_WORKSPACE_PROJECT',None)
run('package122',['cmd','/c','gradlew.bat','prepareGameRuntime115',':workspace-engine:installDist','--console=plain','--max-workers=2'])
run('receipt-recheck122',['python','docs/publication122/recheck122.py'])
run('stamp122',['node','scripts/Study-Build095.cjs','--stamp'])
shutil.copy2(r/'build/study095-build.json',r/'build/study095-build-before-ui122.json')
env['GAME122_OUTPUT']=str(r/'build/game122-ui')
run('game122-ui',['node','workspace-desktop/test/game122-integration.cjs'])
run('receipt122',['node','scripts/Study-Build095.cjs'])
