"""Collect the isolated workbench trial and exact inspected output files."""
import json
from base64 import b64decode
from pathlib import Path
import shutil
import sys

root = Path.cwd()
sys.path.insert(0, str(root / 'scripts'))
import evidence_archive

build = root / 'build'
study = build / 'workbench180-live'
final = build / 'workbench180-final'
validation = build / 'validation180-final'
app = json.loads((study / 'result.json').read_bytes())
assert app['passed'] and json.loads((validation / 'summary.json').read_bytes())['passed']
assert json.loads((build / 'workbench180-prior.json').read_bytes())['passed']
rendered = json.loads((final / 'result.json').read_bytes())
assert rendered['presentationPassed'] and rendered['studySource'] == app['source'] and rendered['engineJarsMatched']
paths = [study / 'result.json', final / 'result.json', validation / 'summary.json', *validation.rglob('*.xml')]
paths.extend(build / f'{name}180-tests.log' for name in ('root', 'workspace', 'desktop', 'archive'))
paths.extend(build / name for name in ('ci180-first.json', 'study095-build.json', 'workbench180-live.log',
    'workbench180-study-build.json', 'workbench180-presentation.log', 'workbench180-prior.json',
    'workbench180-prior.log', 'install180-pilot.log', 'install180.log'))
paths.extend(study / item['name'] for item in app['captures'])
paths.extend(final / item['name'] for item in rendered['captures'])
paths.append(study / 'project' / 'creation-044' / 'requests.json')
observations = []
for label in ('forward', 'reverse', 'inspected', 'taught'):
    checked = app['files'][label]
    checkpoint = study / 'project' / 'request-workbench' / (checked['id'] + '.task.json')
    raw = json.loads(checkpoint.read_bytes())['data']['source']
    body = b64decode(raw['base64'], validate=True)
    assert evidence_archive.digest(body) == raw['sha256'] == checked['source']['sha256']
    assert len(body) == raw['size'] == checked['source']['size']
    assert raw['name'] in {'incoming.csv', 'portable.json', 'signature-sample.bin', 'taught.csv'}
    fixture = study / 'examined-inputs' / label / raw['name']
    fixture.parent.mkdir(parents=True, exist_ok=True)
    fixture.write_bytes(body)
    paths.append(fixture)
    observations.append({'review': checked, 'publicFixture': fixture.relative_to(root).as_posix()})
catalog = study / 'examined-inputs' / 'review-receipts.json'
catalog.write_text(json.dumps(observations, indent=2, ensure_ascii=False) + '\n', encoding='utf8')
paths.append(catalog)
for file in (study / 'project' / 'exports').glob('*'):
    assert not file.is_symlink()
    paths.append(file)
refs = [(key, app['files'][key]['report']) for key in ('forward','reverse','inspected','taught')]
refs.append(('created-tool', app['creation']['ref']))
for name, ref in refs:
    source = study / 'project' / 'artifacts' / ref['id'] / 'versions' / str(ref['version'])
    target = study / 'examined-artifacts' / name
    for file in source.rglob('*'):
        if file.is_file():
            assert not file.is_symlink()
            copy = target / file.relative_to(source)
            copy.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(file, copy)
            paths.append(copy)
    assert (target / 'index.html').is_file()
records = [{'logicalPath': path.relative_to(root).as_posix(), 'source': path} for path in sorted(set(paths))]
receipt = evidence_archive.pack(records, build / 'evidence180.zip', build / 'evidence180-manifest.json')
print(json.dumps({'files': len(records), 'archive': receipt['archive']}))
