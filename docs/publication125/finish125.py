from pathlib import Path
import json,subprocess,shutil
from notes125 import status
r=Path.cwd();git=lambda *a:subprocess.check_output(['git',*a],text=True).strip();run=lambda *a:subprocess.run(a,check=True)
audit=json.loads((r/'build/publication-audit125.json').read_text(encoding='utf8'));assert audit['passed'] and audit['remoteFetched']
release=git('rev-parse','readme-125^{commit}');assert release==audit['commit']==git('rev-parse','HEAD')
ui=json.loads((r/'docs/readmes/125/actual-ui.json').read_text(encoding='utf8'));v=json.loads((r/'docs/readmes/125/verification.json').read_text(encoding='utf8'))
receipt={'edition':125,'releaseCommit':release,'annotatedTag':'readme-125','tagObject':git('rev-parse','readme-125'),'audit':audit,'validation':{'javaMethods':len(v['javaChecks']),'desktopChecks':v['desktopChecks'],'launcher':v['launcher'],'actualRequests':[{'request':x['prompt'],'status':x['task']['status'],'elapsedMs':x['elapsedMs'],'trialOutcomes':[t['status'] for t in x['child']['trials']],'audit':x['child'].get('audit')} for x in ui['requests']],'actualAbruptRecovery':True,'studyRows':v['studyRows'],'regressionExecutions':v['regressionExecutions'],'measurements':v['measurements'],'referenceControllerResults':v['referenceControllerResults'],'modelCalls':v['modelCalls'],'guideWidths':[1440,390],'scope':v['scope']},'ci':{'scope':'CI checked before implementation: edition124 and123 main/release runs were pending. Local verification and fetched publication do not establish edition125 hosted CI success.'}}
ci=r/'build/release125-ci.json'
if ci.exists():receipt['ci']['releaseCheck']=json.loads(ci.read_text(encoding='utf8'))
(r/'docs/PUBLICATION_125.json').write_bytes((json.dumps(receipt,indent=2)+'\n').encode())
s=status('published and fetched back',f"Release {release}; annotated readme-125. Main, edition125 and tag pushed atomically and fetched. Verified {audit['canonicalSourceFiles']} canonical sources, {audit['frozenHashes']} frozen hashes across125 editions and {audit['original125EvidenceFiles']} original evidence entries. See docs/PUBLICATION_125.json.")
for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
 p=r/name;p.write_bytes((s+p.read_text(encoding='utf-8-sig')).encode())
p=r/'README.md';first,rest=p.read_text(encoding='utf8').split('\n',1);link='[Published and fetched verification receipt](docs/PUBLICATION_125.json) · [Release tag](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/tree/readme-125)';p.write_bytes((first+'\n\n'+link+'\n'+rest).encode())
for name in ['push125.log','publication-audit125.json']:shutil.copy2(r/'build'/name,r/'docs/publication125'/name)
files=['AGENTS.md','README-CONTRIBUTORS.md','README.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md','docs/PUBLICATION_125.json','docs/publication125']
run('git','add',*files);run('git','add','-f','docs/publication125/push125.log');run('git','diff','--cached','--check','--','.',':!docs/publication125/*.log');run('git','-c','gc.auto=0','commit','-m','Record fetched edition 125 decision reviews, sources and original evidence')
assert not git('status','--porcelain');head=git('rev-parse','HEAD');changed=git('diff','--name-only',release,head).splitlines();assert all(n in files or n.startswith('docs/publication125/') for n in changed)
run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition125')
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition125-final-main','refs/heads/edition125:refs/verify/edition125-final-branch','refs/tags/readme-125:refs/verify/edition125-final-tag')
assert head==git('rev-parse','refs/verify/edition125-final-main')==git('rev-parse','refs/verify/edition125-final-branch');assert receipt['tagObject']==git('rev-parse','refs/verify/edition125-final-tag');assert release==git('rev-parse','refs/verify/edition125-final-tag^{commit}')
for name in changed:assert subprocess.check_output(['git','show',head+':'+name])==subprocess.check_output(['git','show','refs/verify/edition125-final-main:'+name]),name
assert not git('status','--porcelain');result={'passed':True,'finalMain':head,'release':release,'tag':'readme-125','metadataFilesCompared':len(changed),'scope':'Changed metadata compared byte-for-byte with freshly fetched main; production and frozen evidence unchanged from complete fetched release audit.'};(r/'build/final-publication125.json').write_bytes((json.dumps(result,indent=2)+'\n').encode());print(json.dumps(result))
