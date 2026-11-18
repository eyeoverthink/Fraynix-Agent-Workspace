"""Build a linked edition from exact desktop, verification and release receipts."""
from hashlib import sha256
import json
from pathlib import Path
import shutil
import subprocess
import sys

root = Path.cwd()
previous = 'e1040566e7118c1fc691b41c050f6c5c7c18c059'
publication = root / 'docs/publication176'
study = root / 'build' / sys.argv[1]
out = root / 'docs/readmes/176'
out.mkdir(exist_ok=True)


def read(path):
    return json.loads((root / path).read_bytes())


def write(path, obj):
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + '\n', encoding='utf8')


def prepend(path, text):
    target = root / path
    content = target.read_text(encoding='utf-8-sig')
    if text.startswith('## Edition 176') and content.startswith(text.splitlines()[0] + '\n\n'):
        content = content.split('\n\n', 2)[2]
    if not content.startswith(text):
        target.write_text(text + content, encoding='utf8')


def seconds(ms):
    return f'{ms / 1000:.2f} s'


app = read(study / 'result.json')
smoke = read('build/routing176-offline-check/result.json')
tests = read('build/validation176-final/summary.json')
upload = read('build/evidence176-upload.json')
prior = read('build/routing176-prior.json')
assert app['passed'] and not app['smoke'] and smoke['passed'] and tests['passed'] and prior['passed']
assert upload['download']['passed'] and app['recovery']['modelCallsBefore'] == app['recovery']['modelCallsAfter'] == 1
assert app['declined']['status'] == 'cancelled' and not app['declined'].get('selectedLocalWorkflow')


def task(label):
    return next(row for row in app['tasks'] if row['label'] == label)


pairs = []
for identity, title in (('ramp', 'Paired ramps'), ('analysis', 'Motion summary')):
    fast, plan = task('quick ' + identity), task('model ' + identity)
    assert fast['briefing'] == plan['briefing']
    assert fast['task']['outcome'] == plan['task']['outcome'] == 'verified'
    assert fast['task']['timing052']['modelCalls'] == 0 and plan['task']['timing052']['modelCalls'] == 1
    if identity == 'ramp':
        assert fast['evidence']['samples'] == plan['evidence']['samples']
        detail = f"Every retained sample matched. The signed distance difference was {fast['evidence']['delta']:+.3f} m."
    else:
        evidence = fast['evidence']
        for key in ['travel', 'gaps', 'speeding', 'sourceSha256']:
            assert evidence[key] == plan['evidence'][key]
        detail = f"Both paths checked the same original rows and reported {evidence['travel']:.3f} m of travel."
    planned = plan['task']['timing052']['modelMillis']
    assert 0 < planned < plan['elapsedMs'] and plan['elapsedMs'] > fast['elapsedMs']
    pairs.append({'id': identity, 'title': title, 'request': fast['briefing'], 'directMs': fast['elapsedMs'],
                  'modelMs': plan['elapsedMs'], 'selectionMs': planned, 'result': detail,
                  'quickArtifact': fast['evidence']['artifact'], 'planArtifact': plan['evidence']['artifact']})

fast_apps = task('quick creation')
assert fast_apps['task']['timing052']['modelCalls'] == 0
app_checks = len(fast_apps['evidence']['checks'])
app_calls = sum(job['attempts'] for job in fast_apps['evidence']['nativeJobs'])
assert app_checks == 9 and app_calls >= 2
review, confirmed = task('review before restart'), task('confirmed after restart')
assert app['recovery']['sourceKept'] != app['recovery']['alternateSource']
assert app['recovery']['didVerify']
offline = [row for row in app['tasks'] if row['label'] == 'offline needs clarification']
assert len(offline) == 5 and all(row['task']['timing052']['modelCalls'] == 0 for row in offline)
probes = []
for identity, title, row in (
    ('compound', 'Compound request · offline', offline[0]),
    ('ambiguous', 'Undecided request · with model', task('model clarification')),
    ('missing', 'Recognized request with missing data', task('known missing dataset'))
):
    assert not row['task'].get('selectedLocalWorkflow')
    probes.append({'id': identity, 'title': title, 'request': row['briefing'], 'reason': row['task']['reason'],
                   'modelCalls': row['task']['timing052']['modelCalls'], 'elapsedMs': row['elapsedMs']})

summary = (f"Quick routing finished the matched ramp and analysis jobs in {seconds(pairs[0]['directMs'])} and "
           f"{seconds(pairs[1]['directMs'])} with zero selection calls. On the same inputs Plan took "
           f"{seconds(pairs[0]['modelMs'])} and {seconds(pairs[1]['modelMs'])}. An uncertain suggestion waited "
           "for confirmation and kept its original inputs across restart.")
