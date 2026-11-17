"""Sequential focused regression, desktop checks and installed runtime build."""
from pathlib import Path
import subprocess,os,shutil,json,xml.etree.ElementTree as E
r=Path.cwd();j=Path('C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot');env=os.environ.copy();env.update(JAVA_HOME=str(j),PATH=str(j/'bin')+';'+env['PATH'])
classes=['CalibratedClearance159Test','Calibrated159Test','Alignment041Test','ObservationTask158Test','WorkflowTest','ObservationArtifact128Test','ProcedureLifecycle127Test','Domain044Test','BrowserOutcome126Test','Agency043Test','ContinuityExperiment157Test']
suffix=''

def run(args,log,cwd=r):
 log=log.replace('.log',suffix+'.log')
 with (r/'build'/log).open('xb') as out:subprocess.run(args,cwd=cwd,env=env,stdout=out,stderr=subprocess.STDOUT,check=True)
 print(log+': passed',flush=True)
dest=r/'build/validation159/final';dest.mkdir(parents=True,exist_ok=True)
args=[str(r/'gradlew.bat'),'--no-daemon','--max-workers=1',':workspace-engine:test']
for c in classes:args+=['--tests','fraymus.workspace.'+c]
run(args,'final-java159.log');dest=r/'build/validation159/final';dest.mkdir(parents=True,exist_ok=True);count=0
for p in (r/'workspace-engine/build/test-results/test').glob('*.xml'):
 e=E.parse(p).getroot();assert sum(int(e.get(k,'0')) for k in ['failures','errors','skipped'])==0;count+=int(e.get('tests'));shutil.copy2(p,dest/p.name)
count=sum(int(E.parse(p).getroot().get('tests'))for p in dest.glob('*.xml'))
run(['npm.cmd','test'],'desktop159.log',r/'workspace-desktop')
run([str(r/'gradlew.bat'),'--no-daemon','--max-workers=1',':workspace-engine:installDist'],'install159.log')
run(['node','scripts/Study-Build095.cjs','--stamp'],'stamp159.log')
for name in ['docs/publication159/study159.cjs','workspace-desktop/renderer/calibrated159.js']:run(['node','--check',name],Path(name).stem+'-syntax159.log')
(r/'build/validation159/summary.json').write_text(json.dumps({'passed':True,'java':count,'classes':classes},indent=2)+'\n');print('Final Java checks: '+str(count),flush=True)
