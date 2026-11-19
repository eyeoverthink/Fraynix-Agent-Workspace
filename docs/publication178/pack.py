"""Collect the exact native, rendered and archived results from the isolated desktop trial."""
import json
from pathlib import Path
import shutil
import sys

root = Path.cwd()
sys.path.insert(0, str(root / 'scripts'))
import evidence_archive

build = root / 'build'
study = build / 'relay178-live'
validation = build / 'validation178-final'
app = json.loads((study / 'result.json').read_bytes())
assert app['passed'] and json.loads((validation / 'summary.json').read_bytes())['passed']
assert json.loads((build / 'relay178-prior.json').read_bytes())['passed']
paths = [study / 'result.json', validation / 'summary.json', *validation.rglob('*.xml')]
paths.extend(build / f'{name}178-tests.log' for name in ('root', 'workspace', 'desktop', 'archive'))
paths.extend(build / name for name in ('ci178-first.json', 'study095-build.json', 'relay178-live.log',
                                      'relay178-prior.json', 'relay178-prior.log',
                                      'relay178-admission-pilot.log', 'workspace178-concurrent-compile.log'))
paths.extend(study / row['name'] for row in app['captures'])
for case in ('recovered', 'automatic'):
    result = app[case]
    paths.append(study / 'project' / 'relay-playtests' / (result['id'] + '.json'))
    refs = {'playtest': result['verified']['report'], 'dashboard': result['dashboard']}
    for name, ref in refs.items():
        source = study / 'project' / 'artifacts' / ref['id'] / 'versions' / str(ref['version'])
        target = study / 'examined-artifacts' / case / name
        for file in source.rglob('*'):
            if file.is_file():
                assert not file.is_symlink()
                copy = target / file.relative_to(source)
                copy.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(file, copy)
                paths.append(copy)
        assert (target / 'index.html').is_file()
records = [{'logicalPath': path.relative_to(root).as_posix(), 'source': path} for path in sorted(set(paths))]
receipt = evidence_archive.pack(records, build / 'evidence178.zip', build / 'evidence178-manifest.json')
print(json.dumps({'files': len(records), 'archive': receipt['archive']}))
