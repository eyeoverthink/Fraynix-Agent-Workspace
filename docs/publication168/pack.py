"""Pack only known test and archive evidence, excluding application project data."""
from pathlib import Path
import json
import sys

root = Path.cwd()
sys.path.insert(0, str(root/'scripts'))
import evidence_archive

def read(name):
    return json.loads((root/name).read_bytes())

assert read('build/validation168/summary.json')['passed']
assert read('build/game168-final/result.json')['passed']
assert read('workspace-engine/build/original-courses/result.json')['passed']
assert read('build/archive168/result.json')['passed']
names = [f'build/{name}168-tests.log' for name in ['root','workspace','desktop','archive']]
names += ['build/app168.log','build/app168-final.log','build/game168/result.json','build/prepare168.log','build/native168-a.log','build/ci168-first.json',
          'build/game168-final/result.json','build/install168.log','build/install168-refresh.log',
          'build/install168-copy.log','build/stamp168.log','build/stamp168-final.log','build/validation168/summary.json',
          'workspace-engine/build/original-courses/result.json','build/archive168/result.json',
          'build/controller-identity168.json','docs/ORIGINAL_COURSES_168_LOCK.json','build/study095-build.json']
names.extend('build/archive168/'+name for name in ['legacy162.zip','legacy162-manifest.json','catalog162.json'])
names.extend(p.relative_to(root).as_posix() for p in (root/'build/validation168').rglob('*.xml'))
names.extend(p.relative_to(root).as_posix() for p in (root/'build').glob('game168-attempt-*/result.json'))
records = [{'logicalPath': name, 'source': root/name} for name in sorted(names)]
value = evidence_archive.pack(records, root/'build/evidence168.zip', root/'build/evidence168-manifest.json')
print(json.dumps({'files': len(value['files']), 'archive': value['archive']}))
