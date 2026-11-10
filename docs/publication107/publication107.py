from pathlib import Path
import subprocess,json,shutil
r=Path.cwd()
def git(*a):return subprocess.check_output(['git',*a],text=True).strip()
audit=json.loads((r/'build/publication-audit107.json').read_text(encoding='utf8'));assert audit['passed'] and audit['remoteFetched']
release=git('rev-parse','readme-107^{commit}');assert release==audit['commit']==git('rev-parse','HEAD');tag=git('rev-parse','readme-107')
data={'edition':107,'releaseCommit':release,'annotatedTag':'readme-107','tagObject':tag,'audit':audit,'scope':'Actual non-force atomic main/edition107/tag push, fetched and audited from Git blobs. Explicit full refresh preserves original proof references and dependencies. Eight abrupt numerical recoveries retain exact report inputs and identities. Thirteen completions and two required source refusals. Two paired cold recheck/refresh repetitions at genuine bank1, every source/outcome check retained. Full preparation/caller/browser/restart/refusal costs. No content generation, selective migration or training claim. Parent/post-push CI separate.','analysis':json.loads((r/'build/analysis107.json').read_text(encoding='utf8')),'releaseCI':json.loads((r/'build/ci107-release.json').read_text(encoding='utf8'))}
(r/'docs/PUBLICATION_107.json').write_text(json.dumps(data,indent=2)+'\n',encoding='utf8',newline='\n')
folder=r/'docs/publication107';folder.mkdir(exist_ok=True)
names=['ci107-release.json','publication-audit107.json','push107.log','release107.log','release107-format-first.log','release107.py','audit-publication107.py','publication107.py','finish-publication107.py','freeze107.py','check-guide107.cjs','visual107.json','visual-review107.json','documentation-check.json','doc-check107.log','guide107.log','guide107-1440.png','guide107-390.png','ci107-parent.json','analysis107.json','java107-new.log','java107-final.log','restart107.log','inspect107.log','desktop107.log','desktop107-additional.log','renderer101-107.log','renderer102-107.log','diagnostics107.log','source-identity107.json']
for name in names:
 assert (r/'build'/name).is_file(),name
 shutil.copyfile(r/'build'/name,folder/name)
notice=f"Published release {release}; annotated readme-107 tag object {tag}. Actual main/edition107/tag push fetched and verified: {audit['canonicalSourceFiles']} canonical sources, {audit['frozenHashes']} frozen hashes across107 editions and {audit['original107EvidenceFiles']} logical original archive entries. "
validation='118 Java methods,148 desktop checks,23 browser controls and30 altered-record refusals pass. Fifteen actual request expectations pass:13 completions including eight abrupt numerical recoveries and two required source refusals. Two paired cold refresh/recheck repetitions retain full costs. Every original source/outcome gate retained. Actual screenshots reviewed at1600/1100 pixels and offline guide at1440/390. No delegation; preserve private worlds and frozen earlier editions. '

for name,link in [('AGENTS.md','docs/PUBLICATION_107.json'),('docs/PROJECT_MEMORY.md','PUBLICATION_107.json'),('docs/DEVELOPMENT_LOG.md','PUBLICATION_107.json'),('docs/ARCHITECTURE.md','PUBLICATION_107.json'),('README-CONTRIBUTORS.md','docs/PUBLICATION_107.json')]:
 p=r/name;s=p.read_text(encoding='utf8');assert '## Edition107 — validated recovery and refresh' in s
 s=s.replace('## Edition107 — validated recovery and refresh','## Edition107 — published and fetched back\n\n'+notice+validation+'[Publication receipt]('+link+').',1)
 s=s.replace('Main/tag publication pending actual push/fetch; CI separate.','Main/tag publication and fetched verification completed; remote CI status is recorded separately in the receipt.',1)
 p.write_text(s,encoding='utf8',newline='\n')
p=r/'README.md';p.write_text(p.read_text(encoding='utf8').replace('Publication receipt follows the actual main/tag push and fetched verification.','Published to main. [Fetched GitHub verification](docs/PUBLICATION_107.json).'),encoding='utf8',newline='\n')
p=r/'docs/obsidian/115-measured-qualification-refresh.md';p.write_text(p.read_text(encoding='utf8').replace('Main/tag publication pending actual push/fetch; CI separate.','Main/tag publication and fetched verification completed; remote CI separate.')+'\n'+notice+validation+'[[docs/PUBLICATION_107.json|Publication receipt]]\n',encoding='utf8',newline='\n')
print(notice)
