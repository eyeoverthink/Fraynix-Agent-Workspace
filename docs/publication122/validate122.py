from pathlib import Path
import os,subprocess,shutil,json,time,sys
r=Path.cwd();v=r/'build/validation122';v.mkdir(parents=True,exist_ok=True);env=os.environ.copy();env['JAVA_HOME']='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot';env.pop('GALLIUM_DRIVER',None)
def run(name,args,opts=''):
 env['JAVA_TOOL_OPTIONS']=opts;start=time.time()
 with (r/'build'/f'{name}.log').open('wb') as f:p=subprocess.run(['cmd','/c','gradlew.bat',*args,'--console=plain','--max-workers=2'],env=env,stdout=f,stderr=subprocess.STDOUT)
 for prefix,suffix in [('', 'root'),('workspace-engine/','engine')]:
  folder=r/prefix/'build/test-results/test'
  if folder.exists():shutil.copytree(folder,v/(name+'-'+suffix),dirs_exist_ok=True)
 print(json.dumps({'check':name,'exit':p.returncode,'seconds':round(time.time()-start,3)}),flush=True)
 if p.returncode:raise SystemExit(p.returncode)
group=sys.argv[1]
if group=='unit':
 run('unit122',[':test','--tests','agent.EnemyLanding122Test','--tests','agent.ContactRecovery119Test','--tests','agent.OpportunityMotion121Test'])
elif group=='regression':
 for part in ['old119-development','old119-reserved','old118-development','old118-reserved','new','early-probes']:
  run('hardware122-'+part,[':workspace-engine:test','--tests','fraymus.workspace.GameController120Test'],f'-Dstudy120.partition={part} -Dstudy120.policies=contact122 -Dstudy120.requireWins=true -Dstudy120.output=build/controller122-old-{part}')
 run('hardware122-old120-reserved',[':workspace-engine:test','--tests','fraymus.workspace.GameController120Test'],'-Dstudy120.reserved=true -Dstudy120.policies=contact122 -Dstudy120.requireWins=true -Dstudy120.output=build/controller122-old120-reserved')
 for part in ['regression','wider','reserved']:
  run('hardware122-old117-'+part,[':workspace-engine:test','--tests','fraymus.workspace.GameController117Test'],f'-Dstudy117.policies=contact122 -Dstudy117.partition={part} -Dstudy117.output=build/controller122-old117-{part}')
 for reserved in ['false','true']:
  run('hardware122-old121-'+reserved,[':workspace-engine:test','--tests','fraymus.workspace.GameController121Test'],f'-Dstudy121.reserved={reserved} -Dstudy121.policies=contact122 -Dstudy121.output=build/controller122-old121-{reserved}')
elif group=='full':
 run('hardware122-main',[':test',':workspace-engine:test','--tests','agent.*','--tests','runtime.*','--tests','components.*','--tests','renderer.*','--tests','fraymus.workspace.GameContact122Test','--tests','fraymus.workspace.GameOpportunity121Test','--tests','fraymus.workspace.GameOpportunity120Test','--tests','fraymus.workspace.GameRecovery119Test','--tests','fraymus.workspace.GameChallenge118Test','--tests','fraymus.workspace.GameLevels114Test','--tests','fraymus.workspace.GameTask115Test','--tests','fraymus.workspace.GameReference115Test','--tests','fraymus.workspace.GameReference116Test','--tests','fraymus.workspace.EmbeddedGame*','--tests','fraymus.workspace.SpatialGame040Test','--tests','fraymus.workspace.Domain044Test','--tests','fraymus.workspace.ChatChain052Test','--tests','fraymus.workspace.Agency043Test','--tests','fraymus.workspace.Load055Test'],'-Dreference116.output=build/reference122-final')
else:raise ValueError('Unknown group')
