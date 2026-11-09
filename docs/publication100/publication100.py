from pathlib import Path
import subprocess,json,shutil
r=Path.cwd()
def git(*a):return subprocess.check_output(['git',*a],text=True).strip()
audit=json.loads((r/'build/publication-audit100.json').read_text(encoding='utf8'));assert audit['passed'] and audit['remoteFetched']
release=git('rev-parse','readme-100^{commit}');assert release==audit['commit']==git('rev-parse','HEAD');tag=git('rev-parse','readme-100')
data={'edition':100,'releaseCommit':release,'annotatedTag':'readme-100','tagObject':tag,'audit':audit,'scope':'Actual non-force atomic main/edition100/tag push, fetched and independently audited from Git blobs. Actual local-model data and creation workflows, source-derived checks, independent source and rendered-outcome inspection, layout and restart checks passed. Retained initial failures are part of the original evidence. Post-push CI is separate.','analysis':json.loads((r/'build/analysis100.json').read_text(encoding='utf8')),'releaseCI':json.loads((r/'build/ci100-release.json').read_text(encoding='utf8'))}
(r/'docs/PUBLICATION_100.json').write_text(json.dumps(data,indent=2)+'\n',encoding='utf8')
folder=r/'docs/publication100';folder.mkdir(exist_ok=True)
for name in ['ci100-release.json','publication-audit100.json','push100.log','release100.py','audit-publication100.py','publication100.py','finish-publication100.py','freeze100.py','document100.py','check-guide100.cjs','visual100.json','visual-review100.json','documentation-check.json','doc-check100.log','guide100.log','guide100-1440.png','guide100-390.png','ci100-status.json','analysis100.json','java100.log','inputs100-tests.log','desktop100-tests-final.log','layout100-final.log','layout100-complete.log','source-comparison100.json','review100.cjs','review100-final.log']:
 if (r/'build'/name).is_file():shutil.copyfile(r/'build'/name,folder/name)
notice=f"Published release {release}; annotated readme-100 tag object {tag}. Actual main/edition100/tag push fetched and verified: {audit['canonicalSourceFiles']} canonical sources, {audit['frozenHashes']} frozen hashes across100 editions and {audit['original100EvidenceFiles']} logical original archive entries. "
for name,link in [('AGENTS.md','docs/PUBLICATION_100.json'),('docs/PROJECT_MEMORY.md','PUBLICATION_100.json'),('docs/DEVELOPMENT_LOG.md','PUBLICATION_100.json'),('docs/ARCHITECTURE.md','PUBLICATION_100.json'),('README-CONTRIBUTORS.md','docs/PUBLICATION_100.json')]:
 p=r/name;s=p.read_text(encoding='utf8');assert '## Edition100 — locally verified; publication pending' in s;s=s.replace('## Edition100 — locally verified; publication pending','## Edition100 — published and fetched back\n\n'+notice+'[Publication receipt]('+link+').',1).replace('Main/tag publication and fetched receipt still pending.','Main/tag publication and fetched-content audit completed. Post-push CI is separate.',1);p.write_text(s,encoding='utf8',newline='\n')
p=r/'README.md';p.write_text(p.read_text(encoding='utf8').replace('Publication receipt follows the actual push and fetched-content audit.','Published to main. [Fetched GitHub verification](docs/PUBLICATION_100.json).'),encoding='utf8',newline='\n')
p=r/'docs/obsidian/108-verified-data-handoffs.md';p.write_text(p.read_text(encoding='utf8')+'\n'+notice+'[[docs/PUBLICATION_100.json|Publication receipt]]\n',encoding='utf8',newline='\n')
print(notice)
