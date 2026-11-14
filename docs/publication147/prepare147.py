"""Commit source-only event labels and identical comparison packs; never runs tracking."""
from pathlib import Path
import json,hashlib,zipfile,base64,datetime
from preflight147 import validate
r=Path.cwd();root=r/'build/otb147';out=r/'docs/publication147';sha=lambda b:hashlib.sha256(b).hexdigest();lock=r/'docs/OBSERVATION_147_LOCK.json'
records=[];annotations=[];review=json.loads((out/'visibility147.json').read_text(encoding='utf8'))
for source in review['sources']:
 if not source['eligible']:continue
 name=source['name'];idx=json.loads((root/(name+'-review')/'source-review-index.json').read_text(encoding='utf8'));z=zipfile.ZipFile(root/(name+'.zip'));w,h=idx['frames'][0]['width'],idx['frames'][0]['height'];raw=(out/(name+'-groundtruth.txt')).read_bytes();annotations.append({'path':'docs/publication147/'+name+'-groundtruth.txt','sha256':sha(raw),'bytes':len(raw)})
 preflight=validate(source,idx)
 def visibility(i):
  hits=[v['visibility'] for v in source['ranges'] if v['start']<=i<=v['end']];assert len(hits)==1,(name,i);return hits[0]
 def box(i):
  x,y,bw,bh=idx['frames'][i-1]['originalBox'];b=[max(0,x/w),max(0,y/h),min(1,(x+bw)/w),min(1,(y+bh)/h)];assert b[2]-b[0]>=.025 and b[3]-b[1]>=.025,(name,i,b);return b
 hiddenStart,returnFrame=source['event']['hiddenStart'],source['event']['returnFrame'];assert 2<=returnFrame-hiddenStart<=32
 assert all(visibility(i) in ['occluded','out-of-view'] for i in range(hiddenStart,returnFrame))
 assert all(visibility(i) in ['visible','partial'] for i in list(range(hiddenStart-10,hiddenStart))+list(range(returnFrame,returnFrame+10)))
 assert source['evaluation']==[hiddenStart-10,returnFrame+20]
 donorStart,donorEnd=source['donor'];assert donorEnd-donorStart+1==24
 if source.get('supplementalNoReferenceDiagnostic'):
  assert not source['eligible'] and all(visibility(i)=='partial' for i in range(donorStart,donorEnd+1))
 else:
  assert visibility(donorStart)=='visible' and sum(visibility(i)=='visible' for i in range(donorStart,donorEnd+1))>=12
 assert donorEnd<source['evaluation'][0] or donorStart>source['evaluation'][1]
 for role,(start,end) in [('reference-source',source['donor']),('evaluation',source['evaluation'])]:
  assert end-start+1<=64;case=name.lower()+'-'+str(start);frames=[];labels=[]
  for ordinal,i in enumerate(range(start,end+1)):
   row=idx['frames'][i-1];raw=z.read(row['archivePath']);digest=sha(raw);assert digest==row['imageSha256'];frames.append({'sourceFrame':i,'image':base64.b64encode(raw).decode(),'imageSha256':digest})
   if ordinal==0:continue
   v=visibility(i);label={'ordinal':ordinal,'sourceFrame':i,'imageSha256':digest,'visibility':v,'note':'External benchmark geometry and source-only AI visibility review; no prediction input or independent human adjudication.'}
   if v in ['visible','partial']:label['targetBox']=box(i)
   if str(i) in source.get('otherBoxes',{}):label['otherBoxes']=source['otherBoxes'][str(i)]
   labels.append(label)
  pack={'schema':'fraynix-observation-frames-036-v1','title':case+' / real-camera147 '+role,'source':'Original OTB source / https://rvlab.snu.ac.kr/research/tracker_benchmark/','license':'Research benchmark; authors retain rights. Original archive not redistributed.','clock':'source-frame','objects':[{'key':'subject','label':name+' target','box':box(start)}],'frames':frames};p=root/(case+'.fobs.json');p.write_bytes(json.dumps(pack).encode());assert p.stat().st_size<20_000_000
  lab={'schema':'association-labels-134-v1','objectKey':'subject','annotator':'OTB authors (rectangles); Codex source-only AI (visibility and marked competing objects)','method':'external-annotations','exposure':'before-tracking','provenance':('Supplemental baseline-only full-occlusion diagnostic; excluded from primary qualification, all donor frames partial. ' if source.get('supplementalNoReferenceDiagnostic') else 'Predeclared earliest eligible disappearance/return. ')+'Original source frames reviewed before predictions. No human adjudication.','rows':labels};lp=out/(case+'-labels.json');lp.write_text(json.dumps(lab,indent=2)+'\n',encoding='utf8')
  records.append({'case':case,'group':name.lower(),'name':name,'target':1,'role':role,'domain':'real-camera','primaryEligible':source['eligible'],'supplementalNoReferenceDiagnostic':source.get('supplementalNoReferenceDiagnostic',False),'start':start,'end':end,'stride':1,'frames':len(frames),'width':w,'height':h,'sourceFrames':list(range(start,end+1)),'pack':p.relative_to(r).as_posix(),'packSha256':sha(p.read_bytes()),'labels':lp.relative_to(r).as_posix(),'labelsSha256':sha(lp.read_bytes()),'visibilityCounts':{v:sum(l['visibility']==v for l in labels) for v in ['visible','partial','occluded','out-of-view','uncertain']},'distractorLabelledSamples':sum('otherBoxes' in l for l in labels),'originalGroundTruth':annotations[-1]['path'],'originalGroundTruthSha256':annotations[-1]['sha256']})
for group in {x['group'] for x in records}:
 donor=next(x for x in records if x['group']==group and x['role']=='reference-source');hashes={f['imageSha256'] for f in json.loads((r/donor['pack']).read_bytes())['frames']}
 for target in [x for x in records if x['group']==group and x['role']=='evaluation']:assert not hashes.intersection(f['imageSha256'] for f in json.loads((r/target['pack']).read_bytes())['frames'])
manifest={'createdAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'algorithmLockSha256':sha(lock.read_bytes()),'records':records,'downloads':json.loads((root/'download.json').read_bytes()),'sourceReview':review,'limitations':['Source-only eligibility is separate from actual donor admission and useful recovery.','External geometry and attributed AI visibility/competitor review are not independent human adjudication.','No camera physical-scale or semantic-identity certification.']}
for name,value in [('annotation-integrity147.json',annotations),('source147.json',manifest)]: (out/name).write_bytes((json.dumps(value,indent=2)+'\n').encode('utf8'))
print(json.dumps([{'case':x['case'],'group':x['group'],'role':x['role'],'counts':x['visibilityCounts']} for x in records]))
