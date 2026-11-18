"""Expose the old-build category tradeoff alongside complete task costs."""
from pathlib import Path
import importlib.util, json

r = Path.cwd()
spec = importlib.util.spec_from_file_location('evidence_archive', r/'scripts/evidence_archive.py')
archive_api = importlib.util.module_from_spec(spec); spec.loader.exec_module(archive_api)
manifest = json.loads((r/'build/evidence165-manifest.json').read_bytes())
archive = r/'build/downloaded-evidence165.zip'
archive_api.verify(manifest, archive)
rows = []
for name in ['on-e', 'on-d']:
    value = json.loads(archive_api.original(manifest, archive, f'baseline164/build/study164-{name}/result.json'))
    assert value['passed']
    totals = {}
    for task in value['tasks']:
        segments = [(task['beforeDiagnostics'], task['afterDiagnostics'])]
        if 'beforeRestart' in task:
            segments = [(task['beforeDiagnostics'], task['beforeRestart']),
                        ({'java': {'epoch': task['afterRestart']['java']['epoch'], 'totals': {}}}, task['afterDiagnostics'])]
        for before, after in segments:
            assert before['java']['epoch'] == after['java']['epoch']
            for kind, item in after['java']['totals'].items():
                elapsed = item['inclusiveMs'] - before['java']['totals'].get(kind, {}).get('inclusiveMs', 0)
                assert elapsed >= 0
                totals[kind] = totals.get(kind, 0) + elapsed
    rows.append({'run': name, 'totalsMs': totals})
value = {'passed': True, 'rows': rows, 'scope': 'Derived from downloaded and hash-verified untouched164 task spans with the same restart segment accounting. Inclusive categories overlap; do not add them to task totals.'}
(r/'docs/readmes/165/baseline-diagnostics.json').write_text(json.dumps(value, indent=2)+'\n', encoding='utf8')
print(json.dumps(value))
