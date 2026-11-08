from pathlib import Path
import subprocess,json,shutil
r=Path.cwd()
def git(*args):return subprocess.check_output(['git',*args],text=True).strip()
audit=json.loads((r/'build/publication-audit091.json').read_text());assert audit['passed'] and audit['remoteFetched']
release=git('rev-parse','readme-091^{commit}');assert release==audit['commit']==git('rev-parse','HEAD')
tag=git('rev-parse','readme-091');data={'edition':91,'releaseCommit':release,'annotatedTag':'readme-091','tagObject':tag,'audit':audit,'scope':'Actual non-force atomic main/edition091/tag push, then fetch and complete Git-tree verification. This receipt follows the frozen release; it does not amend the release tag.'}
(r/'docs/PUBLICATION_091.json').write_text(json.dumps(data,indent=2)+'\n',encoding='utf8')
folder=r/'docs/publication091';folder.mkdir(exist_ok=True)
for name in ['publication-audit091.json','push091.log','release091.py','audit-publication091.py','publication091.py','freeze091.py','document091.py','check-guide091.cjs','visual091.json','documentation-check.json']:
 shutil.copyfile(r/'build'/name,folder/name)
notice=f"Published release {release}; annotated readme-091 tag object {tag}. Actual main/edition091/tag push fetched and verified: {audit['canonicalSourceFiles']} canonical sources, {audit['frozenHashes']} frozen hashes across91 editions and {audit['original091EvidenceFiles']} logical original archive entries. "
for name,link in [('AGENTS.md','docs/PUBLICATION_091.json'),('docs/PROJECT_MEMORY.md','PUBLICATION_091.json'),('docs/DEVELOPMENT_LOG.md','PUBLICATION_091.json')]:
 p=r/name;s=p.read_text(encoding='utf8');s=s.replace('## Edition091 — locally verified; publication pending','## Edition091 — published and fetched back\n\n'+notice+'[Publication receipt]('+link+').',1);p.write_text(s,encoding='utf8',newline='\n')
p=r/'README.md';s=p.read_text(encoding='utf8');needle='**Latest: [edition091 — less repeated verification work](README-091.md).**';s=s.replace(needle,needle+'\n\nPublished to main with original evidence and screenshots. [Fetched GitHub verification](docs/PUBLICATION_091.json).',1);p.write_text(s,encoding='utf8',newline='\n')
p=r/'docs/obsidian/99-native-envelope-verification.md';p.write_text(p.read_text(encoding='utf8')+'\n'+notice+'[[docs/PUBLICATION_091.json|Publication receipt]]\n',encoding='utf8',newline='\n')
print(notice)
