"""Write the linked README and offline trace guide from the verified desktop run."""
from hashlib import sha256
import json
from pathlib import Path
import shutil
import subprocess

root = Path.cwd()
prior_commit = '98c7af42ffddd543539f7e86dba8cf7389c9e0da'
publication = root / 'docs/publication177'
study = root / 'build/chain177-live'
out = root / 'docs/readmes/177'
out.mkdir(exist_ok=True)


def read(path):
    return json.loads((root / path).read_bytes())


def write(path, value):
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + '\n', encoding='utf8')


def prepend(path, text):
    target = root / path
    content = target.read_text(encoding='utf-8-sig')
    if text.startswith('## Edition 177') and content.startswith(text.splitlines()[0] + '\n\n'):
        content = content.split('\n\n', 2)[2]
    if not content.startswith(text):
        target.write_text(text + content, encoding='utf8')


app = read(study / 'result.json')
tests = read('build/validation177-final/summary.json')
upload = read('build/evidence177-upload.json')
previous = read('build/chain177-prior.json')
assert app['passed'] and tests['passed'] and upload['download']['passed'] and previous['passed']
assert app['archived']['passed'] and app['archived']['handoffSha256'] == app['recovered']['proof']['handoffSha256']
assert 'Saved ramp report changed' in app['refused']['reason'] and 'comparison is a tie' in app['tie']['reason']
assert 'ownedWorkflowId' not in app['boundary']['task']['context044']

trials = []
for name, title in [('recovered', 'Stopped and resumed after the comparison'),
                    ('automatic', 'Single confirmation with reversed input rows')]:
    trial = app[name]
    assert trial['done']['timing052']['modelCalls'] == 0
    assert trial['proof']['passed'] and trial['workflow']['audit']['passed']
    handoff = trial['done']['completion044']['chainHandoff']
    audit = trial['done']['completion044']['rampMission']['audit']
    trials.append({'id': name, 'title': title, 'rows': trial['rows'], 'winner': handoff['winner'],
                   'traceSha256': handoff['traceSha256'], 'travelM': trial['travel'],
                   'comparison': handoff['report'], 'report': trial['workflow']['report'],
                   'firstM': audit['setups']['first']['distanceM'], 'secondM': audit['setups']['second']['distanceM'],
                   'detail': f"The {handoff['winner']} setup supplied all {handoff['traceRows']} original samples. "
                             f"The summary independently measured {trial['travel']:.3f} m of travel."})
first, second = trials
assert first['traceSha256'] == second['traceSha256'] and first['rows'] == second['rows']
assert first['winner'] == 'second' and second['winner'] == 'first'
rows = len(first['rows'])
summary = (f"One confirmation starts two verified procedures. Fraynix compares the paired ramps, "
           f"then passes all {rows} original samples from the farther setup into the motion summary. "
           f"The test carried {first['travelM']:.3f} m of simulated travel through a full Java restart and retained both checks.")
checks = (f"{tests['rootJava']} root Java tests, {tests['workspaceJava']} focused workspace Java tests, "
          f"{tests['desktop']} desktop tests and {tests['archiveTests']} archive tests passed.")

titles = {'chain-review.png': 'Review both steps before authorizing work',
          'chain-first-verified.png': 'First report verified before step two exists',
          'chain-summary.png': 'Verified summary after resuming the linked trace',
          'chain-refused.png': 'An edited first report blocks the handoff'}
screenshots = []
for capture in app['captures']:
    name = capture['name']
    png = study / name
    assert sha256(png.read_bytes()).hexdigest() == capture['sha256']
    shutil.copy2(png, out / name)
    screenshots.append({'id': name, 'path': 'docs/readmes/177/' + name, 'title': titles[name]})
