from pathlib import Path
import subprocess,json
r=Path.cwd()
def run(*a,**kw):return subprocess.run(a,check=True,**kw)
def git(*a):return subprocess.check_output(['git',*a],text=True).strip()
assert git('remote','get-url','--push','origin').lower()=='https://github.com/eyeoverthink/fraynix-agent-workspace.git'
for p in ['docs/readmes/112/verification.json','build/documentation-check.json','build/visual112.json','build/visual-review112.json','build/analysis112.json']:
 assert json.loads((r/p).read_text(encoding='utf-8-sig'))['passed'],p
assert git('branch','--show-current')=='edition112'
run('git','-c','gc.auto=0','fetch','origin','main');run('git','merge-base','--is-ancestor','origin/main','HEAD')
run('node','scripts/Study-Build095.cjs')
files=['README-CONTRIBUTORS.md','docs/EDITION_112_CONTRACT.md','AGENTS.md','README.md','README-112.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md','docs/obsidian/00-home.md','docs/obsidian/120-completed-chat-history.md','docs/obsidian/120-completed-chat-history.canvas','docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/112','scripts/Inspect-ChatArchive112.cjs','scripts/Document-ChatArchive112.py','scripts/chatarchive112-guide.html']
run('git','add',*files);run('git','add','-f','docs/readmes/112/original');run('git','diff','--cached','--check')
run('git','-c','gc.auto=0','commit','-m','Document edition 112 verifiable completed chat-task history')
assert not git('status','--porcelain')
release=git('rev-parse','HEAD');print('Release',release,flush=True)
run('python','build/audit-publication112.py','HEAD',release)
run('git','tag','-a','readme-112','-m','Edition 112: exact chat snapshots, fresh verification and recovery')
with (r/'build/push112.log').open('wb') as f:
 run('git','-c','pack.threads=4','push','--no-thin','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition112','refs/tags/readme-112',stdout=f,stderr=subprocess.STDOUT)
print((r/'build/push112.log').read_text(encoding='utf8'),flush=True)
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition112-main','refs/heads/edition112:refs/verify/edition112-branch','refs/tags/readme-112:refs/verify/edition112-tag')
assert git('rev-parse','refs/verify/edition112-branch')==release
assert git('rev-parse','refs/verify/edition112-tag^{commit}')==release
run('python','build/audit-publication112.py','refs/verify/edition112-main',release)
