from pathlib import Path
import subprocess,json,shutil
r=Path.cwd()
def git(*args):return subprocess.check_output(['git',*args],text=True).strip()
audit=json.loads((r/'build/publication-audit095.json').read_text());assert audit['passed'] and audit['remoteFetched']
release=git('rev-parse','readme-095^{commit}');assert release==audit['commit']==git('rev-parse','HEAD')
tag=git('rev-parse','readme-095');data={'edition':95,'releaseCommit':release,'annotatedTag':'readme-095','tagObject':tag,'audit':audit,'scope':'Actual non-force atomic main/edition095/tag push, followed by fetch and complete Git-tree verification. This receipt follows the frozen release without amending the tag.'}
(r/'docs/PUBLICATION_095.json').write_text(json.dumps(data,indent=2)+'\n',encoding='utf8')
folder=r/'docs/publication095';folder.mkdir(exist_ok=True)
for name in ['publication-audit095.json','push095.log','release095.py','audit-publication095.py','publication095.py','freeze095.py','document095.py','check-guide095.cjs','visual095.json','visual-review095.json','documentation-check.json','archive-units095.py','regress095.ps1','doc-check095-final.log','guide-check095-final.log','source-comparison095.json','mutations095.json','cost-tests095.log']:
 shutil.copyfile(r/'build'/name,folder/name)
notice=f"Published release {release}; annotated readme-095 tag object {tag}. Actual main/edition095/tag push fetched and verified: {audit['canonicalSourceFiles']} canonical sources, {audit['frozenHashes']} frozen hashes across 95 editions and {audit['original095EvidenceFiles']} logical original archive entries. "
for name,link in [('AGENTS.md','docs/PUBLICATION_095.json'),('docs/PROJECT_MEMORY.md','PUBLICATION_095.json'),('docs/DEVELOPMENT_LOG.md','PUBLICATION_095.json')]:
 p=r/name;s=p.read_text(encoding='utf8');s=s.replace('## Edition095 — locally verified; publication pending','## Edition095 — published and fetched back\n\n'+notice+'[Publication receipt]('+link+').',1);p.write_text(s,encoding='utf8',newline='\n')
p=r/'README.md';s=p.read_text(encoding='utf8');needle='**Latest: [edition095 — complete mixed-task costs](README-095.md).**';s=s.replace(needle,needle+'\n\nPublished to main with original evidence and screenshots. [Fetched GitHub verification](docs/PUBLICATION_095.json).',1);p.write_text(s,encoding='utf8',newline='\n')
p=r/'docs/obsidian/103-mixed-task-costs.md';p.write_text(p.read_text(encoding='utf8')+'\n'+notice+'[[docs/PUBLICATION_095.json|Publication receipt]]\n',encoding='utf8',newline='\n')
print(notice)
