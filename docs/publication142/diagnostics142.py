"""Descriptive post-run diagnostics only; no tracker calls or qualification changes."""
from pathlib import Path
import json
r=Path.cwd();study=json.loads((r/'build/partial142-app/result.json').read_text(encoding='utf8'));assert study['passed'];rows=[]
for run in study['runs']:
 if run['policy']!='partial-reference-142':continue
 metadata=[f['objects'][0]['tracker']['partial142']['recovery'] for f in run['run']['trace']];attempted=[x for x in metadata if x.get('attempted')]
 rows.append({'case':run['caseName'],'attemptedSearches':len(attempted),'qualifiedUniqueSearches':sum(bool(x.get('unique')) for x in attempted),'combinedSourceSupportPasses':sum(bool(x.get('spatialSupported')) for x in attempted),'maximumConfirmations':max(x.get('confirmations',0) for x in metadata),'maximumRecordedSimilarity':max(x.get('score',0) for x in metadata),'traceCandidateEvaluations':sum(x.get('evaluations',0) for x in metadata)})
data={'scope':'Descriptive retained-trace diagnostics after completed evaluation. Similarity is not calibrated confidence. Trace evaluation counts exclude repeated work from resume, verification and replay. Combined source support is not a separate spatial-only score. No fresh tuning or changed qualification rules.','rows':rows}
(r/'build/partial142-diagnostics.json').write_text(json.dumps(data,indent=2)+'\n',encoding='utf8');print(json.dumps(data))
