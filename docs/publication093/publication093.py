from pathlib import Path
import subprocess,json,shutil
r=Path.cwd()
def git(*args):return subprocess.check_output(['git',*args],text=True).strip()
audit=json.loads((r/'build/publication-audit093.json').read_text());assert audit['passed'] and audit['remoteFetched']
release=git('rev-parse','readme-093^{commit}');assert release==audit['commit']==git('rev-parse','HEAD')
tag=git('rev-parse','readme-093');data={'edition':93,'releaseCommit':release,'annotatedTag':'readme-093','tagObject':tag,'audit':audit,'scope':'Actual non-force atomic main/edition093/tag push, followed by fetch and complete Git-tree verification. This receipt follows the frozen release without amending the tag.'}
(r/'docs/PUBLICATION_093.json').write_text(json.dumps(data,indent=2)+'\n',encoding='utf8')
folder=r/'docs/publication093';folder.mkdir(exist_ok=True)
for name in ['publication-audit093.json','push093.log','release093.py','audit-publication093.py','publication093.py','freeze093.py','document093.py','check-guide093.cjs','visual093.json','visual-review093.json','documentation-check.json','archive-units093.py','regress093.ps1','prepare-publication093.py','prepare-inspector093.py','prepare-guide093.py','doc-check093-final.log','guide-check093-final.log','source-comparison093.json']:
 shutil.copyfile(r/'build'/name,folder/name)
notice=f"Published release {release}; annotated readme-093 tag object {tag}. Actual main/edition093/tag push fetched and verified: {audit['canonicalSourceFiles']} canonical sources, {audit['frozenHashes']} frozen hashes across 93 editions and {audit['original093EvidenceFiles']} logical original archive entries. "
for name,link in [('AGENTS.md','docs/PUBLICATION_093.json'),('docs/PROJECT_MEMORY.md','PUBLICATION_093.json'),('docs/DEVELOPMENT_LOG.md','PUBLICATION_093.json')]:
 p=r/name;s=p.read_text(encoding='utf8');s=s.replace('## Edition093 — locally verified; publication pending','## Edition093 — published and fetched back\n\n'+notice+'[Publication receipt]('+link+').',1);p.write_text(s,encoding='utf8',newline='\n')
p=r/'README.md';s=p.read_text(encoding='utf8');needle='**Latest: [edition093 — forecast from recovered state](README-093.md).**';s=s.replace(needle,needle+'\n\nPublished to main with original evidence and screenshots. [Fetched GitHub verification](docs/PUBLICATION_093.json).',1);p.write_text(s,encoding='utf8',newline='\n')
p=r/'docs/obsidian/101-recovered-state-forecast.md';p.write_text(p.read_text(encoding='utf8')+'\n'+notice+'[[docs/PUBLICATION_093.json|Publication receipt]]\n',encoding='utf8',newline='\n')
print(notice)
