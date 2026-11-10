from pathlib import Path
import subprocess,json
r=Path.cwd()
def run(*a,**kw):return subprocess.run(a,check=True,**kw)
def git(*a):return subprocess.check_output(['git',*a],text=True).strip()
assert git('remote','get-url','--push','origin').lower()=='https://github.com/eyeoverthink/fraynix-agent-workspace.git'
for p in ['docs/readmes/107/verification.json','build/documentation-check.json','build/visual107.json','build/visual-review107.json','build/analysis107.json']:
 assert json.loads((r/p).read_text(encoding='utf-8-sig'))['passed'],p
assert git('branch','--show-current')=='edition107'
run('git','-c','gc.auto=0','fetch','origin','main');run('git','merge-base','--is-ancestor','origin/main','HEAD')
run('node','scripts/Study-Build095.cjs')
files=['README-CONTRIBUTORS.md','docs/EDITION_107_CONTRACT.md','AGENTS.md','README.md','README-107.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md','docs/obsidian/00-home.md','docs/obsidian/115-measured-qualification-refresh.md','docs/obsidian/115-measured-qualification-refresh.canvas','docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/107','scripts/Inspect-Restart107.cjs','scripts/Document-Restart107.py','scripts/restart107-guide.html']
run('git','add',*files);run('git','add','-f','docs/readmes/107/original');run('git','diff','--cached','--check')
run('git','-c','gc.auto=0','commit','-m','Document edition 107 numerical restart and measured qualification refresh')
assert not git('status','--porcelain')
release=git('rev-parse','HEAD');print('Release',release,flush=True)
run('python','build/audit-publication107.py','HEAD',release)
run('git','tag','-a','readme-107','-m','Edition 107: exact numerical recovery and measured qualification refresh')
with (r/'build/push107.log').open('wb') as f:
 run('git','-c','pack.threads=4','push','--no-thin','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition107','refs/tags/readme-107',stdout=f,stderr=subprocess.STDOUT)
print((r/'build/push107.log').read_text(encoding='utf8'),flush=True)
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition107-main','refs/heads/edition107:refs/verify/edition107-branch','refs/tags/readme-107:refs/verify/edition107-tag')
assert git('rev-parse','refs/verify/edition107-branch')==release
assert git('rev-parse','refs/verify/edition107-tag^{commit}')==release
run('python','build/audit-publication107.py','refs/verify/edition107-main',release)
