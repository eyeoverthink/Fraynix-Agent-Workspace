"""Build release notes and the offline walkthrough from recorded outcomes."""
from hashlib import sha256
from html import escape
import json
from pathlib import Path
import shutil
from statistics import median
import subprocess

root = Path.cwd()
study = root / 'build/recommend179-live'
out = root / 'docs/readmes/179'
out.mkdir(exist_ok=True)
publication = root / 'docs/publication179'


def read(path):
    return json.loads((root / path).read_bytes())


def write(path, value):
    (root / path).write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf8')


def prepend(path, entry):
    path = root / path
    text = path.read_text(encoding='utf-8-sig')
    if not text.startswith(entry):
        path.write_text(entry + text, encoding='utf8')


app = read(study / 'result.json')
tests = read('build/validation179-final/summary.json')
upload = read('build/evidence179-upload.json')
prior = read('build/recommend179-prior.json')
history = read('build/recommend179-history.json')
assert app['passed'] and tests['passed'] and upload['download']['passed'] and prior['passed'] and history['passed']
assert prior['source'] == upload['source'] == history['source'] == app['source']
assert len(app['trials']) == 8 and len(history['opened']) == 2
trials = []
for row in app['trials']:
    provider = row['modelCall']
    assert row['approvalRequired'] and provider['returned'] and int(provider['providerEvidence']['eval_count']) > 0
    trials.append({'id': row['fixture']['id'], 'request': row['fixture']['text'], 'expected': row['fixture']['want'],
                   'suggested': row['actual'], 'matched': row['matched'], 'detail': row.get('refusal', row['firstReason']),
                   'engineDecision': row['disposition'], 'modelMillis': provider['durationMs'],
                   'verified': row.get('proof', {}).get('passed', False)})
verified = [row for row in app['trials'] if row.get('proof', {}).get('passed')]
assert len(verified) == 2
assert any(row.get('modelDisabledRestart') for row in verified)
assert app['matched'] == sum(row['matched'] for row in trials)
timings = [row['modelMillis'] / 1000 for row in trials]
timing = f"The model call took {min(timings):.1f}–{max(timings):.1f} s on this CPU-only installation (median {median(timings):.1f} s)."
notes = (f"Fraynix can recommend a reviewed Ramp or Relay workflow from ordinary language. {app['model']} "
         f"matched {app['matched']} of {len(trials)} predeclared choices. Java required approval before either chain ran "
         'and independently checked the original native and rendered results.')
counts = (f"{tests['rootJava']} root Java tests, {tests['workspaceJava']} focused workspace Java tests, "
          f"{tests['desktop']} desktop tests and {tests['archiveTests']} archive tests passed.")
names = {
    'ramp-review.png': 'Ramp recommendation before approval',
    'ramp-verified.png': 'Verified Ramp motion summary from the pinned source',
    'relay-review.png': 'Relay recommendation while a paired ramp page is also open',
    'relay-verified.png': 'Verified Relay dashboard after restart with discovery disabled',
    'ambiguous.png': 'Java asks which workflow the user wants',
}
shots = []
for capture in app['captures']:
    name = capture['name']
    source = study / name
    assert sha256(source.read_bytes()).hexdigest() == capture['sha256']
    shutil.copyfile(source, out / name)
    shots.append({'path': f'docs/readmes/179/{name}', 'title': names[name]})
assert len(shots) >= 4
screens = '\n'.join(f"- [{shot['title']}]({shot['path']})" for shot in shots if 'relay-review.png' not in shot['path'])
labels = {'ramp': 'Ramp', 'relay': 'Relay', 'clarify': 'Clarify', 'unparsed': 'Invalid response'}
outcomes = '\n'.join(f"| {trial['id']} | {labels[trial['expected']]} | {labels[trial['suggested']]} | "
                     f"{trial['engineDecision']} | {trial['modelMillis']/1000:.1f} s |" for trial in trials)
