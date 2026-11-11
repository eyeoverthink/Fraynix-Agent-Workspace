from pathlib import Path
import os,subprocess,shutil,json,time
r=Path.cwd();v=r/'build/validation118';env=os.environ.copy();env['JAVA_HOME']='S:/Codex-Work/fraynix-workspace-edition116/build/mesa116/jdk21';env['GALLIUM_DRIVER']='llvmpipe';env.pop('JAVA_TOOL_OPTIONS',None)
for n in ['reference115','game115-repair','game115-restart','challenge118-task','challenge118-failed','challenge118-image']:
 p=r/'workspace-engine/build'/n
 if p.exists():shutil.copytree(p,v/'software-before-default'/n,dirs_exist_ok=True)
args=[':test','--tests','agent.PickupSafety118Test',':workspace-engine:test','--tests','fraymus.workspace.GameChallenge118Test','--tests','fraymus.workspace.GameTask115Test','--tests','fraymus.workspace.GameReference115Test','--tests','fraymus.workspace.EmbeddedGameTest','--console=plain','--max-workers=2']
start=time.time()
with (r/'build/final-default118.log').open('wb') as f:p=subprocess.run(['cmd','/c','gradlew.bat',*args],env=env,stdout=f,stderr=subprocess.STDOUT)
for prefix,suffix in [('', 'root'),('workspace-engine/', 'engine')]:shutil.copytree(r/prefix/'build/test-results/test',v/('final-default118-'+suffix),dirs_exist_ok=True)
print(json.dumps({'check':'final-default118','exit':p.returncode,'seconds':time.time()-start}),flush=True)
if p.returncode:raise SystemExit(p.returncode)
env['JAVA_HOME']='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot';env.pop('GALLIUM_DRIVER',None)
with (r/'build/final-install118.log').open('wb') as f:p=subprocess.run(['cmd','/c','gradlew.bat','prepareGameRuntime115',':workspace-engine:installDist','--console=plain','--max-workers=2'],env=env,stdout=f,stderr=subprocess.STDOUT)
if p.returncode:raise SystemExit(p.returncode)
subprocess.run(['node','scripts/Study-Build095.cjs','--stamp'],check=True)
with (r/'build/game118-ui.log').open('wb') as f:p=subprocess.run(['node','workspace-desktop/test/game118-integration.cjs'],env=env,stdout=f,stderr=subprocess.STDOUT)
print(json.dumps({'check':'actual-ui118','exit':p.returncode}),flush=True)
if p.returncode:raise SystemExit(p.returncode)
subprocess.run(['node','scripts/Study-Build095.cjs'],check=True)
