from pathlib import Path
import json,subprocess,re,gzip
r=Path.cwd();run=lambda *args,**kwargs:subprocess.run(args,check=True,**kwargs);git=lambda *args:subprocess.check_output(['git',*args],text=True).strip()
assert git('remote','get-url','--push','origin').lower()=='https://github.com/eyeoverthink/fraynix-agent-workspace.git'
assert git('branch','--show-current')=='edition118'
for name in ['docs/readmes/118/verification.json','build/documentation-check.json','build/visual118.json','build/visual-review118.json']:assert json.loads((r/name).read_text(encoding='utf8'))['passed'],name
patterns=[rb'gh[pousr]_[A-Za-z0-9]{30,}',rb'github_pat_[A-Za-z0-9_]{40,}',rb'sk-[A-Za-z0-9_-]{30,}',rb'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----']
for folder in ['docs/readmes/118','docs/publication118']:
 for p in (r/folder).rglob('*'):
  if p.is_file() and p.suffix!='.png':
   b=gzip.decompress(p.read_bytes()) if p.suffix=='.gz' else p.read_bytes()
   for pattern in patterns:assert not re.search(pattern,b),str(p)
run('git','-c','gc.auto=0','fetch','origin','main');run('git','merge-base','--is-ancestor','origin/main','HEAD')
files=['AGENTS.md','README.md','README-118.md','README-CONTRIBUTORS.md','docs/ARCHITECTURE.md','docs/DEVELOPMENT_LOG.md','docs/PROJECT_MEMORY.md','docs/EDITION_118_CONTRACT.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md','docs/obsidian/00-home.md','docs/obsidian/126-risk-aware-game-control.md','docs/obsidian/126-risk-aware-game-control.canvas','docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/118','docs/publication118']
run('git','add',*files)
logs=[str(p.relative_to(r)) for p in (r/'docs/publication118').glob('*.log')]
if logs:run('git','add','-f',*logs)
run('git','diff','--cached','--check','--','.',':!docs/publication118/*.log')
run('git','-c','gc.auto=0','commit','-m','Document edition 118 enemy challenges, risk-based pickups and retained verification with actual evidence')
assert not git('status','--porcelain')
release=git('rev-parse','HEAD');(r/'build/release118.json').write_text(json.dumps({'release':release}),encoding='utf8');print('Release '+release,flush=True)
run('python','docs/publication118/audit-publication118.py','HEAD',release)
run('git','tag','-a','readme-118','-m','Edition 118: input-only enemy challenges and measured risk decisions')
with (r/'build/push118.log').open('wb') as out:run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition118','refs/tags/readme-118',stdout=out,stderr=subprocess.STDOUT)
print((r/'build/push118.log').read_text(encoding='utf8'),flush=True)
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition118-main','refs/heads/edition118:refs/verify/edition118-branch','refs/tags/readme-118:refs/verify/edition118-tag')
assert git('rev-parse','refs/verify/edition118-branch')==release
assert git('rev-parse','refs/verify/edition118-tag^{commit}')==release
run('python','docs/publication118/audit-publication118.py','refs/verify/edition118-main',release)
