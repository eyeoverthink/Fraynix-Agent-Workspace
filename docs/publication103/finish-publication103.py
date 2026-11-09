from pathlib import Path
import json,subprocess,hashlib,re
r=Path.cwd()
def git(*a):return subprocess.check_output(['git',*a],text=True).strip()
def run(*a,**kwargs):return subprocess.run(a,check=True,**kwargs)
release=git('rev-parse','readme-103^{commit}')
assert git('rev-parse','HEAD')==release
receipt=json.loads((r/'docs/PUBLICATION_103.json').read_text(encoding='utf8'))
assert receipt['releaseCommit']==release and receipt['audit']['passed'] and receipt['audit']['remoteFetched']
roots=['AGENTS.md','README.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md','docs/obsidian/111-verified-implementation-reuse.md','docs/PUBLICATION_103.json','docs/publication103']
patterns=[rb'gh[pousr]_[A-Za-z0-9]{30,}',rb'github_pat_[A-Za-z0-9_]{40,}',rb'sk-[A-Za-z0-9_-]{30,}',rb'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----']
for name in roots:
 p=r/name
 for f in (p.rglob('*') if p.is_dir() else [p]):
  if f.is_file() and f.suffix!='.png':
   for pattern in patterns:assert not re.search(pattern,f.read_bytes()),('Credential signature',str(f))
run('git','add',*roots)
logs=[p.relative_to(r).as_posix() for p in (r/'docs/publication103').glob('*.log')]
if logs:run('git','add','-f',*logs)
run('git','diff','--cached','--check','--','.',':!docs/publication103/*.log')
run('git','-c','gc.auto=0','commit','-m','Record fetched edition 103 publication and full evidence verification')
assert not git('status','--porcelain')
head=git('rev-parse','HEAD')
names=git('diff','--name-only',release,head).splitlines()
assert all(n in roots or n.startswith('docs/publication103/') for n in names),names
with (r/'build/push103-metadata.log').open('wb') as out:run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition103',stdout=out,stderr=subprocess.STDOUT)
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition103-final-main','refs/heads/edition103:refs/verify/edition103-final-branch','refs/tags/readme-103:refs/verify/edition103-final-tag')
assert git('rev-parse','refs/verify/edition103-final-main')==head==git('rev-parse','refs/verify/edition103-final-branch')
assert git('rev-parse','refs/verify/edition103-final-tag^{commit}')==release
assert git('rev-parse','refs/verify/edition103-final-tag')==receipt['tagObject']
run('git','merge-base','--is-ancestor',release,'refs/verify/edition103-final-main')
for name in names:
 local=subprocess.check_output(['git','show',head+':'+name]);remote=subprocess.check_output(['git','show','refs/verify/edition103-final-main:'+name]);assert local==remote,name
assert not git('status','--porcelain')
result={'passed':True,'release':release,'finalMain':head,'tag':'readme-103','tagObject':receipt['tagObject'],'verifiedMetadataFiles':len(names),'scope':'Fetched main and edition branch equal final local commit. Annotated tag unchanged at fully audited release. Only enumerated metadata/receipt files differ; every changed Git blob compared byte-for-byte. Canonical sources and frozen snapshots are unchanged from the fetched full-release audit.'}
(r/'build/final-publication103.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf8');print(json.dumps(result))
