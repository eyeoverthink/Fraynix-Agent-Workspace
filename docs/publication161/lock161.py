from pathlib import Path
import subprocess,json,hashlib
r=Path.cwd();p=r/'docs/SMALL_BODY_161_LOCK.json';assert not p.exists()
assert json.loads((r/'build/validation161/summary.json').read_bytes())['passed']
paths=set(json.loads((r/'docs/PLANE_ACCURACY_160_LOCK.json').read_bytes())['sourceHashes'])
for command in [['git','diff','--name-only','HEAD'],['git','ls-files','--others','--exclude-standard']]:
 paths.update(n for n in subprocess.check_output(command,text=True).splitlines()if n.startswith(('workspace-engine/','workspace-desktop/')))
paths.update('docs/publication161/'+n for n in ['study161.cjs','motion161.cjs','plan161.md','validate161.py','lock161.py'])
hashes={n:hashlib.sha256((r/n).read_bytes().replace(b'\r\n',b'\n')).hexdigest()for n in sorted(paths)}
p.write_bytes((json.dumps({'edition':161,'baseline':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),'scope':'Locked before app trials. New explicit small-body domain; all old defaults retained. Actual native motion and independent report verification. Existing exposed physical episode plus two new generated endpoint-labelled motion sequences; fresh physical validation remains pending.','sourceHashes':hashes},indent=2)+'\n').encode());print(len(hashes))
