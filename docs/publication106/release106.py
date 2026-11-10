from pathlib import Path
import subprocess,json
r=Path.cwd()
def run(*a,**kw):return subprocess.run(a,check=True,**kw)
def git(*a):return subprocess.check_output(['git',*a],text=True).strip()
assert git('remote','get-url','--push','origin').lower()=='https://github.com/eyeoverthink/fraynix-agent-workspace.git'
for p in ['docs/readmes/106/verification.json','build/documentation-check.json','build/visual106.json','build/visual-review106.json','build/analysis106.json']:
 assert json.loads((r/p).read_text(encoding='utf-8-sig'))['passed'],p
assert git('branch','--show-current')=='edition106'
run('git','-c','gc.auto=0','fetch','origin','main');run('git','merge-base','--is-ancestor','origin/main','HEAD')
run('node','scripts/Study-Build095.cjs')
files=['README-CONTRIBUTORS.md','docs/EDITION_106_CONTRACT.md','AGENTS.md','README.md','README-106.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md','docs/obsidian/00-home.md','docs/obsidian/114-original-refusals.md','docs/obsidian/114-original-refusals.canvas','docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/106','scripts/Inspect-Numerical106.cjs','scripts/Document-Numerical106.py','scripts/numerical106-guide.html']
run('git','add',*files);run('git','add','-f','docs/readmes/106/original');run('git','diff','--cached','--check')
run('git','-c','gc.auto=0','commit','-m','Document edition 106 original refusals and fresh numerical verification')
assert not git('status','--porcelain')
release=git('rev-parse','HEAD');print('Release',release,flush=True)
run('python','build/audit-publication106.py','HEAD',release)
run('git','tag','-a','readme-106','-m','Edition 106: original native refusals without planner retries and verified numerical edges')
with (r/'build/push106.log').open('wb') as f:
 run('git','-c','pack.threads=4','push','--no-thin','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition106','refs/tags/readme-106',stdout=f,stderr=subprocess.STDOUT)
print((r/'build/push106.log').read_text(encoding='utf8'),flush=True)
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition106-main','refs/heads/edition106:refs/verify/edition106-branch','refs/tags/readme-106:refs/verify/edition106-tag')
assert git('rev-parse','refs/verify/edition106-branch')==release
assert git('rev-parse','refs/verify/edition106-tag^{commit}')==release
run('python','build/audit-publication106.py','refs/verify/edition106-main',release)
