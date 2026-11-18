from pathlib import Path
import subprocess,json
r=Path.cwd();git=lambda *a:subprocess.check_output(['git',*a],text=True).strip();run=lambda *a:subprocess.run(a,check=True)
assert git('remote','get-url','--push','origin').lower()=='https://github.com/eyeoverthink/fraynix-agent-workspace.git'
assert git('branch','--show-current')=='edition164'
for name in ['docs/readmes/164/verification.json','build/documentation-check.json','build/visual164.json','build/visual-review164.json']:assert json.loads((r/name).read_bytes())['passed'],name
run('git','-c','gc.auto=0','fetch','origin','main');run('git','merge-base','--is-ancestor','origin/main','HEAD')
files=['AGENTS.md','README-CONTRIBUTORS.md','README.md','README-164.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md','docs/INTEGRATED_AGENT_ROADMAP.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md','docs/obsidian/00-home.md','docs/obsidian/172-history-report-costs.md','docs/obsidian/172-history-report-costs.canvas','docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/164','docs/publication164']
run('git','add',*files);run('git','diff','--cached','--check');run('git','-c','gc.auto=0','commit','-m','Document edition 164 longer mixed workflow history and report costs')
assert not git('status','--porcelain');release=git('rev-parse','HEAD');run('python','-X','utf8','docs/publication164/audit164.py','HEAD',release)
run('git','tag','-a','readme-164','-m','Edition 164: physics history and reports across longer mixed workflows')
run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition164','refs/tags/readme-164')
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition164-main','refs/heads/edition164:refs/verify/edition164-branch','refs/tags/readme-164:refs/verify/edition164-tag')
assert git('rev-parse','refs/verify/edition164-branch')==release and git('rev-parse','refs/verify/edition164-tag^{commit}')==release
run('python','-X','utf8','docs/publication164/audit164.py','refs/verify/edition164-main',release)
receipt={'edition':164,'releaseCommit':release,'annotatedTag':'readme-164','tagObject':git('rev-parse','readme-164'),'audit':json.loads((r/'build/publication-audit164.json').read_bytes()),'validation':'docs/readmes/164/verification.json','documentation':json.loads((r/'build/documentation-check.json').read_bytes()),'ci':json.loads((r/'build/ci164-before-publication.json').read_bytes())}
(r/'docs/PUBLICATION_164.json').write_bytes((json.dumps(receipt,indent=2)+'\n').encode())
status='## Edition 164 — published and fetched back\n\n2026-11-17. Release '+release+'; main, edition164 and readme-164 pushed atomically and fetched. Full164 source/evidence inventory and all historical frozen hashes verified from fetched Git blobs. See docs/PUBLICATION_164.json. Bounded timing and read-only history inspection shipped; original native class bytes, tracker and controller defaults unchanged. User selected recorded tests; fresh physical validation remains a separate gate. The following local-study notes retain their original chronology.\n\n'
metadata=['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']
for name in metadata:
 p=r/name;p.write_bytes((status+p.read_text(encoding='utf-8-sig')).encode())
p=r/'README.md';p.write_bytes((p.read_text(encoding='utf8').replace('\n','\n\n[Published and fetched receipt](docs/PUBLICATION_164.json) · [Release tag](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/tree/readme-164)\n',1)).encode())
metadata+=['README.md','docs/PUBLICATION_164.json'];run('git','add',*metadata);run('git','diff','--cached','--check');run('git','-c','gc.auto=0','commit','-m','Record fetched edition 164 release and continuity')
assert not git('status','--porcelain');head=git('rev-parse','HEAD');assert set(git('diff','--name-only',release,head).splitlines())<=set(metadata)
run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition164')
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition164-final-main','refs/heads/edition164:refs/verify/edition164-final-branch')
assert git('rev-parse','refs/verify/edition164-final-main')==head==git('rev-parse','refs/verify/edition164-final-branch')
for name in metadata:assert subprocess.check_output(['git','show','HEAD:'+name])==subprocess.check_output(['git','show','refs/verify/edition164-final-main:'+name])
result={'passed':True,'release':release,'finalMain':head,'tag':'readme-164','releaseAudit':receipt['audit'],'finalMetadataCompared':len(metadata)};(r/'build/final-publication164.json').write_bytes((json.dumps(result,indent=2)+'\n').encode());print(json.dumps(result))
