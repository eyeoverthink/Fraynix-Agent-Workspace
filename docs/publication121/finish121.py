from pathlib import Path
import json,subprocess,shutil
from notes121 import status
r=Path.cwd();git=lambda *a:subprocess.check_output(['git',*a],text=True).strip();run=lambda *a:subprocess.run(a,check=True)
audit=json.loads((r/'build/publication-audit121.json').read_text(encoding='utf8'));assert audit['passed'] and audit['remoteFetched']
release=git('rev-parse','readme-121^{commit}');assert release==audit['commit']==git('rev-parse','HEAD')
ui=json.loads((r/'docs/readmes/121/actual-ui.json').read_text(encoding='utf8'));v=json.loads((r/'docs/readmes/121/verification.json').read_text(encoding='utf8'))
receipt={'edition':121,'releaseCommit':release,'annotatedTag':'readme-121','tagObject':git('rev-parse','readme-121'),'audit':audit,'validation':{'javaMethods':len(v['javaChecks']),'desktopChecks':v['desktopChecks'],'launcher':v['launcher'],'actualRequests':[{'request':x['prompt'],'status':x['task']['status'],'elapsedMs':x['elapsedMs'],'trialOutcomes':[t['status'] for t in x['child']['trials']],'audit':x['child'].get('audit')} for x in ui['requests']],'actualAbruptRecovery':True,'referenceChecks':v['referenceCases'],'controllerCases':v['controllerCases'],'referenceControllerResults':v['referenceControllerResults'],'modelCalls':v['modelCalls'],'guideWidths':[1440,390],'scope':v['scope']},'ci':{'scope':'CI checked before implementation. At the initial check, 119 main failed an interruption timing check;120 jobs were pending. Local verification and fetched publication do not establish remote CI success.'}}
ci=r/'build/release121-ci.json'
if ci.exists():receipt['ci']['releaseCheck']=json.loads(ci.read_text(encoding='utf8'))
(r/'docs/PUBLICATION_121.json').write_bytes((json.dumps(receipt,indent=2)+'\n').encode())
s=status('published and fetched back',f"Release {release}; annotated readme-121. Main, edition121 and tag pushed atomically and fetched. Verified {audit['canonicalSourceFiles']} canonical sources, {audit['frozenHashes']} frozen hashes across121 editions and {audit['original121EvidenceFiles']} original evidence entries. See docs/PUBLICATION_121.json.")
for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
 p=r/name;p.write_bytes((s+p.read_text(encoding='utf-8-sig')).encode())
p=r/'README.md';first,rest=p.read_text(encoding='utf8').split('\n',1);link='[Published and fetched verification receipt](docs/PUBLICATION_121.json) · [Release tag](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/tree/readme-121)';p.write_bytes((first+'\n\n'+link+'\n'+rest).encode())
for name in ['push121.log','publication-audit121.json']:shutil.copy2(r/'build'/name,r/'docs/publication121'/name)
files=['AGENTS.md','README-CONTRIBUTORS.md','README.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md','docs/PUBLICATION_121.json','docs/publication121']
run('git','add',*files);run('git','add','-f','docs/publication121/push121.log');run('git','diff','--cached','--check','--','.',':!docs/publication121/*.log');run('git','-c','gc.auto=0','commit','-m','Record fetched edition 121 sources, verified return value and original evidence')
assert not git('status','--porcelain');head=git('rev-parse','HEAD');changed=git('diff','--name-only',release,head).splitlines();assert all(n in files or n.startswith('docs/publication121/') for n in changed)
run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition121')
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition121-final-main','refs/heads/edition121:refs/verify/edition121-final-branch','refs/tags/readme-121:refs/verify/edition121-final-tag')
assert head==git('rev-parse','refs/verify/edition121-final-main')==git('rev-parse','refs/verify/edition121-final-branch');assert receipt['tagObject']==git('rev-parse','refs/verify/edition121-final-tag');assert release==git('rev-parse','refs/verify/edition121-final-tag^{commit}')
for name in changed:assert subprocess.check_output(['git','show',head+':'+name])==subprocess.check_output(['git','show','refs/verify/edition121-final-main:'+name]),name
assert not git('status','--porcelain');result={'passed':True,'finalMain':head,'release':release,'tag':'readme-121','metadataFilesCompared':len(changed),'scope':'Changed metadata compared byte-for-byte with freshly fetched main; production and frozen evidence unchanged from complete fetched release audit.'};(r/'build/final-publication121.json').write_bytes((json.dumps(result,indent=2)+'\n').encode());print(json.dumps(result))
