from pathlib import Path
import subprocess,json
r=Path.cwd()
def git(*a):return subprocess.check_output(['git',*a],text=True).strip()
def run(*a,**kw):return subprocess.run(a,check=True,**kw)
assert json.loads((r/'build/documentation-check.json').read_text())['passed']
run('git','add','AGENTS.md','README.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/obsidian/103-mixed-task-costs.md','docs/PUBLICATION_095.json','docs/publication095')
run('git','diff','--cached','--check');run('git','-c','gc.auto=0','commit','-m','Record fetched edition 095 main publication and original evidence audit');head=git('rev-parse','HEAD')
with (r/'build/push095-receipt.log').open('wb') as f:run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition095',stdout=f,stderr=subprocess.STDOUT)
print((r/'build/push095-receipt.log').read_text(),flush=True)
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition095-main','refs/heads/edition095:refs/verify/edition095-branch','refs/tags/readme-095:refs/verify/edition095-tag')
assert git('rev-parse','refs/verify/edition095-branch')==head
assert git('rev-parse','refs/verify/edition095-tag')==git('rev-parse','readme-095')
run('python','build/audit-publication095.py','refs/verify/edition095-main',head)
assert not git('status','--porcelain');print('Verified final main '+head+'; working tree clean.')
