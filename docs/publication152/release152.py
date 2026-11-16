from pathlib import Path
import subprocess,json
r=Path.cwd();git=lambda *a:subprocess.check_output(['git',*a],text=True).strip();run=lambda *a:subprocess.run(a,check=True)
assert git('remote','get-url','--push','origin').lower()=='https://github.com/eyeoverthink/fraynix-agent-workspace.git'
assert git('branch','--show-current')=='edition152'
for name in ['docs/readmes/152/verification.json','build/documentation-check.json','build/visual152.json','build/visual-review152.json']:assert json.loads((r/name).read_bytes())['passed'],name
run('git','-c','gc.auto=0','fetch','origin','main');run('git','merge-base','--is-ancestor','origin/main','HEAD')
files=['AGENTS.md','README-CONTRIBUTORS.md','README.md','README-152.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md','docs/INTEGRATED_AGENT_ROADMAP.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md','docs/obsidian/00-home.md','docs/obsidian/160-rotation-search.md','docs/obsidian/160-rotation-search.canvas','docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/152','docs/publication152']
run('git','add',*files);run('git','diff','--cached','--check');run('git','-c','gc.auto=0','commit','-m','Document edition 152 rotation search, independent controls and replay')
assert not git('status','--porcelain');release=git('rev-parse','HEAD');run('python','-X','utf8','docs/publication152/audit152.py','HEAD',release)
run('git','tag','-a','readme-152','-m','Edition 152: rotation search without supplied target locations')
run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition152','refs/tags/readme-152')
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition152-main','refs/heads/edition152:refs/verify/edition152-branch','refs/tags/readme-152:refs/verify/edition152-tag')
assert git('rev-parse','refs/verify/edition152-branch')==release and git('rev-parse','refs/verify/edition152-tag^{commit}')==release
run('python','-X','utf8','docs/publication152/audit152.py','refs/verify/edition152-main',release)
receipt={'edition':152,'releaseCommit':release,'annotatedTag':'readme-152','tagObject':git('rev-parse','readme-152'),'audit':json.loads((r/'build/publication-audit152.json').read_bytes()),'validation':'docs/readmes/152/verification.json','ci':json.loads((r/'build/ci152-before-publication.json').read_bytes())}
(r/'docs/PUBLICATION_152.json').write_bytes((json.dumps(receipt,indent=2)+'\n').encode())
status='## Edition 152 — published and fetched back\n\n2026-11-16. Release '+release+'; main, edition152 and readme-152 pushed atomically and fetched. Full152 source/evidence inventory and all historical frozen hashes verified from fetched Git blobs. See docs/PUBLICATION_152.json. Experimental search shipped; defaults unchanged and fresh physical validation pending. The following local-study notes retain their original chronology.\n\n'
metadata=['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']
for name in metadata:
 p=r/name;p.write_bytes((status+p.read_text(encoding='utf-8-sig')).encode())
p=r/'README.md';p.write_bytes((p.read_text(encoding='utf8').replace('\n','\n\n[Published and fetched receipt](docs/PUBLICATION_152.json) · [Release tag](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/tree/readme-152)\n',1)).encode())
metadata+=['README.md','docs/PUBLICATION_152.json'];run('git','add',*metadata);run('git','diff','--cached','--check');run('git','-c','gc.auto=0','commit','-m','Record fetched edition 152 release and continuity')
assert not git('status','--porcelain');head=git('rev-parse','HEAD');assert set(git('diff','--name-only',release,head).splitlines())<=set(metadata)
run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition152')
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition152-final-main','refs/heads/edition152:refs/verify/edition152-final-branch')
assert git('rev-parse','refs/verify/edition152-final-main')==head==git('rev-parse','refs/verify/edition152-final-branch')
for name in metadata:assert subprocess.check_output(['git','show','HEAD:'+name])==subprocess.check_output(['git','show','refs/verify/edition152-final-main:'+name])
result={'passed':True,'release':release,'finalMain':head,'tag':'readme-152','releaseAudit':receipt['audit'],'finalMetadataCompared':len(metadata)};(r/'build/final-publication152.json').write_bytes((json.dumps(result,indent=2)+'\n').encode());print(json.dumps(result))
