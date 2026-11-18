from pathlib import Path
import hashlib
import json
import subprocess
import zipfile

root = Path.cwd()
target = root/'docs/DOMAIN_EXTRACTION_166_LOCK.json'
assert not target.exists()
assert json.loads((root/'build/validation166/summary.json').read_bytes())['passed']
paths = set(json.loads((root/'docs/EVIDENCE_REUSE_165_LOCK.json').read_bytes())['sourceHashes'])
paths.update(subprocess.check_output(['git', 'diff', '--name-only',
    '671a3605d7481e7e5994b75265bc7fab8ce70578'], text=True).splitlines())
paths.update(p.relative_to(root).as_posix() for p in (root/'docs/publication166').glob('*') if p.is_file())
sha = lambda b: hashlib.sha256(b).hexdigest()
hashes = {name: sha((root/name).read_bytes().replace(b'\r\n', b'\n')) for name in sorted(paths)}
names = ['SmallBody161', 'SmallPhysics161', 'SmallStore161', 'SmallReport161',
         'SmallVerify161', 'RecordedSmall162']
with zipfile.ZipFile(root/'build/baseline166/workspace-engine.jar') as old, zipfile.ZipFile(
        root/'workspace-engine/build/install/workspace-engine/lib/workspace-engine.jar') as new:
    classes = [name for name in old.namelist() if any(
        name == 'fraymus/workspace/'+c+'.class' or name.startswith('fraymus/workspace/'+c+'$') for c in names)]
    identities = {name: sha(old.read(name)) for name in classes}
    for name, checksum in identities.items():
        assert sha(new.read(name)) == checksum, name
(root/'build/native-identity166.json').write_text(json.dumps({'passed': True, 'classes': identities}, indent=2)+'\n')
target.write_text(json.dumps({'edition': 166,
    'baseline': subprocess.check_output(['git', 'rev-parse', 'HEAD'], text=True).strip(),
    'scope': 'Locked domain extraction and archival tools before final actual app trials. '
             'Unchanged native/controller/report and parent class bytes; all source/outcome checks retained.',
    'sourceHashes': hashes}, indent=2)+'\n')
print(json.dumps({'lockedFiles': len(hashes), 'unchangedClasses': len(identities)}))
