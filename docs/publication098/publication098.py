from pathlib import Path
import subprocess,json,shutil
r=Path.cwd()
def git(*a):return subprocess.check_output(['git',*a],text=True).strip()
audit=json.loads((r/'build/publication-audit098.json').read_text(encoding='utf8'));assert audit['passed'] and audit['remoteFetched']
release=git('rev-parse','readme-098^{commit}');assert release==audit['commit']==git('rev-parse','HEAD');tag=git('rev-parse','readme-098')
analysis=json.loads((r/'build/analysis098.json').read_text(encoding='utf8'))
data={'edition':98,'releaseCommit':release,'annotatedTag':'readme-098','tagObject':tag,'audit':audit,'scope':'Actual non-force atomic main/edition098/tag push, fetched and audited from Git blobs. Local native and original checks passed. Existing calibration gate genuinely declined; no fit forced. Original harness failure retained and whole original preparation time unavailable. No amortized payoff claim. Post-push CI is separate.','comparisons':analysis['comparisons'],'releaseCI':json.loads((r/'build/ci098-release.json').read_text(encoding='utf8'))}
(r/'docs/PUBLICATION_098.json').write_text(json.dumps(data,indent=2)+'\n',encoding='utf8')
folder=r/'docs/publication098';folder.mkdir(exist_ok=True)
for name in ['ci098-release.json','publication-audit098.json','push098.log','release098.py','audit-publication098.py','publication098.py','freeze098.py','document098.py','check-guide098.cjs','visual098.json','visual-review098.json','documentation-check.json','doc-check098.log','guide098.log','guide098.html','guide098-1440.png','guide098-390.png','ci098-status.json','ci098-parent.log','regression098-summary.json','analysis098.json','source-comparison098.json','detail098.json','capture-detail098.cjs','capture-detail098.log']:
 shutil.copyfile(r/'build'/name,folder/name)
notice=f"Published release {release}; annotated readme-098 tag object {tag}. Actual main/edition098/tag push fetched and verified: {audit['canonicalSourceFiles']} canonical sources, {audit['frozenHashes']} frozen hashes across98 editions and {audit['original098EvidenceFiles']} logical original archive entries. "
for name,link in [('AGENTS.md','docs/PUBLICATION_098.json'),('docs/PROJECT_MEMORY.md','PUBLICATION_098.json'),('docs/DEVELOPMENT_LOG.md','PUBLICATION_098.json'),('docs/ARCHITECTURE.md','PUBLICATION_098.json'),('README-CONTRIBUTORS.md','docs/PUBLICATION_098.json')]:
 p=r/name;s=p.read_text(encoding='utf8');assert '## Edition098 — locally verified; publication pending' in s;s=s.replace('## Edition098 — locally verified; publication pending','## Edition098 — published and fetched back\n\n'+notice+'[Publication receipt]('+link+').',1).replace('Main/tag publication and fetched receipt still pending.','Main/tag publication and fetched-content audit completed. Post-push CI is separate.',1);p.write_text(s,encoding='utf8',newline='\n')
p=r/'README.md';p.write_text(p.read_text(encoding='utf8')+'\nPublished to main with original evidence and screenshots. [Fetched GitHub verification](docs/PUBLICATION_098.json).\n',encoding='utf8',newline='\n')
p=r/'docs/obsidian/106-benefit-selection.md';p.write_text(p.read_text(encoding='utf8')+'\n'+notice+'[[docs/PUBLICATION_098.json|Publication receipt]]\n',encoding='utf8',newline='\n')
print(notice)
