from pathlib import Path
import subprocess,json
r=Path.cwd();git=lambda *a:subprocess.check_output(['git',*a],text=True).strip();run=lambda *a:subprocess.run(a,check=True)
assert git('remote','get-url','--push','origin').lower()=='https://github.com/eyeoverthink/fraynix-agent-workspace.git'
assert git('branch','--show-current')=='edition153'
for name in ['docs/readmes/153/verification.json','build/documentation-check.json','build/visual153.json','build/visual-review153.json']:assert json.loads((r/name).read_bytes())['passed'],name
run('git','-c','gc.auto=0','fetch','origin','main');run('git','merge-base','--is-ancestor','origin/main','HEAD')
files=['AGENTS.md','README-CONTRIBUTORS.md','README.md','README-153.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md','docs/INTEGRATED_AGENT_ROADMAP.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md','docs/obsidian/00-home.md','docs/obsidian/161-rotated-confirmation.md','docs/obsidian/161-rotated-confirmation.canvas','docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/153','docs/publication153']
run('git','add',*files);run('git','diff','--cached','--check');run('git','-c','gc.auto=0','commit','-m','Document edition 153 rotated confirmation, exact arithmetic and measured costs')
assert not git('status','--porcelain');release=git('rev-parse','HEAD');run('python','-X','utf8','docs/publication153/audit153.py','HEAD',release)
run('git','tag','-a','readme-153','-m','Edition 153: confirmed rotated returns and exact search optimization')
run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition153','refs/tags/readme-153')
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition153-main','refs/heads/edition153:refs/verify/edition153-branch','refs/tags/readme-153:refs/verify/edition153-tag')
assert git('rev-parse','refs/verify/edition153-branch')==release and git('rev-parse','refs/verify/edition153-tag^{commit}')==release
run('python','-X','utf8','docs/publication153/audit153.py','refs/verify/edition153-main',release)
receipt={'edition':153,'releaseCommit':release,'annotatedTag':'readme-153','tagObject':git('rev-parse','readme-153'),'audit':json.loads((r/'build/publication-audit153.json').read_bytes()),'validation':'docs/readmes/153/verification.json','ci':json.loads((r/'build/ci153-before-publication.json').read_bytes())}
(r/'docs/PUBLICATION_153.json').write_bytes((json.dumps(receipt,indent=2)+'\n').encode())
status='## Edition 153 — published and fetched back\n\n2026-11-16. Release '+release+'; main, edition153 and readme-153 pushed atomically and fetched. Full153 source/evidence inventory and all historical frozen hashes verified from fetched Git blobs. See docs/PUBLICATION_153.json. Experimental rotated confirmation and exact arithmetic optimization shipped; defaults unchanged. User selected recorded tests; fresh physical validation remains a separate gate. The following local-study notes retain their original chronology.\n\n'
metadata=['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']
for name in metadata:
 p=r/name;p.write_bytes((status+p.read_text(encoding='utf-8-sig')).encode())
p=r/'README.md';p.write_bytes((p.read_text(encoding='utf8').replace('\n','\n\n[Published and fetched receipt](docs/PUBLICATION_153.json) · [Release tag](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/tree/readme-153)\n',1)).encode())
metadata+=['README.md','docs/PUBLICATION_153.json'];run('git','add',*metadata);run('git','diff','--cached','--check');run('git','-c','gc.auto=0','commit','-m','Record fetched edition 153 release and continuity')
assert not git('status','--porcelain');head=git('rev-parse','HEAD');assert set(git('diff','--name-only',release,head).splitlines())<=set(metadata)
run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition153')
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition153-final-main','refs/heads/edition153:refs/verify/edition153-final-branch')
assert git('rev-parse','refs/verify/edition153-final-main')==head==git('rev-parse','refs/verify/edition153-final-branch')
for name in metadata:assert subprocess.check_output(['git','show','HEAD:'+name])==subprocess.check_output(['git','show','refs/verify/edition153-final-main:'+name])
result={'passed':True,'release':release,'finalMain':head,'tag':'readme-153','releaseAudit':receipt['audit'],'finalMetadataCompared':len(metadata)};(r/'build/final-publication153.json').write_bytes((json.dumps(result,indent=2)+'\n').encode());print(json.dumps(result))
