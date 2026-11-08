from pathlib import Path
import subprocess,json
r=Path.cwd()
def run(*a,**kw):return subprocess.run(a,check=True,**kw)
def git(*a):return subprocess.check_output(['git',*a],text=True).strip()
assert git('remote','get-url','--push','origin').lower() in ['https://github.com/eyeoverthink/fraynix-agent-workspace.git','git@github.com:eyeoverthink/fraynix-agent-workspace.git']
assert json.loads((r/'build/documentation-check.json').read_text(encoding='utf8'))['passed']
assert json.loads((r/'build/visual091.json').read_text(encoding='utf8'))['passed']
run('git','-c','gc.auto=0','fetch','origin','main');run('git','merge-base','--is-ancestor','origin/main','HEAD')
files=['AGENTS.md','README.md','README-091.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md','docs/obsidian/00-home.md','docs/obsidian/99-native-envelope-verification.md','docs/obsidian/99-native-envelope-verification.canvas','docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/091']
run('git','add',*files);run('git','add','-f','docs/readmes/091/original');run('git','diff','--cached','--check');run('git','-c','gc.auto=0','commit','-m','Document edition 091 measured verification reuse with original outcomes and screenshots')
release=git('rev-parse','HEAD');print('Release',release,flush=True);run('python','build/audit-publication091.py','HEAD',release)
run('git','tag','-a','readme-091','-m','Edition 091: byte-bound fingerprint reuse with all source and outcome checks')
with (r/'build/push091.log').open('wb') as f:run('git','-c','pack.threads=4','push','--no-thin','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition091','refs/tags/readme-091',stdout=f,stderr=subprocess.STDOUT)
print((r/'build/push091.log').read_text(encoding='utf8'),flush=True)
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition091-main','refs/heads/edition091:refs/verify/edition091-branch','refs/tags/readme-091:refs/verify/edition091-tag')
assert git('rev-parse','refs/verify/edition091-branch')==release
assert git('rev-parse','refs/verify/edition091-tag^{commit}')==release
run('python','build/audit-publication091.py','refs/verify/edition091-main',release)