readme = f"""# Fraynix Agent Workspace · Edition 179

[Edition 178](README-178.md) · [Offline walkthrough](docs/readmes/179/index.html) · [Full edition history](docs/readmes/INDEX.md)

{notes}

![Reviewing the local model's Relay recommendation before any game action](docs/readmes/179/relay-review.png)

## Use plain language

1. Choose an installed model in Chat. For Ramp, first open a saved two-setup page through **Live browser → Ramp input**. For Relay, name **Relay Ridge** or **Relay Switchyard** in your request.
2. Describe the result you want and select **Suggest Ramp or Relay**. For example, ask “Which slope sends its block farther, and how did that block move?”
3. In **Mission**, review the suggested workflow and its bound source. Choose **Confirm two-step chain** to execute or **Stop mission** to decline. If the task asks for a missing input or a clearer goal, submit a new recommendation.
4. Use **Open motion summary** or **Open clearance dashboard** after verification. History can reopen the original checked artifact later.

## What the model decided

| Trial | Expected | Suggested | Java disposition | Model time |
| --- | --- | --- | --- | ---: |
{outcomes}

{timing} These eight requests test this model and configuration. Baseline direct commands remain available without inference.

The confirmed Ramp chain retained the original browser source even after another valid ramp page was opened. The confirmed Relay chain retained the suggestion and source after a hard Java stop, then finished after explicit approval with model discovery disabled. Both newly archived results reopened and verified, and the original Edition 178 Relay archive matched its existing receipts.

## What Java owns

The local model has one typed recommendation function. Java checks prerequisites, fixes the reviewed plan and records approval before creating a child. It then applies the existing source, native replay, report and Chromium checks. Mission can show a suggestion and a clarification without claiming either workflow ran.

{counts} The pilot's first harness read the wrong field in the provider receipt. Its log and original response are included alongside the corrected study.

## Screenshots and evidence

{screens}

[Verification and case text](docs/readmes/179/verification.json) · [Original test evidence](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/releases/tag/evidence-179) · [Implementation and next phase](docs/publication179/plan.md)
"""
(root / 'README-179.md').write_text(readme, encoding='utf8')
(root / 'README.md').write_text('# Fraynix Agent Workspace\n\n'
    '[Edition 179 — recommendations for reviewed workflows](README-179.md) · '
    '[Previous edition](README-178.md) · [Offline walkthrough](docs/readmes/179/index.html) · '
    '[Full history](docs/readmes/INDEX.md)\n\n' + notes + '\n', encoding='utf8')
index = root / 'docs/readmes/INDEX.md'
index_text = index.read_text(encoding='utf8')
if 'README-179.md' not in index_text:
    index.write_text(index_text.replace('\n', '\n\n[Edition 179 — local Ramp and Relay recommendations](../../README-179.md) · '
        '[Walkthrough](179/index.html)\n', 1), encoding='utf8')
record = ('## Edition 179 — local model recommends reviewed Ramp and Relay workflows\n\n2026-11-18. '
          + notes + ' ' + timing + ' ' + counts
          + ' Ramp preserved its saved input after browser selection changed. Relay completed from the retained recommendation '
            'after restarting with model discovery disabled. History reopened both new outcomes and the exact previous Relay proof. '
            'The first harness read the wrong provider metadata field. The original receipt and corrected run are both retained. '
            'Read README-179.md and docs/publication179/plan.md. Next, compare smaller local models on a broader fixed test bank.\n\n')
for path in ['AGENTS.md', 'README-CONTRIBUTORS.md', 'docs/PROJECT_MEMORY.md', 'docs/DEVELOPMENT_LOG.md',
             'docs/ARCHITECTURE.md', 'docs/INTEGRATED_AGENT_ROADMAP.md']:
    prepend(path, record)
note = 'docs/obsidian/187-local-chain-recommendation.md'
prepend('docs/obsidian/00-home.md', '[Edition 179 — local Ramp and Relay recommendations](187-local-chain-recommendation.md)\n\n')
(root / note).write_text('# Local recommendations for reviewed workflows\n\n' + notes + '\n\n'
    '[Edition 179](../../README-179.md) · [Walkthrough](../readmes/179/index.html) · '
    '[Implementation and next phase](../publication179/plan.md)\n', encoding='utf8')
java = 'workspace-engine/src/main/java/fraymus/workspace/'
modules = [{'title': title, 'source': java + source, 'note': note} for title, source in [
    ('Typed model recommendation', 'ChainRecommendation.java'),
    ('Review, approval and restart', 'Agency043.java'),
    ('Java handoff verification', 'ReviewedChain.java')]]
