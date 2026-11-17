"""Serial complete forced reconstruction comparisons, including every original verifier."""
from pathlib import Path
import json,subprocess
r=Path.cwd();java=Path('C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot/bin/java.exe')
for group in ['generated','exposed150']:
 study=json.loads((r/f'build/study157-{group}-final/result.json').read_bytes());assert study['passed']
 for i,case in enumerate(study['cases']):
  name='camera' if group=='exposed150' else 'generated' if i==0 else case['name']
  repeats=2 if group=='exposed150' else 3 if i==0 else 1
  out=r/f'build/replay157-{name}.json';assert not out.exists(),'Preserve original completed measurements'
  subprocess.run([str(java),'-XX:ActiveProcessorCount=2','-Xmx768m','-cp','build/measure157-classes;workspace-engine/build/install/workspace-engine/lib/*','fraymus.workspace.ReplayMeasure157',str(r/f'build/study157-{group}-final/project'),case['result']['experiment']['id'],str(out),str(repeats)],check=True)
  print(name+': exact complete reconstruction comparison passed',flush=True)
subprocess.run(['python','-X','utf8','docs/publication157/qualify157.py'],check=True)
