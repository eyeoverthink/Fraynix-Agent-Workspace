
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

assert git('branch', '--show-current') == 'edition180'
assert git('remote', 'get-url', '--push', 'origin').lower() == 'https://github.com/eyeoverthink/fraynix-agent-workspace.git'
documentation = receipt('build/documentation-check.json')
assert documentation['passed'] and documentation['editions'] == 180
for path in ['docs/readmes/180/verification.json','build/visual180.json']:
    assert receipt(path)['passed'], path
run('git','-c','gc.auto=0','fetch','origin','main')
run('git','merge-base','--is-ancestor','origin/main','HEAD')
paths = [
    'AGENTS.md','README-CONTRIBUTORS.md','README.md','README-180.md',
    'docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md',
    'docs/INTEGRATED_AGENT_ROADMAP.md',
    'docs/obsidian/00-home.md',
    'docs/obsidian/188-request-workbench.md','docs/obsidian/188-request-workbench.canvas',
    'docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/180','docs/publication180',
    'docs/REQUEST_WORKBENCH_180_LOCK.json'
]
run('git','add',*paths)
run('git','diff','--cached','--check')
run('git','-c','gc.auto=0','commit','-m','Document edition 180 teachable request workbench and checked conversions')
assert not git('status','--porcelain')
release = git('rev-parse','HEAD')
run('python','-X','utf8','docs/publication180/audit.py','HEAD',release)
run('git','tag','-a','readme-180','-m','Edition 180 teachable requests and verified conversion')
run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main',
    'HEAD:refs/heads/edition180','refs/tags/readme-180')
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition180-main',
    'refs/heads/edition180:refs/verify/edition180-branch','refs/tags/readme-180:refs/verify/edition180-tag')
assert git('rev-parse','refs/verify/edition180-branch') == release
assert git('rev-parse','refs/verify/edition180-tag^{commit}') == release
run('python','-X','utf8','docs/publication180/audit.py','refs/verify/edition180-main',release)
result = {'passed':True,'edition':180,'finalMain':release,'annotatedTag':'readme-180',
          'tagObject':git('rev-parse','readme-180'),'documentation':documentation,
          'externalEvidence':receipt('build/evidence180-upload.json'),
          'releaseAudit':receipt('build/publication-audit180.json')}
(root/'build/final-publication180.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf8')
print(json.dumps(result))
