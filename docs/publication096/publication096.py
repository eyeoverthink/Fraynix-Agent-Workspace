from pathlib import Path
import subprocess,json,shutil
r=Path.cwd()
def git(*args):return subprocess.check_output(['git',*args],text=True).strip()
audit=json.loads((r/'build/publication-audit096.json').read_text(encoding='utf8'));assert audit['passed'] and audit['remoteFetched']
release=git('rev-parse','readme-096^{commit}');assert release==audit['commit']==git('rev-parse','HEAD')
tag=git('rev-parse','readme-096');data={'edition':96,'releaseCommit':release,'annotatedTag':'readme-096','tagObject':tag,'audit':audit,'scope':'Actual non-force atomic main/edition096/tag push, then fetch and complete Git-tree verification. Receipt follows the frozen release without amending its tag. Source-qualified combined local method coverage does not predeclare the remote CI outcome; the original full invocation failed four subsequently corrected test-boundary assertions.'}
(r/'docs/PUBLICATION_096.json').write_text(json.dumps(data,indent=2)+'\n',encoding='utf8')
folder=r/'docs/publication096';folder.mkdir(exist_ok=True)
for name in ['publication-audit096.json','push096.log','release096.py','audit-publication096.py','publication096.py','freeze096.py','document096.py','check-guide096.cjs','visual096.json','visual-review096.json','documentation-check.json','full096.ps1','full096-run.json','full096-finished.json','coverage096.json','corrected096-finished.json','packaging096-finished.json','payload-boundary096-correction.json','zip64096.json','zip096.json','final-zip096.json','final-package096.log','prior-distribution096.json','ci096-release.json','release096.log','ui096-attempts.json','ui096-audit.json','doc-check096-final.log','guide-check096-final.log','source-comparison096.json','heap096-summary512.json','heap096-summary1024.json','README096-template.md','guide096.html','check-zip096.py','coverage096.py','audit-ui096.py','guard-correction096.py','fix-payload096.py','corrected096.ps1','packaging096.ps1']:
 shutil.copyfile(r/'build'/name,folder/name)
notice=f"Published release {release}; annotated readme-096 tag object {tag}. Actual main/edition096/tag push fetched and verified: {audit['canonicalSourceFiles']} canonical sources, {audit['frozenHashes']} frozen hashes across96 editions and {audit['original096EvidenceFiles']} logical original archive entries. "
for name,link in [('AGENTS.md','docs/PUBLICATION_096.json'),('docs/PROJECT_MEMORY.md','PUBLICATION_096.json'),('docs/DEVELOPMENT_LOG.md','PUBLICATION_096.json')]:
 p=r/name;s=p.read_text(encoding='utf8').replace('## Edition096 — locally verified; publication pending','## Edition096 — published and fetched back\n\n'+notice+'Inspect the new remote CI result before starting the warm-workspace study. [Publication receipt]('+link+').',1);p.write_text(s,encoding='utf8',newline='\n')
p=r/'README.md';s=p.read_text(encoding='utf8');needle='**Latest: [edition096 — restoring the full verification path](README-096.md).**';assert needle in s;s=s.replace(needle,needle+'\n\nPublished to main with original evidence and screenshots. [Fetched GitHub verification](docs/PUBLICATION_096.json).',1);p.write_text(s,encoding='utf8',newline='\n')
p=r/'docs/obsidian/104-full-verification.md';p.write_text(p.read_text(encoding='utf8')+'\n'+notice+'[[docs/PUBLICATION_096.json|Publication receipt]]\n',encoding='utf8',newline='\n')
print(notice)
