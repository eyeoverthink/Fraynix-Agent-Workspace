"""Build the linked edition and its offline guide from original study receipts."""
from hashlib import sha256
import json
from pathlib import Path
import shutil
import subprocess
import sys

root = Path.cwd()
previous = 'd2a77dfa55a0216f225c244397f447f5000c542d'
publication = root / 'docs/publication175'
study = root / 'build' / sys.argv[1]
out = root / 'docs/readmes/175'
out.mkdir(exist_ok=True)


def read(path):
    return json.loads((root / path).read_bytes())


def write(path, obj):
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + '\n', encoding='utf8')


def prepend(path, text):
    target = root / path
    content = target.read_text(encoding='utf-8-sig')
    if text.startswith('## Edition 175') and content.startswith(text.splitlines()[0] + '\n\n'):
        content = content.split('\n\n', 2)[2]
    if not content.startswith(text):
        target.write_text(text + content, encoding='utf8')


def seconds(ms):
    return f'{ms / 1000:.2f} s'


app = read(study / 'result.json')
tests = read('build/validation175-final/summary.json')
upload = read('build/evidence175-upload.json')
prior_report = read('build/routing175-prior.json')
assert app['passed'] and tests['passed'] and upload['download']['passed'] and prior_report['passed']
assert len(app['cases']) == 10 and all(row['verified'] for row in app['cases'])
assert app['recovery']['pending']['timing052']['modelCalls'] == app['recovery']['final']['timing052']['modelCalls'] == 1

titles = {
    'ramp-a': 'Paired ramps A', 'ramp-b': 'Paired ramps B', 'data-a': 'Delfys recording',
    'data-b': 'Synthetic reversal', 'creation': 'Workshop tally'
}
pairs = []
for identity, title in titles.items():
    direct, model = (next(row for row in app['cases'] if row['caseId'] == identity and row['mode'] == mode)
                     for mode in ('direct', 'model'))
    assert direct['expected'] == model['expected'] == model['proposal']
    evidence = model['evidence']
    if model['kind'] == 'ramp':
        assert direct['evidence']['samples'] == evidence['samples']
        detail = f"Both checked comparisons matched every retained sample. Second minus first was {evidence['delta']:+.3f} m."
    elif model['kind'] == 'analysis':
        assert direct['evidence']['travel'] == evidence['travel']
        gaps, speeds = evidence['gaps'], evidence['speeds']
        detail = (f"Both checked reports agreed over {evidence['rowCount']} rows. Travel was {evidence['travel']:.3f} m. "
                  f"The checks found {gaps} gap {'flag' if gaps == 1 else 'flags'} and "
                  f"{speeds} speed {'flag' if speeds == 1 else 'flags'}.")
    else:
        assert direct['evidence']['artifact']['id'] != evidence['artifact']['id']
        assert all(row['passed'] for row in evidence['behavior'])
        direct_calls = sum(job['attempts'] for job in direct['evidence']['generated'])
        model_calls = sum(job['attempts'] for job in evidence['generated'])
        detail = (f"Two separately generated apps passed the same {len(evidence['behavior'])} independent steps. "
                  f"The direct run used {direct_calls} creation requests and the model-routed "
                  f"run used {model_calls}, including any creation retries. Route selection is counted separately.")
    planned = model['task']['timing052']['modelMillis']
    assert 0 < planned < model['elapsedMs']
    pairs.append({'id': identity, 'title': title, 'request': model['goal'], 'directMs': direct['elapsedMs'],
                  'modelMs': model['elapsedMs'], 'selectionMs': planned, 'result': detail,
                  'procedure': model['expected'], 'directArtifact': direct['evidence']['artifact'],
                  'modelArtifact': evidence['artifact']})

probes = []
for row in app['probes']:
    task = row['task']
    assert task['status'] == 'interrupted' and not task.get('selectedLocalWorkflow')
    probes.append({'id': row['label'].replace(' ', '-'), 'title': row['label'].capitalize(),
                   'request': row['text'], 'reason': task['reason'], 'modelCalls': task['timing052']['modelCalls'],
                   'proposal': row.get('proposal'), 'elapsedMs': row['elapsedMs']})
