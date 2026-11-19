from pathlib import Path
import json,hashlib,subprocess,re,gzip
r=Path.cwd();sha=lambda b:hashlib.sha256(b).hexdigest()
def canon(p,binary=False):
 b=p.read_bytes();return b if binary else b.decode('utf-8-sig').replace('\r\n','\n').encode()
assert subprocess.run(['git','show-ref','--quiet','--verify','refs/tags/readme-178']).returncode==1
prior=json.loads(subprocess.check_output(['git','show','e1faaacfa:docs/readmes/editions.json']));current=json.loads((r/'docs/readmes/editions.json').read_bytes());assert current==prior or current['editions'][:-1]==prior['editions']
assert json.loads((r/'docs/readmes/178/verification.json').read_bytes())['passed']
entry={'number':178,'date':'2026-11-18','title':'Verified Relay playtest to clearance dashboard','status':'validated-local','markdown':'README-178.md','markdownSha256':sha(canon(r/'README-178.md')),'companion':'docs/readmes/178/index.html','tag':'readme-178','sourceBaseline':json.loads((r/'docs/readmes/178/verification.json').read_bytes())['source'],'snapshotDirectory':'docs/readmes/178','snapshotFiles':[]}
for p in sorted((r/'docs/readmes/178').rglob('*')):
 if not p.is_file():continue
 binary=p.suffix in ['.png','.gz'];entry['snapshotFiles'].append({'path':p.relative_to(r).as_posix(),'kind':'binary' if binary else 'text','sha256':sha(canon(p,binary))})
 if p.suffix!='.png':
  b=gzip.decompress(p.read_bytes()) if p.suffix=='.gz' else p.read_bytes()
  for pattern in [rb'gh[pousr]_[A-Za-z0-9]{30,}',rb'github_pat_[A-Za-z0-9_]{40,}',rb'sk-[A-Za-z0-9_-]{30,}',rb'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----']:assert not re.search(pattern,b),str(p)
prior['latest']=178;prior['editions'].append(entry);(r/'docs/readmes/editions.json').write_bytes((json.dumps(prior,indent=2,ensure_ascii=False)+'\n').encode());print(json.dumps({'frozenFiles':len(entry['snapshotFiles']),'priorEditionsUnchanged':True}))
