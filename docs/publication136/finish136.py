from pathlib import Path
import json,subprocess,shutil
from notes136 import status
r=Path.cwd();git=lambda *a:subprocess.check_output(['git',*a],text=True).strip();run=lambda *a:subprocess.run(a,check=True)
audit=json.loads((r/'build/publication-audit136.json').read_text(encoding='utf8'));assert audit['passed'] and audit['remoteFetched']
release=git('rev-parse','readme-136^{commit}');assert release==audit['commit']==git('rev-parse','HEAD')
v=json.loads((r/'docs/readmes/136/verification.json').read_text(encoding='utf8'))
receipt={'edition':136,'releaseCommit':release,'annotatedTag':'readme-136','tagObject':git('rev-parse','readme-136'),'audit':audit,'validation':v,'ci':{'scope':'CI checked before implementation. Hosted success is separate from local verification and fetched publication.'}}
for key,p in [('beforeImplementation','build/ci136-start.json'),('beforePublication','build/ci136-before-publication.json'),('releasePreflight','build/ci136-release-preflight.json'),('afterRelease','build/release136-ci.json')]:
 if (r/p).exists():receipt['ci'][key]=json.loads((r/p).read_text(encoding='utf8'))
(r/'docs/PUBLICATION_136.json').write_text(json.dumps(receipt,indent=2)+'\n',encoding='utf8')
s=status('published and fetched back',f"Release {release}; annotated readme-136. Main, edition136 and tag pushed atomically and fetched. Verified {audit['canonicalSourceFiles']} canonical sources, {audit['frozenHashes']} frozen hashes across136 editions and {audit['original136EvidenceFiles']} original evidence entries. See docs/PUBLICATION_136.json.")
for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
 p=r/name;p.write_text(s+p.read_text(encoding='utf-8-sig'),encoding='utf8')
p=r/'README.md';first,rest=p.read_text(encoding='utf8').split('\n',1);p.write_text(first+'\n\n[Published and fetched verification receipt](docs/PUBLICATION_136.json) Â· [Release tag](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/tree/readme-136)\n'+rest,encoding='utf8')
for name in ['push136.log','publication-audit136.json']:shutil.copy2(r/'build'/name,r/'docs/publication136'/name)
files=['AGENTS.md','README-CONTRIBUTORS.md','README.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md','docs/PUBLICATION_136.json','docs/publication136']
run('git','add',*files);run('git','add','-f','docs/publication136/push136.log');run('git','diff','--cached','--check','--','.',':!docs/publication136/*.log');run('git','-c','gc.auto=0','commit','-m','Record fetched edition 136 identity recovery comparisons, sources and original evidence')
assert not git('status','--porcelain');head=git('rev-parse','HEAD');changed=git('diff','--name-only',release,head).splitlines();assert all(n in files or n.startswith('docs/publication136/') for n in changed)
run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition136')
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition136-final-main','refs/heads/edition136:refs/verify/edition136-final-branch','refs/tags/readme-136:refs/verify/edition136-final-tag')
assert head==git('rev-parse','refs/verify/edition136-final-main')==git('rev-parse','refs/verify/edition136-final-branch');assert receipt['tagObject']==git('rev-parse','refs/verify/edition136-final-tag');assert release==git('rev-parse','refs/verify/edition136-final-tag^{commit}')
for name in changed:assert subprocess.check_output(['git','show',head+':'+name])==subprocess.check_output(['git','show','refs/verify/edition136-final-main:'+name]),name
assert not git('status','--porcelain');result={'passed':True,'finalMain':head,'release':release,'tag':'readme-136','metadataFilesCompared':len(changed),'scope':'Changed metadata compared byte-for-byte with freshly fetched main; production and frozen evidence unchanged from complete fetched release audit.'};(r/'build/final-publication136.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf8');print(json.dumps(result))
