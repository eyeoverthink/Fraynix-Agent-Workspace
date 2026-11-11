from pathlib import Path
import os,subprocess,shutil,json,time
r=Path.cwd();v=r/'build/validation117';v.mkdir(parents=True,exist_ok=True)
env=os.environ.copy();env['JAVA_HOME']='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot';env.pop('GALLIUM_DRIVER',None)
def run(name,args,opts='',copy=True):
 env['JAVA_TOOL_OPTIONS']=opts
 started=time.time()
 with (r/'build'/f'{name}.log').open('wb') as f:p=subprocess.run(['cmd','/c','gradlew.bat',*args,'--console=plain','--max-workers=2'],env=env,stdout=f,stderr=subprocess.STDOUT)
 if copy:
  for prefix in (['', 'workspace-engine/'] if ':test' in args else ['workspace-engine/']):
   folder=r/prefix/'build/test-results/test'
   if folder.exists():shutil.copytree(folder,v/(name+('-root' if not prefix else '-engine')),dirs_exist_ok=True)
 print(json.dumps({'check':name,'exit':p.returncode,'seconds':round(time.time()-started,3)}),flush=True)
 if p.returncode:raise SystemExit(p.returncode)
run('hardware117-main',[':test',':workspace-engine:test','--tests','agent.*','--tests','runtime.*','--tests','components.*','--tests','renderer.*','--tests','fraymus.workspace.GameLevels114Test','--tests','fraymus.workspace.GameTask115Test','--tests','fraymus.workspace.GameReference115Test','--tests','fraymus.workspace.GameReference116Test','--tests','fraymus.workspace.GameController117Test','--tests','fraymus.workspace.EmbeddedGame*','--tests','fraymus.workspace.SpatialGame040Test','--tests','fraymus.workspace.Domain044Test','--tests','fraymus.workspace.ChatChain052Test','--tests','fraymus.workspace.Agency043Test'], '-Dstudy117.partition=regression -Dstudy117.output=build/controller117-final-regression -Dreference116.output=build/reference117-final')
for partition in ['wider','reserved']:
 run('hardware117-'+partition,[':workspace-engine:test','--tests','fraymus.workspace.GameController117Test'],f'-Dstudy117.partition={partition} -Dstudy117.output=build/controller117-final-{partition}')
run('install117',[':workspace-engine:installDist'],'',False)
