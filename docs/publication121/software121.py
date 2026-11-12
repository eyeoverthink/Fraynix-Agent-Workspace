from pathlib import Path
import os,subprocess,shutil,json,time
r=Path.cwd();v=r/'build/validation121';env=os.environ.copy();env['JAVA_HOME']='S:/Codex-Work/fraynix-workspace-edition116/build/mesa116/jdk21';env['GALLIUM_DRIVER']='llvmpipe';env['JAVA_TOOL_OPTIONS']='-Dstudy121.reserved=true -Dstudy121.policies=simple,opportunity120,observe121,revisit121 -Dstudy121.output=build/controller121-mesa-reserved -Dreference116.output=build/reference121-mesa'
for name in ['reference115','game115-repair','game115-restart','challenge118-task','challenge118-failed','challenge118-image','recovery119-task','opportunity121-task','opportunity120-task']:
 source=r/'workspace-engine/build'/name
 if source.exists():shutil.copytree(source,v/'hardware-artifacts'/name,dirs_exist_ok=True)
started=time.time()
args=[':test','--tests','agent.OpportunityMotion121Test','--tests','agent.OpportunityTiming120Test','--tests','agent.PickupOpportunity120Test','--tests','agent.ContactRecovery119Test','--tests','agent.ContactObserver119Test','--tests','agent.PowerupPlannerTest','--tests','renderer.*',':workspace-engine:test','--tests','fraymus.workspace.GameController121Test','--tests','fraymus.workspace.GameOpportunity121Test','--tests','fraymus.workspace.GameRecovery119Test','--tests','fraymus.workspace.GameReference115Test','--tests','fraymus.workspace.GameReference116Test','--tests','fraymus.workspace.GameTask115Test','--tests','fraymus.workspace.GameChallenge118Test','--console=plain','--max-workers=2']
with (r/'build/software121.log').open('wb') as f:p=subprocess.run(['cmd','/c','gradlew.bat',*args],env=env,stdout=f,stderr=subprocess.STDOUT)
for prefix,suffix in [('', 'root'),('workspace-engine/', 'engine')]:shutil.copytree(r/prefix/'build/test-results/test',v/('software121-'+suffix),dirs_exist_ok=True)
if p.returncode==0:
 data=json.loads((r/'workspace-engine/build/controller121-mesa-reserved/result.json').read_text(encoding='utf-8'));assert all('llvmpipe' in json.dumps(t['initial']['renderer']).lower() for c in data['cases'] for t in c['trials']),'Expected actual software renderer'
print(json.dumps({'check':'software121','exit':p.returncode,'seconds':round(time.time()-started,3)}),flush=True);raise SystemExit(p.returncode)