assert len(screenshots) == len(titles)
screens = '\n'.join(f"- [{shot['title']}]({shot['path']})" for shot in screenshots if shot['id'] != 'chain-summary.png')
readme = f"""# Fraynix Agent Workspace · Edition 177

[Edition 176](README-176.md) · [Interactive offline trace guide](docs/readmes/177/index.html) · [Full edition history](docs/readmes/INDEX.md)

{summary}

![The second verified result in the Fraynix desktop](docs/readmes/177/chain-summary.png)

## Run the chain

1. Open **Live browser → Ramp input** and import a two-row CSV. Use the same duration for both setups.
2. In **Report**, choose **Compare, then summarize the farther setup**. To inspect the first result before continuing, select **Pause after the verified comparison**.
3. Choose **Save page and prepare chat** or **Use saved page**, then **Start task**. In **Mission**, select **Confirm two-step chain**. Confirming authorizes the comparison followed by the motion summary.
4. If you requested a pause, review the verified comparison and select **Resume mission**. When the chain completes, open either report from Mission.

The matching Chat request is `Review a local chain: Compare both ramp setups, then analyze the farther setup's motion.` Add `Pause after comparison.` for the checkpoint.

## Verification on both sides of the handoff

The first procedure checks the pinned browser input, separate Bullet replays and the actual canvas report. Java saves the report identity, winning native trace hash and exact derived CSV in the parent task journal. It rereads the original comparison before it creates the dataset and starts the summary. Stable dataset and workflow IDs let an interrupted write reopen under the same identity.

The second procedure takes elapsed seconds and signed downhill displacement in metres from the original simulated samples. It runs the existing motion summary, independently checks the displayed values and retains its own report and audit. The parent verifies both reports and their shared trace again at completion and in History.

## Observed outcomes

| Trial | First verified result | What happened next |
| --- | --- | --- |
| Forced restart | First setup {first['firstM']:.3f} m, second {first['secondM']:.3f} m | The second setup supplied {rows} rows. Java resumed the original trace and verified {first['travelM']:.3f} m of travel. |
| Automatic handoff | The same setups were supplied in reverse order. | One confirmation carried the first setup's trace into the summary. Its CSV matched the restarted trial byte for byte. |
| Changed first report | A later report revision was deliberately introduced after the checkpoint. | Java refused the handoff before creating a dataset or starting the summary. |
| Tie | The verified comparison found equal travel within its existing replay tolerance. | Java asked for setups with different travel. No summary started. |

The restart trial kept its first native sessions and saved report. After archiving and reopening, History reverified both receipts and the original link. Both successful chains used zero model calls. The fixture measured simulated motion in one registered two-step chain.

{checks} The Edition 176 report also reopened against its original receipts.

## Screenshots and original evidence

{screens}

[Verification summary](docs/readmes/177/verification.json) · [Original test evidence](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/releases/tag/evidence-177) · [Implementation and next gates](docs/publication177/plan.md)
"""
(root / 'README-177.md').write_text(readme, encoding='utf8')
(root / 'README.md').write_text('# Fraynix Agent Workspace\n\n'
    '[Edition 177 — reviewed ramp-to-analysis chain](README-177.md) · '
    '[Previous edition](README-176.md) · [Offline guide](docs/readmes/177/index.html) · '
    '[Full history](docs/readmes/INDEX.md)\n\n' + summary + '\n', encoding='utf8')
index = root / 'docs/readmes/INDEX.md'
old = index.read_text(encoding='utf8')
if 'README-177.md' not in old:
    index.write_text(old.replace('\n', '\n\n[Edition 177 — reviewed verified handoff](../../README-177.md) · '
                                 '[Guide](177/index.html)\n', 1), encoding='utf8')
note = ('## Edition 177 — reviewed ramp-to-analysis chain\n\n2026-11-18. ' + summary + ' ' + checks
        + ' Java refused a changed comparison report and a tie before creating the downstream dataset. Reversing setup order '
          'kept the same winning trace bytes. Archive verification rereads both original receipts and the exact handoff. '
          'Read README-177.md and docs/publication177/plan.md. Scope is the registered ramp-to-motion chain. Next, qualify a second typed handoff.\n\n')
for name in ('AGENTS.md', 'README-CONTRIBUTORS.md', 'docs/PROJECT_MEMORY.md', 'docs/DEVELOPMENT_LOG.md',
             'docs/ARCHITECTURE.md', 'docs/INTEGRATED_AGENT_ROADMAP.md'):
    prepend(name, note)
