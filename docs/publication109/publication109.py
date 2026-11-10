from pathlib import Path
import subprocess,json,shutil
r=Path.cwd()
def git(*a):return subprocess.check_output(['git',*a],text=True).strip()
audit=json.loads((r/'build/publication-audit109.json').read_text(encoding='utf-8'));assert audit['passed'] and audit['remoteFetched']
release=git('rev-parse','readme-109^{commit}');assert release==audit['commit']==git('rev-parse','HEAD');tag=git('rev-parse','readme-109')
analysis=json.loads((r/'build/analysis109.json').read_text(encoding='utf-8'));assert analysis['passed']
data={'edition':109,'releaseCommit':release,'annotatedTag':'readme-109','tagObject':tag,'audit':audit,'analysis':analysis,'releaseCI':json.loads((r/'build/ci109-release.json').read_text(encoding='utf-8')),'scope':'Actual non-force atomic main/edition109/tag push, fetched and audited. Genuine bank growth, same-bank cold maintenance pairs, unchanged verification, source refusals and abrupt near-capacity recovery. No padding, history deletion or raised limits. All342 Java engine classes unchanged. CI separate.'}
(r/'docs/PUBLICATION_109.json').write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8',newline='\n')
folder=r/'docs/publication109';folder.mkdir(exist_ok=True)
names=['record-driver109.py','java109-final.log','test-driver109.json','test-driver109.diff','repair-driver109.py','ci109-release.json','publication-audit109.json','push109.log','release109.log','release109.py','audit-publication109.py','publication109.py','finish-publication109.py','freeze109.py','check-guide109.cjs','visual109.json','visual-review109.json','documentation-check.json','doc-check109.log','guide109.log','guide109-1440.png','guide109-390.png','ci109-parent.json','analysis109.json','java109.log','maintenance109.log','inspect109.log','desktop109.log','desktop109-additional.log','source-identity109.json']
for name in names:
 assert (r/'build'/name).is_file(),name
 shutil.copyfile(r/'build'/name,folder/name)
notice=f"Published release {release}; annotated readme-109 tag object {tag}. Actual main/edition109/tag push fetched and verified: {audit['canonicalSourceFiles']} canonical sources, {audit['frozenHashes']} frozen hashes across109 editions and {audit['original109EvidenceFiles']} logical original archive entries. "
validation='All342 production engine classes and both report resources unchanged.126 distinct Java methods covered;23 affected methods pass after an initial test-driver timing failure, retained with the test-only fix.148 desktop checks and30 altered-record refusals pass. Actual36-request study:30 completions, six required refusals and one abrupt recovery. Genuine15-procedure bank reaches20 executable workflows before32 procedure slots. Two reversed-order cold recheck/refresh pairs at each bank1/8/9/14 retain all preparation, copy, request, restart and refusal costs. Source/outcome checks and limits preserved. Actual images reviewed at1600/1100 and guide at1440/390. Preserve private worlds and frozen editions; no delegation. '
for name,link in [('AGENTS.md','docs/PUBLICATION_109.json'),('README-CONTRIBUTORS.md','docs/PUBLICATION_109.json'),('docs/PROJECT_MEMORY.md','PUBLICATION_109.json'),('docs/DEVELOPMENT_LOG.md','PUBLICATION_109.json'),('docs/ARCHITECTURE.md','PUBLICATION_109.json')]:
 p=r/name;s=p.read_text(encoding='utf-8');assert '## Edition109 - validated maintenance at capacity' in s
 s=s.replace('## Edition109 - validated maintenance at capacity','## Edition109 - published and fetched back\n\n'+notice+validation+'[Publication receipt]('+link+').',1).replace('Publication pending actual push/fetch; CI separate.','Main/tag publication and fetched verification completed; remote CI status is recorded separately in the receipt.',1);p.write_text(s,encoding='utf-8',newline='\n')
p=r/'README.md';p.write_text(p.read_text(encoding='utf-8')+'\nPublished to main. [Fetched GitHub verification](docs/PUBLICATION_109.json).\n',encoding='utf-8',newline='\n')
p=r/'docs/obsidian/117-maintenance-at-capacity.md';p.write_text(p.read_text(encoding='utf-8').replace('Publication pending actual push/fetch; CI separate.','Main/tag publication and fetched verification completed; remote CI separate.')+'\n'+notice+validation+'[[docs/PUBLICATION_109.json|Publication receipt]]\n',encoding='utf-8',newline='\n')
print(notice)
