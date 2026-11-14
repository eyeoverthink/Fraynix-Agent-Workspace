from pathlib import Path
import json,subprocess,re,gzip
r=Path.cwd();run=lambda *args,**kwargs:subprocess.run(args,check=True,**kwargs);git=lambda *args:subprocess.check_output(['git',*args],text=True).strip()
assert git('remote','get-url','--push','origin').lower()=='https://github.com/eyeoverthink/fraynix-agent-workspace.git'
assert git('branch','--show-current')=='edition146'
for name in ['docs/readmes/146/verification.json','build/documentation-check.json','build/visual146.json','build/visual-review146.json']:assert json.loads((r/name).read_text(encoding='utf8'))['passed'],name
patterns=[rb'gh[pousr]_[A-Za-z0-9]{30,}',rb'github_pat_[A-Za-z0-9_]{40,}',rb'sk-[A-Za-z0-9_-]{30,}',rb'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----']
for folder in ['docs/readmes/146','docs/publication146']:
 for p in (r/folder).rglob('*'):
  if p.is_file() and p.suffix!='.png':
   b=gzip.decompress(p.read_bytes()) if p.suffix=='.gz' else p.read_bytes()
   for pattern in patterns:assert not re.search(pattern,b),str(p)
run('git','-c','gc.auto=0','fetch','origin','main');run('git','merge-base','--is-ancestor','origin/main','HEAD')
files=['docs/.gitattributes','docs/OBSERVATION_146_LOCK.json','AGENTS.md','README.md','README-146.md','README-CONTRIBUTORS.md','docs/ARCHITECTURE.md','docs/DEVELOPMENT_LOG.md','docs/PROJECT_MEMORY.md','docs/EDITION_146_CONTRACT.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md','docs/INTEGRATED_AGENT_ROADMAP.md','docs/obsidian/00-home.md','docs/obsidian/154-return-continuity.md','docs/obsidian/154-return-continuity.canvas','docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/146','docs/publication146']
run('git','add',*files)
logs=[str(p.relative_to(r)) for p in (r/'docs/publication146').glob('*.log')]
if logs:run('git','add','-f',*logs)
run('git','diff','--cached','--check','--','.',':!docs/publication146/*.log',':!docs/publication146/*-groundtruth.txt')
run('git','-c','gc.auto=0','commit','-m','Document edition 146 return continuity without a competitor warning')
assert not git('status','--porcelain')
release=git('rev-parse','HEAD');(r/'build/release146.json').write_text(json.dumps({'release':release}),encoding='utf8');print('Release '+release,flush=True)
run('python','docs/publication146/audit-publication146.py','HEAD',release)
run('git','tag','-a','readme-146','-m','Edition 146: verified return continuity')
with (r/'build/push146.log').open('wb') as out:run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition146','refs/tags/readme-146',stdout=out,stderr=subprocess.STDOUT)
print((r/'build/push146.log').read_text(encoding='utf8'),flush=True)
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition146-main','refs/heads/edition146:refs/verify/edition146-branch','refs/tags/readme-146:refs/verify/edition146-tag')
assert git('rev-parse','refs/verify/edition146-branch')==release
assert git('rev-parse','refs/verify/edition146-tag^{commit}')==release
run('python','docs/publication146/audit-publication146.py','refs/verify/edition146-main',release)
