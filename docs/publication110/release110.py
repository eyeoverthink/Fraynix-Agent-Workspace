from pathlib import Path
import subprocess,json
r=Path.cwd()
def run(*a,**kw):return subprocess.run(a,check=True,**kw)
def git(*a):return subprocess.check_output(['git',*a],text=True).strip()
assert git('remote','get-url','--push','origin').lower()=='https://github.com/eyeoverthink/fraynix-agent-workspace.git'
for p in ['docs/readmes/110/verification.json','build/documentation-check.json','build/visual110.json','build/visual-review110.json','build/analysis110.json']:
 assert json.loads((r/p).read_text(encoding='utf-8-sig'))['passed'],p
assert git('branch','--show-current')=='edition110'
run('git','-c','gc.auto=0','fetch','origin','main');run('git','merge-base','--is-ancestor','origin/main','HEAD')
run('node','scripts/Study-Build095.cjs')
files=['README-CONTRIBUTORS.md','docs/EDITION_110_CONTRACT.md','AGENTS.md','README.md','README-110.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md','docs/obsidian/00-home.md','docs/obsidian/118-verifiable-workflow-archives.md','docs/obsidian/118-verifiable-workflow-archives.canvas','docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/110','scripts/Inspect-Archive110.cjs','scripts/Document-Archive110.py','scripts/archive110-guide.html']
run('git','add',*files);run('git','add','-f','docs/readmes/110/original');run('git','diff','--cached','--check')
run('git','-c','gc.auto=0','commit','-m','Document edition 110 verifiable completed workflow archives')
assert not git('status','--porcelain')
release=git('rev-parse','HEAD');print('Release',release,flush=True)
run('python','build/audit-publication110.py','HEAD',release)
run('git','tag','-a','readme-110','-m','Edition 110: exact workflow archives, fresh verification and recovery')
with (r/'build/push110.log').open('wb') as f:
 run('git','-c','pack.threads=4','push','--no-thin','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition110','refs/tags/readme-110',stdout=f,stderr=subprocess.STDOUT)
print((r/'build/push110.log').read_text(encoding='utf8'),flush=True)
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition110-main','refs/heads/edition110:refs/verify/edition110-branch','refs/tags/readme-110:refs/verify/edition110-tag')
assert git('rev-parse','refs/verify/edition110-branch')==release
assert git('rev-parse','refs/verify/edition110-tag^{commit}')==release
run('python','build/audit-publication110.py','refs/verify/edition110-main',release)
