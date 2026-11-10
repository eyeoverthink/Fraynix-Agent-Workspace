"""Freeze actual edition104 results, source inventory and lossless originals after validation."""
from pathlib import Path
import json,hashlib,gzip,shutil,subprocess,xml.etree.ElementTree as ET
r=Path(__file__).resolve().parent.parent;d=r/'docs/readmes/104'
def load(p):return json.loads((r/p).read_text(encoding='utf-8-sig'))
def write(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8')
def js(p,v):write(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
def sha(b):return hashlib.sha256(b).hexdigest()
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-104'],cwd=r).returncode==1
analysis=load('build/analysis104.json');assert analysis['passed'] and len(analysis['refusals'])==20
trial=load('build/summary104/result.json');assert trial['passed']
visual=load('build/visual-review104.json');assert visual['passed']
tests=[]
for f in sorted((r/'workspace-engine/build/test-results/test').glob('TEST-*.xml')):
 e=ET.parse(f).getroot();assert e.attrib['failures']=='0' and e.attrib['errors']=='0' and e.attrib['skipped']=='0';tests.append(dict(e.attrib))
assert sum(int(x['tests']) for x in tests)>=93
assert 'fail 0' in (r/'build/desktop104.log').read_text(encoding='utf-8-sig')
assert load('build/source-identity104.json')['passed']
for name,count in [('renderer101-104',5),('renderer102-104',11),('diagnostics104',7)]:
 p=load('build/'+name+'/result.json');assert p['passed'] and len(p['results'])==count
d.mkdir(parents=True,exist_ok=True);js(d/'analysis.json',analysis)
js(d/'verification.json',{'passed':True,'java':tests,'independentRefusals':analysis['refusals'],'visual':visual,'sourceIdentity':load('build/source-identity104.json'),'build':load('build/study095-build.json'),'parentCI':load('build/ci104-parent.json')})
captions={
 'heldout37-saved-1600.png':'A new held-out source passes every check and is retained only after five earlier varied-data proofs are reverified.',
 'units-flags16-1600.png':'Milliseconds and centimetres mapped to SI; new gap and speed criteria produce visible flags.',
 'reused27-mapped-1600.png':'The cold-reopened saved implementation calculates a different mapped source with new thresholds.',
 'reused109-restarted-1600.png':'A fresh 109-row reuse resumes after abrupt Java interruption and retains the verified data child.',
 'rendered-fault-refused-1600.png':'A deliberately corrupted displayed travel value is refused by the independent audit after behavior checks passed.',
 'early-retention-refused-1600.png':'The report passes, but Java refuses premature skill retention because varied-data qualification is missing.',
 'precision2-1600.png':'A half-micro-metre decimal boundary is recalculated and checked against the unchanged Java formatter.',
 'summary104-1100.png':'Actual responsive workspace at 1100 pixels.',
 'restart104.png':'Actual 109-row data report at the review pause before interruption; the lower Mission still shows the preceding 27-row task. The later recovered109 screenshot shows its matching completed Mission.'}
for name in captions:shutil.copyfile(r/'build/summary104'/name,d/name)
for row in analysis['results']:
 name=row['name']+'-1600.png';shutil.copyfile(r/'build/summary104'/name,d/name)
roots=['build/summary104','build/summary104-harness-failure','build/renderer101-104','build/renderer102-104','build/diagnostics104','workspace-engine/build/test-results/test']
extras=['build/java104-initial.log','build/desktop104.log','build/desktop104-additional.log','build/renderer101-104.log','build/renderer102-104.log','build/diagnostics104.log','build/source-identity104.json','build/ci104-parent.json','build/study095-build.json','build/analysis104.json','build/visual-review104.json','build/summary104.log','build/summary104-harness-failure.log','build/summary104-continuation.log']
paths=[]
for folder in roots:
 for p in (r/folder).rglob('*'):
  if p.is_file() and not any(k in {'desktop-profile','runtime-042','.northstar-kernel','binary'} for k in p.parts) and not p.name.endswith('.lock'):paths.append(p)
paths.extend(r/p for p in extras)
archive=[]
for p in sorted(set(paths)):
 raw=p.read_bytes();compressed=gzip.compress(raw,mtime=0);name=d/'original'/(sha(raw)+'.gz');name.parent.mkdir(exist_ok=True);name.write_bytes(compressed);archive.append({'logicalPath':p.relative_to(r).as_posix(),'path':name.relative_to(r).as_posix(),'bytes':len(compressed),'sha256':sha(compressed),'rawBytes':len(raw),'rawSha256':sha(raw)})
js(d/'original-evidence.json',{'files':archive,'scope':'Lossless actual104 workflows, original data, authored code, all successes/refusals, screenshots, journals, tests and complete timings. No private live project or model-output fixture.'})
kinds={x['path']:x['hashKind'] for x in load('docs/readmes/103/file-inventory.json')}
for p in ['docs/EDITION_104_CONTRACT.md','workspace-engine/src/main/java/fraymus/workspace/DataSummary104.java','workspace-engine/src/test/java/fraymus/workspace/DataSummary104Test.java','workspace-engine/src/main/resources/summary104.html','workspace-engine/src/main/resources/summary104.js','workspace-desktop/test/summary104-integration.cjs','scripts/Inspect-Summary104.cjs','scripts/Document-Summary104.py','scripts/summary104-guide.html']:kinds[p]='utf8-lf'
inventory=[]
for p,kind in sorted(kinds.items()):
 b=(r/p).read_bytes();b=b.decode('utf8').replace('\r\n','\n').encode() if kind=='utf8-lf' else b;inventory.append({'path':p,'bytes':len(b),'sha256':sha(b),'hashKind':kind})
js(d/'file-inventory.json',inventory)
note='docs/obsidian/112-input-driven-summaries.md'
modules=[('chat','Explicit input-driven summary intent','workspace-engine/src/main/java/fraymus/workspace/Domain044.java'),('producer','Java-owned implementation and contract','workspace-engine/src/main/java/fraymus/workspace/DataSummary104.java'),('calculation','Fresh input calculations and UI','workspace-engine/src/main/resources/summary104.js'),('workflow','Qualification and durable execution','workspace-engine/src/main/java/fraymus/workspace/WorkflowRunner.java'),('child','Pinned fresh criteria and code','workspace-engine/src/main/java/fraymus/workspace/GoalRunner.java'),('audit','Unchanged independent outcome verifier','workspace-engine/src/main/java/fraymus/workspace/DataSummary102.java'),('inspection','Independent source and timing analysis','scripts/Inspect-Summary104.cjs')]
system={'modules':[{'id':i,'name':n,'source':p,'note':note} for i,n,p in modules],'screenshots':[{'path':'docs/readmes/104/'+n,'caption':c} for n,c in captions.items()],'evidence':[{'path':'docs/readmes/104/'+n,'caption':n} for n in ['analysis.json','verification.json','original-evidence.json']]};js(d/'system-map.json',system)
table='\n'.join('| '+x['name']+' | '+str(x['rows'])+' | '+x['outcome']+' | '+' | '.join(f'{x[k]/1000:.3f}' for k in ['totalMs','browserMs','restartMs','otherMs'])+' |' for x in analysis['results'])
readme=f'''# Fraynix Agent Workspace — edition 104

## Calculate the new input; qualify before remembering

Edition 104 adds an **input-driven motion summary owned by Java**. The shipped browser implementation calculates all six summary values, the quality label and every chart point from fresh normalized rows and the current thresholds. Java independently verifies what the actual browser displayed. Saving the procedure additionally requires varied, linked verification examples.

The actual result is **nine completed requests**, **one refused premature retention** and **one correctly refused displayed corruption**. All eleven used the authored implementation with zero new content-generation calls. The saved procedure survived cold restart and calculated new mapped data; another request recovered from abrupt Java interruption without repeating its verified data stage.

[Previous edition 103](README-103.md) · [Interactive offline guide](docs/readmes/104/index.html) · [Complete measurements](docs/readmes/104/analysis.json) · [Validation and build identity](docs/readmes/104/verification.json) · [Contributor direction](README-CONTRIBUTORS.md)

![Held-out summary qualified and retained](docs/readmes/104/heldout37-saved-1600.png)

## What changed, and why

Edition 103 established exact implementation reuse with fresh checks. It also exposed the limit of the older model-generated summary: its six summary values were fixed constants. Changing the data changed the table and trace, but the report retained old numbers and was correctly refused. That older artifact and its failure remain unchanged in the previous edition.

This release adds a separate `input-summary-workflow-104-v1` procedure. Java loads the repository’s owned implementation and writes a new artifact with fresh reserved inputs. The embedded browser code performs the presentation calculations; the Java data engine and independent verifier calculate the expected outcomes from the original source. No new external library, remote service or content model is involved in this path. This is hand-authored application capability, not a newly trained model or a claimed autonomous code discovery.

The existing generative motion-summary procedure remains available. Chat chooses the new procedure when the request explicitly says **input-driven** motion summary. Named retained procedures still use their exact recorded family. A saved 104 implementation is copied exactly for reuse and checked against its source and current identity.

## Fresh values, defined calculations

Java first checks the raw CSV, explicit unit mapping, time order, missing-row policy and declared quality limits. It preserves the original file and produces normalized seconds/metres. The new creation input contains those exact source strings plus freshly bound gap and speed thresholds and the source’s verified skipped-row count. Expected summary answers are not supplied to the renderer.

| Display | Calculation from the current full source |
|---|---|
| Samples | Number of mapped rows |
| Duration | Last timestamp minus first timestamp |
| Total travel | Sum of absolute displacement changes between adjacent rows |
| Peak speed | Largest absolute adjacent displacement change divided by its time interval |
| Gap flags | Count of time intervals above the current gap threshold |
| Speed flags | Count of adjacent speeds above the current speed threshold |

Travel includes reversals; it is not the displacement range. Flat motion has zero travel and speed. Threshold comparisons preserve the existing strict greater-than rule. Missing rows remain declared source facts: even if the remaining intervals pass, skipped rows keep the quality warning visible. The shipped formatter follows the existing Java six-decimal display convention, including the tested half-micro boundary.

The chart retains every normalized source point in order with the same fixed coordinate contract as edition 102. It does not smooth or invent points. Filtering changes only the source table. Summary values and the full trace remain unchanged across all rows, an exact row match, no match and reset.

## Qualification before skill retention

A single passing dataset does not qualify the implementation for a reusable skill. Before saving a 104 procedure, Java requires at least four distinct, already completed 104 datasets collectively covering:

- Monotonic motion.
- Reversal.
- Flat motion.
- Both gap and speed flags.
- Explicit nonidentity units.

Java reconstructs and verifies each original source, implementation, behavior receipt and independent audit. It saves exact links to those proofs, then requires the current save request to pass independently. The actual cohort retained five prior examples, including an additional skipped-row case, before saving the 37-row held-out result.

The qualification is part of the saved procedure’s lineage. Reuse and publication recheck the linked originals. Changed source files, code, evidence or qualification identity are refused. A later saved descendant still needs valid qualification. This is bounded empirical coverage in the current project, not a guarantee for every possible dataset. A new private project does not silently inherit these trial proofs.

![Changed units and actual quality flags](docs/readmes/104/units-flags16-1600.png)

## Verification stays independent

`Creation`, `MotionData`, `DataCreation101` and `DataSummary102` retain their previous compiled bytes. The 104 wrapper routes the new procedure through the same complete source-table, summary and SVG checks. The authored producer has a separate identity that includes its Java adapter and shipped HTML/JavaScript. The goal runner checks exact code and fresh criteria throughout execution and recovery.

A successful behavior check does not bypass the independent source audit. In the deliberate corruption trial, the displayed travel value was changed after behavior tests. The independent audit rejected it, the request retained its failed evidence and Resume could not silently treat the earlier receipt as success. No failed procedure was saved.

![Independent audit refuses a corrupted displayed value](docs/readmes/104/rendered-fault-refused-1600.png)

## Actual trials and full costs

These requests ran through the real local chat, Java runner and Electron canvas in an isolated fresh project. One unchanged Delfys recording is reused as recorded data. All other sequences are explicitly synthetic arithmetic fixtures, not new camera observations. The original project, requests, artifacts, source strings and observations are in the lossless evidence archive.

Times below are seconds. Total is the full caller interval from submission to collected completion or refusal. Browser intervals are unioned to avoid double counting. Restart includes deliberate interruption and reopening, plus the explicitly labelled harness recovery below. Other includes Java computation, persistence, original evidence checks, qualification, scheduling and harness polling. Startup and gaps between requests remain in the full cohort timestamps. Skill qualification and retention are charged inside their request.

| Request | Rows | Outcome | Total s | Browser s | Restart s | Other s |
|---|---:|---|---:|---:|---:|---:|
{table}

The first harness run failed while collecting the already completed 96-row reversal request: it compared an untrimmed prompt against the application's trimmed text. The harness alone was corrected, and the same completed task was collected after reopening; neither its runtime nor its result was regenerated. Its full 142.440-second caller interval includes 101.855 seconds of developer-assisted harness recovery and the earlier failed polling. Original failure records and screenshots are retained alongside the continuation. This is distinct from the deliberate 109-row Java restart test and is not an autonomous recovery claim.

These are complete observed costs, not a matched speedup comparison with previous model-generation runs. Zero content inference here means the authored implementation did the calculation; it does not show improved model weights. Reuse still pays for source, ancestry and outcome checks.

The initial save attempt produced a correctly verified report but was refused at the retention gate because varied-data proof was missing. After the separate qualification examples, the held-out report saved `fresh-summary104`. A cold-reopened reuse changed units, origin, direction and thresholds. Another reuse handled 109 rows after an abrupt Java process interruption at the verified-data boundary. Its original data child remained unchanged.

![Recovered fresh-input reuse](docs/readmes/104/reused109-restarted-1600.png)

## Try it in the app

1. Import or select a motion CSV in Inputs, with explicit time and distance units.
2. Ask: “Analyze the selected data and create an input-driven motion summary report.” Optional gap and speed limits are read from the request.
3. Review the actual cards, quality warning, source rows and full trace. Mission identifies the Java-owned calculation and records verification.
4. Before saving, run distinct examples covering monotonic, reversal, flat, flagged and changed-unit inputs. These must complete in the same project. The current release does not automatically invent qualification data inside a user’s request.
5. Select another dataset and ask: “Analyze the selected data and create an input-driven motion summary report. Save the whole workflow as fresh-summary104.” Java rechecks the linked qualification before publication.
6. After saving, ask: “Reuse fresh-summary104 version 1 on the selected dataset.” New thresholds and data are bound afresh. Add “Review after data” for an explicit pause; Resume reconciles the saved child and input.

If retention is refused for missing coverage, Stop that request, complete the missing examples without saving, then start a fresh save request. A corrupted or incompatible result remains a refusal; the runtime does not rewrite its evidence to pass.

![Actual narrower workspace](docs/readmes/104/summary104-1100.png)

## Validation, evidence and compatibility

The focused Java suite contains {sum(int(x['tests']) for x in tests)} methods, including qualification, cold reuse, changed original evidence, forbidden public injection, independent failure and legacy behavior. All 141 desktop checks and 23 legacy/summary/diagnostic actual Chromium controls pass. Twenty altered-record controls are independently refused by the source and timing inspector. Unit host receipts are explicitly software fixtures; the eleven app requests supply actual browser observations. Screenshots were reviewed at 1600 and 1100 pixels. The refusal capture shows the actual failed task with Resume disabled; its cards are above the visible table scroll position.

The source identity comparison preserves the original verifier classes. Older 103 implementation bindings include the runner identity and remain conservatively refused if that identity changes after upgrading; they are not relabelled as 104 proof. Start a new reviewed 104 procedure rather than modifying an old checkpoint. Existing private worlds and frozen editions remain untouched.

Build with Java 21 and the checked-in Gradle wrapper, then use `Launch-Agent-Workspace.bat` with a separate review project. The actual cohort harness is `workspace-desktop/test/summary104-integration.cjs`; it refuses an existing output folder. `scripts/Inspect-Summary104.cjs` recomputes raw input mapping, source arithmetic, every displayed metric and point, qualification links and complete cost accounting. [Original evidence](docs/readmes/104/original-evidence.json) retains the original artifacts and results. Remote CI is reported separately from local checks.

## Next steps

Measure how qualification and ancestry verification costs grow with more retained procedures. Test wider numeric ranges, additional missing-data patterns and threshold boundary cases before widening the input contract. Then add an explicit workflow that prepares and runs qualification examples with clear provenance, so a user can request qualification without manually selecting each case. Each new creation family should have its own input contract and independent outcome checks.

This advances the original direction: a local agent can choose a useful procedure, calculate from current reality-linked data, observe the rendered result and retain verified ways of working across sessions. The verified capability here is reusable motion-data reporting; it is not general scientific reasoning or an unrestricted self-improvement system. One UI wording limitation remains: inherited completion text calls the output “generated”; the new producer chip identifies it as a Java summary. A missing-qualification refusal still exposes the generic Resume control, but its verification gate continues to refuse; follow the stated Stop-and-start-fresh instructions.
'''
write(r/'README-104.md',readme)
write(r/'README.md','# Fraynix Agent Workspace\n\nEdition 104 calculates fresh motion summaries and qualifies them on varied data before saving reusable procedures. Nine requests completed; premature retention and a corrupted displayed result were correctly refused.\n\n[Edition 104: functions, results and next steps](README-104.md) · [Interactive offline guide](docs/readmes/104/index.html) · [Previous edition](README-103.md) · [Contributor direction](README-CONTRIBUTORS.md)\n\nJava owns durable goals, tools, verification and memory. The browser displays actual calculated results alongside chat, Game Lab and spatial views.\n\n![Qualified input-driven summary](docs/readmes/104/heldout37-saved-1600.png)\n\nPublication receipt follows the actual main/tag push and fetched verification.\n')
note_text='''2026-11-09, America/Los_Angeles. Isolated S:/Codex-Work/fraynix-workspace-edition104. Nine actual chat workflows completed; early retention and injected displayed corruption refused. Java-owned input-driven summary: all six values, quality and chart from fresh normalized input/criteria; no expected metric answers supplied to renderer, zero content-generation calls. Not model-generated or trained. Five original varied-data proofs checked before held-out37 retention; cold mapped reuse and abrupt109-row recovery verified. Preserve original verifier class bytes, private worlds and frozen001–103. Old103 runner-bound identities are not relabelled. No delegation; heavy work serial. Read README104 and contract. Next: broader numeric cases and measured qualification cost, then explicit automatic qualification with provenance. Main/tag publication pending actual push/fetch; CI separate.
'''
write(r/note,'# Edition 104 — input-driven summaries\n\n'+note_text+'\n[[README-104.md|Full edition]] · [[docs/obsidian/111-verified-implementation-reuse.md|Prior implementation reuse]]\n')
for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
 p=r/name;old=p.read_text(encoding='utf8');header='## Edition104 — validated input-driven summaries\n\n'
 if header not in old:write(p,header+note_text+'\n'+old)
for name,header in [('docs/readmes/INDEX.md','[Edition 104 — input-driven summaries](../../README-104.md) · [Offline guide](104/index.html)'),('docs/obsidian/00-home.md','[[docs/obsidian/112-input-driven-summaries.md|Edition 104 — input-driven summaries]]')]:
 p=r/name;old=p.read_text(encoding='utf8')
 if header not in old:write(p,header+'\n\n'+old)
js(r/'docs/obsidian/112-input-driven-summaries.canvas',{'nodes':[{'id':'request','type':'file','file':'README-104.md','x':0,'y':0,'width':500,'height':320},{'id':'memory','type':'file','file':note,'x':580,'y':0,'width':500,'height':320}],'edges':[{'id':'qualification','fromNode':'request','fromSide':'right','toNode':'memory','toSide':'left','label':'fresh calculations, linked qualification, fresh outcome checks'}]})
template=(r/'scripts/summary104-guide.html').read_text(encoding='utf8');write(d/'index.html',template.replace('SYSTEM_JSON',json.dumps(system)).replace('INVENTORY_JSON',json.dumps(inventory)).replace('RESULT_JSON',json.dumps(analysis)))
print(json.dumps({'sourceFiles':len(inventory),'originalEntries':len(archive),'readmeWords':len(readme.split())}))
