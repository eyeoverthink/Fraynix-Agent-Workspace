
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

assert git('branch', '--show-current') == 'edition171'
assert git('remote', 'get-url', '--push', 'origin').lower() == 'https://github.com/eyeoverthink/fraynix-agent-workspace.git'
documentation = receipt('build/documentation-check.json')
assert documentation['passed'] and documentation['editions'] == 171
for path in ['docs/readmes/171/verification.json','build/visual171.json']:
    assert receipt(path)['passed'], path
run('git','-c','gc.auto=0','fetch','origin','main')
run('git','merge-base','--is-ancestor','origin/main','HEAD')
paths = [
    'AGENTS.md','README-CONTRIBUTORS.md','README.md','README-171.md',
    'docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md',
    'docs/INTEGRATED_AGENT_ROADMAP.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md',
    'docs/ARCHITECTURE_RENEWAL_PLAN.md','docs/obsidian/00-home.md',
    'docs/obsidian/179-offline-playtesting.md','docs/obsidian/179-offline-playtesting.canvas',
    'docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/171','docs/publication171',
    'docs/OFFLINE_PLAYTEST_171_LOCK.json'
]
run('git','add',*paths)
run('git','diff','--cached','--check')
run('git','-c','gc.auto=0','commit','-m','Document edition 171 offline missions and release evidence')
assert not git('status','--porcelain')
release = git('rev-parse','HEAD')
run('python','-X','utf8','docs/publication171/audit.py','HEAD',release)
run('git','tag','-a','readme-171','-m','Edition 171 durable offline playtest')
run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main',
    'HEAD:refs/heads/edition171','refs/tags/readme-171')
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition171-main',
    'refs/heads/edition171:refs/verify/edition171-branch','refs/tags/readme-171:refs/verify/edition171-tag')
assert git('rev-parse','refs/verify/edition171-branch') == release
assert git('rev-parse','refs/verify/edition171-tag^{commit}') == release
run('python','-X','utf8','docs/publication171/audit.py','refs/verify/edition171-main',release)
result = {'passed':True,'edition':171,'finalMain':release,'annotatedTag':'readme-171',
          'tagObject':git('rev-parse','readme-171'),'documentation':documentation,
          'externalEvidence':receipt('build/evidence171-upload.json'),
          'releaseAudit':receipt('build/publication-audit171.json')}
(root/'build/final-publication171.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf8')
print(json.dumps(result))
