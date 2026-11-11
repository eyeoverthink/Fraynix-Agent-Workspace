from pathlib import Path
import os,subprocess,shutil,json,time,sys
r=Path.cwd();v=r/'build/validation119';v.mkdir(parents=True,exist_ok=True)
env=os.environ.copy();env['JAVA_HOME']='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot';env.pop('GALLIUM_DRIVER',None)
def run(name,args,opts=''):
 env['JAVA_TOOL_OPTIONS']=opts;start=time.time()
 with (r/'build'/f'{name}.log').open('wb') as f:p=subprocess.run(['cmd','/c','gradlew.bat',*args,'--console=plain','--max-workers=2'],env=env,stdout=f,stderr=subprocess.STDOUT)
 for prefix in (['','workspace-engine/'] if ':test' in args else ['workspace-engine/']):
  folder=r/prefix/'build/test-results/test'
  if folder.exists():shutil.copytree(folder,v/(name+('-root' if not prefix else '-engine')),dirs_exist_ok=True)
 print(json.dumps({'check':name,'exit':p.returncode,'seconds':round(time.time()-start,3)}),flush=True)
 if p.returncode:raise SystemExit(p.returncode)
if sys.argv[1]=='regression':
 for partition in ['old-development','old-reserved']:
  run('hardware119-'+partition,[':workspace-engine:test','--tests','fraymus.workspace.GameController119Test'],f'-Dstudy119.partition={partition} -Dstudy119.policies=recover119 -Dstudy119.requireWins=true -Dstudy119.output=build/controller119-{partition}')
 for partition in ['regression','wider','reserved']:
  run('hardware119-old-'+partition,[':workspace-engine:test','--tests','fraymus.workspace.GameController117Test'],f'-Dstudy117.policies=safe117,recover119 -Dstudy117.partition={partition} -Dstudy117.output=build/controller117-119-{partition}')
elif sys.argv[1]=='reserved':
 run('hardware119-reserved',[':workspace-engine:test','--tests','fraymus.workspace.GameController119Test'],'-Dstudy119.reserved=true -Dstudy119.policies=simple,safe117,risk118,recover119 -Dstudy119.output=build/controller119-reserved')
elif sys.argv[1]=='full':
 run('hardware119-main',[':test',':workspace-engine:test','--tests','agent.*','--tests','runtime.*','--tests','components.*','--tests','renderer.*','--tests','fraymus.workspace.GameRecovery119Test','--tests','fraymus.workspace.GameChallenge118Test','--tests','fraymus.workspace.GameLevels114Test','--tests','fraymus.workspace.GameTask115Test','--tests','fraymus.workspace.GameReference115Test','--tests','fraymus.workspace.GameReference116Test','--tests','fraymus.workspace.EmbeddedGame*','--tests','fraymus.workspace.SpatialGame040Test','--tests','fraymus.workspace.Domain044Test','--tests','fraymus.workspace.ChatChain052Test','--tests','fraymus.workspace.Agency043Test'],'-Dreference116.output=build/reference119-final')
else:raise ValueError('Unknown validation group')
