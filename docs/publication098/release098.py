from pathlib import Path
import subprocess,json
r=Path.cwd()
def run(*a,**kw):return subprocess.run(a,check=True,**kw)
def git(*a):return subprocess.check_output(['git',*a],text=True).strip()
assert git('remote','get-url','--push','origin').lower() in ['https://github.com/eyeoverthink/fraynix-agent-workspace.git','git@github.com:eyeoverthink/fraynix-agent-workspace.git']
ci=json.loads((r/'build/ci098-status.json').read_text(encoding='utf8'));assert ci['head_sha']=='29129e6f085441ba860ec42f99c98fe4af0ce0eb' and ci['id']==42571456884
assert json.loads((r/'docs/readmes/098/verification.json').read_text(encoding='utf8'))['passed']
assert json.loads((r/'build/documentation-check.json').read_text(encoding='utf8'))['passed']
assert json.loads((r/'build/visual098.json').read_text(encoding='utf8'))['passed']
run('git','-c','gc.auto=0','fetch','origin','main');run('git','merge-base','--is-ancestor','origin/main','HEAD')
files=['README-CONTRIBUTORS.md','docs/EDITION_098_CONTRACT.md','AGENTS.md','README.md','README-098.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md','docs/obsidian/00-home.md','docs/obsidian/106-benefit-selection.md','docs/obsidian/106-benefit-selection.canvas','docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/098']
run('git','add',*files);run('git','add','-f','docs/readmes/098/original');run('git','diff','--cached','--check');run('git','-c','gc.auto=0','commit','-m','Document edition 098 benefit gate and verified native comparisons')
release=git('rev-parse','HEAD');print('Release',release,flush=True);run('python','build/audit-publication098.py','HEAD',release)
run('git','tag','-a','readme-098','-m','Edition 098: complete-task benefit, current movement coverage and verified recovery')
with (r/'build/push098.log').open('wb') as f:run('git','-c','pack.threads=4','push','--no-thin','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition098','refs/tags/readme-098',stdout=f,stderr=subprocess.STDOUT)
print((r/'build/push098.log').read_text(encoding='utf8'),flush=True)
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition098-main','refs/heads/edition098:refs/verify/edition098-branch','refs/tags/readme-098:refs/verify/edition098-tag')
assert git('rev-parse','refs/verify/edition098-branch')==release
assert git('rev-parse','refs/verify/edition098-tag^{commit}')==release
run('python','build/audit-publication098.py','refs/verify/edition098-main',release)
