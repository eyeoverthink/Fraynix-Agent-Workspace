from pathlib import Path
import subprocess,json,hashlib
r=Path.cwd();p=r/'docs/RECORDED_SMALL_162_LOCK.json';assert not p.exists()
assert json.loads((r/'build/validation162/summary.json').read_bytes())['passed']
paths=set(json.loads((r/'docs/SMALL_BODY_161_LOCK.json').read_bytes())['sourceHashes'])
for command in [['git','diff','--name-only','3c18de98161490f4fb914cf0d1dfbe3b5151072d'],['git','ls-files','--others','--exclude-standard']]:
 paths.update(n for n in subprocess.check_output(command,text=True).splitlines()if n.startswith(('workspace-engine/','workspace-desktop/')))
paths.update('docs/publication162/'+n for n in ['study162.cjs','plan162.md','validate162.py','lock162.py'])
hashes={n:hashlib.sha256((r/n).read_bytes().replace(b'\r\n',b'\n')).hexdigest()for n in sorted(paths)}
p.write_bytes((json.dumps({'edition':162,'baseline':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),'scope':'Locked before app trials. Recorded object coordinates and conditional bounds drive the unchanged small-body controller through durable chat and retained procedures. Existing exposed footage only; fresh physical validation remains pending.','sourceHashes':hashes},indent=2)+'\n').encode());print(len(hashes))
