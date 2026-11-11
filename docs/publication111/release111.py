from pathlib import Path
import subprocess,json
r=Path.cwd()
def run(*a,**kw):return subprocess.run(a,check=True,**kw)
def git(*a):return subprocess.check_output(['git',*a],text=True).strip()
assert git('remote','get-url','--push','origin').lower()=='https://github.com/eyeoverthink/fraynix-agent-workspace.git'
for p in ['docs/readmes/111/verification.json','build/documentation-check.json','build/visual111.json','build/visual-review111.json','build/analysis111.json']:
 assert json.loads((r/p).read_text(encoding='utf-8-sig'))['passed'],p
assert git('branch','--show-current')=='edition111'
run('git','-c','gc.auto=0','fetch','origin','main');run('git','merge-base','--is-ancestor','origin/main','HEAD')
run('node','scripts/Study-Build095.cjs')
files=['README-CONTRIBUTORS.md','docs/EDITION_111_CONTRACT.md','AGENTS.md','README.md','README-111.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md','docs/obsidian/00-home.md','docs/obsidian/119-completed-child-goal-archives.md','docs/obsidian/119-completed-child-goal-archives.canvas','docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/111','scripts/Inspect-GoalArchive111.cjs','scripts/Document-GoalArchive111.py','scripts/goalarchive111-guide.html']
run('git','add',*files);run('git','add','-f','docs/readmes/111/original');run('git','diff','--cached','--check')
run('git','-c','gc.auto=0','commit','-m','Document edition 111 verifiable completed child-goal archives')
assert not git('status','--porcelain')
release=git('rev-parse','HEAD');print('Release',release,flush=True)
run('python','build/audit-publication111.py','HEAD',release)
run('git','tag','-a','readme-111','-m','Edition 111: exact child-goal archives, fresh verification and recovery')
with (r/'build/push111.log').open('wb') as f:
 run('git','-c','pack.threads=4','push','--no-thin','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition111','refs/tags/readme-111',stdout=f,stderr=subprocess.STDOUT)
print((r/'build/push111.log').read_text(encoding='utf8'),flush=True)
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition111-main','refs/heads/edition111:refs/verify/edition111-branch','refs/tags/readme-111:refs/verify/edition111-tag')
assert git('rev-parse','refs/verify/edition111-branch')==release
assert git('rev-parse','refs/verify/edition111-tag^{commit}')==release
run('python','build/audit-publication111.py','refs/verify/edition111-main',release)
