"""Commit source-only event labels and identical comparison packs; never runs tracking."""
from pathlib import Path
import json,hashlib,zipfile,base64,datetime
r=Path.cwd();root=r/'build/otb146';out=r/'docs/publication146';sha=lambda b:hashlib.sha256(b).hexdigest();lock=r/'docs/OBSERVATION_146_LOCK.json'
records=[];annotations=[];review=json.loads((out/'visibility146.json').read_text(encoding='utf8'))
for source in review['sources']:
 if not source['eligible'] and not source.get('supplementalNoReferenceDiagnostic'):continue
 name=source['name'];idx=json.loads((root/(name+'-review')/'source-review-index.json').read_text(encoding='utf8'));z=zipfile.ZipFile(root/(name+'.zip'));w,h=idx['frames'][0]['width'],idx['frames'][0]['height'];raw=(out/(name+'-groundtruth.txt')).read_bytes();annotations.append({'path':'docs/publication146/'+name+'-groundtruth.txt','sha256':sha(raw),'bytes':len(raw)})
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
  pack={'schema':'fraynix-observation-frames-036-v1','title':case+' / real-camera146 '+role,'source':'Original OTB source / https://rvlab.snu.ac.kr/research/tracker_benchmark/','license':'Research benchmark; authors retain rights. Original archive not redistributed.','clock':'source-frame','objects':[{'key':'subject','label':name+' target','box':box(start)}],'frames':frames};p=root/(case+'.fobs.json');p.write_bytes(json.dumps(pack).encode());assert p.stat().st_size<20_000_000
  lab={'schema':'association-labels-134-v1','objectKey':'subject','annotator':'OTB authors (rectangles); Codex source-only AI (visibility and marked competing objects)','method':'external-annotations','exposure':'before-tracking','provenance':('Supplemental baseline-only full-occlusion diagnostic; excluded from primary qualification, all donor frames partial. ' if source.get('supplementalNoReferenceDiagnostic') else 'Predeclared earliest eligible disappearance/return. ')+'Original source frames reviewed before predictions. No human adjudication.','rows':labels};lp=out/(case+'-labels.json');lp.write_text(json.dumps(lab,indent=2)+'\n',encoding='utf8')
  records.append({'case':case,'group':name.lower(),'name':name,'target':1,'role':role,'domain':'real-camera','primaryEligible':source['eligible'],'supplementalNoReferenceDiagnostic':source.get('supplementalNoReferenceDiagnostic',False),'start':start,'end':end,'stride':1,'frames':len(frames),'width':w,'height':h,'sourceFrames':list(range(start,end+1)),'pack':p.relative_to(r).as_posix(),'packSha256':sha(p.read_bytes()),'labels':lp.relative_to(r).as_posix(),'labelsSha256':sha(lp.read_bytes()),'visibilityCounts':{v:sum(l['visibility']==v for l in labels) for v in ['visible','partial','occluded','out-of-view','uncertain']},'distractorLabelledSamples':sum('otherBoxes' in l for l in labels),'originalGroundTruth':annotations[-1]['path'],'originalGroundTruthSha256':annotations[-1]['sha256']})
generated=r/'build/generated146'
for row in json.loads((generated/'generated-index.json').read_text(encoding='utf8')):
 p=generated/(row['case']+'.fobs.json');lp=out/(row['case']+'-labels.json');lp.write_bytes((generated/lp.name).read_bytes());lab=json.loads(lp.read_text(encoding='utf8'));n=row['frames'];row.update(domain='generated-overlap-diagnostic',qualificationEligible=False,name=row['case'],target=1,start=1,end=n,stride=1,width=320,height=240,sourceFrames=list(range(1,n+1)),pack=p.relative_to(r).as_posix(),packSha256=sha(p.read_bytes()),labels=lp.relative_to(r).as_posix(),labelsSha256=sha(lp.read_bytes()),visibilityCounts={v:sum(l['visibility']==v for l in lab['rows']) for v in ['visible','partial','occluded','out-of-view','uncertain']},distractorLabelledSamples=sum('otherBoxes' in l for l in lab['rows']));records.append(row)
# Already-exposed145 original pixels. Reused only for development/regression and actual restart qualification.
old=json.loads((r/'docs/publication145/source145.json').read_text(encoding='utf8'))
for source in old['records']:
 if source['case'] not in ['generated-reference','copy-remains','trajectory-return']:continue
 row=dict(source);p=r/'build/regression146'/(row['case']+'.fobs.json');assert sha(p.read_bytes())==row['packSha256'];row.update(group='regression',domain='exposed-development',pack=p.relative_to(r).as_posix());records.append(row)
for group in {x['group'] for x in records}:
 donor=next(x for x in records if x['group']==group and x['role']=='reference-source');hashes={f['imageSha256'] for f in json.loads((r/donor['pack']).read_bytes())['frames']}
 for target in [x for x in records if x['group']==group and x['role']=='evaluation']:
  overlap=[f['sourceFrame'] for f in json.loads((r/target['pack']).read_bytes())['frames'] if f['imageSha256'] in hashes]
  target['donorIdenticalEvaluationFrames']=overlap
  if group=='generated':assert overlap==[8,9,10] and target['qualificationEligible'] is False
  else:assert not overlap,(group,target['case'],overlap)
manifest={'createdAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'algorithmLockSha256':sha(lock.read_bytes()),'records':records,'downloads':json.loads((root/'download.json').read_text(encoding='utf8')),'sourceReview':review,'limitations':['Source-only event selection under frozen rules, not outcome selection.','External geometry and attributed AI visibility review; no independent human adjudication.','Generated controls and already-exposed regressions are separate from fresh footage.','Image-plane motion and identical appearances cannot certify identity.']}
(out/'annotation-integrity146.json').write_text(json.dumps(annotations,indent=2)+'\n',encoding='utf8');(out/'source146.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf8');print(json.dumps([{'case':x['case'],'group':x['group'],'role':x['role'],'counts':x['visibilityCounts']} for x in records]))
