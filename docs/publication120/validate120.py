from pathlib import Path
import os,subprocess,shutil,json,time,sys
r=Path.cwd();v=r/'build/validation120';v.mkdir(parents=True,exist_ok=True);env=os.environ.copy();env['JAVA_HOME']='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot';env.pop('GALLIUM_DRIVER',None)
def run(name,args,opts=''):
 suffix='-final' if len(sys.argv)>2 and sys.argv[2]=='final' else '';name+=suffix;opts=opts.replace(' -Dstudy120.requireWins',suffix+' -Dstudy120.requireWins') if sys.argv[1]=='development' else opts+suffix if suffix else opts
 env['JAVA_TOOL_OPTIONS']=opts;start=time.time()
 with (r/'build'/f'{name}.log').open('wb') as f:p=subprocess.run(['cmd','/c','gradlew.bat',*args,'--console=plain','--max-workers=2'],env=env,stdout=f,stderr=subprocess.STDOUT)
 for prefix in (['','workspace-engine/'] if ':test' in args else ['workspace-engine/']):
  folder=r/prefix/'build/test-results/test'
  if folder.exists():shutil.copytree(folder,v/(name+('-root' if not prefix else '-engine')),dirs_exist_ok=True)
 print(json.dumps({'check':name,'exit':p.returncode,'seconds':round(time.time()-start,3)}),flush=True)
 if p.returncode:raise SystemExit(p.returncode)
if sys.argv[1]=='development':
 run('hardware120-development',[':test','--tests','agent.OpportunityTiming120Test','--tests','agent.PickupOpportunity120Test',':workspace-engine:test','--tests','fraymus.workspace.GameController120Test'],'-Dstudy120.output=build/controller120-development -Dstudy120.requireWins=true')
elif sys.argv[1]=='regression':
 for partition in ['old119-development','old119-reserved','old118-development','old118-reserved','early-probes']:
  run('hardware120-'+partition,[':workspace-engine:test','--tests','fraymus.workspace.GameController120Test'],f'-Dstudy120.partition={partition} -Dstudy120.policies=opportunity120 -Dstudy120.requireWins=true -Dstudy120.output=build/controller120-{partition}')
 for partition in ['regression','wider','reserved']:
  run('hardware120-old-'+partition,[':workspace-engine:test','--tests','fraymus.workspace.GameController117Test'],f'-Dstudy117.policies=safe117,opportunity120 -Dstudy117.partition={partition} -Dstudy117.output=build/controller117-120-{partition}')
elif sys.argv[1]=='reserved':
 run('hardware120-reserved',[':workspace-engine:test','--tests','fraymus.workspace.GameController120Test'],'-Dstudy120.reserved=true -Dstudy120.output=build/controller120-reserved')
else:raise ValueError('Unknown group')
