from pathlib import Path
import subprocess,os,shutil,xml.etree.ElementTree as ET
r=Path.cwd();j=Path('C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot');env=os.environ.copy();env.update(JAVA_HOME=str(j),PATH=str(j/'bin')+';'+env['PATH'])
xml=r/'workspace-engine/build/test-results/test';pre=r/'build/validation157/preflight';final=r/'build/validation157/final';pre.mkdir(parents=True);final.mkdir(parents=True)
checks=0
for p in xml.glob('*.xml'):
 e=ET.parse(p).getroot();assert sum(int(e.get(k,'0')) for k in ['failures','errors','skipped'])==0;checks+=int(e.get('tests'));shutil.copy2(p,pre/p.name);shutil.copy2(p,final/p.name)
assert checks==13,checks
classes=['RotationSearch155Test','ContinuityExperiment155Test','ContinuityReturn154Test','ContinuityExperiment154Test','RotationSearch153Test','RotationExperiment153Test','RotationSearch152Test','RotationExperiment152Test','ReplayEvidence151Test','ReplayPaths152Test','ReturnDiagnosis151Test']
def run(args,log,cwd=r):
 with (r/'build'/log).open('xb') as output:subprocess.run(args,cwd=cwd,env=env,stdout=output,stderr=subprocess.STDOUT,check=True)
 print(log+': passed',flush=True)
args=[str(r/'gradlew.bat'),'--no-daemon','--max-workers=1',':workspace-engine:test']
for c in classes:args+=['--tests','fraymus.workspace.'+c]
run(args,'regression157.log')
count=0
for p in xml.glob('*.xml'):
 e=ET.parse(p).getroot();assert sum(int(e.get(k,'0')) for k in ['failures','errors','skipped'])==0;count+=int(e.get('tests'));shutil.copy2(p,final/p.name)
assert count==44,count
run(['npm.cmd','test'],'desktop157.log',r/'workspace-desktop')
run([str(r/'gradlew.bat'),'--no-daemon','--max-workers=1',':workspace-engine:installDist','--rerun-tasks'],'install157.log')
out=r/'build/measure157-classes';out.mkdir()
run([str(j/'bin/javac.exe'),'-encoding','UTF-8','-cp','workspace-engine/build/install/workspace-engine/lib/*','-d',str(out),'docs/publication157/HarderSources157.java','docs/publication157/ReplayMeasure157.java'],'helpers157.log')
for n in ['workspace-desktop/renderer/association134-panel.js','docs/publication157/study157.cjs']:run(['node','--check',n],Path(n).stem+'-syntax157.log')
print('57 distinct checks, desktop, installation and helper compilation completed; reserved sources not generated.',flush=True)
