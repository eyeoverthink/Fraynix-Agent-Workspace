from pathlib import Path
import subprocess,json,shutil
r=Path.cwd()
def git(*a):return subprocess.check_output(['git',*a],text=True).strip()
audit=json.loads((r/'build/publication-audit103.json').read_text(encoding='utf8'));assert audit['passed'] and audit['remoteFetched']
release=git('rev-parse','readme-103^{commit}');assert release==audit['commit']==git('rev-parse','HEAD');tag=git('rev-parse','readme-103')
data={'edition':103,'releaseCommit':release,'annotatedTag':'readme-103','tagObject':tag,'audit':audit,'scope':'Actual non-force atomic main/edition103/tag push, fetched and audited from Git blobs. Six actual retained-implementation trials: three completed on fresh sources, three required refusals. No new generation calls. Every original source and outcome check retained. Abrupt recovery, saved descendant reuse and cold restart verified. UI provenance and refused Resume corrected after timing; compiled Java bytes unchanged. Refused quality, injected displayed cell and stale summary remain failures, not repaired successes. No matched speedup or model-weight training claim. Post-push CI separate.','analysis':json.loads((r/'build/analysis103.json').read_text(encoding='utf8')),'releaseCI':json.loads((r/'build/ci103-release.json').read_text(encoding='utf8'))}
(r/'docs/PUBLICATION_103.json').write_text(json.dumps(data,indent=2)+'\n',encoding='utf8')
folder=r/'docs/publication103';folder.mkdir(exist_ok=True)
names=['ci103-release.json','publication-audit103.json','push103.log','release103.log','release103.py','audit-publication103.py','publication103.py','finish-publication103.py','freeze103.py','check-guide103.cjs','visual103.json','visual-review103.json','documentation-check.json','doc-check103.log','guide103.log','guide103-checker-failure.log','guide103-1440.png','guide103-390.png','ci103-parent.json','analysis103.json','java103-initial.log','java103-final.log','java103-verified.log','desktop103.log','desktop103-additional.log','desktop103-presentation.log','renderer101-103.log','renderer102-103.log','diagnostics103.log','source-identity103.json','before-encoding103-build.json','presentation103-comparison.json','reuse103.log','reuse103-continuation.log','reuse103-harness-failure.log','reuse103-result-before-harness-fix.json','review103.cjs','review103.log']
for name in names:
 assert (r/'build'/name).is_file(),name
 shutil.copyfile(r/'build'/name,folder/name)
notice=f"Published release {release}; annotated readme-103 tag object {tag}. Actual main/edition103/tag push fetched and verified: {audit['canonicalSourceFiles']} canonical sources, {audit['frozenHashes']} frozen hashes across 103 editions and {audit['original103EvidenceFiles']} logical original archive entries. "
validation='88 Java methods, 141 desktop checks, 23 actual browser fixture controls and 16 altered-evidence refusals pass. All six measured trial expectations pass: three completions and three required refusals. Presentation reviewed separately at 1600 and 1100 pixels; offline guide checked at 1440 and 390 pixels. No new delegation; preserve private worlds and frozen prior editions. '
for name,link in [('AGENTS.md','docs/PUBLICATION_103.json'),('docs/PROJECT_MEMORY.md','PUBLICATION_103.json'),('docs/DEVELOPMENT_LOG.md','PUBLICATION_103.json'),('docs/ARCHITECTURE.md','PUBLICATION_103.json'),('README-CONTRIBUTORS.md','docs/PUBLICATION_103.json')]:
 p=r/name;s=p.read_text(encoding='utf8');assert '## Edition103 — validated implementation reuse' in s
 s=s.replace('## Edition103 — validated implementation reuse','## Edition103 — published and fetched back\n\n'+notice+validation+'[Publication receipt]('+link+').',1)
 s=s.replace('Publication pending actual push/fetch; CI separate.','Main/tag publication and fetched verification completed; remote CI status is recorded separately in the receipt.',1)
 p.write_text(s,encoding='utf8')
p=r/'README.md';p.write_text(p.read_text(encoding='utf8').replace('Publication receipt is added after the actual main/tag push and fetched verification.','Published to main. [Fetched GitHub verification](docs/PUBLICATION_103.json).'),encoding='utf8')
p=r/'docs/obsidian/111-verified-implementation-reuse.md';p.write_text(p.read_text(encoding='utf8').replace('Publication pending actual push/fetch; CI separate.','Main/tag publication and fetched verification completed; remote CI separate.')+'\n'+notice+validation+'[[docs/PUBLICATION_103.json|Publication receipt]]\n',encoding='utf8')
print(notice)
