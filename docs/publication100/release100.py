from pathlib import Path
import subprocess,json
r=Path.cwd()
def run(*a,**kw):return subprocess.run(a,check=True,**kw)
def git(*a):return subprocess.check_output(['git',*a],text=True).strip()
assert git('remote','get-url','--push','origin').lower() in ['https://github.com/eyeoverthink/fraynix-agent-workspace.git','git@github.com:eyeoverthink/fraynix-agent-workspace.git']
for p in ['docs/readmes/100/verification.json','build/documentation-check.json','build/visual100.json','build/visual-review100.json']:assert json.loads((r/p).read_text(encoding='utf-8-sig'))['passed'],p
assert git('branch','--show-current')=='edition100'
run('git','-c','gc.auto=0','fetch','origin','main');run('git','merge-base','--is-ancestor','origin/main','HEAD')
files=['README-CONTRIBUTORS.md','docs/EDITION_100_CONTRACT.md','AGENTS.md','README.md','README-100.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md','docs/obsidian/00-home.md','docs/obsidian/108-verified-data-handoffs.md','docs/obsidian/108-verified-data-handoffs.canvas','docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/100']
run('git','add',*files);run('git','add','-f','docs/readmes/100/original');run('git','diff','--cached','--check');run('git','-c','gc.auto=0','commit','-m','Document edition 100 verified data handoffs and fresh-source skill reuse')
assert not git('status','--porcelain')
release=git('rev-parse','HEAD');print('Release',release,flush=True);run('python','build/audit-publication100.py','HEAD',release)
run('git','tag','-a','readme-100','-m','Edition 100: explicit data-to-creation lineage and verified fresh-source procedure reuse')
with (r/'build/push100.log').open('wb') as f:run('git','-c','pack.threads=4','push','--no-thin','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition100','refs/tags/readme-100',stdout=f,stderr=subprocess.STDOUT)
print((r/'build/push100.log').read_text(encoding='utf8'),flush=True)
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition100-main','refs/heads/edition100:refs/verify/edition100-branch','refs/tags/readme-100:refs/verify/edition100-tag')
assert git('rev-parse','refs/verify/edition100-branch')==release
assert git('rev-parse','refs/verify/edition100-tag^{commit}')==release
run('python','build/audit-publication100.py','refs/verify/edition100-main',release)
