"""Descriptive post-run diagnostics only; no tracker calls or qualification changes."""
from pathlib import Path
import json
r=Path.cwd();study=json.loads((r/'build/confirmation143-app/result.json').read_text(encoding='utf8'));assert study['passed'];rows=[]
for run in study['runs']:
 if run['policy']!='reference-confirmation-143':continue
 frames=[f['objects'][0]['tracker']['references143'] for f in run['run']['trace']];metadata=[f['recovery'] for f in frames];attempted=[x for x in metadata if x.get('attempted')]
 rows.append({'case':run['caseName'],'attemptedSearches':len(attempted),'qualifiedUniqueSearches':sum(any(h['unique'] for h in f['hypotheses']) for f in frames),'confirmedFrames':sum(x.get('qualified',False) for x in metadata),'maximumConfirmations':max(f['maximumConfirmations'] for f in frames),'adjacentUniqueReferenceChanges':sum(a.get('unique',False) and b.get('unique',False) and a.get('sourceKey')!=b.get('sourceKey') for a,b in zip(metadata,metadata[1:])),'maximumRecordedSimilarity':max(x.get('score',0) for x in metadata),'traceCandidateEvaluations':sum(f['search'].get('positionSamples',0) for f in frames),'appearanceEvaluations':sum(f['search'].get('appearanceEvaluations',0) for f in frames),'referenceCountIncludingSeed':1+len(run['run']['referenceMemory137']['examples']),'descriptorRegionComparisons':sum(f['search'].get('regionComparisons',0) for f in frames)})

data={'scope':'Descriptive retained-trace diagnostics after completed evaluation. Similarity is not calibrated confidence. Position counts and five-region comparisons against each retained appearance exclude repeated work from resume, verification and replay. No fresh tuning or changed qualification rules.','rows':rows}
(r/'build/references143-diagnostics.json').write_text(json.dumps(data,indent=2)+'\n',encoding='utf8');print(json.dumps(data))
