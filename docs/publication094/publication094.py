from pathlib import Path
import subprocess,json,shutil
r=Path.cwd()
def git(*args):return subprocess.check_output(['git',*args],text=True).strip()
audit=json.loads((r/'build/publication-audit094.json').read_text());assert audit['passed'] and audit['remoteFetched']
release=git('rev-parse','readme-094^{commit}');assert release==audit['commit']==git('rev-parse','HEAD')
tag=git('rev-parse','readme-094');data={'edition':94,'releaseCommit':release,'annotatedTag':'readme-094','tagObject':tag,'audit':audit,'scope':'Actual non-force atomic main/edition094/tag push, followed by fetch and complete Git-tree verification. This receipt follows the frozen release without amending the tag.'}
(r/'docs/PUBLICATION_094.json').write_text(json.dumps(data,indent=2)+'\n',encoding='utf8')
folder=r/'docs/publication094';folder.mkdir(exist_ok=True)
for name in ['publication-audit094.json','push094.log','release094.py','audit-publication094.py','publication094.py','freeze094.py','document094.py','check-guide094.cjs','visual094.json','visual-review094.json','documentation-check.json','archive-units094.py','regress094.ps1','doc-check094-final.log','guide-check094-final.log','source-comparison094.json','github094-metadata.json']:
 shutil.copyfile(r/'build'/name,folder/name)
notice=f"Published release {release}; annotated readme-094 tag object {tag}. Actual main/edition094/tag push fetched and verified: {audit['canonicalSourceFiles']} canonical sources, {audit['frozenHashes']} frozen hashes across 94 editions and {audit['original094EvidenceFiles']} logical original archive entries. "
for name,link in [('AGENTS.md','docs/PUBLICATION_094.json'),('docs/PROJECT_MEMORY.md','PUBLICATION_094.json'),('docs/DEVELOPMENT_LOG.md','PUBLICATION_094.json')]:
 p=r/name;s=p.read_text(encoding='utf8');s=s.replace('## Edition094 — locally verified; publication pending','## Edition094 — published and fetched back\n\n'+notice+'[Publication receipt]('+link+').',1);s=s.replace('Edition094 is in progress in its isolated D: checkout; its prospective contract and original evidence govern actual results.','Edition094 is published and fetched back from its isolated D: checkout; the release, contract and original evidence record actual results.');p.write_text(s,encoding='utf8',newline='\n')
p=r/'README.md';s=p.read_text(encoding='utf8');needle='**Latest: [edition094 — three-stage crash recovery](README-094.md).**';s=s.replace(needle,needle+'\n\nPublished to main with original evidence and screenshots. [Fetched GitHub verification](docs/PUBLICATION_094.json).',1);p.write_text(s,encoding='utf8',newline='\n')
p=r/'docs/obsidian/102-three-stage-recovery.md';p.write_text(p.read_text(encoding='utf8')+'\n'+notice+'[[docs/PUBLICATION_094.json|Publication receipt]]\n',encoding='utf8',newline='\n')
print(notice)
