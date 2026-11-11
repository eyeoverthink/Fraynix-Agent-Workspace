from pathlib import Path
import subprocess,json
r=Path.cwd()
def run(*a,**kw):return subprocess.run(a,check=True,**kw)
def git(*a):return subprocess.check_output(['git',*a],text=True).strip()
assert git('remote','get-url','--push','origin').lower()=='https://github.com/eyeoverthink/fraynix-agent-workspace.git'
for p in ['docs/readmes/113/verification.json','build/documentation-check.json','build/visual113.json','build/visual-review113.json','build/analysis113.json']:
 assert json.loads((r/p).read_text(encoding='utf-8-sig'))['passed'],p
assert git('branch','--show-current')=='edition113'
run('git','-c','gc.auto=0','fetch','origin','main');run('git','merge-base','--is-ancestor','origin/main','HEAD')
run('node','scripts/Study-Build095.cjs')
files=['README-CONTRIBUTORS.md', 'docs/EDITION_113_CONTRACT.md', 'docs/APP_REVIEW_113.md', 'AGENTS.md', 'README.md', 'README-113.md', 'docs/PROJECT_MEMORY.md', 'docs/DEVELOPMENT_LOG.md', 'docs/ARCHITECTURE.md', 'docs/obsidian/00-home.md', 'docs/obsidian/121-usable-experience-history.md', 'docs/obsidian/121-usable-experience-history.canvas', 'docs/readmes/INDEX.md', 'docs/readmes/editions.json', 'docs/readmes/113', 'scripts/Inspect-History113.cjs', 'scripts/Document-History113.py', 'scripts/history113-guide.html']
run('git','add',*files);run('git','add','-f','docs/readmes/113/original');run('git','diff','--cached','--check')
run('git','-c','gc.auto=0','commit','-m','Document edition 113 searchable original history and measured growth')
assert not git('status','--porcelain')
release=git('rev-parse','HEAD');print('Release',release,flush=True)
run('python','build/audit-publication113.py','HEAD',release)
run('git','tag','-a','readme-113','-m','Edition 113: usable original history, exact result checks and growing-bank costs')
with (r/'build/push113.log').open('wb') as f:
 run('git','-c','pack.threads=4','push','--no-thin','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition113','refs/tags/readme-113',stdout=f,stderr=subprocess.STDOUT)
print((r/'build/push113.log').read_text(encoding='utf8'),flush=True)
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition113-main','refs/heads/edition113:refs/verify/edition113-branch','refs/tags/readme-113:refs/verify/edition113-tag')
assert git('rev-parse','refs/verify/edition113-branch')==release
assert git('rev-parse','refs/verify/edition113-tag^{commit}')==release
run('python','build/audit-publication113.py','refs/verify/edition113-main',release)
