"""Archive the actual product runs, refusals and test records."""
from pathlib import Path
import json
import sys

root = Path.cwd()
sys.path.insert(0, str(root / 'scripts'))
import evidence_archive

app = root / 'build/ramp172-verified'
out = root / 'build/validation172-final'
product = json.loads((app / 'result.json').read_bytes())
assert product['passed'] and json.loads((out / 'summary.json').read_bytes())['passed']
assert product['archived']['passed'] and product['one']['verified']['canvasReceipt']['passed']
paths = [app / 'result.json', out / 'summary.json', *out.rglob('*.xml')]
paths.extend(root / 'build' / (name + '172-tests.log') for name in ('root', 'workspace', 'desktop', 'archive'))
paths.extend(root / 'build' / name for name in ('ramp172-verified.log', 'ci172-first.json', 'study095-build.json'))
paths.extend(app / shot['name'] for shot in product['captures'])
records = [{'logicalPath': p.relative_to(root).as_posix(), 'source': p} for p in sorted(paths)]
manifest = evidence_archive.pack(records, root / 'build/evidence172.zip', root / 'build/evidence172-manifest.json')
print(json.dumps({'files': len(manifest['files']), 'archive': manifest['archive']}))
