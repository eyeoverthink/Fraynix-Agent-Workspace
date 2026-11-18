"""Retain raw final and failed receipts once, outside the Git snapshot."""
from pathlib import Path
import importlib.util, json

r = Path.cwd()
spec = importlib.util.spec_from_file_location('evidence_archive', r/'scripts/evidence_archive.py')
archive_api = importlib.util.module_from_spec(spec)
spec.loader.exec_module(archive_api)
assert json.loads((r/'build/comparison165.json').read_bytes())['totalCompletedWorkflowsIncludingBaseline'] == 60
names = {p.relative_to(r).as_posix() for p in (r/'build').glob('*165*.log')}
names.update(p.relative_to(r).as_posix() for p in (r/'build').glob('study165*/result.json'))
names.update(p.relative_to(r).as_posix() for p in (r/'build/validation165').rglob('*.xml'))
names.update(p.relative_to(r).as_posix() for p in (r/'build/attempts165').rglob('*') if p.is_file())
names.update(['build/ci165-start.json', 'build/ci165-before-publication.json', 'build/attempts165.json', 'build/findings165.md', 'build/findings165.json', 'build/app-visual-review165.json', 'build/validation165/summary.json', 'build/study095-build.json', 'build/comparison165.json', 'build/native-identity165.json', 'build/render163.json', 'docs/EVIDENCE_REUSE_165_LOCK.json', 'build/review165-inventory.json', 'build/review165-reproduced.json'])
inputs = [{'logicalPath': n, 'source': str(r/n)} for n in sorted(names)]
inputs.append({'logicalPath': 'build/release165-preflight.json', 'source': str(r/'build/release165-preflight.json')})
baseline = Path('J:/Codex-Work/fraynix-baseline-164')
baseline_names = ['build/study095-build.json', 'docs/WORKFLOW_COST_164_LOCK.json']
for name in ['on-c', 'on-e', 'on-d']:
    baseline_names.extend([f'build/study164-{name}.log', f'build/study164-{name}/result.json'])
inputs.extend({'logicalPath': 'baseline164/'+n, 'source': str(baseline/n)} for n in baseline_names)
(r/'build/evidence165-inputs.json').write_text(json.dumps(inputs, indent=2)+'\n', encoding='utf8')
manifest = archive_api.pack(inputs, r/'build/evidence165.zip', r/'build/evidence165-manifest.json')
print(json.dumps({'files': len(manifest['files']), 'archive': manifest['archive']}))
