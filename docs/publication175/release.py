
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

assert git('branch', '--show-current') == 'edition175'
assert git('remote', 'get-url', '--push', 'origin').lower() == 'https://github.com/eyeoverthink/fraynix-agent-workspace.git'
documentation = receipt('build/documentation-check.json')
assert documentation['passed'] and documentation['editions'] == 175
for path in ['docs/readmes/175/verification.json','build/visual175.json']:
    assert receipt(path)['passed'], path
run('git','-c','gc.auto=0','fetch','origin','main')
run('git','merge-base','--is-ancestor','origin/main','HEAD')
paths = [
    'AGENTS.md','README-CONTRIBUTORS.md','README.md','README-175.md',
    'docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md',
    'docs/INTEGRATED_AGENT_ROADMAP.md',
    'docs/obsidian/00-home.md',
    'docs/obsidian/183-local-workflow-selection.md','docs/obsidian/183-local-workflow-selection.canvas',
    'docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/175','docs/publication175',
    'docs/LOCAL_WORKFLOW_175_LOCK.json'
]
run('git','add',*paths)
run('git','diff','--cached','--check')
run('git','-c','gc.auto=0','commit','-m','Document edition 175 workflow selection timings and verification')
assert not git('status','--porcelain')
release = git('rev-parse','HEAD')
run('python','-X','utf8','docs/publication175/audit.py','HEAD',release)
run('git','tag','-a','readme-175','-m','Edition 175 measured local workflow selection')
run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main',
    'HEAD:refs/heads/edition175','refs/tags/readme-175')
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition175-main',
    'refs/heads/edition175:refs/verify/edition175-branch','refs/tags/readme-175:refs/verify/edition175-tag')
assert git('rev-parse','refs/verify/edition175-branch') == release
assert git('rev-parse','refs/verify/edition175-tag^{commit}') == release
run('python','-X','utf8','docs/publication175/audit.py','refs/verify/edition175-main',release)
result = {'passed':True,'edition':175,'finalMain':release,'annotatedTag':'readme-175',
          'tagObject':git('rev-parse','readme-175'),'documentation':documentation,
          'externalEvidence':receipt('build/evidence175-upload.json'),
          'releaseAudit':receipt('build/publication-audit175.json')}
(root/'build/final-publication175.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf8')
print(json.dumps(result))
