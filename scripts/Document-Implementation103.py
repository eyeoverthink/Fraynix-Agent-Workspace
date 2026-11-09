"""Build edition103 documentation only from completed validation and actual app captures."""
from pathlib import Path
import json, hashlib, gzip, shutil, subprocess, xml.etree.ElementTree as ET

r=Path(__file__).resolve().parent.parent
d=r/'docs/readmes/103'
def load(p): return json.loads((r/p).read_text(encoding='utf-8-sig'))
def write(p,s): p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8')
def js(p,v): write(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
def sha(b): return hashlib.sha256(b).hexdigest()
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-103'],cwd=r).returncode==1
analysis=load('build/analysis103.json');assert analysis['passed'] and len(analysis['refusals'])==16
trial=load('build/reuse103/result.json');assert trial['passed']
visual=load('build/visual-review103.json');assert visual['passed']
tests=[]
for f in sorted((r/'workspace-engine/build/test-results/test').glob('TEST-*.xml')):
    e=ET.parse(f).getroot();assert e.attrib['failures']=='0' and e.attrib['errors']=='0' and e.attrib['skipped']=='0'
    tests.append(dict(e.attrib))
assert sum(int(x['tests']) for x in tests)>=88
assert 'fail 0' in (r/'build/desktop103.log').read_text(encoding='utf-8-sig')
assert load('build/source-identity103.json')['passed']
for name,count in [('renderer101-103',5),('renderer102-103',11),('diagnostics103',7)]:
    proof=load('build/'+name+'/result.json');assert proof['passed'] and len(proof['results'])==count
d.mkdir(parents=True,exist_ok=True)
js(d/'analysis.json',analysis)
js(d/'verification.json',{'passed':True,'java':tests,'independentRefusals':analysis['refusals'],'visual':visual,'sourceIdentity':load('build/source-identity103.json'),'build':load('build/study095-build.json'),'parentCI':load('build/ci103-parent.json')})
captions={
 'recorded52-restarted-1600.png':'Actual recorded52 result after abrupt Java interruption, explicit Resume and fresh checks; the saved implementation is unchanged.',
 'synthetic96-descendant-1600.png':'A saved descendant reuses the same code on a new, explicitly synthetic reversal sequence.',
 'synthetic32-flat-1600.png':'A new flat synthetic sequence passes with all source rows checked.',
 'quality-refused-1600.png':'Fresh stricter criteria reject the old implementation’s fixed quality label.',
 'display-fault-refused-1600.png':'The independent audit refuses a deliberately corrupted displayed cell, despite passing earlier behavior checks.',
 'summary16-refused-1600.png':'The original summary code contains fixed numbers; the changed input is correctly refused.',
 'reuse103-1100.png':'The actual workspace at1100pixels; the reuse mode remains visible.',
 'restart103.png':'The actual paused task before deliberate interruption; completed data remains bound.'}
for name in captions: shutil.copyfile(r/'build/reuse103'/name,d/name)
archive=[]
roots=['build/reuse103','build/renderer101-103','build/renderer102-103','build/diagnostics103','build/junit103-initial','build/junit103-second','workspace-engine/build/test-results/test']
extras=['build/reuse103-continuation.log','build/reuse103-harness-failure.log','build/reuse103-result-before-harness-fix.json','build/reuse103.log','build/desktop103-additional.log','build/renderer101-103-encoding-failure.log','build/before-encoding103-build.json','build/java103-initial.log','build/java103-final.log','build/java103-verified.log','build/desktop103.log','build/renderer101-103.log','build/renderer102-103.log','build/diagnostics103.log','build/source-identity103.json','build/ci103-parent.json','build/study095-build.json','build/analysis103.json','build/visual-review103.json']
paths=[]
for folder in roots:
    for p in (r/folder).rglob('*'):
        if p.is_file() and not any(k in {'desktop-profile','runtime-042','.northstar-kernel','binary'} for k in p.parts) and not p.name.endswith('.lock'):paths.append(p)
paths.extend(r/p for p in extras)
for p in sorted(set(paths)):
    raw=p.read_bytes();compressed=gzip.compress(raw,mtime=0);name=d/'original'/(sha(raw)+'.gz');name.parent.mkdir(exist_ok=True);name.write_bytes(compressed)
    archive.append({'logicalPath':p.relative_to(r).as_posix(),'path':name.relative_to(r).as_posix(),'bytes':len(compressed),'sha256':sha(compressed),'rawBytes':len(raw),'rawSha256':sha(raw)})
js(d/'original-evidence.json',{'files':archive,'scope':'Lossless original trial checkpoints, app results, measured failures, initial test harness failures, logs and final verification. Private live projects are excluded. Recorded and synthetic sources remain labelled.'})
kinds={x['path']:x['hashKind'] for x in load('docs/readmes/102/file-inventory.json')}
for p in ['docs/EDITION_103_CONTRACT.md','workspace-engine/src/main/java/fraymus/workspace/Implementation103.java','workspace-engine/src/test/java/fraymus/workspace/Implementation103Test.java','workspace-desktop/test/implementation103-integration.cjs','scripts/Inspect-Implementation103.cjs','scripts/Prepare-Implementation103.cjs','scripts/Document-Implementation103.py','scripts/implementation103-guide.html']:kinds[p]='utf8-lf'
inventory=[]
for p,kind in sorted(kinds.items()):
    b=(r/p).read_bytes();b=b.decode('utf8').replace('\r\n','\n').encode() if kind=='utf8-lf' else b;inventory.append({'path':p,'bytes':len(b),'sha256':sha(b),'hashKind':kind})
js(d/'file-inventory.json',inventory)
note='docs/obsidian/111-verified-implementation-reuse.md'
modules=[('chat','Request and selected input','workspace-engine/src/main/java/fraymus/workspace/Domain044.java'),('parent','Durable workflow and ancestry','workspace-engine/src/main/java/fraymus/workspace/WorkflowRunner.java'),('binding','Exact implementation identity','workspace-engine/src/main/java/fraymus/workspace/Implementation103.java'),('child','Fresh content task','workspace-engine/src/main/java/fraymus/workspace/GoalRunner.java'),('audit','Unchanged source/outcome verifier','workspace-engine/src/main/java/fraymus/workspace/DataSummary102.java'),('inspection','Independent complete measurements','scripts/Inspect-Implementation103.cjs')]
system={'modules':[{'id':i,'name':n,'source':p,'note':note} for i,n,p in modules],'screenshots':[{'path':'docs/readmes/103/'+n,'caption':c} for n,c in captions.items()],'evidence':[{'path':'docs/readmes/103/'+n,'caption':n} for n in ['analysis.json','verification.json','original-evidence.json']]};js(d/'system-map.json',system)
table='\n'.join('| '+x['name']+' | '+str(x['rows'])+' | '+x['outcome']+' | '+' | '.join(f'{x[k]/1000:.3f}' for k in ['totalMs','browserMs','restartMs','otherMs'])+' |' for x in analysis['results'])
readme=f'''# Fraynix Agent Workspace — edition103

## Verified code, fresh data, fresh evidence

Edition103 lets a durable chat request reuse the **exact implementation** of a published data-to-creation procedure. Java analyzes the newly selected dataset, binds new input files, creates a separate artifact, runs the original browser checks and independently verifies the rendered outcome. It does not copy the old passing verdict.

The measured result is **three completed requests and three correctly refused candidates**. All six used retained local-model code without a new content-generation call. One successful request survived an abrupt Java process interruption between stages, and its saved procedure was reused after a cold restart. This is useful procedural memory; it is not model-weight training or a demonstration of general autonomous learning.

[Previous edition102](README-102.md) · [Interactive offline guide](docs/readmes/103/index.html) · [Full measurements](docs/readmes/103/analysis.json) · [Validation and build identity](docs/readmes/103/verification.json) · [Contributor direction](README-CONTRIBUTORS.md)

![Actual recorded result after recovery](docs/readmes/103/recorded52-restarted-1600.png)

## Why this edition exists

Edition102 showed both a useful workflow and an expensive limitation. It could analyze data, generate an interactive report, check it, save the procedure and recover an interrupted task. However, invoking a saved procedure regenerated its code. The changed-input summary request exhausted three drafts and three resumes while still confusing travel with range. That request remains a failure in the frozen102 record;103 does not reopen it or extend its limits.

Fresh inspection also found that the successful49-row summary hardcoded its six summary values. Its chart and table read the source, but its summary did not generalize. A verified artifact is evidence for the inputs and contract it actually passed. Reuse needs another full check. Edition103 tests this directly instead of assuming that a saved artifact will work on every new dataset.

## What the application now does

The existing local workspace still combines chat, a browser, a creation canvas, Game Lab, spatial worlds, data tools, physics and persistent procedures. Java owns the durable task state and verification boundaries. The desktop presents the work and supplies actual browser observations. This release changes the data-to-creation reuse path; it does not replace the game or physics runtime, introduce another window as the agent’s brain, or change the older world controllers.

For a saved source explorer or motion summary, chat now selects implementation reuse by default. The Mission panel identifies **Reused code · fresh checks**. An explicit request containing “regenerate” selects the previous generation path. Direct API callers retain the old default unless they pass the boolean `reuseImplementation103: true`; this avoids silently changing older automation. The new option applies only to the registered101/102 data workflows, not arbitrary programs, general tool handlers or every skill family.

### The complete execution path

1. Select a dataset and name a published procedure and version.
2. Java checks the original parent completion, published skill linkage, source data, verified normalized handoff, artifact version/hash, behavior receipt and independent outcome audit.
3. Java freezes a separate103 implementation binding: original workflow and child, skill reference, verifier identity, exact implementation hash, model provenance and current reuse protocol identity.
4. The fresh data child checks the selected dataset and quality criteria. Its normalized report is verified before creation.
5. The content child copies the original non-input files byte-for-byte. Only the reserved `inputs/source.csv` and `inputs/source.json` are replaced with the fresh Java-owned handoff. The result receives its own artifact identity and no inherited browser receipt.
6. The actual canvas performs the frozen behavior checks. The independent audit then reads all rows, exact filtering, no-match filtering and reset. A summary must additionally pass all six numeric outputs, every SVG point, coordinate system, units and visibility checks.
7. Completion and skill publication follow durable parent completion. A saved descendant retains its ancestry. Recovery reuses the same child intent and checks the original and new bindings again.

The implementation ancestry is bounded to eight links. Source, artifact, protocol or lineage changes refuse continuation. This is conservative compatibility handling: historical evidence is retained rather than relabelled under changed code.

## Measured outcomes and complete costs

These are actual Electron/Java runs on isolated projects restored from the published102 archive. Sources comprise one unchanged recorded Delfys sequence and explicitly synthetic arithmetic sequences. Synthetic inputs are test data, not new camera measurements or simulated physical experiments.

All costs are seconds. Total is the complete caller interval from submission through completion or refusal. Browser spans are unioned; nested reads are not added twice. Restart includes the deliberate interruption and reopening. Other includes Java source verification, analysis, persistence, scheduling and harness polling. Initial startup/import and work between requests sit outside these request intervals and remain in the original cohort timestamps. Skill saving is inside the first request. Cold reopening of its saved skill is recorded separately.

| Request | Rows | Outcome | Total s | Browser s | Restart s | Other s |
|---|---:|---|---:|---:|---:|---:|
{table}

There were **zero new content-generation calls** for these six requests. The original model provenance remains attached to the copied code. These timings are not a controlled speedup comparison with102: the inputs, paths, restart conditions and outcomes differ. No replacement generation was hidden outside the measured request. Refused candidates consumed work and are included.

### Three completed requests

The recorded52 request reused the49-row source explorer with fresh data under the declared0.05-second gap and5m/s speed limits. It paused after verified data, underwent deliberate process termination and resumed through the actual Mission control. The original data child and implementation binding survived. Its complete procedure was saved as `retained-explorer103`.

After a cold application restart, that descendant handled a96-row synthetic reversal sequence. A separate32-row flat sequence also passed. Both retained the exact implementation bytes while changing input bytes, artifact identity and observations. Every source row and all four table states were checked again.

![Reversal sequence through a retained descendant](docs/readmes/103/synthetic96-descendant-1600.png)

### Three refused candidates

The explorer’s quality label is fixed in the original code. A stricter0.01-second gap criterion produces real quality flags in the fresh analysis, while the old page still says it is within limits. The unchanged behavior check rejects that mismatch.

The display-fault control deliberately changes one actual displayed cell after behavior checks and before the independent source audit. The audit rejects the altered value. This is a labelled injected fault, not a spontaneous model mistake or an edited source file.

The16-row summary control reuses the exact hardcoded49-row implementation. It correctly refuses its stale numbers. The new data has0.310000m total travel; the old page still contains0.775845m from its original source. No code patch, new model draft or substitute result is inserted to turn this refusal into success.

![Changed-input summary refused](docs/readmes/103/summary16-refused-1600.png)

A reused candidate receives one checked attempt. A failed behavior or independent audit is retained and interrupts the request. Resume cannot recycle its earlier passing behavior receipt or trigger silent regeneration. Stop the refused task, then explicitly start a separate regeneration request if wanted. The original successful artifact and skill remain unchanged.

## Try it in the application

Use a published data procedure in your own project; the test procedure names below exist in the restored review project, not automatically in unrelated private worlds.

1. Open Inputs, import or select a motion CSV and confirm its column/unit mapping.
2. Choose the saved procedure/version and enter: “Reuse cost-explorer102 version 1 on the selected dataset with gap limit 0.05 seconds and speed limit 5 m/s.”
3. Open Mission. Confirm the selected source, reuse indicator, data stage, content stage and final verification status. Open the actual result when completed.
4. To retain the whole successful procedure, add “Save the whole workflow as retained-explorer103.” Reopen the app and reuse that exact version with another selected dataset.
5. To review recovery, add “Review after data.” Resume from Mission after the pause. Do not assume a closed window means the task completed.
6. For a separate regenerated implementation, use “Reuse cost-explorer102 version 1 on the selected dataset and regenerate the implementation.” Generation still has the existing budgets and full checks.103 does not claim a new successful regenerated summary.

![Actual narrower workspace](docs/readmes/103/reuse103-1100.png)

## Reproduce and inspect

The repository keeps the Java21/Gradle and existing desktop dependencies. No new external package is introduced for this feature. Build the workspace engine before launching; the ordinary launcher remains `Launch-Agent-Workspace.bat`. Use a separate project location for experiments so private worlds and databases are preserved.

`node scripts/Prepare-Implementation103.cjs` restores original102 evidence into a new `build/reuse103` folder and refuses an existing destination. It verifies both compressed and decompressed bytes against the frozen archive. It never launches the old live projects. The integration harness is `workspace-desktop/test/implementation103-integration.cjs`; it uses the real app and existing model catalog, without a new content inference. The recorded102 model artifacts are inputs, not invented model responses.

`scripts/Inspect-Implementation103.cjs` independently recomputes original source arithmetic, quality flags, normalized rows, exact implementation and artifact hashes, handoff lineage, actual browser assertions, four table states, refusal causes and complete cost accounting. Sixteen altered evidence records are refused. It checks the actual generation journal for omitted calls. The initial test harness mistakes are archived as failures: it first omitted the required data report host check, then expected one narrower exception class than the legitimate file validator returned. Those were corrected without weakening production checks. The first app launch also caught an invalid text encoding in the new status label. That production text was corrected; the original failure log is retained, and the subsequent56-file source integrity check and real browser trials passed. After the first request completed, the integration harness used POST for the GET-only state endpoint. The method was corrected and the remaining trials continued; the completed request was not repeated. This assistance is outside the six request intervals and remains in the full cohort timestamps and original failure record.

The final focused Java suite contains{sum(int(x['tests']) for x in tests)} methods. Existing desktop tests and actual legacy/summary/diagnostic browser controls also pass. The source comparison confirms that the101/102 verifier class bytes are unchanged. Full originals, failures and receipts are retained in the [lossless evidence manifest](docs/readmes/103/original-evidence.json). The source inventory and all frozen editions are checked before publication. Remote CI is reported separately in the publication receipt; local validation is not described as CI success.

## Limits and next steps

This release proves bounded implementation reuse for registered data workflows. It does not make every generated artifact reusable. Both retained implementations have fixed quality labels, and the retained summary has fixed metrics. The guard catches those limitations; it does not fix the implementation automatically. Browser checks cover declared behaviors and inspected DOM/SVG visibility, not universal visual correctness, arbitrary occlusion or every interpretation of a request.

The next useful step is a genuinely input-driven summary implementation, with a separately defined input contract for quality criteria and source-derived metrics. Verify it across reversal, flat, gap and changed-unit cases before retaining it. Keep old artifacts and checks unchanged. After that, measure when a cheap reuse attempt is worthwhile versus explicit regeneration, including refusal cost and growing ancestry. A bounded compatibility descriptor could prevent obviously unsuitable reuse, but it must never replace fresh outcome checks.

The direction remains a local agent that remembers useful verified work, acts through owned tools, observes the result and keeps its evidence across restarts.103 makes that practical for existing creation code. It contributes persistent procedural competence without claiming that changing weights, general intelligence or reliable self-improvement have already been demonstrated.
'''
write(r/'README-103.md',readme)
write(r/'README.md','# Fraynix Agent Workspace\n\nEdition103 reuses verified implementations with fresh data and every original source/outcome check. Three requests completed; three incompatible or corrupted candidates were correctly refused. No new content-generation calls were needed in the six measured reuse trials.\n\n[Edition103: functions, measurements and next steps](README-103.md) · [Interactive offline guide](docs/readmes/103/index.html) · [Previous edition](README-102.md) · [Contributor direction](README-CONTRIBUTORS.md)\n\nJava owns durable goals, physics, tools, verification and retained procedures. Chat, browser, creation canvas, Game Lab and spatial views share those capabilities.\n\n![Actual retained implementation with fresh recorded input](docs/readmes/103/recorded52-restarted-1600.png)\n\nPublication receipt is added after the actual main/tag push and fetched verification.\n')
note_text='''# Edition103 — exact code reuse with fresh evidence

2026-11-09, America/Los_Angeles. Isolated S:/Codex-Work/fraynix-workspace-edition103. Three actual reuse requests completed and three were refused as required. All six retain exact original102 model code; no new generation calls. Fresh data reports, new artifact identities, behavior receipts and independent audits remain mandatory. Recorded52 survived abrupt Java interruption and explicit Resume; its saved descendant survived cold restart and handled96-row reversal and32-row flat synthetic inputs. Strict quality, actual injected cell corruption and hardcoded summary16 all refused; no failed skill promoted.

Source/outcome verifier classes from101/102 are byte-identical. New103 protocol identity and exact ancestry are separate; eight-link bound. Refused reuse cannot silently regenerate or bypass independent failure on Resume. Original private worlds and frozen001–102 remain untouched. No delegation; heavy work serial. Read README103 and docs/EDITION_103_CONTRACT.md. Next: input-driven summary and quality contract, held-out reuse checks, then measure reuse-versus-regeneration costs. No weight-training or matched speedup claim. Publication pending actual push/fetch; CI separate.
'''
write(r/note,note_text+'\n[[README-103.md|Full edition103]] · [[docs/obsidian/110-measured-content-procedures.md|Previous measured creation work]]\n')
for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
    p=r/name;old=p.read_text(encoding='utf8');header='## Edition103 — validated implementation reuse\n\n'
    if header not in old:write(p,header+note_text.split('\n\n',1)[1]+'\n\n'+old)
js(r/'docs/obsidian/111-verified-implementation-reuse.canvas',{'nodes':[{'id':'request','type':'file','file':'README-103.md','x':0,'y':0,'width':500,'height':320},{'id':'memory','type':'file','file':note,'x':580,'y':0,'width':500,'height':320}],'edges':[{'id':'verified-link','fromNode':'request','fromSide':'right','toNode':'memory','toSide':'left','label':'retain exact implementation, verify new outcomes'}]})
template=(r/'scripts/implementation103-guide.html').read_text(encoding='utf8');write(d/'index.html',template.replace('SYSTEM_JSON',json.dumps(system)).replace('INVENTORY_JSON',json.dumps(inventory)).replace('RESULT_JSON',json.dumps(analysis)))
print(json.dumps({'sourceFiles':len(inventory),'originalEntries':len(archive),'readmeWords':len(readme.split())}))