assert len(probes) == 3

min_select, max_select = (fn(row['selectionMs'] for row in pairs) for fn in (min, max))
model = app['model']
physics_data = pairs[:4]
if all(row['modelMs'] > row['directMs'] for row in physics_data):
    conclusion = "The named command was faster in all four physics and analysis pairs."
else:
    conclusion = "The individual timing pairs show where explicit dispatch reduced complete task time."
summary = (f"Both routes produced verified results in five paired trials. {model} chose the correct procedure in every pair. "
           f"Model selection alone took {seconds(min_select)} to {seconds(max_select)} on this CPU-only setup. {conclusion}")

captions = {
    'routing-ramp-a.png': 'Model-selected paired ramp report', 'routing-ramp-b.png': 'Second paired ramp report',
    'routing-data-a.png': 'Verified report from the Delfys recording', 'routing-data-b.png': 'Synthetic reversal and flagged intervals',
    'routing-creation.png': 'Generated Workshop tally after live checks', 'routing-clarification.png': 'Clarification in Mission',
    'routing-restart.png': 'Saved motion-analysis selection before process stop'
}
shots = []
for capture in app['captures']:
    name = capture['name']
    raw = study / name
    assert sha256(raw.read_bytes()).hexdigest() == capture['sha256']
    shutil.copy2(raw, out / name)
    shots.append({'id': name, 'path': 'docs/readmes/175/' + name, 'title': captions[name]})
assert len(shots) == len(captions)

table = ['| Case | Named command | Model-selected | Selection alone |',
         '| --- | ---: | ---: | ---: |']
for row in pairs:
    table.append(f"| {row['title']} | {seconds(row['directMs'])} | {seconds(row['modelMs'])} | {seconds(row['selectionMs'])} |")
details = '\n'.join(f"- **{row['title']}.** {row['result']}" for row in pairs)
probe_times = {row['id']: seconds(row['elapsedMs']) for row in probes}
validation = (f"{tests['rootJava']} root Java tests, {tests['workspaceJava']} focused workspace Java tests, "
              f"{tests['desktop']} desktop tests and {tests['archiveTests']} archive tests passed.")
screens = '\n'.join(f"- [{shot['title']}]({shot['path']})" for shot in shots[1:])
readme = f"""# Fraynix Agent Workspace · Edition 175

[Edition 174](README-174.md) · [Interactive offline guide](docs/readmes/175/index.html) · [Full edition history](docs/readmes/INDEX.md)

{summary}

![Verified ramp comparison selected in the actual Fraynix desktop](docs/readmes/175/routing-ramp-a.png)

## Choose how to start

1. Open **Chat**. For physics, open a saved two-setup **Ramp input** in the browser. For analysis, select a motion dataset in **Data tasks**. For a standalone app, select an installed local model to generate the content.
2. Use `Plan a local workflow: <request>` to have the model choose between the three procedures. Use `Run local workflow ramp: <request>`, `Run local workflow analysis: <request>`, or `Run local workflow creation: <request>` to name one directly.
3. Open **Mission** to see the selected procedure, model-selection time, progress and saved result. Supply the missing information in a new chat request when Mission asks for clarification.

The planner returns a procedure identifier or asks a question. Java seals the briefing and source identities before asking the model. Physics reuses the verified paired comparison. Analysis uses the existing Java-owned input-driven summary. Standalone creation uses the existing worker to agree on observable checks before generating an app. Java retains the choice and keeps the original native, source and canvas checks in charge of completion. The selector handles one of these three outcomes per request.

## What the model added

{chr(10).join(table)}

The desktop ran both routes for each request with matched inputs and alternate ordering for the two physics and two analysis pairs. Time starts at Chat submission and ends when Java records a terminal outcome. Production checks are included. Preparing the inputs, taking screenshots and repeating independent study checks are outside the measured interval. The creation pair generated separate drafts, so its total-to-total change includes generation variability. The selection span is measured independently.

{details}

The ambiguous request returned a clarification in {probe_times['ambiguous']}. A missing source took {probe_times['missing-direct']} through the named analysis command and {probe_times['missing-model']} through selection. Both stopped before a child started. A separate model-selected analysis resumed after an abrupt Java stop with model discovery disabled. The same source and selection remained bound and the model-call count stayed at one through the verified result. An Edition 174 report reopened against the same original receipts.

## Decision

Keep named commands for known tasks. Model selection showed that it can interpret several ways of asking and tell a comparison, an analysis and a standalone app apart. On this machine that convenience cost one full local inference. This small study measures latency and completed outcomes on the supplied tasks. The user's time saved, broader routing accuracy and timing on other hardware remain open.

{validation}

## Desktop screenshots and evidence

{screens}

[Verification summary](docs/readmes/175/verification.json) · [Original test evidence](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/releases/tag/evidence-175) · [Implementation and next gates](docs/publication175/plan.md)
"""
(root / 'README-175.md').write_text(readme, encoding='utf8')
(root / 'README.md').write_text('# Fraynix Agent Workspace\n\n'
    '[Edition 175 — local selection across physics, data and creation](README-175.md) · '
    '[Previous edition](README-174.md) · [Offline guide](docs/readmes/175/index.html) · '
    '[Full history](docs/readmes/INDEX.md)\n\n' + summary + '\n', encoding='utf8')

