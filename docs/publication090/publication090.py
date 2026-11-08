from pathlib import Path
import subprocess,json,gzip
r=Path.cwd()
def git(*a):return subprocess.check_output(['git',*a],text=True).strip()
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def write(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8',newline='\n')
def js(p,v):write(p,json.dumps(v,indent=2)+'\n')
a=load(r/'build/publication-audit090.json');assert a['passed'] and a['remoteFetched'];release=a['commit'];tag=git('rev-parse','refs/verify/edition090-tag');assert git('rev-parse','refs/verify/edition090-tag^{commit}')==release
v=load(r/'docs/readmes/090/verification.json');doc=load(r/'build/documentation-check.json');assert doc['passed'];x=v['analysis']
js(r/'docs/publication090/release-audit.json',a);js(r/'docs/publication090/documentation.json',doc);(r/'docs/publication090/push.log.gz').write_bytes(gzip.compress((r/'build/push090.log').read_bytes(),mtime=0))
js(r/'docs/PUBLICATION_090.json',{**a,'releaseCommit':release,'annotatedTag':'readme-090','tagObject':tag,'publishedRefs':['refs/heads/main','refs/heads/edition090','refs/tags/readme-090'],'documentation':doc,'evidence':'docs/readmes/090/verification.json'})
p=r/'README.md';s=p.read_text(encoding='utf8');old='Edition090 is locally verified; publication receipt follows actual fetched verification.';assert old in s;write(p,s.replace(old,'Edition090 is published to main with code, measured outcomes, screenshots and exact original evidence. [Fetched GitHub verification](docs/PUBLICATION_090.json).'))
for name in ['docs/ARCHITECTURE.md','docs/obsidian/98-cost-freshness-and-mixed-tasks.md']:
 p=r/name;s=p.read_text(encoding='utf8');start=s.rfind('Edition090 implemented and locally validated.');assert start>=0
 head,tail=s[:start],s[start:];tail=tail.replace('Edition090 implemented and locally validated.','Edition090 implemented, validated and published to main.',1).replace('Publication remains pending fetched verification.','Fetched release verification is recorded in docs/PUBLICATION_090.json.',1);write(p,head+tail)
for name in ['publication090.py','push-receipt090.py']:
 write(r/'docs/publication090'/name,(r/'build'/name).read_text(encoding='utf8'))
helpful=sum(t['memorySavingMillis']>0 for t in x['tasks']);harmful=sum(t['memorySavingMillis']<0 for t in x['tasks'])
follow=v['followup'];fh=sum(t['memorySavingMillis']>0 for t in follow['tasks']);fc=sum(t['memorySavingMillis']<0 for t in follow['tasks'])
entry=f'''## Edition090 — published and fetched back

Release {release}; annotated readme-090 tag object {tag}. Actual atomic main/edition090/tag publication to private eyeoverthink/Fraynix-Agent-Workspace fetched and audited: {a['canonicalSourceFiles']} canonical sources, {a['frozenHashes']} frozen hashes across90 editions, {a['original090EvidenceFiles']} logical originals with compressed and raw hashes verified. [Publication receipt](docs/PUBLICATION_090.json).

Java now detects scene coverage changes and bounded recent memory-cost disagreement. It rechecks original cost/job/native/commit evidence and retains warnings for a model until verified replacement. Original controller, model fitting/admission, proof, report and outcome checks remain.328 prior engine classes are byte-identical to089; GoalRunner, WorkspaceServer and WorldProcedure079 integrate the guard; TaskCost090 is new.

{v['javaUniqueCases']} distinct Java checks across source-qualified invocations,135 desktop checks and49 newly executed native goals pass. The initial normalized-shape coverage bug produced eight failed new tests; the fix passed all16 freshness tests plus29 affected checks. Other240 checks passed in the initial run; original failures retained.18/18 original mixed tasks completed on2CPU/512MiB with one fresh receipt/fit. Forced memory was faster on{helpful}/6 tasks and slower on{harmful}/6. The separate shorter-trip diagnostic follow-up completed nine actual warmups and9/9 task comparisons; memory was faster on{fh}/3 and slower on{fc}/3. The original obstacle change did not remove direct control's timeout; follow-up cases were declared afterward and are not independent held-out confirmation. Use README090 for full guarded totals, overhead and limitations. No population confidence, physical accuracy or LLM-weight training claim.

README090/screenshots/offline results, all original evidence, Obsidian98, source inventory and continuity notes published. Original live045/qa046/private data and localmain045 preserved. No subagents. Next: measure whether freshness checking repays its own cost and broaden independently held-out families; preserve001–090 and use091 next.

'''
for name in ['AGENTS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md']:
 p=r/name;s=p.read_text(encoding='utf8');write(p,(entry.replace('](docs/','](') if name.startswith('docs/') else entry)+s)
print('Prepared fetched publication receipt',release)
