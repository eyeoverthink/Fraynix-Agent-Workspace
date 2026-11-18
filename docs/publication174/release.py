
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

assert git('branch', '--show-current') == 'edition174'
assert git('remote', 'get-url', '--push', 'origin').lower() == 'https://github.com/eyeoverthink/fraynix-agent-workspace.git'
documentation = receipt('build/documentation-check.json')
assert documentation['passed'] and documentation['editions'] == 174
for path in ['docs/readmes/174/verification.json','build/visual174.json']:
    assert receipt(path)['passed'], path
run('git','-c','gc.auto=0','fetch','origin','main')
run('git','merge-base','--is-ancestor','origin/main','HEAD')
paths = [
    'AGENTS.md','README-CONTRIBUTORS.md','README.md','README-174.md',
    'docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md',
    'docs/INTEGRATED_AGENT_ROADMAP.md',
    'docs/ARCHITECTURE_RENEWAL_PLAN.md','docs/obsidian/00-home.md',
    'docs/obsidian/182-local-ramp-routing.md','docs/obsidian/182-local-ramp-routing.canvas',
    'docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/174','docs/publication174',
    'docs/PAIRED_RAMP_174_LOCK.json'
]
run('git','add',*paths)
run('git','diff','--cached','--check')
run('git','-c','gc.auto=0','commit','-m','Document edition 174 comparison reports and routing evidence')
assert not git('status','--porcelain')
release = git('rev-parse','HEAD')
run('python','-X','utf8','docs/publication174/audit.py','HEAD',release)
run('git','tag','-a','readme-174','-m','Edition 174 verified local procedure routing')
run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main',
    'HEAD:refs/heads/edition174','refs/tags/readme-174')
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition174-main',
    'refs/heads/edition174:refs/verify/edition174-branch','refs/tags/readme-174:refs/verify/edition174-tag')
assert git('rev-parse','refs/verify/edition174-branch') == release
assert git('rev-parse','refs/verify/edition174-tag^{commit}') == release
run('python','-X','utf8','docs/publication174/audit.py','refs/verify/edition174-main',release)
result = {'passed':True,'edition':174,'finalMain':release,'annotatedTag':'readme-174',
          'tagObject':git('rev-parse','readme-174'),'documentation':documentation,
          'externalEvidence':receipt('build/evidence174-upload.json'),
          'releaseAudit':receipt('build/publication-audit174.json')}
(root/'build/final-publication174.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf8')
print(json.dumps(result))
