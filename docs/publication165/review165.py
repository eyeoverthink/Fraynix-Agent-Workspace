"""Reproduce the architecture size inventory from the published164 Git tree."""
from pathlib import Path
import hashlib, json, subprocess

base = '100518181e74a3324ce2bc65b328b10ff0e61be5'
rows = []
for line in subprocess.check_output(['git', 'ls-tree', '-r', '-l', base], text=True).splitlines():
    metadata, name = line.split('\t', 1)
    fields = metadata.split()
    if fields[1] == 'blob': rows.append((name, int(fields[3]), fields[2]))
readmes = [row for row in rows if row[0].startswith('docs/readmes/')]
java = [row for row in rows if row[0].startswith('workspace-engine/src/main/java/') and row[0].endswith('.java')]
lengths = []
for name, _, _ in java:
    raw = subprocess.check_output(['git', 'show', base+':'+name]).decode('utf8')
    lengths.append((max(map(len, raw.splitlines()), default=0), name))
catalog = json.loads(subprocess.check_output(['git', 'show', base+':workspace-engine/src/main/resources/tools-044.json']))
result = {'source': base, 'trackedBytes': sum(row[1] for row in rows), 'readmeBytes': sum(row[1] for row in readmes), 'readmeFiles': len(readmes), 'gzipFiles': sum(name.endswith('.gz') for name, _, _ in readmes), 'javaFiles': len(java), 'numberedJavaFiles': sum(Path(name).stem[-1].isdigit() for name, _, _ in java), 'longestJavaLines': sorted(lengths, reverse=True)[:5], 'catalogKeys': list(catalog) if isinstance(catalog, dict) else None, 'catalogArrayLength': len(catalog) if isinstance(catalog, list) else None}
original = json.loads(Path('build/review165-inventory.json').read_bytes())
for key in ['readmeFiles', 'gzipFiles', 'javaFiles', 'numberedJavaFiles']:
    assert result[key] == original[key], key
result['originalWorkingTreeReviewBytes'] = original['trackedBytes']
result['gitBlobSizeDifference'] = result['trackedBytes'] - original['trackedBytes']
result['scope'] = 'Immutable published Git blob sizes; the initial review measured the tracked checkout, whose text line endings may differ. Counts must agree.'
Path('build/review165-reproduced.json').write_text(json.dumps(result, indent=2)+'\n', encoding='utf8')
print(json.dumps({key: value for key, value in result.items() if key != 'longestJavaLines'}))
