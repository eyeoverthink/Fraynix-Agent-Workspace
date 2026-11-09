from pathlib import Path
import subprocess,json
r=Path.cwd()
def run(*a,**kw):return subprocess.run(a,check=True,**kw)
def git(*a):return subprocess.check_output(['git',*a],text=True).strip()
assert git('remote','get-url','--push','origin').lower() in ['https://github.com/eyeoverthink/fraynix-agent-workspace.git','git@github.com:eyeoverthink/fraynix-agent-workspace.git']
for p in ['docs/readmes/102/verification.json','build/documentation-check.json','build/visual102.json','build/visual-review102.json']:assert json.loads((r/p).read_text(encoding='utf-8-sig'))['passed'],p
assert git('branch','--show-current')=='edition102'
run('git','-c','gc.auto=0','fetch','origin','main');run('git','merge-base','--is-ancestor','origin/main','HEAD')
files=['README-CONTRIBUTORS.md','docs/EDITION_102_CONTRACT.md','AGENTS.md','README.md','README-102.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md','docs/obsidian/00-home.md','docs/obsidian/110-measured-content-procedures.md','docs/obsidian/110-measured-content-procedures.canvas','docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/102']
run('git','add',*files);run('git','add','-f','docs/readmes/102/original');run('git','diff','--cached','--check');run('git','-c','gc.auto=0','commit','-m','Document edition 102 complete costs and verified motion summaries')
assert not git('status','--porcelain')
release=git('rev-parse','HEAD');print('Release',release,flush=True);run('python','build/audit-publication102.py','HEAD',release)
run('git','tag','-a','readme-102','-m','Edition 102: complete workflow costs and source-verified motion summaries')
with (r/'build/push102.log').open('wb') as f:run('git','-c','pack.threads=4','push','--no-thin','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition102','refs/tags/readme-102',stdout=f,stderr=subprocess.STDOUT)
print((r/'build/push102.log').read_text(encoding='utf8'),flush=True)
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition102-main','refs/heads/edition102:refs/verify/edition102-branch','refs/tags/readme-102:refs/verify/edition102-tag')
assert git('rev-parse','refs/verify/edition102-branch')==release
assert git('rev-parse','refs/verify/edition102-tag^{commit}')==release
run('python','build/audit-publication102.py','refs/verify/edition102-main',release)
