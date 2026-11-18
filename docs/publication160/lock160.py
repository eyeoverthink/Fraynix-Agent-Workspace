from pathlib import Path
import subprocess,json,hashlib
r=Path.cwd();p=r/'docs/PLANE_ACCURACY_160_LOCK.json';assert not p.exists()
assert json.loads((r/'build/validation160/summary.json').read_bytes())['passed']
paths=set(json.loads((r/'docs/CALIBRATED_159_LOCK.json').read_bytes())['sourceHashes'])
for command in [['git','diff','--name-only','HEAD'],['git','ls-files','--others','--exclude-standard']]:paths.update(n for n in subprocess.check_output(command,text=True).splitlines() if n.startswith(('workspace-engine/','workspace-desktop/')))
paths.update('docs/publication160/'+n for n in ['study160.cjs','plan160.md','validate160.py','lock160.py'])
hashes={n:hashlib.sha256((r/n).read_bytes().replace(b'\r\n',b'\n')).hexdigest()for n in sorted(paths)}
p.write_bytes((json.dumps({'edition':160,'baseline':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),'scope':'Locked before final app trials. Fixed recorded endpoint windows, fresh exact replay, independent tape displacement and conditional error allowances. Correlated pairs remain one episode; no physical accuracy promotion or action authority.','sourceHashes':hashes},indent=2)+'\n').encode())
print(len(hashes))
