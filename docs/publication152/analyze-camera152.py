"""Retrospective labels score saved proposals; they never enter the original search."""
from pathlib import Path
import json
r=Path.cwd();study=json.loads((r/'build/study152-exposed150-final/result.json').read_bytes());e=study['cases'][0]['result']['experiment']
project=r/'build/study152-exposed150-final/project';audit=json.loads((project/'association-labels134/audits'/f"{e['auditId']}.json").read_bytes())
previous=json.loads((r/'docs/readmes/151/webcam150-displaced-return-study.json').read_bytes())['cases'][0]['diagnosis'];assert previous['auditId']==e['auditId']
def iou(a,b):
 i=max(0,min(a[2],b[2])-max(a[0],b[0]))*max(0,min(a[3],b[3])-max(a[1],b[1]));return i/((a[2]-a[0])*(a[3]-a[1])+(b[2]-b[0])*(b[3]-b[1])-i)
rows=[];hidden=False;maximum=0;vetoes=0
for label in audit['scores']:
 if label['visibility'] in ['occluded','out-of-view']:hidden=True;continue
 if not hidden or label['visibility']!='visible':continue
 t=e['trace'][label['ordinal']-1];near=[h for h in t['hypotheses'] if iou(h['box'],label['targetBox'])>=.5]
 maximum=max(maximum,max([h['confirmations'] for h in t['hypotheses']]+[0]));vetoes+=int(t['ambiguityRetained'])
 rows.append({'sourceFrame':label['sourceFrame'],'searchNearTarget':bool(near),'strongNearTarget':any(h['strong'] for h in near),'accepted':t['box'] is not None,
  'maximumOverlap':max([iou(h['box'],label['targetBox']) for h in t['hypotheses']]+[0]),'nearScores':[{'score':h['score'],'gradient':h['gradient'],'degrees':h['pose']['degrees'],'failedChecks':h.get('failedChecks'),'confirmations':h['confirmations']} for h in near]})
summary={'returned':len(rows),'searchNearTarget':sum(y['searchNearTarget'] for y in rows),'strongNearTarget':sum(y['strongNearTarget'] for y in rows),'acceptedReturns':sum(y['accepted'] for y in rows),'maximumConfirmations':maximum,'ambiguityFrames':vetoes,'original149SearchNearTarget':previous['summary']['searchOverlapsTarget']}
result={'auditId':e['auditId'],'experimentId':e['id'],'summary':summary,'rows':rows,'scope':'Exposed retrospective proposal coverage. Independent labels used only after frozen search completed. No added observations, appearance admissions or physical pose claims.'}
(r/'build/camera-search152.json').write_bytes((json.dumps(result,indent=2)+'\n').encode());print(json.dumps(summary))