captions = {
    'quick-ramp.png': 'Quick routing with a verified paired ramp report',
    'quick-analysis.png': 'Quick analysis with the original rows and quality flags',
    'quick-creation.png': 'A newly generated tally after real control checks',
    'review-pending.png': 'Suggested analysis waiting for explicit confirmation',
    'review-confirmed.png': 'Confirmed analysis after restart with its original dataset',
    'compound-clarification.png': 'Offline compound request asks for one result'
}
shots = []
for capture in app['captures']:
    name = capture['name']
    raw = study / name
    assert sha256(raw.read_bytes()).hexdigest() == capture['sha256']
    shutil.copy2(raw, out / name)
    shots.append({'id': name, 'path': 'docs/readmes/176/' + name, 'title': captions[name]})
assert len(shots) == len(captions)

table = ['| Matched job | Quick | Plan | Model selection in Plan |',
         '| --- | ---: | ---: | ---: |']
for row in pairs:
    table.append(f"| {row['title']} | {seconds(row['directMs'])} | {seconds(row['modelMs'])} | {seconds(row['selectionMs'])} |")
validation = (f"{tests['rootJava']} root Java tests, {tests['workspaceJava']} focused workspace Java tests, "
              f"{tests['desktop']} desktop tests and {tests['archiveTests']} archive tests passed.")
screens = '\n'.join(f"- [{shot['title']}]({shot['path']})" for shot in shots[1:])
readme = f"""# Fraynix Agent Workspace · Edition 176

[Edition 175](README-175.md) · [Interactive offline guide](docs/readmes/176/index.html) · [Full edition history](docs/readmes/INDEX.md)

{summary}

![Verified ramp comparison after the quick route selected the procedure](docs/readmes/176/quick-ramp.png)

## Use Quick and Review

1. Open a saved paired ramp input or select a motion dataset. To generate a standalone app, select a local model for content generation.
2. In Chat try `Route a local workflow: Compare the two prepared ramp configurations on the same clock.` or `Route a local workflow: Analyse my selected motion recording for total travel.`
3. When **Mission** offers a suggestion, review the selected procedure and saved input. Click its **Confirm** button to run it, or **Stop mission** to decline. If the app asks a question, submit a new request with the missing information.

Quick routing checks the complete briefing against a small, versioned set of forms. Requests that fall outside those forms can use an installed model to suggest one existing workflow. The suggestion is retained before a worker starts. Java holds the source, confirmation and original verifier receipts. `Plan a local workflow:` and named commands retain their Edition 175 behavior.

## Complete verified task time

{chr(10).join(table)}

Both branches used the same original inputs and independent verifiers. The ramps matched at every retained sample. The analysis kept the stated 0.12 s gap limit and 0.6 m/s speed limit and matched independently computed travel and quality flags. Times begin at Chat submission and include production checks. The model ran on CPU. Inputs, screenshots and extra study checks are outside those spans.

Quick selected a new tally app with zero routing calls. The content worker made {app_calls} local generation requests. The resulting app passed {app_checks} independent steps for adding, resetting and adding again. Its full time was {seconds(fast_apps['elapsedMs'])}. This single creation result measures the working path. The paired timing comparison covers ramp and analysis.

## When the request needs review

- The missing-data question returned in {seconds(task('known missing dataset')['elapsedMs'])} with zero selection calls. Five offline requests covering compound work, a quotation, negation, a conditional and a request to email the result asked for clarification without starting work.
- A proposed ramp comparison was declined before execution. An undecided request returned a question without proposing a child.
- A less direct data request produced a suggestion in {seconds(review['elapsedMs'])}. Ordinary Resume refused it. After an abrupt stop and a restart with models disabled, one explicit confirmation completed the verified analysis in {seconds(confirmed['elapsedMs'])}. The app used the original dataset even after a different dataset was selected. The selection call count stayed at one, and a second confirmation was refused.

The interaction count is one Chat submission for a known workflow and one additional confirmation for a suggestion. That count covers actions that authorize work. Reading, navigating and user time saved were outside this study. The offline checks exercised deliberately limited wording. Compound requests still need to be broken into supported workflows.

{validation} An Edition 175 report reopened and matched its retained receipts.

## Screenshots and original results

{screens}

[Verification summary](docs/readmes/176/verification.json) · [Original test evidence](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/releases/tag/evidence-176) · [Implementation and next gates](docs/publication176/plan.md)
"""
(root / 'README-176.md').write_text(readme, encoding='utf8')
(root / 'README.md').write_text('# Fraynix Agent Workspace\n\n'
    '[Edition 176 — quick routing and durable confirmation](README-176.md) · '
    '[Previous edition](README-175.md) · [Offline guide](docs/readmes/176/index.html) · '
    '[Full history](docs/readmes/INDEX.md)\n\n' + summary + '\n', encoding='utf8')

