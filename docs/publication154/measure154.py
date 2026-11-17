"""Run bounded complete-request comparisons serially after the actual apps have closed."""
from pathlib import Path
import json,subprocess
r=Path.cwd();java=Path('C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot/bin/java.exe')
for group,name,repeats in [('generated','generated',3),('exposed150','camera',1)]:
 study=json.loads((r/f'build/study154-{group}-final/result.json').read_bytes());assert study['passed']
 case=study['cases'][0];out=r/f'build/replay154-{name}.json';assert not out.exists(),'Preserve the completed original measurement'
 subprocess.run([str(java),'-XX:ActiveProcessorCount=2','-Xmx768m','-cp','build/measure154-classes;workspace-engine/build/install/workspace-engine/lib/*','fraymus.workspace.ReplayMeasure154',str(r/f'build/study154-{group}-final/project'),case['result']['experiment']['id'],str(out),str(repeats)],check=True)
 print(name+': complete forced comparison passed',flush=True)
subprocess.run(['python','-X','utf8','docs/publication154/qualify154.py'],check=True)
