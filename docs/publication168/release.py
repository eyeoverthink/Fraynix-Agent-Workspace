
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

assert git('branch', '--show-current') == 'edition168'
assert git('remote', 'get-url', '--push', 'origin').lower() == 'https://github.com/eyeoverthink/fraynix-agent-workspace.git'
documentation = receipt('build/documentation-check.json')
assert documentation['passed'] and documentation['editions'] == 168
for path in ['docs/readmes/168/verification.json','build/visual168.json','build/visual-review168.json']:
    assert receipt(path)['passed'], path
run('git','-c','gc.auto=0','fetch','origin','main')
run('git','merge-base','--is-ancestor','origin/main','HEAD')
paths = [
    'AGENTS.md','README-CONTRIBUTORS.md','README.md','README-168.md',
    'docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md',
    'docs/INTEGRATED_AGENT_ROADMAP.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md',
    'docs/ARCHITECTURE_RENEWAL_PLAN.md','docs/obsidian/00-home.md',
    'docs/obsidian/176-original-courses.md','docs/obsidian/176-original-courses.canvas',
    'docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/168','docs/publication168',
    'docs/ORIGINAL_COURSES_168_LOCK.json'
]
run('git','add',*paths)
run('git','diff','--cached','--check')
run('git','-c','gc.auto=0','commit','-m','Document edition 168 courses, playtests and restored archive batch')
assert not git('status','--porcelain')
release = git('rev-parse','HEAD')
run('python','-X','utf8','docs/publication168/audit.py','HEAD',release)
run('git','tag','-a','readme-168','-m','Edition 168 original pickups, hazards and two playable courses')
run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main',
    'HEAD:refs/heads/edition168','refs/tags/readme-168')
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition168-main',
    'refs/heads/edition168:refs/verify/edition168-branch','refs/tags/readme-168:refs/verify/edition168-tag')
assert git('rev-parse','refs/verify/edition168-branch') == release
assert git('rev-parse','refs/verify/edition168-tag^{commit}') == release
run('python','-X','utf8','docs/publication168/audit.py','refs/verify/edition168-main',release)
result = {'passed':True,'edition':168,'finalMain':release,'annotatedTag':'readme-168',
          'tagObject':git('rev-parse','readme-168'),'documentation':documentation,
          'externalEvidence':receipt('build/evidence168-upload.json'),
          'releaseAudit':receipt('build/publication-audit168.json')}
(root/'build/final-publication168.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf8')
print(json.dumps(result))
