from pathlib import Path
import json,copy,collections
r=Path.cwd();study=json.loads((r/'build/trajectory145-app/result.json').read_text(encoding='utf8'));tail=json.loads((r/'build/trajectory145-tail-app/result.json').read_text(encoding='utf8'));exposed=json.loads((r/'build/exposed145-app/result.json').read_text(encoding='utf8'));assert study['passed'] and tail['passed'] and exposed['passed'];study['runs']+=tail['runs']+exposed['runs'];rows=[]
for run in study['runs']:
 if run['role']!='evaluation' or run['policy']!='trajectory-guard-145':continue
 previous=next(x for x in study['runs'] if x['caseName']==run['caseName'] and x['policy']=='ambiguity-guard-144')
 last_baseline=0;restored=[];blocked=[];preserved=0;conflicts=[];anchors=[];filtered=dict(accepted=0,correct=0,incorrect=0,unknown=0);labels={s["ordinal"]:s for s in run["audit"]["scores"]}
 for i,frame in enumerate(run['run']['trace']):
  t=frame['objects'][0]['tracker'];g=t['trajectory145'];base=previous['run']['trace'][i]['objects'][0]['tracker'];ordinal=i+1
  if t['references143']['baselineAccepted']:last_baseline=ordinal
  if base['box'] is not None:
   exact=copy.deepcopy(t);del exact['trajectory145'];assert exact==base;preserved+=1
  assert len(g['rivals'])<=8
  if 'anchorOrdinal' in g['target']:assert g['target']['anchorOrdinal']==last_baseline
  if g['restored']:
   assert base['box'] is None and g['qualified'] and g['target']['valid'] and not g['conflict']
   assert 0<g['target']['age']<=12 and g['target']['samples']>=4 and g['rivals']
   assert all(p['valid'] and p['samples']>=2 and p['separated'] and p['separationMargin']>0 for p in g['rivals'])
   assert t['ambiguity144']['blocked'] and t['references143']['recovery']['accepted']
   restored.append(ordinal);anchors.append(last_baseline)
   outcome=labels[ordinal]
   if outcome['accepted']:
    filtered['accepted']+=1
    verdict='correct' if outcome['correct'] else 'unknown' if outcome['visibility']=='uncertain' else 'incorrect'
    filtered[verdict]+=1
  if g['blocked']:blocked.append(ordinal)
  if g['conflict']:conflicts.append(ordinal)
 assert len(restored)==run['audit']['trajectory145']['restored']['returned']
 rows.append({'case':run['caseName'],'domain':run['domain'],'restoredOrdinals':restored,'restorationAnchorOrdinals':anchors,'blockedOrdinals':blocked,'conflictOrdinals':conflicts,'all144AcceptedProposalsPreserved':preserved,'summary':run['audit']['summary'],'outcomes':run['audit']['trajectory145']['restored'],'filterAcceptedRestorations':filtered,'activation':{'latchedFrames':sum(f['objects'][0]['tracker']['ambiguity144']['latched'] for f in run['run']['trace']),'eligibleMotionFrames':sum(f['objects'][0]['tracker']['ambiguity144']['latched'] and f['objects'][0]['tracker']['trajectory145']['target']['valid'] for f in run['run']['trace']),'qualifiedFrames':sum(f['objects'][0]['tracker']['trajectory145']['qualified'] for f in run['run']['trace']),'reasons':dict(collections.Counter(f['objects'][0]['tracker']['trajectory145']['reason'] for f in run['run']['trace']))}})
result={'passed':True,'scope':'Independent host invariants: no restored/predicted anchor, fixed horizons/capacities and retained original144 accepted proposals. Original-pixel replay and separately labelled outcome arithmetic are in the app study. No default promotion.','rows':rows}
(r/'build/diagnostics145.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf8');print(json.dumps(result))
