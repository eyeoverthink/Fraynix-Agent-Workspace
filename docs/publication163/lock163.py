from pathlib import Path
import subprocess,json,hashlib
r=Path.cwd();p=r/'docs/RECORDED_REPLAY_163_LOCK.json';assert not p.exists()
assert json.loads((r/'build/validation163/summary.json').read_bytes())['passed']
paths=set(json.loads((r/'docs/RECORDED_SMALL_162_LOCK.json').read_bytes())['sourceHashes'])
for command in [['git','diff','--name-only','f9710cba65229efb01e8b28e7b53bc03e31c13e1'],['git','ls-files','--others','--exclude-standard']]:
 paths.update(n for n in subprocess.check_output(command,text=True).splitlines()if n.startswith(('workspace-engine/','workspace-desktop/')))
paths.update('docs/publication163/'+n for n in ['study163.cjs','plan163.md','validate163.py','lock163.py','compare163.py','render163.cjs'])
hashes={n:hashlib.sha256((r/n).read_bytes().replace(b'\r\n',b'\n')).hexdigest()for n in sorted(paths)}
p.write_bytes((json.dumps({'edition':163,'baseline':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),'scope':'Locked before app trials. Complete recorded-object workflow costs; exact source-bound process-local reconstruction reuse, unchanged numerical controller/outcome checks and canonical visible labels. Two alternating full/memo pairs, growing history, three existing recordings. Existing exposed footage only; fresh physical validation remains pending.','sourceHashes':hashes},indent=2)+'\n').encode());print(len(hashes))
