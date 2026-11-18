
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

assert git('branch', '--show-current') == 'edition176'
assert git('remote', 'get-url', '--push', 'origin').lower() == 'https://github.com/eyeoverthink/fraynix-agent-workspace.git'
documentation = receipt('build/documentation-check.json')
assert documentation['passed'] and documentation['editions'] == 176
for path in ['docs/readmes/176/verification.json','build/visual176.json']:
    assert receipt(path)['passed'], path
run('git','-c','gc.auto=0','fetch','origin','main')
run('git','merge-base','--is-ancestor','origin/main','HEAD')
paths = [
    'AGENTS.md','README-CONTRIBUTORS.md','README.md','README-176.md',
    'docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md',
    'docs/INTEGRATED_AGENT_ROADMAP.md',
    'docs/obsidian/00-home.md',
    'docs/obsidian/184-quick-workflow-routing.md','docs/obsidian/184-quick-workflow-routing.canvas',
    'docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/176','docs/publication176',
    'docs/QUICK_ROUTING_176_LOCK.json'
]
run('git','add',*paths)
run('git','diff','--cached','--check')
run('git','-c','gc.auto=0','commit','-m','Document edition 176 quick routing and workflow confirmation')
assert not git('status','--porcelain')
release = git('rev-parse','HEAD')
run('python','-X','utf8','docs/publication176/audit.py','HEAD',release)
run('git','tag','-a','readme-176','-m','Edition 176 verified quick routing and confirmation')
run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main',
    'HEAD:refs/heads/edition176','refs/tags/readme-176')
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition176-main',
    'refs/heads/edition176:refs/verify/edition176-branch','refs/tags/readme-176:refs/verify/edition176-tag')
assert git('rev-parse','refs/verify/edition176-branch') == release
assert git('rev-parse','refs/verify/edition176-tag^{commit}') == release
run('python','-X','utf8','docs/publication176/audit.py','refs/verify/edition176-main',release)
result = {'passed':True,'edition':176,'finalMain':release,'annotatedTag':'readme-176',
          'tagObject':git('rev-parse','readme-176'),'documentation':documentation,
          'externalEvidence':receipt('build/evidence176-upload.json'),
          'releaseAudit':receipt('build/publication-audit176.json')}
(root/'build/final-publication176.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf8')
print(json.dumps(result))
