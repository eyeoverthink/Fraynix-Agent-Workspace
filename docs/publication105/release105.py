from pathlib import Path
import subprocess,json
r=Path.cwd()
def run(*a,**kw):return subprocess.run(a,check=True,**kw)
def git(*a):return subprocess.check_output(['git',*a],text=True).strip()
assert git('remote','get-url','--push','origin').lower()=='https://github.com/eyeoverthink/fraynix-agent-workspace.git'
for p in ['docs/readmes/105/verification.json','build/documentation-check.json','build/visual105.json','build/visual-review105.json','build/analysis105.json']:
 assert json.loads((r/p).read_text(encoding='utf-8-sig'))['passed'],p
assert git('branch','--show-current')=='edition105'
run('git','-c','gc.auto=0','fetch','origin','main');run('git','merge-base','--is-ancestor','origin/main','HEAD')
run('node','scripts/Study-Build095.cjs')
files=['README-CONTRIBUTORS.md','docs/EDITION_105_CONTRACT.md','AGENTS.md','README.md','README-105.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md','docs/obsidian/00-home.md','docs/obsidian/113-automatic-qualification.md','docs/obsidian/113-automatic-qualification.canvas','docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/105','scripts/Inspect-Qualification105.cjs','scripts/Document-Qualification105.py','scripts/qualification105-guide.html']
run('git','add',*files);run('git','add','-f','docs/readmes/105/original');run('git','diff','--cached','--check')
run('git','-c','gc.auto=0','commit','-m','Document edition 105 automatic qualification, durable recovery and collection-growth costs')
assert not git('status','--porcelain')
release=git('rev-parse','HEAD');print('Release',release,flush=True)
run('python','build/audit-publication105.py','HEAD',release)
run('git','tag','-a','readme-105','-m','Edition 105: automatic qualification with complete original checks and measured collection costs')
with (r/'build/push105.log').open('wb') as f:
 run('git','-c','pack.threads=4','push','--no-thin','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition105','refs/tags/readme-105',stdout=f,stderr=subprocess.STDOUT)
print((r/'build/push105.log').read_text(encoding='utf8'),flush=True)
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition105-main','refs/heads/edition105:refs/verify/edition105-branch','refs/tags/readme-105:refs/verify/edition105-tag')
assert git('rev-parse','refs/verify/edition105-branch')==release
assert git('rev-parse','refs/verify/edition105-tag^{commit}')==release
run('python','build/audit-publication105.py','refs/verify/edition105-main',release)
