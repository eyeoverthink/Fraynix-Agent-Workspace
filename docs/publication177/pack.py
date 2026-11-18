"""Collect exact chain reports and original desktop evidence for publication."""
from pathlib import Path
import json
import shutil
import sys

root = Path.cwd()
sys.path.insert(0, str(root / 'scripts'))
import evidence_archive

build = root / 'build'
study = build / 'chain177-live'
validation = build / 'validation177-final'
app = json.loads((study / 'result.json').read_bytes())
assert app['passed'] and json.loads((validation / 'summary.json').read_bytes())['passed']
paths = [study / 'result.json', validation / 'summary.json', *validation.rglob('*.xml')]
paths.extend(build / f'{name}177-tests.log' for name in ('root', 'workspace', 'desktop', 'archive'))
paths.extend(build / name for name in ('ci177-first.json', 'study095-build.json', 'chain177-live.log',
                                      'chain177-prior.json', 'chain177-prior.log'))
paths.extend(study / row['name'] for row in app['captures'])
for mode in ('recovered', 'automatic'):
    outcome = app[mode]
    for name, ref in [('comparison', outcome['done']['completion044']['chainHandoff']['report']),
                      ('summary', outcome['workflow']['report'])]:
        source = study / 'project' / 'artifacts' / ref['id'] / 'versions' / str(ref['version'])
        target = study / 'examined-artifacts' / mode / name
        for file in source.rglob('*'):
            if file.is_file():
                assert not file.is_symlink()
                copy = target / file.relative_to(source)
                copy.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(file, copy)
                paths.append(copy)
        assert (target / 'index.html').is_file()
records = [{'logicalPath': path.relative_to(root).as_posix(), 'source': path} for path in sorted(set(paths))]
receipt = evidence_archive.pack(records, build / 'evidence177.zip', build / 'evidence177-manifest.json')
print(json.dumps({'files': len(records), 'archive': receipt['archive']}))
