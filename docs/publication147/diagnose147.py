"""Describe completed, now-exposed traces without changing methods or source labels."""
from pathlib import Path
import json

r = Path.cwd()
p = r / 'build/occlusion147-black-lighter-webcam-app/result.json'
s = json.loads(p.read_bytes())
assert s['passed']

def overlap(a, b):
    area = max(0, min(a[2], b[2]) - max(a[0], b[0])) * max(0, min(a[3], b[3]) - max(a[1], b[1]))
    return area / ((a[2]-a[0])*(a[3]-a[1]) + (b[2]-b[0])*(b[3]-b[1]) - area)

rows = []
for run in s['runs']:
    if run['role'] != 'evaluation':
        continue
    returned = [x for x in run['audit']['scores'] if x['sourceFrame'] >= 57]
    hypotheses = []
    for score in returned:
        trace = run['run']['trace'][score['ordinal']-1]['objects'][0]['tracker']
        for h in trace.get('references143', {}).get('hypotheses', []):
            hypotheses.append({'sourceFrame': score['sourceFrame'], 'sourceKey': h['sourceKey'],
                               'overlap': overlap(h['box'], score['targetBox']),
                               'score': h['score'], 'rivalScore': h['rivalScore'],
                               'margin': h['score']-h['rivalScore'], 'unique': h['unique'],
                               'confirmations': h['confirmations'], 'qualified': h['qualified']})
    rows.append({'policy': run['policy'], 'returnedFrames': len(returned),
                 'acceptedReturned': sum(x['accepted'] for x in returned),
                 'correctHypotheses': sum(x['overlap'] >= .5 for x in hypotheses),
                 'hypotheses': hypotheses,
                 'trackingMs': run['trackingMs'], 'auditMs': run['auditMs']})

out = {'scope': 'Post-comparison trace diagnosis, not a new trial or changed method. Rival scores do not identify which physical object caused them.',
       'rows': rows, 'defaultsUnchanged': True, 'thresholdsUnchanged': True}
(r/'build/diagnosis147.json').write_bytes((json.dumps(out, indent=2)+'\n').encode())
print(json.dumps([{k:v for k,v in x.items() if k != 'hypotheses'} for x in rows]))
