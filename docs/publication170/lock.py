"""Compare shipped controller bytes and pin the tested code and assets."""
from pathlib import Path
import hashlib
import json
import subprocess
import zipfile

root = Path.cwd()
sha = lambda value: hashlib.sha256(value).hexdigest()
baseline = '20399ecafab8d1355413ec005b5dc7d1bc3b1164'
tracked = subprocess.check_output(['git', 'ls-files'], text=True).splitlines()
paths = [name for name in tracked if name.startswith(
    ('src/', 'workspace-engine/src/', 'assets/original/', 'workspace-desktop/renderer/'))
    or name in {'build.gradle', 'workspace-engine/build.gradle', 'workspace-desktop/main.cjs',
        'scripts/history_archive.py', 'scripts/test_history_archive.py', 'docs/publication170/study.cjs'}
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
        'scope': 'Original multi-rail courses, pace controls, runtime and trial inputs',
        'sourceHashes': dict(sorted(hashes.items())), 'binaryPaths': sorted(binary)}
(root/'docs/MULTI_RAIL_170_LOCK.json').write_text(json.dumps(lock, indent=2)+'\n')
previous = json.loads(subprocess.check_output(
    ['git', 'show', baseline+':docs/readmes/169/controller-identity.json']))
with zipfile.ZipFile(root/'build/libs/Fraynix-Agent-Workspace-0.1.0.jar') as jar:
    actual = {name: sha(jar.read(name)) for name in previous['classes']}
changed = [name for name in actual if actual[name] != previous['hashes'][name]]
assert changed == ['agent/GameInput.class'], changed
unchanged = {name: digest for name, digest in actual.items() if name not in changed}
old_assets = subprocess.check_output(
    ['git', 'ls-tree', '-r', '--name-only', baseline, 'assets/original/relay-garden/'], text=True).splitlines()
for name in old_assets:
    assert subprocess.check_output(['git', 'show', baseline+':'+name]) == (root/name).read_bytes(), name
receipt = {'passed': True, 'baseline': baseline, 'hashes': actual,
           'classes': previous['classes'], 'changed': changed, 'unchanged': unchanged,
           'baselineAssetsCompared': len(old_assets)}
(root/'build/controller-identity170.json').write_text(json.dumps(receipt, indent=2)+'\n')
print(json.dumps({'sources': len(paths), 'baselineAssets': len(old_assets), 'unchangedCompiledClasses': len(unchanged)}))
