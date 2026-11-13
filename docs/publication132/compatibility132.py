"""Compare actual current traces against published 131 evidence, not a rebuilt baseline."""
from pathlib import Path
import json,gzip,hashlib
r=Path.cwd();study=json.loads((r/'build/labels132-app/result.json').read_text(encoding='utf8'));assert study['passed']
index=json.loads((r/'docs/readmes/131/original-evidence.json').read_text(encoding='utf8'))['files']
rows=[]
for c in study['comparisons']:
 method={'legacy-flow':'flow','renewing-flow-131':'renewing-flow'}[c['policy']]
 logical=f"build/diagnostic131/held-{c['window']}-{method}.json"
 entry=next(x for x in index if x['logicalPath']==logical)
 packed=(r/entry['path']).read_bytes();assert len(packed)==entry['bytes'] and hashlib.sha256(packed).hexdigest()==entry['sha256']
 raw=gzip.decompress(packed);assert len(raw)==entry['rawBytes'] and hashlib.sha256(raw).hexdigest()==entry['rawSha256']
 old=json.loads(raw);dataset=next(d for d in study['datasets'] if d['window']==c['window'])
 assert old['sourceSha256']==dataset['packSha256']
 assert len(c['run']['trace'])==len(old['rows'])==95
 for frame,prior in zip(c['run']['trace'],old['rows']):
  assert frame['ordinal']==prior['ordinal'] and frame['sourceFrame']==prior['sourceFrame']
  current=frame['objects'][0]
  assert current['estimate']==prior['estimate'],(logical,frame['ordinal'],'estimate')
  # App binds the same initial source rectangle to a persistent body's UUID and source label.
  # The standalone historical study used literal subject/human supplied seed. Numeric evidence stays exact.
  a={k:v for k,v in current['tracker'].items() if k not in ['id','label']}
  b={k:v for k,v in prior['tracker'].items() if k not in ['id','label']}
  assert a==b,(logical,frame['ordinal'],'tracker')
 rows.append({'window':c['window'],'policy':c['policy'],'frames':95,'priorEvidenceRawSha256':entry['rawSha256'],'sourcePackSha256':dataset['packSha256']})
result={'passed':True,'frames':sum(x['frames'] for x in rows),'comparisons':rows,'scope':'Exact estimates and complete numeric tracker evidence match published131 traces on identical source bytes. Only declared body identity and descriptive label differ between historical standalone study and persistent app. No tracking/forecast default changed; labels are downstream.'}
(r/'build/compatibility132.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf8');print(json.dumps({'passed':True,'frames':result['frames']}))
