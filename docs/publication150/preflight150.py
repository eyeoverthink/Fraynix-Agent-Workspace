"""Prospective source-only eligibility. No predictions or detector scores are read."""
import math
VISIBILITY={'visible','partial','occluded','out-of-view','uncertain'}
def box(b):
 assert len(b)==4 and all(isinstance(v,(int,float)) and math.isfinite(v) for v in b),'finite rectangle required'
 assert 0<=b[0]<b[2]<=1 and 0<=b[1]<b[3]<=1,'rectangle outside source'
 return b
def overlap(a,b):
 i=max(0,min(a[2],b[2])-max(a[0],b[0]))*max(0,min(a[3],b[3])-max(a[1],b[1]));return i/((a[2]-a[0])*(a[3]-a[1])+(b[2]-b[0])*(b[3]-b[1])-i)
def validate(source,index):
 assert source['eligible'] is True,'source not declared eligible'
 frames=index['frames'];donor=list(range(source['donor'][0],source['donor'][1]+1));evaluation=list(range(source['evaluation'][0],source['evaluation'][1]+1))
 assert donor==list(range(1,25)) and evaluation==list(range(25,len(frames)+1)),'Keep exact1-24 donor and complete25-end evaluation'
 assert 1<=len(evaluation)<=96,'Existing96-frame capacity'
 selected=donor+evaluation;assert source['selectedReviewedSourceFrames']==selected,'All originals require source-only review'
 def visibility(i):
  values=[x['visibility'] for x in source['ranges'] if x['start']<=i<=x['end']]
  assert len(values)==1 and values[0] in VISIBILITY,'complete unambiguous visibility labels required'
  return values[0]
 for i in selected:visibility(i)
 assert all(visibility(i)=='visible' for i in donor),'Clear donor required, including its starting observations'
 assert all(visibility(i)=='visible' for i in range(25,30)),'Unobscured evaluation start required'
 start,returned=source['event']['hiddenStart'],source['event']['returnFrame'];hidden_end=source['event'].get('hiddenEnd',returned)
 assert 34<=start<hidden_end<=returned<=len(frames) and 18<=hidden_end-start<=60,'Full-cover event bounds18-60'
 assert all(visibility(i)=='occluded' for i in range(start,hidden_end)),'Complete cover required'
 assert returned-hidden_end<=5 and all(visibility(i) in ['partial','visible'] for i in range(hidden_end,returned)),'At most five recorded transition samples before full return'
 assert visibility(returned)=='visible','First return must be fully visible'
 assert sum(visibility(i)=='visible' for i in range(returned,len(frames)+1))>=20,'Twenty clear return samples required'
 hashes={frames[i-1]['imageSha256'] for i in donor}
 assert not hashes.intersection(frames[i-1]['imageSha256'] for i in evaluation),'Reference and evaluation share pixels'
 def target(i):
  f=frames[i-1];x,y,w,h=f['originalBox'];W,H=f['width'],f['height']
  assert all(math.isfinite(v) for v in [x,y,w,h]) and w>0 and h>0,'Valid original geometry required'
  return box([x/W,y/H,(x+w)/W,(y+h)/H])
 for i in [1,25]:
  b=target(i);assert min(b[2]-b[0],b[3]-b[1])>=.025,'Existing minimum seed size'
 other=source['otherBoxes'];assert source['lookalike']['distinctPhysicalObject'] is True and len(source['lookalike']['similarityRationale'].strip())>=12,'Source-only distractor rationale required'
 labelled=[]
 for key,boxes in other.items():
  i=int(key);assert i in evaluation and boxes,'Competitor outside evaluation or empty'
  for b in boxes:box(b)
  labelled.append(i)
 assert len(labelled)>=5 and any(start<=i<returned for i in labelled) and sum(i>=returned for i in labelled)>=3,'Distractor needed during cover and return'
 assert all(overlap(target(i),b)<.5 for i in labelled if i>=returned and visibility(i)=='visible' for b in other[str(i)]),'Returned target and comparison must be separately visible'
 first=target(25);final=target(returned);W,H=frames[24]['width'],frames[24]['height']
 delta=math.hypot(((final[0]+final[2])-(first[0]+first[2]))*W/2,((final[1]+final[3])-(first[1]+first[3]))*H/2)
 threshold=max(22,(first[2]-first[0])*W/2)
 assert delta>=threshold,'First fully visible return must move at least max22px/half-width'
 return {'passed':True,'donorFrames':24,'evaluationFrames':len(evaluation),'fullyHiddenFrames':hidden_end-start,'returnTransitionFrames':returned-hidden_end,'lookalikeLabelledFrames':len(labelled),'donorEvaluationImagesDisjoint':True,'movement':{'firstReturnPixels':delta,'minimumPixels':threshold},'scope':'Source-only manual image-plane eligibility; original capture and all source frames retained. Not tracking success or instrumented physical truth.'}
