from pathlib import Path
import subprocess,json,shutil
r=Path.cwd()
def git(*a):return subprocess.check_output(['git',*a],text=True).strip()
audit=json.loads((r/'build/publication-audit108.json').read_text(encoding='utf8'));assert audit['passed'] and audit['remoteFetched']
release=git('rev-parse','readme-108^{commit}');assert release==audit['commit']==git('rev-parse','HEAD');tag=git('rev-parse','readme-108')
data={'edition':108,'releaseCommit':release,'annotatedTag':'readme-108','tagObject':tag,'audit':audit,'scope':'Actual non-force atomic main/edition108/tag push, fetched and audited from Git blobs. Explicit coordination-only transition retains original107 identity, sources, proofs and publication. Five fresh cases plus selected report, two abrupt recoveries, fresh reuse and five required refusals. All original checks and full costs retained. No arbitrary verifier migration, selective refresh or weight training. CI separate.','analysis':json.loads((r/'build/analysis108.json').read_text(encoding='utf8')),'releaseCI':json.loads((r/'build/ci108-release.json').read_text(encoding='utf8'))}
(r/'docs/PUBLICATION_108.json').write_text(json.dumps(data,indent=2)+'\n',encoding='utf8',newline='\n')
folder=r/'docs/publication108';folder.mkdir(exist_ok=True)
names=['documentation108-first.json','doc-check108-first.log','recorder-only108.json','analysis108-prior.json','prior-boundary108.json','inspect108-original-contract.log','transition108.log','correct-recorder108.py','initial108-build.json','ci108-release.json','publication-audit108.json','push108.log','release108.log','java108-command-refusal.log','release108.py','audit-publication108.py','publication108.py','finish-publication108.py','freeze108.py','check-guide108.cjs','visual108.json','visual-review108.json','documentation-check.json','doc-check108.log','guide108.log','guide108-1440.png','guide108-390.png','ci108-parent.json','analysis108.json','java108-new.log','java108-full.log','lineage-change108.json','java108-final.log','transition108-final.log','inspect108.log','desktop108.log','desktop108-additional.log','renderer101-108.log','renderer102-108.log','diagnostics108.log','source-identity108.json']
for name in names:
 assert (r/'build'/name).is_file(),name
 shutil.copyfile(r/'build'/name,folder/name)
notice=f"Published release {release}; annotated readme-108 tag object {tag}. Actual main/edition108/tag push fetched and verified: {audit['canonicalSourceFiles']} canonical sources, {audit['frozenHashes']} frozen hashes across108 editions and {audit['original108EvidenceFiles']} logical original archive entries. "
validation='126 distinct Java checks plus22 final affected rechecks,148 desktop checks,23 browser controls and28 altered-record refusals pass. Twelve actual expectations pass: seven completions including two abrupt recoveries and five required refusals. Prior recorder-boundary refusal and full331.632s costs retained separately; corrected fresh trial passes declared boundaries. Two cold clean/upgrade pairs retain all costs. Every original source/outcome gate retained. Actual images reviewed at1600/1100; guide at1440/390. Preserve private worlds and frozen editions; no delegation. '


for name,link in [('AGENTS.md','docs/PUBLICATION_108.json'),('docs/PROJECT_MEMORY.md','PUBLICATION_108.json'),('docs/DEVELOPMENT_LOG.md','PUBLICATION_108.json'),('docs/ARCHITECTURE.md','PUBLICATION_108.json'),('README-CONTRIBUTORS.md','docs/PUBLICATION_108.json')]:
 p=r/name;s=p.read_text(encoding='utf8');assert '## Edition108 - validated explicit qualification upgrades' in s
 s=s.replace('## Edition108 - validated explicit qualification upgrades','## Edition108 - published and fetched back\n\n'+notice+validation+'[Publication receipt]('+link+').',1)
 s=s.replace('Publication pending actual push/fetch; CI separate.','Main/tag publication and fetched verification completed; remote CI status is recorded separately in the receipt.',1)
 p.write_text(s,encoding='utf8',newline='\n')
p=r/'README.md';p.write_text(p.read_text(encoding='utf8')+'\nPublished to main. [Fetched GitHub verification](docs/PUBLICATION_108.json).\n',encoding='utf8',newline='\n')
p=r/'docs/obsidian/116-explicit-qualification-upgrades.md';p.write_text(p.read_text(encoding='utf8').replace('Publication pending actual push/fetch; CI separate.','Main/tag publication and fetched verification completed; remote CI separate.')+'\n'+notice+validation+'[[docs/PUBLICATION_108.json|Publication receipt]]\n',encoding='utf8',newline='\n')
print(notice)
