from pathlib import Path
import subprocess,json,hashlib
r=Path.cwd();p=r/'docs/CALIBRATED_159_LOCK.json';assert not p.exists()
assert json.loads((r/'build/validation159/summary.json').read_bytes())['passed']
paths=set(json.loads((r/'docs/OBSERVATION_158_LOCK.json').read_bytes())['sourceHashes'])
for command in [['git','diff','--name-only','HEAD'],['git','ls-files','--others','--exclude-standard']]:paths.update(n for n in subprocess.check_output(command,text=True).splitlines() if n.startswith(('workspace-engine/','workspace-desktop/')))
paths.update('docs/publication159/'+n for n in ['study159.cjs','plan159.md','validate159.py','lock159.py'])
hashes={n:hashlib.sha256((r/n).read_bytes().replace(b'\r\n',b'\n')).hexdigest()for n in sorted(paths)}
p.write_text(json.dumps({'edition':159,'baseline':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),'scope':'Locked before final app trials. Recorded measured plane and labelled generated references; conditional error allowances, original native control and independent outcomes. Defaults unchanged. Not live pose or statistical physical accuracy.','sourceHashes':hashes},indent=2)+'\n',encoding='utf8')
print(len(hashes))
