from pathlib import Path
import json,subprocess,shutil
from notes127 import status
r=Path.cwd();git=lambda *a:subprocess.check_output(['git',*a],text=True).strip();run=lambda *a:subprocess.run(a,check=True)
audit=json.loads((r/'build/publication-audit127.json').read_text(encoding='utf8'));assert audit['passed'] and audit['remoteFetched']
release=git('rev-parse','readme-127^{commit}');assert release==audit['commit']==git('rev-parse','HEAD')
v=json.loads((r/'docs/readmes/127/verification.json').read_text(encoding='utf8'))
receipt={'edition':127,'releaseCommit':release,'annotatedTag':'readme-127','tagObject':git('rev-parse','readme-127'),'audit':audit,'validation':v,'ci':{'scope':'CI checked before implementation. Hosted success is separate from local verification and fetched publication.'}}
for key,p in [('beforeImplementation','build/ci127-start.json'),('beforePublication','build/ci127-before-publication.json'),('releasePreflight','build/ci127-release-preflight.json'),('afterRelease','build/release127-ci.json')]:
 if (r/p).exists():receipt['ci'][key]=json.loads((r/p).read_text(encoding='utf8'))
(r/'docs/PUBLICATION_127.json').write_text(json.dumps(receipt,indent=2)+'\n',encoding='utf8')
s=status('published and fetched back',f"Release {release}; annotated readme-127. Main, edition127 and tag pushed atomically and fetched. Verified {audit['canonicalSourceFiles']} canonical sources, {audit['frozenHashes']} frozen hashes across127 editions and {audit['original127EvidenceFiles']} original evidence entries. See docs/PUBLICATION_127.json.")
for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
 p=r/name;p.write_text(s+p.read_text(encoding='utf-8-sig'),encoding='utf8')
p=r/'README.md';first,rest=p.read_text(encoding='utf8').split('\n',1);p.write_text(first+'\n\n[Published and fetched verification receipt](docs/PUBLICATION_127.json) · [Release tag](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/tree/readme-127)\n'+rest,encoding='utf8')
for name in ['push127.log','publication-audit127.json']:shutil.copy2(r/'build'/name,r/'docs/publication127'/name)
files=['AGENTS.md','README-CONTRIBUTORS.md','README.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md','docs/PUBLICATION_127.json','docs/publication127']
run('git','add',*files);run('git','add','-f','docs/publication127/push127.log');run('git','diff','--cached','--check','--','.',':!docs/publication127/*.log');run('git','-c','gc.auto=0','commit','-m','Record fetched edition 127 procedure lifecycle results, sources and original evidence')
assert not git('status','--porcelain');head=git('rev-parse','HEAD');changed=git('diff','--name-only',release,head).splitlines();assert all(n in files or n.startswith('docs/publication127/') for n in changed)
run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition127')
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition127-final-main','refs/heads/edition127:refs/verify/edition127-final-branch','refs/tags/readme-127:refs/verify/edition127-final-tag')
assert head==git('rev-parse','refs/verify/edition127-final-main')==git('rev-parse','refs/verify/edition127-final-branch');assert receipt['tagObject']==git('rev-parse','refs/verify/edition127-final-tag');assert release==git('rev-parse','refs/verify/edition127-final-tag^{commit}')
for name in changed:assert subprocess.check_output(['git','show',head+':'+name])==subprocess.check_output(['git','show','refs/verify/edition127-final-main:'+name]),name
assert not git('status','--porcelain');result={'passed':True,'finalMain':head,'release':release,'tag':'readme-127','metadataFilesCompared':len(changed),'scope':'Changed metadata compared byte-for-byte with freshly fetched main; production and frozen evidence unchanged from complete fetched release audit.'};(r/'build/final-publication127.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf8');print(json.dumps(result))
