from pathlib import Path
import json,subprocess,shutil
from notes150 import status
r=Path.cwd();git=lambda *a:subprocess.check_output(['git',*a],text=True).strip();run=lambda *a:subprocess.run(a,check=True)
assert not git('status','--porcelain'),'Preserve unrelated changes before recording publication metadata'
audit=json.loads((r/'build/publication-audit150.json').read_text(encoding='utf8'));assert audit['passed'] and audit['remoteFetched']
release=git('rev-parse','readme-150^{commit}');assert release==audit['commit']==git('rev-parse','HEAD')
v=json.loads((r/'docs/readmes/150/verification.json').read_text(encoding='utf8'))
receipt={'edition':150,'releaseCommit':release,'annotatedTag':'readme-150','tagObject':git('rev-parse','readme-150'),'audit':audit,'validation':v,'ci':{'scope':'CI checked before implementation. Hosted success is separate from local verification and fetched publication.'}}
for key,p in [('beforeImplementation','docs/publication150/ci-start150.json'),('predecessorJobs','docs/publication150/ci-predecessor-jobs150.json'),('beforePublication','build/ci150-before-publication.json'),('releasePreflight','build/ci150-release-preflight.json'),('afterRelease','build/release150-ci.json')]:
 if (r/p).exists():receipt['ci'][key]=json.loads((r/p).read_text(encoding='utf8'))
(r/'docs/PUBLICATION_150.json').write_text(json.dumps(receipt,indent=2)+'\n',encoding='utf8')
s=status('published and fetched back',f"Release {release}; annotated readme-150. Main, edition150 and tag pushed atomically and fetched. Verified {audit['canonicalSourceFiles']} canonical sources, {audit['frozenHashes']} frozen hashes across 150 editions and {audit['original150EvidenceFiles']} evidence entries, including {audit.get('privateReceiptViews',0)} public views of unchanged private receipts. See docs/PUBLICATION_150.json.")
for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
 p=r/name;p.write_text(s+p.read_text(encoding='utf-8-sig'),encoding='utf8')
p=r/'README.md';first,rest=p.read_text(encoding='utf8').split('\n',1);p.write_text(first+'\n\n[Published and fetched verification receipt](docs/PUBLICATION_150.json) · [Release tag](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/tree/readme-150)\n'+rest,encoding='utf8')
for name in ['push150.log','publication-audit150.json']:shutil.copy2(r/'build'/name,r/'docs/publication150'/name)
files=['AGENTS.md','README-CONTRIBUTORS.md','README.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md','docs/PUBLICATION_150.json','docs/publication150/push150.log','docs/publication150/publication-audit150.json']
run('git','add',*[name for name in files if not name.endswith('.log')]);run('git','add','-f','docs/publication150/push150.log');run('git','diff','--cached','--check','--','.',':!docs/publication150/*.log',':!docs/publication150/*-groundtruth.txt');run('git','-c','gc.auto=0','commit','-m','Record fetched edition 150 starting-reference study results and original evidence')
assert not git('status','--porcelain');head=git('rev-parse','HEAD');changed=git('diff','--name-only',release,head).splitlines();assert all(n in files for n in changed)
run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition150')
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition150-final-main','refs/heads/edition150:refs/verify/edition150-final-branch','refs/tags/readme-150:refs/verify/edition150-final-tag')
assert head==git('rev-parse','refs/verify/edition150-final-main')==git('rev-parse','refs/verify/edition150-final-branch');assert receipt['tagObject']==git('rev-parse','refs/verify/edition150-final-tag');assert release==git('rev-parse','refs/verify/edition150-final-tag^{commit}')
for name in changed:assert subprocess.check_output(['git','show',head+':'+name])==subprocess.check_output(['git','show','refs/verify/edition150-final-main:'+name]),name
assert not git('status','--porcelain');result={'passed':True,'finalMain':head,'release':release,'tag':'readme-150','metadataFilesCompared':len(changed),'scope':'Changed metadata compared byte-for-byte with freshly fetched main; production and frozen evidence unchanged from complete fetched release audit.'};(r/'build/final-publication150.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf8');print(json.dumps(result))
