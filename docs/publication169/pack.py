"""Pack the original trial outputs and exact published comparison."""
from pathlib import Path
import json
import sys

root = Path.cwd()
sys.path.insert(0, str(root/'scripts'))
import evidence_archive

def read(name):
    return json.loads((root/name).read_bytes())

for result in ['build/game169/result.json', 'build/validation169/summary.json',
               'workspace-engine/build/rail-navigation/result.json']:
    assert read(result)['passed']
assert read('build/baseline169/receipt.json')['exactPublishedOriginal']
names = [f'build/{name}169-tests.log' for name in ['root','workspace','desktop','archive']]
names += ['build/game169/result.json','build/prepare169.log','build/app169.log','build/install169.log',
          'build/focused169.log','build/ci169-first.json','build/ci168-failed-chat.xml',
          'build/baseline169/edition168-app.json','build/baseline169/receipt.json',
          'build/controller-identity169.json','docs/RAIL_NAVIGATION_169_LOCK.json','build/study095-build.json',
          'build/validation169/summary.json','workspace-engine/build/rail-navigation/result.json',
          'workspace-engine/build/original-courses/result.json']
names.extend(p.relative_to(root).as_posix() for p in (root/'build/validation169').rglob('*.xml'))
names.extend(p.relative_to(root).as_posix() for p in (root/'build').glob('game169-attempt-*/result.json'))
records = [{'logicalPath':name,'source':root/name} for name in sorted(names)]
value = evidence_archive.pack(records,root/'build/evidence169.zip',root/'build/evidence169-manifest.json')
print(json.dumps({'files':len(value['files']),'archive':value['archive']}))
