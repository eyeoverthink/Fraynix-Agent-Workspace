"""Collect actual local inference, approval, replay, browser and archive evidence."""
import json
from pathlib import Path
import shutil
import sys

root = Path.cwd()
sys.path.insert(0, str(root / 'scripts'))
import evidence_archive

build = root / 'build'
study = build / 'recommend179-live'
validation = build / 'validation179-final'
app = json.loads((study / 'result.json').read_bytes())
assert app['passed'] and json.loads((validation / 'summary.json').read_bytes())['passed']
assert json.loads((build / 'recommend179-prior.json').read_bytes())['passed']
assert json.loads((build / 'recommend179-history.json').read_bytes())['passed']
paths = [study / 'result.json', validation / 'summary.json', *validation.rglob('*.xml')]
paths.extend(build / f'{name}179-tests.log' for name in ('root', 'workspace', 'desktop', 'archive'))
paths.extend(build / name for name in ('ci179-first.json', 'study095-build.json', 'recommend179-live.log',
    'recommend179-prior.json', 'recommend179-prior.log', 'recommend179-history.json', 'recommend179-history.log',
    'recommend179-provider-pilot.log', 'recommend179-provider-pilot/project/agency-043/tasks.json'))
paths.extend(study / item['name'] for item in app['captures'])
for row in app['trials']:
    if not row.get('proof', {}).get('passed'):
        continue
    completion = row['completion']
    if row['actual'] == 'ramp':
        original = completion['rampMission']['report']
        report = completion['javaWorkflow']['report']
    else:
        original = completion['javaGameTask115']['report']
        report = completion['relayDashboard']
    for name, ref in [('first-procedure', original), ('second-procedure', report)]:
        source = study / 'project' / 'artifacts' / ref['id'] / 'versions' / str(ref['version'])
        target = study / 'examined-artifacts' / row['fixture']['id'] / name
        for file in source.rglob('*'):
            if file.is_file():
                assert not file.is_symlink()
                copy = target / file.relative_to(source)
                copy.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(file, copy)
                paths.append(copy)
        assert (target / 'index.html').is_file()
records = [{'logicalPath': path.relative_to(root).as_posix(), 'source': path} for path in sorted(set(paths))]
receipt = evidence_archive.pack(records, build / 'evidence179.zip', build / 'evidence179-manifest.json')
print(json.dumps({'files': len(records), 'archive': receipt['archive']}))
