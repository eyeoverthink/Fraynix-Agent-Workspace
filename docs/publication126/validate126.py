from pathlib import Path
import os,subprocess,time,shutil,json,sys
r=Path.cwd();env=os.environ.copy();env['JAVA_HOME']='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot';env.pop('JAVA_TOOL_OPTIONS',None);env.pop('GALLIUM_DRIVER',None)
folder=r/'build/validation126';folder.mkdir(parents=True,exist_ok=True)
def run(name,args,cwd=r):
 start=time.monotonic()
 with (r/'build'/f'{name}.log').open('wb') as out:p=subprocess.run(args,cwd=cwd,env=env,stdout=out,stderr=subprocess.STDOUT)
 result={'name':name,'exit':p.returncode,'seconds':time.monotonic()-start};(r/'build'/f'{name}.json').write_text(json.dumps(result,indent=2),encoding='utf8');print(json.dumps(result),flush=True)
 if p.returncode:raise SystemExit(p.returncode)
group=sys.argv[1]
if group=='java':
 tests=['BrowserOutcome126Test','Agency043Test','Agent042Test','ChatChain052Test','ChatArchive112Test','LearningStoreTest','Domain044Test','Chat099Test','KnowledgeStoreTest','LocalModelAdmissionTest','CreationPlanner044Test','CreationTest','CreationRetry102Test','GoalRunnerTest','GoalArchive111Test']
 args=['cmd','/c','gradlew.bat',':workspace-engine:test']
 for t in tests:args+=['--tests','fraymus.workspace.'+t]
 run('java126-final',args+['--console=plain','--max-workers=2'])
 shutil.copytree(r/'workspace-engine/build/test-results/test',folder/'final',dirs_exist_ok=False)
elif group=='desktop':
 run('desktop126-final',['cmd','/c','npm.cmd','test'],r/'workspace-desktop')
 run('agency126-regression',['node','workspace-desktop/test/agency043-integration.cjs'])
elif group=='package':
 env['FRAYNIX_WORKSPACE_PROJECT']=str(r/'build/launcher126-isolated-project')
 run('launcher126',['powershell.exe','-NoProfile','-ExecutionPolicy','Bypass','-File','scripts/Start-Agent-Workspace.ps1','-PrepareOnly','-Offline'])
 env.pop('FRAYNIX_WORKSPACE_PROJECT',None)
 run('package126',['cmd','/c','gradlew.bat','prepareGameRuntime115',':workspace-engine:installDist','--console=plain','--max-workers=2'])
 run('receipt126',['node','scripts/Study-Build095.cjs'])
else:raise ValueError('Unknown validation group')
