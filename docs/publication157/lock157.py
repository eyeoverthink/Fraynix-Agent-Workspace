"""Freeze the method before new generated evaluation. Run once after development checks."""
from pathlib import Path
import hashlib,json,subprocess,xml.etree.ElementTree as ET
r=Path.cwd();target=r/'docs/OBSERVATION_157_LOCK.json';assert not target.exists()
assert json.loads((r/'build/probe157.json').read_bytes())['passed']
count=0
for p in (r/'workspace-engine/build/test-results/test').glob('*.xml'):
 e=ET.parse(p).getroot();assert sum(int(e.get(k,'0')) for k in ['failures','errors','skipped'])==0;count+=int(e.get('tests'))
assert count==13
paths=set(json.loads((r/'docs/OBSERVATION_156_LOCK.json').read_bytes())['sourceHashes'])
paths.add('.gitattributes')
for folder in ['workspace-engine/src/main/java/fraymus/workspace','workspace-engine/src/test/java/fraymus/workspace']:
 paths.update(p.relative_to(r).as_posix() for p in (r/folder).glob('*157*.java'))
for name in ['Profile157.java','OriginalProbe157.java','profile157.py','profile-report157.py','plan157.md','HarderSources157.java','verify-sources157.py','study157.cjs','ReplayMeasure157.java','measure157.py','qualify157.py','run-studies157.py','validate157.py','lock157.py']:
 paths.add('docs/publication157/'+name)
hashes={name:hashlib.sha256((r/name).read_bytes().replace(b'\r\n',b'\n')).hexdigest() for name in sorted(paths)}
data={'edition':157,'baseline':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),'scope':'Frozen before reserved157 source generation. Exact original sampling coordinates, fresh-frame grayscale and immutable reference terms; compare complete outputs against published156. Every original source/outcome check and default retained.','sourceHashes':hashes}
target.write_bytes((json.dumps(data,indent=2)+'\n').encode());print(json.dumps({'locked':len(paths),'reservedSourcesExist':(r/'build/reserved157').exists()}))
