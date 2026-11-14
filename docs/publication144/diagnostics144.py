from pathlib import Path
import json,copy
r=Path.cwd();study=json.loads((r/'build/ambiguity144-app/result.json').read_text(encoding='utf8'));assert study['passed'];rows=[]
for run in study['runs']:
 if run['policy']!='ambiguity-guard-144':continue
 trace=run['run']['trace'];rows.append({'case':run['caseName'],'domain':run['domain'],'latchedOrdinals':[f['ordinal'] for f in trace if f['objects'][0]['tracker']['ambiguity144']['latched']],'blockedOrdinals':[f['ordinal'] for f in trace if f['objects'][0]['tracker']['ambiguity144']['blocked']],'clearedByBaselineOrdinals':[f['ordinal'] for f in trace if f['objects'][0]['tracker']['ambiguity144']['clearedByBaseline']],'acceptedSupplementalOrdinals':[f['ordinal'] for f in trace if f['objects'][0]['tracker']['ambiguity144']['accepted']]})
for policy in ['reference-confirmation-143','ambiguity-monitor-144','ambiguity-guard-144']:
 a=next(x for x in study['runs'] if x['caseName']=='clean-full' and x['policy']==policy);b=next(x for x in study['runs'] if x['caseName']=='unseen-copy' and x['policy']==policy)
 memories=[copy.deepcopy(x['run']['referenceMemory137']) for x in [a,b]]
 for m in memories:
  for k in ['targetDatasetId','targetDatasetSha256']:m.pop(k)
 assert memories[0]==memories[1]
 traces=[copy.deepcopy(x['run']['trace']) for x in [a,b]]
 for trace in traces:
  for f in trace:f['objects'][0]['tracker']['references143'].pop('memorySha256')
 assert traces[0]==traces[1],policy
out={'passed':True,'rows':rows,'pairedIdenticalPixelsProduceIdenticalDecisionsAndEstimates':True,'scope':'Post-run descriptive diagnostics; no method or qualification changes. Compare identity-pair traces after excluding only the memory digest, which binds different target manifests. Reference memories must otherwise match after excluding target IDs/hashes. Every original digest is independently reverified by the unchanged audit.'};(r/'build/diagnostics144.json').write_text(json.dumps(out,indent=2)+'\n',encoding='utf8');print(json.dumps(out))
