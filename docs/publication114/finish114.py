from pathlib import Path
import json,subprocess,shutil
r=Path.cwd();git=lambda *a:subprocess.check_output(['git',*a],text=True).strip();run=lambda *a:subprocess.run(a,check=True)
audit=json.loads((r/'build/publication-audit114.json').read_text(encoding='utf8'));assert audit['passed'] and audit['remoteFetched'];release=git('rev-parse','readme-114^{commit}');assert release==audit['commit']==git('rev-parse','HEAD')
receipt={'edition':114,'releaseCommit':release,'annotatedTag':'readme-114','tagObject':git('rev-parse','readme-114'),'audit':audit,'planPublication':json.loads((r/'build/plan114-publication.json').read_text(encoding='utf8')),'validation':{'javaMethods':38,'desktopChecks':165,'actualRuntimePassed':True,'actualDesktopPassed':True,'actualDesktopSeconds':13.4515428,'standaloneRuntimeSeconds':5.0586359,'finishTick':384,'candidateObjects':114,'modelCalls':0,'guideWidths':[1440,390],'scope':'One declared generated course completed and reopened; construction varied across three seeds. No held-out playability, screenshot reconstruction, chat-generation or training claim.'},'ci':{'scope':'Local checks are distinct from remote CI. The saved parent CI was still compiling/testing when checked; no remote CI pass is claimed.'}}
ci=r/'build/release114-ci.json'
if ci.exists():receipt['ci']['releaseCheck']=json.loads(ci.read_text(encoding='utf8'))
(r/'docs/PUBLICATION_114.json').write_text(json.dumps(receipt,indent=2)+'\n',encoding='utf8')
text=f'''## Edition114 — published and fetched back

2026-11-10. Release {release}; annotated readme-114. Main, edition114 and tag pushed atomically and fetched. Verified {audit['canonicalSourceFiles']} canonical source files, {audit['frozenHashes']} frozen hashes across114 editions and {audit['original114EvidenceFiles']} original114 evidence entries. New Java source-based constructor saves separate candidates and opens them in the existing embedded Game Lab. Actual114-object course completed via input controls at tick384, then reopened with stable player identity and fresh initial state.38 Java methods,165 desktop checks, actual runtime/UI and desktop/mobile guide pass. Source/asset/candidate corruption and changed-reset refusals retained. Initial startup encoding refusal and intermediate layouts are preserved. No new LLM inference/training or generic image-to-level claim. Next115: durable chat construction/playtest/repair skill and explicit procedure lifecycle before retaining new skills into the unchanged32-slot bank. Preserve private projects and frozen001–114; no delegation; heavy checks serial. See docs/PUBLICATION_114.json and the full phased plan. Remote CI remains separately reported.

'''
for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
 p=r/name;p.write_text(text+p.read_text(encoding='utf-8-sig'),encoding='utf8')
p=r/'README.md';s=p.read_text(encoding='utf8');s=s.replace('# Fraynix Agent Workspace — edition 114','# Fraynix Agent Workspace — edition 114\n\n[Published and fetched verification receipt](docs/PUBLICATION_114.json) · [Release tag](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/tree/readme-114)',1);p.write_text(s,encoding='utf8')
shutil.copy2(r/'build/finish114.py',r/'docs/publication114/finish114.py')
for name in ['release114.py','release114-first.py','release114-first.log','push114.log','publication-audit114.json']:
 shutil.copy2(r/'build'/name,r/'docs/publication114'/name)
files=['AGENTS.md','README-CONTRIBUTORS.md','README.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md','docs/PUBLICATION_114.json','docs/readmes/INDEX.md','docs/publication114']
run('git','add',*files);run('git','add','-f','docs/publication114/push114.log','docs/publication114/release114-first.log');run('git','diff','--cached','--check','--','.',':!docs/publication114/*.log');run('git','-c','gc.auto=0','commit','-m','Record fetched edition 114 publication and full source/evidence verification')
assert not git('status','--porcelain');head=git('rev-parse','HEAD');changed=git('diff','--name-only',release,head).splitlines();assert all(n in files or n.startswith('docs/publication114/') for n in changed)
run('git','-c','pack.threads=4','push','--atomic','origin','HEAD:refs/heads/main','HEAD:refs/heads/edition114')
run('git','-c','gc.auto=0','fetch','origin','refs/heads/main:refs/verify/edition114-final-main','refs/heads/edition114:refs/verify/edition114-final-branch','refs/tags/readme-114:refs/verify/edition114-final-tag')
assert head==git('rev-parse','refs/verify/edition114-final-main')==git('rev-parse','refs/verify/edition114-final-branch')
assert receipt['tagObject']==git('rev-parse','refs/verify/edition114-final-tag');assert release==git('rev-parse','refs/verify/edition114-final-tag^{commit}')
for name in changed:assert subprocess.check_output(['git','show',head+':'+name])==subprocess.check_output(['git','show','refs/verify/edition114-final-main:'+name]),name
assert not git('status','--porcelain')
result={'passed':True,'finalMain':head,'release':release,'tag':'readme-114','metadataFilesCompared':len(changed),'scope':'All changed metadata compared byte-for-byte from freshly fetched main; production sources and frozen evidence unchanged from the complete fetched release audit.'};(r/'build/final-publication114.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf8');print(json.dumps(result))
