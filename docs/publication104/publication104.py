from pathlib import Path
import subprocess,json,shutil
r=Path.cwd()
def git(*a):return subprocess.check_output(['git',*a],text=True).strip()
audit=json.loads((r/'build/publication-audit104.json').read_text(encoding='utf8'));assert audit['passed'] and audit['remoteFetched']
release=git('rev-parse','readme-104^{commit}');assert release==audit['commit']==git('rev-parse','HEAD');tag=git('rev-parse','readme-104')
data={'edition':104,'releaseCommit':release,'annotatedTag':'readme-104','tagObject':tag,'audit':audit,'scope':'Actual non-force atomic main/edition104/tag push, fetched and audited from Git blobs. Eleven actual Java-owned summary trials: nine completions, premature retention and displayed corruption refused. All six summary values and chart points calculated from fresh input; no expected metrics injected. Five prior varied original proofs rechecked before held-out retention. Cold mapped reuse and abrupt Java recovery verified. Source/outcome verifier classes byte-identical; authored code, not model-generated or weight-trained. The 96-row harness prompt-matching failure and developer assistance remain in full timings and lossless originals; no runtime change or rerun of that completed task. No matched speedup claim. Post-push CI separate','analysis':json.loads((r/'build/analysis104.json').read_text(encoding='utf8')),'releaseCI':json.loads((r/'build/ci104-release.json').read_text(encoding='utf8'))}
(r/'docs/PUBLICATION_104.json').write_text(json.dumps(data,indent=2)+'\n',encoding='utf8')
folder=r/'docs/publication104';folder.mkdir(exist_ok=True)
names=['ci104-release.json', 'publication-audit104.json', 'push104.log', 'release104.log', 'release104.py', 'audit-publication104.py', 'publication104.py', 'finish-publication104.py', 'freeze104.py', 'check-guide104.cjs', 'visual104.json', 'visual-review104.json', 'documentation-check.json', 'doc-check104.log', 'guide104.log', 'guide104-1440.png', 'guide104-390.png', 'ci104-parent.json', 'analysis104.json', 'java104-initial.log', 'desktop104.log', 'desktop104-additional.log', 'renderer101-104.log', 'renderer102-104.log', 'diagnostics104.log', 'source-identity104.json', 'summary104.log', 'summary104-harness-failure.log', 'summary104-continuation.log']
for name in names:
 assert (r/'build'/name).is_file(),name
 shutil.copyfile(r/'build'/name,folder/name)
notice=f"Published release {release}; annotated readme-104 tag object {tag}. Actual main/edition104/tag push fetched and verified: {audit['canonicalSourceFiles']} canonical sources, {audit['frozenHashes']} frozen hashes across 104 editions and {audit['original104EvidenceFiles']} logical original archive entries. "
validation='93 Java methods, 141 desktop checks, 23 actual browser fixture controls and 20 altered-evidence refusals pass. All eleven measured trial expectations pass: nine completions and two required refusals. Actual application captures reviewed at 1600 and 1100 pixels; offline guide checked at 1440 and 390 pixels. Harness collection failure and developer-assisted reopen remain in measured costs. No new delegation; preserve private worlds and frozen prior editions. '
for name,link in [('AGENTS.md','docs/PUBLICATION_104.json'),('docs/PROJECT_MEMORY.md','PUBLICATION_104.json'),('docs/DEVELOPMENT_LOG.md','PUBLICATION_104.json'),('docs/ARCHITECTURE.md','PUBLICATION_104.json'),('README-CONTRIBUTORS.md','docs/PUBLICATION_104.json')]:
 p=r/name;s=p.read_text(encoding='utf8');assert '## Edition104 — validated input-driven summaries' in s
 s=s.replace('## Edition104 — validated input-driven summaries','## Edition104 — published and fetched back\n\n'+notice+validation+'[Publication receipt]('+link+').',1)
 s=s.replace('Main/tag publication pending actual push/fetch; CI separate.','Main/tag publication and fetched verification completed; remote CI status is recorded separately in the receipt.',1)
 p.write_text(s,encoding='utf8')
p=r/'README.md';p.write_text(p.read_text(encoding='utf8').replace('Publication receipt follows the actual main/tag push and fetched verification.','Published to main. [Fetched GitHub verification](docs/PUBLICATION_104.json).'),encoding='utf8')
p=r/'docs/obsidian/112-input-driven-summaries.md';p.write_text(p.read_text(encoding='utf8').replace('Main/tag publication pending actual push/fetch; CI separate.','Main/tag publication and fetched verification completed; remote CI separate.')+'\n'+notice+validation+'[[docs/PUBLICATION_104.json|Publication receipt]]\n',encoding='utf8')
print(notice)
