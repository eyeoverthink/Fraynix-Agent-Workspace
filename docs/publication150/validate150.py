from pathlib import Path
import os,subprocess,time,shutil,json,sys,re,ast
r=Path.cwd();env=os.environ.copy();env['JAVA_HOME']='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot';env['PYTHONUTF8']='1';env.pop('JAVA_TOOL_OPTIONS',None);env.pop('GALLIUM_DRIVER',None)
folder=r/'build/validation150';folder.mkdir(parents=True,exist_ok=True)
def run(name,args,cwd=r):
 start=time.monotonic()
 for suffix in ['log','json']:
  prior=r/'build'/f'{name}.{suffix}'
  if prior.exists():
   archived=folder/'prior-attempts';archived.mkdir(exist_ok=True);shutil.copy2(prior,archived/(str(time.time_ns())+'-'+prior.name))
 with (r/'build'/f'{name}.log').open('wb') as out:p=subprocess.run(args,cwd=cwd,env=env,stdout=out,stderr=subprocess.STDOUT)
 result={'name':name,'exit':p.returncode,'seconds':time.monotonic()-start,'source':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()};(r/'build'/f'{name}.json').write_text(json.dumps(result,indent=2),encoding='utf8');print(json.dumps(result),flush=True)
 if p.returncode:raise SystemExit(p.returncode)
group=sys.argv[1]
if group=='java':
 import hashlib
 lock=json.loads((r/'docs/OBSERVATION_150_LOCK.json').read_bytes())
 for name,digest in lock['sourceHashes'].items():assert hashlib.sha256((r/name).read_bytes().replace(b'\r\n',b'\n')).hexdigest()==digest,name
 run('kernel150-final',['cmd','/c','gradlew.bat',':factory-kernel:verifyIntegration','--console=plain','--max-workers=2'])
 prior=(r/'docs/publication149/validate149.py').read_text(encoding='utf8')
 tests=ast.literal_eval(re.search(r' tests=(\[.*?\])\n',prior).group(1))
 tests+=['IdentityReturn149Test','IdentityReserved149Test','StartingReference150Test','StartingReference150FixturesTest']
 args=['cmd','/c','gradlew.bat',':workspace-engine:test']
 for t in dict.fromkeys(tests):args+=['--tests','fraymus.workspace.'+t]
 run('java150-final',args+['--console=plain','--max-workers=2'])
 dest=folder/('final' if not (folder/'final').exists() else 'final-'+str(time.time_ns()))
 shutil.copytree(r/'workspace-engine/build/test-results/test',dest)
elif group=='desktop':
 run('desktop150-final',['cmd','/c','npm.cmd','test'],r/'workspace-desktop')
 env['AGENCY043_OUTPUT']=str(r/'build/agency150-evidence')
 run('agency150-regression',['node','workspace-desktop/test/agency043-integration.cjs'])
elif group=='package':
 env['FRAYNIX_WORKSPACE_PROJECT']=str(r/'build/launcher150-isolated-project')
 run('launcher150',['powershell.exe','-NoProfile','-ExecutionPolicy','Bypass','-File','scripts/Start-Agent-Workspace.ps1','-PrepareOnly','-Offline'])
 env.pop('FRAYNIX_WORKSPACE_PROJECT',None)
 run('package150',['cmd','/c','gradlew.bat','prepareGameRuntime115',':workspace-engine:installDist','--console=plain','--max-workers=2'])
 run('stamp150',['node','scripts/Study-Build095.cjs','--stamp'])
 run('receipt150',['node','scripts/Study-Build095.cjs'])
else:raise ValueError('Unknown validation group')
