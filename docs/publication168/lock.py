"""Compare shipped controller bytes and pin the tested code and assets."""
from pathlib import Path
import hashlib
import json
import subprocess
import zipfile

root = Path.cwd()
sha = lambda value: hashlib.sha256(value).hexdigest()
baseline = 'f841ed003a912b1778ff05a0def4428b5e41b6ea'
tracked = subprocess.check_output(['git', 'ls-files'], text=True).splitlines()
paths = [name for name in tracked if name.startswith(
    ('src/', 'workspace-engine/src/', 'assets/original/', 'workspace-desktop/renderer/'))
    or name in {'build.gradle', 'workspace-engine/build.gradle', 'workspace-desktop/main.cjs',
        'scripts/history_archive.py', 'scripts/test_history_archive.py', 'docs/publication168/study.cjs'}
    or name.endswith('/RelayCourse.java')]
hashes, binary = {}, []
for name in paths:
    raw = (root/name).read_bytes()
    if Path(name).suffix in {'.png', '.wav', '.gz', '.bin', '.jpg', '.zip'}:
        binary.append(name)
    else:
        try:
            raw = raw.decode('utf-8-sig').replace(chr(13) + chr(10), chr(10)).encode()
        except UnicodeDecodeError:
            binary.append(name)
    hashes[name] = sha(raw)
lock = {'baseline': baseline,
        'scope': 'Original courses, engine source, visible controls and trial fixtures',
        'sourceHashes': dict(sorted(hashes.items())), 'binaryPaths': sorted(binary)}
(root/'docs/ORIGINAL_COURSES_168_LOCK.json').write_text(json.dumps(lock, indent=2)+'\n')
previous = json.loads(subprocess.check_output(
    ['git', 'show', baseline+':docs/readmes/167/controller-identity.json']))
with zipfile.ZipFile(root/'build/libs/Fraynix-Agent-Workspace-0.1.0.jar') as jar:
    actual = {name: sha(jar.read(name)) for name in previous['classes']}
assert actual == previous['hashes']
old_assets = subprocess.check_output(
    ['git', 'ls-tree', '-r', '--name-only', baseline, 'assets/original/relay-garden/'], text=True).splitlines()
for name in old_assets:
    assert subprocess.check_output(['git', 'show', baseline+':'+name]) == (root/name).read_bytes(), name
receipt = {'passed': True, 'baseline': baseline, 'hashes': actual,
           'classes': previous['classes'], 'baselineAssetsCompared': len(old_assets)}
(root/'build/controller-identity168.json').write_text(json.dumps(receipt, indent=2)+'\n')
print(json.dumps({'sources': len(paths), 'baselineAssets': len(old_assets), 'compiledClasses': len(actual)}))
