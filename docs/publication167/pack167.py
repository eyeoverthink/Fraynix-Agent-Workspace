from pathlib import Path
import sys,json
r=Path.cwd();sys.path.insert(0,str(r/'scripts'))
import evidence_archive
assert json.loads((r/'build/validation167/summary.json').read_bytes())['passed']
assert json.loads((r/'build/game167-final/result.json').read_bytes())['passed']
names={p.relative_to(r).as_posix() for p in (r/'build').glob('*167*.log')}
names.update(p.relative_to(r).as_posix() for p in (r/'build/validation167').rglob('*.xml'))
names.update(p.relative_to(r).as_posix() for p in (r/'build/attempts167').iterdir() if p.is_file())
names.update(p.relative_to(r).as_posix() for p in (r/'build').glob('game167*/result.json'))
names.update(['build/relay-runtime-attempt-a/result.json','workspace-engine/build/original-content/result.json',
 'build/archive167/result.json','build/archive167/catalog163.json','build/archive167/legacy163.zip',
 'build/archive167/legacy163-manifest.json','build/validation167/summary.json','build/controller-identity167.json',
 'build/ci167-first.json','build/ci166-failed-job.json','build/ci167-before-publication.json',
 'build/study095-build.json','docs/ORIGINAL_CONTENT_167_LOCK.json','build/app-visual167.json'])
for directory in ['reference115','reference116','game115-repair','level114-runtime']:
 p=r/'workspace-engine/build'/directory/'result.json'
 if p.exists():names.add(p.relative_to(r).as_posix())
value=evidence_archive.pack([{'logicalPath':n,'source':r/n} for n in sorted(names)],
 r/'build/evidence167.zip',r/'build/evidence167-manifest.json')
print(json.dumps({'files':len(value['files']),'archive':value['archive']}))
