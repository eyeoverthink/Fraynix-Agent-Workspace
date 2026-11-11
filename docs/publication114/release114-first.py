from pathlib import Path
import json,subprocess,re,gzip
r=Path.cwd();run=lambda *args:subprocess.run(args,check=True);git=lambda *args:subprocess.check_output(['git',*args],text=True).strip()
assert git('remote','get-url','--push','origin').lower()=='https://github.com/eyeoverthink/fraynix-agent-workspace.git'
assert git('branch','--show-current')=='edition114'
for name in ['docs/readmes/114/verification.json','build/documentation-check.json','build/visual114.json','build/visual-review114.json']:assert json.loads((r/name).read_text(encoding='utf8'))['passed'],name
patterns=[rb'gh[pousr]_[A-Za-z0-9]{30,}',rb'github_pat_[A-Za-z0-9_]{40,}',rb'sk-[A-Za-z0-9_-]{30,}',rb'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----']
for folder in ['docs/readmes/114','docs/publication114']:
 for p in (r/folder).rglob('*'):
  if p.is_file() and p.suffix!='.png':
   b=gzip.decompress(p.read_bytes()) if p.suffix=='.gz' else p.read_bytes()
   for pattern in patterns:assert not re.search(pattern,b),str(p)
run('git','-c','gc.auto=0','fetch','origin','main');run('git','merge-base','--is-ancestor','origin/main','HEAD')
files=['AGENTS.md','README.md','README-114.md','README-CONTRIBUTORS.md','docs/ARCHITECTURE.md','docs/DEVELOPMENT_LOG.md','docs/PROJECT_MEMORY.md','docs/EDITION_114_CONTRACT.md','docs/obsidian/00-home.md','docs/obsidian/122-playable-level-creation.md','docs/obsidian/122-playable-level-creation.canvas','docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/114','docs/publication114']
run('git','add',*files)
run('git','add','-f',*[str(p.relative_to(r)) for p in (r/'docs/publication114').glob('*.log')])
run('git','diff','--cached','--check','--','.',':!docs/publication114/*.log')
run('git','-c','gc.auto=0','commit','-m','Document edition 114 playable level construction with actual runtime evidence')
assert not git('status','--porcelain')
release=git('rev-parse','HEAD');(r/'build/release114.json').write_text(json.dumps({'release':release}),encoding='utf8');print('Release '+release,flush=True)
run('python','docs/publication114/audit-publication114.py','HEAD',release)
run('git','tag','-a','readme-114','-m','Edition 114: source-based playable level construction and actual Game Lab verification')
with (r/'build/push114.log').open('wb') as out:run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition114','refs/tags/readme-114',stdout=out,stderr=subprocess.STDOUT)
print((r/'build/push114.log').read_text(encoding='utf8'),flush=True)
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition114-main','refs/heads/edition114:refs/verify/edition114-branch','refs/tags/readme-114:refs/verify/edition114-tag')
assert git('rev-parse','refs/verify/edition114-branch')==release
assert git('rev-parse','refs/verify/edition114-tag^{commit}')==release
run('python','docs/publication114/audit-publication114.py','refs/verify/edition114-main',release)
