from pathlib import Path
import json,hashlib,subprocess,gzip,re
r=Path.cwd();parent='62c62fda3be73b823ce6139ae5d74007ee03ac5d'
def raw(p,binary=False):
 b=p.read_bytes();return b if binary else b.decode('utf-8-sig').replace('\r\n','\n').encode()
def sha(b):return hashlib.sha256(b).hexdigest()
assert json.loads((r/'docs/readmes/133/verification.json').read_text(encoding='utf8'))['passed']
prior=json.loads(subprocess.check_output(['git','show',parent+':docs/readmes/editions.json']).decode('utf8'));assert prior['latest']==132
p=r/'docs/readmes/editions.json';current=json.loads(p.read_text(encoding='utf-8-sig'))
if current['latest']==133:
 assert current['editions'][:-1]==prior['editions'];assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-133']).returncode==1
else:assert current==prior
entry={'number':133,'date':'2026-11-12','title':'Association and visual support candidates','status':'validated-local','markdown':'README-133.md','markdownSha256':sha(raw(r/'README-133.md')),'companion':'docs/readmes/133/index.html','tag':'readme-133','sourceBaseline':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),'snapshotDirectory':'docs/readmes/133','snapshotFiles':[]}
patterns=[rb'gh[pousr]_[A-Za-z0-9]{30,}',rb'github_pat_[A-Za-z0-9_]{40,}',rb'sk-[A-Za-z0-9_-]{30,}',rb'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----']
for f in sorted((r/'docs/readmes/133').rglob('*')):
 if not f.is_file():continue
 binary=f.suffix in ['.png','.gz'];entry['snapshotFiles'].append({'path':f.relative_to(r).as_posix(),'kind':'binary' if binary else 'text','sha256':sha(raw(f,binary))})
 if f.suffix!='.png':
  b=gzip.decompress(f.read_bytes()) if f.suffix=='.gz' else f.read_bytes()
  for pattern in patterns:assert not re.search(pattern,b),('Credential signature found',str(f))
prior['latest']=133;prior['editions'].append(entry);p.write_text(json.dumps(prior,indent=2,ensure_ascii=False)+'\n',encoding='utf8');print(json.dumps({'frozenFiles':len(entry['snapshotFiles']),'priorEditionsUnchanged':True}))
