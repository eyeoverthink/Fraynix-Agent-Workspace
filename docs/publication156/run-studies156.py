"""Sequential study orchestration. Call only after the method is committed and the build stamped."""
from pathlib import Path
import os,subprocess,json,hashlib
r=Path.cwd();java=Path('C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot/bin/java.exe')
lock=json.loads((r/'docs/OBSERVATION_156_LOCK.json').read_bytes())
for name,h in lock['sourceHashes'].items():assert hashlib.sha256((r/name).read_bytes().replace(b'\r\n',b'\n')).hexdigest()==h,name
def run(args,log,env=None):
 with (r/'build'/log).open('xb') as output:subprocess.run(args,cwd=r,env=env,stdout=output,stderr=subprocess.STDOUT,check=True)
 print(log+': passed',flush=True)
run([str(java),'-XX:ActiveProcessorCount=2','-Xmx512m','-cp','build/measure156-classes;workspace-engine/build/install/workspace-engine/lib/*','fraymus.workspace.HarderSources156','build/reserved156'],'generate156.log')
run(['python','-X','utf8','docs/publication156/verify-sources156.py'],'source-check156.log')
for group in ['generated','exposed150']:
 env=os.environ.copy();env.update(STUDY156_GROUP=group,STUDY156_ATTEMPT='-final')
 run(['node','docs/publication156/study156.cjs'],'study156-'+group+'-final.log',env)
run(['python','-X','utf8','docs/publication156/measure156.py'],'measure156.log')
