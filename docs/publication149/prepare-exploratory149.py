"""Explicit post-acquisition exploratory source; never changes primary eligibility."""
from pathlib import Path
import json,hashlib,base64,math
from preflight149 import validate,box,overlap
r=Path.cwd();out=r/'docs/publication149';root=r/'build/webcam149-private';sha=lambda b:hashlib.sha256(b).hexdigest()
review=json.loads((out/'webcam-labels149.json').read_bytes());s=review['sources'][-1]
assert review['exposure']=='before-tracking' and s['attempt']=='attempt-rqrzd0' and s['eligible'] is False and s['physicalIdentityConfirmedByUser']
assert s['exploratoryDeclaration']=='docs/publication149/EXPLORATORY_CAMERA_149.md'
folder=root/s['attempt'];c=json.loads((folder/'capture.json').read_bytes());frames=c['frames'];assert c['complete'] and c['closed'] and not c['syntheticRecorderCheck']
lock=json.loads((out/'webcam-lock149.json').read_bytes())
for name,h in lock['sourceHashes'].items():assert sha((r/name).read_bytes().replace(b'\r\n',b'\n'))==h,name
assert c['recorderSha256']==lock['sourceHashes']['workspace-desktop/test/webcam149-recorder.cjs']
assert s['donor']==[1,24] and s['evaluation']==[32,124] and s['event']=={'hiddenStart':42,'returnFrame':104}
selected=list(range(1,25))+list(range(32,125));assert selected==s['selectedReviewedSourceFrames'] and s['allReviewedSourceFrames']==list(range(1,135))
assert len(frames)==134 and len((folder/'frames.jsonl').read_text(encoding='utf8').splitlines())==134
for i,f in enumerate(frames,1):
 assert f['sourceFrame']==i and f['file']==f'{i:04d}.jpg'
 raw=(folder/f['file']).read_bytes();assert len(raw)==f['bytes'] and sha(raw)==f['imageSha256']
 if i>1:assert 0<f['seconds']-frames[i-2]['seconds']<=5
assert not {frames[i-1]['imageSha256'] for i in range(1,25)}.intersection(frames[i-1]['imageSha256'] for i in range(32,125))
def vis(i):
 a=[v['visibility'] for v in s['ranges'] if v['start']<=i<=v['end']];assert len(a)==1;return a[0]
def target(i):
 x,y,w,h=s['targetPixelBoxes'][str(i)];assert w>0 and h>0;return box([x/640,y/480,(x+w)/640,(y+h)/480])
assert all(vis(i)=='visible' for i in list(range(1,42))+list(range(104,135))) and all(vis(i)=='occluded' for i in range(42,104))
a,b=target(32),target(104);displacement=math.hypot((b[0]+b[2]-a[0]-a[2])/2,(b[1]+b[3]-a[1]-a[3])/2);assert displacement>=(a[2]-a[0])*.5
for i in range(32,125):
 assert s['otherBoxes'][str(i)]
 for b in s['otherBoxes'][str(i)]:box(b)
 if i>=104:assert all(overlap(target(i),b)<.5 for b in s['otherBoxes'][str(i)])
trial={**s,'eligible':True}
try:validate(trial,{'frames':frames})
except AssertionError as e:assert str(e)==s['failureReason']
else:raise AssertionError('Must remain refused by primary protocol')
manifest=json.loads((out/'source149.json').read_bytes());assert not any(x['group']==s['name'] for x in manifest['records'])
for role,(start,end) in [('reference-source',s['donor']),('evaluation',s['evaluation'])]:
 case=s['name']+'-'+str(start);images=[];labels=[];times=[]
 assert end-start+1<=96
 for ordinal,i in enumerate(range(start,end+1)):
  f=frames[i-1];images.append({'sourceFrame':i,'image':base64.b64encode((folder/f['file']).read_bytes()).decode(),'imageSha256':f['imageSha256']});times.append(f['seconds']-frames[start-1]['seconds'])
  if ordinal:
   row={'ordinal':ordinal,'sourceFrame':i,'imageSha256':f['imageSha256'],'visibility':vis(i),'note':'Manual source-only labels before predictions; exploratory protocol deviation, not primary qualification or instrumented truth.'}
   if vis(i)=='visible':row['targetBox']=target(i)
   if str(i) in s['otherBoxes']:row['otherBoxes']=s['otherBoxes'][str(i)]
   labels.append(row)
 pack={'schema':'fraynix-observation-frames-037-v1','title':case+' / exploratory webcam149 '+role,'source':'Private original continuous webcam samples; exploratory source declaration before tracking','license':'Private user camera recording; do not publish','clock':'sample-index','cameraKey':'webcam149-'+s['attempt'],'timingSource':'Original Chromium mediaTime gaps retained','seconds':times,'objects':[{'key':'subject','label':s['targetDescription'],'box':target(start)}],'frames':images}
 p=root/(case+'.fobs.json');assert not p.exists();p.write_bytes(json.dumps(pack).encode());assert p.stat().st_size<20_000_000
 lab={'schema':'association-labels-134-v1','objectKey':'subject','annotator':'Codex source-only pixel review; user confirms physical actions','method':'ai-source-only','exposure':'before-tracking','provenance':'Exploratory protocol deviation declared before predictions; manual image-plane geometry, not instrumented truth.','rows':labels};lp=out/(case+'-labels.json');assert not lp.exists();lp.write_bytes((json.dumps(lab,indent=2)+'\n').encode())
 manifest['records'].append({'case':case,'group':s['name'],'name':s['targetDescription'],'target':1,'role':role,'domain':'exploratory-real-camera','primaryEligible':False,'start':start,'end':end,'stride':1,'frames':len(images),'width':640,'height':480,'sourceFrames':list(range(start,end+1)),'pack':p.relative_to(r).as_posix(),'packSha256':sha(p.read_bytes()),'labels':lp.relative_to(r).as_posix(),'labelsSha256':sha(lp.read_bytes()),'visibilityCounts':{v:sum(x['visibility']==v for x in labels) for v in ['visible','partial','occluded','out-of-view','uncertain']},'distractorLabelledSamples':sum('otherBoxes'in x for x in labels),'preflight':{'passed':False,'refusal':s['failureReason'],'actualHiddenSamples':62,'primaryMaximum':60,'completeEventPreserved':True,'sourceChecksPassed':True,'declaration':s['exploratoryDeclaration'],'declarationSha256':sha((r/s['exploratoryDeclaration']).read_bytes()),'preparerSha256':sha(Path(__file__).read_bytes()),'firstReturnDisplacementSeedWidths':displacement/(a[2]-a[0]),'scope':'Post-acquisition exploratory comparison only; original primary protocol remains failed.'},'privateImages':True})
manifest['scope']='Unaltered exposed148 regression plus explicitly declared post-acquisition exploratory149 camera source. No primary fresh149 source qualified.'
(out/'source149.json').write_bytes((json.dumps(manifest,indent=2)+'\n').encode())
print(json.dumps({'recordsAdded':2,'primaryEligible':False,'completeEventFrames':93,'frameLimit':96,'fullHiddenSamples':62,'sourceOnly':True}))
