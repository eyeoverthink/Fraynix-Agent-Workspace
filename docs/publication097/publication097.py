from pathlib import Path
import subprocess,json,shutil
r=Path.cwd()
def git(*a):return subprocess.check_output(['git',*a],text=True).strip()
audit=json.loads((r/'build/publication-audit097.json').read_text(encoding='utf8'));assert audit['passed'] and audit['remoteFetched']
release=git('rev-parse','readme-097^{commit}');assert release==audit['commit']==git('rev-parse','HEAD')
tag=git('rev-parse','readme-097');data={'edition':97,'releaseCommit':release,'annotatedTag':'readme-097','tagObject':tag,'audit':audit,'scope':'Actual non-force atomic main/edition097/tag push, followed by fresh fetch and complete Git-tree audit. The frozen release is not amended by this publication receipt. Parent096 full CI passed before publishing. New-head CI is separate. Native study passed all24 main goals,8 abrupt recoveries and4 saved-skill executions. Memory took200.53seconds longer across full streams;291.14seconds after preparation.'}
(r/'docs/PUBLICATION_097.json').write_text(json.dumps(data,indent=2)+'\n',encoding='utf8')
folder=r/'docs/publication097';folder.mkdir(exist_ok=True)
for name in ['publication-audit097.json','push097.log','release097.py','audit-publication097.py','publication097.py','freeze097.py','document097.py','check-guide097.cjs','visual097.json','visual-review097.json','documentation-check.json','doc-check097-final.log','guide097-final.log','source-comparison097.json','ci097-final.json','ci097-release.json','ci097-final-jobs.json','ci097-first-check.json','ci097-gate.json','detail097.json','capture-detail097.cjs','capture-detail097.log','capture-detail097-attempt1.cjs','capture-detail097-attempt1.log','guide097.html','guide097-preview-attempt1.log','guide097-1440.png','guide097-390.png']:
 shutil.copyfile(r/'build'/name,folder/name)
notice=f"Published release {release}; annotated readme-097 tag object {tag}. Actual main/edition097/tag push fetched and verified: {audit['canonicalSourceFiles']} canonical sources, {audit['frozenHashes']} frozen hashes across97 editions and {audit['original097EvidenceFiles']} logical original archive entries. "
for name,link in [('AGENTS.md','docs/PUBLICATION_097.json'),('docs/PROJECT_MEMORY.md','PUBLICATION_097.json'),('docs/DEVELOPMENT_LOG.md','PUBLICATION_097.json'),('docs/ARCHITECTURE.md','PUBLICATION_097.json'),('README-CONTRIBUTORS.md','docs/PUBLICATION_097.json')]:
 p=r/name;s=p.read_text(encoding='utf8');assert '## Edition097 — locally measured, publication pending' in s;s=s.replace('## Edition097 — locally measured, publication pending','## Edition097 — published and fetched back\n\n'+notice+'[Publication receipt]('+link+').',1).replace('Actual main publication and fetched receipt still required.','Actual main publication and fetched-content audit completed. New-head CI is separate.',1);p.write_text(s,encoding='utf8',newline='\n')
p=r/'README.md';p.write_text(p.read_text(encoding='utf8')+'\nPublished to main with original evidence and screenshots. [Fetched GitHub verification](docs/PUBLICATION_097.json).\n',encoding='utf8',newline='\n')
p=r/'docs/obsidian/105-continuing-costs.md';p.write_text(p.read_text(encoding='utf8')+'\n'+notice+'[[docs/PUBLICATION_097.json|Publication receipt]]\n',encoding='utf8',newline='\n')
print(notice)
