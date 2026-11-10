from pathlib import Path
import subprocess,json
r=Path.cwd()
def run(*a,**kw):return subprocess.run(a,check=True,**kw)
def git(*a):return subprocess.check_output(['git',*a],text=True).strip()
assert git('remote','get-url','--push','origin').lower()=='https://github.com/eyeoverthink/fraynix-agent-workspace.git'
for p in ['docs/readmes/104/verification.json','build/documentation-check.json','build/visual104.json','build/visual-review104.json','build/analysis104.json']:
 assert json.loads((r/p).read_text(encoding='utf-8-sig'))['passed'],p
assert git('branch','--show-current')=='edition104'
run('git','-c','gc.auto=0','fetch','origin','main');run('git','merge-base','--is-ancestor','origin/main','HEAD')
run('node','scripts/Study-Build095.cjs')
files=['README-CONTRIBUTORS.md','docs/EDITION_104_CONTRACT.md','AGENTS.md','README.md','README-104.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md','docs/obsidian/00-home.md','docs/obsidian/112-input-driven-summaries.md','docs/obsidian/112-input-driven-summaries.canvas','docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/104','scripts/Document-Summary104.py','scripts/summary104-guide.html']
run('git','add',*files);run('git','add','-f','docs/readmes/104/original');run('git','diff','--cached','--check')
run('git','-c','gc.auto=0','commit','-m','Document edition 104 input-driven summaries, varied qualification and fresh-data results')
assert not git('status','--porcelain')
release=git('rev-parse','HEAD');print('Release',release,flush=True)
run('python','build/audit-publication104.py','HEAD',release)
run('git','tag','-a','readme-104','-m','Edition 104: input-driven summaries with varied qualification and full checks')
with (r/'build/push104.log').open('wb') as f:
 run('git','-c','pack.threads=4','push','--no-thin','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition104','refs/tags/readme-104',stdout=f,stderr=subprocess.STDOUT)
print((r/'build/push104.log').read_text(encoding='utf8'),flush=True)
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition104-main','refs/heads/edition104:refs/verify/edition104-branch','refs/tags/readme-104:refs/verify/edition104-tag')
assert git('rev-parse','refs/verify/edition104-branch')==release
assert git('rev-parse','refs/verify/edition104-tag^{commit}')==release
run('python','build/audit-publication104.py','refs/verify/edition104-main',release)
