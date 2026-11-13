from pathlib import Path
import json,subprocess,shutil
from notes130 import status
r=Path.cwd();git=lambda *a:subprocess.check_output(['git',*a],text=True).strip();run=lambda *a:subprocess.run(a,check=True)
audit=json.loads((r/'build/publication-audit130.json').read_text(encoding='utf8'));assert audit['passed'] and audit['remoteFetched']
release=git('rev-parse','readme-130^{commit}');assert release==audit['commit']==git('rev-parse','HEAD')
v=json.loads((r/'docs/readmes/130/verification.json').read_text(encoding='utf8'))
receipt={'edition':130,'releaseCommit':release,'annotatedTag':'readme-130','tagObject':git('rev-parse','readme-130'),'audit':audit,'validation':v,'ci':{'scope':'CI checked before implementation. Hosted success is separate from local verification and fetched publication.'}}
for key,p in [('beforeImplementation','build/ci130-start.json'),('beforePublication','build/ci130-before-publication.json'),('releasePreflight','build/ci130-release-preflight.json'),('afterRelease','build/release130-ci.json')]:
 if (r/p).exists():receipt['ci'][key]=json.loads((r/p).read_text(encoding='utf8'))
(r/'docs/PUBLICATION_130.json').write_text(json.dumps(receipt,indent=2)+'\n',encoding='utf8')
s=status('published and fetched back',f"Release {release}; annotated readme-130. Main, edition130 and tag pushed atomically and fetched. Verified {audit['canonicalSourceFiles']} canonical sources, {audit['frozenHashes']} frozen hashes across130 editions and {audit['original130EvidenceFiles']} original evidence entries. See docs/PUBLICATION_130.json.")
for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
 p=r/name;p.write_text(s+p.read_text(encoding='utf-8-sig'),encoding='utf8')
p=r/'README.md';first,rest=p.read_text(encoding='utf8').split('\n',1);p.write_text(first+'\n\n[Published and fetched verification receipt](docs/PUBLICATION_130.json) · [Release tag](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/tree/readme-130)\n'+rest,encoding='utf8')
for name in ['push130.log','publication-audit130.json']:shutil.copy2(r/'build'/name,r/'docs/publication130'/name)
files=['AGENTS.md','README-CONTRIBUTORS.md','README.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md','docs/PUBLICATION_130.json','docs/publication130']
run('git','add',*files);run('git','add','-f','docs/publication130/push130.log');run('git','diff','--cached','--check','--','.',':!docs/publication130/*.log');run('git','-c','gc.auto=0','commit','-m','Record fetched edition 130 tracking and forecast results, sources and original evidence')
assert not git('status','--porcelain');head=git('rev-parse','HEAD');changed=git('diff','--name-only',release,head).splitlines();assert all(n in files or n.startswith('docs/publication130/') for n in changed)
run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition130')
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition130-final-main','refs/heads/edition130:refs/verify/edition130-final-branch','refs/tags/readme-130:refs/verify/edition130-final-tag')
assert head==git('rev-parse','refs/verify/edition130-final-main')==git('rev-parse','refs/verify/edition130-final-branch');assert receipt['tagObject']==git('rev-parse','refs/verify/edition130-final-tag');assert release==git('rev-parse','refs/verify/edition130-final-tag^{commit}')
for name in changed:assert subprocess.check_output(['git','show',head+':'+name])==subprocess.check_output(['git','show','refs/verify/edition130-final-main:'+name]),name
assert not git('status','--porcelain');result={'passed':True,'finalMain':head,'release':release,'tag':'readme-130','metadataFilesCompared':len(changed),'scope':'Changed metadata compared byte-for-byte with freshly fetched main; production and frozen evidence unchanged from complete fetched release audit.'};(r/'build/final-publication130.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf8');print(json.dumps(result))
