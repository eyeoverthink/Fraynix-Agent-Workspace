
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

assert git('branch', '--show-current') == 'edition178'
assert git('remote', 'get-url', '--push', 'origin').lower() == 'https://github.com/eyeoverthink/fraynix-agent-workspace.git'
documentation = receipt('build/documentation-check.json')
assert documentation['passed'] and documentation['editions'] == 178
for path in ['docs/readmes/178/verification.json','build/visual178.json']:
    assert receipt(path)['passed'], path
run('git','-c','gc.auto=0','fetch','origin','main')
run('git','merge-base','--is-ancestor','origin/main','HEAD')
paths = [
    'AGENTS.md','README-CONTRIBUTORS.md','README.md','README-178.md',
    'docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md',
    'docs/INTEGRATED_AGENT_ROADMAP.md',
    'docs/obsidian/00-home.md',
    'docs/obsidian/186-relay-clearance-chain.md','docs/obsidian/186-relay-clearance-chain.canvas',
    'docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/178','docs/publication178',
    'docs/RELAY_CLEARANCE_CHAIN_178_LOCK.json'
]
run('git','add',*paths)
run('git','diff','--cached','--check')
run('git','-c','gc.auto=0','commit','-m','Document edition 178 verified Relay playtest and clearance dashboard')
assert not git('status','--porcelain')
release = git('rev-parse','HEAD')
run('python','-X','utf8','docs/publication178/audit.py','HEAD',release)
run('git','tag','-a','readme-178','-m','Edition 178 verified Relay playtest to clearance dashboard')
run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main',
    'HEAD:refs/heads/edition178','refs/tags/readme-178')
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition178-main',
    'refs/heads/edition178:refs/verify/edition178-branch','refs/tags/readme-178:refs/verify/edition178-tag')
assert git('rev-parse','refs/verify/edition178-branch') == release
assert git('rev-parse','refs/verify/edition178-tag^{commit}') == release
run('python','-X','utf8','docs/publication178/audit.py','refs/verify/edition178-main',release)
result = {'passed':True,'edition':178,'finalMain':release,'annotatedTag':'readme-178',
          'tagObject':git('rev-parse','readme-178'),'documentation':documentation,
          'externalEvidence':receipt('build/evidence178-upload.json'),
          'releaseAudit':receipt('build/publication-audit178.json')}
(root/'build/final-publication178.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf8')
print(json.dumps(result))
