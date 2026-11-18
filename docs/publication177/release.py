
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

assert git('branch', '--show-current') == 'edition177'
assert git('remote', 'get-url', '--push', 'origin').lower() == 'https://github.com/eyeoverthink/fraynix-agent-workspace.git'
documentation = receipt('build/documentation-check.json')
assert documentation['passed'] and documentation['editions'] == 177
for path in ['docs/readmes/177/verification.json','build/visual177.json']:
    assert receipt(path)['passed'], path
run('git','-c','gc.auto=0','fetch','origin','main')
run('git','merge-base','--is-ancestor','origin/main','HEAD')
paths = [
    'AGENTS.md','README-CONTRIBUTORS.md','README.md','README-177.md',
    'docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md',
    'docs/INTEGRATED_AGENT_ROADMAP.md',
    'docs/obsidian/00-home.md',
    'docs/obsidian/185-ramp-trace-chain.md','docs/obsidian/185-ramp-trace-chain.canvas',
    'docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/177','docs/publication177',
    'docs/RAMP_TRACE_CHAIN_177_LOCK.json'
]
run('git','add',*paths)
run('git','diff','--cached','--check')
run('git','-c','gc.auto=0','commit','-m','Document edition 177 reviewed ramp-to-analysis workflow')
assert not git('status','--porcelain')
release = git('rev-parse','HEAD')
run('python','-X','utf8','docs/publication177/audit.py','HEAD',release)
run('git','tag','-a','readme-177','-m','Edition 177 reviewed chain and verified handoff')
run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main',
    'HEAD:refs/heads/edition177','refs/tags/readme-177')
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition177-main',
    'refs/heads/edition177:refs/verify/edition177-branch','refs/tags/readme-177:refs/verify/edition177-tag')
assert git('rev-parse','refs/verify/edition177-branch') == release
assert git('rev-parse','refs/verify/edition177-tag^{commit}') == release
run('python','-X','utf8','docs/publication177/audit.py','refs/verify/edition177-main',release)
result = {'passed':True,'edition':177,'finalMain':release,'annotatedTag':'readme-177',
          'tagObject':git('rev-parse','readme-177'),'documentation':documentation,
          'externalEvidence':receipt('build/evidence177-upload.json'),
          'releaseAudit':receipt('build/publication-audit177.json')}
(root/'build/final-publication177.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf8')
print(json.dumps(result))
