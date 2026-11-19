"""Generate the linked edition record from measured test results."""
from hashlib import sha256
from html import escape
import json
from pathlib import Path
import shutil
import subprocess

root = Path.cwd()
study = root / 'build/relay178-live'
out = root / 'docs/readmes/178'
out.mkdir(exist_ok=True)
publication = root / 'docs/publication178'


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
tests = read('build/validation178-final/summary.json')
upload = read('build/evidence178-upload.json')
prior = read('build/relay178-prior.json')
assert app['passed'] and tests['passed'] and upload['download']['passed'] and prior['passed']
assert app['revisedDashboardRefused'] and 'saved relay report changed' in app['revisedPlaytestRefused'].lower()
assert prior['source'] == upload['source'] == app['source']
trials = []
for name in ['recovered', 'automatic']:
    trial = app[name]
    audit = trial['verified']['audit']
    assert trial['proof']['passed'] and trial['chromium']['passed'] and audit['independentReplays'] == 3
    trials.append({'course': audit['title'], 'rows': len(trial['rows']), 'minimumGameUnits': audit['minimumClearance'],
                   'checks': len(trial['chromium']['checks']), 'report': trial['dashboard'],
                   'replays': audit['independentReplays'], 'originalReport': trial['verified']['report']})
first, second = trials
assert app['archive']['passed'] and app['archive']['report'] == first['report']['id']

notes = ('Fraynix replays a Relay course at three paces, then builds a filterable clearance '
         'dashboard from the verified game measurements. One Mission review authorizes both steps. '
         'This is the second registered offline chain alongside the ramp-to-motion workflow.')
counts = (f"{tests['rootJava']} root Java tests, {tests['workspaceJava']} focused workspace Java tests, "
          f"{tests['desktop']} desktop tests and {tests['archiveTests']} archive tests passed.")
names = {
    'relay-review.png': 'Course and both procedures awaiting confirmation',
    'relay-first-verified.png': 'The original playtest report at the restart checkpoint',
    'relay-dashboard.png': 'Interactive clearance dashboard inside the desktop',
    'relay-refused.png': 'The changed source stops the chain before dashboard creation',
}
shots = []
for capture in app['captures']:
    name = capture['name']
    source = study / name
    assert sha256(source.read_bytes()).hexdigest() == capture['sha256']
    shutil.copyfile(source, out / name)
    shots.append({'path': f'docs/readmes/178/{name}', 'title': names[name]})
assert {Path(shot['path']).name for shot in shots} == names.keys()
screens = '\n'.join(f"- [{shot['title']}]({shot['path']})" for shot in shots if 'dashboard.png' not in shot['path'])
outcomes = '\n'.join(f"| {trial['course']} | {trial['replays']} | {trial['rows']} | "
                     f"{trial['minimumGameUnits']:.3f} | {trial['checks']} |"
                     for trial in trials)
readme = f"""# Fraynix Agent Workspace · Edition 178

[Edition 177](README-177.md) · [Offline walkthrough](docs/readmes/178/index.html) · [Full edition history](docs/readmes/INDEX.md)

{notes}

![The verified clearance dashboard in Fraynix](docs/readmes/178/relay-dashboard.png)

## Run the Relay chain

1. Select **Review Relay mission** in the workspace header and choose **Ridge** or **Switchyard**.
2. Choose **Prepare in Chat** and **Start task**. In Mission, check the course manifest and the two steps, then select **Confirm two-step chain**.
3. If you enabled **Pause after the verified playtest**, open the original report at the checkpoint and select **Resume mission** when ready. Mission can open both results after completion.
4. In the dashboard, filter by pace, download the crossing CSV or open its source receipt. The CSV retains game units and engine ticks. The report shows the original playtest's artifact ID and version.

The equivalent Chat request is `Review a local chain: Playtest Relay Ridge, then build a clearance dashboard.`
Use `Switchyard` for the other course and append `Pause after playtest.` for the checkpoint.

## Results

| Course | Fresh replays | Verified crossings | Minimum clearance in game units | Chromium checks |
| --- | ---: | ---: | ---: | ---: |
{outcomes}

Ridge resumed after abrupt Java termination with its six original sessions and the same report. Switchyard completed automatically from one confirmation. Both used zero model calls. A later revision of the source playtest blocked dashboard creation, and a later revision of the completed dashboard blocked proof verification.

The shared Java review logic enforces approval and a recorded handoff. The native game verifier establishes actual course contacts, rail clearances and matching replays before Java derives an export. The dashboard verifier checks the displayed values, version and filters in Chromium. History reconstructs the original evidence link before opening the dashboard. The original Edition 177 ramp chain also reopened against its exact existing receipts.

{counts} These are game simulation measurements from the two built-in courses.

## Screenshots and evidence

{screens}

[Verification summary](docs/readmes/178/verification.json) · [Download original test evidence](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/releases/tag/evidence-178) · [Implementation and next phase](docs/publication178/plan.md)
"""
(root / 'README-178.md').write_text(readme, encoding='utf8')
(root / 'README.md').write_text('# Fraynix Agent Workspace\n\n'
    '[Edition 178 — verified Relay playtest and clearance dashboard](README-178.md) · '
    '[Previous edition](README-177.md) · [Offline walkthrough](docs/readmes/178/index.html) · '
    '[Full history](docs/readmes/INDEX.md)\n\n' + notes + '\n', encoding='utf8')
index = root / 'docs/readmes/INDEX.md'
index_text = index.read_text(encoding='utf8')
if 'README-178.md' not in index_text:
    index.write_text(index_text.replace('\n', '\n\n[Edition 178 — Relay playtest and clearance dashboard](../../README-178.md) · '
        '[Walkthrough](178/index.html)\n', 1), encoding='utf8')