index = root / 'docs/readmes/INDEX.md'
old = index.read_text(encoding='utf8')
if 'README-175.md' not in old:
    index.write_text(old.replace('\n', '\n\n[Edition 175 — local workflow selection costs](../../README-175.md) · '
                                 '[Guide](175/index.html)\n', 1), encoding='utf8')
note = ('## Edition 175 — cost of local workflow selection\n\n2026-11-18. ' + summary + ' ' + validation
        + ' Named and selected workflows matched their source and verification gates. Standalone generation used the same '
        'creation worker for both routes. A stopped model-selected analysis resumed with the original choice while discovery '
        'was disabled. The first pilot caught a Mission projection issue on a missing-source request. The original pilot is '
        'retained in the evidence bundle. The next pilots caught stale desktop ownership during screenshot collection, a missing '
        'in-process dataset read route and an empty executor label for the authored summary. The final driver records completion '
        'before waiting for desktop readiness. Both named analysis fixtures passed before the paired run. '
        'A method mismatch in the restart driver halted its first model-disabled reopen before execution. The recovery check '
        'resumed that same saved task from the original stop and retained the initial driver output. '
        'See README-175.md and docs/publication175/plan.md. Next, measure user effort and '
        'qualification over more outcomes before widening automatic selection.\n\n')
for name in ('AGENTS.md', 'README-CONTRIBUTORS.md', 'docs/PROJECT_MEMORY.md', 'docs/DEVELOPMENT_LOG.md',
             'docs/ARCHITECTURE.md', 'docs/INTEGRATED_AGENT_ROADMAP.md'):
    prepend(name, note)

note_path = 'docs/obsidian/183-local-workflow-selection.md'
prepend('docs/obsidian/00-home.md', '[Edition 175 — local workflow selection](183-local-workflow-selection.md)\n\n')
(root / note_path).write_text('# Local workflow selection\n\n' + summary + '\n\n'
    '[Edition 175](../../README-175.md) · [Offline guide](../readmes/175/index.html) · '
    '[Implementation](../publication175/plan.md)\n', encoding='utf8')
java = 'workspace-engine/src/main/java/fraymus/workspace/'
modules = [
    {'id': 'selector', 'title': 'Typed local selection', 'source': java + 'LocalWorkflowSelection.java',
     'description': 'Lists the allowed routes and checks the retained choice against the original request.'},
    {'id': 'admission', 'title': 'Java admission and execution', 'source': java + 'Domain044.java',
     'description': 'Freezes input identities and hands the chosen procedure to the existing runner.'},
    {'id': 'durability', 'title': 'Durable task journal', 'source': java + 'Agency043.java',
     'description': 'Saves the choice before execution and restores it after a stop.'}
]
for module in modules:
    module['note'] = note_path
