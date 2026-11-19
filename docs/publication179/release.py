
"""Publish the audited edition and independently verify what GitHub serves."""
from pathlib import Path
import json
import subprocess

root = Path.cwd()

def git(*args):
    return subprocess.check_output(['git', *args], text=True).strip()

def run(*args):
    subprocess.run(args, check=True)

def receipt(path):
    return json.loads((root/path).read_bytes())

assert git('branch', '--show-current') == 'edition179'
assert git('remote', 'get-url', '--push', 'origin').lower() == 'https://github.com/eyeoverthink/fraynix-agent-workspace.git'
documentation = receipt('build/documentation-check.json')
assert documentation['passed'] and documentation['editions'] == 179
for path in ['docs/readmes/179/verification.json','build/visual179.json']:
    assert receipt(path)['passed'], path
run('git','-c','gc.auto=0','fetch','origin','main')
run('git','merge-base','--is-ancestor','origin/main','HEAD')
paths = [
    'AGENTS.md','README-CONTRIBUTORS.md','README.md','README-179.md',
    'docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md',
    'docs/INTEGRATED_AGENT_ROADMAP.md',
    'docs/obsidian/00-home.md',
    'docs/obsidian/187-local-chain-recommendation.md','docs/obsidian/187-local-chain-recommendation.canvas',
    'docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/179','docs/publication179',
    'docs/CHAIN_RECOMMENDATION_179_LOCK.json'
]
run('git','add',*paths)
run('git','diff','--cached','--check')
run('git','-c','gc.auto=0','commit','-m','Document edition 179 local model recommendations for reviewed chains')
assert not git('status','--porcelain')
release = git('rev-parse','HEAD')
run('python','-X','utf8','docs/publication179/audit.py','HEAD',release)
run('git','tag','-a','readme-179','-m','Edition 179 local Ramp and Relay recommendations')
run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main',
    'HEAD:refs/heads/edition179','refs/tags/readme-179')
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition179-main',
    'refs/heads/edition179:refs/verify/edition179-branch','refs/tags/readme-179:refs/verify/edition179-tag')
assert git('rev-parse','refs/verify/edition179-branch') == release
assert git('rev-parse','refs/verify/edition179-tag^{commit}') == release
run('python','-X','utf8','docs/publication179/audit.py','refs/verify/edition179-main',release)
result = {'passed':True,'edition':179,'finalMain':release,'annotatedTag':'readme-179',
          'tagObject':git('rev-parse','readme-179'),'documentation':documentation,
          'externalEvidence':receipt('build/evidence179-upload.json'),
          'releaseAudit':receipt('build/publication-audit179.json')}
(root/'build/final-publication179.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf8')
print(json.dumps(result))
