from pathlib import Path
import json,subprocess,re,gzip
r=Path.cwd();run=lambda *args,**kwargs:subprocess.run(args,check=True,**kwargs);git=lambda *args:subprocess.check_output(['git',*args],text=True).strip()
assert git('remote','get-url','--push','origin').lower()=='https://github.com/eyeoverthink/fraynix-agent-workspace.git'
assert git('branch','--show-current')=='edition124'
for name in ['docs/readmes/124/verification.json','build/documentation-check.json','build/visual124.json','build/visual-review124.json']:assert json.loads((r/name).read_text(encoding='utf8'))['passed'],name
patterns=[rb'gh[pousr]_[A-Za-z0-9]{30,}',rb'github_pat_[A-Za-z0-9_]{40,}',rb'sk-[A-Za-z0-9_-]{30,}',rb'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----']
for folder in ['docs/readmes/124','docs/publication124']:
 for p in (r/folder).rglob('*'):
  if p.is_file() and p.suffix!='.png':
   b=gzip.decompress(p.read_bytes()) if p.suffix=='.gz' else p.read_bytes()
   for pattern in patterns:assert not re.search(pattern,b),str(p)
run('git','-c','gc.auto=0','fetch','origin','main');run('git','merge-base','--is-ancestor','origin/main','HEAD')
files=['AGENTS.md','README.md','README-124.md','README-CONTRIBUTORS.md','docs/ARCHITECTURE.md','docs/DEVELOPMENT_LOG.md','docs/PROJECT_MEMORY.md','docs/EDITION_124_CONTRACT.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md','docs/obsidian/00-home.md','docs/obsidian/132-budgeted-optional-actions.md','docs/obsidian/132-budgeted-optional-actions.canvas','docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/124','docs/publication124']
run('git','add',*files)
logs=[str(p.relative_to(r)) for p in (r/'docs/publication124').glob('*.log')]
if logs:run('git','add','-f',*logs)
run('git','diff','--cached','--check','--','.',':!docs/publication124/*.log')
run('git','-c','gc.auto=0','commit','-m','Document edition 124 budgeted optional actions and retained verification with actual evidence')
assert not git('status','--porcelain')
release=git('rev-parse','HEAD');(r/'build/release124.json').write_text(json.dumps({'release':release}),encoding='utf8');print('Release '+release,flush=True)
run('python','docs/publication124/audit-publication124.py','HEAD',release)
run('git','tag','-a','readme-124','-m','Edition 124: budgeted optional actions')
with (r/'build/push124.log').open('wb') as out:run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition124','refs/tags/readme-124',stdout=out,stderr=subprocess.STDOUT)
print((r/'build/push124.log').read_text(encoding='utf8'),flush=True)
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition124-main','refs/heads/edition124:refs/verify/edition124-branch','refs/tags/readme-124:refs/verify/edition124-tag')
assert git('rev-parse','refs/verify/edition124-branch')==release
assert git('rev-parse','refs/verify/edition124-tag^{commit}')==release
run('python','docs/publication124/audit-publication124.py','refs/verify/edition124-main',release)
