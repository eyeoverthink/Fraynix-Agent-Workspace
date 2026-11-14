"""Prepare private camera packs from source-only manual labels, never predictions."""
from pathlib import Path
import json,hashlib,base64,datetime
from preflight147 import validate

r=Path.cwd();out=r/'docs/publication147';root=r/'build/webcam147-private'
sha=lambda b:hashlib.sha256(b).hexdigest()
review=json.loads((out/'webcam-labels147.json').read_bytes())
assert review['exposure']=='before-tracking'
records=[];receipts=[]
for source in review['sources']:
 folder=(root/source['attempt']).resolve();assert folder.parent==root.resolve()
 raw=(folder/'capture.json').read_bytes();capture=json.loads(raw)
 assert capture['private'] and capture['closed'] and capture['complete'],'Incomplete attempt cannot qualify'
 frames=capture['frames'];assert 1<=len(frames)<=600
 assert len(frames)==len((folder/'frames.jsonl').read_text(encoding='utf8').splitlines())
 assert sum(x['bytes'] for x in frames)==capture['bytes']<=120000000
 index={'frames':[]};seconds=[]
 for i,f in enumerate(frames,1):
  assert f['sourceFrame']==i and f['file']==f'{i:04d}.jpg'
  image=(folder/f['file']).read_bytes();assert sha(image)==f['imageSha256'] and len(image)==f['bytes']
  seconds.append(f['seconds']);assert i==1 or seconds[-1]>seconds[-2]
  index['frames'].append({**f,'originalBox':source['targetPixelBoxes'].get(str(i))})
 receipts.append({'attempt':source['attempt'],'captureSha256':sha(raw),'frameJournalSha256':sha((folder/'frames.jsonl').read_bytes()),'samples':len(frames),'bytes':capture['bytes'],'eligible':source['eligible'],'events':capture['events'],'frames':[{k:f[k] for k in ['sourceFrame','imageSha256','mediaTime','seconds','width','height','bytes']} for f in frames]})
 if not source['eligible']:continue
 preflight=validate(source,index)
 assert source['physicalIdentityConfirmedByUser'] is True
 def visibility(i):return next(v['visibility'] for v in source['ranges'] if v['start']<=i<=v['end'])
 def box(i):
  f=index['frames'][i-1];x,y,w,h=f['originalBox'];W,H=f['width'],f['height'];b=[x/W,y/H,(x+w)/W,(y+h)/H]
  assert 0<=b[0]<b[2]<=1 and 0<=b[1]<b[3]<=1;return b
 for role,(start,end) in [('reference-source',source['donor']),('evaluation',source['evaluation'])]:
  assert end-start+1<=64;case=source['name']+'-'+str(start);images=[];labels=[];times=[]
  for ordinal,i in enumerate(range(start,end+1)):
   f=frames[i-1];images.append({'sourceFrame':i,'image':base64.b64encode((folder/f['file']).read_bytes()).decode(),'imageSha256':f['imageSha256']});times.append(f['seconds']-frames[start-1]['seconds'])
   if ordinal:assert 0<times[-1]-times[-2]<=5,'Keep real gaps; do not splice or retime source'
   if not ordinal:continue
   v=visibility(i);label={'ordinal':ordinal,'sourceFrame':i,'imageSha256':f['imageSha256'],'visibility':v,'note':'Manual source-only camera annotation before predictions; user confirms physical objects/actions. Not independent instrumented ground truth.'}
   if v in ['visible','partial']:label['targetBox']=box(i)
   if str(i) in source.get('otherBoxes',{}):label['otherBoxes']=source['otherBoxes'][str(i)]
   labels.append(label)
  pack={'schema':'fraynix-observation-frames-037-v1','title':case+' / private webcam147 '+role,'source':'User-authorized fixed webcam; original continuous camera samples; no model or tracking during capture','license':'Private user camera recording; do not publish','clock':'sample-index','cameraKey':'webcam147-'+source['attempt'],'timingSource':'Chromium requestVideoFrameCallback mediaTime; actual variable gaps retained','seconds':times,'objects':[{'key':'subject','label':source['targetDescription'],'box':box(start)}],'frames':images}
  p=root/(case+'.fobs.json');p.write_bytes(json.dumps(pack).encode());assert p.stat().st_size<20_000_000
  lab={'schema':'association-labels-134-v1','objectKey':'subject','annotator':'Codex source-only manual pixel review; physical setup confirmed by user','method':'ai-source-only','exposure':'before-tracking','provenance':'Fixed webcam study, independent of predictions. Not externally instrumented geometry or calibrated physical identity.','rows':labels};lp=out/(case+'-labels.json');lp.write_bytes((json.dumps(lab,indent=2)+'\n').encode())
  records.append({'case':case,'group':source['name'],'name':source['targetDescription'],'target':1,'role':role,'domain':'real-camera','primaryEligible':True,'start':start,'end':end,'stride':1,'frames':len(images),'width':frames[start-1]['width'],'height':frames[start-1]['height'],'sourceFrames':list(range(start,end+1)),'pack':p.relative_to(r).as_posix(),'packSha256':sha(p.read_bytes()),'labels':lp.relative_to(r).as_posix(),'labelsSha256':sha(lp.read_bytes()),'visibilityCounts':{v:sum(l['visibility']==v for l in labels) for v in ['visible','partial','occluded','out-of-view','uncertain']},'distractorLabelledSamples':sum('otherBoxes' in l for l in labels),'preflight':preflight,'privateImages':True})
manifest={'createdAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'algorithmLockSha256':sha((r/'docs/OBSERVATION_147_LOCK.json').read_bytes()),'webcamLockSha256':sha((out/'webcam-lock147.json').read_bytes()),'records':records,'privateCameraReceipts':receipts,'sourceReview':review,'limitations':['Manual source-only annotations are independent of predictions but not independently instrumented truth.','2 Hz camera sampling and fixed planar view do not establish general identity or calibrated real-world accuracy.','Private source images remain local; hashes/labels only published.','Public benchmark protocol remains separate.']}
(out/'source147.json').write_bytes((json.dumps(manifest,indent=2)+'\n').encode('utf8'))
print(json.dumps({'records':len(records),'attempts':len(receipts),'sourceOnly':True}))
