"""Package the original desktop trials and focused verification results."""
from pathlib import Path
import json
import sys

root = Path.cwd()
sys.path.insert(0,str(root/'scripts'))
import evidence_archive

app = root/'build/ramp173-complete'
validation = root/'build/validation173-final'
result = json.loads((app/'result.json').read_bytes())
assert result['passed'] and json.loads((validation/'summary.json').read_bytes())['passed']
paths = [app/'result.json', validation/'summary.json', *validation.rglob('*.xml')]
paths.extend(root/'build'/f'{name}173-tests.log' for name in ('root','workspace','desktop','archive'))
paths.extend(root/'build'/name for name in ('ramp173-complete.log','ci173-first.json','study095-build.json','ramp173-legacy.json','ramp173-model-refusal.json','ramp173-legacy.log'))
paths.extend(app/shot['name'] for shot in result['captures'])
report = result['planned']['mission']['report']
from hashlib import sha256
from shutil import copyfile
folder = app/'project/artifacts'/report['id']/'versions'/str(report['version'])
original = folder/'input.csv'
assert sha256(original.read_bytes()).hexdigest() == result['planned']['mission']['source']['csvSha256']
for name in ('index.html','input.csv','samples.csv','experiment.json'):
    target=app/('verified-'+name)
    copyfile(folder/name,target)
    paths.append(target)
records = [{'logicalPath':p.relative_to(root).as_posix(),'source':p} for p in sorted(paths)]
manifest = evidence_archive.pack(records,root/'build/evidence173.zip',root/'build/evidence173-manifest.json')
print(json.dumps({'files':len(manifest['files']),'archive':manifest['archive']}))
