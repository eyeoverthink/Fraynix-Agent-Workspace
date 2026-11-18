"""Package the exact attempts, checked paired report and Java regression results."""
from hashlib import sha256
from pathlib import Path
from shutil import copyfile
import json
import sys

root = Path.cwd()
sys.path.insert(0,str(root/'scripts'))
import evidence_archive

app = root/'build/ramp174-release'
validation = root/'build/validation174-final'
result = json.loads((app/'result.json').read_bytes())
assert result['passed'] and json.loads((validation/'summary.json').read_bytes())['passed']
paths = [app/'result.json', validation/'summary.json', *validation.rglob('*.xml')]
paths.extend(root/'build'/f'{name}174-tests.log' for name in ('root','workspace','desktop','archive'))
paths.extend(root/'build'/name for name in ('ramp174-release.log','ci174-first.json','study095-build.json',
    'ramp174-legacy.json','ramp174-legacy.log','ramp174-initial-model.json','ramp174-original-gpt-routing.json','ramp174-planned-resume.json','ramp174-planned-resume.log'))
paths.extend(app/shot['name'] for shot in result['captures'])
comparison = next(case for case in result['cases'] if case['wanted']=='local-ramp-comparison-v1')['completed']['mission']
report = comparison['report']
folder = app/'project/artifacts'/report['id']/'versions'/str(report['version'])
assert sha256((folder/'input.csv').read_bytes()).hexdigest() == comparison['source']['csvSha256']
for name in ('index.html','input.csv','samples.csv','experiment.json'):
    target=app/('verified-'+name)
    copyfile(folder/name,target)
    paths.append(target)
records = [{'logicalPath':p.relative_to(root).as_posix(),'source':p} for p in sorted(paths)]
manifest = evidence_archive.pack(records,root/'build/evidence174.zip',root/'build/evidence174-manifest.json')
print(json.dumps({'files':len(manifest['files']),'archive':manifest['archive']}))