canvas = 'docs/obsidian/187-local-chain-recommendation.canvas'
paths = [m['source'] for m in modules] + ['README-179.md']
write(canvas, {'nodes': [{'id': str(i), 'type': 'file', 'file': path, 'x': i * 380, 'y': 0, 'width': 350, 'height': 240}
                        for i, path in enumerate(paths)],
               'edges': [{'id': 'step-' + str(i), 'fromNode': str(i - 1), 'fromSide': 'right',
                          'toNode': str(i), 'toSide': 'left'} for i in range(1, len(paths))]})
verification = {'passed': True, 'source': app['source'], 'model': app['model'], 'trials': trials, 'tests': tests,
                'matched': app['matched'], 'noWorkBeforeApproval': True, 'verifiedCompletions': len(verified),
                'providerCalls': len(trials), 'history': history, 'priorRelayChain': prior}
write(out / 'verification.json', verification)
write(out / 'original-evidence.json', read('build/evidence179-manifest.json'))
write(out / 'evidence-publication.json', upload)
system = {'summary': notes, 'modules': modules, 'screenshots': shots, 'evidence': [
    {'path': 'docs/readmes/179/verification.json', 'title': 'Verification and case text'},
    {'path': 'docs/readmes/179/original-evidence.json', 'title': 'Evidence manifest'}]}
write(out / 'system-map.json', system)


def entry(path):
    data = (root / path).read_bytes()
    try:
        data = data.decode('utf-8-sig').replace('\r\n','\n').encode()
        kind = 'utf8-lf'
    except UnicodeDecodeError:
        kind = 'binary'
    return {'path': path, 'bytes': len(data), 'sha256': sha256(data).hexdigest(), 'hashKind': kind}


prior_commit = '6bc67a1b41fed0cbb0477cccdb27a7cf7131fb41'
changed = subprocess.check_output(['git','diff','--name-only',prior_commit,app['source']], text=True).splitlines()
assert not any(name.startswith(('assets/', 'fraynix-modules/')) or name.endswith(
    ('RampMission.java', 'RampComparison.java', 'DataSummary104.java', 'RelayPlaytest.java',
     'RelayClearanceChain.java', 'RampTraceChain.java')) for name in changed)
locked = [entry(name) for name in changed if name.startswith(('workspace-engine/', 'workspace-desktop/'))]
lock = 'docs/CHAIN_RECOMMENDATION_179_LOCK.json'
write(lock, {'source': app['source'], 'sourceHashes': {row['path']: row['sha256'] for row in locked},
             'binaryPaths': [row['path'] for row in locked if row['hashKind'] == 'binary']})
paths = {row['path'] for row in read('docs/readmes/178/file-inventory.json')}
paths.update(changed)
paths.update(file.relative_to(root).as_posix() for file in publication.iterdir() if file.is_file())
paths.update([lock, note, canvas])
inventory = [entry(name) for name in sorted(paths)]
write(out / 'file-inventory.json', inventory)
images = '\n'.join(f'<figure><img src="{Path(row["path"]).name}" alt="{escape(row["title"])}">'
                   f'<figcaption>{escape(row["title"])}</figcaption></figure>' for row in shots)
datasets = '\n'.join(f'<script id="{key}-data" type="application/json">'
                     + json.dumps(value, ensure_ascii=True).replace('</', '<\\/') + '</script>'
                     for key, value in [('system', system), ('inventory', inventory)])
html = f"""<!doctype html><html lang="en"><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1"><title>Fraynix Edition 179</title>
<style>body{{background:#0e1821;color:#edf4f6;font:16px/1.55 system-ui;margin:0}}
main{{max-width:1000px;margin:auto;padding:clamp(16px,5vw,40px)}}a{{color:#8ce9d5}}
figure{{margin:1.5rem 0}}img{{max-width:100%;height:auto;border-radius:.3rem}}h1{{line-height:1.12}}</style>
<main><h1>Choose a workflow with a local model</h1><p>{escape(notes)}</p><p>{escape(timing)}</p>
<nav><a href="../../../README-179.md">Run instructions</a> · <a href="verification.json">Verification</a>
 · <a href="../178/index.html">Edition 178</a></nav>{images}</main>
{datasets}</html>
"""
(out / 'index.html').write_text(html, encoding='utf8')
print(json.dumps({'passed': True, 'screenshots': len(shots), 'inventory': len(inventory)}))
