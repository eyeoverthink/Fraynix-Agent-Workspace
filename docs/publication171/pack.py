"""Archive actual trial data and the associated test records."""
from pathlib import Path
import json
import sys

root = Path.cwd()
sys.path.insert(0, str(root / 'scripts'))
import evidence_archive

app = root / 'build/game171-verified'
replay = root / 'workspace-engine/build/relay-playtest/result.json'
out = root / 'build/validation171'
for evidence in (app / 'result.json', replay, out / 'summary.json'):
    assert json.loads(evidence.read_bytes())['passed'], evidence

paths = [app / 'result.json', replay, out / 'summary.json', *out.rglob('*.xml')]
paths.extend(root / 'build' / (name + '171-tests.log') for name in ('root', 'workspace', 'desktop', 'archive'))
paths.extend(root / 'build' / name for name in ('app171-verified.log', 'focused171a.log',
                                                'ci171-first.json', 'study095-build.json'))
for capture in json.loads((app / 'result.json').read_bytes())['captures']:
    paths.append(app / capture['name'])
records = [{'logicalPath': p.relative_to(root).as_posix(), 'source': p} for p in sorted(paths)]
manifest = evidence_archive.pack(records, root / 'build/evidence171.zip', root / 'build/evidence171-manifest.json')
print(json.dumps({'files': len(manifest['files']), 'archive': manifest['archive']}))
