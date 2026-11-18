
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

assert git('branch', '--show-current') == 'edition170'
assert git('remote', 'get-url', '--push', 'origin').lower() == 'https://github.com/eyeoverthink/fraynix-agent-workspace.git'
documentation = receipt('build/documentation-check.json')
assert documentation['passed'] and documentation['editions'] == 170
for path in ['docs/readmes/170/verification.json','build/visual170.json','build/visual-review170.json']:
    assert receipt(path)['passed'], path
run('git','-c','gc.auto=0','fetch','origin','main')
run('git','merge-base','--is-ancestor','origin/main','HEAD')
paths = [
    'AGENTS.md','README-CONTRIBUTORS.md','README.md','README-170.md',
    'docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md',
    'docs/INTEGRATED_AGENT_ROADMAP.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md',
    'docs/ARCHITECTURE_RENEWAL_PLAN.md','docs/obsidian/00-home.md',
    'docs/obsidian/178-multi-rail.md','docs/obsidian/178-multi-rail.canvas',
    'docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/170','docs/publication170',
    'docs/MULTI_RAIL_170_LOCK.json'
]
run('git','add',*paths)
run('git','diff','--cached','--check')
run('git','-c','gc.auto=0','commit','-m','Document edition 170 multi-rail courses and pace trials')
assert not git('status','--porcelain')
release = git('rev-parse','HEAD')
run('python','-X','utf8','docs/publication170/audit.py','HEAD',release)
run('git','tag','-a','readme-170','-m','Edition 170 multi-rail navigation and measured paces')
run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main',
    'HEAD:refs/heads/edition170','refs/tags/readme-170')
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition170-main',
    'refs/heads/edition170:refs/verify/edition170-branch','refs/tags/readme-170:refs/verify/edition170-tag')
assert git('rev-parse','refs/verify/edition170-branch') == release
assert git('rev-parse','refs/verify/edition170-tag^{commit}') == release
run('python','-X','utf8','docs/publication170/audit.py','refs/verify/edition170-main',release)
result = {'passed':True,'edition':170,'finalMain':release,'annotatedTag':'readme-170',
          'tagObject':git('rev-parse','readme-170'),'documentation':documentation,
          'externalEvidence':receipt('build/evidence170-upload.json'),
          'releaseAudit':receipt('build/publication-audit170.json')}
(root/'build/final-publication170.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf8')
print(json.dumps(result))
