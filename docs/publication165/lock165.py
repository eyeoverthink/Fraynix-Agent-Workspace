from pathlib import Path
import json,hashlib,subprocess
r=Path.cwd();p=r/'docs/EVIDENCE_REUSE_165_LOCK.json';assert not p.exists();assert json.loads((r/'build/validation165/summary.json').read_bytes())['passed']
paths=set(json.loads((r/'docs/WORKFLOW_COST_164_LOCK.json').read_bytes())['sourceHashes'])
for command in [['git','diff','--name-only','100518181e74a3324ce2bc65b328b10ff0e61be5'],['git','ls-files','--others','--exclude-standard']]:paths.update(n for n in subprocess.check_output(command,text=True).splitlines()if n.startswith(('workspace-engine/','workspace-desktop/')))
paths.update('docs/publication165/'+n for n in ['study165.cjs','plan165.md','validate165.py','lock165.py','compare165.py'])
hashes={n:hashlib.sha256((r/n).read_bytes().replace(b'\r\n',b'\n')).hexdigest()for n in sorted(paths)}
native={}
for n in ['SmallBody161','SmallPhysics161','SmallStore161','SmallReport161','SmallVerify161']:
 path='workspace-engine/src/main/java/fraymus/workspace/'+n+'.java';old=subprocess.check_output(['git','show','100518181:'+path]);assert old.replace(b'\r\n',b'\n')==(r/path).read_bytes().replace(b'\r\n',b'\n');native[path]=hashes[path]
p.write_bytes((json.dumps({'edition':165,'baseline':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),'scope':'Locked before final app trials: eight native actions plus two fresh-data Java summary workflows, two restarts, history2/4/6/8 extra probes. Two alternating reuse/full pairs, plus two untouched published164 runs. Native execution and acceptance remain original; reused checks have fresh complete witnesses before/after. Controller/store/verifier/report source bytes unchanged. All outcome checks retained; diagnostics are not authority.','sourceHashes':hashes,'unchangedNativeSources':native},indent=2)+'\n').encode());print(len(hashes))
