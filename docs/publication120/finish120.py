from pathlib import Path
import json,subprocess,shutil
from notes120 import status
r=Path.cwd();git=lambda *a:subprocess.check_output(['git',*a],text=True).strip();run=lambda *a:subprocess.run(a,check=True)
audit=json.loads((r/'build/publication-audit120.json').read_text(encoding='utf8'));assert audit['passed'] and audit['remoteFetched']
release=git('rev-parse','readme-120^{commit}');assert release==audit['commit']==git('rev-parse','HEAD')
ui=json.loads((r/'docs/readmes/120/actual-ui.json').read_text(encoding='utf8'));v=json.loads((r/'docs/readmes/120/verification.json').read_text(encoding='utf8'))
receipt={'edition':120,'releaseCommit':release,'annotatedTag':'readme-120','tagObject':git('rev-parse','readme-120'),'audit':audit,'validation':{'javaMethods':len(v['javaChecks']),'desktopChecks':v['desktopChecks'],'actualRequests':[{'request':x['prompt'],'status':x['task']['status'],'elapsedMs':x['elapsedMs'],'trialOutcomes':[t['status'] for t in x['child']['trials']],'audit':x['child'].get('audit')} for x in ui['requests']],'actualAbruptRecovery':True,'referenceChecks':v['referenceCases'],'controllerCases':v['controllerCases'],'referenceControllerResults':v['referenceControllerResults'],'modelCalls':v['modelCalls'],'guideWidths':[1440,390],'scope':v['scope']},'ci':{'scope':'CI checked before implementation. Prior119 jobs pending. Local verification and fetched publication do not establish remote CI success.'}}
ci=r/'build/release120-ci.json'
if ci.exists():receipt['ci']['releaseCheck']=json.loads(ci.read_text(encoding='utf8'))
(r/'docs/PUBLICATION_120.json').write_bytes((json.dumps(receipt,indent=2)+'\n').encode())
s=status('published and fetched back',f"Release {release}; annotated readme-120. Main, edition120 and tag pushed atomically and fetched. Verified {audit['canonicalSourceFiles']} canonical sources, {audit['frozenHashes']} frozen hashes across120 editions and {audit['original120EvidenceFiles']} original evidence entries. See docs/PUBLICATION_120.json.")
for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
 p=r/name;p.write_bytes((s+p.read_text(encoding='utf-8-sig')).encode())
p=r/'README.md';first,rest=p.read_text(encoding='utf8').split('\n',1);link='[Published and fetched verification receipt](docs/PUBLICATION_120.json) · [Release tag](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/tree/readme-120)';p.write_bytes((first+'\n\n'+link+'\n'+rest).encode())
for name in ['push120.log','publication-audit120.json']:shutil.copy2(r/'build'/name,r/'docs/publication120'/name)
files=['AGENTS.md','README-CONTRIBUTORS.md','README.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md','docs/PUBLICATION_120.json','docs/publication120']
run('git','add',*files);run('git','add','-f','docs/publication120/push120.log');run('git','diff','--cached','--check','--','.',':!docs/publication120/*.log');run('git','-c','gc.auto=0','commit','-m','Record fetched edition 120 sources, verified waiting and original evidence')
assert not git('status','--porcelain');head=git('rev-parse','HEAD');changed=git('diff','--name-only',release,head).splitlines();assert all(n in files or n.startswith('docs/publication120/') for n in changed)
run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition120')
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition120-final-main','refs/heads/edition120:refs/verify/edition120-final-branch','refs/tags/readme-120:refs/verify/edition120-final-tag')
assert head==git('rev-parse','refs/verify/edition120-final-main')==git('rev-parse','refs/verify/edition120-final-branch');assert receipt['tagObject']==git('rev-parse','refs/verify/edition120-final-tag');assert release==git('rev-parse','refs/verify/edition120-final-tag^{commit}')
for name in changed:assert subprocess.check_output(['git','show',head+':'+name])==subprocess.check_output(['git','show','refs/verify/edition120-final-main:'+name]),name
assert not git('status','--porcelain');result={'passed':True,'finalMain':head,'release':release,'tag':'readme-120','metadataFilesCompared':len(changed),'scope':'Changed metadata compared byte-for-byte with freshly fetched main; production and frozen evidence unchanged from complete fetched release audit.'};(r/'build/final-publication120.json').write_bytes((json.dumps(result,indent=2)+'\n').encode());print(json.dumps(result))
