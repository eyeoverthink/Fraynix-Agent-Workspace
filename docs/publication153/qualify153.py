"""Outcome gates declared before the five reserved153 sequences are rendered."""
from pathlib import Path
import json,statistics,hashlib
r=Path.cwd()
g=json.loads((r/'build/study153-generated-final/result.json').read_bytes())
c=json.loads((r/'build/study153-exposed150-final/result.json').read_bytes())
assert g['passed'] and c['passed']
rows=[]
for case in g['cases']:
 e=case['result']['experiment'];s=e['summary'];returned=[x for x in e['scores'] if x['ordinal']>=10]
 accepted=sum(x['accepted'] for x in returned);correct=sum(x['accepted'] and x['correct'] for x in returned)
 positive=case['name'] in ['return-53','moving-return-minus113']
 passed=(s['overlapFailures']==0 and s['unsupported']==0 and s['confirmedDistractorMatches']==0 and (correct>0 if positive else accepted==0))
 rows.append({'case':case['name'],'passed':passed,'acceptedReturns':accepted,'correctReturns':correct,'summary':s})
camera=c['cases'][0]['result']['experiment'];old=json.loads((r/'docs/readmes/152/exposed150-study.json').read_bytes())['cases'][0]['result']['experiment']
assert camera['datasetId']==old['datasetId'] and camera['runSha256']==old['runSha256']
assert [x['imageSha256'] for x in camera['trace']]==[x['imageSha256'] for x in old['trace']]
cameraPass=all(camera['summary'][x]==0 for x in ['overlapFailures','unsupported','confirmedDistractorMatches'])
measure=json.loads((r/'build/recorded153-measured.json').read_bytes());assert measure['passed'] and measure['exactTraceComparison']
actual=[{k:v for k,v in x.items() if k!='imageSha256'} for x in camera['trace']]
assert actual==measure['SCALAR']['trace']==measure['PRECOMPUTED']['trace']
paired={mode:[v['ms'] for v in measure['pairedTimings'] if v['mode']==mode] for mode in ['SCALAR','PRECOMPUTED']}
assert all(len(v)==3 for v in paired.values());ratio=statistics.median(paired['SCALAR'])/statistics.median(paired['PRECOMPUTED'])
controls=json.loads((r/'build/generated153-arithmetic.json').read_bytes());assert controls['passed']
result={'passed':all(x['passed'] for x in rows) and cameraPass and ratio>1,'generated':rows,'cameraSafeOnExposedLabels':cameraPass,'cameraAcceptedReturns':sum(x['accepted'] and x['correct'] for x in camera['scores'] if x['ordinal']>=43),'pairedSearchMs':paired,'medianSearchSpeedup':ratio,'fullCameraTraceExactlyEqual':True,'generatedArithmetic':controls,'scope':'New rendered controls and exposed original camera, independently scored. Search arithmetic timings exclude source replay; actual app requests include verification. No fresh physical footage or default promotion.'}
(r/'build/qualification153.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf8')
print(json.dumps({k:v for k,v in result.items() if k not in ['generatedArithmetic','generated']}));assert result['passed'],'Reserved outcome or cost gate failed; preserve the result'
