from pathlib import Path
import json,subprocess,re,gzip
r=Path.cwd();run=lambda *args,**kwargs:subprocess.run(args,check=True,**kwargs);git=lambda *args:subprocess.check_output(['git',*args],text=True).strip()
assert git('remote','get-url','--push','origin').lower()=='https://github.com/eyeoverthink/fraynix-agent-workspace.git'
assert git('branch','--show-current')=='edition150'
for name in ['docs/readmes/150/verification.json','build/documentation-check.json','build/visual150.json','build/visual-review150.json']:assert json.loads((r/name).read_text(encoding='utf8'))['passed'],name
patterns=[rb'gh[pousr]_[A-Za-z0-9]{30,}',rb'github_pat_[A-Za-z0-9_]{40,}',rb'sk-[A-Za-z0-9_-]{30,}',rb'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----']
for folder in ['docs/readmes/150','docs/publication150']:
 for p in (r/folder).rglob('*'):
  if p.is_file() and p.suffix!='.png':
   b=gzip.decompress(p.read_bytes()) if p.suffix=='.gz' else p.read_bytes()
   for pattern in patterns:assert not re.search(pattern,b),str(p)
run('git','-c','gc.auto=0','fetch','origin','main');run('git','merge-base','--is-ancestor','origin/main','HEAD')
files=['docs/INTEGRATED_AGENT_ROADMAP.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md','docs/OBSERVATION_150_LOCK.json','AGENTS.md','README.md','README-150.md','README-CONTRIBUTORS.md','docs/ARCHITECTURE.md','docs/DEVELOPMENT_LOG.md','docs/PROJECT_MEMORY.md','docs/publication150/PROTOCOL_150.md','docs/obsidian/00-home.md','docs/obsidian/158-starting-references.md','docs/obsidian/158-starting-references.canvas','docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/150','docs/publication150']
run('git','add',*files)
logs=[str(p.relative_to(r)) for p in (r/'docs/publication150').glob('*.log')]
if logs:run('git','add','-f',*logs)
run('git','diff','--cached','--check','--','.',':!docs/publication150/*.log',':!docs/publication150/*-groundtruth.txt')
run('git','-c','gc.auto=0','commit','-m','Document edition 150 verified starting references and bounded recording')
assert not git('status','--porcelain')
release=git('rev-parse','HEAD');(r/'build/release150.json').write_text(json.dumps({'release':release}),encoding='utf8');print('Release '+release,flush=True)
run('python','docs/publication150/audit-publication150.py','HEAD',release)
run('git','tag','-a','readme-150','-m','Edition 150: verified starting references and bounded recording')
with (r/'build/push150.log').open('wb') as out:run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition150','refs/tags/readme-150',stdout=out,stderr=subprocess.STDOUT)
print((r/'build/push150.log').read_text(encoding='utf8'),flush=True)
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition150-main','refs/heads/edition150:refs/verify/edition150-branch','refs/tags/readme-150:refs/verify/edition150-tag')
assert git('rev-parse','refs/verify/edition150-branch')==release
assert git('rev-parse','refs/verify/edition150-tag^{commit}')==release
run('python','docs/publication150/audit-publication150.py','refs/verify/edition150-main',release)
