from pathlib import Path
import subprocess,json
r=Path.cwd()
def run(*a,**kw):return subprocess.run(a,check=True,**kw)
def git(*a):return subprocess.check_output(['git',*a],text=True).strip()
assert git('remote','get-url','--push','origin').lower() in ['https://github.com/eyeoverthink/fraynix-agent-workspace.git','git@github.com:eyeoverthink/fraynix-agent-workspace.git']
for p in ['docs/readmes/101/verification.json','build/documentation-check.json','build/visual101.json','build/visual-review101.json']:assert json.loads((r/p).read_text(encoding='utf-8-sig'))['passed'],p
assert git('branch','--show-current')=='edition101'
run('git','-c','gc.auto=0','fetch','origin','main');run('git','merge-base','--is-ancestor','origin/main','HEAD')
files=['README-CONTRIBUTORS.md','docs/EDITION_101_CONTRACT.md','AGENTS.md','README.md','README-101.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md','docs/obsidian/00-home.md','docs/obsidian/109-durable-data-creation.md','docs/obsidian/109-durable-data-creation.canvas','docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/101']
run('git','add',*files);run('git','add','-f','docs/readmes/101/original');run('git','diff','--cached','--check');run('git','-c','gc.auto=0','commit','-m','Document edition 101 durable chat workflows and independent review')
assert not git('status','--porcelain')
release=git('rev-parse','HEAD');print('Release',release,flush=True);run('python','build/audit-publication101.py','HEAD',release)
run('git','tag','-a','readme-101','-m','Edition 101: one durable chat request, verified data-to-creation and restart recovery')
with (r/'build/push101.log').open('wb') as f:run('git','-c','pack.threads=4','push','--no-thin','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition101','refs/tags/readme-101',stdout=f,stderr=subprocess.STDOUT)
print((r/'build/push101.log').read_text(encoding='utf8'),flush=True)
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition101-main','refs/heads/edition101:refs/verify/edition101-branch','refs/tags/readme-101:refs/verify/edition101-tag')
assert git('rev-parse','refs/verify/edition101-branch')==release
assert git('rev-parse','refs/verify/edition101-tag^{commit}')==release
run('python','build/audit-publication101.py','refs/verify/edition101-main',release)
