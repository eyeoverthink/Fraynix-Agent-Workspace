"""Recover the actual edition 168 AI comparison from its downloaded release asset."""
from pathlib import Path
import json
import sys

root = Path.cwd()
sys.path.insert(0, str(root/'scripts'))
import evidence_archive

manifest = json.loads((root/'docs/readmes/168/original-evidence.json').read_bytes())
archive = root/'build/downloaded-evidence168.zip'
if not archive.exists():
    evidence_archive.download(manifest, archive)
evidence_archive.verify(manifest, archive)
name = 'build/game168-final/result.json'
original = evidence_archive.original(manifest, archive, name)
report = json.loads(original)
assert report['passed']
out = root/'build/baseline169'
out.mkdir(exist_ok=True)
(out/'edition168-app.json').write_bytes(original)
receipt = {'exactPublishedOriginal': True, 'source': report['source'], 'archive': manifest['archive'],
           'logicalPath': name, 'courses': []}
for course in report['courses']:
    run = course['defaultAI']
    assert run['player']['dead'] and not run['player']['won']
    receipt['courses'].append({'id': course['id'], 'observedTick': run['tick'],
                               'hazard': run['progress']['hazard']})
(out/'receipt.json').write_text(json.dumps(receipt, indent=2)+'\n',encoding='utf8')
print(json.dumps(receipt))
