"""Post-evaluation motion report; independent future geometry never guides tracking."""
from pathlib import Path
import json,math,hashlib
r=Path.cwd();sha=lambda b:hashlib.sha256(b).hexdigest();rows=[]
for folder,exposure in [('identity149-exposed-v2','exposed generated regression'),('identity149-reserved-v2','reserved generated cohort')]:
 p=r/'workspace-engine/build'/folder/'result.json';data=json.loads(p.read_bytes())
 for case in data['cases']:
  errors=[];unscored=0;segments=set();observations=0
  for frame in case['trace']:
   q=frame['identity149']
   if not q['restored']:continue
   s=q['chosen']['segment'];segments.add((s['sourceKey'],s['startOrdinal']));observations+=1;f=s['nextSample'];i=f['ordinal']
   # Geometry from the frozen source generator, not the proposed tracker boxes.
   visible=i<=35 and not(5<=i<=22 or case['case']=='second-loss' and i==29) and not(case['case']=='wrong-pattern' and i>=23)
   if not visible:unscored+=1;continue
   actual=((128+2*(i-23)+40)/320,(48+40)/240)
   errors.append(math.hypot((f['centerX']-actual[0])*320,(f['centerY']-actual[1])*240))
  rows.append({'source':folder,'exposure':exposure,'sourceSha256':sha(p.read_bytes()),'textureSeed':case['textureSeed'],'case':case['case'],'segments':len(segments),'restoredObservations':observations,'nextSampleComparisons':len(errors),'futureUnavailableOrHidden':unscored,'meanNextSamplePixelError':sum(errors)/len(errors) if errors else None,'maximumNextSamplePixelError':max(errors) if errors else None})
groups=sorted({x['group'] for x in json.loads((r/'docs/publication149/source149.json').read_bytes())['records']})
for group in groups:
 p=r/f'build/occlusion149-{group}-app/result.json';study=json.loads(p.read_bytes());assert study['passed']
 for case in study['runs']:
  if case['policy']!='identity-return-149':continue
  labels={row['ordinal']:row for row in case['audit']['scores']};errors=[];unscored=0;segments=set();observations=0
  for frame in case['run']['trace']:
   q=frame['objects'][0]['tracker']['identity149']
   if not q['restored']:continue
   s=q['chosen']['segment'];segments.add((s['sourceKey'],s['startOrdinal']));observations+=1;f=s['nextSample'];label=labels.get(f['ordinal'])
   if not label or label['visibility'] not in ['visible','partial']:unscored+=1;continue
   b=label['targetBox'];errors.append(math.hypot((f['centerX']-(b[0]+b[2])/2)*640,(f['centerY']-(b[1]+b[3])/2)*480))
  rows.append({'source':case['caseName'],'exposure':case['domain']+', manual source-only boxes','sourceSha256':sha(p.read_bytes()),'segments':len(segments),'restoredObservations':observations,'nextSampleComparisons':len(errors),'futureUnavailableOrHidden':unscored,'meanNextSamplePixelError':sum(errors)/len(errors) if errors else None,'maximumNextSamplePixelError':max(errors) if errors else None})
out={'schema':'observed-motion-149-report-v1','passed':True,'calculatorSha256':sha(Path(__file__).read_bytes()),'rows':rows,'scope':'Future centers independently compared after evaluation. Generated renderer geometry is exact; camera boxes are manual and not instrumented truth. Pixels per sample, not physical velocity or calibrated uncertainty. Hidden/missing future labels stay unscored. No evidence is admitted to tracking by this report.'}
(r/'build/motion149.json').write_bytes((json.dumps(out,indent=2)+'\n').encode());print(json.dumps(out))
