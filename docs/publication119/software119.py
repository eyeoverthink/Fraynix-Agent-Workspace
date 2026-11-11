from pathlib import Path
import os,subprocess,shutil,json,time
r=Path.cwd();v=r/'build/validation119';env=os.environ.copy();env['JAVA_HOME']='S:/Codex-Work/fraynix-workspace-edition116/build/mesa116/jdk21';env['GALLIUM_DRIVER']='llvmpipe';env['JAVA_TOOL_OPTIONS']='-Dstudy119.reserved=true -Dstudy119.policies=simple,safe117,risk118,recover119 -Dstudy119.output=build/controller119-mesa-reserved -Dreference116.output=build/reference119-mesa'
for name in ['reference115','game115-repair','game115-restart','challenge118-task','challenge118-failed','challenge118-image','recovery119-task']:
 source=r/'workspace-engine/build'/name
 if source.exists():shutil.copytree(source,v/'hardware-artifacts'/name,dirs_exist_ok=True)
started=time.time()
args=[':test','--tests','agent.ContactRecovery119Test','--tests','agent.ContactObserver119Test','--tests','agent.PowerupPlannerTest','--tests','renderer.*',':workspace-engine:test','--tests','fraymus.workspace.GameController119Test','--tests','fraymus.workspace.GameRecovery119Test','--tests','fraymus.workspace.GameReference115Test','--tests','fraymus.workspace.GameReference116Test','--tests','fraymus.workspace.GameTask115Test','--tests','fraymus.workspace.GameChallenge118Test','--console=plain','--max-workers=2']
with (r/'build/software119.log').open('wb') as f:p=subprocess.run(['cmd','/c','gradlew.bat',*args],env=env,stdout=f,stderr=subprocess.STDOUT)
for prefix,suffix in [('', 'root'),('workspace-engine/', 'engine')]:shutil.copytree(r/prefix/'build/test-results/test',v/('software119-'+suffix),dirs_exist_ok=True)
print(json.dumps({'check':'software119','exit':p.returncode,'seconds':round(time.time()-started,3)}),flush=True);raise SystemExit(p.returncode)
