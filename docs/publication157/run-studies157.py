"""Sequential study orchestration. Call only after the method is committed and the build stamped."""
from pathlib import Path
import os,subprocess,json,hashlib
r=Path.cwd();java=Path('C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot/bin/java.exe')
lock=json.loads((r/'docs/OBSERVATION_157_LOCK.json').read_bytes())
for name,h in lock['sourceHashes'].items():assert hashlib.sha256((r/name).read_bytes().replace(b'\r\n',b'\n')).hexdigest()==h,name
def run(args,log,env=None):
 with (r/'build'/log).open('xb') as output:subprocess.run(args,cwd=r,env=env,stdout=output,stderr=subprocess.STDOUT,check=True)
 print(log+': passed',flush=True)
run([str(java),'-XX:ActiveProcessorCount=2','-Xmx512m','-cp','build/measure157-classes;workspace-engine/build/install/workspace-engine/lib/*','fraymus.workspace.HarderSources157','build/reserved157'],'generate157.log')
run(['python','-X','utf8','docs/publication157/verify-sources157.py'],'source-check157.log')
for group in ['generated','exposed150']:
 env=os.environ.copy();env.update(STUDY157_GROUP=group,STUDY157_ATTEMPT='-final')
 run(['node','docs/publication157/study157.cjs'],'study157-'+group+'-final.log',env)
run(['python','-X','utf8','docs/publication157/measure157.py'],'measure157.log')
