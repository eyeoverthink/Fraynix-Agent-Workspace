from pathlib import Path
import subprocess,json,shutil
r=Path.cwd()
def git(*a):return subprocess.check_output(['git',*a],text=True).strip()
audit=json.loads((r/'build/publication-audit106.json').read_text(encoding='utf8'));assert audit['passed'] and audit['remoteFetched']
release=git('rev-parse','readme-106^{commit}');assert release==audit['commit']==git('rev-parse','HEAD');tag=git('rev-parse','readme-106')
data={'edition':106,'releaseCommit':release,'annotatedTag':'readme-106','tagObject':tag,'audit':audit,'scope':'Actual non-force atomic main/edition106/tag push, fetched and audited from Git blobs. Authentic native admission refusals preserve original reasons without planner retry; cold restart and Resume prohibition verified. One qualification preparation and ten fresh numerical reuse completions; three required request refusals and five invalid import refusals. All original source/outcome checks retained. Abrupt recovery retains the verified data child. No content generation or weight training. Complete timings retained; no matched speedup claim. Parent/post-push CI separate.','analysis':json.loads((r/'build/analysis106.json').read_text(encoding='utf8')),'releaseCI':json.loads((r/'build/ci106-release.json').read_text(encoding='utf8'))}
(r/'docs/PUBLICATION_106.json').write_text(json.dumps(data,indent=2)+'\n',encoding='utf8',newline='\n')
folder=r/'docs/publication106';folder.mkdir(exist_ok=True)
names=['ci106-release.json','publication-audit106.json','push106.log','release106.log','release106.py','audit-publication106.py','publication106.py','finish-publication106.py','freeze106.py','check-guide106.cjs','visual106.json','visual-review106.json','documentation-check.json','doc-check106.log','guide106.log','guide106-1440.png','guide106-390.png','ci106-parent.json','analysis106.json','java106-final.log','numerical106-final.log','inspect106-prior.log','original-proof106.json','OriginalProof106.java','java106-first.log','java106-regression.log','inspect106.log','desktop106.log','desktop106-additional.log','renderer101-106.log','renderer102-106.log','diagnostics106.log','source-identity106.json','numerical106.log']
for name in names:
 assert (r/'build'/name).is_file(),name
 shutil.copyfile(r/'build'/name,folder/name)
notice=f"Published release {release}; annotated readme-106 tag object {tag}. Actual main/edition106/tag push fetched and verified: {audit['canonicalSourceFiles']} canonical sources, {audit['frozenHashes']} frozen hashes across106 editions and {audit['original106EvidenceFiles']} logical original archive entries. "
validation='110 Java methods,148 desktop checks,23 actual browser controls and26 final plus6 preliminary altered-record refusals pass. Fourteen actual request expectations pass: eleven completions and three required refusals, plus five invalid import refusals. Cold refusal and abrupt recovery checked; all original source/outcome gates retained. Actual screenshots reviewed at1600/1100 pixels and offline guide at1440/390. No delegation; preserve private worlds and frozen earlier editions. '

for name,link in [('AGENTS.md','docs/PUBLICATION_106.json'),('docs/PROJECT_MEMORY.md','PUBLICATION_106.json'),('docs/DEVELOPMENT_LOG.md','PUBLICATION_106.json'),('docs/ARCHITECTURE.md','PUBLICATION_106.json'),('README-CONTRIBUTORS.md','docs/PUBLICATION_106.json')]:
 p=r/name;s=p.read_text(encoding='utf8');assert '## Edition106 — validated original refusals' in s
 s=s.replace('## Edition106 — validated original refusals','## Edition106 — published and fetched back\n\n'+notice+validation+'[Publication receipt]('+link+').',1)
 s=s.replace('Main/tag publication pending actual push/fetch; CI separate.','Main/tag publication and fetched verification completed; remote CI status is recorded separately in the receipt.',1)
 p.write_text(s,encoding='utf8',newline='\n')
p=r/'README.md';p.write_text(p.read_text(encoding='utf8').replace('Publication receipt follows the actual main/tag push and fetched verification.','Published to main. [Fetched GitHub verification](docs/PUBLICATION_106.json).'),encoding='utf8',newline='\n')
p=r/'docs/obsidian/114-original-refusals.md';p.write_text(p.read_text(encoding='utf8').replace('Main/tag publication pending actual push/fetch; CI separate.','Main/tag publication and fetched verification completed; remote CI separate.')+'\n'+notice+validation+'[[docs/PUBLICATION_106.json|Publication receipt]]\n',encoding='utf8',newline='\n')
print(notice)
