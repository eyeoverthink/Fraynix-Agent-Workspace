from pathlib import Path
import subprocess,json
r=Path.cwd()
def git(*a):return subprocess.check_output(['git',*a],text=True).strip()
def run(*a,**kw):return subprocess.run(a,check=True,**kw)
assert json.loads((r/'build/documentation-check.json').read_text())['passed']
run('git','add','AGENTS.md','README.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/obsidian/101-recovered-state-forecast.md','docs/PUBLICATION_093.json','docs/publication093')
run('git','diff','--cached','--check');run('git','-c','gc.auto=0','commit','-m','Record fetched edition 093 main publication and original evidence audit');head=git('rev-parse','HEAD')
with (r/'build/push093-receipt.log').open('wb') as f:run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition093',stdout=f,stderr=subprocess.STDOUT)
print((r/'build/push093-receipt.log').read_text(),flush=True)
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition093-main','refs/heads/edition093:refs/verify/edition093-branch','refs/tags/readme-093:refs/verify/edition093-tag')
assert git('rev-parse','refs/verify/edition093-branch')==head
assert git('rev-parse','refs/verify/edition093-tag')==git('rev-parse','readme-093')
run('python','build/audit-publication093.py','refs/verify/edition093-main',head)
assert not git('status','--porcelain');print('Verified final main '+head+'; working tree clean.')
