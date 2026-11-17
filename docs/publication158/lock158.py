"""Freeze final implementation and study before connected application evaluation."""
from pathlib import Path
import subprocess,json,hashlib
r=Path.cwd();p=r/'docs/OBSERVATION_158_LOCK.json';assert not p.exists()
assert json.loads((r/'build/validation158/summary.json').read_bytes())['passed']
paths=set(json.loads((r/'docs/OBSERVATION_157_LOCK.json').read_bytes())['sourceHashes'])
paths.update(x for x in subprocess.check_output(['git','diff','--name-only'],text=True).splitlines() if x.startswith(('workspace-engine/','workspace-desktop/')))
paths.update(x for x in subprocess.check_output(['git','ls-files','--others','--exclude-standard'],text=True).splitlines() if x.startswith(('workspace-engine/','workspace-desktop/')))
paths.update('docs/publication158/'+n for n in ['study158.cjs','plan158.md','validate158.py','lock158.py'])
hashes={n:hashlib.sha256((r/n).read_bytes().replace(b'\r\n',b'\n')).hexdigest()for n in sorted(paths)}
p.write_bytes((json.dumps({'edition':158,'baseline':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),'scope':'Frozen before fresh connected task executions on exposed recorded sources. Full original replay, world binding, explicit browser contract, uncertainty-aware report and independent outcome verification. No new tracking/defaults/physical authority.','sourceHashes':hashes},indent=2)+'\n').encode());print(len(hashes))
