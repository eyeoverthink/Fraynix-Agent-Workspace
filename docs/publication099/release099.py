from pathlib import Path
import subprocess,json
r=Path.cwd()
def run(*a,**kw):return subprocess.run(a,check=True,**kw)
def git(*a):return subprocess.check_output(['git',*a],text=True).strip()
assert git('remote','get-url','--push','origin').lower() in ['https://github.com/eyeoverthink/fraynix-agent-workspace.git','git@github.com:eyeoverthink/fraynix-agent-workspace.git']
for p in ['docs/readmes/099/verification.json','build/documentation-check.json','build/visual099.json','build/visual-review099.json']:assert json.loads((r/p).read_text(encoding='utf-8-sig'))['passed'],p
assert git('branch','--show-current')=='edition099'
run('git','-c','gc.auto=0','fetch','origin','main');run('git','merge-base','--is-ancestor','origin/main','HEAD')
files=['README-CONTRIBUTORS.md','docs/EDITION_099_CONTRACT.md','AGENTS.md','README.md','README-099.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md','docs/obsidian/00-home.md','docs/obsidian/107-local-model-workflows.md','docs/obsidian/107-local-model-workflows.canvas','docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/099']
run('git','add',*files);run('git','add','-f','docs/readmes/099/original');run('git','diff','--cached','--check');run('git','-c','gc.auto=0','commit','-m','Document edition 099 layout real local-model tasks and retained originals')
assert not git('status','--porcelain')
release=git('rev-parse','HEAD');print('Release',release,flush=True);run('python','build/audit-publication099.py','HEAD',release)
run('git','tag','-a','readme-099','-m','Edition 099: adjustable chat and verified real local-model world data creation and skill reuse')
with (r/'build/push099.log').open('wb') as f:run('git','-c','pack.threads=4','push','--no-thin','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition099','refs/tags/readme-099',stdout=f,stderr=subprocess.STDOUT)
print((r/'build/push099.log').read_text(encoding='utf8'),flush=True)
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition099-main','refs/heads/edition099:refs/verify/edition099-branch','refs/tags/readme-099:refs/verify/edition099-tag')
assert git('rev-parse','refs/verify/edition099-branch')==release
assert git('rev-parse','refs/verify/edition099-tag^{commit}')==release
run('python','build/audit-publication099.py','refs/verify/edition099-main',release)
