"""Frozen gates: exact optimization and separate behavioral outcomes. Never relabel failures."""
from pathlib import Path
import json,statistics
r=Path.cwd()
def read(p):return json.loads((r/p).read_bytes())
g=read('build/study157-generated-final/result.json');c=read('build/study157-exposed150-final/result.json');assert g['passed'] and c['passed']
checks=[];measurements=[]
for study in [g,c]:
 for i,case in enumerate(study['cases']):
  e=case['result']['experiment'];s=e['summary'];preserved=0;additions=0
  audit=read(f"build/study157-{study['group']}-final/project/association-labels134/audits/{e['auditId']}.json")
  state=read(f"build/study157-{study['group']}-final/project/world-observations/state.json")['payload'];original=next(x for x in state['runs'] if x['id']==audit['input']['runId']);binding=next(x['bodyId'] for x in original['bindings'] if x['objectKey']==e['objectKey'])
  for t in e['trace']:
   rot=t['rotation'];o=next(x for x in original['trace'][t['ordinal']-1]['objects'] if x['bodyId']==binding)
   if t['origin']=='original-continuity':
    assert o['estimate']['accepted'] and t['box']==o['tracker']['box'];preserved+=1;additions+=rot['box'] is None
   if rot['box'] is not None:assert t['box'] is not None
   if t['origin']=='confirmed-return-153':assert t['box']==rot['box']
  n=case['name'];behavior=all(s[k]==0 for k in ['overlapFailures','unsupported','confirmedDistractorMatches'])
  if n in ['wrong-only','near-copy-only','copy-veto']:behavior &= all(not x['accepted'] for x in e['scores'] if x['ordinal']>=18)
  if n in ['long-cover-return','partial-before-return']:behavior &= any(x['correct'] for x in e['scores'] if x['ordinal']>=18)
  if n=='double-return':behavior &= any(x['correct'] for x in e['scores'] if 12<=x['ordinal']<24) and any(x['correct'] for x in e['scores'] if x['ordinal']>=30)
  if n=='reversing-motion':behavior &= s['correct']==s['visible']
  name='camera' if study['group']=='exposed150' else 'generated' if i==0 else n;m=read(f'build/replay157-{name}.json');assert m['passed'] and m['fullEvidenceExactlyEqual'] and m['projectUnchanged'];measurements.append({'case':n,**m})
  assert len(case['warmTimings'])==3
  checks.append({'case':n,'passed':True,'behavioralTargetPassed':bool(behavior),'summary':s,'preservedOriginalBoxes':preserved,'newlyPreservedComparedWith153':additions,'warmMedianMs':statistics.median(case['warmTimings']),'fullRequestMs':case['result']['totalMs'],'exactReferenceEvidence':True})
old=read('docs/readmes/156/exposed150-study.json')['cases'][0]['result']['experiment'];camera=c['cases'][0]['result']['experiment']
for key in ['trace','scores','summary','datasetId','datasetSha256','runSha256','auditId','auditSha256','baselineSummary','referenceCount','searchEvaluations']:assert camera[key]==old[key],key
for study in [g,c]:
 assert not study['forced']['cost']['reusedReplay'] and not study['restartCost']['reusedReplay'];assert statistics.median(study['cases'][-1]['warmTimings'])<study['forced']['totalMs']
cost=[]
for m in measurements:
 ref=[x['totalMs'] for x in m['measurements'] if not x['precomputedOriginalReplay']];fast=[x['totalMs'] for x in m['measurements'] if x['precomputedOriginalReplay']]
 cost.append({'case':m['case'],'pairs':len(ref),'referenceMedianMs':statistics.median(ref),'precomputedMedianMs':statistics.median(fast),'medianRatio':statistics.median(ref)/statistics.median(fast),'measuredReduction':statistics.median(fast)<statistics.median(ref)})
result={'passed':True,'behavioralTargetsAllPassed':all(x['behavioralTargetPassed'] for x in checks),'cases':checks,'unchanged156CameraEvidence':True,'completeReplayMeasurements':measurements,'costSummary':cost,'scope':'All exact reference/optimized evidence checks must pass. Harder behavioral goals and measured timing benefit reported separately, including failures. No defaults or thresholds changed. Exposed camera regression and new generated controls only.'}
(r/'build/qualification157.json').write_bytes((json.dumps(result,indent=2)+'\n').encode());print(json.dumps({k:v for k,v in result.items() if k!='completeReplayMeasurements'}))
