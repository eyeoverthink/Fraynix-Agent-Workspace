"""Package exact verifier output, examined artifacts and captured desktop evidence."""
from pathlib import Path
import json
import shutil
import sys

root = Path.cwd()
sys.path.insert(0, str(root / 'scripts'))
import evidence_archive

base = root / 'build'
study = base / sys.argv[1]
validation = base / 'validation175-final'
result = json.loads((study / 'result.json').read_bytes())
assert result['passed'] and json.loads((validation / 'summary.json').read_bytes())['passed']
paths = [study / 'result.json', validation / 'summary.json', *validation.rglob('*.xml')]
paths.extend(base / f'{name}175-tests.log' for name in ('root', 'workspace', 'desktop', 'archive'))
paths.extend(base / name for name in ('ci175-first.json', 'study095-build.json', 'routing175-prior.json'))
paths.extend(study / row['name'] for row in result['captures'])
if (study / 'result-before-recovery.json').is_file():
    paths.append(study / 'result-before-recovery.json')
for log in sorted(base.glob('routing175-*.log')):
    paths.append(log)
    record = base / log.stem / 'result.json'
    if record.is_file() and record != study / 'result.json':
        paths.append(record)
for case in result['cases']:
    ref = case['evidence']['artifact']
    origin = study / 'project' / 'artifacts' / ref['id'] / 'versions' / str(ref['version'])
    target = study / 'examined-artifacts' / case['caseId'] / case['mode']
    for file in origin.rglob('*'):
        if not file.is_file():
            continue
        assert not file.is_symlink()
        copy = target / file.relative_to(origin)
        copy.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(file, copy)
        paths.append(copy)
    assert (target / 'index.html').is_file()
records = [{'logicalPath': path.relative_to(root).as_posix(), 'source': path} for path in sorted(set(paths))]
manifest = evidence_archive.pack(records, base / 'evidence175.zip', base / 'evidence175-manifest.json')
print(json.dumps({'files': len(records), 'archive': manifest['archive']}))
