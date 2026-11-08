from pathlib import Path
import subprocess,json
r=Path.cwd()
def run(*a,**kw):return subprocess.run(a,check=True,**kw)
def git(*a):return subprocess.check_output(['git',*a],text=True).strip()
assert git('remote','get-url','--push','origin').lower() in ['https://github.com/eyeoverthink/fraynix-agent-workspace.git','git@github.com:eyeoverthink/fraynix-agent-workspace.git']
assert json.loads((r/'build/documentation-check.json').read_text(encoding='utf8'))['passed']
assert json.loads((r/'build/visual092.json').read_text(encoding='utf8'))['passed']
run('git','-c','gc.auto=0','fetch','origin','main');run('git','merge-base','--is-ancestor','origin/main','HEAD')
files=['AGENTS.md','README.md','README-092.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md','docs/obsidian/00-home.md','docs/obsidian/100-growing-bank-restarts.md','docs/obsidian/100-growing-bank-restarts.canvas','docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/092']
run('git','add',*files);run('git','add','-f','docs/readmes/092/original');run('git','diff','--cached','--check');run('git','-c','gc.auto=0','commit','-m','Document edition 092 growing banks and restarted tasks with original outcomes and screenshots')
release=git('rev-parse','HEAD');print('Release',release,flush=True);run('python','build/audit-publication092.py','HEAD',release)
run('git','tag','-a','readme-092','-m','Edition 092: growing banks and restarted tasks with all verification')
with (r/'build/push092.log').open('wb') as f:run('git','-c','pack.threads=4','push','--no-thin','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition092','refs/tags/readme-092',stdout=f,stderr=subprocess.STDOUT)
print((r/'build/push092.log').read_text(encoding='utf8'),flush=True)
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition092-main','refs/heads/edition092:refs/verify/edition092-branch','refs/tags/readme-092:refs/verify/edition092-tag')
assert git('rev-parse','refs/verify/edition092-branch')==release
assert git('rev-parse','refs/verify/edition092-tag^{commit}')==release
run('python','build/audit-publication092.py','refs/verify/edition092-main',release)
