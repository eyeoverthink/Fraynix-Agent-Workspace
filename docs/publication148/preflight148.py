"""Source-only eligibility and byte separation. Never reads tracker predictions."""
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
 assert len(donor)==24 and donor[-1]<evaluation[0],'causal separate24-frame donor required'
 start,returned=source['event']['hiddenStart'],source['event']['returnFrame']
 assert 2<=returned-start<=32 and evaluation==list(range(start-10,returned+21)) and len(evaluation)<=64,'fixed full-cover event bounds required'
 selected=donor+evaluation;assert all(1<=i<=len(frames) for i in selected),'source bounds'
 assert source['selectedReviewedSourceFrames']==selected,'all selected original frames require source-only review'
 def visibility(i):
  values=[x['visibility'] for x in source['ranges'] if x['start']<=i<=x['end']]
  assert len(values)==1 and values[0] in VISIBILITY,'complete unambiguous visibility labels required'
  return values[0]
 assert visibility(donor[0])=='visible' and sum(visibility(i)=='visible' for i in donor[1:])>=12,'unobscured reference requirement'
 assert all(visibility(i)=='occluded' for i in range(start,returned)),'complete cover, not merely out of view'
 assert all(visibility(i) in ['visible','partial'] for i in list(range(start-10,start))+list(range(returned,returned+10))),'visible context and re-entry required'
 for i in selected:visibility(i)
 hashes={frames[i-1]['imageSha256'] for i in donor}
 assert not hashes.intersection(frames[i-1]['imageSha256'] for i in evaluation),'reference and evaluation share source image bytes'
 def target(i):
  f=frames[i-1];x,y,w,h=f['originalBox'];W,H=f['width'],f['height'];assert all(math.isfinite(v) for v in [x,y,w,h]) and w>0 and h>0,'valid original geometry required'
  return box([max(0,x/W),max(0,y/H),min(1,(x+w)/W),min(1,(y+h)/H)])
 for i in [donor[0],evaluation[0]]:
  b=target(i);assert min(b[2]-b[0],b[3]-b[1])>=.025,'existing minimum seed size'
 other=source['otherBoxes'];assert source['lookalike']['distinctPhysicalObject'] is True and len(source['lookalike']['similarityRationale'].strip())>=12,'source-only lookalike rationale required'
 labelled=[]
 for key,boxes in other.items():
  i=int(key);assert i in evaluation and boxes,'competitor outside evaluation or empty'
  for b in boxes:box(b)
  labelled.append(i)
 assert len(labelled)>=5 and any(start<=i<returned for i in labelled) and sum(i>=returned for i in labelled)>=3,'lookalike required during cover and after return'
 assert any(i>=returned and all(overlap(target(i),b)<.5 for b in other[str(i)]) for i in labelled),'competitor must be separately visible from target'
 visible=[i for i in evaluation if visibility(i) in ['visible','partial']]
 first=target(evaluation[0]);cx=lambda b:(b[0]+b[2])/2;cy=lambda b:(b[1]+b[3])/2
 displacements=[(math.hypot(cx(target(i))-cx(first),cy(target(i))-cy(first)),i) for i in visible]
 maximum,peak=max(displacements);width=first[2]-first[0]
 assert maximum>=width*.5,'Fresh movement must exceed half the original target width'
 motion={'maxCenterDisplacement':maximum,'displacementInSeedWidths':maximum/width,'peakSourceFrame':peak,'firstReturnDisplacement':math.hypot(cx(target(returned))-cx(first),cy(target(returned))-cy(first)),'scope':'Manual image-plane boxes; not real-world distance'}
 return {'passed':True,'movement':motion,'donorFrames':len(donor),'evaluationFrames':len(evaluation),'fullyHiddenFrames':returned-start,'lookalikeLabelledFrames':len(labelled),'donorEvaluationImagesDisjoint':True,'scope':'Source preflight only; actual donor admission and recovery outcomes remain separate.'}
