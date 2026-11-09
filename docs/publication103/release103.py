from pathlib import Path
import subprocess,json
r=Path.cwd()
def run(*a,**kw):return subprocess.run(a,check=True,**kw)
def git(*a):return subprocess.check_output(['git',*a],text=True).strip()
assert git('remote','get-url','--push','origin').lower()=='https://github.com/eyeoverthink/fraynix-agent-workspace.git'
for p in ['docs/readmes/103/verification.json','build/documentation-check.json','build/visual103.json','build/visual-review103.json','build/analysis103.json']:
 assert json.loads((r/p).read_text(encoding='utf-8-sig'))['passed'],p
assert git('branch','--show-current')=='edition103'
run('git','-c','gc.auto=0','fetch','origin','main');run('git','merge-base','--is-ancestor','origin/main','HEAD')
run('node','scripts/Study-Build095.cjs')
files=['README-CONTRIBUTORS.md','docs/EDITION_103_CONTRACT.md','AGENTS.md','README.md','README-103.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md','docs/obsidian/00-home.md','docs/obsidian/111-verified-implementation-reuse.md','docs/obsidian/111-verified-implementation-reuse.canvas','docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/103','scripts/Document-Implementation103.py','scripts/implementation103-guide.html']
run('git','add',*files);run('git','add','-f','docs/readmes/103/original');run('git','diff','--cached','--check')
run('git','-c','gc.auto=0','commit','-m','Document edition 103 verified implementation reuse and fresh-data results')
assert not git('status','--porcelain')
release=git('rev-parse','HEAD');print('Release',release,flush=True)
run('python','build/audit-publication103.py','HEAD',release)
run('git','tag','-a','readme-103','-m','Edition 103: verified implementations with fresh data and full checks')
with (r/'build/push103.log').open('wb') as f:
 run('git','-c','pack.threads=4','push','--no-thin','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition103','refs/tags/readme-103',stdout=f,stderr=subprocess.STDOUT)
print((r/'build/push103.log').read_text(encoding='utf8'),flush=True)
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition103-main','refs/heads/edition103:refs/verify/edition103-branch','refs/tags/readme-103:refs/verify/edition103-tag')
assert git('rev-parse','refs/verify/edition103-branch')==release
assert git('rev-parse','refs/verify/edition103-tag^{commit}')==release
run('python','build/audit-publication103.py','refs/verify/edition103-main',release)
