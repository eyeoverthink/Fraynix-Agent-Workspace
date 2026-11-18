from pathlib import Path
import json,hashlib,subprocess
r=Path.cwd();p=r/'docs/WORKFLOW_COST_164_LOCK.json';assert not p.exists();assert json.loads((r/'build/validation164/summary.json').read_bytes())['passed']
paths=set(json.loads((r/'docs/RECORDED_REPLAY_163_LOCK.json').read_bytes())['sourceHashes'])
for command in [['git','diff','--name-only','f0f5c292c3106f0f5d464979ceb3df447f3bc632'],['git','ls-files','--others','--exclude-standard']]:paths.update(n for n in subprocess.check_output(command,text=True).splitlines()if n.startswith(('workspace-engine/','workspace-desktop/')))
paths.update('docs/publication164/'+n for n in ['study164.cjs','plan164.md','validate164.py','lock164.py','compare164.py'])
hashes={n:hashlib.sha256((r/n).read_bytes().replace(b'\r\n',b'\n')).hexdigest()for n in sorted(paths)}
native={}
for n in ['SmallBody161','SmallPhysics161','SmallStore161','SmallReport161','SmallVerify161']:
 path='workspace-engine/src/main/java/fraymus/workspace/'+n+'.java';old=subprocess.check_output(['git','show','f0f5c292c:'+path]);assert old.replace(b'\r\n',b'\n')==(r/path).read_bytes().replace(b'\r\n',b'\n');native[path]=hashes[path]
p.write_bytes((json.dumps({'edition':164,'baseline':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),'scope':'Locked before final app trials: eight native actions plus two fresh-data Java summary workflows, two restarts, history2/4/6/8 extra probes. Two alternating on/off pairs. Controller/store/verifier/report source bytes unchanged. All outcome checks retained; diagnostics are not authority.','sourceHashes':hashes,'unchangedNativeSources':native},indent=2)+'\n').encode());print(len(hashes))
