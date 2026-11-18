from pathlib import Path
import subprocess,json
r=Path.cwd();git=lambda *a:subprocess.check_output(['git',*a],text=True).strip();run=lambda *a:subprocess.run(a,check=True)
assert git('remote','get-url','--push','origin').lower()=='https://github.com/eyeoverthink/fraynix-agent-workspace.git'
assert git('branch','--show-current')=='edition163'
for name in ['docs/readmes/163/verification.json','build/documentation-check.json','build/visual163.json','build/visual-review163.json']:assert json.loads((r/name).read_bytes())['passed'],name
run('git','-c','gc.auto=0','fetch','origin','main');run('git','merge-base','--is-ancestor','origin/main','HEAD')
files=['AGENTS.md','README-CONTRIBUTORS.md','README.md','README-163.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md','docs/INTEGRATED_AGENT_ROADMAP.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md','docs/obsidian/00-home.md','docs/obsidian/171-recorded-workflow-costs.md','docs/obsidian/171-recorded-workflow-costs.canvas','docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/163','docs/publication163']
run('git','add',*files);run('git','diff','--cached','--check');run('git','-c','gc.auto=0','commit','-m','Document edition 163 recorded-workflow verification reuse')
assert not git('status','--porcelain');release=git('rev-parse','HEAD');run('python','-X','utf8','docs/publication163/audit163.py','HEAD',release)
run('git','tag','-a','readme-163','-m','Edition 163: complete workflow costs and verified reconstruction reuse')
run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition163','refs/tags/readme-163')
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition163-main','refs/heads/edition163:refs/verify/edition163-branch','refs/tags/readme-163:refs/verify/edition163-tag')
assert git('rev-parse','refs/verify/edition163-branch')==release and git('rev-parse','refs/verify/edition163-tag^{commit}')==release
run('python','-X','utf8','docs/publication163/audit163.py','refs/verify/edition163-main',release)
receipt={'edition':163,'releaseCommit':release,'annotatedTag':'readme-163','tagObject':git('rev-parse','readme-163'),'audit':json.loads((r/'build/publication-audit163.json').read_bytes()),'validation':'docs/readmes/163/verification.json','ci':json.loads((r/'build/ci163-before-publication.json').read_bytes())}
(r/'docs/PUBLICATION_163.json').write_bytes((json.dumps(receipt,indent=2)+'\n').encode())
status='## Edition 163 — published and fetched back\n\n2026-11-17. Release '+release+'; main, edition163 and readme-163 pushed atomically and fetched. Full163 source/evidence inventory and all historical frozen hashes verified from fetched Git blobs. See docs/PUBLICATION_163.json. Experimental recorded-workflow verification reuse shipped; tracker and controller defaults unchanged. User selected recorded tests; fresh physical validation remains a separate gate. The following local-study notes retain their original chronology.\n\n'
metadata=['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']
for name in metadata:
 p=r/name;p.write_bytes((status+p.read_text(encoding='utf-8-sig')).encode())
p=r/'README.md';p.write_bytes((p.read_text(encoding='utf8').replace('\n','\n\n[Published and fetched receipt](docs/PUBLICATION_163.json) · [Release tag](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/tree/readme-163)\n',1)).encode())
metadata+=['README.md','docs/PUBLICATION_163.json'];run('git','add',*metadata);run('git','diff','--cached','--check');run('git','-c','gc.auto=0','commit','-m','Record fetched edition 163 release and continuity')
assert not git('status','--porcelain');head=git('rev-parse','HEAD');assert set(git('diff','--name-only',release,head).splitlines())<=set(metadata)
run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition163')
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition163-final-main','refs/heads/edition163:refs/verify/edition163-final-branch')
assert git('rev-parse','refs/verify/edition163-final-main')==head==git('rev-parse','refs/verify/edition163-final-branch')
for name in metadata:assert subprocess.check_output(['git','show','HEAD:'+name])==subprocess.check_output(['git','show','refs/verify/edition163-final-main:'+name])
result={'passed':True,'release':release,'finalMain':head,'tag':'readme-163','releaseAudit':receipt['audit'],'finalMetadataCompared':len(metadata)};(r/'build/final-publication163.json').write_bytes((json.dumps(result,indent=2)+'\n').encode());print(json.dumps(result))
