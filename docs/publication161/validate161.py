from pathlib import Path
import subprocess,os,json,shutil,xml.etree.ElementTree as E
r=Path.cwd();env=os.environ.copy();env['JAVA_HOME']='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot';env['PATH']=env['JAVA_HOME']+'/bin;'+env['PATH'];classes=['SmallBody161Test','PlaneAccuracy160Test','Alignment041Test','PlaneCalibrationTest','Calibrated159Test','CalibratedClearance159Test','WorldPhysicsTest']
args=[str(r/'gradlew.bat'),'--no-daemon','--max-workers=1',':workspace-engine:test']
for c in classes:args+=['--tests','fraymus.workspace.'+c]
def run(args,n,cwd=r):
 with (r/'build'/n).open('wb')as f:subprocess.run(args,cwd=cwd,env=env,stdout=f,stderr=subprocess.STDOUT,check=True)
run(args,'java161-final.log');out=r/'build/validation161/final';out.mkdir(parents=True,exist_ok=True);count=0
for p in (r/'workspace-engine/build/test-results/test').glob('*.xml'):
 e=E.parse(p).getroot();assert sum(int(e.get(k,'0'))for k in ['failures','errors','skipped'])==0;count+=int(e.get('tests'));shutil.copy2(p,out/p.name)
(r/'build/validation161/summary.json').write_text(json.dumps({'passed':True,'java':count,'classes':classes},indent=2)+'\n');print('Java passed '+str(count),flush=True)
run(['npm.cmd','test'],'desktop161-final.log',r/'workspace-desktop');print('Desktop passed',flush=True)
run([str(r/'gradlew.bat'),'--no-daemon','--max-workers=1',':workspace-engine:installDist'],'install161.log');print('Installed',flush=True)
