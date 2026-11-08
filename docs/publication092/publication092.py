from pathlib import Path
import subprocess,json,shutil
r=Path.cwd()
def git(*args):return subprocess.check_output(['git',*args],text=True).strip()
audit=json.loads((r/'build/publication-audit092.json').read_text());assert audit['passed'] and audit['remoteFetched']
release=git('rev-parse','readme-092^{commit}');assert release==audit['commit']==git('rev-parse','HEAD')
tag=git('rev-parse','readme-092');data={'edition':92,'releaseCommit':release,'annotatedTag':'readme-092','tagObject':tag,'audit':audit,'visualFollowup':'A fresh390px browser capture confirmed final text wrapping after a transient paint artifact in the multi-capture run. The original capture remains in the frozen QA archive; clean follow-up is included here.', 'scope':'Actual non-force atomic main/edition092/tag push, then fetch and complete Git-tree verification. This receipt follows the frozen release; it does not amend the release tag.'}
(r/'docs/PUBLICATION_092.json').write_text(json.dumps(data,indent=2)+'\n',encoding='utf8')
folder=r/'docs/publication092';folder.mkdir(exist_ok=True)
for name in ['publication-audit092.json','push092.log','release092.py','audit-publication092.py','publication092.py','freeze092.py','document092.py','check-guide092.cjs','visual092.json','documentation-check.json','offline092-review-390.png','review-capture092.cjs','review-capture092.log','visual-app092.json']:
 shutil.copyfile(r/'build'/name,folder/name)
notice=f"Published release {release}; annotated readme-092 tag object {tag}. Actual main/edition092/tag push fetched and verified: {audit['canonicalSourceFiles']} canonical sources, {audit['frozenHashes']} frozen hashes across92 editions and {audit['original092EvidenceFiles']} logical original archive entries. "
for name,link in [('AGENTS.md','docs/PUBLICATION_092.json'),('docs/PROJECT_MEMORY.md','PUBLICATION_092.json'),('docs/DEVELOPMENT_LOG.md','PUBLICATION_092.json')]:
 p=r/name;s=p.read_text(encoding='utf8');s=s.replace('## Edition092 — locally verified; publication pending','## Edition092 — published and fetched back\n\n'+notice+'[Publication receipt]('+link+').',1);p.write_text(s,encoding='utf8',newline='\n')
p=r/'README.md';s=p.read_text(encoding='utf8');needle='**Latest: [edition092 — growing banks and restarted tasks](README-092.md).**';s=s.replace(needle,needle+'\n\nPublished to main with original evidence and screenshots. [Fetched GitHub verification](docs/PUBLICATION_092.json).',1);p.write_text(s,encoding='utf8',newline='\n')
p=r/'docs/obsidian/100-growing-bank-restarts.md';p.write_text(p.read_text(encoding='utf8')+'\n'+notice+'[[docs/PUBLICATION_092.json|Publication receipt]]\n',encoding='utf8',newline='\n')
print(notice)
