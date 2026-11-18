from pathlib import Path
import json
import sys

root = Path.cwd()
sys.path.insert(0, str(root/'scripts'))
import evidence_archive

assert json.loads((root/'build/comparison166.json').read_bytes())['passed']
names = {path.relative_to(root).as_posix() for path in (root/'build').glob('*166*.log')}
names.update(path.relative_to(root).as_posix() for path in (root/'build/validation166').rglob('*.xml'))
names.update(path.relative_to(root).as_posix() for path in (root/'build').glob('study166*/result.json'))
names.update(path.relative_to(root).as_posix() for path in (root/'build').glob('compatibility166*/result.json'))
names.update(['build/validation166/summary.json', 'build/comparison166.json',
              'build/native-identity166.json', 'build/render163.json', 'build/study095-build.json',
              'docs/DOMAIN_EXTRACTION_166_LOCK.json', 'build/ci166-start.json',
              'build/ci166-before-publication.json', 'build/app-visual-review166.json'])
for filename in ['catalog.json', 'catalog.json.gz', 'assets.json', 'preparation.json',
                 'candidate.json', 'candidate-files.json', 'candidate-build.log',
                 'legacy164.zip', 'legacy164-manifest.json']:
    names.add('build/migration166/'+filename)
names.update(path.relative_to(root).as_posix() for path in
             (root/'build/migration166/clone/workspace-engine/build/test-results/test').glob('*.xml'))
inputs = [{'logicalPath': name, 'source': str(root/name)} for name in sorted(names)]
value = evidence_archive.pack(inputs, root/'build/evidence166.zip', root/'build/evidence166-manifest.json')
print(json.dumps({'files': len(value['files']), 'archive': value['archive']}))
