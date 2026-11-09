from pathlib import Path
import subprocess,json,shutil
r=Path.cwd()
def git(*a):return subprocess.check_output(['git',*a],text=True).strip()
audit=json.loads((r/'build/publication-audit099.json').read_text(encoding='utf8'));assert audit['passed'] and audit['remoteFetched']
release=git('rev-parse','readme-099^{commit}');assert release==audit['commit']==git('rev-parse','HEAD');tag=git('rev-parse','readme-099')
data={'edition':99,'releaseCommit':release,'annotatedTag':'readme-099','tagObject':tag,'audit':audit,'scope':'Actual non-force atomic main/edition099/tag push, fetched and independently audited from Git blobs. Local model/native/data/creation/layout/restart tests passed. Retained initial failures are part of the original evidence. Post-push CI is separate.','analysis':json.loads((r/'build/analysis099.json').read_text(encoding='utf8')),'releaseCI':json.loads((r/'build/ci099-release.json').read_text(encoding='utf8'))}
(r/'docs/PUBLICATION_099.json').write_text(json.dumps(data,indent=2)+'\n',encoding='utf8')
folder=r/'docs/publication099';folder.mkdir(exist_ok=True)
for name in ['ci099-release.json','publication-audit099.json','push099.log','release099.py','audit-publication099.py','publication099.py','freeze099.py','document099.py','check-guide099.cjs','visual099.json','visual-review099.json','documentation-check.json','doc-check099.log','guide099.log','guide099-1440.png','guide099-390.png','ci099-status.json','analysis099.json','source-comparison099.json','desktop099-summary.json','unit099-final.log','layout099-final.log','layout099-complete.log','review099.cjs','review099-final.log','refine-layout099.py']:
 if (r/'build'/name).is_file():shutil.copyfile(r/'build'/name,folder/name)
notice=f"Published release {release}; annotated readme-099 tag object {tag}. Actual main/edition099/tag push fetched and verified: {audit['canonicalSourceFiles']} canonical sources, {audit['frozenHashes']} frozen hashes across99 editions and {audit['original099EvidenceFiles']} logical original archive entries. "
for name,link in [('AGENTS.md','docs/PUBLICATION_099.json'),('docs/PROJECT_MEMORY.md','PUBLICATION_099.json'),('docs/DEVELOPMENT_LOG.md','PUBLICATION_099.json'),('docs/ARCHITECTURE.md','PUBLICATION_099.json'),('README-CONTRIBUTORS.md','docs/PUBLICATION_099.json')]:
 p=r/name;s=p.read_text(encoding='utf8');assert '## Edition099 — locally verified; publication pending' in s;s=s.replace('## Edition099 — locally verified; publication pending','## Edition099 — published and fetched back\n\n'+notice+'[Publication receipt]('+link+').',1).replace('Main/tag publication and fetched receipt still pending.','Main/tag publication and fetched-content audit completed. Post-push CI is separate.',1);p.write_text(s,encoding='utf8',newline='\n')
p=r/'README.md';p.write_text(p.read_text(encoding='utf8').replace('Publication receipt follows the actual push and fetched-content audit.','Published to main. [Fetched GitHub verification](docs/PUBLICATION_099.json).'),encoding='utf8',newline='\n')
p=r/'docs/obsidian/107-local-model-workflows.md';p.write_text(p.read_text(encoding='utf8')+'\n'+notice+'[[docs/PUBLICATION_099.json|Publication receipt]]\n',encoding='utf8',newline='\n')
print(notice)
