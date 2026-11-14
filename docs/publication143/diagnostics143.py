"""Descriptive post-run diagnostics only; no tracker calls or qualification changes."""
from pathlib import Path
import json
r=Path.cwd();study=json.loads((r/'build/confirmation143-app/result.json').read_text(encoding='utf8'));assert study['passed'];rows=[]
for run in study['runs']:
 if run['policy']!='reference-confirmation-143':continue
 metadata=[f['objects'][0]['tracker']['references143']['recovery'] for f in run['run']['trace']];attempted=[x for x in metadata if x.get('attempted')]
 rows.append({'case':run['caseName'],'attemptedSearches':len(attempted),'qualifiedUniqueSearches':sum(bool(x.get('unique')) for x in attempted),'confirmedFrames':sum(x.get('confirmations',0)>=3 for x in attempted),'maximumConfirmations':max(x.get('confirmations',0) for x in metadata),'adjacentUniqueReferenceChanges':sum(a.get('unique',False) and b.get('unique',False) and a.get('sourceKey')!=b.get('sourceKey') for a,b in zip(metadata,metadata[1:])),'maximumRecordedSimilarity':max(x.get('score',0) for x in metadata),'traceCandidateEvaluations':sum(x.get('evaluations',0) for x in metadata),'referenceCountIncludingSeed':1+len(run['run']['referenceMemory137']['examples']),'descriptorRegionComparisons':sum(x.get('evaluations',0) for x in metadata)*5*(1+len(run['run']['referenceMemory137']['examples']))})
data={'scope':'Descriptive retained-trace diagnostics after completed evaluation. Similarity is not calibrated confidence. Position counts and five-region comparisons against each retained appearance exclude repeated work from resume, verification and replay. No fresh tuning or changed qualification rules.','rows':rows}
(r/'build/references143-diagnostics.json').write_text(json.dumps(data,indent=2)+'\n',encoding='utf8');print(json.dumps(data))
