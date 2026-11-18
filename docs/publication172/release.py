
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

assert git('branch', '--show-current') == 'edition172'
assert git('remote', 'get-url', '--push', 'origin').lower() == 'https://github.com/eyeoverthink/fraynix-agent-workspace.git'
documentation = receipt('build/documentation-check.json')
assert documentation['passed'] and documentation['editions'] == 172
for path in ['docs/readmes/172/verification.json','build/visual172.json']:
    assert receipt(path)['passed'], path
run('git','-c','gc.auto=0','fetch','origin','main')
run('git','merge-base','--is-ancestor','origin/main','HEAD')
paths = [
    'AGENTS.md','README-CONTRIBUTORS.md','README.md','README-172.md',
    'docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md',
    'docs/INTEGRATED_AGENT_ROADMAP.md',
    'docs/ARCHITECTURE_RENEWAL_PLAN.md','docs/obsidian/00-home.md',
    'docs/obsidian/180-browser-world-experiment.md','docs/obsidian/180-browser-world-experiment.canvas',
    'docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/172','docs/publication172',
    'docs/LOCAL_RAMP_172_LOCK.json'
]
run('git','add',*paths)
run('git','diff','--cached','--check')
run('git','-c','gc.auto=0','commit','-m','Document edition 172 browser, world and canvas verification')
assert not git('status','--porcelain')
release = git('rev-parse','HEAD')
run('python','-X','utf8','docs/publication172/audit.py','HEAD',release)
run('git','tag','-a','readme-172','-m','Edition 172 browser-to-world experiments')
run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main',
    'HEAD:refs/heads/edition172','refs/tags/readme-172')
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition172-main',
    'refs/heads/edition172:refs/verify/edition172-branch','refs/tags/readme-172:refs/verify/edition172-tag')
assert git('rev-parse','refs/verify/edition172-branch') == release
assert git('rev-parse','refs/verify/edition172-tag^{commit}') == release
run('python','-X','utf8','docs/publication172/audit.py','refs/verify/edition172-main',release)
result = {'passed':True,'edition':172,'finalMain':release,'annotatedTag':'readme-172',
          'tagObject':git('rev-parse','readme-172'),'documentation':documentation,
          'externalEvidence':receipt('build/evidence172-upload.json'),
          'releaseAudit':receipt('build/publication-audit172.json')}
(root/'build/final-publication172.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf8')
print(json.dumps(result))
