"""Sequential focused regression, desktop checks and installed runtime build."""
from pathlib import Path
import subprocess,os,shutil,json,xml.etree.ElementTree as E
r=Path.cwd();j=Path('C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot');env=os.environ.copy();env.update(JAVA_HOME=str(j),PATH=str(j/'bin')+';'+env['PATH'])
classes=['ObservationTask158Test','WorkflowTest','ObservationArtifact128Test','ProcedureLifecycle127Test','Domain044Test','BrowserOutcome126Test','Agency043Test','ContinuityExperiment157Test']
def run(args,log,cwd=r):
 with (r/'build'/log).open('xb') as out:subprocess.run(args,cwd=cwd,env=env,stdout=out,stderr=subprocess.STDOUT,check=True)
 print(log+': passed',flush=True)
args=[str(r/'gradlew.bat'),'--no-daemon','--max-workers=1',':workspace-engine:test']
for c in classes:args+=['--tests','fraymus.workspace.'+c]
run(args,'final-java158.log');dest=r/'build/validation158/final';dest.mkdir(parents=True);count=0
for p in (r/'workspace-engine/build/test-results/test').glob('*.xml'):
 e=E.parse(p).getroot();assert sum(int(e.get(k,'0')) for k in ['failures','errors','skipped'])==0;count+=int(e.get('tests'));shutil.copy2(p,dest/p.name)
run(['npm.cmd','test'],'desktop158.log',r/'workspace-desktop')
run([str(r/'gradlew.bat'),'--no-daemon','--max-workers=1',':workspace-engine:installDist'],'install158.log')
run(['node','scripts/Study-Build095.cjs','--stamp'],'stamp158.log')
for name in ['docs/publication158/study158.cjs','workspace-desktop/renderer/association134-panel.js']:run(['node','--check',name],Path(name).stem+'-syntax158.log')
(r/'build/validation158/summary.json').write_text(json.dumps({'passed':True,'java':count,'classes':classes},indent=2)+'\n');print('Final Java checks: '+str(count),flush=True)
