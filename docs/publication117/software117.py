from pathlib import Path
import os,subprocess,shutil,json,time
r=Path.cwd();v=r/'build/validation117';env=os.environ.copy();env['JAVA_HOME']='S:/Codex-Work/fraynix-workspace-edition116/build/mesa116/jdk21';env['GALLIUM_DRIVER']='llvmpipe';env['JAVA_TOOL_OPTIONS']='-Dstudy117.partition=reserved -Dstudy117.output=build/controller117-mesa-reserved -Dreference116.output=build/reference117-mesa'
started=time.time()
args=[':test','--tests','agent.Pursuit117Test','--tests','renderer.*',':workspace-engine:test','--tests','fraymus.workspace.GameController117Test','--tests','fraymus.workspace.GameReference115Test','--tests','fraymus.workspace.GameReference116Test','--tests','fraymus.workspace.GameTask115Test','--console=plain','--max-workers=2']
with (r/'build/software117.log').open('wb') as f:p=subprocess.run(['cmd','/c','gradlew.bat',*args],env=env,stdout=f,stderr=subprocess.STDOUT)
for prefix,suffix in [('', 'root'),('workspace-engine/', 'engine')]:shutil.copytree(r/prefix/'build/test-results/test',v/('software117-'+suffix),dirs_exist_ok=True)
print(json.dumps({'check':'software117','exit':p.returncode,'seconds':round(time.time()-started,3)}),flush=True);raise SystemExit(p.returncode)
