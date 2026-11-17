"""Frozen before reserved154 rendering: outcome, preservation and complete-cost gates."""
from pathlib import Path
import json,statistics
r=Path.cwd()
def read(p):return json.loads((r/p).read_bytes())
g=read('build/study154-generated-final/result.json');c=read('build/study154-exposed150-final/result.json')
assert g['passed'] and c['passed']
checks=[]
for study in [g,c]:
 for case in study['cases']:
  e=case['result']['experiment'];s=e['summary'];scores={x['ordinal']:x for x in e['scores']}
  baseline153Correct=sum(x['rotation']['box'] is not None and scores[x['ordinal']]['visibility'] in ['visible','partial'] and scores[x['ordinal']]['correct'] for x in e['trace'])
  # Independent label geometry is used here only after the entire trace is saved.
  audit=json.loads((r/f"build/study154-{study['group']}-final/project/association-labels134/audits/{e['auditId']}.json").read_bytes())
  state=json.loads((r/f"build/study154-{study['group']}-final/project/world-observations/state.json").read_bytes())['payload']
  original=next(x for x in state['runs'] if x['id']==audit['input']['runId'])
  binding=next(x['bodyId'] for x in original['bindings'] if x['objectKey']==e['objectKey'])
  preserved=0;additions=0
  for t in e['trace']:
   rot=t['rotation'];originalObject=next(x for x in original['trace'][t['ordinal']-1]['objects'] if x['bodyId']==binding)
   if t['origin']=='original-continuity':
    assert originalObject['estimate']['accepted'] and t['box']==originalObject['tracker']['box'];preserved+=1
    if rot['box'] is None:additions+=1
   if rot['box'] is not None:assert t['box'] is not None
   if t['origin']=='confirmed-return-153':assert t['box']==rot['box']
  negative=case['name'] in ['wrong-only','near-copy-only','copy-veto']
  positive=case['name'] in ['return-67','double-return']
  passed=all(s[x]==0 for x in ['overlapFailures','unsupported','confirmedDistractorMatches']) and additions>=3
  if negative:passed &= all(not x['accepted'] for x in e['scores'] if x['ordinal']>=10)
  if positive:passed &= any(x['correct'] for x in e['scores'] if x['ordinal']>=10)
  if case['name']=='double-return':passed &= any(x['correct'] for x in e['scores'] if x['ordinal']>=25)
  if case['name']=='continuous-motion':passed &= s['correct']==s['visible']
  assert len(case['warmTimings'])==3
  checks.append({'case':case['name'],'passed':bool(passed),'summary':s,'preservedOriginalBoxes':preserved,'newlyPreservedComparedWith153':additions,'warmMedianMs':statistics.median(case['warmTimings']),'fullRequestMs':case['result']['totalMs']})
old=read('docs/readmes/153/exposed150-study.json')['cases'][0]['result']['experiment'];camera=c['cases'][0]['result']['experiment']
assert camera['datasetId']==old['datasetId'] and camera['runSha256']==old['runSha256']
assert [t['rotation'] for t in camera['trace']]==[{k:v for k,v in t.items() if k!='imageSha256'} for t in old['trace']]
assert camera['summary']['correct']>=old['summary']['correct']+3
measurements=[read('build/replay154-generated.json'),read('build/replay154-camera.json')]
assert all(x['passed'] and x['fullEvidenceExactlyEqual'] and x['projectUnchanged'] for x in measurements)
for study in [g,c]:
 last=study['cases'][-1];assert statistics.median(last['warmTimings'])<study['forced']['totalMs']
 assert not study['forced']['cost']['reusedReplay'] and not study['restartCost']['reusedReplay']
result={'passed':all(x['passed'] for x in checks),'cases':checks,'unchanged153CameraTrace':True,'completeReplayMeasurements':measurements,'scope':'Recorded evidence and new generated controls. Exact original-box preservation, unchanged153 returns, no extra wrong/hidden/other outputs, byte-bound warm reuse, forced/cold reconstruction and reference/snapshot equivalence. No fresh physical validation or default promotion.'}
(r/'build/qualification154.json').write_bytes((json.dumps(result,indent=2)+'\n').encode());print(json.dumps({'passed':result['passed'],'cases':checks}));assert result['passed'],'Reserved outcome gate failed; preserve original result'