record = ('## Edition 178 — verified Relay playtest and clearance dashboard\n\n2026-11-18. '
          + notes + ' ' + counts + f" Ridge verified {first['rows']} crossings and Switchyard {second['rows']}. "
          'Hard Java restart preserved the original game sessions. Changed source and changed dashboard trials were refused. '
          'The archived ramp chain reopened against its Edition 177 evidence. Both successful Relay requests used zero model calls. '
          'The initial desktop harness used the wrong method for model status. A simultaneous compile also disrupted the first unit-test pass. '
          'The evidence includes both logs and the corrected runs. '
          'Read README-178.md and docs/publication178/plan.md. Next, qualify local model selection of the registered chains.\n\n')
for path in ['AGENTS.md', 'README-CONTRIBUTORS.md', 'docs/PROJECT_MEMORY.md', 'docs/DEVELOPMENT_LOG.md',
             'docs/ARCHITECTURE.md', 'docs/INTEGRATED_AGENT_ROADMAP.md']:
    prepend(path, record)
note = 'docs/obsidian/186-relay-clearance-chain.md'
prepend('docs/obsidian/00-home.md', '[Edition 178 — Relay playtest and clearance dashboard](186-relay-clearance-chain.md)\n\n')
(root / note).write_text('# Relay playtest and clearance dashboard\n\n' + notes + '\n\n'
    '[Edition 178](../../README-178.md) · [Walkthrough](../readmes/178/index.html) · '
    '[Verification and next phase](../publication178/plan.md)\n', encoding='utf8')
java = 'workspace-engine/src/main/java/fraymus/workspace/'
modules = [{'title': title, 'source': java + source, 'note': note} for title, source in [
    ('Durable review and approval', 'ReviewedChain.java'), ('Native course trials and replays', 'RelayPlaytest.java'),
    ('Typed handoff and Chromium checks', 'RelayClearanceChain.java')]]
canvas = 'docs/obsidian/186-relay-clearance-chain.canvas'
paths = [m['source'] for m in modules] + ['README-178.md']
write(canvas, {'nodes': [{'id': str(i), 'type': 'file', 'file': path, 'x': i * 380, 'y': 0, 'width': 350, 'height': 240}
                        for i, path in enumerate(paths)],
               'edges': [{'id': 'step-' + str(i), 'fromNode': str(i - 1), 'fromSide': 'right',
                          'toNode': str(i), 'toSide': 'left'} for i in range(1, len(paths))]})
verification = {'passed': True, 'source': app['source'], 'trials': trials, 'tests': tests,
                'revisedPlaytestRefused': True, 'revisedDashboardRefused': True, 'archive': app['archive'],
                'priorRampChain': prior, 'planningCalls': 0}
write(out / 'verification.json', verification)
write(out / 'original-evidence.json', read('build/evidence178-manifest.json'))
write(out / 'evidence-publication.json', upload)
system = {'summary': notes, 'modules': modules, 'screenshots': shots, 'evidence': [
    {'path': 'docs/readmes/178/verification.json', 'title': 'Verification summary'},
    {'path': 'docs/readmes/178/original-evidence.json', 'title': 'Evidence manifest'}]}
write(out / 'system-map.json', system)


def entry(path):
    data = (root / path).read_bytes()
    try:
        data = data.decode('utf-8-sig').replace('\r\n','\n').encode()
        kind = 'utf8-lf'
    except UnicodeDecodeError:
        kind = 'binary'
    return {'path': path, 'bytes': len(data), 'sha256': sha256(data).hexdigest(), 'hashKind': kind}


prior_commit = 'e1faaacfaac706a4d5b7ac0e6bc00a5ed6da6544'
changed = subprocess.check_output(['git','diff','--name-only',prior_commit,app['source']], text=True).splitlines()
assert not any(name.startswith(('assets/', 'fraynix-modules/')) or name.endswith(
    ('RampMission.java', 'RampComparison.java', 'DataSummary104.java')) for name in changed)
locked = [entry(name) for name in changed if name.startswith(('workspace-engine/', 'workspace-desktop/'))]
lock = 'docs/RELAY_CLEARANCE_CHAIN_178_LOCK.json'
write(lock, {'source': app['source'], 'sourceHashes': {row['path']: row['sha256'] for row in locked},
             'binaryPaths': [row['path'] for row in locked if row['hashKind'] == 'binary']})
paths = {row['path'] for row in read('docs/readmes/177/file-inventory.json')}
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
<meta name="viewport" content="width=device-width, initial-scale=1"><title>Fraynix Edition 178</title>
<style>body{{background:#0e1821;color:#edf4f6;font:16px/1.55 system-ui;margin:0}}
main{{max-width:1000px;margin:auto;padding:clamp(16px,5vw,40px)}}a{{color:#8ce9d5}}
figure{{margin:1.5rem 0}}img{{max-width:100%;height:auto;border-radius:.3rem}}h1{{line-height:1.12}}</style>
<main><h1>Verified Relay playtest and clearance dashboard</h1><p>{escape(notes)}</p>
<nav><a href="../../../README-178.md">Run instructions</a> · <a href="verification.json">Verification</a>
 · <a href="../177/index.html">Edition 177</a></nav>{images}</main>
{datasets}</html>
"""
(out / 'index.html').write_text(html, encoding='utf8')
print(json.dumps({'passed': True, 'screenshots': len(shots), 'inventory': len(inventory)}))