canvas_path = 'docs/obsidian/183-local-workflow-selection.canvas'
map_paths = ['README-175.md', *(module['source'] for module in modules), 'docs/readmes/175/verification.json',
             'docs/publication175/plan.md']
write(root / canvas_path, {
    'nodes': [{'id': str(i), 'type': 'file', 'file': name, 'x': 530 * (i % 3), 'y': 420 * (i // 3),
               'width': 475, 'height': 375} for i, name in enumerate(map_paths)],
    'edges': [{'id': f'edge{i}', 'fromNode': str(i), 'fromSide': 'right', 'toNode': str(i+1), 'toSide': 'left'}
              for i in range(len(map_paths)-1)]
})
system = {'summary': summary, 'pairs': pairs, 'probes': probes, 'modules': modules, 'screenshots': shots,
          'evidence': [{'path': 'docs/readmes/175/verification.json'}]}
write(out / 'system-map.json', system)
write(out / 'verification.json', {'passed': True, 'source': app['source'], 'model': model,
      'correctModelSelections': len(pairs), 'verifiedTasks': len(app['cases']), 'pairedTimings': pairs,
      'clarifications': probes, 'recovery': {'checked': True, 'modelCallsBefore': 1, 'modelCallsAfter': 1,
      'source': app['recovery']['final']['localWorkflow']['dataset']}, 'priorReport': prior_report,
      'tests': tests, 'ciFirst': read('build/ci175-first.json')})
for name, target in [('evidence175-manifest.json', 'original-evidence.json'), ('evidence175-upload.json', 'evidence-publication.json')]:
    shutil.copy2(root / 'build' / name, out / target)


def entry(name):
    raw = (root / name).read_bytes()
    try:
        raw = raw.decode('utf-8-sig').replace('\r\n', '\n').encode()
        kind = 'utf8-lf'
    except UnicodeDecodeError:
        kind = 'binary'
    return {'path': name, 'bytes': len(raw), 'sha256': sha256(raw).hexdigest(), 'hashKind': kind}


changed = subprocess.check_output(['git', 'diff', '--name-only', previous, app['source']], text=True).splitlines()
assert not any(name.endswith(('RampComparison.java', 'RampMission.java', 'BulletRamp.java',
                              'DataSummary104.java', 'CreationPlanner044.java', 'WorkflowRunner.java'))
               or name.startswith(('src/main/', 'assets/')) for name in changed)
locked = [entry(name) for name in changed if name.startswith(('workspace-engine/', 'workspace-desktop/'))]
lock = 'docs/LOCAL_WORKFLOW_175_LOCK.json'
write(root / lock, {'source': app['source'], 'sourceHashes': {row['path']: row['sha256'] for row in locked},
      'binaryPaths': [row['path'] for row in locked if row['hashKind'] == 'binary']})
paths = {row['path'] for row in read('docs/readmes/174/file-inventory.json')}
paths.update(changed)
paths.update(file.relative_to(root).as_posix() for file in publication.iterdir() if file.is_file())
paths.update([lock, note_path, canvas_path])
inventory = [entry(name) for name in sorted(paths)]
write(out / 'file-inventory.json', inventory)
html = (publication / 'guide-template.html').read_text(encoding='utf8')
for key, value in [('SYSTEM', system), ('INVENTORY', inventory)]:
    html = html.replace('@@' + key + '@@', json.dumps(value).replace('</', '<\\/'))
(out / 'index.html').write_text(html, encoding='utf8')
shutil.copy2(publication / 'guide.js', out / 'guide.js')
print(json.dumps({'passed': True, 'pairedCases': len(pairs), 'screenshots': len(shots), 'inventory': len(inventory)}))
