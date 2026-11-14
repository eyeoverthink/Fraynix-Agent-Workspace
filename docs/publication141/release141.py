from pathlib import Path
import json,subprocess,re,gzip
r=Path.cwd();run=lambda *args,**kwargs:subprocess.run(args,check=True,**kwargs);git=lambda *args:subprocess.check_output(['git',*args],text=True).strip()
assert git('remote','get-url','--push','origin').lower()=='https://github.com/eyeoverthink/fraynix-agent-workspace.git'
assert git('branch','--show-current')=='edition141'
for name in ['docs/readmes/141/verification.json','build/documentation-check.json','build/visual141.json','build/visual-review141.json']:assert json.loads((r/name).read_text(encoding='utf8'))['passed'],name
patterns=[rb'gh[pousr]_[A-Za-z0-9]{30,}',rb'github_pat_[A-Za-z0-9_]{40,}',rb'sk-[A-Za-z0-9_-]{30,}',rb'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----']
for folder in ['docs/readmes/141','docs/publication141']:
 for p in (r/folder).rglob('*'):
  if p.is_file() and p.suffix!='.png':
   b=gzip.decompress(p.read_bytes()) if p.suffix=='.gz' else p.read_bytes()
   for pattern in patterns:assert not re.search(pattern,b),str(p)
run('git','-c','gc.auto=0','fetch','origin','main');run('git','merge-base','--is-ancestor','origin/main','HEAD')
files=['AGENTS.md','README.md','README-141.md','README-CONTRIBUTORS.md','docs/ARCHITECTURE.md','docs/DEVELOPMENT_LOG.md','docs/PROJECT_MEMORY.md','docs/EDITION_141_CONTRACT.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md','docs/INTEGRATED_AGENT_ROADMAP.md','docs/obsidian/00-home.md','docs/obsidian/149-loss-recovery.md','docs/obsidian/149-loss-recovery.canvas','docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/141','docs/publication141']
run('git','add',*files)
logs=[str(p.relative_to(r)) for p in (r/'docs/publication141').glob('*.log')]
if logs:run('git','add','-f',*logs)
run('git','diff','--cached','--check','--','.',':!docs/publication141/*.log',':!docs/publication141/*-groundtruth.txt')
run('git','-c','gc.auto=0','commit','-m','Document edition 141 recovery after loss with independent retained evidence')
assert not git('status','--porcelain')
release=git('rev-parse','HEAD');(r/'build/release141.json').write_text(json.dumps({'release':release}),encoding='utf8');print('Release '+release,flush=True)
run('python','docs/publication141/audit-publication141.py','HEAD',release)
run('git','tag','-a','readme-141','-m','Edition 141: recovery after loss with independent evidence')
with (r/'build/push141.log').open('wb') as out:run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition141','refs/tags/readme-141',stdout=out,stderr=subprocess.STDOUT)
print((r/'build/push141.log').read_text(encoding='utf8'),flush=True)
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition141-main','refs/heads/edition141:refs/verify/edition141-branch','refs/tags/readme-141:refs/verify/edition141-tag')
assert git('rev-parse','refs/verify/edition141-branch')==release
assert git('rev-parse','refs/verify/edition141-tag^{commit}')==release
run('python','docs/publication141/audit-publication141.py','refs/verify/edition141-main',release)
