from pathlib import Path
import os,subprocess,shutil,json,time,sys
r=Path.cwd();v=r/'build/validation124';v.mkdir(parents=True,exist_ok=True);env=os.environ.copy();env['JAVA_HOME']='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot';env.pop('GALLIUM_DRIVER',None)
def run(name,args,opts=''):
 env['JAVA_TOOL_OPTIONS']=opts;start=time.time()
 with (r/'build'/f'{name}.log').open('wb') as f:p=subprocess.run(['cmd','/c','gradlew.bat',*args,'--console=plain','--max-workers=2'],env=env,stdout=f,stderr=subprocess.STDOUT)
 for prefix,suffix in [('', 'root'),('workspace-engine/','engine')]:
  folder=r/prefix/'build/test-results/test'
  if folder.exists():shutil.copytree(folder,v/(name+'-'+suffix),dirs_exist_ok=True)
 print(json.dumps({'check':name,'exit':p.returncode,'seconds':round(time.time()-start,3)}),flush=True)
 if p.returncode:raise SystemExit(p.returncode)
group=sys.argv[1]
if group in ['development','reserved','software']:
 if group=='software':
  env['JAVA_HOME']='S:/Codex-Work/fraynix-workspace-edition116/build/mesa116/jdk21';env['GALLIUM_DRIVER']='llvmpipe'
 run('budget124-'+group,[':workspace-engine:test','--tests','fraymus.workspace.GameBudgetStudy124Test'], '-Dstudy124.partition='+('smoke' if group=='software' else group)+' -Dstudy124.output=build/budget124-'+group)
elif group=='unit':
 run('unit124',[':workspace-engine:test','--tests','fraymus.workspace.GameCost123Test','--tests','fraymus.workspace.GameBudgetPolicy124Test','--tests','fraymus.workspace.GameBudgetTask124Test','--tests','fraymus.workspace.GamePickupTask123Test'])
elif group=='regression':
 run('regression124-development',[':workspace-engine:test','--tests','fraymus.workspace.GameController122Test'],'-Dstudy122.policies=contact122 -Dstudy122.output=build/controller124-regression-development')
 run('regression124-reserved',[':workspace-engine:test','--tests','fraymus.workspace.GameController122Test'],'-Dstudy122.reserved=true -Dstudy122.policies=contact122 -Dstudy122.requireWins=true -Dstudy122.output=build/controller124-regression-reserved')
 run('regression124-waits',[':workspace-engine:test','--tests','fraymus.workspace.GameController120Test'],'-Dstudy120.only=retained-two-enemies,retained-turtle -Dstudy120.policies=contact122 -Dstudy120.requireWins=true -Dstudy120.output=build/controller124-regression-waits')
elif group=='full':
 run('hardware124-main',[':test',':workspace-engine:test','--tests','agent.*','--tests','runtime.*','--tests','components.*','--tests','renderer.*','--tests','fraymus.workspace.GameContact122Test','--tests','fraymus.workspace.GameCost123Test','--tests','fraymus.workspace.GameBudgetPolicy124Test','--tests','fraymus.workspace.GameBudgetTask124Test','--tests','fraymus.workspace.GamePickupTask123Test','--tests','fraymus.workspace.GameOpportunity121Test','--tests','fraymus.workspace.GameOpportunity120Test','--tests','fraymus.workspace.GameRecovery119Test','--tests','fraymus.workspace.GameChallenge118Test','--tests','fraymus.workspace.GameLevels114Test','--tests','fraymus.workspace.GameTask115Test','--tests','fraymus.workspace.GameReference115Test','--tests','fraymus.workspace.GameReference116Test','--tests','fraymus.workspace.EmbeddedGame*','--tests','fraymus.workspace.SpatialGame040Test','--tests','fraymus.workspace.Domain044Test','--tests','fraymus.workspace.ChatChain052Test','--tests','fraymus.workspace.Agency043Test','--tests','fraymus.workspace.Load055Test'],'-Dreference116.output=build/reference124-final')
else:raise ValueError('Unknown group')
