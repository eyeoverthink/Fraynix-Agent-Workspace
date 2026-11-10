from pathlib import Path
import subprocess,json
r=Path.cwd()
def run(*a,**kw):return subprocess.run(a,check=True,**kw)
def git(*a):return subprocess.check_output(['git',*a],text=True).strip()
assert git('remote','get-url','--push','origin').lower()=='https://github.com/eyeoverthink/fraynix-agent-workspace.git'
for p in ['docs/readmes/108/verification.json','build/documentation-check.json','build/visual108.json','build/visual-review108.json','build/analysis108.json']:
 assert json.loads((r/p).read_text(encoding='utf-8-sig'))['passed'],p
assert git('branch','--show-current')=='edition108'
run('git','-c','gc.auto=0','fetch','origin','main');run('git','merge-base','--is-ancestor','origin/main','HEAD')
run('node','scripts/Study-Build095.cjs')
files=['README-CONTRIBUTORS.md','docs/EDITION_108_CONTRACT.md','AGENTS.md','README.md','README-108.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md','docs/obsidian/00-home.md','docs/obsidian/116-explicit-qualification-upgrades.md','docs/obsidian/116-explicit-qualification-upgrades.canvas','docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/108','scripts/Inspect-Transition108.cjs','scripts/Document-Transition108.py','scripts/transition108-guide.html']
run('git','add',*files);run('git','add','-f','docs/readmes/108/original');run('git','diff','--cached','--check')
run('git','-c','gc.auto=0','commit','-m','Document edition 108 explicit qualification upgrades with original evidence')
assert not git('status','--porcelain')
release=git('rev-parse','HEAD');print('Release',release,flush=True)
run('python','build/audit-publication108.py','HEAD',release)
run('git','tag','-a','readme-108','-m','Edition 108: verified coordination upgrades and fresh evidence')
with (r/'build/push108.log').open('wb') as f:
 run('git','-c','pack.threads=4','push','--no-thin','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition108','refs/tags/readme-108',stdout=f,stderr=subprocess.STDOUT)
print((r/'build/push108.log').read_text(encoding='utf8'),flush=True)
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition108-main','refs/heads/edition108:refs/verify/edition108-branch','refs/tags/readme-108:refs/verify/edition108-tag')
assert git('rev-parse','refs/verify/edition108-branch')==release
assert git('rev-parse','refs/verify/edition108-tag^{commit}')==release
run('python','build/audit-publication108.py','refs/verify/edition108-main',release)
