from pathlib import Path
import json,subprocess,shutil
r=Path.cwd()
git=lambda *a:subprocess.check_output(['git',*a],text=True).strip()
run=lambda *a:subprocess.run(a,check=True)
audit=json.loads((r/'build/publication-audit116.json').read_text(encoding='utf8'))
assert audit['passed'] and audit['remoteFetched']
release=git('rev-parse','readme-116^{commit}');assert release==audit['commit']==git('rev-parse','HEAD')
ui=json.loads((r/'docs/readmes/116/actual-ui.json').read_text(encoding='utf8'))
validation=json.loads((r/'docs/readmes/116/verification.json').read_text(encoding='utf8'))
receipt={'edition':116,'releaseCommit':release,'annotatedTag':'readme-116','tagObject':git('rev-parse','readme-116'),'audit':audit,'validation':{'javaMethods':len(validation['javaChecks']),'desktopChecks':validation['desktopChecks'],'actualRequests':[{'request':x['prompt'],'status':x['task']['status'],'elapsedMs':x['elapsedMs'],'trialOutcomes':[t['status'] for t in x['child']['trials']],'audit':x['child'].get('audit')} for x in ui['requests']],'actualAbruptRecovery':True,'referenceChecks':validation['referenceCases'],'controllerCases':validation['controllerCases'],'referenceControllerResults':validation['referenceControllerResults'],'modelCalls':validation['modelCalls'],'guideWidths':[1440,390],'scope':validation['scope']},'ci':{'scope':'Local verification and fetched publication do not establish remote CI success. The prior115 failure was reproduced and corrected on the same software-renderer package.'}}
ci=r/'build/release116-ci.json'
if ci.exists():receipt['ci']['releaseCheck']=json.loads(ci.read_text(encoding='utf8'))
(r/'docs/PUBLICATION_116.json').write_bytes((json.dumps(receipt,indent=2)+'\n').encode())
status=f'''## Edition116 — published and fetched back

2026-11-11, America/Los_Angeles. Release {release}; annotated readme-116. Main, edition116 and tag pushed atomically and fetched. Verified {audit['canonicalSourceFiles']} canonical sources, {audit['frozenHashes']} frozen hashes across116 editions and {audit['original116EvidenceFiles']} original evidence entries. CI first:115 had one failing native reference test. Reproduced invalid GLSL330 dynamic sampler indexing with the pinned software renderer; literal sampler branches and explicit shader exceptions fix it without changing image matching. Shader bytes bind current engine identity. Input-only landing prediction and committed edge crossings finish12/12 hardware layouts versus5/12 original and12/12 simple; revised pickups2 versus7 original. The reserved six layouts repeat on software rendering. Six fresh references and four required refusals pass on each renderer. Their wider seed15 five-tile gap remains an AI counterexample: simple finishes6/6 reconstructed courses, revised5/6; keep this distinct from the narrower12-layout comparison. {len(validation['javaChecks'])} distinct Java methods,165 desktop checks, four actual UI requests, abrupt recovery and desktop/mobile guide pass. Original CI failure and unsuccessful controller prototype retained. Zero model calls, no general recognition/training claim, unchanged procedure limits. Next: wider-gap contact recovery and safe power-up pursuit without losing finishes, broader declared image components and explicit procedure lifecycle. Preserve private runtimes33224/37676, original projects and frozen001–116. No delegation; heavy checks serial. See docs/PUBLICATION_116.json. New remote CI is reported separately.

'''
for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
 p=r/name;p.write_bytes((status+p.read_text(encoding='utf-8-sig')).encode('utf8'))
p=r/'README.md';s=p.read_text(encoding='utf8');first,rest=s.split('\n',1)
link='[Published and fetched verification receipt](docs/PUBLICATION_116.json) · [Release tag](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/tree/readme-116)'
p.write_bytes((first+'\n\n'+link+'\n'+rest).encode('utf8'))
for name in ['push116.log','publication-audit116.json']:shutil.copy2(r/'build'/name,r/'docs/publication116'/name)
files=['AGENTS.md','README-CONTRIBUTORS.md','README.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md','docs/PUBLICATION_116.json','docs/publication116']
run('git','add',*files);run('git','add','-f','docs/publication116/push116.log');run('git','diff','--cached','--check','--','.',':!docs/publication116/*.log')
run('git','-c','gc.auto=0','commit','-m','Record fetched edition 116 source and original-evidence verification')
assert not git('status','--porcelain')
head=git('rev-parse','HEAD');changed=git('diff','--name-only',release,head).splitlines()
assert all(n in files or n.startswith('docs/publication116/') for n in changed)
run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition116')
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition116-final-main','refs/heads/edition116:refs/verify/edition116-final-branch','refs/tags/readme-116:refs/verify/edition116-final-tag')
assert head==git('rev-parse','refs/verify/edition116-final-main')==git('rev-parse','refs/verify/edition116-final-branch')
assert receipt['tagObject']==git('rev-parse','refs/verify/edition116-final-tag')
assert release==git('rev-parse','refs/verify/edition116-final-tag^{commit}')
for name in changed:assert subprocess.check_output(['git','show',head+':'+name])==subprocess.check_output(['git','show','refs/verify/edition116-final-main:'+name]),name
assert not git('status','--porcelain')
result={'passed':True,'finalMain':head,'release':release,'tag':'readme-116','metadataFilesCompared':len(changed),'scope':'Every changed metadata blob compared with freshly fetched main; production sources and frozen evidence unchanged from the complete fetched release audit.'}
(r/'build/final-publication116.json').write_bytes((json.dumps(result,indent=2)+'\n').encode());print(json.dumps(result))
