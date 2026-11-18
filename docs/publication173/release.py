
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

assert git('branch', '--show-current') == 'edition173'
assert git('remote', 'get-url', '--push', 'origin').lower() == 'https://github.com/eyeoverthink/fraynix-agent-workspace.git'
documentation = receipt('build/documentation-check.json')
assert documentation['passed'] and documentation['editions'] == 173
for path in ['docs/readmes/173/verification.json','build/visual173.json']:
    assert receipt(path)['passed'], path
run('git','-c','gc.auto=0','fetch','origin','main')
run('git','merge-base','--is-ancestor','origin/main','HEAD')
paths = [
    'AGENTS.md','README-CONTRIBUTORS.md','README.md','README-173.md',
    'docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md',
    'docs/INTEGRATED_AGENT_ROADMAP.md',
    'docs/ARCHITECTURE_RENEWAL_PLAN.md','docs/obsidian/00-home.md',
    'docs/obsidian/181-registered-local-experiment.md','docs/obsidian/181-registered-local-experiment.canvas',
    'docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/173','docs/publication173',
    'docs/REGISTERED_RAMP_173_LOCK.json'
]
run('git','add',*paths)
run('git','diff','--cached','--check')
run('git','-c','gc.auto=0','commit','-m','Document edition 173 shared procedure and CSV evidence')
assert not git('status','--porcelain')
release = git('rev-parse','HEAD')
run('python','-X','utf8','docs/publication173/audit.py','HEAD',release)
run('git','tag','-a','readme-173','-m','Edition 173 local procedure catalog and CSV input')
run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main',
    'HEAD:refs/heads/edition173','refs/tags/readme-173')
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition173-main',
    'refs/heads/edition173:refs/verify/edition173-branch','refs/tags/readme-173:refs/verify/edition173-tag')
assert git('rev-parse','refs/verify/edition173-branch') == release
assert git('rev-parse','refs/verify/edition173-tag^{commit}') == release
run('python','-X','utf8','docs/publication173/audit.py','refs/verify/edition173-main',release)
result = {'passed':True,'edition':173,'finalMain':release,'annotatedTag':'readme-173',
          'tagObject':git('rev-parse','readme-173'),'documentation':documentation,
          'externalEvidence':receipt('build/evidence173-upload.json'),
          'releaseAudit':receipt('build/publication-audit173.json')}
(root/'build/final-publication173.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf8')
print(json.dumps(result))
