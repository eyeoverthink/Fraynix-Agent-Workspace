"""Retain both the final trials and the original below-margin result."""
from pathlib import Path
import json
import sys

root=Path.cwd()
sys.path.insert(0,str(root/'scripts'))
import evidence_archive

def read(name):
    return json.loads((root/name).read_bytes())

for path in ['build/game170/result.json','workspace-engine/build/multi-rail-navigation/result.json',
             'build/validation170/summary.json','build/controller-identity170.json','build/baseline170.json']:
    assert read(path)['passed'],path
assert not read('build/multi-rail170-attempt-a.json')['passed']
names=[f'build/{name}170-tests.log' for name in ['root','workspace','desktop','archive']]
names += ['build/game170/result.json','build/prepare170.log','build/app170.log','build/install170.log',
          'build/focused170-attempt-a.log','build/multi-rail170-attempt-a.json','build/focused170b.log',
          'build/ci170-first.json','build/baseline170.json','build/controller-identity170.json',
          'docs/MULTI_RAIL_170_LOCK.json','build/study095-build.json','build/validation170/summary.json',
          'workspace-engine/build/multi-rail-navigation/result.json',
          'workspace-engine/build/rail-navigation/result.json']
names.extend(p.relative_to(root).as_posix() for p in (root/'build/validation170').rglob('*.xml'))
names.extend(p.relative_to(root).as_posix() for p in (root/'build').glob('game170-attempt-*/result.json'))
records=[{'logicalPath':name,'source':root/name} for name in sorted(names)]
manifest=evidence_archive.pack(records,root/'build/evidence170.zip',root/'build/evidence170-manifest.json')
print(json.dumps({'files':len(manifest['files']),'archive':manifest['archive']}))