index = root / 'docs/readmes/INDEX.md'
old = index.read_text(encoding='utf8')
if 'README-176.md' not in old:
    index.write_text(old.replace('\n', '\n\n[Edition 176 — quick routing and durable confirmation](../../README-176.md) · '
                                 '[Guide](176/index.html)\n', 1), encoding='utf8')
note = ('## Edition 176 — quick routing and durable confirmation\n\n2026-11-18. ' + summary + ' ' + validation
        + ' A separately generated tally passed real controls, an uncertain ramp suggestion was declined, and a selected replacement '
        'dataset did not rebind the confirmed request. Edition 175 report receipts were reread. The earliest driver attempts used the '
        'wrong method to read chat state and are retained separately. See README-176.md and docs/publication176/plan.md. '
        'Next, test broader wording and independently verified compound workflows.\n\n')
for name in ('AGENTS.md', 'README-CONTRIBUTORS.md', 'docs/PROJECT_MEMORY.md', 'docs/DEVELOPMENT_LOG.md',
             'docs/ARCHITECTURE.md', 'docs/INTEGRATED_AGENT_ROADMAP.md'):
    prepend(name, note)
note_path = 'docs/obsidian/184-quick-workflow-routing.md'
prepend('docs/obsidian/00-home.md', '[Edition 176 — quick workflow routing](184-quick-workflow-routing.md)\n\n')
(root / note_path).write_text('# Quick workflow routing\n\n' + summary + '\n\n'
    '[Edition 176](../../README-176.md) · [Offline guide](../readmes/176/index.html) · '
    '[Implementation](../publication176/plan.md)\n', encoding='utf8')
java = 'workspace-engine/src/main/java/fraymus/workspace/'
modules = [
    {'id': 'selector', 'title': 'Quick workflow matching', 'source': java + 'QuickWorkflowRouting.java',
     'description': 'Matches complete briefings with a versioned set of forms. Other wording goes to review or clarification.'},
    {'id': 'admission', 'title': 'Sealed source selection', 'source': java + 'Domain044.java',
     'description': 'Freezes input identities before choosing a workflow and checks them again before running.'},
    {'id': 'durability', 'title': 'Confirmation and task journal', 'source': java + 'Agency043.java',
     'description': 'Records model proposals and waits for explicit confirmation before scheduling the worker.'}
]
for module in modules:
    module['note'] = note_path
canvas_path = 'docs/obsidian/184-quick-workflow-routing.canvas'
map_paths = ['README-176.md', *(module['source'] for module in modules), 'docs/readmes/176/verification.json',
             'docs/publication176/plan.md']
write(root / canvas_path, {
    'nodes': [{'id': str(i), 'type': 'file', 'file': name, 'x': 530 * (i % 3), 'y': 420 * (i // 3),
               'width': 475, 'height': 375} for i, name in enumerate(map_paths)],
    'edges': [{'id': f'edge{i}', 'fromNode': str(i), 'fromSide': 'right', 'toNode': str(i+1), 'toSide': 'left'}
              for i in range(len(map_paths)-1)]
})
system = {'summary': summary, 'pairs': pairs, 'probes': probes, 'modules': modules, 'screenshots': shots,
          'evidence': [{'path': 'docs/readmes/176/verification.json'}]}
write(out / 'system-map.json', system)
effort = {key: app['effort'][key] for key in ('dispatchForKnown', 'confirmForSuggestion')}
effort.update({'includesNavigation': False, 'humanTimeMeasured': False})
write(out / 'verification.json', {'passed': True, 'source': app['source'], 'model': app['model'],
      'pairedTimings': pairs, 'clarifications': probes, 'recovery': app['recovery'], 'effort': effort,
      'verifiedTasks': sum(bool(row.get('evidence')) for row in app['tasks']), 'declinedWithoutChild': True,
      'standalone': {'checks': app_checks, 'contentRequests': app_calls, 'elapsedMs': fast_apps['elapsedMs'],
                     'selectionCalls': 0}, 'offlineClarifications': len(offline),
      'priorReport': prior, 'tests': tests, 'ciFirst': read('build/ci176-first.json')})
for name, target in [('evidence176-manifest.json', 'original-evidence.json'), ('evidence176-upload.json', 'evidence-publication.json')]:
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
lock = 'docs/QUICK_ROUTING_176_LOCK.json'
write(root / lock, {'source': app['source'], 'sourceHashes': {row['path']: row['sha256'] for row in locked},
      'binaryPaths': [row['path'] for row in locked if row['hashKind'] == 'binary']})
paths = {row['path'] for row in read('docs/readmes/175/file-inventory.json')}
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
