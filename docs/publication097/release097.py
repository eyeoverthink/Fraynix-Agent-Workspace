from pathlib import Path
import subprocess,json
r=Path.cwd()
def run(*a,**kw):return subprocess.run(a,check=True,**kw)
def git(*a):return subprocess.check_output(['git',*a],text=True).strip()
assert git('remote','get-url','--push','origin').lower() in ['https://github.com/eyeoverthink/fraynix-agent-workspace.git','git@github.com:eyeoverthink/fraynix-agent-workspace.git']
ci=json.loads((r/'build/ci097-final.json').read_text(encoding='utf8'));assert ci['status']=='completed' and ci['conclusion']=='success' and ci['head_sha']=='453e8708ab9353d86ed666f3d059d28563372530' and ci['id']==42554061175
assert json.loads((r/'docs/readmes/097/verification.json').read_text(encoding='utf8'))['passed']
assert json.loads((r/'build/documentation-check.json').read_text(encoding='utf8'))['passed']
assert json.loads((r/'build/visual097.json').read_text(encoding='utf8'))['passed']
run('git','-c','gc.auto=0','fetch','origin','main');run('git','merge-base','--is-ancestor','origin/main','HEAD')
files=['README-CONTRIBUTORS.md','docs/EDITION_097_CONTRACT.md','AGENTS.md','README.md','README-097.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md','docs/obsidian/00-home.md','docs/obsidian/105-continuing-costs.md','docs/obsidian/105-continuing-costs.canvas','docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/097']
run('git','add',*files);run('git','add','-f','docs/readmes/097/original');run('git','diff','--cached','--check');run('git','-c','gc.auto=0','commit','-m','Document edition 097 continuing-world costs and verified recovery')
release=git('rev-parse','HEAD');print('Release',release,flush=True);run('python','build/audit-publication097.py','HEAD',release)
run('git','tag','-a','readme-097','-m','Edition 097: growing verified memories, continuing worlds and complete task costs')
with (r/'build/push097.log').open('wb') as f:run('git','-c','pack.threads=4','push','--no-thin','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition097','refs/tags/readme-097',stdout=f,stderr=subprocess.STDOUT)
print((r/'build/push097.log').read_text(encoding='utf8'),flush=True)
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition097-main','refs/heads/edition097:refs/verify/edition097-branch','refs/tags/readme-097:refs/verify/edition097-tag')
assert git('rev-parse','refs/verify/edition097-branch')==release
assert git('rev-parse','refs/verify/edition097-tag^{commit}')==release
run('python','build/audit-publication097.py','refs/verify/edition097-main',release)
