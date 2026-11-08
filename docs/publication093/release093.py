from pathlib import Path
import subprocess,json
r=Path.cwd()
def run(*a,**kw):return subprocess.run(a,check=True,**kw)
def git(*a):return subprocess.check_output(['git',*a],text=True).strip()
assert git('remote','get-url','--push','origin').lower() in ['https://github.com/eyeoverthink/fraynix-agent-workspace.git','git@github.com:eyeoverthink/fraynix-agent-workspace.git']
assert json.loads((r/'build/documentation-check.json').read_text(encoding='utf8'))['passed']
assert json.loads((r/'build/visual093.json').read_text(encoding='utf8'))['passed']
run('git','-c','gc.auto=0','fetch','origin','main');run('git','merge-base','--is-ancestor','origin/main','HEAD')
files=['AGENTS.md','README.md','README-093.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md','docs/obsidian/00-home.md','docs/obsidian/101-recovered-state-forecast.md','docs/obsidian/101-recovered-state-forecast.canvas','docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/093']
run('git','add',*files);run('git','add','-f','docs/readmes/093/original');run('git','diff','--cached','--check');run('git','-c','gc.auto=0','commit','-m','Document edition 093 recovered-state forecasts with original outcomes and screenshots')
release=git('rev-parse','HEAD');print('Release',release,flush=True);run('python','build/audit-publication093.py','HEAD',release)
run('git','tag','-a','readme-093','-m','Edition 093: recovered-state forecasts with all verification')
with (r/'build/push093.log').open('wb') as f:run('git','-c','pack.threads=4','push','--no-thin','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition093','refs/tags/readme-093',stdout=f,stderr=subprocess.STDOUT)
print((r/'build/push093.log').read_text(encoding='utf8'),flush=True)
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition093-main','refs/heads/edition093:refs/verify/edition093-branch','refs/tags/readme-093:refs/verify/edition093-tag')
assert git('rev-parse','refs/verify/edition093-branch')==release
assert git('rev-parse','refs/verify/edition093-tag^{commit}')==release
run('python','build/audit-publication093.py','refs/verify/edition093-main',release)
