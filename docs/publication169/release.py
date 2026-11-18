
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

assert git('branch', '--show-current') == 'edition169'
assert git('remote', 'get-url', '--push', 'origin').lower() == 'https://github.com/eyeoverthink/fraynix-agent-workspace.git'
documentation = receipt('build/documentation-check.json')
assert documentation['passed'] and documentation['editions'] == 169
for path in ['docs/readmes/169/verification.json','build/visual169.json','build/visual-review169.json']:
    assert receipt(path)['passed'], path
run('git','-c','gc.auto=0','fetch','origin','main')
run('git','merge-base','--is-ancestor','origin/main','HEAD')
paths = [
    'AGENTS.md','README-CONTRIBUTORS.md','README.md','README-169.md',
    'docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md',
    'docs/INTEGRATED_AGENT_ROADMAP.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md',
    'docs/ARCHITECTURE_RENEWAL_PLAN.md','docs/obsidian/00-home.md',
    'docs/obsidian/177-rail-navigation.md','docs/obsidian/177-rail-navigation.canvas',
    'docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/169','docs/publication169',
    'docs/RAIL_NAVIGATION_169_LOCK.json'
]
run('git','add',*paths)
run('git','diff','--cached','--check')
run('git','-c','gc.auto=0','commit','-m','Document edition 169 rail navigation, native runs and app proof')
assert not git('status','--porcelain')
release = git('rev-parse','HEAD')
run('python','-X','utf8','docs/publication169/audit.py','HEAD',release)
run('git','tag','-a','readme-169','-m','Edition 169 verified AI rail clearance')
run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main',
    'HEAD:refs/heads/edition169','refs/tags/readme-169')
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition169-main',
    'refs/heads/edition169:refs/verify/edition169-branch','refs/tags/readme-169:refs/verify/edition169-tag')
assert git('rev-parse','refs/verify/edition169-branch') == release
assert git('rev-parse','refs/verify/edition169-tag^{commit}') == release
run('python','-X','utf8','docs/publication169/audit.py','refs/verify/edition169-main',release)
result = {'passed':True,'edition':169,'finalMain':release,'annotatedTag':'readme-169',
          'tagObject':git('rev-parse','readme-169'),'documentation':documentation,
          'externalEvidence':receipt('build/evidence169-upload.json'),
          'releaseAudit':receipt('build/publication-audit169.json')}
(root/'build/final-publication169.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf8')
print(json.dumps(result))
