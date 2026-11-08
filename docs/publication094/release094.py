from pathlib import Path
import subprocess,json
r=Path.cwd()
def run(*a,**kw):return subprocess.run(a,check=True,**kw)
def git(*a):return subprocess.check_output(['git',*a],text=True).strip()
assert git('remote','get-url','--push','origin').lower() in ['https://github.com/eyeoverthink/fraynix-agent-workspace.git','git@github.com:eyeoverthink/fraynix-agent-workspace.git']
assert json.loads((r/'build/documentation-check.json').read_text(encoding='utf8'))['passed']
assert json.loads((r/'build/visual094.json').read_text(encoding='utf8'))['passed']
run('git','-c','gc.auto=0','fetch','origin','main');run('git','merge-base','--is-ancestor','origin/main','HEAD')
files=['README-CONTRIBUTORS.md','docs/TRAJECTORY_REVIEW_094.md','docs/EDITION_094_CONTRACT.md','AGENTS.md','README.md','README-094.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md','docs/obsidian/00-home.md','docs/obsidian/102-three-stage-recovery.md','docs/obsidian/102-three-stage-recovery.canvas','docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/094']
run('git','add',*files);run('git','add','-f','docs/readmes/094/original');run('git','diff','--cached','--check');run('git','-c','gc.auto=0','commit','-m','Document edition 094 three-stage crash recovery and contributor direction with original evidence')
release=git('rev-parse','HEAD');print('Release',release,flush=True);run('python','build/audit-publication094.py','HEAD',release)
run('git','tag','-a','readme-094','-m','Edition 094: three-stage abrupt recovery and contributor direction, with all verification')
with (r/'build/push094.log').open('wb') as f:run('git','-c','pack.threads=4','push','--no-thin','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition094','refs/tags/readme-094',stdout=f,stderr=subprocess.STDOUT)
print((r/'build/push094.log').read_text(encoding='utf8'),flush=True)
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition094-main','refs/heads/edition094:refs/verify/edition094-branch','refs/tags/readme-094:refs/verify/edition094-tag')
assert git('rev-parse','refs/verify/edition094-branch')==release
assert git('rev-parse','refs/verify/edition094-tag^{commit}')==release
run('python','build/audit-publication094.py','refs/verify/edition094-main',release)