note_path = 'docs/obsidian/185-ramp-trace-chain.md'
prepend('docs/obsidian/00-home.md', '[Edition 177 — reviewed ramp-to-analysis chain](185-ramp-trace-chain.md)\n\n')
(root / note_path).write_text('# Reviewed ramp-to-analysis chain\n\n' + summary + '\n\n'
    '[Edition 177](../../README-177.md) · [Offline trace guide](../readmes/177/index.html) · '
    '[Implementation](../publication177/plan.md)\n', encoding='utf8')
java = 'workspace-engine/src/main/java/fraymus/workspace/'
modules = [
    {'id': 'handoff', 'title': 'Verified trace handoff', 'source': java + 'RampTraceChain.java',
     'description': 'Checks that the original comparison belongs to the task and derives the winning simulation trace.'},
    {'id': 'journal', 'title': 'Review and restart', 'source': java + 'Agency043.java',
     'description': 'Retains confirmation and the first verified receipt before scheduling the next step.'},
    {'id': 'summary', 'title': 'Input-driven summary', 'source': java + 'DataSummary104.java',
     'description': 'Runs the original calculation and independent rendered-outcome contract on the derived CSV.'}
]
for module in modules:
    module['note'] = note_path
canvas_path = 'docs/obsidian/185-ramp-trace-chain.canvas'
map_paths = ['README-177.md', *(module['source'] for module in modules), 'docs/readmes/177/verification.json',
             'docs/publication177/plan.md']
write(root / canvas_path, {
    'nodes': [{'id': str(i), 'type': 'file', 'file': name, 'x': 530 * (i % 3), 'y': 420 * (i // 3),
               'width': 475, 'height': 375} for i, name in enumerate(map_paths)],
    'edges': [{'id': f'edge{i}', 'fromNode': str(i), 'fromSide': 'right', 'toNode': str(i+1), 'toSide': 'left'}
              for i in range(len(map_paths)-1)]
})
probes = [{'id': 'edited', 'title': 'Edited comparison report', 'reason': app['refused']['reason']},
          {'id': 'tie', 'title': 'Exact tie', 'reason': app['tie']['reason']}]
system = {'summary': summary, 'trials': trials, 'probes': probes, 'modules': modules, 'screenshots': screenshots,
          'evidence': [{'path': 'docs/readmes/177/verification.json'}]}
write(out / 'system-map.json', system)
write(out / 'verification.json', {'passed': True, 'source': app['source'], 'verifiedTasks': 2,
      'recovery': {'modelCalls': 0, 'report': first['report'], 'traceSha256': first['traceSha256'],
                   'independentlyArchived': app['archived']['passed']}, 'trials': trials,
      'changedComparisonRefused': True, 'tieRefused': True, 'priorReport': previous,
      'tests': tests, 'ciFirst': read('build/ci177-first.json')})
for name, target in [('evidence177-manifest.json', 'original-evidence.json'), ('evidence177-upload.json', 'evidence-publication.json')]:
    shutil.copy2(root / 'build' / name, out / target)


def entry(name):
    raw = (root / name).read_bytes()
    try:
        raw = raw.decode('utf-8-sig').replace('\r\n', '\n').encode()
        kind = 'utf8-lf'
    except UnicodeDecodeError:
        kind = 'binary'
    return {'path': name, 'bytes': len(raw), 'sha256': sha256(raw).hexdigest(), 'hashKind': kind}


changed = subprocess.check_output(['git', 'diff', '--name-only', prior_commit, app['source']], text=True).splitlines()
assert not any(name.endswith(('RampComparison.java', 'RampMission.java', 'BulletRamp.java',
                              'DataSummary104.java', 'DataSummary102.java', 'CreationPlanner044.java'))
               or name.startswith(('src/main/', 'assets/')) for name in changed)
locked = [entry(name) for name in changed if name.startswith(('workspace-engine/', 'workspace-desktop/'))]
lock = 'docs/RAMP_TRACE_CHAIN_177_LOCK.json'
write(root / lock, {'source': app['source'], 'sourceHashes': {row['path']: row['sha256'] for row in locked},
      'binaryPaths': [row['path'] for row in locked if row['hashKind'] == 'binary']})
paths = {row['path'] for row in read('docs/readmes/176/file-inventory.json')}
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
print(json.dumps({'passed': True, 'trials': len(trials), 'screenshots': len(screenshots), 'inventory': len(inventory)}))
