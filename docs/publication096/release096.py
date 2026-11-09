from pathlib import Path
import subprocess,json
r=Path.cwd()
def run(*a,**kw):return subprocess.run(a,check=True,**kw)
def git(*a):return subprocess.check_output(['git',*a],text=True).strip()
assert git('remote','get-url','--push','origin').lower() in ['https://github.com/eyeoverthink/fraynix-agent-workspace.git','git@github.com:eyeoverthink/fraynix-agent-workspace.git']
assert json.loads((r/'build/documentation-check.json').read_text(encoding='utf8'))['passed']
assert json.loads((r/'build/visual096.json').read_text(encoding='utf8'))['passed']
run('git','-c','gc.auto=0','fetch','origin','main');run('git','merge-base','--is-ancestor','origin/main','HEAD')
files=['README-CONTRIBUTORS.md','docs/EDITION_096_CONTRACT.md','AGENTS.md','README.md','README-096.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md','docs/obsidian/00-home.md','docs/obsidian/104-full-verification.md','docs/obsidian/104-full-verification.canvas','docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/096']
run('git','add',*files);run('git','add','-f','docs/readmes/096/original');run('git','diff','--cached','--check');run('git','-c','gc.auto=0','commit','-m','Document edition 096 full verification, original failures and app screenshots')
release=git('rev-parse','HEAD');print('Release',release,flush=True);run('python','build/audit-publication096.py','HEAD',release)
run('git','tag','-a','readme-096','-m','Edition 096: complete verification with original failures and fresh native studies')
with (r/'build/push096.log').open('wb') as f:run('git','-c','pack.threads=4','push','--no-thin','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition096','refs/tags/readme-096',stdout=f,stderr=subprocess.STDOUT)
print((r/'build/push096.log').read_text(encoding='utf8'),flush=True)
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition096-main','refs/heads/edition096:refs/verify/edition096-branch','refs/tags/readme-096:refs/verify/edition096-tag')
assert git('rev-parse','refs/verify/edition096-branch')==release
assert git('rev-parse','refs/verify/edition096-tag^{commit}')==release
run('python','build/audit-publication096.py','refs/verify/edition096-main',release)
