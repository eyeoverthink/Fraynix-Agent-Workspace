from pathlib import Path
import subprocess,json
r=Path.cwd();git=lambda *a:subprocess.check_output(['git',*a],text=True).strip();run=lambda *a:subprocess.run(a,check=True)
assert git('remote','get-url','--push','origin').lower()=='https://github.com/eyeoverthink/fraynix-agent-workspace.git'
assert git('branch','--show-current')=='edition165'
for name in ['docs/readmes/165/verification.json','build/documentation-check.json','build/visual165.json','build/visual-review165.json']:assert json.loads((r/name).read_bytes())['passed'],name
run('git','-c','gc.auto=0','fetch','origin','main');run('git','merge-base','--is-ancestor','origin/main','HEAD')
files=['AGENTS.md','README-CONTRIBUTORS.md','README.md','README-165.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md','docs/INTEGRATED_AGENT_ROADMAP.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md','docs/obsidian/00-home.md','docs/obsidian/173-verified-evidence-reuse.md','docs/obsidian/173-verified-evidence-reuse.canvas','docs/readmes/INDEX.md','docs/readmes/editions.json','docs/readmes/165','docs/publication165','docs/ARCHITECTURE_RENEWAL_PLAN.md','scripts/evidence_archive.py','scripts/test_evidence_archive.py','.github/workflows/build.yml']
run('git','add',*files);run('git','diff','--cached','--check');run('git','-c','gc.auto=0','commit','-m','Document edition 165 verified reuse and architecture renewal')
assert not git('status','--porcelain');release=git('rev-parse','HEAD');run('python','-X','utf8','docs/publication165/audit165.py','HEAD',release)
run('git','tag','-a','readme-165','-m','Edition 165: verified source/history reuse and retrievable evidence')
run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition165','refs/tags/readme-165')
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition165-main','refs/heads/edition165:refs/verify/edition165-branch','refs/tags/readme-165:refs/verify/edition165-tag')
assert git('rev-parse','refs/verify/edition165-branch')==release and git('rev-parse','refs/verify/edition165-tag^{commit}')==release
run('python','-X','utf8','docs/publication165/audit165.py','refs/verify/edition165-main',release)
receipt={'edition':165,'externalEvidence':json.loads((r/'build/evidence165-upload.json').read_bytes()),'releaseCommit':release,'annotatedTag':'readme-165','tagObject':git('rev-parse','readme-165'),'audit':json.loads((r/'build/publication-audit165.json').read_bytes()),'validation':'docs/readmes/165/verification.json','documentation':json.loads((r/'build/documentation-check.json').read_bytes()),'ci':json.loads((r/'build/ci165-before-publication.json').read_bytes())}
(r/'docs/PUBLICATION_165.json').write_bytes((json.dumps(receipt,indent=2)+'\n').encode())
status='## Edition 165 — published and fetched back\n\n2026-11-17. Release '+release+'; main, edition165 and readme-165 pushed atomically and fetched. Full165 source/evidence inventory and all historical frozen hashes verified from fetched Git blobs. See docs/PUBLICATION_165.json. Verified source/history reuse and retrievable external evidence shipped; original native class bytes, tracker and controller defaults unchanged. User selected recorded tests; fresh physical validation remains a separate gate. The following local-study notes retain their original chronology.\n\n'
metadata=['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']
for name in metadata:
 p=r/name;p.write_bytes((status+p.read_text(encoding='utf-8-sig')).encode())
p=r/'README.md';p.write_bytes((p.read_text(encoding='utf8').replace('\n','\n\n[Published and fetched receipt](docs/PUBLICATION_165.json) · [Release tag](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/tree/readme-165)\n',1)).encode())
metadata+=['README.md','docs/PUBLICATION_165.json'];run('git','add',*metadata);run('git','diff','--cached','--check');run('git','-c','gc.auto=0','commit','-m','Record fetched edition 165 release and continuity')
assert not git('status','--porcelain');head=git('rev-parse','HEAD');assert set(git('diff','--name-only',release,head).splitlines())<=set(metadata)
run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition165')
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition165-final-main','refs/heads/edition165:refs/verify/edition165-final-branch')
assert git('rev-parse','refs/verify/edition165-final-main')==head==git('rev-parse','refs/verify/edition165-final-branch')
for name in metadata:assert subprocess.check_output(['git','show','HEAD:'+name])==subprocess.check_output(['git','show','refs/verify/edition165-final-main:'+name])
result={'passed':True,'release':release,'finalMain':head,'tag':'readme-165','releaseAudit':receipt['audit'],'finalMetadataCompared':len(metadata)};(r/'build/final-publication165.json').write_bytes((json.dumps(result,indent=2)+'\n').encode());print(json.dumps(result))
