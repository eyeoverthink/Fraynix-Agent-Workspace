"""Sequential setup after Java regressions; never overlaps heavy jobs."""
from pathlib import Path
import subprocess,os,json,hashlib
r=Path.cwd();env=os.environ.copy();env['JAVA_HOME']='C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot';env['PYTHONUTF8']='1';run=lambda *a:subprocess.run(a,check=True,env=env)
assert json.loads((r/'build/java144-final.json').read_text())['exit']==0
assert not (r/'build/generated144').exists(),'Retain previous attempts; do not overwrite source generation'
run(env['JAVA_HOME']+'/bin/java.exe','-Xmx512m','-XX:ActiveProcessorCount=2','-cp','workspace-engine/build/classes/java/test;workspace-engine/build/classes/java/main;J:/Codex-Work/fraynix-workspace-edition143/workspace-engine/build/install/workspace-engine/lib/*','fraymus.workspace.AmbiguityFixtures144','build/generated144')
run('python','-X','utf8','docs/publication144/prepare144.py')
run('git','add','docs/publication144/source144.json','docs/publication144/annotation-integrity144.json',*[str(p.relative_to(r)) for p in (r/'docs/publication144').glob('*-labels.json')])
run('git','diff','--cached','--check');run('git','-c','gc.auto=0','commit','-m','Record edition 144 fixed source labels before evaluation')
run('python','-X','utf8','docs/publication144/validate144.py','package')
run('python','-X','utf8','docs/publication144/validate144.py','desktop')
with (r/'build/app144.log').open('wb') as log:subprocess.run(['node','workspace-desktop/test/ambiguity144-integration.cjs'],env=env,stdout=log,stderr=subprocess.STDOUT,check=True)
print('Actual app qualification completed',flush=True)
