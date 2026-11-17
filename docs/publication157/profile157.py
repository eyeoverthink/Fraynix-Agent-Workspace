"""One exposed original156 audit, using its installed classes and an isolated copy."""
from pathlib import Path
import hashlib,json,shutil,subprocess
r=Path.cwd();prior=r.parent/'fraynix-workspace-edition156';out=r/'build/profile157';out.mkdir()
source=prior/'build/study156-exposed150-final/project';project=out/'project'
def inventory(root):return {p.relative_to(root).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in root.rglob('*') if p.is_file()}
before=inventory(source);shutil.copytree(source,project);assert inventory(project)==before
study=json.loads((prior/'build/study156-exposed150-final/result.json').read_bytes());assert study['passed'];audit=study['cases'][0]['auditId']
j=Path('C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot/bin');cp='build/profile157-classes;../fraynix-workspace-edition156/workspace-engine/build/install/workspace-engine/lib/*'
with (out/'run.log').open('xb') as log:
 subprocess.run([str(j/'java.exe'),'-XX:ActiveProcessorCount=2','-Xmx768m','-XX:StartFlightRecording=filename=build/profile157/baseline.jfr,settings=profile,maxsize=32m,dumponexit=true','-cp',cp,'fraymus.workspace.Profile157',str(project),audit,str(out/'receipt.json')],stdout=log,stderr=subprocess.STDOUT,check=True)
assert inventory(source)==before
with (out/'samples.json').open('xb') as log:subprocess.run([str(j/'jfr.exe'),'print','--json','--events','jdk.ExecutionSample','--stack-depth','12',str(out/'baseline.jfr')],stdout=log,check=True)
print('Complete exposed original audit profiled; source unchanged; samples ready.',flush=True)
