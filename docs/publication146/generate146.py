"""Generate fixed new controls and exact exposed145 regression pixels after freezing; run serially."""
from pathlib import Path
import json,hashlib,subprocess,os
r=Path.cwd();lock=json.loads((r/'docs/OBSERVATION_146_LOCK.json').read_text(encoding='utf8'))
for n,h in lock['sourceHashes'].items():assert hashlib.sha256((r/n).read_bytes().replace(b'\r\n',b'\n')).hexdigest()==h,n
java='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot/bin/java.exe'
libs=r/'workspace-engine/build/install/workspace-engine/lib';assert libs.exists()
classpath=os.pathsep.join([str(r/'workspace-engine/build/classes/java/test'),str(r/'workspace-engine/build/classes/java/main'),str(libs/'*')])
for clazz,folder in [('ReturnFixtures146','generated146'),('TrajectoryFixtures145','regression146')]:
 out=r/'build'/folder;assert not out.exists(),'Preserve original source generation'
 subprocess.run([java,'-Xmx512m','-XX:ActiveProcessorCount=2','-cp',classpath,'fraymus.workspace.'+clazz,str(out)],check=True)
print('Fixed fresh and separately exposed generated pixels saved')
