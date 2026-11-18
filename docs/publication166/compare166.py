"""Use the frozen165 normalization, not a new comparison tuned to this run."""
from pathlib import Path
import ast
import hashlib
import json

root = Path.cwd()
tree = ast.parse((root/'docs/publication165/compare165.py').read_text())
function = next(node for node in tree.body if isinstance(node, ast.FunctionDef) and node.name == 'normalized')
scope = {}
exec(compile(ast.Module(body=[function], type_ignores=[]), '<frozen165-normalizer>', 'exec'), scope)
normalize = scope['normalized']
old = json.loads((root/'build/study165-on-b/result.json').read_bytes())
new = json.loads((root/'build/study166-on-b/result.json').read_bytes())
assert old['passed'] and new['passed'] and len(new['tasks']) == 10
rows = []
for prior, current in zip(old['tasks'], new['tasks']):
    expected, actual = normalize(prior), normalize(current)
    if expected != actual:
        (root/'build/comparison166-difference.json').write_text(json.dumps(
            {'prior': expected, 'actual': actual}, indent=2))
        raise AssertionError('Original numerical/source/report comparison differs')
    assert current['verified']['passed'] and current['task']['status'] == 'completed'
    rows.append({'name': current['name'], 'kind': current['kind'], 'passed': True,
                 'sha256': hashlib.sha256(json.dumps(actual, sort_keys=True).encode()).hexdigest(),
                 'totalMs': current['totalMs']})
assert new['finalWorld']['revision'] == 8
assert sum('beforeRestart' in row for row in new['tasks']) == 2
result = {'passed': True, 'freshWorkflows': 10, 'nativeRevisions': 8, 'savedProcedures': 4,
          'interruptions': 2, 'exactComparisons': rows,
          'scope': 'Original165 normalizer checks exact source, uncertainty, native motion, outcomes '
                   'and actual reports. Fresh IDs are normalized, not evidence contents. '
                   'One functional regression run, not a performance comparison.'}
(root/'build/comparison166.json').write_text(json.dumps(result, indent=2)+'\n')
print(json.dumps({'passed': True, 'exactComparisons': len(rows)}))
