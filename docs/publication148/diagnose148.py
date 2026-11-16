"""Describe already-exposed run traces; never changes methods, sources or labels."""
from pathlib import Path
from collections import Counter
import json, hashlib

r = Path.cwd()
p = r / 'build/occlusion148-black-lighter-movement-app/result.json'
s = json.loads(p.read_bytes())
rows = []
for run in s['runs']:
    if run['role'] != 'evaluation':
        continue
    scores = run['audit']['scores']
    periods = {}
    for name, low, high in [('beforeCover', 28, 36), ('covered', 37, 59),
                            ('originalPositionReturn', 60, 61),
                            ('partialMovement', 62, 66), ('newPosition', 67, 80)]:
        selected = [x for x in scores if low <= x['sourceFrame'] <= high]
        periods[name] = {'samples': len(selected), 'accepted': sum(x['accepted'] for x in selected),
                         'correct': sum(x['correct'] for x in selected),
                         'wrongObjectMatches': sum(x['confirmedDistractorMatch'] for x in selected)}
    paths = Counter()
    search = Counter()
    if run['policy'].endswith('148'):
        for frame in run['run']['trace']:
            m = frame['objects'][0]['tracker']['confirmation148']
            search.update(m.get('search', {}))
            for h in m['hypotheses']:
                paths[h.get('path', {}).get('reason', 'no path result')] += 1
    rows.append({'policy': run['policy'], 'periods': periods,
                 'perReferencePathReasons': dict(paths), 'searchCountersSum': dict(search),
                 'trackingMs': run['trackingMs'], 'auditMs': run['auditMs']})
result = {'scope': 'Post-comparison diagnosis of the now-exposed fixed source. '
                  'No parameter change or fresh validation claim. Counts describe hypotheses, '
                  'not independently calibrated probabilities. Timings are single runs; '
                  'candidate tracking includes one stop/resume.',
          'inputResultSha256': hashlib.sha256(p.read_bytes()).hexdigest(), 'rows': rows}
(r / 'build/diagnosis148.json').write_bytes((json.dumps(result, indent=2) + '\n').encode())
print(json.dumps(result))
