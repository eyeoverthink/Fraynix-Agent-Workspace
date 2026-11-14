from pathlib import Path
import json
r=Path.cwd();study=json.loads((r/'build/ambiguity144-app/result.json').read_text(encoding='utf8'));assert study['passed'];rows=[]
for run in study['runs']:
 if run['policy']!='ambiguity-guard-144':continue
 trace=run['run']['trace'];rows.append({'case':run['caseName'],'domain':run['domain'],'latchedOrdinals':[f['ordinal'] for f in trace if f['objects'][0]['tracker']['ambiguity144']['latched']],'blockedOrdinals':[f['ordinal'] for f in trace if f['objects'][0]['tracker']['ambiguity144']['blocked']],'clearedByBaselineOrdinals':[f['ordinal'] for f in trace if f['objects'][0]['tracker']['ambiguity144']['clearedByBaseline']],'acceptedSupplementalOrdinals':[f['ordinal'] for f in trace if f['objects'][0]['tracker']['ambiguity144']['accepted']]})
for policy in ['reference-confirmation-143','ambiguity-monitor-144','ambiguity-guard-144']:
 a=next(x for x in study['runs'] if x['caseName']=='clean-full' and x['policy']==policy);b=next(x for x in study['runs'] if x['caseName']=='unseen-copy' and x['policy']==policy);assert a['run']['trace']==b['run']['trace'],policy
out={'passed':True,'rows':rows,'pairedIdenticalPixelsProduceIdenticalTraces':True,'scope':'Post-run descriptive diagnostics; no changes to frozen method or qualification. Identity-pair traces identical despite different independently declared hidden identities.'};(r/'build/diagnostics144.json').write_text(json.dumps(out,indent=2)+'\n',encoding='utf8');print(json.dumps(out))
